package com.rackspace.idm.api.converter.cloudv20;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.AuthenticatedBy;
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.TenantRole;
import org.dozer.Mapper;
import org.openstack.docs.identity.api.v2.TenantForAuthenticateResponse;
import org.openstack.docs.identity.api.v2.Token;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

@Component
public class TokenConverterCloudV20 {
    @Autowired
    Mapper mapper;


    @Autowired
    private JAXBObjectFactories objFactories;
    private Logger logger = LoggerFactory.getLogger(TokenConverterCloudV20.class);

    public Token toToken(ScopeAccess scopeAccess) {
        return toToken(scopeAccess, null);
    }

    public Token toToken(ScopeAccess scopeAccess, List<TenantRole> roles) {
        Token token = objFactories.getOpenStackIdentityV2Factory().createToken();

        if (scopeAccess != null) {

            token.setId(scopeAccess.getAccessTokenString());

            if (roles != null) {
                token.setTenant(toTenantForAuthenticateResponse(roles));
            }
            Date expires = scopeAccess.getAccessTokenExp();
            GregorianCalendar gc = new GregorianCalendar();
            gc.setTime(expires);

            XMLGregorianCalendar expiresDate = null;
            try {
                expiresDate = DatatypeFactory.newInstance().newXMLGregorianCalendar(gc);
            } catch (DatatypeConfigurationException e) {
                logger.info("failed to create XMLGregorianCalendar: " + e.getMessage());
            }
            token.setExpires(expiresDate);

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
            // TODO: Check for other names? This is to match the Mosso Type
            if (tenant.getName().equals("compute:default") && !tenant.getTenantIds().isEmpty()) {
                TenantForAuthenticateResponse tenantForAuthenticateResponse = new TenantForAuthenticateResponse();
                tenantForAuthenticateResponse.setId(tenant.getTenantIds().iterator().next());
                tenantForAuthenticateResponse.setName(tenant.getTenantIds().iterator().next());
                return tenantForAuthenticateResponse;
            }
        }
        return null;
    }

    //TODO: This is if the XSD is to be a list for tenants
    /*
    private List<TenantForAuthenticateResponse> toTenantsForAuthenticateResponse(List<TenantRole> tenantRoleList) {
        List<TenantForAuthenticateResponse> tenantForAuthenticateResponseList = new ArrayList<TenantForAuthenticateResponse>();

        for(TenantRole tenant : tenantRoleList) {
            if(tenant.getTenantIds() != null) {
                String[] tenantIds = tenant.getTenantIds();
                for(int i=0; i < tenantIds.length; i++){
                    TenantForAuthenticateResponse tenantForAuthenticateResponse = new TenantForAuthenticateResponse();
                    tenantForAuthenticateResponse.setId(tenantIds[i]);
                    tenantForAuthenticateResponse.setName(tenantIds[i]);
                    tenantForAuthenticateResponseList.add(tenantForAuthenticateResponse);
                }
            }
        }
        return tenantForAuthenticateResponseList;
    }
    */

    public void setObjFactories(JAXBObjectFactories objFactories) {
        this.objFactories = objFactories;
    }
}
