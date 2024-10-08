package com.rackspace.idm.util;

import com.rackspace.idm.exception.IdmException;
import junit.framework.Assert;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.RandomStringUtils;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.junit.Before;
import org.junit.Test;

import java.security.GeneralSecurityException;
import java.security.InvalidParameterException;

import static junit.framework.Assert.assertTrue;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CryptHelperTestsOld {
    CryptHelper cryptHelper;
    DefaultEncryptionPasswordSource encryptionPasswordSource;

    @Before
    public void setUp() throws Exception {
        encryptionPasswordSource = mock(DefaultEncryptionPasswordSource.class);
        cryptHelper = new CryptHelper();
        cryptHelper.setEncryptionPasswordSource(encryptionPasswordSource);

        when(encryptionPasswordSource.getPassword(anyString())).thenReturn("password");
    }

	@Test
	public void shouldEncrypAndDecrypt() throws GeneralSecurityException, InvalidCipherTextException {
		String secret = "This is a secret";
		byte[] ciphertext = cryptHelper.encrypt(secret, "0", "a1 b1");
		Assert.assertFalse(ciphertext.length == 0);
		Assert.assertFalse(ciphertext.equals(secret.getBytes()));
		String decrypt = cryptHelper.decrypt(ciphertext, "0", "a1 b1");
		Assert.assertEquals(secret, decrypt);
	}
	
	@Test
	public void shouldEncrypAndDecryptNothing() throws GeneralSecurityException, InvalidCipherTextException {
		String secret = "";
		byte[] ciphertext = cryptHelper.encrypt(secret, "0", "a1 b1");
		Assert.assertFalse(ciphertext.length == 0);
		Assert.assertFalse(ciphertext.equals(secret.getBytes()));
		String decrypt = cryptHelper.decrypt(ciphertext, "0", "a1 b1");
		Assert.assertEquals(secret, decrypt);
	}
	
	@Test
	public void shouldEncrypAndDecryptLongValues() throws GeneralSecurityException, InvalidCipherTextException {
		String secret = RandomStringUtils.random(1024);
		byte[] ciphertext = cryptHelper.encrypt(secret, "0", "a1 b1");
		Assert.assertFalse(ciphertext.length == 0);
		Assert.assertFalse(ciphertext.equals(secret.getBytes()));
		String decrypt = cryptHelper.decrypt(ciphertext, "0", "a1 b1");
		Assert.assertEquals(secret, decrypt);
	}

    @Test
    public void encrypt_nullPlainText_throwsInvalidParameterException() throws Exception {
        try{
            cryptHelper.encrypt(null, "0", "a1 b1");
            assertTrue("should throw exception",false);
        }catch (InvalidParameterException ex){
            assertThat("message",ex.getMessage(),equalTo("Null argument is not valid"));
        }
    }

    @Test(expected = IdmException.class)
    public void encrypt_invalidHexConfig() throws Exception {
        String secret = "This is a secret";
        byte[] ciphertext = cryptHelper.encrypt(secret, "0", "in va lid");
        assertTrue("should throw exception",false);
    }

}
