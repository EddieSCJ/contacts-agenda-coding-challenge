package com.contacts.agenda.mapper;

import com.contacts.agenda.model.Contact;
import com.contacts.agenda.model.ContactEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static com.contacts.agenda.fixture.ContactEntityFixture.*;
import static com.contacts.agenda.fixture.ContactFixture.*;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Contact Mapper Tests")
class ContactMapperTest {

    private final ContactMapper mapper = ContactMapper.INSTANCE;

    @Nested
    @DisplayName("To Entity Mapping")
    class ToEntityMapping {

        @Test
        @DisplayName("Should map Contact to ContactEntity with all fields")
        void shouldMapContactToEntity() {
            Instant now = now();
            Contact contact = createContact(1L, "John Doe", "john@example.com", "external-api", now, now);

            ContactEntity entity = mapper.toEntity(contact);

            assertThat(entity.id()).isEqualTo(1L);
            assertThat(entity.name()).isEqualTo("John Doe");
            assertThat(entity.email()).isEqualTo("john@example.com");
            assertThat(entity.source()).isEqualTo("external-api");
            assertThat(entity.createdAt()).isEqualTo(now);
            assertThat(entity.updatedAt()).isEqualTo(now);
            assertThat(entity.syncedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should set syncedAt to current time when mapping to entity")
        void shouldSetSyncedAtToCurrentTime() {
            Instant before = Instant.now();
            Contact contact = createContact(1L, "Test", "test@example.com");

            ContactEntity entity = mapper.toEntity(contact);

            assertThat(entity.syncedAt()).isBetween(before, Instant.now());
        }

        @Test
        @DisplayName("Should handle null values in Contact")
        void shouldHandleNullValuesInContact() {
            Contact contact = Contact.builder()
                    .id(1L)
                    .build();

            ContactEntity entity = mapper.toEntity(contact);

            assertThat(entity.id()).isEqualTo(1L);
            assertThat(entity.name()).isNull();
            assertThat(entity.email()).isNull();
            assertThat(entity.source()).isNull();
            assertThat(entity.syncedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("To Domain Mapping")
    class ToDomainMapping {

        @Test
        @DisplayName("Should map ContactEntity to Contact with all fields")
        void shouldMapEntityToContact() {
            Instant now = now();
            ContactEntity entity = createEntity(1L, "Jane Smith", "jane@example.com", "database", now, now, now);

            Contact contact = mapper.toDomain(entity);

            assertThat(contact.id()).isEqualTo(1L);
            assertThat(contact.name()).isEqualTo("Jane Smith");
            assertThat(contact.email()).isEqualTo("jane@example.com");
            assertThat(contact.source()).isEqualTo("database");
            assertThat(contact.createdAt()).isEqualTo(now);
            assertThat(contact.updatedAt()).isEqualTo(now);
        }

        @Test
        @DisplayName("Should not include syncedAt in domain Contact")
        void shouldNotIncludeSyncedAtInDomain() {
            ContactEntity entity = createMinimalEntity(1L);

            Contact contact = mapper.toDomain(entity);

            assertThat(contact.id()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Should handle null values in ContactEntity")
        void shouldHandleNullValuesInEntity() {
            ContactEntity entity = ContactEntity.builder()
                    .id(1L)
                    .build();

            Contact contact = mapper.toDomain(entity);

            assertThat(contact.id()).isEqualTo(1L);
            assertThat(contact.name()).isNull();
            assertThat(contact.email()).isNull();
            assertThat(contact.source()).isNull();
        }
    }

    @Nested
    @DisplayName("Bidirectional Mapping")
    class BidirectionalMapping {

        @Test
        @DisplayName("Should preserve data through bidirectional mapping")
        void shouldPreserveDataThroughBidirectionalMapping() {
            Instant now = now();
            Contact original = createContact(100L, "Bidirectional Test", "test@example.com", "test-source", now, now);

            Contact result = mapper.toDomain(mapper.toEntity(original));

            assertThat(result.id()).isEqualTo(original.id());
            assertThat(result.name()).isEqualTo(original.name());
            assertThat(result.email()).isEqualTo(original.email());
            assertThat(result.source()).isEqualTo(original.source());
            assertThat(result.createdAt()).isEqualTo(original.createdAt());
            assertThat(result.updatedAt()).isEqualTo(original.updatedAt());
        }
    }

    private static Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MILLIS);
    }
}
