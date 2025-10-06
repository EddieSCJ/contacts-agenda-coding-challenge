package com.contacts.agenda.controller;

import com.contacts.agenda.model.Contact;
import com.contacts.agenda.service.ContactService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.contacts.agenda.controller.ControllerDoc.Contacts.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = TAG_NAME, description = TAG_DESCRIPTION)
public class ContactController {

    private final ContactService contactService;

    /**
     * Retrieves all contacts from the external API with automatic fallback.
     * <p>
     * This endpoint provides high availability and performance through multiple layers of resilience
     * and optimization strategies implemented in the service layer.
     * <p>
     * <strong>⚠️ Production Consideration:</strong>
     * <blockquote>
     * Pagination should be implemented to prevent potential DoS attacks and performance issues
     * with large datasets. Currently returns all contacts since it's a requirement.
     * </blockquote>
     */
    @Operation(summary = GET_ALL_SUMMARY, description = GET_ALL_DESCRIPTION)
    @ApiResponse(
            responseCode = "200",
            description = RESPONSE_200_DESCRIPTION,
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = Contact.class))
    )
    @GetMapping("/contacts")
    public List<Contact> getAllContacts() {
        return contactService.getAllContacts();
    }
}
