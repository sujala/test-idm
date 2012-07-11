package com.rackspace.idm.api.resource;

import com.rackspace.idm.api.resource.application.ApplicationsResource;
import com.rackspace.idm.api.resource.cloud.CloudVersionsResource;
import com.rackspace.idm.api.resource.customeridentityprofile.CustomerIdentityProfilesResource;
import com.rackspace.idm.api.resource.passwordrule.PasswordRulesResource;
import com.rackspace.idm.api.resource.roles.RolesResource;
import com.rackspace.idm.api.resource.tenant.TenantsResource;
import com.rackspace.idm.api.resource.token.TokensResource;
import com.rackspace.idm.api.resource.user.RackerResource;
import com.rackspace.idm.api.resource.user.UsersResource;
import com.rackspace.idm.api.serviceprofile.CanonicalContractDescriptionBuilder;
import com.rackspace.idm.domain.service.ApiDocService;
import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Created with IntelliJ IDEA.
 * User: Yung Lai
 * Date: 6/18/12
 * Time: 10:41 AM
 * To change this template use File | Settings | File Templates.
 */
public class Version10ResourceTest {
    private Version10Resource version10Resource;
    private ApiDocService apiDocService;
    private RolesResource rolesResource;
    private UsersResource usersResource;
    private CustomerIdentityProfilesResource customerIdentityProfilesResource;
    private PasswordRulesResource passwordRulesResource;
    private TokensResource tokenResource;
    private ApplicationsResource applicationsResource;
    private CanonicalContractDescriptionBuilder canonicalContractDescriptionBuilder;
    private TenantsResource tenantsResource;
    private RackerResource rackerResource;
    private CloudVersionsResource cloudVersionsResource;
    private Configuration config;

    @Before
    public void setUp() throws Exception {
        apiDocService = mock(ApiDocService.class);
        rolesResource = mock(RolesResource.class);
        usersResource = mock(UsersResource.class);
        customerIdentityProfilesResource = mock(CustomerIdentityProfilesResource.class);
        passwordRulesResource = mock(PasswordRulesResource.class);
        tokenResource = mock(TokensResource.class);
        applicationsResource = mock(ApplicationsResource.class);
        canonicalContractDescriptionBuilder = mock(CanonicalContractDescriptionBuilder.class);
        tenantsResource = mock(TenantsResource.class);
        rackerResource = mock(RackerResource.class);
        cloudVersionsResource = mock(CloudVersionsResource.class);
        config = mock(Configuration.class);
        version10Resource = new Version10Resource(usersResource, customerIdentityProfilesResource, passwordRulesResource, tokenResource, rolesResource,
                cloudVersionsResource, apiDocService, config, canonicalContractDescriptionBuilder, applicationsResource, tenantsResource, rackerResource);
    }

    @Test
    public void getInternalVersionInfo_callsCanonicalContractDescriptionBuilder_buildInternalVersionPage() throws Exception {
        version10Resource.getInternalVersionInfo("versionId");
        verify(canonicalContractDescriptionBuilder).buildInternalVersionPage(eq("versionId"), any(UriInfo.class));
    }

    @Test
    public void getInternalVersionInfo_responseOk_returns200() throws Exception {
        Response response = version10Resource.getInternalVersionInfo("versionId");
        assertThat("response code", response.getStatus(), equalTo(200));
    }

    @Test
    public void getPublicVersionInfo_callsCanonicalContractDescriptionBuilder_buildPublicVersionPage() throws Exception {
        version10Resource.getPublicVersionInfo("versionId");
        verify(canonicalContractDescriptionBuilder).buildPublicVersionPage(eq("versionId"), any(UriInfo.class));
    }

    @Test
    public void getPublicVersionInfo_responseOk_returns200() throws Exception {
        Response response = version10Resource.getPublicVersionInfo("versionId");
        assertThat("response code", response.getStatus(), equalTo(200));
    }

    @Test
    public void getCustomerIdentityProfileResource_returnsCustomerIdentityProfileResource() throws Exception {
        CustomerIdentityProfilesResource resource = version10Resource.getCustomerIdentityProfilesResource();
        assertThat("customer identity profile resource", resource, equalTo(customerIdentityProfilesResource));
    }

    @Test
    public void getUsersResource_returnsUsersResource() throws Exception {
        UsersResource resource = version10Resource.getUsersResource();
        assertThat("users resource", resource, equalTo(usersResource));
    }

    @Test
    public void getPasswordRulesResource_returnsPasswordRulesResource() throws Exception {
        PasswordRulesResource resource = version10Resource.getPasswordRulesResource();
        assertThat("password rules resource", resource, equalTo(passwordRulesResource));
    }

    @Test
    public void getTokensResource_returnsTokensResource() throws Exception {
        TokensResource resource = version10Resource.getTokenResource();
        assertThat("token resource", resource, equalTo(tokenResource));
    }

    @Test
    public void getTenantsResource_returnsTenantsResource() throws Exception {
        TenantsResource resource = version10Resource.getTenantResource();
        assertThat("tenants resource", resource, equalTo(tenantsResource));
    }

    @Test
    public void getApplicationsResource_returnsApplicationsResource() throws Exception {
        ApplicationsResource resource = version10Resource.getApplicationsResource();
        assertThat("applications resource", resource, equalTo(applicationsResource));
    }

    @Test
    public void getRolesResource_returnsRolesResource() throws Exception {
        RolesResource resource = version10Resource.getRolesResource();
        assertThat("roles resource", resource, equalTo(rolesResource));
    }

    @Test
    public void getRackerResource_returnsRackerResource() throws Exception {
        RackerResource resource = version10Resource.getRackerResource();
        assertThat("racker resource", resource, equalTo(rackerResource));
    }

    @Test
    public void getWadl_callsApiDocService_getWadl() throws Exception {
        version10Resource.getWadl();
        verify(apiDocService).getWadl();
    }

    @Test
    public void getWadl_responseOk_returns200() throws Exception {
        Response response = version10Resource.getWadl();
        assertThat("response code", response.getStatus(), equalTo(200));
    }
}
