package com.rackspace.idm.modules.usergroups.api.resource;

import com.rackspace.idm.ErrorCodes;
import com.rackspace.idm.api.security.IdentityRole;
import com.rackspace.idm.api.security.RequestContextHolder;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.entity.BaseUser;
import com.rackspace.idm.domain.entity.Domain;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.DomainService;
import com.rackspace.idm.domain.service.IdentityUserTypeEnum;
import com.rackspace.idm.exception.ForbiddenException;
import com.rackspace.idm.modules.usergroups.Constants;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

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

    @Autowired
    private IdentityConfig identityConfig;

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
        verifyAreUserGroupsEnabledForDomain(targetDomainId);
    }

    @Override
    public void verifyAreUserGroupsEnabledForDomain(String domainId) {
        if (!areUserGroupsEnabledForDomain(domainId)) {
            throw new ForbiddenException(String.format(Constants.ERROR_CODE_USER_GROUPS_NOT_ENABLED_FOR_DOMAIN_MSG_PATTERN, domainId), ErrorCodes.ERROR_CODE_FORBIDDEN_ACTION);
        }
    }

    @Override
    public boolean areUserGroupsEnabledForDomain(String domainId) {
        return identityConfig.getReloadableConfig().areUserGroupsGloballyEnabled()
                || areUserGroupsExplicitlyEnabledForDomain(domainId);
    }

    private boolean areUserGroupsExplicitlyEnabledForDomain(String domainId) {
        List<String> enabledDomainIds = identityConfig.getRepositoryConfig().getExplicitUserGroupEnabledDomains();

        if (CollectionUtils.isNotEmpty(enabledDomainIds)) {
            for (String enabledDomainId : enabledDomainIds) {
                // DomainIds are case insensitive so can't just use list contains
                if (enabledDomainId.equalsIgnoreCase(domainId)) {
                    return true;
                }
            }
        }
        return false;
    }

}
