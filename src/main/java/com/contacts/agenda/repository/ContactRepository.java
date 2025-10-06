package com.contacts.agenda.repository;

import com.contacts.agenda.model.ContactEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContactRepository extends MongoRepository<ContactEntity, Long> {}


