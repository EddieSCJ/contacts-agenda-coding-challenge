package com.contacts.agenda.service;

import com.contacts.agenda.model.Contact;
import com.contacts.agenda.model.ContactEntity;
import com.contacts.agenda.exception.ServiceUnavailableException;
import com.contacts.agenda.mapper.ContactMapper;
import com.contacts.agenda.repository.ContactRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service responsible for database persistence and fallback data retrieval.
 * <p>
 * Acts as a backup data source when the external API is unavailable. Contacts are
 * automatically saved to database after successful API calls, ensuring data availability
 * during outages.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ContactFallbackService {

    private final ContactRepository contactRepository;

    /**
     * Retrieves all contacts from fallback database.
     * <p>
     * This method is called by the circuit breaker fallback when the external API is unavailable.
     * It returns the last successful dataset saved to database.
     *
     * @throws ServiceUnavailableException if database is empty (no previous successful sync)
     */
    public List<Contact> getContactsFromDatabase() {
        log.debug("Fetching contacts from fallback database");

        List<ContactEntity> entities = contactRepository.findAll();

        if (entities.isEmpty()) {
            log.error("Critical: fallback databaase is empty and external API is unavailable");
            throw new ServiceUnavailableException("External API is unavailable and no cached data exists");
        }

        log.debug("Retrieved {} contacts from database", entities.size());
        return entities.stream()
                .map(ContactMapper.INSTANCE::toDomain)
                .toList();
    }

    public List<Contact> saveContacts(List<Contact> contacts) {
        if (contacts == null || contacts.isEmpty()) {
            log.debug("Skipping save - empty contacts list");
            return List.of();
        }

        log.debug("Saving {} contacts to database", contacts.size());
        List<ContactEntity> entities = contacts.stream()
                .map(ContactMapper.INSTANCE::toEntity)
                .toList();

        return contactRepository.saveAll(entities)
                .stream().map(ContactMapper.INSTANCE::toDomain)
                .toList();
    }
}
