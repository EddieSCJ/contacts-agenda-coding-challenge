package com.contacts.agenda.fixture;

import com.contacts.agenda.model.Contact;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.IntStream;

public class ContactFixture {

    public static Contact.ContactBuilder aContact() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        return Contact.builder()
                .id(1L)
                .name("Test Contact")
                .email("test@example.com")
                .source("test-source")
                .createdAt(now)
                .updatedAt(now);
    }

    public static Contact createContact(Long id, String name, String email) {
        return aContact()
                .id(id)
                .name(name)
                .email(email)
                .build();
    }

    public static Contact createContact(Long id, String name, String email, String source, Instant createdAt, Instant updatedAt) {
        return Contact.builder()
                .id(id)
                .name(name)
                .email(email)
                .source(source)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }

    public static List<Contact> createContactList(int count, String namePrefix, Instant timestamp) {
        return createContactList(count, namePrefix, timestamp, 1);
    }

    public static List<Contact> createContactList(int count, String namePrefix, Instant timestamp, int startId) {
        return IntStream.range(0, count)
                .mapToObj(i -> createContact(
                        (long) (startId + i),
                        namePrefix + " " + (i + 1),
                        "contact" + (startId + i) + "@kenectlabs.com",
                        "KENECT_LABS",
                        timestamp,
                        timestamp
                ))
                .toList();
    }
}
