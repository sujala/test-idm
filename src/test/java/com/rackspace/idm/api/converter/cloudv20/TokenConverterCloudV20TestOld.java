package com.rackspace.idm.api.converter.cloudv20;

import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import com.rackspace.idm.domain.entity.RackerScopeAccess;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.TenantRole;
import org.junit.Before;
import org.junit.Test;
import org.openstack.docs.identity.api.v2.TenantForAuthenticateResponse;
import org.openstack.docs.identity.api.v2.Token;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Created with IntelliJ IDEA.
 * User: Yung Lai
 * Date: 6/15/12
 * Time: 9:25 AM
 * To change this template use File | Settings | File Templates.
 */
public class TokenConverterCloudV20TestOld {
    private TokenConverterCloudV20 tokenConverterCloudV20;
    private JAXBObjectFactories jaxbObjectFactories;

    @Before
    public void setUp() throws Exception {
        tokenConverterCloudV20 = new TokenConverterCloudV20();
        jaxbObjectFactories = new JAXBObjectFactories();
        tokenConverterCloudV20.setObjFactories(jaxbObjectFactories);
    }

    @Test
    public void toToken_onlyScopeAccessParam_returnsToken() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        String tokenString = UUID.randomUUID().toString().replace("-", "");
        scopeAccess.setAccessTokenExp(new Date());
        scopeAccess.setAccessTokenString(tokenString);
        Token token = tokenConverterCloudV20.toToken(scopeAccess, null);
        assertThat("id", token.getId(), equalTo(tokenString));
    }

    @Test
    public void toToken_rolesIsNull_returnsToken() throws Exception {
        RackerScopeAccess scopeAccess = new RackerScopeAccess();
        scopeAccess.setAccessTokenString("accessToken");
        scopeAccess.setAccessTokenExp(new Date(3000, 1, 1));
        Token token = tokenConverterCloudV20.toToken(scopeAccess, null);
        assertThat("id", token.getId(), equalTo("accessToken"));
    }

    @Test
    public void toTenantForAuthenticateResponse_tenantNameNotMatch_returnsNull() throws Exception {
        TenantRole tenantRole = new TenantRole();
        tenantRole.setName("notMatch");
        List<TenantRole> tenantRoleList = new ArrayList<TenantRole>();
        tenantRoleList.add(tenantRole);
        TenantForAuthenticateResponse response = tokenConverterCloudV20.toTenantForAuthenticateResponse(tenantRoleList);
        assertThat("tenant", response, equalTo(null));
    }

    @Test
    public void toTenantForAuthenticateResponse_tenantNameMatch_returnsTenantResponse() throws Exception {
        TenantRole tenantRole = new TenantRole();
        tenantRole.setName("compute:default");
        tenantRole.getTenantIds().add("id");
        List<TenantRole> tenantRoleList = new ArrayList<TenantRole>();
        tenantRoleList.add(tenantRole);
        TenantForAuthenticateResponse response = tokenConverterCloudV20.toTenantForAuthenticateResponse(tenantRoleList);
        assertThat("response tenant id", response.getId(), equalTo("id"));
    }
}
