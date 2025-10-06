package com.contacts.agenda.fixture;

import com.contacts.agenda.model.ContactEntity;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class ContactEntityFixture {

    private static final String DEFAULT_SOURCE = "test-source";

    public static ContactEntity.ContactEntityBuilder aContact() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        return ContactEntity.builder()
                .id(1L)
                .name("Test Contact")
                .email("test@example.com")
                .source(DEFAULT_SOURCE)
                .createdAt(now)
                .updatedAt(now)
                .syncedAt(now);
    }

    public static ContactEntity createSimpleContact(Long id, String name, String email) {
        return aContact()
                .id(id)
                .name(name)
                .email(email)
                .build();
    }

    public static ContactEntity createEntity(Long id, String name, String email, String source, Instant createdAt, Instant updatedAt, Instant syncedAt) {
        return ContactEntity.builder()
                .id(id)
                .name(name)
                .email(email)
                .source(source)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .syncedAt(syncedAt)
                .build();
    }

    public static ContactEntity createMinimalEntity(Long id) {
        return aContact()
                .id(id)
                .name("Test")
                .email("test@example.com")
                .build();
    }

    public static ContactEntity.ContactEntityBuilder aContactEntity() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        return ContactEntity.builder()
                .id(1L)
                .name("Test Contact Entity")
                .email("test@example.com")
                .source("KENECT_LABS")
                .createdAt(now)
                .updatedAt(now)
                .syncedAt(now);
    }

    public static ContactEntity createContactEntity(Long id, String name, String email, Instant timestamp) {
        return ContactEntity.builder()
                .id(id)
                .name(name)
                .email(email)
                .source("KENECT_LABS")
                .createdAt(timestamp)
                .updatedAt(timestamp)
                .syncedAt(timestamp)
                .build();
    }
}
