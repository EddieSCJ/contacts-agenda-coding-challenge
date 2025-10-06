package com.contacts.agenda.service;

import com.contacts.agenda.client.ContactPageResponse;
import com.contacts.agenda.model.Contact;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * <b>Optimization Strategy:</b>
 * <ul>
 *   <li><b>First Request:</b> Fetches up to 1000 contacts (default-page-size) from the external API</li>
 *   <li><b>If total ≤ 1000:</b> Returns immediately (1 API call - optimized case)</li>
 *   <li><b>If total > 1000:</b> Makes second request for remaining contacts (2 API calls total)</li>
 *   <li><b>Benefit:</b> Avoids network overloading and rate limits while minimizing API calls</li>
 * </ul>
 * <p>
 * <b>Example Scenarios:</b>
 * <ul>
 *   <li>500 contacts → 1 API call (fetches all 500 immediately)</li>
 *   <li>1000 contacts → 1 API call (fetches all 1000 immediately)</li>
 *   <li>5000 contacts → 2 API calls (1st: 1000 contacts, 2nd: remaining 4000)</li>
 * </ul>
 *
 * @see ResilientContactClient for resilience features (retry, circuit breaker, cache)
 * @see ContactFallbackService for database persistence and fallback mechanism
 */
@Service
@Slf4j
public class ContactService {

    private final ResilientContactClient contactClient;
    private final ContactFallbackService fallbackService;
    private final Long defaultPageSize;

    public ContactService(
            ResilientContactClient contactClient,
            ContactFallbackService fallbackService,
            @Value("${kenect.api.default-page-size}") Long defaultPageSize
    ) {
        this.contactClient = contactClient;
        this.fallbackService = fallbackService;
        this.defaultPageSize = defaultPageSize;
    }

    /**
     * <b>⚠️Performance Optimization Opportunity⚠️</b>
     * <br>
     * Database writes are performed on every successful response, including cache hits from Redis.
     * This creates unnecessary MongoDB writes since cached data was already persisted from previous
     * API calls.
     * <br><br>
     * Future enhancement should detect cache hits and skip database persistence to
     * reduce database load and improve performance for frequently accessed data.
     */
    public List<Contact> getAllContacts() {
        ContactPageResponse firstPage = contactClient.getContacts(1L, defaultPageSize);

        if (isFallback(firstPage)) {
            log.warn("Using database fallback since external api is unavailable");
            return firstPage.getContacts();
        }

        Long totalCount = getTotalCount(firstPage);
        if (totalCount <= defaultPageSize) {
            log.debug("Fetched all {} contacts in single request", totalCount);
            return fallbackService.saveContacts(firstPage.getContacts());
        }

        final var allContacts = fetchRemainingContacts(firstPage, totalCount);
        return fallbackService.saveContacts(allContacts);
    }

    private boolean isFallback(ContactPageResponse response) {
        return "true".equals(response.getHeaders().get("x-fallback"));
    }

    private Long getTotalCount(ContactPageResponse response) {
        return Long.parseLong(response.getHeaders().get("total-count"));
    }

    private List<Contact> fetchRemainingContacts(ContactPageResponse firstPage, Long totalCount) {
        Long remainingContacts = totalCount - defaultPageSize;
        log.debug("Fetching remaining {} of {} total contacts", remainingContacts, totalCount);

        ContactPageResponse secondPage = contactClient.getContacts(2L, remainingContacts);

        List<Contact> allContacts = new ArrayList<>(firstPage.getContacts());
        allContacts.addAll(secondPage.getContacts());

        return allContacts;
    }
}
