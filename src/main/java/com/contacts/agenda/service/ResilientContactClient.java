package com.contacts.agenda.service;

import com.contacts.agenda.client.ContactPageResponse;
import com.contacts.agenda.client.KenectLabsClient;
import com.contacts.agenda.model.Contact;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Resilient client for Kenect Labs external API with multiple resilience strategies.
 * <p>
 * <b>Resilience Features:</b>
 * <ul>
 *   <li><b>Retry:</b> Up to 3 attempts with exponential backoff (1s, 2s, 4s) + jitter</li>
 *   <li><b>Circuit Breaker:</b> Opens after 50% failures in 10 calls, stays open for 30 seconds</li>
 *   <li><b>Cache:</b> Stores responses in Redis for 5 minutes</li>
 *   <li><b>Fallback:</b> Returns data from MongoDB when API is unavailable</li>
 * </ul>
 * <p>
 * <b>Data Source Priority:</b>
 * <ol>
 *   <li><b>Redis Cache:</b> If available and not expired (fastest response)</li>
 *   <li><b>External API:</b> With retry and circuit breaker protection (primary source)</li>
 *   <li><b>Database Fallback:</b> When circuit breaker is open (high availability)</li>
 * </ol>
 * <p>
 * <b>Why Multiple Resilience Layers?</b>
 * <ul>
 *   <li><b>Protection Against Overload:</b> External APIs may contain thousands of pages or large datasets.
 *       If we overwhelm the API, the circuit breaker provides a fallback mechanism allowing the external
 *       service time to recover while we continue serving cached data.</li>
 *   <li><b>Performance Optimization:</b> External network calls can be slow, especially with large datasets.
 *       Redis caching significantly reduces response times and prevents unnecessary API calls, enabling
 *       the system to scale efficiently.</li>
 * </ul>
 *
 * @see io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
 * @see io.github.resilience4j.retry.annotation.Retry
 * @see com.contacts.agenda.service.ContactFallbackService
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResilientContactClient {

    private final KenectLabsClient kenectLabsClient;
    private final ContactFallbackService fallbackService;

    /**
     * Fetches contacts from external API with retry, circuit breaker, and cache.
     *
     * <p><b>Circuit Breaker Behavior:</b></p>
     * <ul>
     *   <li><b>CLOSED:</b> Normal operation - all calls pass through</li>
     *   <li><b>OPENS:</b> When 5 out of 10 recent calls fail (50% threshold)</li>
     *   <li><b>STAYS OPEN:</b> For 30 seconds - all calls go directly to fallback</li>
     *   <li><b>HALF-OPEN:</b> After 30s, allows 3 test calls</li>
     *   <li><b>CLOSES:</b> If all 3 test calls succeed</li>
     * </ul>
     *
     * <p><b>Retry Behavior:</b></p>
     * <ul>
     *   <li>Retries up to 3 times before opening circuit breaker</li>
     *   <li>Waits 1s, 2s, 4s between attempts (exponential backoff)</li>
     *   <li>Adds random jitter (±50%) to prevent thundering herd</li>
     * </ul>
     *
     * <p><b>Cache Strategy:</b></p>
     * <ul>
     *   <li>Stores in Redis for 5 minutes</li>
     *   <li>Key format: "page-pageSize" (e.g., "1-1000")</li>
     * </ul>
     * <p>
     * <strong>⚠️ Configuration:</strong>
     * <blockquote>
     * All resilience parameters (retry attempts, timeouts, thresholds) are externalized in
     * {@code application.yml} under the {@code resilience4j} section. When modifying these
     * values, ensure this documentation is updated accordingly to avoid misleading information.
     * </blockquote>
     */
    @Retry(name = "kenectApi")
    @CircuitBreaker(name = "kenectApi", fallbackMethod = "getContactsFallback")
    @Cacheable(value = "contactPages", key = "#page + '-' + #pageSize")
    public ContactPageResponse getContacts(Long page, Long pageSize) {
        log.debug("Fetching page {} with pageSize {} from external API", page, pageSize);

        ResponseEntity<List<Contact>> response = kenectLabsClient.getContacts(page, pageSize);
        return ContactPageResponse.from(response.getBody(), response.getHeaders());
    }

    /**
     * Fallback method triggered when external API is unavailable.
     * <p><b>Triggered When:</b></p>
     * <ul>
     *   <li>After 3 retry attempts fail</li>
     *   <li>When circuit breaker is OPEN (30 seconds after 50% failures)</li>
     *   <li>When circuit breaker is HALF-OPEN and test call fails</li>
     * </ul>
     * <p>
     * <strong>ℹ️ Infinite Loop Prevention:</strong>
     * <blockquote>
     * The {@code x-fallback: true} header signals to {@link ContactService} that this data
     * came from the fallback mechanism, preventing it from attempting additional API calls
     * that would trigger this fallback again.
     * </blockquote>
     *
     * @throws com.contacts.agenda.exception.ServiceUnavailableException if database is also empty
     * @see ContactService#isFallback(ContactPageResponse)
     */
    private ContactPageResponse getContactsFallback(Long page, Long pageSize, Throwable throwable) {
        log.warn("API call failed, using fallback. Error: {}", throwable.getMessage());

        List<Contact> fallbackContacts = fallbackService.getContactsFromDatabase();

        HttpHeaders fallbackHeaders = new HttpHeaders();
        fallbackHeaders.add("x-fallback", "true");

        return ContactPageResponse.from(fallbackContacts, fallbackHeaders);
    }
}
