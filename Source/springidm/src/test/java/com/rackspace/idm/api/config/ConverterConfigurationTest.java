package com.rackspace.idm.api.config;

import com.rackspace.idm.api.converter.*;
import com.rackspace.idm.api.converter.cloudv11.EndpointConverterCloudV11;
import com.rackspace.idm.api.converter.cloudv11.TokenConverterCloudV11;
import com.rackspace.idm.api.converter.cloudv11.UserConverterCloudV11;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * Created with IntelliJ IDEA.
 * User: ryan5034
 * Date: 6/28/12
 * Time: 2:01 PM
 * To change this template use File | Settings | File Templates.
 */
public class ConverterConfigurationTest {

    ConverterConfiguration converterConfiguration;
    ConverterConfiguration spy;

    @Before
    public void setUp() throws Exception {
        converterConfiguration = new ConverterConfiguration();
        spy = spy(converterConfiguration);
    }

    @Test
    public void rolesConverter_returnsNewRolesConverter() throws Exception {
        assertThat("role converter", converterConfiguration.rolesConverter(),instanceOf(RolesConverter.class));
    }

    @Test
    public void passwordConverter_returnsNewPasswordConverter() throws Exception {
        assertThat("password converter",converterConfiguration.passwordConverter(),instanceOf(PasswordConverter.class));
    }

    @Test
    public void passwordRulesConverter_returnsNewPasswordRulesConverter() throws Exception {
        assertThat("password rules converter",converterConfiguration.passwordRulesConverter(),instanceOf(PasswordRulesConverter.class));
    }

    @Test
    public void clientConverter_callsRolesConverter() throws Exception {
        spy.clientConverter();
        verify(spy).rolesConverter();
    }

    @Test
    public void clientConverter_returnsApplicationConverter() throws Exception {
        assertThat("application converter",converterConfiguration.clientConverter(),instanceOf(ApplicationConverter.class));
    }

    @Test
    public void userConverter_callsRolesConverter() throws Exception {
        spy.userConverter();
        verify(spy).rolesConverter();
    }

    @Test
    public void userConverter_returnsUserConverter() throws Exception {
        assertThat("user converter",converterConfiguration.userConverter(),instanceOf(UserConverter.class));
    }

    @Test
    public void customerConverter_returnsCustomerConverter() throws Exception {
        assertThat("customer converter",converterConfiguration.customerConverter(),instanceOf(CustomerConverter.class));
    }

    @Test
    public void tokenConverter_returnsTokenConverter() throws Exception {
        assertThat("token converter",converterConfiguration.tokenConverter(),instanceOf(TokenConverter.class));
    }

    @Test
    public void authConverter_callsTokenConverter() throws Exception {
        spy.authConverter();
        verify(spy).tokenConverter();
    }

    @Test
    public void authConverter_callsClientConverter() throws Exception {
        spy.authConverter();
        verify(spy).clientConverter();
    }

    @Test
    public void authConverter_callsUserConverter() throws Exception {
        spy.authConverter();
        verify(spy).userConverter();
    }

    @Test
    public void authConverter_returnsAuthConverter() throws Exception {
        assertThat("auth converter",converterConfiguration.authConverter(),instanceOf(AuthConverter.class));
    }

    @Test
    public void authConverterCloudV11_callsTokenConverterCloudV11() throws Exception {
        spy.authConverterCloudV11();
        verify(spy).tokenConverterCloudV11();
    }

    @Test
    public void authConverterCloudV11_callsEndpointConverterCloudV11() throws Exception {
        spy.authConverterCloudV11();
        verify(spy).endpointConverterCloudV11();
    }

    @Test
    public void credentialConverter_returnsNewCredentialsConverter() throws Exception {
        assertThat("credentials converter", converterConfiguration.credentialsConverter(),instanceOf(CredentialsConverter.class));
    }

    @Test
    public void endpointConverterCloudV11_returnsNewEndpointConverterCloudV11() throws Exception {
        assertThat("endpoint converter cloud v11", converterConfiguration.endpointConverterCloudV11(),instanceOf(EndpointConverterCloudV11.class));
    }

    @Test
    public void tokenConverterCloudV11_returnsTokenConverterCloudV11() throws Exception {
        assertThat("token converter cloud v11",converterConfiguration.tokenConverterCloudV11(),instanceOf(TokenConverterCloudV11.class));
    }

    @Test
    public void userConverterCloudV11_callsEndpointConverterCloudV11() throws Exception {
        spy.userConverterCloudV11();
        verify(spy).endpointConverterCloudV11();
    }

    @Test
    public void userConverterCloudV11_returnsUserConverterCloudV11() throws Exception {
        assertThat("user converter cloud v11",converterConfiguration.userConverterCloudV11(),instanceOf(UserConverterCloudV11.class));
    }
}
