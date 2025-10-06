package com.contacts.agenda.client;

import com.contacts.agenda.model.Contact;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.util.List;

@HttpExchange("/api/v1/contacts")
public interface KenectLabsClient {

    @GetExchange
    ResponseEntity<List<Contact>> getContacts(
            @RequestParam(required = false) Long page,
            @RequestParam(required = false) Long pageSize
    );
}
