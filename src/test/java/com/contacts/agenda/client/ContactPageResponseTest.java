package com.contacts.agenda.client;

import com.contacts.agenda.model.Contact;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import java.io.Serializable;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static com.contacts.agenda.fixture.ContactFixture.createContact;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

@DisplayName("Contact Page Response Tests")
class ContactPageResponseTest {

    @Nested
    @DisplayName("Factory Method")
    class FactoryMethod {

        @Test
        @DisplayName("Should create response from contacts and headers")
        void shouldCreateResponseFromContactsAndHeaders() {
            List<Contact> contacts = List.of(
                    createContact(1L, "John Doe", "john@example.com"),
                    createContact(2L, "Jane Smith", "jane@example.com")
            );
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.add("X-Total-Count", "100");
            httpHeaders.add("X-Page-Number", "1");

            ContactPageResponse response = ContactPageResponse.from(contacts, httpHeaders);

            assertThat(response.getContacts()).hasSize(2);
            assertThat(response.getHeaders()).containsEntry("x-total-count", "100");
            assertThat(response.getHeaders()).containsEntry("x-page-number", "1");
        }

        @Test
        @DisplayName("Should convert header keys to lowercase")
        void shouldConvertHeaderKeysToLowercase() {
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.add("Content-Type", "application/json");
            httpHeaders.add("X-Custom-Header", "custom-value");

            ContactPageResponse response = ContactPageResponse.from(List.of(), httpHeaders);

            assertThat(response.getHeaders()).containsKeys("content-type", "x-custom-header");
            assertThat(response.getHeaders()).doesNotContainKeys("Content-Type", "X-Custom-Header");
        }

        @Test
        @DisplayName("Should take first value when header has multiple values")
        void shouldTakeFirstValueWhenHeaderHasMultipleValues() {
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.add("Accept", "application/json");
            httpHeaders.add("Accept", "text/html");
            httpHeaders.add("Accept", "application/xml");

            ContactPageResponse response = ContactPageResponse.from(List.of(), httpHeaders);

            assertThat(response.getHeaders()).containsEntry("accept", "application/json");
        }

        @Test
        @DisplayName("Should handle empty headers")
        void shouldHandleEmptyHeaders() {
            List<Contact> contacts = List.of(createContact(1L, "Test", "test@example.com"));
            HttpHeaders httpHeaders = new HttpHeaders();

            ContactPageResponse response = ContactPageResponse.from(contacts, httpHeaders);

            assertThat(response.getContacts()).hasSize(1);
            assertThat(response.getHeaders()).isEmpty();
        }

        @Test
        @DisplayName("Should handle empty contacts list")
        void shouldHandleEmptyContactsList() {
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.add("X-Total-Count", "0");

            ContactPageResponse response = ContactPageResponse.from(List.of(), httpHeaders);

            assertThat(response.getContacts()).isEmpty();
            assertThat(response.getHeaders()).containsEntry("x-total-count", "0");
        }

        @Test
        @DisplayName("Should skip headers with empty values")
        void shouldSkipHeadersWithEmptyValues() {
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.add("X-Valid-Header", "value");
            httpHeaders.put("X-Empty-Header", List.of());

            ContactPageResponse response = ContactPageResponse.from(List.of(), httpHeaders);

            assertThat(response.getHeaders()).containsEntry("x-valid-header", "value");
            assertThat(response.getHeaders()).doesNotContainKey("x-empty-header");
        }
    }

    @Nested
    @DisplayName("Serialization")
    class Serialization {

        @Test
        @DisplayName("Should be serializable")
        void shouldBeSerializable() {
            ContactPageResponse response = ContactPageResponse.from(List.of(), HttpHeaders.EMPTY);
            assertThat(response).isInstanceOf(Serializable.class);
        }
    }

    @Nested
    @DisplayName("Real World Scenarios")
    class RealWorldScenarios {

        @Test
        @DisplayName("Should handle typical pagination headers")
        void shouldHandleTypicalPaginationHeaders() {
            List<Contact> contacts = List.of(
                    createContact(1L, "Contact 1", "contact1@example.com"),
                    createContact(2L, "Contact 2", "contact2@example.com")
            );

            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.add("X-Total-Count", "100");
            httpHeaders.add("X-Page-Number", "1");
            httpHeaders.add("X-Page-Size", "2");
            httpHeaders.add("Link", "<https://api.example.com/contacts?page=2>; rel=\"next\"");

            ContactPageResponse response = ContactPageResponse.from(contacts, httpHeaders);

            assertThat(response.getContacts()).hasSize(2);
            assertThat(response.getHeaders())
                    .contains(
                            entry("x-total-count", "100"),
                            entry("x-page-number", "1"),
                            entry("x-page-size", "2"),
                            entry("link", "<https://api.example.com/contacts?page=2>; rel=\"next\"")
                    );
        }

        @Test
        @DisplayName("Should preserve contact data integrity")
        void shouldPreserveContactDataIntegrity() {
            Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
            Contact originalContact = Contact.builder()
                    .id(100L)
                    .name("Important Contact")
                    .email("important@example.com")
                    .source("external-api")
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            ContactPageResponse response = ContactPageResponse.from(
                    List.of(originalContact),
                    new HttpHeaders()
            );

            Contact retrievedContact = response.getContacts().getFirst();
            assertThat(retrievedContact).isEqualTo(originalContact);
            assertThat(retrievedContact.id()).isEqualTo(100L);
            assertThat(retrievedContact.name()).isEqualTo("Important Contact");
            assertThat(retrievedContact.email()).isEqualTo("important@example.com");
            assertThat(retrievedContact.source()).isEqualTo("external-api");
            assertThat(retrievedContact.createdAt()).isEqualTo(now);
            assertThat(retrievedContact.updatedAt()).isEqualTo(now);
        }
    }
}
