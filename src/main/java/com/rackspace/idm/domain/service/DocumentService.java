package com.rackspace.idm.domain.service;

public interface DocumentService {
    String getMFALockedOutEmail();
    String getMFAEnabledEmail();
    String getMFADisabledEmail();
}
