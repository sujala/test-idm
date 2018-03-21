package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.DelegationAgreement;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.PrincipalType;
import com.rackspace.idm.ErrorCodes;
import com.rackspace.idm.GlobalConstants;
import com.rackspace.idm.api.converter.cloudv20.DelegationAgreementConverter;
import com.rackspace.idm.api.resource.IdmPathUtils;
import com.rackspace.idm.api.security.RequestContextHolder;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.ExceptionHandler;
import com.rackspace.idm.exception.ForbiddenException;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.modules.usergroups.entity.UserGroup;
import com.rackspace.idm.modules.usergroups.service.UserGroupService;
import lombok.Getter;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

/**
 * Implementation of the delegation API services. Implementation is greatly simplified and standardized by combining
 * actual business logic into common services regardless of principal or delegate type.
 */
@Component
public class DefaultDelegationCloudService implements DelegationCloudService {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultDelegationCloudService.class);

    public static final String ERROR_MSG_PRINCIPAL_NOT_FOUND = "The specified principal was not found or you are not authorized to use this principal";

    @Autowired
    private IdentityConfig identityConfig;

    @Autowired
    private RequestContextHolder requestContextHolder;

    @Autowired
    private AuthorizationService authorizationService;

    @Autowired
    private DelegationService delegationService;

    @Autowired
    private UserGroupService userGroupService;

    @Autowired
    private ExceptionHandler exceptionHandler;

    @Autowired
    private IdentityUserService identityUserService;

    @Autowired
    private DomainService domainService;

    @Autowired
    private IdmPathUtils idmPathUtils;

    @Autowired
    private DelegationAgreementConverter delegationAgreementConverter;

    @Override
    public Response addAgreement(UriInfo uriInfo, String authToken, DelegationAgreement agreementWeb) {
        try {
            // Verify token exists and valid
            BaseUserToken token = requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerTokenAsBaseToken(authToken);

            // Verify token is not a scoped token or delegation token
            if (!StringUtils.isBlank(token.getScope()) || token.isDelegationToken()) {
                throw new ForbiddenException(GlobalConstants.FORBIDDEN_DUE_TO_RESTRICTED_TOKEN, ErrorCodes.ERROR_CODE_FORBIDDEN_ACTION);
            }

            // Verify caller has appropriate access
            authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER);

            // Verify caller is enabled
            BaseUser callerBu = requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled();

            // Only EndUsers would pass above checks, but to be paranoid...
            if (!(callerBu instanceof EndUser)) {
                throw new BadRequestException("Only provisioned and federated users can create delegation agreements", ErrorCodes.ERROR_CODE_GENERIC_BAD_REQUEST);
            }
            EndUser caller = (EndUser) callerBu;

            // If a principal is not specified, default to the caller
            if (StringUtils.isBlank(agreementWeb.getPrincipalId())) {
                agreementWeb.setPrincipalId(callerBu.getId());
                agreementWeb.setPrincipalType(PrincipalType.USER);
            } else if (agreementWeb.getPrincipalType() == null) {
                throw new BadRequestException("Must specify the principal type", ErrorCodes.ERROR_CODE_GENERIC_BAD_REQUEST);
            }

            // The principal must exist and caller must be authorized to create a DA for the principal
            SimplePrincipalValidator principalValidator = new SimplePrincipalValidator(agreementWeb.getPrincipalType(), agreementWeb.getPrincipalId());
            principalValidator.verifyCallerAuthorizedOnPrincipal(caller);
            DelegationPrincipal principal = principalValidator.getPrincipal();

            // Verify RCN is supported for DAs. Blank RCNs only supported if feature is globally enabled for all RCNs
            Domain callerDomain = requestContextHolder.getRequestContext().getEffectiveCallerDomain();
            if (callerDomain == null || !identityConfig.getReloadableConfig().areDelegationAgreementsEnabledForRcn(callerDomain.getRackspaceCustomerNumber())) {
                throw new ForbiddenException("This domain is not allowed to create delegation agreements", ErrorCodes.ERROR_CODE_DA_NOT_ALLOWED_FOR_RCN);
            }

            User delegate = identityUserService.getProvisionedUserById(agreementWeb.getDelegateId());
            if (delegate == null) {
                throw new NotFoundException("Invalid delegate", ErrorCodes.ERROR_CODE_NOT_FOUND);
            }

            // Verify the delegate is within same RCN as caller. If in same domain, allow delegation even if no RCN set
            boolean sameDomain = callerDomain.getDomainId().equalsIgnoreCase(delegate.getDomainId());
            if (!sameDomain) {
                // Must compare RCNs of each user's domain if not in same domain
                Domain delegateDomain = domainService.getDomain(delegate.getDomainId());

                boolean missingRcns = delegateDomain == null || StringUtils.isBlank(callerDomain.getRackspaceCustomerNumber()) || StringUtils.isBlank(delegateDomain.getRackspaceCustomerNumber());

                if (missingRcns || !callerDomain.getRackspaceCustomerNumber().equalsIgnoreCase(delegateDomain.getRackspaceCustomerNumber())) {
                    throw new NotFoundException("Invalid delegate", ErrorCodes.ERROR_CODE_NOT_FOUND);
                }
            }

            // Convert to entity object
            com.rackspace.idm.domain.entity.DelegationAgreement delegationAgreement = delegationAgreementConverter.fromDelegationAgreementWeb(agreementWeb);
            delegationAgreement.setPrincipal(principal);
            delegationAgreement.setDomainId(principal.getDomainId());
            delegationAgreement.getDelegates().add(delegate.getDn());

            // Add the agreement
            delegationService.addDelegationAgreement(delegationAgreement);

            URI location = idmPathUtils.createLocationHeaderValue(uriInfo, delegationAgreement.getId());
            Response.ResponseBuilder response = Response.created(location);
            response.entity(delegationAgreementConverter.toDelegationAgreementWeb(delegationAgreement));
            return response.build();
        } catch (Exception ex) {
            LOG.debug("Error creating delegation agreement", ex);
            return exceptionHandler.exceptionResponse(ex).build();
        }
    }

    @Override
    public Response getAgreement(String authToken, String agreementId) {
        try {
            // Verify token exists and valid
            requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerTokenAsBaseToken(authToken);

            // Verify caller is enabled
            BaseUser callerBu = requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled();

            // Verify caller has appropriate access
            authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER);

            com.rackspace.idm.domain.entity.DelegationAgreement delegationAgreement = delegationService.getDelegationAgreementById(agreementId);

            EndUser caller = (EndUser) callerBu; // To get this far requires user to be EU

            // Currently only principals can retrieve DA
            if (delegationAgreement == null || !(new SimplePrincipalValidator(delegationAgreement.getPrincipal()).isCallerAuthorizedOnPrincipal(caller))) {
                throw new NotFoundException("The specified agreement does not exist for this user");
            }

            return Response.ok().entity(delegationAgreementConverter.toDelegationAgreementWeb(delegationAgreement)).build();
        } catch (Exception ex) {
            LOG.debug(String.format("Error returning delegation agreement '%s'", agreementId), ex);
            return exceptionHandler.exceptionResponse(ex).build();
        }
    }

    @Override
    public Response deleteAgreement(String authToken, String agreementId) {
        try {
            // Verify token exists and valid
            requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerToken(authToken);

            // Verify caller is enabled
            BaseUser callerBu = requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled();

            // Verify caller has appropriate access
            authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER);

            EndUser caller = (EndUser) callerBu; // To get this far requires user to be EU

            // Caller must be the DA principal to delete it
            com.rackspace.idm.domain.entity.DelegationAgreement delegationAgreement = delegationService.getDelegationAgreementById(agreementId);
            if (delegationAgreement == null || !(new SimplePrincipalValidator(delegationAgreement.getPrincipal()).isCallerAuthorizedOnPrincipal(caller))) {
                throw new NotFoundException("The specified agreement does not exist for this user");
            }

            delegationService.deleteDelegationAgreement(delegationAgreement);
            return Response.noContent().build();
        } catch (Exception ex) {
            LOG.debug(String.format("Error deleting delegation agreement '%s'", agreementId), ex);
            return exceptionHandler.exceptionResponse(ex).build();
        }
    }

    private class SimplePrincipalValidator {
        @Getter
        String id;
        @Getter
        PrincipalType principalType;
        @Getter
        DelegationPrincipal principal;

        PrincipalValidator validator;

        public SimplePrincipalValidator(PrincipalType principalType, String id) {
            this.id = id;
            this.principalType = principalType;

            PrincipalLookup innerLookup;
            if (principalType == PrincipalType.USER_GROUP) {
                UserGroupPrincipalLookupValidator lv = new UserGroupPrincipalLookupValidator(id);
                innerLookup = lv;
                validator = lv;
            } else if (principalType == PrincipalType.USER) {
                EndUserPrincipalLookupValidator lv = new EndUserPrincipalLookupValidator(id);
                innerLookup = lv;
                validator = lv;
            } else {
                throw new IllegalArgumentException("Unrecognized principal type");
            }
            principal = innerLookup.getPrincipal();
        }

        public SimplePrincipalValidator(DelegationPrincipal delegationPrincipal) {
            this.id = delegationPrincipal.getId();
            this.principalType = delegationPrincipal.getPrincipalType();
            this.principal = delegationPrincipal;

            if (principalType == PrincipalType.USER_GROUP) {
                validator = new UserGroupPrincipalLookupValidator(delegationPrincipal);
            } else if (principalType == PrincipalType.USER) {
                validator = new EndUserPrincipalLookupValidator(id);
            } else {
                throw new IllegalArgumentException("Unrecognized principal type");
            }
        }

        public boolean isCallerAuthorizedOnPrincipal(EndUser caller) {
            if (principal == null || !validator.isCallerAuthorizedOnPrincipal(caller)) {
                return false;
            }
            return true;
        }

        public void verifyCallerAuthorizedOnPrincipal(EndUser caller) {
            if (!isCallerAuthorizedOnPrincipal(caller)) {
                throw new NotFoundException(ERROR_MSG_PRINCIPAL_NOT_FOUND, ErrorCodes.ERROR_CODE_NOT_FOUND);
            }
        }
    }

    private interface PrincipalLookup {
        DelegationPrincipal getPrincipal();
        String getId();
    }

    private interface PrincipalValidator {
        boolean isCallerAuthorizedOnPrincipal(EndUser caller);
    }

    private class UserGroupPrincipalLookupValidator implements PrincipalLookup, PrincipalValidator {
        private String groupId;
        com.rackspace.idm.modules.usergroups.entity.UserGroup userGroup;

        public UserGroupPrincipalLookupValidator(String groupId) {
            this.groupId = groupId;
            this.userGroup = userGroupService.getGroupById(groupId);
        }

        public UserGroupPrincipalLookupValidator(DelegationPrincipal group) {
            if (group != null) {
                this.groupId = group.getId();
                if (!(group instanceof UserGroup)) {
                    throw new IllegalArgumentException("Supplied principal must be a user group");
                }
                this.userGroup = (UserGroup) group;
            }
        }

        @Override
        public DelegationPrincipal getPrincipal() {
            return userGroup;
        }

        @Override
        public String getId() {
            return groupId;
        }

        @Override
        public boolean isCallerAuthorizedOnPrincipal(EndUser caller) {
            Validate.notNull(caller);

            if (getPrincipal() != null) {
                /*
                 Caller is only valid for a user group principal if the caller is a member of the user group and
                 the user group domain is the same as the callers. Technically, should always be the case as members
                 of a user group must belong to the same domain as the user group, but provide extra check here
                 as delegation provides great power
                 */
                return caller.getDomainId().equalsIgnoreCase(getPrincipal().getDomainId())
                        && caller.getUserGroupDNs().contains(getPrincipal().getDn());
            }
            return false;
        }
    }

    private class EndUserPrincipalLookupValidator implements PrincipalLookup, PrincipalValidator {
        private String userId;
        private EndUser endUser;

        public EndUserPrincipalLookupValidator(String userId) {
            this.userId = userId;
            this.endUser = identityUserService.getEndUserById(userId);
            if (endUser != null) {
                if (!(endUser instanceof DelegationPrincipal)) {
                    throw new IllegalArgumentException("Specified user not a valid delegation principal");
                }
            }
        }

        public EndUserPrincipalLookupValidator(DelegationPrincipal user) {
            if (user != null) {
                this.userId = user.getId();
                if (!(user instanceof EndUser)) {
                    throw new IllegalArgumentException("Supplied user must be an end user");
                }
                this.endUser = (EndUser) user;
            }
        }

        @Override
        public DelegationPrincipal getPrincipal() {
            return (DelegationPrincipal) endUser;
        }

        @Override
        public boolean isCallerAuthorizedOnPrincipal(EndUser caller) {
            Validate.notNull(caller);

            if (getPrincipal() != null) {
                // Caller is only valid for a user principal if the caller is the same user
                return caller.getId().equalsIgnoreCase(getPrincipal().getId());
            }
            return false;
        }

        @Override
        public String getId() {
            return userId;
        }
    }
}
