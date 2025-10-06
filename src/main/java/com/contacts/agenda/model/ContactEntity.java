package com.contacts.agenda.model;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * <p>
 * <p>
 * This entity is automatically saved to the database after each successful API fetch, creating a backup
 * that can be used when the external API becomes unavailable. The 'syncedAt' field tracks when
 * each contact was last synchronized.
 * <p>
 * When the circuit breaker detects API failures, it retrieves contacts from this collection instead,
 * ensuring the service remains available even during upstream outages.
 * <p>
 * Note: Use {@link com.contacts.agenda.mapper.ContactMapper} for conversions between
 * {@link Contact} and {@link ContactEntity}.
 *
 * @see com.contacts.agenda.service.ContactFallbackService
 * @see com.contacts.agenda.service.ContactService
 */
@Builder
@Document(collection = "contacts")
public record ContactEntity(
        @Id
        @NotNull
        Long id,
        String name,
        String email,
        String source,
        Instant createdAt,
        Instant updatedAt,
        Instant syncedAt
) {
}
