package com.rackspace.idm.api.converter.cloudv20;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.AuthenticatedBy;
import com.rackspace.idm.GlobalConstants;
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.ServiceCatalogInfo;
import com.rackspace.idm.domain.service.TenantService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.openstack.docs.identity.api.v2.TenantForAuthenticateResponse;
import org.openstack.docs.identity.api.v2.Token;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.GregorianCalendar;
import java.util.List;

@Component
public class TokenConverterCloudV20 {

    @Autowired
    private JAXBObjectFactories objFactories;

    private Logger logger = LoggerFactory.getLogger(TokenConverterCloudV20.class);

    private Token toTokenInternal(ScopeAccess scopeAccess, String tokenTenantId) {
        Token token = objFactories.getOpenStackIdentityV2Factory().createToken();

        if (scopeAccess != null) {
            GregorianCalendar gc = new GregorianCalendar();
            gc.setTime(scopeAccess.getAccessTokenExp());

            XMLGregorianCalendar expiresDate = null;
            try {
                expiresDate = DatatypeFactory.newInstance().newXMLGregorianCalendar(gc);
            } catch (DatatypeConfigurationException e) {
                logger.info("failed to create XMLGregorianCalendar: " + e.getMessage());
            }

            token.setId(scopeAccess.getAccessTokenString());
            token.setExpires(expiresDate);
            if (StringUtils.isNotBlank(tokenTenantId)) {
                TenantForAuthenticateResponse tenantForAuthenticateResponse = new TenantForAuthenticateResponse();
                tenantForAuthenticateResponse.setId(tokenTenantId);
                tenantForAuthenticateResponse.setName(tokenTenantId);
                token.setTenant(tenantForAuthenticateResponse);
            }

            if (scopeAccess.getAuthenticatedBy().size() > 0) {
                AuthenticatedBy authenticatedByEntity = objFactories.getRackspaceIdentityExtRaxgaV1Factory().createAuthenticatedBy();
                for (String authenticatedBy : scopeAccess.getAuthenticatedBy()) {
                    authenticatedByEntity.getCredential().add(authenticatedBy);
                }

                token.setAuthenticatedBy(authenticatedByEntity);
            }
        }

        return token;
    }

    public Token toToken(ScopeAccess scopeAccess, List<TenantRole> roles) {
        String tokenTenantId = calculateCloudTenantByRoles(roles);
        return toTokenInternal(scopeAccess, tokenTenantId);
    }

    public Token toRackerToken(ScopeAccess scopeAccess) {
        return toTokenInternal(scopeAccess, null);
    }

    public Token toEndUserToken(ScopeAccess scopeAccess, EndUser user, ServiceCatalogInfo scInfo) {
        Tenant tokenTenant;
        if (scInfo.wereRcnRolesAppliedForCatalog()) {
            tokenTenant = findDefaultTenantForUser(user, scInfo);
        } else {
            tokenTenant = findLegacyDefaultTenantForUser(user, scInfo);
        }
        String tokenTenantId = tokenTenant != null ? tokenTenant.getTenantId() : null;
        return toTokenInternal(scopeAccess, tokenTenantId);
    }

        /**
         * This service finds the default tenant for the user based on the roles assigned and tenants to which the user has
         * access based on prioritized rules:
         * 1. The tenant, within the user's own domain, on which the user has the 'compute:default' role
         * 2. No tenant
         *
         * @return
         */
    private Tenant findDefaultTenantForUser(BaseUser user, ServiceCatalogInfo scInfo) {
        Tenant defaultTenant = null;

        /*
        If the user has no access to any tenants or no domain, a tenant can not be returned.
         */
        if (user != null && scInfo != null && CollectionUtils.isNotEmpty(scInfo.getUserTenants())
                && StringUtils.isNotBlank(user.getDomainId())) {
            TenantRole assignedComputeRole = scInfo.findAssignedRoleByName(GlobalConstants.COMPUTE_DEFAULT_ROLE);

            // If the role isn't assigned to any tenants, can't be returned
            if (assignedComputeRole != null && CollectionUtils.isNotEmpty(assignedComputeRole.getTenantIds())) {
                for (String tenantId : assignedComputeRole.getTenantIds()) {
                    Tenant tenant = scInfo.findUserTenantById(tenantId);

                    // Tenant must exist within user's domain
                    if (tenant != null && user.getDomainId().equals(tenant.getDomainId())) {
                        defaultTenant = tenant;
                    }
                }
            }
        }
        return defaultTenant;
    }

    /**
     * This service finds the default tenant for the user based on the roles assigned and tenants to which the user has
     * access based on legacy requirements based on the prioritized rules:
     * 1. The tenant, within *any* domain, on which the user has the 'compute:default' role
     * 2. The tenant, within *any* domain, that is all numeric
     * 2. No tenant
     *
     * @return
     */
    private Tenant findLegacyDefaultTenantForUser(BaseUser user, ServiceCatalogInfo scInfo) {
        Tenant defaultTenant = null;

        /*
        If the user has no access to any tenants, a tenant can not be returned.
         */
        if (user != null && scInfo != null && CollectionUtils.isNotEmpty(scInfo.getUserTenants())) {
            TenantRole assignedComputeRole = scInfo.findAssignedRoleByName(GlobalConstants.COMPUTE_DEFAULT_ROLE);

            // If the role isn't assigned to any tenants, can't be returned
            if (assignedComputeRole != null && CollectionUtils.isNotEmpty(assignedComputeRole.getTenantIds())) {
                for (String tenantId : assignedComputeRole.getTenantIds()) {
                    Tenant tenant = scInfo.findUserTenantById(tenantId);
                    if (tenant != null) {
                        defaultTenant = tenant;
                    }
                }
            }

            // Search by role didn't provide result, fallback to parsing tenantId of accessible tenants
            if (defaultTenant == null) {
                for (Tenant tenant : scInfo.getUserTenants()) {
                    if (tenant.getTenantId().matches("\\d+")) {
                        defaultTenant = tenant;
                    }
                }
            }
        }

        return defaultTenant;
    }


    private String calculateCloudTenantByRoles(List<TenantRole> tenantRoleList) {
        String result = null;

        if (CollectionUtils.isNotEmpty(tenantRoleList)) {
            for (TenantRole tenantRole : tenantRoleList) {
                // TODO: This is to match the Mosso Type. This is a hack!!
                // trying to identify the mosso tenant, and any tenant that has the role "compute:default"
                // is deemed the mosso tenant. In the future we will get rid of this completely by allowing
                // multiple tenants in the token response. We are stuck because the contract controlled by openstack.
                if (tenantRole.getName().equals(GlobalConstants.COMPUTE_DEFAULT_ROLE) && !tenantRole.getTenantIds().isEmpty()) {
                    result = tenantRole.getTenantIds().iterator().next();
                }
            }

            //Another terrible hack. Check above sees if you have the "compute:default" role assigned to you.
            //The tenant for that role is assumed to be the "mosso tenant". Because of restrictions in the
            //keystone contract, we can only display one tenant, and Rackspace assumes this tenant is the mosso
            //tenant. If a user does not have that specific role, use other default horrible logic which is mosso tenant
            //is numerical while nast tenant is a string. Currently users are restricted to those two tenants.
            for (TenantRole tenantRole : tenantRoleList) {
                for (String tenantId : tenantRole.getTenantIds()) {
                    if (tenantId.matches("\\d+")) {
                        result = tenantId;
                    }
                }
            }
        }

        return result;
    }
}
