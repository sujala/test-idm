package com.rackspace.idm.api.converter;

import com.rackspace.idm.domain.entity.*;
import org.junit.Before;
import org.junit.Test;

import javax.xml.bind.JAXBElement;
import java.util.Date;

import static org.easymock.EasyMock.verify;
import static org.hamcrest.CoreMatchers.equalTo;

/**
 * Created with IntelliJ IDEA.
 * User: kurt
 * Date: 6/11/12
 * Time: 10:23 AM
 * To change this template use File | Settings | File Templates.
 */
public class AuthConverterTest {
    AuthConverter authConverter;
    private AuthData authData;

    @Before
    public void setUp() throws Exception {
        authConverter = new AuthConverter(new TokenConverter(), new ApplicationConverter(new RolesConverter()), new UserConverter(new RolesConverter()));
        authData = new AuthData();
    }

    @Test
    public void toAuthDataJaxb_withAuthData_setsAccessToken() throws Exception {
        authData.setAccessToken("accessToken");
        JAXBElement<com.rackspace.api.idm.v1.AuthData> authDataJAXBElement = authConverter.toAuthDataJaxb(authData);
        verify("access token", authDataJAXBElement.getValue().getAccessToken().getId(), equalTo("accessToken"));
    }

    @Test
    public void toAuthDataJaxb_withAuthData_setsRefreshToken() throws Exception {
        authData.setRefreshToken("refreshToken");
        JAXBElement<com.rackspace.api.idm.v1.AuthData> authDataJAXBElement = authConverter.toAuthDataJaxb(authData);
        verify("refresh token", authDataJAXBElement.getValue().getRefreshToken().getId(), equalTo("refreshToken"));
    }

    @Test
    public void toAuthDataJaxb_withAuthData_setsApplication() throws Exception {
        authData.setApplication(new Application("clientId", ClientSecret.newInstance("clientSecret"), "name", "customerId", ClientStatus.ACTIVE));
        JAXBElement<com.rackspace.api.idm.v1.AuthData> authDataJAXBElement = authConverter.toAuthDataJaxb(authData);
        verify("application", authDataJAXBElement.getValue().getApplication().getClientId(), equalTo("clientId"));
    }

    @Test
    public void toAuthDataJaxb_withAuthData_setsUser() throws Exception {
        authData.setUser(new User("username"));
        JAXBElement<com.rackspace.api.idm.v1.AuthData> authDataJAXBElement = authConverter.toAuthDataJaxb(authData);
        verify("user", authDataJAXBElement.getValue().getUser().getUsername(), equalTo("username"));
    }

    @Test
    public void toAuthDataJaxb_withAuthData_setsRacker() throws Exception {
        Racker racker = new Racker();
        racker.setRackerId("rackerId");
        authData.setRacker(racker);
        JAXBElement<com.rackspace.api.idm.v1.AuthData> authDataJAXBElement = authConverter.toAuthDataJaxb(authData);
        verify("racker", authDataJAXBElement.getValue().getRacker().getId(), equalTo("rackerId"));
    }

    @Test
    public void toAuthDataJaxb_withAuthData_setsIsPasswordResetOnly() throws Exception {
        authData.setPasswordResetOnlyToken(true);
        JAXBElement<com.rackspace.api.idm.v1.AuthData> authDataJAXBElement = authConverter.toAuthDataJaxb(authData);
        verify("password reset only", authDataJAXBElement.getValue().isIsPasswordResetOnlyToken(), equalTo(true));
    }

//    @Test
//    public void toAuthDataJaxb_withAuthData_setsRefreshToken() throws Exception {
//        authData.setRefreshToken("refreshToken");
//        JAXBElement<com.rackspace.api.idm.v1.AuthData> authDataJAXBElement = authConverter.toAuthDataJaxb(authData);
//        verify("refresh token", authDataJAXBElement.getValue().getRefreshToken().getId(), equalTo("refreshToken"));
//    }
}
