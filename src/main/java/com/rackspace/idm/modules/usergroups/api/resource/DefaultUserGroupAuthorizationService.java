package com.rackspace.idm.modules.usergroups.api.resource;

import com.rackspace.idm.api.security.IdentityRole;
import com.rackspace.idm.api.security.RequestContextHolder;
import com.rackspace.idm.domain.entity.BaseUser;
import com.rackspace.idm.domain.entity.Domain;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.DomainService;
import com.rackspace.idm.domain.service.IdentityUserTypeEnum;
import com.rackspace.idm.exception.ForbiddenException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service.NOT_AUTHORIZED;

/**
 * Makes authorization decisions on whether the caller (defined by that provided in RequestContext) is able to
 * perform user group actions on the specified domain.
 *
 */
@Component
public class DefaultUserGroupAuthorizationService implements UserGroupAuthorizationService {
    @Autowired
    private RequestContextHolder requestContextHolder;

    @Autowired
    private AuthorizationService authorizationService;

    @Autowired
    private DomainService domainService;

    /**
     * This method verifies the caller (defined by that provided in RequestContext) can "manage" the user groups within
     * the specified domain according to the following rules
     *
     * <ol>
     *     <li>identity:service-admins and identity:admins can manage groups for any domain</li>
     *     <li>identity:user-admin and identity:user-manage can manage groups for their own domain</li>
     *     <li>rcn:admin can manage groups for any domain within their RCN</li>
     * </ol>
     *
     * @param targetDomainId
     */
    @Override
    public void verifyEffectiveCallerHasManagementAccessToDomain(String targetDomainId) {
        // Verify domain exists
        Domain targetDomain = domainService.checkAndGetDomain(targetDomainId);
        BaseUser caller = requestContextHolder.getRequestContext().getEffectiveCaller();

        // Verify user has one of necessary roles
        authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccessOrRole(IdentityUserTypeEnum.USER_MANAGER, IdentityRole.RCN_ADMIN.getRoleName());

        // If domain based identity role, must verify user has access to domain
        IdentityUserTypeEnum userType = requestContextHolder.getRequestContext().getEffectiveCallersUserType();

        if (userType == null) {
            // If we don't know the type of user, we can't authorize the user for anything
            throw new ForbiddenException(NOT_AUTHORIZED);
        } else if (userType.isDomainBasedAccessLevel()) {
            // Only need test when the user's domain is different than the target domain.
            if (!caller.getDomainId().equalsIgnoreCase(targetDomain.getDomainId())) {
                boolean isRcnAdmin = authorizationService.authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(IdentityRole.RCN_ADMIN.getRoleName());
                if (isRcnAdmin) {
                    // Compare the RCNs of the two domains. If same, rcn admin can manage
                    Domain userDomain = domainService.getDomain(caller.getDomainId());
                    if (userDomain == null
                            || userDomain.getRackspaceCustomerNumber() == null
                            || targetDomain.getRackspaceCustomerNumber() == null
                            || !userDomain.getRackspaceCustomerNumber().equalsIgnoreCase(targetDomain.getRackspaceCustomerNumber())) {
                        throw new ForbiddenException(NOT_AUTHORIZED);
                    }
                } else {
                    // Only RCN admins can manage across domains
                    throw new ForbiddenException(NOT_AUTHORIZED);
                }
            }
        }
    }
}
