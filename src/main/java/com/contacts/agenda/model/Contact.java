package com.contacts.agenda.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.io.Serializable;
import java.time.Instant;

@Builder
@Schema(description = "Contact information from external API or fallback storage", example = """
                {
                    "id": 12345,
                    "name": "John Doe",
                    "email": "john.doe@gmail.com",
                    "source": "KENECT_LABS",
                    "createdAt": "2025-10-05T10:30:00Z",
                    "updatedAt": "2025-10-05T10:30:00Z"
                }
        """)
public record Contact(
        @Schema(description = "Unique identifier of the contact", example = "12345")
        Long id,

        @Schema(description = "Full name of the contact", example = "John Doe")
        String name,

        @Schema(description = "Email address of the contact", example = "john.doe@example.com")
        String email,

        @Schema(description = "Source of the contact data, it's always KENECT_LABS", example = "KENECT_LABS")
        String source,

        @Schema(description = "Timestamp when the contact was created", example = "2025-10-05T10:30:00Z")
        Instant createdAt,

        @Schema(description = "Timestamp when the contact was last updated", example = "2025-10-05T10:30:00Z")
        Instant updatedAt
) {
}
