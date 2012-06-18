package com.rackspace.idm.api.resource.cloud.v11;

import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.CloudAdminAuthorizationException;
import com.rackspacecloud.docs.auth.api.v1.NastCredentials;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertTrue;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 6/18/12
 * Time: 10:55 AM
 */
public class CredentialValidatorTest {

    CredentialValidator credentialValidator;

    @Before
    public void setUp() throws Exception {
        credentialValidator = new CredentialValidator();
    }

    @Test(expected = BadRequestException.class)
    public void validateCredential_NastCredentialsWithNullNastId_throwsBadRequestException() throws Exception {
        final NastCredentials credential = new NastCredentials();
        credential.setKey("key");
        credentialValidator.validateCredential(credential);
    }

    @Test
    public void validateCredential_NastCredentialsWithNullKey_ReturnsExpectedMessage() throws Exception {
        final NastCredentials credential = new NastCredentials();
        credential.setNastId("id");
        try {
            credentialValidator.validateCredential(credential);
            assertTrue(false);
        } catch (Exception e) {
            assertThat("message", e.getMessage(), equalTo("Expecting apiKey"));
        }
    }

    @Test
    public void validateCredential_NastCredentialsWithNullNastId_ReturnsExpectedMessage() throws Exception {
        final NastCredentials credential = new NastCredentials();
        credential.setKey("key");
        try {
            credentialValidator.validateCredential(credential);
            assertTrue(false);
        } catch (Exception e) {
            assertThat("message", e.getMessage(), equalTo("Expecting nastId"));
        }
    }

    @Test
    public void validateCredential_NastCredentialsWithEmptyNastId_ReturnsExpectedMessage() throws Exception {
        final NastCredentials credential = new NastCredentials();
        credential.setKey("key");
        credential.setNastId("");
        try {
            credentialValidator.validateCredential(credential);
            assertTrue(false);
        } catch (Exception e) {
            assertThat("message", e.getMessage(), equalTo("Expecting nastId"));
        }
    }

    @Test
    public void validateCredential_NastCredentialsWithEmptyKey_ReturnsExpectedMessage() throws Exception {
        final NastCredentials credential = new NastCredentials();
        credential.setKey("");
        credential.setNastId("id");
        try {
            credentialValidator.validateCredential(credential);
            assertTrue(false);
        } catch (Exception e) {
            assertThat("message", e.getMessage(), equalTo("Expecting apiKey"));
        }
    }

    @Test(expected = BadRequestException.class)
    public void validateCredential_NastCredentialsWithNullKey_throwsBadRequestException() throws Exception {
        final NastCredentials credential = new NastCredentials();
        credential.setNastId("id");
        credentialValidator.validateCredential(credential);
    }

    @Test(expected = BadRequestException.class)
    public void validateCredential_NastCredentialsWithBlankNastId_throwsBadRequestException() throws Exception {
        final NastCredentials credential = new NastCredentials();
        credential.setKey("");
        credentialValidator.validateCredential(credential);
    }

    @Test(expected = BadRequestException.class)
    public void validateCredential_NastCredentialsWithBlankKey_throwsBadRequestException() throws Exception {
        final NastCredentials credential = new NastCredentials();
        credential.setNastId("");
        credentialValidator.validateCredential(credential);
    }
}
