package org.openstack.docs.identity.api.v2;

import org.junit.Before;
import org.junit.Test;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Created with IntelliJ IDEA.
 * User: Yung Lai
 * Date: 7/5/12
 * Time: 1:46 PM
 * To change this template use File | Settings | File Templates.
 */
public class ObjectFactoryTest {
    ObjectFactory objectFactory;
    UserForAuthenticateResponse userForAuthenticateResponse;
    UserList userList;
    Tenants tenants;
    EndpointList endpointList;
    AuthenticateResponse authenticateResponse;
    Endpoint endpoint;
    OverLimitFault overLimitFault;
    Tenant tenant;
    IdentityFault identityFault;
    RoleList roleList;
    Role role;
    CredentialListType credentialListType;
    AuthenticationRequest authenticationRequest;
    TenantForAuthenticateResponse tenantForAuthenticateResponse;
    VersionForService versionForService;
    EndpointForService endpointForService;   
    ServiceCatalog serviceCatalog;
    Token token;
    ServiceForCatalog serviceForCatalog;
    PasswordCredentialsBase passwordCredentialsBase;

    @Before
    public void setUp() throws Exception {
        objectFactory = new ObjectFactory();

        // Objects that gets tested
        userForAuthenticateResponse = objectFactory.createUserForAuthenticateResponse();
        userList = objectFactory.createUserList();
        tenants = objectFactory.createTenants();
        endpointList = objectFactory.createEndpointList();
        authenticateResponse = objectFactory.createAuthenticateResponse();
        endpoint = objectFactory.createEndpoint();
        overLimitFault = objectFactory.createOverLimitFault();
        tenant = objectFactory.createTenant();
        identityFault = objectFactory.createIdentityFault();
        roleList = objectFactory.createRoleList();
        role = objectFactory.createRole();
        credentialListType = objectFactory.createCredentialListType();
        authenticationRequest = objectFactory.createAuthenticationRequest();
        tenantForAuthenticateResponse = objectFactory.createTenantForAuthenticateResponse();
        versionForService = objectFactory.createVersionForService();
        endpointForService = objectFactory.createEndpointForService();
        serviceCatalog = objectFactory.createServiceCatalog();
        token = objectFactory.createToken();
        serviceForCatalog = objectFactory.createServiceForCatalog();
        passwordCredentialsBase = objectFactory.createPasswordCredentialsRequiredUsername();
    }

    @Test
    public void userForAuthenticateResponse_callsGetAny_returnsEmptyList() throws Exception {
        List<Object> result = userForAuthenticateResponse.getAny();
        assertThat("list", result.isEmpty(), equalTo(true));
    }

    @Test
    public void userForAuthenticateResponse_callsGetAny_returnsExistingList() throws Exception {
        List<Object> any = userForAuthenticateResponse.getAny();
        any.add("test");
        List<Object> result = userForAuthenticateResponse.getAny();
        assertThat("list", result.get(0).toString(), equalTo("test"));
    }

    @Test
    public void userForAuthenticateResponse_callsGetOtherAttributes_returnsOtherAttributes() throws Exception {
        Map<QName,String> result = userForAuthenticateResponse.getOtherAttributes();
        assertThat("list", result.isEmpty(), equalTo(true));
    }

    @Test
    public void userList_callsGetAny_returnsEmptyList() throws Exception {
        List<Object> result = userList.getAny();
        assertThat("list", result.isEmpty(), equalTo(true));
    }

    @Test
    public void userList_callsGetAny_returnsExistingList() throws Exception {
        List<Object> any = userList.getAny();
        any.add("test");
        List<Object> result = userList.getAny();
        assertThat("list", result.get(0).toString(), equalTo("test"));
    }

    @Test
    public void userList_callsGetOtherAttributes_returnsOtherAttributes() throws Exception {
        Map<QName,String> result = userList.getOtherAttributes();
        assertThat("list", result.isEmpty(), equalTo(true));
    }

    @Test
    public void tenants_callsGetAny_returnsEmptyList() throws Exception {
        List<Object> result = tenants.getAny();
        assertThat("list", result.isEmpty(), equalTo(true));
    }

    @Test
    public void tenants_callsGetAny_returnsExistingList() throws Exception {
        List<Object> any = tenants.getAny();
        any.add("test");
        List<Object> result = tenants.getAny();
        assertThat("list", result.get(0).toString(), equalTo("test"));
    }

    @Test
    public void tenants_callsGetOtherAttributes_returnsOtherAttributes() throws Exception {
        Map<QName,String> result = tenants.getOtherAttributes();
        assertThat("list", result.isEmpty(), equalTo(true));
    }

    @Test
    public void endpointList_callsGetAny_returnsEmptyList() throws Exception {
        List<Object> result = endpointList.getAny();
        assertThat("list", result.isEmpty(), equalTo(true));
    }

    @Test
    public void endpointList_callsGetAny_returnsExistingList() throws Exception {
        List<Object> any = endpointList.getAny();
        any.add("test");
        List<Object> result = endpointList.getAny();
        assertThat("list", result.get(0).toString(), equalTo("test"));
    }

    @Test
    public void endpointList_callsGetOtherAttributes_returnsOtherAttributes() throws Exception {
        Map<QName,String> result = endpointList.getOtherAttributes();
        assertThat("list", result.isEmpty(), equalTo(true));
    }

    @Test
    public void authenticateResponse_callsGetOtherAttributes_returnsOtherAttributes() throws Exception {
        Map<QName,String> result = authenticateResponse.getOtherAttributes();
        assertThat("list", result.isEmpty(), equalTo(true));
    }

    @Test
    public void endpoint_callsGetAny_returnsEmptyList() throws Exception {
        List<Object> result = endpoint.getAny();
        assertThat("list", result.isEmpty(), equalTo(true));
    }

    @Test
    public void endpoint_callsGetAny_returnsExistingList() throws Exception {
        List<Object> any = endpoint.getAny();
        any.add("test");
        List<Object> result = endpoint.getAny();
        assertThat("list", result.get(0).toString(), equalTo("test"));
    }

    @Test
    public void endpoint_callsGetOtherAttributes_returnsOtherAttributes() throws Exception {
        Map<QName,String> result = endpoint.getOtherAttributes();
        assertThat("list", result.isEmpty(), equalTo(true));
    }

    @Test
    public void overLimitFault_setRetryAndGetRetry_returnsRetryValue() throws Exception {
        overLimitFault.setRetryAt(null);
        XMLGregorianCalendar result = overLimitFault.getRetryAt();
        assertThat("retry value", result, equalTo(null));
    }

    @Test
    public void createServiceUnavailableFault_returnsNewCreatedObject() throws Exception {
        ServiceUnavailableFault result = objectFactory.createServiceUnavailableFault();
        assertThat("fault", result.code, equalTo(0));
    }

    @Test
    public void tenant_callsGetAny_returnsEmptyList() throws Exception {
        List<Object> result = tenant.getAny();
        assertThat("list", result.isEmpty(), equalTo(true));
    }

    @Test
    public void tenant_callsGetAny_returnsExistingList() throws Exception {
        List<Object> any = tenant.getAny();
        any.add("test");
        List<Object> result = tenant.getAny();
        assertThat("list", result.get(0).toString(), equalTo("test"));
    }

    @Test
    public void tenant_callsGetOtherAttributes_returnsOtherAttributes() throws Exception {
        Map<QName,String> result = tenant.getOtherAttributes();
        assertThat("list", result.isEmpty(), equalTo(true));
    }

    @Test
    public void identityFault_callsGetAny_returnsEmptyList() throws Exception {
        List<Object> result = identityFault.getAny();
        assertThat("list", result.isEmpty(), equalTo(true));
    }

    @Test
    public void identityFault_callsGetAny_returnsExistingList() throws Exception {
        List<Object> any = identityFault.getAny();
        any.add("test");
        List<Object> result = identityFault.getAny();
        assertThat("list", result.get(0).toString(), equalTo("test"));
    }

    @Test
    public void identityFault_callsGetOtherAttributes_returnsOtherAttributes() throws Exception {
        Map<QName,String> result = identityFault.getOtherAttributes();
        assertThat("list", result.isEmpty(), equalTo(true));
    }

    @Test
    public void identityFault_callsGetCode_returnsCode() throws Exception {
        int result = identityFault.getCode();
        assertThat("code", result, equalTo(0));
    }

    @Test
    public void identityFault_callsGetDetails_returnsDetails() throws Exception {
        String result = identityFault.getDetails();
        assertThat("details", result, equalTo(null));
    }

    @Test
    public void roleList_callsGetAny_returnsEmptyList() throws Exception {
        List<Object> result = roleList.getAny();
        assertThat("list", result.isEmpty(), equalTo(true));
    }

    @Test
    public void roleList_callsGetAny_returnsExistingList() throws Exception {
        List<Object> any = roleList.getAny();
        any.add("test");
        List<Object> result = roleList.getAny();
        assertThat("list", result.get(0).toString(), equalTo("test"));
    }

    @Test
    public void roleList_callsGetOtherAttributes_returnsOtherAttributes() throws Exception {
        Map<QName,String> result = roleList.getOtherAttributes();
        assertThat("list", result.isEmpty(), equalTo(true));
    }

    @Test
    public void role_callsGetAny_returnsEmptyList() throws Exception {
        List<Object> result = role.getAny();
        assertThat("list", result.isEmpty(), equalTo(true));
    }

    @Test
    public void role_callsGetAny_returnsExistingList() throws Exception {
        List<Object> any = role.getAny();
        any.add("test");
        List<Object> result = role.getAny();
        assertThat("list", result.get(0).toString(), equalTo("test"));
    }

    @Test
    public void role_callsGetOtherAttributes_returnsOtherAttributes() throws Exception {
        Map<QName,String> result = role.getOtherAttributes();
        assertThat("list", result.isEmpty(), equalTo(true));
    }

    @Test
    public void passwordCredentialsBase_callsGetAny_returnsEmptyList() throws Exception {
        List<Object> result = passwordCredentialsBase.getAny();
        assertThat("list", result.isEmpty(), equalTo(true));
    }

    @Test
    public void passwordCredentialsBase_callsGetAny_returnsExistingList() throws Exception {
        List<Object> any = passwordCredentialsBase.getAny();
        any.add("test");
        List<Object> result = passwordCredentialsBase.getAny();
        assertThat("list", result.get(0).toString(), equalTo("test"));
    }

    @Test
    public void passwordCredentialsBase_callsGetOtherAttributes_returnsOtherAttributes() throws Exception {
        Map<QName,String> result = passwordCredentialsBase.getOtherAttributes();
        assertThat("list", result.isEmpty(), equalTo(true));
    }

    @Test
    public void credentialListType_callsGetOtherAttributes_returnsOtherAttributes() throws Exception {
        Map<QName,String> result = credentialListType.getOtherAttributes();
        assertThat("list", result.isEmpty(), equalTo(true));
    }

    @Test
    public void authenticationRequest_callsGetAny_returnsEmptyList() throws Exception {
        List<Object> result = authenticationRequest.getAny();
        assertThat("list", result.isEmpty(), equalTo(true));
    }

    @Test
    public void authenticationRequest_callsGetAny_returnsExistingList() throws Exception {
        List<Object> any = authenticationRequest.getAny();
        any.add("test");
        List<Object> result = authenticationRequest.getAny();
        assertThat("list", result.get(0).toString(), equalTo("test"));
    }

    @Test
    public void authenticationRequest_callsGetOtherAttributes_returnsOtherAttributes() throws Exception {
        Map<QName,String> result = authenticationRequest.getOtherAttributes();
        assertThat("list", result.isEmpty(), equalTo(true));
    }

    @Test
    public void tenantForAuthenticateResponse_callsGetOtherAttributes_returnsOtherAttributes() throws Exception {
        Map<QName,String> result = tenantForAuthenticateResponse.getOtherAttributes();
        assertThat("list", result.isEmpty(), equalTo(true));
    }

    @Test
    public void versionForService_callsGetOtherAttributes_returnsOtherAttributes() throws Exception {
        Map<QName,String> result = versionForService.getOtherAttributes();
        assertThat("list", result.isEmpty(), equalTo(true));
    }

    @Test
    public void createPasswordCredentialsBase_returnsNewCreatedObject() throws Exception {
        PasswordCredentialsBase result = objectFactory.createPasswordCredentialsBase();
        assertThat("passwrord credential base", result.password, equalTo(null));
    }

    @Test
    public void endpointForService_callsGetAny_returnsEmptyList() throws Exception {
        List<Object> result = endpointForService.getAny();
        assertThat("list", result.isEmpty(), equalTo(true));
    }

    @Test
    public void endpointForService_callsGetAny_returnsExistingList() throws Exception {
        List<Object> any = endpointForService.getAny();
        any.add("test");
        List<Object> result = endpointForService.getAny();
        assertThat("list", result.get(0).toString(), equalTo("test"));
    }

    @Test
    public void endpointForService_callsGetOtherAttributes_returnsOtherAttributes() throws Exception {
        Map<QName,String> result = endpointForService.getOtherAttributes();
        assertThat("list", result.isEmpty(), equalTo(true));
    }

    @Test
    public void serviceCatalog_callsGetAny_returnsEmptyList() throws Exception {
        List<Object> result = serviceCatalog.getAny();
        assertThat("list", result.isEmpty(), equalTo(true));
    }

    @Test
    public void serviceCatalog_callsGetAny_returnsExistingList() throws Exception {
        List<Object> any = serviceCatalog.getAny();
        any.add("test");
        List<Object> result = serviceCatalog.getAny();
        assertThat("list", result.get(0).toString(), equalTo("test"));
    }

    @Test
    public void serviceCatalog_callsGetOtherAttributes_returnsOtherAttributes() throws Exception {
        Map<QName,String> result = serviceCatalog.getOtherAttributes();
        assertThat("list", result.isEmpty(), equalTo(true));
    }

    @Test
    public void createPasswordCredentialsWithoutUsername_createNewCreatedObject() throws Exception {
        PasswordCredentialsWithoutUsername result = objectFactory.createPasswordCredentialsWithoutUsername();
        assertThat("password cred without username", result.password, equalTo(null));
    }

    @Test
    public void token_callsGetAny_returnsEmptyList() throws Exception {
        List<Object> result = token.getAny();
        assertThat("list", result.isEmpty(), equalTo(true));
    }

    @Test
    public void token_callsGetAny_returnsExistingList() throws Exception {
        List<Object> any = token.getAny();
        any.add("test");
        List<Object> result = token.getAny();
        assertThat("list", result.get(0).toString(), equalTo("test"));
    }

    @Test
    public void token_callsGetOtherAttributes_returnsOtherAttributes() throws Exception {
        Map<QName,String> result = token.getOtherAttributes();
        assertThat("list", result.isEmpty(), equalTo(true));
    }

    @Test
    public void createTokenForAuthenticationRequest_returnsNewCreatedObject() throws Exception {
        TokenForAuthenticationRequest result = objectFactory.createTokenForAuthenticationRequest();
        assertThat("token for auth request", result.id, equalTo(null));
    }

    @Test
    public void serviceForCatalog_callsGetAny_returnsEmptyList() throws Exception {
        List<Object> result = serviceForCatalog.getAny();
        assertThat("list", result.isEmpty(), equalTo(true));
    }

    @Test
    public void serviceForCatalog_callsGetAny_returnsExistingList() throws Exception {
        List<Object> any = serviceForCatalog.getAny();
        any.add("test");
        List<Object> result = serviceForCatalog.getAny();
        assertThat("list", result.get(0).toString(), equalTo("test"));
    }

    @Test
    public void serviceForCatalog_callsGetOtherAttributes_returnsOtherAttributes() throws Exception {
        Map<QName,String> result = serviceForCatalog.getOtherAttributes();
        assertThat("list", result.isEmpty(), equalTo(true));
    }

    @Test
    public void createServiceUnavailable_createsNewJAXBElementObject() throws Exception {
        ServiceUnavailableFault serviceUnavailableFault = new ServiceUnavailableFault();
        serviceUnavailableFault.setMessage("test");
        JAXBElement<ServiceUnavailableFault> result = objectFactory.createServiceUnavailable(serviceUnavailableFault);
        assertThat("message", result.getValue().getMessage(), equalTo("test"));
    }

    @Test
    public void createOverLimitFault_createsNewJAXBElementObject() throws Exception {
        OverLimitFault overLimitFault = new OverLimitFault();
        overLimitFault.setMessage("test");
        JAXBElement<OverLimitFault> result = objectFactory.createOverLimit(overLimitFault);
        assertThat("message", result.getValue().getMessage(), equalTo("test"));
    }
}
