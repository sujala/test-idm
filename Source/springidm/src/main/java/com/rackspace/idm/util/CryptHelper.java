package com.rackspace.idm.util;

import java.security.GeneralSecurityException;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CryptHelper {
	private static final String ALOGRITHM = "PBEWithMD5AndDES";

	Logger logger = LoggerFactory.getLogger(CryptHelper.class);

	// Salt
	private static byte[] salt = { (byte) 0xc7, (byte) 0x73, (byte) 0x21, (byte) 0x8c,
			(byte) 0x7e, (byte) 0xc8, (byte) 0xee, (byte) 0x99 };

	// Iteration count
	private static int count = 20;

	// Create PBE parameter set
	private static PBEParameterSpec pbeParamSpec = new PBEParameterSpec(salt, count);


	private static PBEKeySpec pbeKeySpec = new PBEKeySpec(
			"this is a super secret key!".toCharArray());
	
	static SecretKeyFactory keyFac;
	private static SecretKey pbeKey;
	static {
		try {
			keyFac = SecretKeyFactory.getInstance(ALOGRITHM);
			pbeKey = keyFac.generateSecret(pbeKeySpec);

		} catch (Exception e) {
			LoggerFactory.getLogger(CryptHelper.class).error("Error initializing", e);
		}
	}
	
	private static CryptHelper instance = new CryptHelper();
	public static CryptHelper getinstance() {
		return instance;
	}

	public byte[] encrypt(String cleartext) throws GeneralSecurityException {
		byte[] ciphertext = new byte[] {};
		try {
			Cipher pbeCipher = Cipher.getInstance(ALOGRITHM);
			// Initialize PBE Cipher with key and parameters
			pbeCipher.init(Cipher.ENCRYPT_MODE, pbeKey, pbeParamSpec);

			// Encrypt the cleartext
			ciphertext = pbeCipher.doFinal(cleartext.getBytes());

		} catch (GeneralSecurityException e) {
			logger.error("Encrypting", e);
			throw(e);
		}
		return ciphertext;
	}

	public String decrypt(byte[] ciphertext) throws GeneralSecurityException {
		String result = "";
		if(ciphertext == null || ciphertext.length < 1) {
			return result;
		}
		try {
			Cipher pbeCipher = Cipher.getInstance(ALOGRITHM);
			// Initialize PBE Cipher with key and parameters
			pbeCipher.init(Cipher.DECRYPT_MODE, pbeKey, pbeParamSpec);

			byte[] bs = pbeCipher.doFinal(ciphertext);

			result = new String(bs);
		} catch (GeneralSecurityException e) {
			logger.error("Decrypting", e);
			throw(e);
		}
		return result;
	}
}
