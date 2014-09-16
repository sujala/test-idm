package com.rackspace.idm.api.security;

import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.DomainService;
import com.rackspace.idm.domain.service.UserService;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class RequestContext {

    @Autowired
    private DomainService domainService;

    @Autowired
    private UserService defaultUserService;

    /**
     * Stores information about the user making the request
     */
    private SecurityContext securityContext;

    /**
     * Used for requests targetting a specific user (../users/{userId}/multi-factor/...).
     * TODO:// Refactor these request type specific requests into a separate object with encapsulation
     * so this class doesn't balloon with tons of request specific properties (e.g. - domain, users, tenants).
     */
    private EndUser targetEndUser;

    /**
     * Helper method to lazily load the effective caller's Domain.
     *
     * @return
     */
    public Domain getEffectiveCallerDomain() {
        Domain domain = getSecurityContext().getEffectiveCallerDomain();

        if (domain == null) {
            BaseUser effectiveCaller = getEffectiveCaller();
            domain = effectiveCaller != null ? domainService.getDomain(effectiveCaller.getDomainId()) : null;
            securityContext.setEffectiveCallerDomain(domain);
        }

        return domain;
    }

    /**
     * Helper method to lazily load the effective caller.
     *
     * @return
     */
    public BaseUser getEffectiveCaller() {
        BaseUser effectiveCaller = getSecurityContext().getEffectiveCaller();

        if (effectiveCaller == null) {
            ScopeAccess token = getSecurityContext().getEffectiveCallerToken();
            if (token == null) {
                throw new IllegalStateException("Effective caller token not set in security context. Can not retrieve caller.");
            }

            //get the user associated with the token. Do NOT verify that the user is enabled and domain is enabled.
            BaseUser endUser = defaultUserService.getUserByScopeAccess(token, false);
            getSecurityContext().setEffectiveCaller(endUser);
        }

        return effectiveCaller;
    }
}
