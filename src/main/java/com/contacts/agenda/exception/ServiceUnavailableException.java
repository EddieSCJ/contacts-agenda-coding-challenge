package com.contacts.agenda.exception;

/**
 * Exception thrown when both the external API and database fallback are unavailable.
 * <p>
 * This exception occurs in the following scenarios:
 * <ul>
 *   <li>External API is down or unreachable AND the fallback database is also unavailable</li>
 *   <li>First call to the external API fails before any data has been cached in database</li>
 * </ul>
 * <p>
 * <strong>HTTP Mapping:</strong>
 * <blockquote>
 * This exception is mapped to HTTP 503 Service Unavailable in {@link com.contacts.agenda.exception.GlobalExceptionHandler}.
 * </blockquote>
 * <p>
 * <strong>⚠️ Future Considerations:</strong>
 * <blockquote>
 * When adding alternative communication layers (e.g., gRPC), ensure this exception is mapped
 * to the appropriate status code (e.g., gRPC UNAVAILABLE status).
 * </blockquote>
 */
public class ServiceUnavailableException extends RuntimeException {
    public ServiceUnavailableException(String message) {
        super(message);
    }
}
