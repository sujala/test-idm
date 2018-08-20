package com.rackspace.idm.modules.usergroups.api.resource;

import com.rackspace.idm.api.resource.cloud.v20.PaginationParams;
import com.rackspace.idm.domain.entity.User.UserType;
import lombok.Getter;

import javax.annotation.Nullable;

/**
 * Consolidates the various search params allowed for the list user group members service.
 */
@Getter
public class UserSearchCriteria {

    @Nullable
    private PaginationParams paginationRequest;

    @Nullable
    private UserType userType;

    public UserSearchCriteria() {
    }

    public UserSearchCriteria(PaginationParams paginationRequest) {
        this.paginationRequest = paginationRequest;
    }

    public UserType getUserType() {
        return null == userType ? userType.VERIFIED : userType;
    }

    public void setUserType(UserType userType) {
        this.userType = userType;
    }
}
