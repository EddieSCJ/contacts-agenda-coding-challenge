package com.contacts.agenda.controller;

import com.contacts.agenda.model.Contact;
import com.contacts.agenda.model.ContactEntity;
import com.contacts.agenda.repository.ContactRepository;
import com.contacts.agenda.fixture.ContactFixture.*;
import com.contacts.agenda.fixture.ContactEntityFixture;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cache.CacheManager;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

import static com.contacts.agenda.fixture.ContactEntityFixture.createContactEntity;
import static com.contacts.agenda.fixture.ContactFixture.createContact;
import static com.contacts.agenda.fixture.ContactFixture.createContactList;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@ActiveProfiles("test")
@DisplayName("Contact Controller Full Integration Tests")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ContactControllerIntegrationTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:4.4.6"))
            .withExposedPorts(27017);

    @Container
    static GenericContainer<?> redisContainer = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().port(8089))
            .build();

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", () -> mongoDBContainer.getConnectionString() + "/test_contacts_agenda");
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", redisContainer::getFirstMappedPort);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @BeforeEach
    void setUp() {
        contactRepository.deleteAll();

        wireMock.resetAll();
        wireMock.resetRequests();
        wireMock.resetScenarios();
        wireMock.resetMappings();

        cacheManager.getCacheNames().forEach(cacheName -> {
            cacheManager.getCache(cacheName).clear();
        });

        circuitBreakerRegistry.getAllCircuitBreakers().forEach(cb -> cb.reset());
    }

    @Nested
    @DisplayName("GET /contacts - External API Success Scenarios")
    class ExternalApiSuccessScenarios {

        @Test
        @DisplayName("Should fetch contacts from external API since is prioritized")
        void shouldFetchContactsFromExternalApiWhenDatabaseIsEmpty() throws Exception {
            Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
            List<Contact> externalApiContacts = Arrays.asList(
                    createContact(1L, "External Contact 1", "external1@kenectlabs.com", "KENECT_LABS", now, now),
                    createContact(2L, "External Contact 2", "external2@kenectlabs.com", "KENECT_LABS", now, now)
            );

            wireMock.stubFor(get(urlPathEqualTo("/api/v1/contacts"))
                    .withQueryParam("page", equalTo("1"))
                    .withQueryParam("pageSize", equalTo("1000"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withHeader("total-count", "2")
                            .withBody(objectMapper.writeValueAsString(externalApiContacts))));

            ResponseEntity<List<Contact>> response = restTemplate.exchange(
                    "/contacts",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {
                    }
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(2);
            assertThat(response.getBody().getFirst().id()).isEqualTo(1);
            assertThat(response.getBody().getFirst().name()).isEqualTo("External Contact 1");
            assertThat(response.getBody().getFirst().email()).isEqualTo("external1@kenectlabs.com");
            assertThat(response.getBody().getFirst().source()).isEqualTo("KENECT_LABS");
            assertThat(response.getBody().get(1).id()).isEqualTo(2);
            assertThat(response.getBody().get(1).name()).isEqualTo("External Contact 2");
            assertThat(response.getBody().get(1).email()).isEqualTo("external2@kenectlabs.com");
        }

        @Test
        @DisplayName("Should handle large dataset from external API with pagination")
        void shouldHandleLargeDatasetFromExternalApiWithPagination() throws Exception {
            Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
            List<Contact> firstPageContacts = createContactList(1000, "First Page Contact", now);
            List<Contact> secondPageContacts = createContactList(500, "Second Page Contact", now, 1001);

            wireMock.stubFor(get(urlPathEqualTo("/api/v1/contacts"))
                    .withQueryParam("page", equalTo("1"))
                    .withQueryParam("pageSize", equalTo("1000"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withHeader("total-count", "1500")
                            .withBody(objectMapper.writeValueAsString(firstPageContacts))));

            wireMock.stubFor(get(urlPathEqualTo("/api/v1/contacts"))
                    .withQueryParam("page", equalTo("2"))
                    .withQueryParam("pageSize", equalTo("500"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withHeader("total-count", "1500")
                            .withBody(objectMapper.writeValueAsString(secondPageContacts))));

            ResponseEntity<List<Contact>> response = restTemplate.exchange(
                    "/contacts",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {
                    }
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(1500);

            List<ContactEntity> savedContacts = contactRepository.findAll();
            assertThat(savedContacts).hasSize(1500);
        }
    }

    @Nested
    @DisplayName("GET /contacts - Fallback to Database Scenarios")
    class FallbackToDatabaseScenarios {

        @Test
        @DisplayName("Should return contacts from database when external API is unavailable")
        void shouldReturnContactsFromDatabaseWhenExternalApiIsUnavailable() {
            Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);

            ContactEntity savedContact1 = createContactEntity(1L, "Cached Contact 1", "cached1@example.com", now);
            ContactEntity savedContact2 = createContactEntity(2L, "Cached Contact 2", "cached2@example.com", now);
            contactRepository.saveAll(Arrays.asList(savedContact1, savedContact2));

            wireMock.stubFor(get(urlPathEqualTo("/api/v1/contacts"))
                    .willReturn(aResponse()
                            .withStatus(500)
                            .withBody("Internal Server Error")));

            ResponseEntity<List<Contact>> response = restTemplate.exchange(
                    "/contacts",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {
                    }
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(2);

            assertThat(response.getBody().getFirst().id()).isEqualTo(1);
            assertThat(response.getBody().getFirst().name()).isEqualTo("Cached Contact 1");
            assertThat(response.getBody().getFirst().email()).isEqualTo("cached1@example.com");

            assertThat(response.getBody().get(1).id()).isEqualTo(2);
            assertThat(response.getBody().get(1).name()).isEqualTo("Cached Contact 2");
            assertThat(response.getBody().get(1).email()).isEqualTo("cached2@example.com");
        }

        @Test
        @DisplayName("Should return contacts from database when external API times out")
        void shouldReturnContactsFromDatabaseWhenExternalApiTimesOut() {
            Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);

            ContactEntity savedContact = createContactEntity(1L, "Timeout Fallback Contact", "timeout@example.com", now);
            contactRepository.save(savedContact);

            wireMock.stubFor(get(urlPathEqualTo("/api/v1/contacts"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withFixedDelay(10000)
                            .withBody("[]")));

            ResponseEntity<List<Contact>> response = restTemplate.exchange(
                    "/contacts",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {
                    }
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(1);
            assertThat(response.getBody().getFirst().name()).isEqualTo("Timeout Fallback Contact");
        }
    }

    @Nested
    @DisplayName("GET /contacts - Circuit Breaker and Resilience")
    class CircuitBreakerAndResilienceScenarios {

        @Test
        @DisplayName("Should open circuit breaker after multiple failures and fallback to database")
        void shouldOpenCircuitBreakerAfterMultipleFailuresAndFallbackToDatabase() {
            Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);

            ContactEntity fallbackContact = createContactEntity(1L, "Circuit Breaker Fallback", "fallback@example.com", now);
            contactRepository.save(fallbackContact);

            wireMock.stubFor(get(urlPathEqualTo("/api/v1/contacts"))
                    .willReturn(aResponse()
                            .withStatus(500)
                            .withBody("Server Error")));

            for (int i = 0; i < 5; i++) {
                ResponseEntity<List<Contact>> response = restTemplate.exchange(
                        "/contacts",
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<>() {
                        });

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).hasSize(1);
                assertThat(response.getBody().getFirst().name()).isEqualTo("Circuit Breaker Fallback");
            }
        }
    }

    @Nested
    @DisplayName("GET /contacts - Data Sync and Caching")
    class DataSyncAndCachingScenarios {

        @Test
        @DisplayName("Should sync fresh data from external API and update database")
        void shouldSyncFreshDataFromExternalApiAndUpdateDatabase() throws Exception {
            Instant past = Instant.now().minus(1, ChronoUnit.HOURS).truncatedTo(ChronoUnit.MILLIS);
            Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);

            ContactEntity staleContact = ContactEntityFixture.createContactEntity(1L, "Stale Contact", "stale@example.com", past);
            contactRepository.save(staleContact);

            List<Contact> freshContacts = Arrays.asList(
                    createContact(1L, "Fresh Contact", "fresh@kenectlabs.com", "KENECT_LABS", now, now),
                    createContact(2L, "New Contact", "new@kenectlabs.com", "KENECT_LABS", now, now)
            );

            wireMock.stubFor(get(urlPathEqualTo("/api/v1/contacts"))
                    .withQueryParam("page", equalTo("1"))
                    .withQueryParam("pageSize", equalTo("1000"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withHeader("total-count", "2")
                            .withBody(objectMapper.writeValueAsString(freshContacts))));

            ResponseEntity<List<Contact>> response = restTemplate.exchange(
                    "/contacts",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {
                    }
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(2);
            assertThat(response.getBody().getFirst().name()).isEqualTo("Fresh Contact");
            assertThat(response.getBody().get(1).name()).isEqualTo("New Contact");

            List<ContactEntity> updatedContacts = contactRepository.findAll();
            assertThat(updatedContacts).hasSize(2);
            assertThat(updatedContacts.stream().noneMatch(c -> c.name().equals("Stale Contact"))).isTrue();
            assertThat(updatedContacts.stream().anyMatch(c -> c.name().equals("Fresh Contact"))).isTrue();
            assertThat(updatedContacts.stream().anyMatch(c -> c.name().equals("New Contact"))).isTrue();
        }
    }
}
