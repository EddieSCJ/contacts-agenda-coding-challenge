package com.contacts.agenda.client;

import com.contacts.agenda.model.Contact;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.util.List;

import static com.contacts.agenda.fixture.ContactFixture.createContact;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("KenectLabsClient WireMock Tests")
class KenectLabsClientTest {

    private WireMockServer wireMockServer;
    private KenectLabsClient kenectLabsClient;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        RestClient restClient = RestClient.builder()
                .baseUrl(wireMockServer.baseUrl())
                .build();

        HttpServiceProxyFactory factory = HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(restClient))
                .build();

        kenectLabsClient = factory.createClient(KenectLabsClient.class);
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Nested
    @DisplayName("GET /api/v1/contacts")
    class GetContacts {

        @Test
        @DisplayName("Should successfully fetch contacts without pagination parameters")
        void shouldFetchContactsWithoutPagination() throws Exception {
            List<Contact> expectedContacts = List.of(
                    createContact(1L, "John Doe", "john@example.com"),
                    createContact(2L, "Jane Smith", "jane@example.com")
            );

            String jsonResponse = objectMapper.writeValueAsString(expectedContacts);

            stubFor(get(urlEqualTo("/api/v1/contacts"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(jsonResponse)));

            ResponseEntity<List<Contact>> response = kenectLabsClient.getContacts(null, null);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody()).hasSize(2);
            assertThat(response.getBody().getFirst().id()).isEqualTo(1L);
            assertThat(response.getBody().getFirst().name()).isEqualTo("John Doe");
            assertThat(response.getBody().getFirst().email()).isEqualTo("john@example.com");
            assertThat(response.getBody().get(1).id()).isEqualTo(2L);
            assertThat(response.getBody().get(1).name()).isEqualTo("Jane Smith");
            assertThat(response.getBody().get(1).email()).isEqualTo("jane@example.com");

            verify(getRequestedFor(urlEqualTo("/api/v1/contacts")));
        }

        @Test
        @DisplayName("Should fetch contacts with page parameter")
        void shouldFetchContactsWithPageParameter() throws Exception {
            List<Contact> expectedContacts = List.of(
                    createContact(11L, "Contact 11", "contact11@example.com")
            );

            String jsonResponse = objectMapper.writeValueAsString(expectedContacts);

            stubFor(get(urlPathEqualTo("/api/v1/contacts"))
                    .withQueryParam("page", equalTo("2"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withHeader("X-Page-Number", "2")
                            .withBody(jsonResponse)));

            ResponseEntity<List<Contact>> response = kenectLabsClient.getContacts(2L, null);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody()).hasSize(1);
            assertThat(response.getBody().getFirst().id()).isEqualTo(11L);
            assertThat(response.getBody().getFirst().name()).isEqualTo("Contact 11");
            assertThat(response.getBody().getFirst().email()).isEqualTo("contact11@example.com");

            verify(getRequestedFor(urlPathEqualTo("/api/v1/contacts"))
                    .withQueryParam("page", equalTo("2")));
        }

        @Test
        @DisplayName("Should fetch contacts with pageSize parameter")
        void shouldFetchContactsWithPageSizeParameter() throws Exception {
            List<Contact> expectedContacts = List.of(
                    createContact(1L, "Contact 1", "contact1@example.com"),
                    createContact(2L, "Contact 2", "contact2@example.com"),
                    createContact(3L, "Contact 3", "contact3@example.com")
            );

            String jsonResponse = objectMapper.writeValueAsString(expectedContacts);

            stubFor(get(urlPathEqualTo("/api/v1/contacts"))
                    .withQueryParam("pageSize", equalTo("3"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withHeader("X-Page-Size", "3")
                            .withBody(jsonResponse)));

            ResponseEntity<List<Contact>> response = kenectLabsClient.getContacts(null, 3L);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody()).hasSize(3);

            verify(getRequestedFor(urlPathEqualTo("/api/v1/contacts"))
                    .withQueryParam("pageSize", equalTo("3")));
        }

        @Test
        @DisplayName("Should fetch contacts with both page and pageSize parameters")
        void shouldFetchContactsWithBothParameters() throws Exception {
            List<Contact> expectedContacts = List.of(
                    createContact(21L, "Contact 21", "contact21@example.com"),
                    createContact(22L, "Contact 22", "contact22@example.com")
            );

            String jsonResponse = objectMapper.writeValueAsString(expectedContacts);

            stubFor(get(urlPathEqualTo("/api/v1/contacts"))
                    .withQueryParam("page", equalTo("3"))
                    .withQueryParam("pageSize", equalTo("2"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withHeader("X-Page-Number", "3")
                            .withHeader("X-Page-Size", "2")
                            .withHeader("X-Total-Count", "50")
                            .withBody(jsonResponse)));

            ResponseEntity<List<Contact>> response = kenectLabsClient.getContacts(3L, 2L);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody()).hasSize(2);
            assertThat(response.getHeaders().get("X-Total-Count")).containsExactly("50");

            verify(getRequestedFor(urlPathEqualTo("/api/v1/contacts"))
                    .withQueryParam("page", equalTo("3"))
                    .withQueryParam("pageSize", equalTo("2")));
        }

        @Test
        @DisplayName("Should handle empty contact list")
        void shouldHandleEmptyContactList() throws Exception {
            String jsonResponse = objectMapper.writeValueAsString(List.of());

            stubFor(get(urlEqualTo("/api/v1/contacts"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withHeader("X-Total-Count", "0")
                            .withBody(jsonResponse)));

            ResponseEntity<List<Contact>> response = kenectLabsClient.getContacts(null, null);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody()).isEmpty();
            assertThat(response.getHeaders().get("X-Total-Count")).containsExactly("0");

            verify(getRequestedFor(urlEqualTo("/api/v1/contacts")));
        }

        @Test
        @DisplayName("Should handle 404 Not Found response")
        void shouldHandle404Response() {
            stubFor(get(urlEqualTo("/api/v1/contacts"))
                    .willReturn(aResponse()
                            .withStatus(404)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"error\": \"Not Found\"}")));

            var ex = assertThrows(HttpClientErrorException.NotFound.class,
                    () -> kenectLabsClient.getContacts(null, null));

            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            verify(getRequestedFor(urlEqualTo("/api/v1/contacts")));
        }

        @Test
        @DisplayName("Should handle 500 Internal Server Error")
        void shouldHandle500Response() {
            stubFor(get(urlEqualTo("/api/v1/contacts"))
                    .willReturn(aResponse()
                            .withStatus(500)
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"error\": \"Internal Server Error\"}")));

            var ex = assertThrows(HttpServerErrorException.InternalServerError.class,
                    () -> kenectLabsClient.getContacts(null, null));

            assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            verify(getRequestedFor(urlEqualTo("/api/v1/contacts")));
        }

        @Test
        @DisplayName("Should handle timeout/delayed response")
        void shouldHandleDelayedResponse() throws Exception {
            List<Contact> expectedContacts = List.of(
                    createContact(1L, "Delayed Contact", "delayed@example.com")
            );

            String jsonResponse = objectMapper.writeValueAsString(expectedContacts);

            stubFor(get(urlEqualTo("/api/v1/contacts"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(jsonResponse)
                            .withFixedDelay(1000)));

            long startTime = System.currentTimeMillis();
            ResponseEntity<List<Contact>> response = kenectLabsClient.getContacts(null, null);
            long endTime = System.currentTimeMillis();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody()).hasSize(1);
            assertThat(endTime - startTime).isGreaterThanOrEqualTo(1000);

            verify(getRequestedFor(urlEqualTo("/api/v1/contacts")));
        }

        @Test
        @DisplayName("Should verify correct headers are sent")
        void shouldVerifyRequestHeaders() throws Exception {
            String jsonResponse = objectMapper.writeValueAsString(List.of());

            stubFor(get(urlEqualTo("/api/v1/contacts"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(jsonResponse)));

            kenectLabsClient.getContacts(null, null);

            verify(getRequestedFor(urlEqualTo("/api/v1/contacts"))
                    .withHeader("Accept", matching(".*")));
        }
    }

    @Nested
    @DisplayName("Response Headers Verification")
    class ResponseHeadersVerification {

        @Test
        @DisplayName("Should capture custom response headers")
        void shouldCaptureCustomResponseHeaders() throws Exception {
            String jsonResponse = objectMapper.writeValueAsString(List.of());

            stubFor(get(urlPathEqualTo("/api/v1/contacts"))
                    .withQueryParam("page", equalTo("1"))
                    .withQueryParam("pageSize", equalTo("10"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withHeader("X-Total-Count", "100")
                            .withHeader("X-Page-Number", "1")
                            .withHeader("X-Page-Size", "10")
                            .withHeader("X-Total-Pages", "10")
                            .withBody(jsonResponse)));

            ResponseEntity<List<Contact>> response = kenectLabsClient.getContacts(1L, 10L);

            assertThat(response.getHeaders().get("X-Total-Count")).containsExactly("100");
            assertThat(response.getHeaders().get("X-Page-Number")).containsExactly("1");
            assertThat(response.getHeaders().get("X-Page-Size")).containsExactly("10");
            assertThat(response.getHeaders().get("X-Total-Pages")).containsExactly("10");
        }
    }
}
