package com.contacts.agenda.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Error response returned when an operation fails", example = """
            {
                "status": 503,
                "message": "Service unavailable - both external API and fallback database are unavailable",
                "timestamp": "2025-10-05T10:30:00Z",
                "path": "/contacts"
            }
        """)
public record ErrorResponse(
        @Schema(description = "HTTP status code", example = "503")
        int status,

        @Schema(description = "Error message describing what went wrong", example = "Service unavailable - both external API and fallback database are unavailable")
        String message,

        @Schema(description = "Timestamp when the error occurred", example = "2025-10-05T10:30:00Z")
        Instant timestamp,

        @Schema(description = "Request path that caused the error", example = "/contacts")
        String path
) {
    public ErrorResponse(int status, String message, String path) {
        this(status, message, Instant.now(), path);
    }
}
