package com.rackspace.idm.api.converter.cloudv20;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.AuthenticatedBy;
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.TenantRole;
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

    public Token toToken(ScopeAccess scopeAccess, List<TenantRole> roles) {
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

            if (roles != null) {
                token.setTenant(toTenantForAuthenticateResponse(roles));
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

    // TODO: Used for single tenant (mosso) in the response, future may be a list -- see below
    TenantForAuthenticateResponse toTenantForAuthenticateResponse(List<TenantRole> tenantRoleList) {
        for (TenantRole tenant : tenantRoleList) {
            // TODO: Check for other names? This is to match the Mosso Type. This is a hack!!
            // trying to identify the mosso tenant, and any tenant that has the role "compute:default"
            // is deemed the mosso tenant. In the future we will get rid of this completely by allowing
            // multiple tenants in the token response. We are stuck because the contract controlled by openstack.
            if (tenant.getName().equals("compute:default") && !tenant.getTenantIds().isEmpty()) {
                TenantForAuthenticateResponse tenantForAuthenticateResponse = new TenantForAuthenticateResponse();
                tenantForAuthenticateResponse.setId(tenant.getTenantIds().iterator().next());
                tenantForAuthenticateResponse.setName(tenant.getTenantIds().iterator().next());
                return tenantForAuthenticateResponse;
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
                    TenantForAuthenticateResponse tenantForAuthenticateResponse = new TenantForAuthenticateResponse();
                    tenantForAuthenticateResponse.setId(tenantId);
                    tenantForAuthenticateResponse.setName(tenantId);
                    return tenantForAuthenticateResponse;
                }
            }
        }

        return null;
    }

    public void setObjFactories(JAXBObjectFactories objFactories) {
        this.objFactories = objFactories;
    }
}
