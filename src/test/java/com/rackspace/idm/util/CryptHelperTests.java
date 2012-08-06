package com.rackspace.idm.util;

import java.security.GeneralSecurityException;
import java.security.InvalidParameterException;

import junit.framework.Assert;

import org.apache.commons.lang.RandomStringUtils;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.junit.Test;

import static junit.framework.Assert.assertTrue;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

public class CryptHelperTests {

	@Test
	public void shouldEncrypAndDecrypt() throws GeneralSecurityException, InvalidCipherTextException {
		String secret = "This is a secret";

		CryptHelper crypto = new CryptHelper();
		byte[] ciphertext = crypto.encrypt(secret);

		Assert.assertFalse(ciphertext.length == 0);
		Assert.assertFalse(ciphertext.equals(secret.getBytes()));

		String decrypt = crypto.decrypt(ciphertext);

		Assert.assertEquals(secret, decrypt);
	}
	
	@Test
	public void shouldEncrypAndDecryptNothing() throws GeneralSecurityException, InvalidCipherTextException {
		String secret = "";

		CryptHelper crypto = new CryptHelper();
		byte[] ciphertext = crypto.encrypt(secret);

		Assert.assertFalse(ciphertext.length == 0);
		Assert.assertFalse(ciphertext.equals(secret.getBytes()));

		String decrypt = crypto.decrypt(ciphertext);

		Assert.assertEquals(secret, decrypt);
	}
	
	@Test
	public void shouldEncrypAndDecryptLongValues() throws GeneralSecurityException, InvalidCipherTextException {
		String secret = RandomStringUtils.random(1024);
		CryptHelper crypto = new CryptHelper();
		byte[] ciphertext = crypto.encrypt(secret);

		Assert.assertFalse(ciphertext.length == 0);
		Assert.assertFalse(ciphertext.equals(secret.getBytes()));

		String decrypt = crypto.decrypt(ciphertext);

		Assert.assertEquals(secret, decrypt);
	}

    @Test
    public void encrypt_nullPlainText_throwsInvalidParameterException() throws Exception {
        try{
            CryptHelper cryptHelper = new CryptHelper();
            cryptHelper.encrypt(null);
            assertTrue("should throw exception",false);
        }catch (InvalidParameterException ex){
            assertThat("message",ex.getMessage(),equalTo("Null argument is not valid"));
        }
    }
}
