package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.DelegationAgreement;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.PrincipalType;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.RolespaceType;
import com.rackspace.idm.ErrorCodes;
import com.rackspace.idm.api.converter.cloudv20.DelegationAgreementConverter;
import com.rackspace.idm.api.resource.IdmPathUtils;
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import com.rackspace.idm.api.security.RequestContextHolder;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.ExceptionHandler;
import com.rackspace.idm.exception.ForbiddenException;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.modules.usergroups.api.resource.converter.RoleAssignmentConverter;
import com.rackspace.idm.modules.usergroups.service.UserGroupService;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

/**
 * Implementation of the delegation API services. Implementation is greatly simplified and standardized by combining
 * actual business logic into common services regardless of principal or delegate type.
 */
@Component
public class DefaultDelegationCloudService implements DelegationCloudService {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultDelegationCloudService.class);

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
            requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerToken(authToken);

            // Verify caller has appropriate access
            authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER);

            BaseUser callerBu = requestContextHolder.getRequestContext().getEffectiveCaller();

            // Only EndUsers would pass above checks, but to be paranoid...
            if (!(callerBu instanceof User)) {
                throw new BadRequestException("Only provisioned users can create delegation agreements", ErrorCodes.ERROR_CODE_GENERIC_BAD_REQUEST);
            }

            // Set principal based on caller. User Group principals are not yet supported
            User caller = (User) callerBu;
            Domain callerDomain = requestContextHolder.getRequestContext().getEffectiveCallerDomain();

            // Verify RCN is supported for DAs. Blank RCNs only supported if feature is globally enabled for all RCNs
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
            delegationAgreement.setPrincipal(caller);
            delegationAgreement.setDomainId(callerDomain.getDomainId());
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
            requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerToken(authToken);

            // Verify caller has appropriate access
            authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER);

            com.rackspace.idm.domain.entity.DelegationAgreement delegationAgreement = delegationService.getDelegationAgreementById(agreementId);

            // Caller must be the DA principal to view it for now TODO: Support delegate viewers
            BaseUser callerBu = requestContextHolder.getRequestContext().getEffectiveCaller();

            if (!(callerBu instanceof User)) {
                throw new BadRequestException("Only provisioned users can retrieve delegation agreements");
            }
            User caller = (User) callerBu; // To get this far requires user to be EU

            // Currently only principals can retrieve DA
            if (delegationAgreement == null || !caller.getDn().equals(delegationAgreement.getPrincipalDN())) {
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

            // Verify caller has appropriate access
            authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER);

            BaseUser callerBu = requestContextHolder.getRequestContext().getEffectiveCaller();

            if (!(callerBu instanceof User)) {
                throw new BadRequestException("Only provisioned users can retrieve delegation agreements");
            }
            User caller = (User) callerBu; // To get this far requires user to be EU

            // Caller must be the DA principal to delete it
            com.rackspace.idm.domain.entity.DelegationAgreement delegationAgreement = delegationService.getDelegationAgreementById(agreementId);
            if (delegationAgreement == null || !caller.getDn().equals(delegationAgreement.getPrincipalDN())) {
                throw new NotFoundException("The specified agreement does not exist for this user");
            }

            delegationService.deleteDelegationAgreement(delegationAgreement);
            return Response.noContent().build();
        } catch (Exception ex) {
            LOG.debug(String.format("Error deleting delegation agreement '%s'", agreementId), ex);
            return exceptionHandler.exceptionResponse(ex).build();
        }
    }

    private interface PrincipalLookup {
        DelegationPrincipal getPrincipal();
        DelegationPrincipal checkAndGetPrincipal();
        String getId();
    }

    private class GenericPrincipalLookup implements PrincipalLookup {
        String id;
        PrincipalType principalType;
        PrincipalLookup innerLookup;

        public GenericPrincipalLookup(PrincipalType principalType, String id) {
            this.id = id;
            this.principalType = principalType;

            if (principalType == PrincipalType.USER_GROUP) {
                innerLookup = new UserGroupPrincipalLookup(id);
            } else if (principalType == PrincipalType.USER) {
                innerLookup = new UserPrincipalLookup(id);
            } else {
                throw new IllegalArgumentException("Unrecognized principal type");
            }
        }

        @Override
        public DelegationPrincipal getPrincipal() {
            return innerLookup.getPrincipal();
        }

        @Override
        public DelegationPrincipal checkAndGetPrincipal() {
            return innerLookup.checkAndGetPrincipal();
        }

        @Override
        public String getId() {
            return innerLookup.getId();
        }
    }

    private class UserGroupPrincipalLookup implements PrincipalLookup {
        private String groupId;

        public UserGroupPrincipalLookup(String groupId) {
            this.groupId = groupId;
        }

        @Override
        public DelegationPrincipal getPrincipal() {
            return userGroupService.getGroupById(groupId);
        }

        @Override
        public DelegationPrincipal checkAndGetPrincipal() {
            DelegationPrincipal delegate = getPrincipal();
            if (delegate == null) {
                String errMsg = String.format("Principal %s not found", getId());
                throw new NotFoundException(errMsg);
            }
            return delegate;
        }

        @Override
        public String getId() {
            return groupId;
        }
    }

    private class UserPrincipalLookup implements PrincipalLookup {
        private String userId;

        public UserPrincipalLookup(String userId) {
            this.userId = userId;
        }

        @Override
        public DelegationPrincipal getPrincipal() {
            return identityUserService.getProvisionedUserById(userId);
        }

        @Override
        public DelegationPrincipal checkAndGetPrincipal() {
            DelegationPrincipal delegate = getPrincipal();
            if (delegate == null) {
                String errMsg = String.format("Principal %s not found", getId());
                throw new NotFoundException(errMsg);
            }
            return delegate;
        }

        @Override
        public String getId() {
            return userId;
        }
    }
}
