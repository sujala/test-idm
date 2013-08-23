package com.rackspace.idm.domain.service;

import com.rackspace.idm.domain.entity.SecretQA;
import com.rackspace.idm.domain.entity.SecretQAs;

import javax.xml.bind.JAXBException;
import java.io.IOException;

public interface SecretQAService {
    void addSecretQA(String userId, SecretQA secretQA) throws IOException, JAXBException;
    SecretQAs getSecretQAs(String userId);
}
