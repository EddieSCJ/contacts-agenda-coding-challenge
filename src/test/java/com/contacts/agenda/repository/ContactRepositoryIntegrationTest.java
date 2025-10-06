package com.contacts.agenda.repository;

import com.contacts.agenda.model.ContactEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static com.contacts.agenda.fixture.ContactEntityFixture.*;
import static org.assertj.core.api.Assertions.assertThat;

@DataMongoTest
@Testcontainers
@DisplayName("Contact Repository Integration Tests")
@EnableMongoRepositories(basePackageClasses = ContactRepository.class)
class ContactRepositoryIntegrationTest {

    @Container
    static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7")
            .withExposedPorts(27017);

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", () -> mongoDBContainer.getConnectionString() + "/test_contacts_agenda");
    }

    @Autowired
    private ContactRepository contactRepository;

    @BeforeEach
    void setUp() {
        contactRepository.deleteAll();
    }

    @Nested
    @DisplayName("Save Operations")
    class SaveOperations {

        @Test
        @DisplayName("Should save and retrieve a contact successfully")
        void shouldSaveAndRetrieveContact() {
            ContactEntity contact = createSimpleContact(1L, "John Doe", "john@example.com");
            ContactEntity savedContact = contactRepository.save(contact);

            assertThat(savedContact).isNotNull();
            assertThat(savedContact.id()).isEqualTo(1L);
            assertThat(savedContact.name()).isEqualTo("John Doe");
            assertThat(savedContact.email()).isEqualTo("john@example.com");
        }

        @Test
        @DisplayName("Should preserve all entity fields after save and retrieve")
        void shouldPreserveAllFieldsAfterSaveAndRetrieve() {
            Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
            ContactEntity contact = aContact()
                    .id(100L)
                    .name("Full Contact")
                    .email("full@example.com")
                    .source("external-api")
                    .createdAt(now)
                    .updatedAt(now)
                    .syncedAt(now)
                    .build();

            contactRepository.save(contact);
            Optional<ContactEntity> retrieved = contactRepository.findById(100L);

            assertThat(retrieved).isPresent();
            ContactEntity retrievedContact = retrieved.get();
            assertThat(retrievedContact.id()).isEqualTo(100L);
            assertThat(retrievedContact.name()).isEqualTo("Full Contact");
            assertThat(retrievedContact.email()).isEqualTo("full@example.com");
            assertThat(retrievedContact.source()).isEqualTo("external-api");
            assertThat(retrievedContact.createdAt()).isEqualTo(now);
            assertThat(retrievedContact.updatedAt()).isEqualTo(now);
            assertThat(retrievedContact.syncedAt()).isEqualTo(now);
        }
    }

    @Nested
    @DisplayName("Find Operations")
    class FindOperations {

        @Test
        @DisplayName("Should find contact by ID when it exists")
        void shouldFindContactById() {
            ContactEntity contact = createSimpleContact(1L, "Jane Smith", "jane@example.com");
            contactRepository.save(contact);

            Optional<ContactEntity> foundContact = contactRepository.findById(1L);

            assertThat(foundContact).isPresent();
            assertThat(foundContact.get().name()).isEqualTo("Jane Smith");
            assertThat(foundContact.get().email()).isEqualTo("jane@example.com");
        }

        @Test
        @DisplayName("Should return empty when contact not found")
        void shouldReturnEmptyWhenContactNotFound() {
            Optional<ContactEntity> foundContact = contactRepository.findById(999L);
            assertThat(foundContact).isEmpty();
        }

        @Test
        @DisplayName("Should find all contacts in the repository")
        void shouldFindAllContacts() {
            ContactEntity contact1 = createSimpleContact(1L, "Alice Johnson", "alice@example.com");
            ContactEntity contact2 = createSimpleContact(2L, "Bob Williams", "bob@example.com");
            ContactEntity contact3 = createSimpleContact(3L, "Charlie Brown", "charlie@example.com");

            contactRepository.saveAll(List.of(contact1, contact2, contact3));
            List<ContactEntity> allContacts = contactRepository.findAll();

            assertThat(allContacts).hasSize(3);
            assertThat(allContacts)
                    .extracting(ContactEntity::name)
                    .containsExactlyInAnyOrder("Alice Johnson", "Bob Williams", "Charlie Brown");
        }
    }

    @Nested
    @DisplayName("Update Operations")
    class UpdateOperations {

        @Test
        @DisplayName("Should update existing contact with new data")
        void shouldUpdateExistingContact() {
            ContactEntity contact = createSimpleContact(1L, "Original Name", "original@example.com");
            contactRepository.save(contact);

            ContactEntity updatedContact = ContactEntity.builder()
                    .id(contact.id())
                    .name("Updated Name")
                    .email("updated@example.com")
                    .source(contact.source())
                    .createdAt(contact.createdAt())
                    .updatedAt(Instant.now().truncatedTo(ChronoUnit.MILLIS))
                    .syncedAt(contact.syncedAt())
                    .build();
            contactRepository.save(updatedContact);

            Optional<ContactEntity> foundContact = contactRepository.findById(1L);
            assertThat(foundContact).isPresent();
            assertThat(foundContact.get().name()).isEqualTo("Updated Name");
            assertThat(foundContact.get().email()).isEqualTo("updated@example.com");
        }

        @Test
        @DisplayName("Should update contact when saving with same ID (upsert behavior)")
        void shouldHandleContactsWithSameIdByUpdating() {
            ContactEntity contact1 = createSimpleContact(1L, "First Version", "first@example.com");
            contactRepository.save(contact1);

            ContactEntity contact2 = createSimpleContact(1L, "Second Version", "second@example.com");
            contactRepository.save(contact2);

            long count = contactRepository.count();
            assertThat(count).isEqualTo(1);

            Optional<ContactEntity> foundContact = contactRepository.findById(1L);
            assertThat(foundContact).isPresent();
            assertThat(foundContact.get().name()).isEqualTo("Second Version");
            assertThat(foundContact.get().email()).isEqualTo("second@example.com");
        }
    }

    @Nested
    @DisplayName("Delete Operations")
    class DeleteOperations {

        @Test
        @DisplayName("Should delete contact by ID")
        void shouldDeleteContact() {
            ContactEntity contact = createSimpleContact(1L, "Delete Me", "delete@example.com");
            contactRepository.save(contact);

            contactRepository.deleteById(1L);

            Optional<ContactEntity> foundContact = contactRepository.findById(1L);
            assertThat(foundContact).isEmpty();
        }

        @Test
        @DisplayName("Should delete all contacts from the repository")
        void shouldDeleteAllContacts() {
            ContactEntity contact1 = createSimpleContact(1L, "Contact 1", "contact1@example.com");
            ContactEntity contact2 = createSimpleContact(2L, "Contact 2", "contact2@example.com");
            contactRepository.saveAll(List.of(contact1, contact2));

            contactRepository.deleteAll();

            List<ContactEntity> allContacts = contactRepository.findAll();
            assertThat(allContacts).isEmpty();
        }
    }

    @Nested
    @DisplayName("Count and Exists Operations")
    class CountAndExistsOperations {

        @Test
        @DisplayName("Should count the total number of contacts")
        void shouldCountContacts() {
            ContactEntity contact1 = createSimpleContact(1L, "Contact 1", "contact1@example.com");
            ContactEntity contact2 = createSimpleContact(2L, "Contact 2", "contact2@example.com");
            ContactEntity contact3 = createSimpleContact(3L, "Contact 3", "contact3@example.com");
            contactRepository.saveAll(List.of(contact1, contact2, contact3));

            long count = contactRepository.count();

            assertThat(count).isEqualTo(3);
        }

        @Test
        @DisplayName("Should check if contact exists by ID")
        void shouldCheckIfContactExists() {
            ContactEntity contact = createSimpleContact(1L, "Exists Test", "exists@example.com");
            contactRepository.save(contact);

            assertThat(contactRepository.existsById(1L)).isTrue();
            assertThat(contactRepository.existsById(999L)).isFalse();
        }
    }
}
