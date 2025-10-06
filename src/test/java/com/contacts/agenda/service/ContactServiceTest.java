package com.contacts.agenda.service;

import com.contacts.agenda.client.ContactPageResponse;
import com.contacts.agenda.model.Contact;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;

import java.util.List;

import static com.contacts.agenda.fixture.ContactFixture.createContact;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ContactService Tests")
class ContactServiceTest {

    @Mock
    private ResilientContactClient contactClient;

    @Mock
    private ContactFallbackService fallbackService;

    @InjectMocks
    private ContactService contactService;

    private final Long defaultPageSize = 2L;

    @BeforeEach
    void setUp() {
        contactService = new ContactService(contactClient, fallbackService, defaultPageSize);
    }

    @Nested
    @DisplayName("Get All Contacts")
    class GetAllContacts {

        @Test
        @DisplayName("Should fetch all contacts in single request when total ≤ defaultPageSize")
        void shouldFetchAllContactsInSingleRequest() {
            var contacts = List.of(
                    createContact(1L, "John Doe", "john@example.com"),
                    createContact(2L, "Jane Smith", "jane@example.com")
            );

            HttpHeaders headers = new HttpHeaders();
            headers.add("total-count", "2");
            var firstPage = ContactPageResponse.from(contacts, headers);

            when(contactClient.getContacts(1L, defaultPageSize)).thenReturn(firstPage);
            when(fallbackService.saveContacts(anyList())).thenAnswer(i -> i.getArgument(0));

            var result = contactService.getAllContacts();

            assertThat(result).hasSize(2)
                    .extracting(Contact::name)
                    .containsExactly("John Doe", "Jane Smith");

            verify(contactClient).getContacts(1L, defaultPageSize);
            verify(contactClient, never()).getContacts(eq(2L), any());
            verify(fallbackService).saveContacts(contacts);
        }

        @Test
        @DisplayName("Should fetch contacts in two requests when total > defaultPageSize")
        void shouldFetchContactsInTwoRequests() {
            var firstPageContacts = List.of(
                    createContact(1L, "Contact 1", "contact1@example.com"),
                    createContact(2L, "Contact 2", "contact2@example.com")
            );
            var secondPageContacts = List.of(
                    createContact(3L, "Contact 3", "contact3@example.com"),
                    createContact(4L, "Contact 4", "contact4@example.com")
            );

            HttpHeaders firstHeaders = new HttpHeaders();
            firstHeaders.add("total-count", "4");
            var firstPage = ContactPageResponse.from(firstPageContacts, firstHeaders);

            var secondPage = ContactPageResponse.from(secondPageContacts, new HttpHeaders());

            when(contactClient.getContacts(1L, defaultPageSize)).thenReturn(firstPage);
            when(contactClient.getContacts(2L, 2L)).thenReturn(secondPage);
            when(fallbackService.saveContacts(anyList())).thenAnswer(i -> i.getArgument(0));

            var result = contactService.getAllContacts();

            assertThat(result).hasSize(4)
                    .extracting(Contact::name)
                    .containsExactly("Contact 1", "Contact 2", "Contact 3", "Contact 4");

            verify(contactClient).getContacts(1L, defaultPageSize);
            verify(contactClient).getContacts(2L, 2L);
            verify(fallbackService).saveContacts(result);
        }

        @Test
        @DisplayName("Should handle fallback headers result due to external API being unavailable")
        void shouldUseFallbackWhenApiUnavailable() {
            var fallbackContacts = List.of(
                    createContact(1L, "Fallback Contact", "fallback@example.com")
            );

            HttpHeaders headers = new HttpHeaders();
            headers.add("x-fallback", "true");
            var fallbackPage = ContactPageResponse.from(fallbackContacts, headers);

            when(contactClient.getContacts(1L, defaultPageSize)).thenReturn(fallbackPage);

            var result = contactService.getAllContacts();

            assertThat(result).hasSize(1)
                    .extracting(Contact::name)
                    .containsExactly("Fallback Contact");

            verify(contactClient).getContacts(1L, defaultPageSize);

            verify(contactClient, never()).getContacts(eq(2L), any());
            verify(fallbackService, never()).saveContacts(anyList());
        }

        @Test
        @DisplayName("Should handle empty response")
        void shouldHandleEmptyResponse() {
            HttpHeaders headers = new HttpHeaders();
            headers.add("total-count", "0");
            var emptyPage = ContactPageResponse.from(List.of(), headers);

            when(contactClient.getContacts(1L, defaultPageSize)).thenReturn(emptyPage);
            when(fallbackService.saveContacts(anyList())).thenAnswer(i -> i.getArgument(0));

            var result = contactService.getAllContacts();

            assertThat(result).isEmpty();
            verify(contactClient).getContacts(1L, defaultPageSize);
            verify(contactClient, never()).getContacts(eq(2L), any());
            verify(fallbackService).saveContacts(List.of());
        }
    }
}
