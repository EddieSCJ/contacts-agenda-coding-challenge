package com.contacts.agenda.client;

import com.contacts.agenda.model.Contact;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpHeaders;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContactPageResponse implements Serializable {

    private List<Contact> contacts;
    private Map<String, String> headers;

    public static ContactPageResponse from(List<Contact> contacts, HttpHeaders httpHeaders) {
        Map<String, String> headerMap = new HashMap<>();
        httpHeaders.forEach((key, values) -> {
            if (!values.isEmpty()) {
                headerMap.put(key.toLowerCase(), values.getFirst());
            }
        });
        return new ContactPageResponse(contacts, headerMap);
    }
}
