package com.contacts.agenda.service;

import com.contacts.agenda.exception.ServiceUnavailableException;
import com.contacts.agenda.mapper.ContactMapper;
import com.contacts.agenda.model.Contact;
import com.contacts.agenda.model.ContactEntity;
import com.contacts.agenda.repository.ContactRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static com.contacts.agenda.fixture.ContactFixture.createContact;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ContactFallbackService Tests")
class ContactFallbackServiceTest {

    @Mock
    private ContactRepository contactRepository;

    @InjectMocks
    private ContactFallbackService fallbackService;

    @Nested
    @DisplayName("Get Contacts From Database")
    class GetContactsFromDatabase {

        @Test
        @DisplayName("Should return contacts when database has data")
        void shouldReturnContactsWhenDatabaseHasData() {
            var contact = createContact(1L, "John Doe", "john@example.com");
            var entity = ContactMapper.INSTANCE.toEntity(contact);

            when(contactRepository.findAll()).thenReturn(List.of(entity));

            var result = fallbackService.getContactsFromDatabase();

            assertThat(result)
                    .hasSize(1)
                    .first()
                    .satisfies(c -> {
                        assertThat(c.id()).isEqualTo(1L);
                        assertThat(c.name()).isEqualTo("John Doe");
                        assertThat(c.email()).isEqualTo("john@example.com");
                    });
        }

        @Test
        @DisplayName("Should throw ServiceUnavailableException when database is empty")
        void shouldThrowExceptionWhenDatabaseEmpty() {
            when(contactRepository.findAll()).thenReturn(List.of());

            assertThatThrownBy(() -> fallbackService.getContactsFromDatabase())
                    .isInstanceOf(ServiceUnavailableException.class)
                    .hasMessage("External API is unavailable and no cached data exists");
        }
    }

    @Nested
    @DisplayName("Save Contacts")
    class SaveContacts {

        @Test
        @DisplayName("Should save contacts and return mapped domain objects")
        void shouldSaveContactsAndReturnMapped() {
            var contacts = List.of(
                    createContact(1L, "John Doe", "john@example.com"),
                    createContact(2L, "Jane Smith", "jane@example.com")
            );
            var entities = contacts.stream()
                    .map(ContactMapper.INSTANCE::toEntity)
                    .toList();

            when(contactRepository.saveAll(anyList())).thenReturn(entities);

            var result = fallbackService.saveContacts(contacts);

            assertThat(result)
                    .hasSize(2)
                    .extracting(Contact::name)
                    .containsExactly("John Doe", "Jane Smith");

            verify(contactRepository, times(1)).saveAll(anyList());
        }

        @Test
        @DisplayName("Should handle empty list gracefully")
        void shouldHandleEmptyList() {
            var result = fallbackService.saveContacts(List.of());

            assertThat(result).isEmpty();
            verify(contactRepository, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("Should handle null input gracefully")
        void shouldHandleNullInput() {
            var result = fallbackService.saveContacts(null);

            assertThat(result).isEmpty();
            verify(contactRepository, never()).saveAll(anyList());
        }
    }
}
