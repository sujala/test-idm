package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.dao.ApiDocDao;
import com.rackspace.idm.domain.service.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DefaultDocumentService implements DocumentService {

    @Autowired
    ApiDocDao apiDocDao;

    @Override
    public String getMFALockedOutEmail() {
        return apiDocDao.getContent("/email_mfa_lockedout.txt");
    }

    @Override
    public String getMFAEnabledEmail() {
        return apiDocDao.getContent("/email_mfa_enabled.txt");
    }

    @Override
    public String getMFADisabledEmail() {
        return apiDocDao.getContent("/email_mfa_disabled.txt");
    }
}
