package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.entity.Application;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.EncryptionService;
import com.rackspace.idm.domain.service.PropertiesService;
import com.rackspace.idm.util.CryptHelper;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.security.GeneralSecurityException;

@Component
public class DefaultEncryptionService implements EncryptionService {

    public static final String ENCRYPTION_VERSION_ID = "encryptionVersionId";
    public static final String USER_ENCRYPTION_ERROR_MESSAGE = "Error encrypting %s value for user %s";
    public static final String USER_DECRYPTION_ERROR_MESSAGE = "Error decrypting %s value for user %s";
    public static final String SECRET_QUESTION = "SecretQuestion";
    public static final String SECRET_QUESTION_ID = "SecretQuestionId";
    public static final String SECRET_ANSWER = "SecretAnswer";
    public static final String DISPLAY_NAME = "DisplayName";
    public static final String API_KEY = "ApiKey";
    public static final String PHONE_PIN = "PhonePin";


    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    CryptHelper cryptHelper;

    @Autowired
    Configuration config;

    @Autowired
    PropertiesService propertiesService;

    @Override
    public void setUserEncryptionSaltAndVersion(User user) {
        user.setEncryptionVersion(propertiesService.getValue(ENCRYPTION_VERSION_ID));
        user.setSalt(cryptHelper.generateSalt());
    }

    @Override
    public void encryptUser(User user) {
        String encryptionVersionId = getEncryptionVersionId(user);
        String encryptionSalt = getEncryptionSalt(user);
        user.setEncryptionVersion(encryptionVersionId);
        user.setSalt(encryptionSalt);

        try {
            if (user.getSecretQuestion() != null) {
                user.setEncryptedSecretQuestion(cryptHelper.encrypt(user.getSecretQuestion(), encryptionVersionId, encryptionSalt));
            }
        } catch (GeneralSecurityException e) {
            logger.error(String.format(USER_ENCRYPTION_ERROR_MESSAGE, SECRET_QUESTION, user.getId()), e);
        } catch (InvalidCipherTextException e) {
            logger.error(String.format(USER_ENCRYPTION_ERROR_MESSAGE, SECRET_QUESTION, user.getId()), e);
        }

        try {
            if (user.getSecretAnswer() != null) {
                user.setEncryptedSecretAnswer(cryptHelper.encrypt(user.getSecretAnswer(), encryptionVersionId, encryptionSalt));
            }
        } catch (GeneralSecurityException e) {
            logger.error(String.format(USER_ENCRYPTION_ERROR_MESSAGE, SECRET_ANSWER, user.getId()), e);
        } catch (InvalidCipherTextException e) {
            logger.error(String.format(USER_ENCRYPTION_ERROR_MESSAGE, SECRET_ANSWER, user.getId()), e);
        }

        try {
            if (user.getSecretQuestionId() != null) {
                user.setEncryptedSecretQuestionId(cryptHelper.encrypt(user.getSecretQuestionId(), encryptionVersionId, encryptionSalt));
            }
        } catch (GeneralSecurityException e) {
            logger.error(String.format(USER_ENCRYPTION_ERROR_MESSAGE, SECRET_QUESTION_ID, user.getId()), e);
        } catch (InvalidCipherTextException e) {
            logger.error(String.format(USER_ENCRYPTION_ERROR_MESSAGE, SECRET_QUESTION_ID, user.getId()), e);
        }

        try {
            if (user.getDisplayName() != null) {
                user.setEncryptedDisplayName(cryptHelper.encrypt(user.getDisplayName(), encryptionVersionId, encryptionSalt));
            }
        } catch (GeneralSecurityException e) {
            logger.error(String.format(USER_ENCRYPTION_ERROR_MESSAGE, DISPLAY_NAME, user.getId()), e);
        } catch (InvalidCipherTextException e) {
            logger.error(String.format(USER_ENCRYPTION_ERROR_MESSAGE, DISPLAY_NAME, user.getId()), e);
        }

        try {
            if (user.getApiKey() != null) {
                user.setEncryptedApiKey(cryptHelper.encrypt(user.getApiKey(), encryptionVersionId, encryptionSalt));
            }
        } catch (GeneralSecurityException e) {
            logger.error(String.format(USER_ENCRYPTION_ERROR_MESSAGE, API_KEY, user.getId()), e);
        } catch (InvalidCipherTextException e) {
            logger.error(String.format(USER_ENCRYPTION_ERROR_MESSAGE, API_KEY, user.getId()), e);
        }

        try {
            if (user.getPhonePin() != null) {
                user.setEncryptedPhonePin(cryptHelper.encrypt(user.getPhonePin(), encryptionVersionId, encryptionSalt));
            }
        } catch (GeneralSecurityException e) {
            logger.error(String.format(USER_ENCRYPTION_ERROR_MESSAGE, PHONE_PIN, user.getId()), e);
        } catch (InvalidCipherTextException e) {
            logger.error(String.format(USER_ENCRYPTION_ERROR_MESSAGE, PHONE_PIN, user.getId()), e);
        }
    }

    private String getEncryptionSalt(User user) {
        if (user.getSalt() == null) {
            return config.getString("crypto.salt");
        } else {
            return user.getSalt();
        }
    }

    private String getEncryptionVersionId(User user) {
        if (user.getEncryptionVersion() == null) {
            return propertiesService.getValue(ENCRYPTION_VERSION_ID);
        } else {
            return user.getEncryptionVersion();
        }
    }

    @Override
    public void decryptUser(User user) {
        String encryptionVersionId = getDecryptionVersionId(user);
        String encryptionSalt = getDecryptionSalt(user);

        try {
            if (user.getEncryptedSecretQuestion() != null) {
                user.setSecretQuestion(cryptHelper.decrypt(user.getEncryptedSecretQuestion(), encryptionVersionId, encryptionSalt));
            }
        } catch (GeneralSecurityException e) {
            logger.error(String.format(USER_DECRYPTION_ERROR_MESSAGE, SECRET_QUESTION, user.getId()), e);
        } catch (InvalidCipherTextException e) {
            logger.error(String.format(USER_DECRYPTION_ERROR_MESSAGE, SECRET_QUESTION, user.getId()), e);
        }

        try {
            if (user.getEncryptedSecretAnswer() != null) {
                user.setSecretAnswer(cryptHelper.decrypt(user.getEncryptedSecretAnswer(), encryptionVersionId, encryptionSalt));
            }
        } catch (GeneralSecurityException e) {
            logger.error(String.format(USER_DECRYPTION_ERROR_MESSAGE, SECRET_ANSWER, user.getId()), e);
        } catch (InvalidCipherTextException e) {
            logger.error(String.format(USER_DECRYPTION_ERROR_MESSAGE, SECRET_ANSWER, user.getId()), e);
        }

        try {
            if (user.getEncryptedSecretQuestionId() != null) {
                user.setSecretQuestionId(cryptHelper.decrypt(user.getEncryptedSecretQuestionId(), encryptionVersionId, encryptionSalt));
            }
        } catch (GeneralSecurityException e) {
            logger.error(String.format(USER_DECRYPTION_ERROR_MESSAGE, SECRET_QUESTION_ID, user.getId()), e);
        } catch (InvalidCipherTextException e) {
            logger.error(String.format(USER_DECRYPTION_ERROR_MESSAGE, SECRET_QUESTION_ID, user.getId()), e);
        }

        try {
            if (user.getEncryptedDisplayName() != null) {
                user.setDisplayName(cryptHelper.decrypt(user.getEncryptedDisplayName(), encryptionVersionId, encryptionSalt));
            }
        } catch (GeneralSecurityException e) {
            logger.error(String.format(USER_DECRYPTION_ERROR_MESSAGE, DISPLAY_NAME, user.getId()), e);
        } catch (InvalidCipherTextException e) {
            logger.error(String.format(USER_DECRYPTION_ERROR_MESSAGE, DISPLAY_NAME, user.getId()), e);
        }

        try {
            if (user.getEncryptedApiKey() != null) {
                user.setApiKey(cryptHelper.decrypt(user.getEncryptedApiKey(), encryptionVersionId, encryptionSalt));
            }
        } catch (GeneralSecurityException e) {
            logger.error(String.format(USER_DECRYPTION_ERROR_MESSAGE, API_KEY, user.getId()), e);
        } catch (InvalidCipherTextException e) {
            logger.error(String.format(USER_DECRYPTION_ERROR_MESSAGE, API_KEY, user.getId()), e);
        }

        try {
            if (user.getEncryptedPhonePin() != null) {
                user.setPhonePin(cryptHelper.decrypt(user.getEncryptedPhonePin(), encryptionVersionId, encryptionSalt));
            }
        } catch (GeneralSecurityException e) {
            logger.error(String.format(USER_DECRYPTION_ERROR_MESSAGE, PHONE_PIN, user.getId()), e);
        } catch (InvalidCipherTextException e) {
            logger.error(String.format(USER_DECRYPTION_ERROR_MESSAGE, PHONE_PIN, user.getId()), e);
        }
    }

    @Override
    public String getEncryptionVersionId() {
        return propertiesService.getValue(ENCRYPTION_VERSION_ID);
    }

    private String getDecryptionVersionId(User user) {
        if (StringUtils.isEmpty(user.getEncryptionVersion())) {
            return "0";
        } else {
            return user.getEncryptionVersion();
        }
    }

    private String getDecryptionSalt(User user) {
        if (StringUtils.isEmpty(user.getSalt())) {
            return config.getString("crypto.salt");
        } else {
            return user.getSalt();
        }
    }
}
