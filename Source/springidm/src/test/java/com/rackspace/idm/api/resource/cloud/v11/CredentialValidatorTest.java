package com.rackspace.idm.api.resource.cloud.v11;

import com.rackspace.idm.exception.BadRequestException;
import com.rackspacecloud.docs.auth.api.v1.NastCredentials;
import org.junit.Before;
import org.junit.Test;

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
