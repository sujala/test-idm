package com.rackspace.idm.domain.service;

import com.rackspace.idm.domain.entity.SecretQA;
import com.rackspace.idm.domain.entity.SecretQAs;

import java.util.List;

public interface SecretQAService {
    void addSecretQA(String userId, SecretQA secretQA);
    SecretQAs getSecretQAs(String userId);
}
