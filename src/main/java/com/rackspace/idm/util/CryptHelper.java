package com.rackspace.idm.util;

import com.rackspace.idm.exception.IdmException;
import org.apache.commons.configuration.Configuration;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.InvalidParameterException;
import java.security.Security;

@Component
public class CryptHelper {

    private CipherParameters keyParams;

    @Autowired
    public void setConfiguration(Configuration configuration){
        CryptHelper.config = configuration;
    }
    
    private static Configuration config;

	private static PBEParametersGenerator keyGenerator;

    public static final int ITERATION_COUNT = 20;

    public static final int KEY_SIZE = 256;

    public static final int IV_SIZE = 128;

    private CipherParameters getKeyParams() {
        if (keyParams == null) {
            keyParams = getKeyParams(config.getString("crypto.password"), config.getString("crypto.salt"));
        }
        return keyParams;
    }

    public CipherParameters getKeyParams(String passwordString, String saltString) {
        CipherParameters result = null;
		try {
            char[] password = passwordString.toCharArray();
            byte[] salt = fromHexString(saltString);
			Security.addProvider(new BouncyCastleProvider());
			keyGenerator = new PKCS12ParametersGenerator(new SHA256Digest());
			keyGenerator.init(PKCS12ParametersGenerator.PKCS12PasswordToBytes(password), salt, ITERATION_COUNT);
			result = keyGenerator.generateDerivedParameters(KEY_SIZE, IV_SIZE);
		} catch (Exception e) {
			throw new IdmException(e.getMessage());
		}
        return result;
	}

    private static CryptHelper instance = new CryptHelper();

    public CryptHelper(){};

	public static CryptHelper getInstance() {
		return instance;
	}

	public byte[] encrypt(String plainText) throws GeneralSecurityException, InvalidCipherTextException {
        return encrypt(plainText, getKeyParams());
    }

    public byte[] encrypt(String plainText, CipherParameters cipherParameters) throws GeneralSecurityException, InvalidCipherTextException {
		if(plainText == null) {
			throw new InvalidParameterException("Null argument is not valid");
		}
		
		BufferedBlockCipher cipher = new PaddedBufferedBlockCipher(new CBCBlockCipher(new AESEngine()), new PKCS7Padding());

		cipher.init(true, cipherParameters);
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
        return decrypt(bytes, getKeyParams());
    }

    public String decrypt(final byte[] bytes, CipherParameters cipherParameters) throws GeneralSecurityException, InvalidCipherTextException {
		if(bytes == null || bytes.length < 1) {
			return null;
		}
		
		final BufferedBlockCipher cipher = new PaddedBufferedBlockCipher(new CBCBlockCipher(new AESEngine()), new PKCS7Padding());
		cipher.init(false, cipherParameters);

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

    private static final byte[] fromHexString(final String s) {
        try {
            String[] v = s.split(" ");
            byte[] arr = new byte[v.length];
            int i = 0;
            for(String val: v) {
                arr[i++] =  Integer.decode("0x" + val).byteValue();
            }
            return arr;
        } catch (Exception e) {
			throw new IdmException("Error creating byte array from Hex string");
		}
    }
}
