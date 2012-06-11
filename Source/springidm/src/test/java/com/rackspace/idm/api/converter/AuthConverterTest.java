package com.rackspace.idm.api.converter;

import com.rackspace.idm.domain.entity.*;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.Date;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;


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
        assertThat("access token", authDataJAXBElement.getValue().getAccessToken().getId(), equalTo("accessToken"));
    }

    @Test
    public void toAuthDataJaxb_withAuthData_setsRefreshToken() throws Exception {
        authData.setRefreshToken("refreshToken");
        JAXBElement<com.rackspace.api.idm.v1.AuthData> authDataJAXBElement = authConverter.toAuthDataJaxb(authData);
        assertThat("refresh token", authDataJAXBElement.getValue().getRefreshToken().getId(), equalTo("refreshToken"));
    }

    @Test
    public void toAuthDataJaxb_withAuthData_setsApplication() throws Exception {
        authData.setApplication(new Application("clientId", ClientSecret.newInstance("clientSecret"), "name", "customerId", ClientStatus.ACTIVE));
        JAXBElement<com.rackspace.api.idm.v1.AuthData> authDataJAXBElement = authConverter.toAuthDataJaxb(authData);
        assertThat("application", authDataJAXBElement.getValue().getApplication().getClientId(), equalTo("clientId"));
    }

    @Test
    public void toAuthDataJaxb_withAuthData_setsUser() throws Exception {
        authData.setUser(new User("username"));
        JAXBElement<com.rackspace.api.idm.v1.AuthData> authDataJAXBElement = authConverter.toAuthDataJaxb(authData);
        assertThat("user", authDataJAXBElement.getValue().getUser().getUsername(), equalTo("username"));
    }

    @Test
    public void toAuthDataJaxb_withAuthData_setsRacker() throws Exception {
        Racker racker = new Racker();
        racker.setRackerId("rackerId");
        authData.setRacker(racker);
        JAXBElement<com.rackspace.api.idm.v1.AuthData> authDataJAXBElement = authConverter.toAuthDataJaxb(authData);
        assertThat("racker", authDataJAXBElement.getValue().getRacker().getId(), equalTo("rackerId"));
    }

    @Test
    public void toAuthDataJaxb_withAuthData_setsIsPasswordResetOnly() throws Exception {
        authData.setPasswordResetOnlyToken(true);
        JAXBElement<com.rackspace.api.idm.v1.AuthData> authDataJAXBElement = authConverter.toAuthDataJaxb(authData);
        assertThat("password reset only", authDataJAXBElement.getValue().isIsPasswordResetOnlyToken(), equalTo(true));
    }

    @Test
    public void toAuthDataJaxb_withAuthData_setsPasswordExpirationDate() throws Exception {
        authData.setPasswordExpirationDate(new DateTime(1));
        JAXBElement<com.rackspace.api.idm.v1.AuthData> authDataJAXBElement = authConverter.toAuthDataJaxb(authData);
        assertThat("password expiration date", authDataJAXBElement.getValue().getPasswordExpirationDate().toGregorianCalendar().getTime(), equalTo(new DateTime(1).toDate()));
    }

    @Test
    public void toAuthDataJaxb_withAuthData_setsDaysUntilPasswordExpires() throws Exception {
        authData.setPasswordExpirationDate(new DateTime());
        JAXBElement<com.rackspace.api.idm.v1.AuthData> authDataJAXBElement = authConverter.toAuthDataJaxb(authData);
        assertThat("password days till expiration", authDataJAXBElement.getValue().getDaysUntilPasswordExpiration(), equalTo(0));
    }

    @Test
    public void toAuthDataJaxb_withAuthDataAndNoAccessToken_setsNullAccessToken() throws Exception {
        JAXBElement<com.rackspace.api.idm.v1.AuthData> authDataJAXBElement = authConverter.toAuthDataJaxb(authData);
        assertThat("access token", authDataJAXBElement.getValue().getAccessToken(), nullValue());
    }

    @Test
    public void toAuthDataJaxb_withAuthDataAndNoRefreshToken_setsNullRefreshToken() throws Exception {
        JAXBElement<com.rackspace.api.idm.v1.AuthData> authDataJAXBElement = authConverter.toAuthDataJaxb(authData);
        assertThat("refresh token", authDataJAXBElement.getValue().getRefreshToken(), nullValue());
    }

    @Test
    public void toAuthDataJaxb_withAuthDataAndNoApplication_setsNullApplication() throws Exception {
        JAXBElement<com.rackspace.api.idm.v1.AuthData> authDataJAXBElement = authConverter.toAuthDataJaxb(authData);
        assertThat("application", authDataJAXBElement.getValue().getApplication(), nullValue());
    }

    @Test
    public void toAuthDataJaxb_withAuthDataAndNoUser_setsNullUser() throws Exception {
        JAXBElement<com.rackspace.api.idm.v1.AuthData> authDataJAXBElement = authConverter.toAuthDataJaxb(authData);
        assertThat("user", authDataJAXBElement.getValue().getUser(), nullValue());
    }

    @Test
    public void toAuthDataJaxb_withAuthDataAndNoRacker_setsNullRacker() throws Exception {
        JAXBElement<com.rackspace.api.idm.v1.AuthData> authDataJAXBElement = authConverter.toAuthDataJaxb(authData);
        assertThat("racker", authDataJAXBElement.getValue().getRacker().getId(), nullValue());
    }

    @Test
    public void toAuthDataJaxb_withAuthDataAndNoPasswordExpirationDate_setsNullPasswordExpirationDate() throws Exception {
        JAXBElement<com.rackspace.api.idm.v1.AuthData> authDataJAXBElement = authConverter.toAuthDataJaxb(authData);
        assertThat("password expiration date", authDataJAXBElement.getValue().getPasswordExpirationDate(), nullValue());
    }

    @Test
    public void toXmlGregorianCalender_withValidDateTime_returnsXmlGregorianCalender() throws Exception {
        XMLGregorianCalendar xmlGregorianCalendar = authConverter.toXmlGregorianCalender(new DateTime(1));
        assertThat("xmlGregorianCalendar time", xmlGregorianCalendar.toGregorianCalendar().getTimeInMillis(), equalTo(1L));
    }

    @Test
    public void toXmlGregorianCalender_withNullDateTime_returnsNull() throws Exception {
        XMLGregorianCalendar xmlGregorianCalendar = authConverter.toXmlGregorianCalender(null);
        assertThat("xmlGregorianCalendar time", xmlGregorianCalendar, nullValue());
    }
}
