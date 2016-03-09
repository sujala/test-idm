package com.rackspace.idm.domain.service;

/**
 * @deprecated Should use the velocity template for emails
 */
@Deprecated
public interface DocumentService {
    String getMFALockedOutEmail();
    String getMFAEnabledEmail();
    String getMFADisabledEmail();
}
