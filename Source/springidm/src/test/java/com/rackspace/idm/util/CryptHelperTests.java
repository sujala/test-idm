package com.rackspace.idm.util;

import java.security.GeneralSecurityException;
import java.util.Random;

import junit.framework.Assert;

import org.bouncycastle.crypto.InvalidCipherTextException;
import org.junit.Test;

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
		byte[] bs = new byte[1024];
		new Random().nextBytes(bs);
		String secret = new String(bs);

		CryptHelper crypto = new CryptHelper();
		byte[] ciphertext = crypto.encrypt(secret);

		Assert.assertFalse(ciphertext.length == 0);
		Assert.assertFalse(ciphertext.equals(secret.getBytes()));

		String decrypt = crypto.decrypt(ciphertext);

		Assert.assertEquals(secret, decrypt);
	}
	
}
