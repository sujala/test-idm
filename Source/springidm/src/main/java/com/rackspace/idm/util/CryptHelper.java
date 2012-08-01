package com.rackspace.idm.util;

import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.PBEParametersGenerator;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.generators.PKCS12ParametersGenerator;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.PKCS7Padding;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.InvalidParameterException;
import java.security.Security;

public class CryptHelper {

	// Salt
	private static byte[] salt = { (byte) 0xc7, (byte) 0x73, (byte) 0x21, (byte) 0x8c, (byte) 0x7e, (byte) 0xc8,
			(byte) 0xee, (byte) 0x99 };

	// TODO: get this from secrets.idm.properties
	private static char[] password = "this is a super secret key!".toCharArray();

	private static PBEParametersGenerator keyGenerator;
	private static CipherParameters keyParams;

	static {
		try {
			Security.addProvider(new BouncyCastleProvider());
			keyGenerator = new PKCS12ParametersGenerator(new SHA256Digest());
			keyGenerator.init(PKCS12ParametersGenerator.PKCS12PasswordToBytes(password), salt, 20);
			keyParams = keyGenerator.generateDerivedParameters(256, 128);
		} catch (Exception e) {
			LoggerFactory.getLogger(CryptHelper.class).error("Error initializing encryption provider", e);
		}
	}

	private static CryptHelper instance = new CryptHelper();

	public static CryptHelper getInstance() {
		return instance;
	}

	public byte[] encrypt(String plainText) throws GeneralSecurityException, InvalidCipherTextException {
		if(plainText == null) {
			throw new InvalidParameterException("Null argument is not valid");
		}
		
		BufferedBlockCipher cipher = new PaddedBufferedBlockCipher(new CBCBlockCipher(new AESEngine()),
				new PKCS7Padding());

		cipher.init(true, keyParams);
		byte[] bytes;
		try {
			bytes = plainText.getBytes("UTF8");
		} catch (UnsupportedEncodingException e) {
			bytes = plainText.getBytes();
		}
		final byte[] processed = new byte[cipher.getOutputSize(bytes.length)];
		int outputLength = cipher.processBytes(bytes, 0, bytes.length, processed, 0);
		outputLength += cipher.doFinal(processed, outputLength);

		final byte[] results = new byte[outputLength];
		System.arraycopy(processed, 0, results, 0, outputLength);
		return results;
	}

	public String decrypt(final byte[] bytes) throws GeneralSecurityException, InvalidCipherTextException {
		if(bytes == null || bytes.length < 1) {
			return null;
		}
		
		final BufferedBlockCipher cipher = new PaddedBufferedBlockCipher(new CBCBlockCipher(new AESEngine()),
				new PKCS7Padding());
		cipher.init(false, keyParams);

		final byte[] processed = new byte[cipher.getOutputSize(bytes.length)];
		int outputLength = cipher.processBytes(bytes, 0, bytes.length, processed, 0);
		outputLength += cipher.doFinal(processed, outputLength);

		final byte[] results = new byte[outputLength];
		System.arraycopy(processed, 0, results, 0, outputLength);
		try {
			return new String(results, "UTF8");
		} catch (UnsupportedEncodingException e) {
			return new String(results);
		}
	}
}
