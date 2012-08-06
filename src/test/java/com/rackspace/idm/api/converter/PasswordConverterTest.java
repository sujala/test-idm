package com.rackspace.idm.api.converter;

import com.rackspace.api.idm.v1.UserPassword;
import com.rackspace.api.idm.v1.UserPasswordCredentials;
import com.rackspace.idm.domain.entity.Password;
import org.junit.Before;
import org.junit.Test;

import javax.xml.bind.JAXBElement;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Created with IntelliJ IDEA.
 * User: kurt
 * Date: 6/11/12
 * Time: 1:39 PM
 * To change this template use File | Settings | File Templates.
 */
public class PasswordConverterTest {

    PasswordConverter passwordConverter;
    private Password domainPassword;
    UserPasswordCredentials apiPassword;

    @Before
    public void setUp() throws Exception {
        passwordConverter = new PasswordConverter();
        domainPassword = new Password();
        apiPassword = new UserPasswordCredentials();
        UserPassword currentPassword = new UserPassword();
        currentPassword.setPassword("currentPassword");
        apiPassword.setCurrentPassword(currentPassword);
        UserPassword newPassword = new UserPassword();
        newPassword.setPassword("newPassword");
        apiPassword.setNewPassword(newPassword);
    }

    @Test
    public void toJaxb_withDomainPassword_setsCurrentPassword() throws Exception {
        domainPassword.setValue("password");
        JAXBElement<UserPasswordCredentials> jaxbPassword = passwordConverter.toJaxb(domainPassword);
        assertThat("jaxbPassword", jaxbPassword.getValue().getCurrentPassword().getPassword(), equalTo("password"));
    }

    @Test
    public void toPasswordCredentialsDO_withApiPassword_setsCurrentPassword() throws Exception {
        com.rackspace.idm.domain.entity.PasswordCredentials passwordCredentials = passwordConverter.toPasswordCredentialsDO(apiPassword);
        assertThat("current password", passwordCredentials.getCurrentPassword().getValue(), equalTo("currentPassword"));
    }

    @Test
    public void toPasswordCredentialsDO_withApiPassword_setsNewPassword() throws Exception {
        com.rackspace.idm.domain.entity.PasswordCredentials passwordCredentials = passwordConverter.toPasswordCredentialsDO(apiPassword);
        assertThat("new password", passwordCredentials.getNewPassword().getValue(), equalTo("newPassword"));
    }

    @Test
    public void toPasswordCredentialsDO_withApiPassword_setsVerifyCurrentPassword() throws Exception {
        apiPassword.setVerifyCurrentPassword(true);
        com.rackspace.idm.domain.entity.PasswordCredentials passwordCredentials = passwordConverter.toPasswordCredentialsDO(apiPassword);
        assertThat("verify current password", passwordCredentials.isVerifyCurrentPassword(), equalTo(true));
    }


}
