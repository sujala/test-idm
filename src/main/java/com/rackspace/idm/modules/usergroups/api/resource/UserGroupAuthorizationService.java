package com.rackspace.idm.modules.usergroups.api.resource;

public interface UserGroupAuthorizationService {
    void verifyEffectiveCallerHasManagementAccessToDomain(String targetDomainId);

}
