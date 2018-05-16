package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.DelegationAgreement;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.PrincipalType;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleAssignments;
import com.rackspace.idm.ErrorCodes;
import com.rackspace.idm.GlobalConstants;
import com.rackspace.idm.api.converter.cloudv20.DelegationAgreementConverter;
import com.rackspace.idm.api.resource.IdmPathUtils;
import com.rackspace.idm.api.security.RequestContextHolder;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.entity.BaseUser;
import com.rackspace.idm.domain.entity.BaseUserToken;
import com.rackspace.idm.domain.entity.DelegationDelegate;
import com.rackspace.idm.domain.entity.DelegationPrincipal;
import com.rackspace.idm.domain.entity.Domain;
import com.rackspace.idm.domain.entity.EndUser;
import com.rackspace.idm.domain.entity.PaginatorContext;
import com.rackspace.idm.domain.entity.TenantRole;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.DelegationService;
import com.rackspace.idm.domain.service.DomainService;
import com.rackspace.idm.domain.service.IdentityUserService;
import com.rackspace.idm.domain.service.IdentityUserTypeEnum;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.exception.ExceptionHandler;
import com.rackspace.idm.exception.ForbiddenException;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.exception.SizeLimitExceededException;
import com.rackspace.idm.modules.usergroups.api.resource.converter.RoleAssignmentConverter;
import com.rackspace.idm.modules.usergroups.entity.UserGroup;
import com.rackspace.idm.modules.usergroups.service.UserGroupService;
import com.rackspace.idm.validation.Validator20;
import lombok.Getter;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;

/**
 * Implementation of the delegation API services. Implementation is greatly simplified and standardized by combining
 * actual business logic into common services regardless of principal or delegate type.
 */
@Component
public class DefaultDelegationCloudService implements DelegationCloudService {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultDelegationCloudService.class);

    public static final String ERROR_MSG_PRINCIPAL_NOT_FOUND = "The specified principal was not found or you are not authorized to use this principal";
    public static final String ERROR_MSG_DELEGATE_NOT_FOUND = "The specified delegate was not found or you are not authorized to use this delegate";
    public static final String ERROR_MSG_DELEGATE_MAX_EXCEEDED = "The maximum number of delegates was exceeded or you are not authorized to use this delegate";

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

    @Autowired
    private Validator20 validator20;

    @Autowired
    private RoleAssignmentConverter roleAssignmentConverter;

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

            // Verify field lengths
            validator20.validateStringNotNullWithMaxLength("name", agreementWeb.getName(), Validator20.MAX_LENGTH_32);
            validator20.validateStringMaxLength("description", agreementWeb.getDescription(), Validator20.MAX_LENGTH_255);

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

            // Verify the max allowed DAs per principal has not been reached.
            if (delegationService.countNumberOfDelegationAgreementsByPrincipal(principal) >= identityConfig.getReloadableConfig().getDelegationMaxNumberOfDaPerPrincipal()) {
                throw new BadRequestException("Maximum number of delegation agreements has been reached for principal", ErrorCodes.ERROR_CODE_THRESHOLD_REACHED);
            }

            // Verify RCN is supported for DAs. Blank RCNs only supported if feature is globally enabled for all RCNs
            Domain callerDomain = requestContextHolder.getRequestContext().getEffectiveCallerDomain();
            if (callerDomain == null || !identityConfig.getReloadableConfig().areDelegationAgreementsEnabledForRcn(callerDomain.getRackspaceCustomerNumber())) {
                throw new ForbiddenException("This domain is not allowed to create delegation agreements", ErrorCodes.ERROR_CODE_DA_NOT_ALLOWED_FOR_RCN);
            }

            // Convert to entity object
            com.rackspace.idm.domain.entity.DelegationAgreement delegationAgreement = delegationAgreementConverter.fromDelegationAgreementWeb(agreementWeb);
            delegationAgreement.setPrincipal(principal);
            delegationAgreement.setDomainId(principal.getDomainId());

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
    public Response updateAgreement(String authToken, DelegationAgreement agreement) {
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

            EndUser caller = (EndUser) callerBu; // To get this far requires user to be EU

            // Caller must be the DA principal to update DA.
            com.rackspace.idm.domain.entity.DelegationAgreement delegationAgreement = delegationService.getDelegationAgreementById(agreement.getId());
            if (delegationAgreement == null || !delegationAgreement.isEffectivePrincipal(caller)) {
                throw new NotFoundException("The specified agreement does not exist for this user", ErrorCodes.ERROR_CODE_NOT_FOUND);
            }

            // Copy over only the attributes that are provided in the request and allowed to be updated.
            if (agreement.getName() != null) {
                validator20.validateStringNotNullWithMaxLength("name", agreement.getName(), Validator20.MAX_LENGTH_32);
                delegationAgreement.setName(agreement.getName());
            }

            if (agreement.getDescription() != null) {
                validator20.validateStringMaxLength("description", agreement.getDescription(), Validator20.MAX_LENGTH_255);
                delegationAgreement.setDescription(agreement.getDescription());
            }

            if (agreement.isAllowSubAgreements() != null) {
                delegationAgreement.setAllowSubAgreements(agreement.isAllowSubAgreements());
            }

            delegationService.updateDelegationAgreement(delegationAgreement);

            return Response.ok(delegationAgreementConverter.toDelegationAgreementWeb(delegationAgreement)).build();
        } catch (Exception ex) {
            LOG.debug(String.format("Error updating delegation agreement '%s'", agreement.getId()), ex);
            return exceptionHandler.exceptionResponse(ex).build();
        }
    }

    @Override
    public Response getAgreement(String authToken, String agreementId) {
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

            com.rackspace.idm.domain.entity.DelegationAgreement delegationAgreement = delegationService.getDelegationAgreementById(agreementId);

            EndUser caller = (EndUser) callerBu; // To get this far requires user to be EU

            // Currently only principals can retrieve DA
            if (delegationAgreement == null || !(new SimplePrincipalValidator(delegationAgreement.getPrincipal()).isCallerAuthorizedOnPrincipal(caller))) {
                throw new NotFoundException("The specified agreement does not exist for this user", ErrorCodes.ERROR_CODE_NOT_FOUND);
            }

            return Response.ok().entity(delegationAgreementConverter.toDelegationAgreementWeb(delegationAgreement)).build();
        } catch (Exception ex) {
            LOG.debug(String.format("Error returning delegation agreement '%s'", agreementId), ex);
            return exceptionHandler.exceptionResponse(ex).build();
        }
    }

    @Override
    public Response listAgreements(String authToken, String relationshipType) {
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

            EndUser caller = (EndUser) callerBu; // To get this far requires user to be EU

            /*
            To search for DAs by principal and/or delegate must provide the delegate and/or principal to search on. This
            is done by providing a DelegationDelegate or DelegationPrincipal. Currently this call is limited to solely
            search for DAs for which the *caller* is an (effective) principal or (effective) delegate. All Federated
            users and provisioned users
            can potentially be a delegate or principal. However, not all implementations of EndUser can currently be
            a delegate or principal. In particular, EndUserDelegates can not be principals until Derived DAs are allowed.
             */
            DelegationDelegate delegationDelegateSearchReference = caller instanceof DelegationDelegate ? (DelegationDelegate) caller : null;
            DelegationPrincipal delegationPrincipalSearchReference = caller instanceof DelegationPrincipal ? (DelegationPrincipal) caller : null;

            /*
            The exact search params depend on the relationship requested.
            - When no relationship is specified, search for all DAs for which caller is either an effective principal
            or effective delegate.
            - When delegate is specified, only find those DAs for which caller is an effective delegate
            - When principal is specified, only find those DAs for which caller is an effective principal
             */
            FindDelegationAgreementParams findDelegationAgreementParams = null;
            try {
                if (StringUtils.isBlank(relationshipType)) {
                    findDelegationAgreementParams = new FindDelegationAgreementParams(delegationDelegateSearchReference, delegationPrincipalSearchReference);
                } else if (StringUtils.equalsIgnoreCase("delegate", relationshipType)) {
                    findDelegationAgreementParams = new FindDelegationAgreementParams(delegationDelegateSearchReference, null);
                } else if (StringUtils.equalsIgnoreCase("principal", relationshipType)) {
                    findDelegationAgreementParams = new FindDelegationAgreementParams(null, delegationPrincipalSearchReference);
                } else {
                    throw new BadRequestException("The specified relationship is invalid", ErrorCodes.ERROR_CODE_INVALID_ATTRIBUTE);
                }
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid caller for relationship", ErrorCodes.ERROR_CODE_INVALID_ATTRIBUTE);
            }

            List<com.rackspace.idm.domain.entity.DelegationAgreement> delegationAgreements = delegationService.findDelegationAgreements(findDelegationAgreementParams);

            return Response.ok().entity(delegationAgreementConverter.toDelegationAgreementsWeb(delegationAgreements)).build();
        } catch (SizeLimitExceededException ex) {
            // For consistency, always go through the standard exceptionHandler method to process exceptions.
            return exceptionHandler.exceptionResponse(new ForbiddenException("The search resulted in too many results. Please apply filters to reduce the number of results.")).build();
        } catch (Exception ex) {
            LOG.debug("Error finding delegation agreements", ex);
            return exceptionHandler.exceptionResponse(ex).build();
        }
    }

    @Override
    public Response deleteAgreement(String authToken, String agreementId) {
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

            EndUser caller = (EndUser) callerBu; // To get this far requires user to be EU

            // Caller must be the DA principal to delete it
            com.rackspace.idm.domain.entity.DelegationAgreement delegationAgreement = delegationService.getDelegationAgreementById(agreementId);
            if (delegationAgreement == null || !(new SimplePrincipalValidator(delegationAgreement.getPrincipal()).isCallerAuthorizedOnPrincipal(caller))) {
                throw new NotFoundException("The specified agreement does not exist for this user", ErrorCodes.ERROR_CODE_NOT_FOUND);
            }

            delegationService.deleteDelegationAgreement(delegationAgreement);
            return Response.noContent().build();
        } catch (Exception ex) {
            LOG.debug(String.format("Error deleting delegation agreement '%s'", agreementId), ex);
            return exceptionHandler.exceptionResponse(ex).build();
        }
    }

    @Override
    public Response addDelegate(String authToken, String agreementId, DelegateReference delegateReference) {
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

            EndUser caller = (EndUser) callerBu; // To get this far requires user to be EU

            // Caller must be the DA principal to modify delegates
            com.rackspace.idm.domain.entity.DelegationAgreement delegationAgreement = delegationService.getDelegationAgreementById(agreementId);
            if (delegationAgreement == null || !delegationAgreement.isEffectivePrincipal(caller)) {
                throw new NotFoundException("The specified agreement does not exist for this user", ErrorCodes.ERROR_CODE_NOT_FOUND);
            }

            // Verify delegate is valid for the specified agreement
            SimpleDelegateValidator delegateLookupValidator = new SimpleDelegateValidator(delegateReference);
            delegateLookupValidator.verifyDelegateCanBeAddedToAgreement(delegationAgreement);

            // Verify max number of delegates has not been exceeded
            NumberOfDelegatesValidator numberOfDelegatesValidator = new NumberOfDelegatesValidator(identityConfig);
            numberOfDelegatesValidator.verifyMaxNumberOfDelegatesIsNotExceeded(delegationAgreement);

            if (delegationAgreement.isExplicitDelegate(delegateLookupValidator.delegate)) {
                throw new DuplicateException("Already a delegate", ErrorCodes.ERROR_CODE_DELEGATE_ALREADY_EXISTS);
            } else {
                delegationService.addDelegate(delegationAgreement, delegateLookupValidator.delegate);
                return Response.noContent().build();
            }

        } catch (Exception ex) {
            LOG.debug(String.format("Error deleting delegation agreement '%s'", agreementId), ex);
            return exceptionHandler.exceptionResponse(ex).build();
        }
    }

    @Override
    public Response deleteDelegate(String authToken, String agreementId, DelegateReference delegateReference) {
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

            EndUser caller = (EndUser) callerBu; // To get this far requires user to be EU

            // Caller must be the DA principal to modify delegates
            com.rackspace.idm.domain.entity.DelegationAgreement delegationAgreement = delegationService.getDelegationAgreementById(agreementId);
            if (delegationAgreement == null || !(new SimplePrincipalValidator(delegationAgreement.getPrincipal()).isCallerAuthorizedOnPrincipal(caller))) {
                throw new NotFoundException("The specified agreement does not exist for this user", ErrorCodes.ERROR_CODE_NOT_FOUND);
            }

            // No validation required for delegate
            boolean wasRemoved = delegationService.deleteDelegate(delegationAgreement, delegateReference);

            if (!wasRemoved) {
                throw new NotFoundException("Delegate does not exist on this agreement");
            }
            return Response.noContent().build();
        } catch (Exception ex) {
            LOG.debug(String.format("Error deleting delegation agreement '%s'", agreementId), ex);
            return exceptionHandler.exceptionResponse(ex).build();
        }
    }

    @Override
    public Response listDelegates(String authToken, String agreementId) {
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

            EndUser caller = (EndUser) callerBu; // To get this far requires user to be EU

            // Caller must be the DA principal or delegate to list delegates
            com.rackspace.idm.domain.entity.DelegationAgreement delegationAgreement = delegationService.getDelegationAgreementById(agreementId);
            if (delegationAgreement == null || (!delegationAgreement.isEffectivePrincipal(caller) && !delegationAgreement.isEffectiveDelegate(caller))) {
                throw new NotFoundException("The specified agreement does not exist for this user", ErrorCodes.ERROR_CODE_NOT_FOUND);
            }

            /*
            Retrieve the delegates. This is inefficient as it looks up each delegate, but must at least look up fed
            users to retrieve the id associated with user. Expect limitations to be placed on how many delegates can
            be associated with a DA to limit impact of this. Expect to want to retrieve more information
            per user (e.g. like a name) to provide more context in response.
             */
            List<DelegationDelegate> delegates = delegationService.getDelegates(delegationAgreement);

            return Response.ok().entity(delegationAgreementConverter.toDelegatesWeb(delegates)).build();
        } catch (Exception ex) {
            LOG.debug(String.format("Error listing delegates for daId '%s'", agreementId), ex);
            return exceptionHandler.exceptionResponse(ex).build();
        }
    }

    @Override
    public Response grantRolesToAgreement(String authToken, String agreementId, RoleAssignments roleAssignments) {
        try {
            // Verify token exists and valid
            BaseUserToken token = requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerTokenAsBaseToken(authToken);

            // Verify token is not a scoped token or delegation token
            if (!StringUtils.isBlank(token.getScope()) || token.isDelegationToken()) {
                throw new ForbiddenException(GlobalConstants.FORBIDDEN_DUE_TO_RESTRICTED_TOKEN, ErrorCodes.ERROR_CODE_FORBIDDEN_ACTION);
            }

            // Verify caller is enabled
            BaseUser callerBu = requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled();

            // Verify caller has appropriate access
            authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER);

            EndUser caller = (EndUser) callerBu; // To get this far requires user to be EU

            // Caller must be authorized to modify roles on DA
            com.rackspace.idm.domain.entity.DelegationAgreement delegationAgreement = delegationService.getDelegationAgreementById(agreementId);
            if (delegationAgreement == null
                    || !(delegationAgreement.isEffectivePrincipal(caller)
                    || authorizationService.isCallerAuthorizedToManageDelegationAgreement(delegationAgreement))) {
                throw new NotFoundException("The specified agreement does not exist for this user", ErrorCodes.ERROR_CODE_NOT_FOUND);
            }

            if (roleAssignments == null) {
                throw new BadRequestException("Must supply a set of assignments", ErrorCodes.ERROR_CODE_REQUIRED_ATTRIBUTE);
            }

            delegationService.replaceRoleAssignmentsOnDelegationAgreement(delegationAgreement, roleAssignments);

            // Retrieve the first 1000 assigned roles on the user
            PaginatorContext<TenantRole> tenantRolePage = delegationService.getRoleAssignmentsOnDelegationAgreement(delegationAgreement, new DelegationAgreementRoleSearchParams(new PaginationParams(0, 1000)));

            return Response.ok(roleAssignmentConverter.toRoleAssignmentsWeb(tenantRolePage.getValueList())).build();
        } catch (Exception ex) {
            LOG.debug(String.format("Error granting roles to delegation agreement '%s'", agreementId), ex);
            return exceptionHandler.exceptionResponse(ex).build();
        }
    }

    @Override
    public Response listRoleAssignmentsOnAgreement(UriInfo uriInfo, String authToken, String agreementId, DelegationAgreementRoleSearchParams searchParams) {
        try {
            // Verify token exists and valid
            BaseUserToken token = requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerTokenAsBaseToken(authToken);

            // Verify token is not a scoped token or delegation token
            if (!StringUtils.isBlank(token.getScope()) || token.isDelegationToken()) {
                throw new ForbiddenException(GlobalConstants.FORBIDDEN_DUE_TO_RESTRICTED_TOKEN, ErrorCodes.ERROR_CODE_FORBIDDEN_ACTION);
            }

            // Verify caller is enabled
            BaseUser callerBu = requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled();

            // Verify caller has appropriate access
            authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER);

            EndUser caller = (EndUser) callerBu; // To get this far requires user to be EU

            // Caller must be the DA principal or a delegate to list DA roles
            com.rackspace.idm.domain.entity.DelegationAgreement delegationAgreement = delegationService.getDelegationAgreementById(agreementId);
            if (delegationAgreement == null
                    || !(delegationAgreement.isEffectivePrincipal(caller)
                    || delegationAgreement.isEffectiveDelegate(caller))) {
                throw new NotFoundException("The specified agreement does not exist for this user", ErrorCodes.ERROR_CODE_NOT_FOUND);
            }

            PaginatorContext<TenantRole> tenantRolePage = delegationService.getRoleAssignmentsOnDelegationAgreement(delegationAgreement, searchParams);

            String linkHeader = idmPathUtils.createLinkHeader(uriInfo, tenantRolePage);

            return Response.status(200)
                    .header(HttpHeaders.LINK, linkHeader)
                    .entity(roleAssignmentConverter.toRoleAssignmentsWeb(tenantRolePage.getValueList())).build();
        } catch (Exception ex) {
            LOG.debug(String.format("Error retrieving roles from delegation agreement '%s'", agreementId), ex);
            return exceptionHandler.exceptionResponse(ex).build();
        }
    }

    @Override
    public Response revokeRoleFromAgreement(String authToken, String agreementId, String roleId) {
        try {
            // Verify token exists and valid
            BaseUserToken token = requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerTokenAsBaseToken(authToken);

            // Verify token is not a scoped token or delegation token
            if (!StringUtils.isBlank(token.getScope()) || token.isDelegationToken()) {
                throw new ForbiddenException(GlobalConstants.FORBIDDEN_DUE_TO_RESTRICTED_TOKEN, ErrorCodes.ERROR_CODE_FORBIDDEN_ACTION);
            }

            // Verify caller is enabled
            BaseUser callerBu = requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled();

            // Verify caller has appropriate access
            authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER);

            EndUser caller = (EndUser) callerBu; // To get this far requires user to be EU

            // Caller must be authorized to revoke roles on DA
            com.rackspace.idm.domain.entity.DelegationAgreement delegationAgreement = delegationService.getDelegationAgreementById(agreementId);
            if (delegationAgreement == null
                    || !(delegationAgreement.isEffectivePrincipal(caller)
                    || authorizationService.isCallerAuthorizedToManageDelegationAgreement(delegationAgreement))) {
                throw new NotFoundException("The specified agreement does not exist for this user", ErrorCodes.ERROR_CODE_NOT_FOUND);
            }

            delegationService.revokeRoleAssignmentOnDelegationAgreement(delegationAgreement, roleId);

            return Response.noContent().build();
        } catch (Exception ex) {
            LOG.debug(String.format("Error revoking role '%s' from delegation agreement '%s'", roleId, agreementId), ex);
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

    private class SimpleDelegateValidator {
        @Getter
        DelegateReference delegateReference;

        @Getter
        DelegationDelegate delegate;

        public SimpleDelegateValidator(DelegateReference delegateReference) {
            this.delegateReference = delegateReference;
            delegate = delegationService.getDelegateByReference(delegateReference);
        }

        public void verifyDelegateCanBeAddedToAgreement(com.rackspace.idm.domain.entity.DelegationAgreement da) {
            verifyDelegateCanBeAddedToAgreementInDomain(da.getDomainId());
        }

        public void verifyDelegateCanBeAddedToAgreementInDomain(String domainId) {
            // Delegate must be within same domain as agreement or same RCN
            if (delegate == null || !domainService.doDomainsShareRcn(delegate.getDomainId(), domainId)) {
                throw new NotFoundException(ERROR_MSG_DELEGATE_NOT_FOUND, ErrorCodes.ERROR_CODE_NOT_FOUND);
            }
        }
    }

    private class NumberOfDelegatesValidator {
        IdentityConfig identityConfig;

        public NumberOfDelegatesValidator(IdentityConfig identityConfig) {
            this.identityConfig = identityConfig;
        }

        public void verifyMaxNumberOfDelegatesIsNotExceeded(com.rackspace.idm.domain.entity.DelegationAgreement da) {
            if (da != null && da.getDelegates() != null && da.getDelegates().size() >= identityConfig.getReloadableConfig().getDelegationMaxNumberOfDelegatesPerDa()) {
                throw new BadRequestException(ERROR_MSG_DELEGATE_MAX_EXCEEDED, ErrorCodes.ERROR_CODE_MAX_LENGTH_EXCEEDED);
            }
        }
    }
}
