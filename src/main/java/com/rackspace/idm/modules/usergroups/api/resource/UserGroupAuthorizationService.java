package com.rackspace.idm.modules.usergroups.api.resource;

public interface UserGroupAuthorizationService {
    void verifyEffectiveCallerHasManagementAccessToDomain(String targetDomainId);

    /**
     * Given a domain id, returns whether or not user groups are supported for that domain.
     *
     * Support means that user groups can be created and managed against that domain and a user's roles will take in to
     * account the roles the user receives via group membership.
     *
     * @param domainId
     */
    boolean areUserGroupsEnabledForDomain(String domainId);
}
