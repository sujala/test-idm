package com.rackspace.idm.util;

import com.rackspace.idm.exception.IdmException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.Crypt;
import org.apache.commons.lang.StringUtils;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class CryptHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(CryptHelper.class);

    @Autowired
    private EncryptionPasswordSource encryptionPasswordSource;

    public static final int ITERATION_COUNT = 20;

    public static final int KEY_SIZE = 256;

    public static final int IV_SIZE = 128;

    private SecureRandom secureRandom = new SecureRandom();

    private static final PBEParametersGenerator keyGenerator;

    private static final String SSHA512 = "{SSHA512}";
    private static final int SHA512_SIZE = 64;
    private static final int SALT_SIZE = 4;

    private static final Pattern CRYPT_PATTERN = Pattern.compile("(\\$[a-z0-9]+\\$(.*\\$){0,1})(.*)");

    static {
        Security.addProvider(new BouncyCastleProvider());
        keyGenerator = new PKCS12ParametersGenerator(new SHA256Digest());
    }

    public CipherParameters getKeyParams(String passwordString, String saltString) {
        CipherParameters result = null;
		try {
            char[] password = passwordString.toCharArray();
            byte[] salt = fromHexString(saltString);
            synchronized (keyGenerator) {
                keyGenerator.init(PKCS12ParametersGenerator.PKCS12PasswordToBytes(password), salt, ITERATION_COUNT);
                result = keyGenerator.generateDerivedParameters(KEY_SIZE, IV_SIZE);
            }
		} catch (Exception e) {
			throw new IdmException(e.getMessage());
		}
        return result;
	}

	public byte[] encrypt(String plainText, String versionId, String salt) throws GeneralSecurityException, InvalidCipherTextException {
        return encrypt(plainText, getKeyParams(encryptionPasswordSource.getPassword(versionId), salt));
    }

    public String generateSalt() {
        String random = new BigInteger(130, secureRandom).toString(16);
        List<String> splitString = new ArrayList<String>();

        while(random.length() > 2) {
            String head = random.substring(0, 2);
            String tail = random.substring(2);
            splitString.add(head);
            random = tail;
        }
        return StringUtils.join(splitString, " ");
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

	public String decrypt(final byte[] bytes, String versionId, String salt) throws GeneralSecurityException, InvalidCipherTextException {
        return decrypt(bytes, getKeyParams(encryptionPasswordSource.getPassword(versionId), salt));
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

    public void setEncryptionPasswordSource(EncryptionPasswordSource encryptionPasswordSource) {
        this.encryptionPasswordSource = encryptionPasswordSource;
    }

    public boolean verifyLegacySHA(String password, String userPassword) {
        try {
            final String sha = userPassword.substring(SSHA512.length());
            final byte[] bytes = Base64.decodeBase64(sha);

            final byte[] salt = new byte[bytes.length - SHA512_SIZE];
            System.arraycopy(bytes, SHA512_SIZE, salt, 0, salt.length);

            final byte[] hash = new byte[SHA512_SIZE];
            System.arraycopy(bytes, 0, hash, 0, SHA512_SIZE);

            final byte[] newHash = createLegacySHA(salt, password);
            return Arrays.equals(hash, newHash);
        } catch (Exception e) {
            LOGGER.debug("Cannot verify legacy SHA", e);
            return false;
        }
    }

    public boolean isPasswordEncrypted(String userPassword) {
        return userPassword != null && isPasswordLegacySHA(userPassword) || isPasswordCrypt(userPassword);
    }

    public boolean isPasswordLegacySHA(String userPassword) {
        return userPassword != null && userPassword.startsWith(SSHA512);
    }

    public boolean isPasswordCrypt(String userPassword) {
        return userPassword != null && CRYPT_PATTERN.matcher(userPassword).matches();
    }

    public boolean checkPassword(String userPassword, String password) {
        if (isPasswordLegacySHA(userPassword)) {
            return verifyLegacySHA(password, userPassword);
        } else {
            return verifyCrypt(password, userPassword);
        }
    }

    public boolean verifyCrypt(String password, String userPassword) {
        try {
            final Matcher matcher = CRYPT_PATTERN.matcher(userPassword);
            if (matcher.matches()) {
                final String salt = matcher.group(1);
                final String newCalc = Crypt.crypt(password, salt);
                return userPassword.equalsIgnoreCase(newCalc);
            }
        } catch (Exception e) {
            LOGGER.debug("Cannot verify crypt", e);
        }
        return false;
    }

    public String createLegacySHA(String password) {
        byte salt[] = new byte[SALT_SIZE];
        secureRandom.nextBytes(salt);
        byte hash[] = createLegacySHA(salt, password);

        byte newPassword[] = new byte[SHA512_SIZE + SALT_SIZE];
        System.arraycopy(hash, 0, newPassword, 0, SHA512_SIZE);
        System.arraycopy(salt, 0, newPassword, SHA512_SIZE, SALT_SIZE);

        return SSHA512 + Base64.encodeBase64String(newPassword);
    }

    private byte[] createLegacySHA(byte[] salt, String password) {
        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA-512");
            digest.update(password.getBytes());
            digest.update(salt);
            return digest.digest();
        } catch (Exception e) {
            LOGGER.debug("Cannot create legacy SHA", e);
        }
        return new byte[0];
    }

}
