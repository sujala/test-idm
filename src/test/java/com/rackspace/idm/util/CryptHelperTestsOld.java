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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class CryptHelperTestsOld {
    Configuration configuration;
    CryptHelper cryptHelper;
    CryptHelper spy;

    @Before
    public void setUp() throws Exception {
        configuration = mock(Configuration.class);
        cryptHelper = new CryptHelper();
        cryptHelper.setConfiguration(configuration);
        spy = spy(cryptHelper);
    }

	@Test
	public void shouldEncrypAndDecrypt() throws GeneralSecurityException, InvalidCipherTextException {
		String secret = "This is a secret";
        when(configuration.getString("crypto.password")).thenReturn("password");
        when(configuration.getString("crypto.salt")).thenReturn("a1 b1");
		byte[] ciphertext = cryptHelper.encrypt(secret);
		Assert.assertFalse(ciphertext.length == 0);
		Assert.assertFalse(ciphertext.equals(secret.getBytes()));
		String decrypt = cryptHelper.decrypt(ciphertext);
		Assert.assertEquals(secret, decrypt);
	}
	
	@Test
	public void shouldEncrypAndDecryptNothing() throws GeneralSecurityException, InvalidCipherTextException {
		String secret = "";
        when(configuration.getString("crypto.password")).thenReturn("password");
        when(configuration.getString("crypto.salt")).thenReturn("a1 b1");
		byte[] ciphertext = cryptHelper.encrypt(secret);
		Assert.assertFalse(ciphertext.length == 0);
		Assert.assertFalse(ciphertext.equals(secret.getBytes()));
		String decrypt = cryptHelper.decrypt(ciphertext);
		Assert.assertEquals(secret, decrypt);
	}
	
	@Test
	public void shouldEncrypAndDecryptLongValues() throws GeneralSecurityException, InvalidCipherTextException {
		String secret = RandomStringUtils.random(1024);
        when(configuration.getString("crypto.password")).thenReturn("password");
        when(configuration.getString("crypto.salt")).thenReturn("a1 b1");
		byte[] ciphertext = cryptHelper.encrypt(secret);
		Assert.assertFalse(ciphertext.length == 0);
		Assert.assertFalse(ciphertext.equals(secret.getBytes()));
		String decrypt = cryptHelper.decrypt(ciphertext);
		Assert.assertEquals(secret, decrypt);
	}

    @Test
    public void encrypt_nullPlainText_throwsInvalidParameterException() throws Exception {
        try{
            CryptHelper cryptHelper = new CryptHelper();
            cryptHelper.setConfiguration(configuration);
            when(configuration.getString("crypto.password")).thenReturn("password");
            when(configuration.getString("crypto.salt")).thenReturn("a1 b1");
            cryptHelper.encrypt(null);
            assertTrue("should throw exception",false);
        }catch (InvalidParameterException ex){
            assertThat("message",ex.getMessage(),equalTo("Null argument is not valid"));
        }
    }

    @Test(expected = IdmException.class)
    public void encrypt_invalidHexConfig() throws Exception {
        String secret = "This is a secret";
        when(configuration.getString("crypto.password")).thenReturn("password");
        when(configuration.getString("crypto.salt")).thenReturn("in va lid");
        byte[] ciphertext = cryptHelper.encrypt(secret);
        assertTrue("should throw exception",false);
    }

}
