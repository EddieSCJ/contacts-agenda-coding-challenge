package com.contacts.agenda.controller;

import com.contacts.agenda.model.Contact;
import com.contacts.agenda.service.ContactService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.contacts.agenda.fixture.ContactFixture.aContact;
import static com.contacts.agenda.fixture.ContactFixture.createContact;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ContactController.class)
@DisplayName("Contact Controller Integration Tests")
class ContactControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ContactService contactService;

    @Nested
    @DisplayName("GET /contacts")
    class GetAllContacts {

        @Test
        @DisplayName("Should return all contacts when service returns multiple contacts")
        void shouldReturnAllContactsWhenServiceReturnsMultipleContacts() throws Exception {
            Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
            List<Contact> expectedContacts = Arrays.asList(
                    createContact(1L, "John Doe", "john.doe@example.com", "KENECT_LABS", now, now),
                    createContact(2L, "Jane Smith", "jane.smith@example.com", "KENECT_LABS", now, now),
                    createContact(3L, "Bob Johnson", "bob.johnson@example.com", "KENECT_LABS", now, now)
            );

            when(contactService.getAllContacts()).thenReturn(expectedContacts);

            mockMvc.perform(get("/contacts")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(3)))
                    .andExpect(jsonPath("$[0].id", is(1)))
                    .andExpect(jsonPath("$[0].name", is("John Doe")))
                    .andExpect(jsonPath("$[0].email", is("john.doe@example.com")))
                    .andExpect(jsonPath("$[0].source", is("KENECT_LABS")))
                    .andExpect(jsonPath("$[1].id", is(2)))
                    .andExpect(jsonPath("$[1].name", is("Jane Smith")))
                    .andExpect(jsonPath("$[1].email", is("jane.smith@example.com")))
                    .andExpect(jsonPath("$[2].id", is(3)))
                    .andExpect(jsonPath("$[2].name", is("Bob Johnson")))
                    .andExpect(jsonPath("$[2].email", is("bob.johnson@example.com")));
        }

        @Test
        @DisplayName("Should return empty array when service returns no contacts")
        void shouldReturnEmptyArrayWhenServiceReturnsNoContacts() throws Exception {
            when(contactService.getAllContacts()).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/contacts")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(0)))
                    .andExpect(jsonPath("$", is(empty())));
        }

        @Test
        @DisplayName("Should return single contact when service returns one contact")
        void shouldReturnSingleContactWhenServiceReturnsOneContact() throws Exception {
            Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
            Contact expectedContact = aContact()
                    .id(42L)
                    .name("Alice Wonder")
                    .email("alice.wonder@example.com")
                    .source("KENECT_LABS")
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            when(contactService.getAllContacts()).thenReturn(List.of(expectedContact));

            mockMvc.perform(get("/contacts"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].id", is(42)))
                    .andExpect(jsonPath("$[0].name", is("Alice Wonder")))
                    .andExpect(jsonPath("$[0].email", is("alice.wonder@example.com")))
                    .andExpect(jsonPath("$[0].source", is("KENECT_LABS")))
                    .andExpect(jsonPath("$[0].createdAt", is(now.toString())))
                    .andExpect(jsonPath("$[0].updatedAt", is(now.toString())));
        }

        @Test
        @DisplayName("Should handle large number of contacts")
        void shouldHandleLargeNumberOfContacts() throws Exception {
            Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
            List<Contact> largeContactList = Arrays.asList(
                    createContact(1L, "Contact 1", "contact1@example.com", "KENECT_LABS", now, now),
                    createContact(2L, "Contact 2", "contact2@example.com", "KENECT_LABS", now, now),
                    createContact(3L, "Contact 3", "contact3@example.com", "KENECT_LABS", now, now),
                    createContact(4L, "Contact 4", "contact4@example.com", "KENECT_LABS", now, now),
                    createContact(5L, "Contact 5", "contact5@example.com", "KENECT_LABS", now, now)
            );

            when(contactService.getAllContacts()).thenReturn(largeContactList);

            mockMvc.perform(get("/contacts"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(5)))
                    .andExpect(jsonPath("$[0].id", is(1)))
                    .andExpect(jsonPath("$[4].id", is(5)))
                    .andExpect(jsonPath("$[*].source", everyItem(is("KENECT_LABS"))));
        }

        @Test
        @DisplayName("Should handle contacts with different timestamps")
        void shouldHandleContactsWithDifferentTimestamps() throws Exception {
            Instant past = Instant.now().minus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MILLIS);
            Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
            Instant future = Instant.now().plus(1, ChronoUnit.HOURS).truncatedTo(ChronoUnit.MILLIS);

            List<Contact> contactsWithDifferentTimes = Arrays.asList(
                    createContact(1L, "Past Contact", "past@example.com", "KENECT_LABS", past, past),
                    createContact(2L, "Present Contact", "present@example.com", "KENECT_LABS", now, now),
                    createContact(3L, "Future Contact", "future@example.com", "KENECT_LABS", past, future)
            );

            when(contactService.getAllContacts()).thenReturn(contactsWithDifferentTimes);

            mockMvc.perform(get("/contacts"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(3)))
                    .andExpect(jsonPath("$[0].createdAt", is(past.toString())))
                    .andExpect(jsonPath("$[0].updatedAt", is(past.toString())))
                    .andExpect(jsonPath("$[1].createdAt", is(now.toString())))
                    .andExpect(jsonPath("$[1].updatedAt", is(now.toString())))
                    .andExpect(jsonPath("$[2].createdAt", is(past.toString())))
                    .andExpect(jsonPath("$[2].updatedAt", is(future.toString())));
        }

        @Test
        @DisplayName("Should handle contacts with special characters in names and emails")
        void shouldHandleContactsWithSpecialCharacters() throws Exception {
            Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
            List<Contact> specialContacts = Arrays.asList(
                    createContact(1L, "José María", "jose.maria@domínio.com", "KENECT_LABS", now, now),
                    createContact(2L, "O'Connor", "o'connor@example.com", "KENECT_LABS", now, now),
                    createContact(3L, "Smith-Johnson", "smith-johnson@test.co.uk", "KENECT_LABS", now, now)
            );

            when(contactService.getAllContacts()).thenReturn(specialContacts);

            mockMvc.perform(get("/contacts"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(3)))
                    .andExpect(jsonPath("$[0].name", is("José María")))
                    .andExpect(jsonPath("$[0].email", is("jose.maria@domínio.com")))
                    .andExpect(jsonPath("$[1].name", is("O'Connor")))
                    .andExpect(jsonPath("$[1].email", is("o'connor@example.com")))
                    .andExpect(jsonPath("$[2].name", is("Smith-Johnson")))
                    .andExpect(jsonPath("$[2].email", is("smith-johnson@test.co.uk")));
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandling {

        @Test
        @DisplayName("Should handle service exceptions gracefully")
        void shouldHandleServiceExceptionsGracefully() throws Exception {
            when(contactService.getAllContacts()).thenThrow(new RuntimeException("External API unavailable"));

            mockMvc.perform(get("/contacts"))
                    .andDo(print())
                    .andExpect(status().is5xxServerError());
        }
    }

    @Nested
    @DisplayName("HTTP Headers and Content Type")
    class HttpHeadersAndContentType {

        @Test
        @DisplayName("Should return correct content type header")
        void shouldReturnCorrectContentTypeHeader() throws Exception {
            when(contactService.getAllContacts()).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/contacts"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Type", "application/json"))
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }

        @Test
        @DisplayName("Should accept requests without specific Accept header")
        void shouldAcceptRequestsWithoutSpecificAcceptHeader() throws Exception {
            when(contactService.getAllContacts()).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/contacts"))
                    .andDo(print())
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should accept requests with JSON Accept header")
        void shouldAcceptRequestsWithJsonAcceptHeader() throws Exception {
            when(contactService.getAllContacts()).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/contacts")
                            .accept(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }
    }
}
