package com.contacts.agenda.mapper;

import com.contacts.agenda.model.Contact;
import com.contacts.agenda.model.ContactEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface ContactMapper {

    ContactMapper INSTANCE = Mappers.getMapper(ContactMapper.class);

    @Mapping(target = "syncedAt", expression = "java(java.time.Instant.now())")
    ContactEntity toEntity(Contact contact);

    Contact toDomain(ContactEntity entity);
}
