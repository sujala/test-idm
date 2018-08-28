package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.*;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.DelegationAgreement;
import com.rackspace.idm.ErrorCodes;
import com.rackspace.idm.GlobalConstants;
import com.rackspace.idm.api.converter.cloudv20.DelegationAgreementConverter;
import com.rackspace.idm.api.resource.IdmPathUtils;
import com.rackspace.idm.api.security.IdentityRole;
import com.rackspace.idm.api.security.RequestContextHolder;
import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.audit.DelegationAgreementAuditBuilder;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.entity.Domain;
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
import com.unboundid.ldap.sdk.DN;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.openstack.docs.identity.api.v2.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.math.BigInteger;
import java.net.URI;
import java.util.*;

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

    public static final String ERROR_MSG_NESTED_ROLE_ASSIGNMENT_FORBIDDEN = "Role assignments on subagreements is not currently supported.";

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
        Audit audit = null;
        try {
            // Verify token exists and valid
            BaseUserToken token = requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerTokenAsBaseToken(authToken);

            // Verify token is not a scoped token or delegation token
            if (!StringUtils.isBlank(token.getScope()) || token.isDelegationToken()) {
                throw new ForbiddenException(GlobalConstants.FORBIDDEN_DUE_TO_RESTRICTED_TOKEN, ErrorCodes.ERROR_CODE_FORBIDDEN_ACTION);
            }

            boolean isNestedAgreement = StringUtils.isNotBlank(agreementWeb.getParentDelegationAgreementId());

            // Verify caller has appropriate access for root and Nested DA
            if (!isNestedAgreement && !identityConfig.getReloadableConfig().isGlobalRootDelegationAgreementCreationEnabled()) {
                authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.USER_ADMIN);
            } else {
                authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER);
            }

            // Verify caller is enabled
            BaseUser callerBu = requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled();

            // Only EndUsers would pass above checks, but to be paranoid...
            if (!(callerBu instanceof EndUser)) {
                throw new BadRequestException("Only provisioned and federated users can create delegation agreements", ErrorCodes.ERROR_CODE_GENERIC_BAD_REQUEST);
            }
            EndUser caller = (EndUser) callerBu;

            boolean isNestLevelSpecified = agreementWeb.getSubAgreementNestLevel() != null;

            // Verify overall request
            validator20.validateStringNotNullWithMaxLength("name", agreementWeb.getName(), Validator20.MAX_LENGTH_32);
            validator20.validateAttributeIsNotEmpty("description", agreementWeb.getDescription());
            validator20.validateAttributeIsNotEmpty("parentDelegationAgreementId", agreementWeb.getParentDelegationAgreementId());
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
            principalValidator.verifyCallerAuthorizedOnPrincipal();
            DelegationPrincipal principal = principalValidator.getPrincipal();

            // Set the principal's domain
            agreementWeb.setPrincipalDomainId(principal.getDomainId());

            // If nested agreement, the parent must exist and the nested DA principal must be an effective delegate on parent DA
            com.rackspace.idm.domain.entity.DelegationAgreement parentDelegationAgreement = null;
            if (isNestedAgreement) {
                parentDelegationAgreement = delegationService.getDelegationAgreementById(agreementWeb.getParentDelegationAgreementId());
                boolean isValid = false;
                if (parentDelegationAgreement != null) {
                    if (principal instanceof DelegationDelegate) {
                        DelegationDelegate delegate = (DelegationDelegate) principal;
                        isValid = parentDelegationAgreement.isExplicitDelegate(delegate);
                    }
                    if (!isValid && principal instanceof EndUser) {
                        // If principal is an enduser, may be an effective delegate due to user group membership
                        isValid = parentDelegationAgreement.isEffectiveDelegate((EndUser) principal);
                    }
                }
                if (!isValid) {
                    throw new NotFoundException("The specified parent agreement does not exist for the requested principal", ErrorCodes.ERROR_CODE_NOT_FOUND);
                }

                // Verify parent DA allows nested DA
                if (!(parentDelegationAgreement.getSubAgreementNestLevelNullSafe() > 0)) {
                    throw new ForbiddenException("The parent agreement does not allow nested agreements", ErrorCodes.ERROR_CODE_FORBIDDEN_ACTION);
                }
            }

            // Validate and set defaults for the nesting attributes. The max nest level varies based on whether or not a nested agreement
            if (!isNestLevelSpecified) {
                agreementWeb.setSubAgreementNestLevel(BigInteger.ZERO);
            }
            int maxNestLevel = isNestedAgreement ? Math.max(0, parentDelegationAgreement.getSubAgreementNestLevelNullSafe() - 1) : identityConfig.getReloadableConfig().getMaxDelegationAgreementNestingLevel();
            validator20.validateIntegerMinMax("subAgreementNestLevel", agreementWeb.getSubAgreementNestLevel().intValue(), 0, maxNestLevel);

            // Verify da domain's RCN supports DAs. Blank RCNs only supported if feature is globally enabled for all RCNs
            // The DA's domain is the domain being delegated into. Nested agreements inherit the domainId of its
            // parent; non-nested inherit the domainId of the principal
            if (isNestedAgreement) {
                agreementWeb.setDomainId(parentDelegationAgreement.getDomainId());
            } else {
                agreementWeb.setDomainId(principal.getDomainId());
            }
            Domain daDomain = null;
            if (agreementWeb.getDomainId().equalsIgnoreCase(caller.getDomainId())) {
                daDomain = requestContextHolder.getRequestContext().getEffectiveCallerDomain();
            }
            if (daDomain == null) {
                daDomain = domainService.getDomain(agreementWeb.getDomainId());
            }

            if (daDomain == null || !identityConfig.getReloadableConfig().areDelegationAgreementsEnabledForRcn(daDomain.getRackspaceCustomerNumber())) {
                throw new ForbiddenException("This domain is not allowed to create delegation agreements", ErrorCodes.ERROR_CODE_DA_NOT_ALLOWED_FOR_RCN);
            }

            // Verify the max allowed DAs per principal has not been reached.
            if (delegationService.countNumberOfDelegationAgreementsByPrincipal(principal) >= identityConfig.getReloadableConfig().getDelegationMaxNumberOfDaPerPrincipal()) {
                throw new BadRequestException("Maximum number of delegation agreements has been reached for principal", ErrorCodes.ERROR_CODE_THRESHOLD_REACHED);
            }

            // Convert to entity object
            com.rackspace.idm.domain.entity.DelegationAgreement delegationAgreement = delegationAgreementConverter.fromDelegationAgreementWeb(agreementWeb);
            delegationAgreement.setPrincipal(principal);


            // Add the agreement
            com.rackspace.idm.domain.entity.DelegationAgreement createdDA = delegationService.addDelegationAgreement(delegationAgreement);
            audit = Audit.log(getDelegationAgreementAuditBuilder(caller, createdDA).build()).add();

            URI location = idmPathUtils.createLocationHeaderValue(uriInfo, delegationAgreement.getId());
            Response.ResponseBuilder response = Response.created(location);
            response.entity(delegationAgreementConverter.toDelegationAgreementWeb(delegationAgreement));
            audit.succeed();
            return response.build();
        } catch (Exception ex) {
            if (audit != null) {
                audit.fail();
            }
            LOG.debug("Error creating delegation agreement", ex);
            return exceptionHandler.exceptionResponse(ex).build();
        }
    }

    @Override
    public Response updateAgreement(String authToken, DelegationAgreement agreementWeb) {
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

            // The caller must be authorized to manage the DA in order to update the DA.
            com.rackspace.idm.domain.entity.DelegationAgreement delegationAgreement = delegationService.getDelegationAgreementById(agreementWeb.getId());
            if (delegationAgreement == null || !authorizationService.isCallerAuthorizedToManageDelegationAgreement(delegationAgreement)) {
                throw new NotFoundException("The specified agreement does not exist for this user", ErrorCodes.ERROR_CODE_NOT_FOUND);
            }

            // Copy over only the attributes that are provided in the request and allowed to be updated.
            if (agreementWeb.getName() != null) {
                validator20.validateStringNotNullWithMaxLength("name", agreementWeb.getName(), Validator20.MAX_LENGTH_32);
                delegationAgreement.setName(agreementWeb.getName());
            }

            if (agreementWeb.getDescription() != null) {
                validator20.validateAttributeIsNotEmpty("description", agreementWeb.getDescription());
                validator20.validateStringMaxLength("description", agreementWeb.getDescription(), Validator20.MAX_LENGTH_255);
                delegationAgreement.setDescription(agreementWeb.getDescription());
            }

            boolean isNestLevelSpecified = agreementWeb.getSubAgreementNestLevel() != null;
            boolean isNestedAgreement = StringUtils.isNotBlank(delegationAgreement.getParentDelegationAgreementId());

            if (isNestLevelSpecified) {
                com.rackspace.idm.domain.entity.DelegationAgreement parentDelegationAgreement = null;

                // The only time we pull parent is when updating a nested agreement's nest levels
                // to ensure they are valid per the parent.
                if (isNestedAgreement) {
                    parentDelegationAgreement = delegationService.getDelegationAgreementById(delegationAgreement.getParentDelegationAgreementId());
                    if (parentDelegationAgreement == null) {
                        throw new NotFoundException("The agreement's nest levels can not be updated. The parent agreement does not exist.", ErrorCodes.ERROR_CODE_NOT_FOUND);
                    }

                    // Verify parent DA allows nested DA. TODO: Other code should reconcile subagreements when parent nest level is changed so subagreements don't exist
                    // under parents that do not allow subagreements.
                    if (!(parentDelegationAgreement.getSubAgreementNestLevelNullSafe() > 0)) {
                        throw new ForbiddenException("This nested agreement nest levels can not be updated. The parent agreement does not allow nested agreements.", ErrorCodes.ERROR_CODE_FORBIDDEN_ACTION);
                    }
                }

                int maxNestLevel = isNestedAgreement ? Math.max(0, parentDelegationAgreement.getSubAgreementNestLevelNullSafe() - 1) : identityConfig.getReloadableConfig().getMaxDelegationAgreementNestingLevel();
                delegationAgreement.setSubAgreementNestLevel(agreementWeb.getSubAgreementNestLevel().intValue());
                validator20.validateIntegerMinMax("subAgreementNestLevel", delegationAgreement.getSubAgreementNestLevel().intValue(), 0, maxNestLevel);
            }

            delegationService.updateDelegationAgreement(delegationAgreement);

            return Response.ok(delegationAgreementConverter.toDelegationAgreementWeb(delegationAgreement)).build();
        } catch (Exception ex) {
            LOG.debug(String.format("Error updating delegation agreement '%s'", agreementWeb.getId()), ex);
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

            // The caller must be authorized to manage the DA in order to get the DA.
            if (delegationAgreement == null || !(new SimplePrincipalValidator(delegationAgreement.getPrincipal()).isCallerAuthorizedOnPrincipal())) {
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

            // A set of principal Domain IDs used when `relationshipType` is not provided and the caller has role
            // "rcn:admin", "identity:user-admin", or "identity:user-manage".
            Set<Domain> principalDomains = new HashSet<>();

            try {
                if (StringUtils.isBlank(relationshipType)) {

                    if (authorizationService.authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(Collections.singletonList(IdentityRole.RCN_ADMIN.getRoleName()))) {
                        Domain callerDomain = requestContextHolder.getRequestContext().getEffectiveCallerDomain();
                        for (Domain domain : domainService.getDomainsByRCN(callerDomain.getRackspaceCustomerNumber())) {
                            principalDomains.add(domain);
                        }
                    } else if (authorizationService.authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(Arrays.asList(
                            IdentityUserTypeEnum.USER_ADMIN.getRoleName(),
                            IdentityUserTypeEnum.USER_MANAGER.getRoleName()))) {
                        Domain callerDomain = requestContextHolder.getRequestContext().getEffectiveCallerDomain();
                        principalDomains.add(callerDomain);
                    }

                    findDelegationAgreementParams = new FindDelegationAgreementParams(delegationDelegateSearchReference, delegationPrincipalSearchReference, principalDomains);
                } else if (StringUtils.equalsIgnoreCase("delegate", relationshipType)) {
                    findDelegationAgreementParams = new FindDelegationAgreementParams(delegationDelegateSearchReference, null, null);
                } else if (StringUtils.equalsIgnoreCase("principal", relationshipType)) {
                    findDelegationAgreementParams = new FindDelegationAgreementParams(null, delegationPrincipalSearchReference, null);
                } else {
                    throw new BadRequestException("The specified relationship is invalid", ErrorCodes.ERROR_CODE_INVALID_ATTRIBUTE);
                }
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid caller for relationship", ErrorCodes.ERROR_CODE_INVALID_ATTRIBUTE);
            }

            List<com.rackspace.idm.domain.entity.DelegationAgreement> delegationAgreements = delegationService.findDelegationAgreements(findDelegationAgreementParams);

            // Filter DAs based on user-manager caller
            if (CollectionUtils.isNotEmpty(principalDomains)
                    && authorizationService.authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(
                    Collections.singletonList(IdentityUserTypeEnum.USER_MANAGER.getRoleName()))) {
                Set<DN> userAdminDNs = new HashSet<>();
                // Find domain admin DNs
                for (Domain domain : principalDomains) {
                    if (domain.getUserAdminDN() != null) {
                        userAdminDNs.add(domain.getUserAdminDN());
                    }
                }

                Iterator<com.rackspace.idm.domain.entity.DelegationAgreement> iterator = delegationAgreements.iterator();
                while (iterator.hasNext()) {
                    com.rackspace.idm.domain.entity.DelegationAgreement delegationAgreement = iterator.next();

                    // Determine if caller has access to delegation agreement.
                    if (!delegationAgreement.isEffectiveDelegate(caller) && userAdminDNs.contains(delegationAgreement.getPrincipalDN())) {
                        iterator.remove();
                    }

                }
            }

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
        Audit audit = null;
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

            // The caller must be authorized to manage the DA in order to delete it.
            com.rackspace.idm.domain.entity.DelegationAgreement delegationAgreement = delegationService.getDelegationAgreementById(agreementId);
            if (delegationAgreement == null || !(new SimplePrincipalValidator(delegationAgreement.getPrincipal()).isCallerAuthorizedOnPrincipal())) {
                throw new NotFoundException("The specified agreement does not exist for this user", ErrorCodes.ERROR_CODE_NOT_FOUND);
            }

            audit = Audit.log(getDelegationAgreementAuditBuilder(caller, delegationAgreement).build()).delete();

            delegationService.deleteDelegationAgreement(delegationAgreement);
            audit.succeed();
            return Response.noContent().build();
        } catch (Exception ex) {
            if (audit != null) {
                audit.fail();
            }
            LOG.debug(String.format("Error deleting delegation agreement '%s'", agreementId), ex);
            return exceptionHandler.exceptionResponse(ex).build();
        }
    }

    @Override
    public Response addDelegate(String authToken, String agreementId, DelegateReference delegateReference) {
        Audit audit = null;
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

            // Caller must have access to manage the DA in order to modify delegates
            com.rackspace.idm.domain.entity.DelegationAgreement delegationAgreement = delegationService.getDelegationAgreementById(agreementId);
            if (delegationAgreement == null ||  !authorizationService.isCallerAuthorizedToManageDelegationAgreement(delegationAgreement)) {
                throw new NotFoundException("The specified agreement does not exist for this user", ErrorCodes.ERROR_CODE_NOT_FOUND);
            }

            // Verify delegate is valid for the specified agreement
            SimpleDelegateValidator delegateLookupValidator = new SimpleDelegateValidator(delegateReference);
            delegateLookupValidator.verifyDelegateCanBeAddedToAgreement(delegationAgreement);

            // Verify max number of delegates has not been exceeded
            NumberOfDelegatesValidator numberOfDelegatesValidator = new NumberOfDelegatesValidator(identityConfig);
            numberOfDelegatesValidator.verifyMaxNumberOfDelegatesIsNotExceeded(delegationAgreement);

            audit = Audit.log(getDelegationAgreementAuditBuilder(caller, delegationAgreement, delegateReference).build()).add();

            if (delegationAgreement.isExplicitDelegate(delegateLookupValidator.delegate)) {
                throw new DuplicateException("Already a delegate", ErrorCodes.ERROR_CODE_DELEGATE_ALREADY_EXISTS);
            } else {
                delegationService.addDelegate(delegationAgreement, delegateLookupValidator.delegate);
                audit.succeed();
                return Response.noContent().build();
            }

        } catch (Exception ex) {
            if (audit != null) {
                audit.fail();
            }
            LOG.debug(String.format("Error deleting delegation agreement '%s'", agreementId), ex);
            return exceptionHandler.exceptionResponse(ex).build();
        }
    }

    @Override
    public Response deleteDelegate(String authToken, String agreementId, DelegateReference delegateReference) {
        Audit audit = null;
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

            // Caller must have access to manage the DA in order to modify delegates
            com.rackspace.idm.domain.entity.DelegationAgreement delegationAgreement = delegationService.getDelegationAgreementById(agreementId);
            if (delegationAgreement == null || !authorizationService.isCallerAuthorizedToManageDelegationAgreement(delegationAgreement)) {
                throw new NotFoundException("The specified agreement does not exist for this user", ErrorCodes.ERROR_CODE_NOT_FOUND);
            }

            audit = Audit.log(getDelegationAgreementAuditBuilder(caller, delegationAgreement, delegateReference).build()).delete();

            // No validation required for delegate
            boolean wasRemoved = delegationService.deleteDelegate(delegationAgreement, delegateReference);

            if (!wasRemoved) {
                throw new NotFoundException("Delegate does not exist on this agreement");
            }
            audit.succeed();
            return Response.noContent().build();
        } catch (Exception ex) {
            if (audit != null) {
                audit.fail();
            }
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

            // Caller must have access to manage the DA or be a delegate to list delegates
            com.rackspace.idm.domain.entity.DelegationAgreement delegationAgreement = delegationService.getDelegationAgreementById(agreementId);
            if (delegationAgreement == null || (!authorizationService.isCallerAuthorizedToManageDelegationAgreement(delegationAgreement) &&
                            !delegationAgreement.isEffectiveDelegate(caller))) {
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
        Audit audit = null;
        try {
            // Verify token exists and valid
            BaseUserToken token = requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerTokenAsBaseToken(authToken);

            // Verify token is not a scoped token or delegation token
            if (!StringUtils.isBlank(token.getScope()) || token.isDelegationToken()) {
                throw new ForbiddenException(GlobalConstants.FORBIDDEN_DUE_TO_RESTRICTED_TOKEN, ErrorCodes.ERROR_CODE_FORBIDDEN_ACTION);
            }

            // Verify caller is enabled
            BaseUser caller = requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled();

            // Verify caller has appropriate access
            authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER);

            // Caller must be authorized to modify roles on DA
            com.rackspace.idm.domain.entity.DelegationAgreement delegationAgreement = delegationService.getDelegationAgreementById(agreementId);
            if (delegationAgreement == null
                    || !authorizationService.isCallerAuthorizedToManageDelegationAgreement(delegationAgreement)) {
                throw new NotFoundException("The specified agreement does not exist for this user", ErrorCodes.ERROR_CODE_NOT_FOUND);
            }

            if (StringUtils.isNotBlank(delegationAgreement.getParentDelegationAgreementId())
                    && !identityConfig.getReloadableConfig().canRolesBeAssignedToNestedDelegationAgreements()) {
                throw new ForbiddenException(ERROR_MSG_NESTED_ROLE_ASSIGNMENT_FORBIDDEN, ErrorCodes.ERROR_CODE_FORBIDDEN_ACTION);
            }

            if (roleAssignments == null) {
                throw new BadRequestException("Must supply a set of assignments", ErrorCodes.ERROR_CODE_REQUIRED_ATTRIBUTE);
            }

            audit = Audit.log(getDelegationAgreementAuditBuilder(caller, delegationAgreement, roleAssignments).build()).add();

            delegationService.replaceRoleAssignmentsOnDelegationAgreement(delegationAgreement, roleAssignments);

            // Retrieve the first 1000 assigned roles on the user
            PaginatorContext<TenantRole> tenantRolePage = delegationService.getRoleAssignmentsOnDelegationAgreement(delegationAgreement, new DelegationAgreementRoleSearchParams(new PaginationParams(0, 1000)));

            audit.succeed();
            return Response.ok(roleAssignmentConverter.toRoleAssignmentsWeb(tenantRolePage.getValueList())).build();
        } catch (Exception ex) {
            if (audit != null) {
                audit.fail();
            }
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
            if (delegationAgreement == null || (!authorizationService.isCallerAuthorizedToManageDelegationAgreement(delegationAgreement)
                    && !delegationAgreement.isEffectiveDelegate(caller))) {
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
        Audit audit = null;
        try {
            // Verify token exists and valid
            BaseUserToken token = requestContextHolder.getRequestContext().getSecurityContext().getAndVerifyEffectiveCallerTokenAsBaseToken(authToken);

            // Verify token is not a scoped token or delegation token
            if (!StringUtils.isBlank(token.getScope()) || token.isDelegationToken()) {
                throw new ForbiddenException(GlobalConstants.FORBIDDEN_DUE_TO_RESTRICTED_TOKEN, ErrorCodes.ERROR_CODE_FORBIDDEN_ACTION);
            }

            BaseUser caller = requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled();

            // Verify caller has appropriate access
            authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.DEFAULT_USER);

            // Caller must be authorized to revoke roles on DA
            com.rackspace.idm.domain.entity.DelegationAgreement delegationAgreement = delegationService.getDelegationAgreementById(agreementId);
            if (delegationAgreement == null
                    || !authorizationService.isCallerAuthorizedToManageDelegationAgreement(delegationAgreement)) {
                throw new NotFoundException("The specified agreement does not exist for this user", ErrorCodes.ERROR_CODE_NOT_FOUND);
            }

            audit = Audit.log(getDelegationAgreementAuditBuilder(caller, delegationAgreement, roleId).build()).delete();

            delegationService.revokeRoleAssignmentOnDelegationAgreement(delegationAgreement, roleId);
            audit.succeed();
            return Response.noContent().build();
        } catch (Exception ex) {
            if (audit != null) {
                audit.fail();
            }
            LOG.debug(String.format("Error revoking role '%s' from delegation agreement '%s'", roleId, agreementId), ex);
            return exceptionHandler.exceptionResponse(ex).build();
        }
    }

    private DelegationAgreementAuditBuilder getDelegationAgreementAuditBuilder(BaseUser caller, com.rackspace.idm.domain.entity.DelegationAgreement delegationAgreement) {
        DelegationAgreementAuditBuilder delegationAgreementAuditBuilder = new DelegationAgreementAuditBuilder()
                .effectiveCaller(caller.getId())
                .delegationAgreementId(delegationAgreement.getId());

        if(requestContextHolder.getRequestContext().getSecurityContext().isRackerImpersonatedRequest()) {
            delegationAgreementAuditBuilder.caller(((ImpersonatedScopeAccess)requestContextHolder.getRequestContext().getSecurityContext().getCallerToken()).getRackerId());
        }
        return delegationAgreementAuditBuilder;
    }

    private DelegationAgreementAuditBuilder getDelegationAgreementAuditBuilder(EndUser caller, com.rackspace.idm.domain.entity.DelegationAgreement delegationAgreement, DelegateReference delegateReference) {
        return getDelegationAgreementAuditBuilder(caller, delegationAgreement)
                .delegateId(delegateReference.getId())
                .delegateType(delegateReference.getDelegateType().value());
    }

    private DelegationAgreementAuditBuilder getDelegationAgreementAuditBuilder(BaseUser caller, com.rackspace.idm.domain.entity.DelegationAgreement delegationAgreement, RoleAssignments roleAssignments) {
        return getDelegationAgreementAuditBuilder(caller, delegationAgreement).roleAssignments(roleAssignments);
    }

    private DelegationAgreementAuditBuilder getDelegationAgreementAuditBuilder(BaseUser caller, com.rackspace.idm.domain.entity.DelegationAgreement delegationAgreement, String roleId) {
        return getDelegationAgreementAuditBuilder(caller, delegationAgreement).roleId(roleId);
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

        /**
         * This method validates if the effective caller is authorized to create delegation agreements
         * with the specified user or user group as the principal.
         *
         * @return
         */
        public boolean isCallerAuthorizedOnPrincipal() {
            if (principal == null || !validator.isCallerAuthorizedOnPrincipal()) {
                return false;
            }
            return true;
        }

        public void verifyCallerAuthorizedOnPrincipal() {
            if (!isCallerAuthorizedOnPrincipal()) {
                throw new NotFoundException(ERROR_MSG_PRINCIPAL_NOT_FOUND, ErrorCodes.ERROR_CODE_NOT_FOUND);
            }
        }
    }

    private interface PrincipalLookup {
        DelegationPrincipal getPrincipal();
        String getId();
    }

    private interface PrincipalValidator {
        boolean isCallerAuthorizedOnPrincipal();
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
        public boolean isCallerAuthorizedOnPrincipal() {
            if (getPrincipal() == null) {
                return false;
            }

            BaseUser caller = requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled();

            if (!(caller instanceof EndUser)) {
                return false;
            }

            EndUser callerEndUser = (EndUser) caller;

            if (caller.getDomainId().equalsIgnoreCase(getPrincipal().getDomainId())
                    && callerEndUser.getUserGroupDNs().contains(getPrincipal().getDn())) {
                // The caller can create a DA with the specified user group principal if the user
                // is a member of the user group and within the same domain as the user group.
                return true;
            } else if (authorizationService.authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(
                    Collections.singletonList(IdentityRole.RCN_ADMIN.getRoleName()))
                    && domainService.doDomainsShareRcn(caller.getDomainId(), getPrincipal().getDomainId())) {
                // RCN admins can create DAs for any user group principal within their RCN
                return true;
            } else if (authorizationService.authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(
                    IdentityUserTypeEnum.USER_ADMIN.getRoleName(), IdentityUserTypeEnum.USER_MANAGER.getRoleName())
                    && caller.getDomainId().equalsIgnoreCase(getPrincipal().getDomainId())) {
                // User Admins and user managers can create DAs for any user group principal for any user group in their domain
                return true;
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
        public boolean isCallerAuthorizedOnPrincipal() {
            if (getPrincipal() == null) {
                return false;
            }

            BaseUser caller = requestContextHolder.getRequestContext().getAndVerifyEffectiveCallerIsEnabled();

            if (caller.getId().equalsIgnoreCase(getPrincipal().getId())) {
                // Users can always specify themselves as the user principal.
                return true;
            } else if (authorizationService.authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(
                    Collections.singletonList(IdentityRole.RCN_ADMIN.getRoleName()))
                    && domainService.doDomainsShareRcn(caller.getDomainId(), getPrincipal().getDomainId())) {
                // RCN admins can create DAs with any user principal within their RCN
                return true;
            } else if (authorizationService.authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(
                    Collections.singletonList(IdentityUserTypeEnum.USER_ADMIN.getRoleName()))
                    && caller.getDomainId().equalsIgnoreCase(getPrincipal().getDomainId())) {
                // User admins can create a DA for any user principal within their domain.
                return true;
            } else if (authorizationService.authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(
                    Collections.singletonList(IdentityUserTypeEnum.USER_MANAGER.getRoleName()))
                    && caller.getDomainId().equalsIgnoreCase(getPrincipal().getDomainId())) {
                // User managers can create a DA for any non user admin user principal within their domain.
                return !authorizationService.hasUserAdminRole(endUser);
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
