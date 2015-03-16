package com.rackspace.idm.api.security;

import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.DomainService;
import com.rackspace.idm.domain.service.TenantService;
import com.rackspace.idm.domain.service.UserService;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.IteratorUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * The request context is a container for thread variables. Spring will automatically create a new one per request, meaning
 * the RequestContext object itself will only exist for the lifetime of the request and it will only be used within the
 * confines of a single thread. Spring provides no guarantee, however, that any objects stored within a RequestContext
 * are single threaded. In fact, in many cases, the objects within will be used across threads (e.g. - various services).
 * The requestContext should be retrieved through the RequestContextHolder, rather than directly in order to provide
 * a standard mechanism for retrieving the requestContext.
 */
@Getter
@Setter
@Component
@Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class RequestContext {

    @Autowired
    private DomainService domainService;

    @Autowired
    private UserService defaultUserService;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private AuthorizationService authorizationService;
    /**
     * Stores information about the user making the request
     */
    private SecurityContext securityContext;

    /**
     * Used for requests targeting a specific user (../users/{userId}/multi-factor/...).
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
            effectiveCaller = defaultUserService.getUserByScopeAccess(token, false);
            getSecurityContext().setEffectiveCaller(effectiveCaller);
        }

        return effectiveCaller;
    }

    /**
     * Helper method to lazily load the effective caller's roles.
     * @return
     */
    public AuthorizationContext getEffectiveCallerAuthorizationContext() {
        AuthorizationContext authorizationContext = getSecurityContext().getEffectiveCallerAuthorizationContext();

        if (authorizationContext == null) {
            List<ImmutableTenantRole> explicitIdentityRoles = new ArrayList<ImmutableTenantRole>();

            BaseUser effectiveCaller = getEffectiveCaller();

            //get all the tenant roles for the user, and limit down to "identity" ones
            List<TenantRole> userTenantRoles = IteratorUtils.toList(tenantService.getTenantRolesForUserNoDetail(effectiveCaller).iterator());
            for (TenantRole userTenantRole : userTenantRoles) {
                ImmutableClientRole identityRole = authorizationService.getCachedIdentityRoleById(userTenantRole.getRoleRsId());
                if (identityRole != null) {
                    //role on user is an "identity" role so add to list of effective identity roles
                    userTenantRole.setName(identityRole.getName());
                    userTenantRole.setDescription(identityRole.getDescription());
                    userTenantRole.setPropagate(identityRole.getPropagate());
                    explicitIdentityRoles.add(new ImmutableTenantRole(userTenantRole));
                }
            }

            authorizationContext = new AuthorizationContext(explicitIdentityRoles);
            getSecurityContext().setEffectiveCallerAuthorizationContext(authorizationContext);
        }

        return authorizationContext;
    }
}
