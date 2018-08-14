package com.rackspace.idm.api.security;

import com.rackspace.idm.ErrorCodes;
import com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.domain.service.impl.DefaultAuthorizationService;
import com.rackspace.idm.exception.ForbiddenException;
import com.rackspace.idm.exception.NotFoundException;
import com.sun.jersey.spi.container.ContainerRequest;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger logger = LoggerFactory.getLogger(RequestContext.class);

    @Autowired
    private DomainService domainService;

    @Autowired
    private UserService defaultUserService;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private AuthorizationService authorizationService;

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private IdentityConfig identityConfig;

    /**
     * Stores information about the user making the request
     */
    private SecurityContext securityContext;

    /**
     * The request (should make immutable)
     */
    private ContainerRequest containerRequest;

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
            domain = effectiveCaller != null && StringUtils.isNotBlank(effectiveCaller.getDomainId()) ? domainService.getDomain(effectiveCaller.getDomainId()) : null;
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
     * Helper method to retrieve the effective caller and verify the caller and the caller's domain are enabled. This
     * call mirrors the {@link com.rackspace.idm.domain.service.impl.DefaultUserService#getUserByScopeAccess(ScopeAccess)}
     * which loads the user from a token and verifies the user is enabled.
     *
     * The NotFoundException message also mirrors the call.
     *
     * @return
     * @throws com.rackspace.idm.exception.NotFoundException if the effective caller is disabled or the callers domain is disabled
     */
    public BaseUser getAndVerifyEffectiveCallerIsEnabled() {
        BaseUser effectiveCaller = getEffectiveCaller();
        if (effectiveCaller == null || effectiveCaller.isDisabled() || (getEffectiveCallerDomain() != null && !getEffectiveCallerDomain().getEnabled())) {
            throw new NotFoundException("Token not found.");
        }
        return effectiveCaller;
    }

    /**
     * Helper method to retrieve the effective caller's user type
     *
     * @return
     */
    public IdentityUserTypeEnum getEffectiveCallersUserType() {
        BaseUser effectiveCaller = getEffectiveCaller();

        return effectiveCaller == null ? null : authorizationService.getIdentityTypeRoleAsEnum(effectiveCaller);
    }

    /**
     * Helper method to lazily load the effective caller's roles.
     * @return
     */
    public AuthorizationContext getEffectiveCallerAuthorizationContext() {
        AuthorizationContext authorizationContext = getSecurityContext().getEffectiveCallerAuthorizationContext();

        if (authorizationContext == null) {
            List<ImmutableTenantRole> explicitIdentityRoles = new ArrayList<ImmutableTenantRole>();
            List<ImmutableClientRole> implicitIdentityRoles = new ArrayList<ImmutableClientRole>();

            BaseUser effectiveCaller = getEffectiveCaller();

            List<TenantRole> userTenantRoles;
            if (effectiveCaller instanceof EndUser) {
                if (StringUtils.isBlank(effectiveCaller.getDomainId())) {
                    //TODO: Push this validation higher since not all services use this method yet.
                    // All end users MUST belong to a domain or it's considered invalid data. Log specific error code
                    // to identify this invalid data scenario.
                    logger.error(String.format("Attempted to retrieve roles for user '%s', but user has no domain!", effectiveCaller.getDomainId()));
                    throw new ForbiddenException(DefaultAuthorizationService.NOT_AUTHORIZED_MSG, ErrorCodes.ERROR_CODE_INVALID_DOMAIN_FOR_USER);
                }
                userTenantRoles = tenantService.getSourcedRoleAssignmentsForUser((EndUser) effectiveCaller).asTenantRoles();
            } else {
                userTenantRoles = IteratorUtils.toList(tenantService.getTenantRolesForUserNoDetail(effectiveCaller).iterator());
            }
            //get all the tenant roles for the user
            for (TenantRole userTenantRole : userTenantRoles) {
                ImmutableClientRole identityRole = applicationService.getCachedClientRoleById(userTenantRole.getRoleRsId());

                if (identityRole != null) {
                    //role on user is an "identity" role so add to list of effective identity roles
                    userTenantRole.setName(identityRole.getName());
                    userTenantRole.setDescription(identityRole.getDescription());
                    userTenantRole.setRoleType(identityRole.getRoleType());
                    explicitIdentityRoles.add(new ImmutableTenantRole(userTenantRole));

                    //get the "implicit" roles associated with the client role, if any. Implicit roles are such that if user has Role X, they implicitly
                    //have Role A, B, C even though the user is not explicitly assigned those roles (and these roles wouldn't show up in list
                    //of roles the user has.
                    List<ImmutableClientRole> implicitRoles = authorizationService.getImplicitRolesForRole(identityRole.getName());
                    implicitIdentityRoles.addAll(implicitRoles);
                }
            }

            authorizationContext = new AuthorizationContext(explicitIdentityRoles, implicitIdentityRoles);
            getSecurityContext().setEffectiveCallerAuthorizationContext(authorizationContext);
        }

        return authorizationContext;
    }
}
