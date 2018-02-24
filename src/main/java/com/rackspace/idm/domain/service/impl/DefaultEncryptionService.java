package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.entity.EndUser;
import com.rackspace.idm.domain.entity.FederatedUser;
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
    public static final String PROV_USER_ENCRYPTION_ERROR_MESSAGE = "Error encrypting %s value for provisioned user %s";
    public static final String PROV_USER_DECRYPTION_ERROR_MESSAGE = "Error decrypting %s value for provisioned user %s";
    public static final String FED_USER_ENCRYPTION_ERROR_MESSAGE = "Error encrypting %s value for federated user %s";
    public static final String FED_USER_DECRYPTION_ERROR_MESSAGE = "Error decrypting %s value for federated user %s";
    public static final String SECRET_QUESTION = "SecretQuestion";
    public static final String SECRET_QUESTION_ID = "SecretQuestionId";
    public static final String SECRET_ANSWER = "SecretAnswer";
    public static final String DISPLAY_NAME = "DisplayName";
    public static final String API_KEY = "ApiKey";
    public static final String PHONE_PIN = "PhonePin";
    public static final String CRYPTO_SALT = "crypto.salt";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    CryptHelper cryptHelper;

    @Autowired
    Configuration config;

    @Autowired
    PropertiesService propertiesService;

    @Override
    public void setUserEncryptionSaltAndVersion(EndUser user) {
        if(user instanceof User) {
            setUserEncryptionSaltAndVersionForProvUser((User) user);
        } else if (user instanceof FederatedUser) {
            setUserEncryptionSaltAndVersionForFedUser((FederatedUser) user);
        } else {
            logger.error(String.format("Unknown user type '%s' when setting user " +
                    "encryption salt and version ", user.getClass().getName()));
            throw new IllegalStateException(String.format("Unknown user type '%s'", user.getClass().getName()));
        }
    }

    private void setUserEncryptionSaltAndVersionForProvUser(User user) {
        user.setEncryptionVersion(propertiesService.getValue(ENCRYPTION_VERSION_ID));
        user.setSalt(cryptHelper.generateSalt());
    }

    private void setUserEncryptionSaltAndVersionForFedUser(FederatedUser user) {
        user.setEncryptionVersion(propertiesService.getValue(ENCRYPTION_VERSION_ID));
        user.setSalt(cryptHelper.generateSalt());
    }

    @Override
    public void encryptUser(EndUser user) {
        if(user instanceof User) {
            encryptProvisionedUser((User) user);
        } else if (user instanceof FederatedUser) {
            encryptFederatedUser((FederatedUser) user);
        } else {
            logger.error(String.format("Unknown user type '%s' when setting encrypting " +
                    "user objects ", user.getClass().getName()));
            throw new IllegalStateException(String.format("Unknown user type '%s'", user.getClass().getName()));
        }
    }

    private void encryptProvisionedUser(User user) {
        String encryptionVersionId = getCryptoVersionId(user);
        String encryptionSalt = getCryptoSalt(user);
        user.setEncryptionVersion(encryptionVersionId);
        user.setSalt(encryptionSalt);

        try {
            if (user.getSecretQuestion() != null ) {
                user.setEncryptedSecretQuestion(cryptHelper.encrypt(user.getSecretQuestion(), encryptionVersionId, encryptionSalt));
            }
        } catch (GeneralSecurityException e) {
            logger.error(String.format(PROV_USER_ENCRYPTION_ERROR_MESSAGE, SECRET_QUESTION, user.getId()), e);
        } catch (InvalidCipherTextException e) {
            logger.error(String.format(PROV_USER_ENCRYPTION_ERROR_MESSAGE, SECRET_QUESTION, user.getId()), e);
        }

        try {
            if (user.getSecretAnswer() != null) {
                user.setEncryptedSecretAnswer(cryptHelper.encrypt(user.getSecretAnswer(), encryptionVersionId, encryptionSalt));
            }
        } catch (GeneralSecurityException e) {
            logger.error(String.format(PROV_USER_ENCRYPTION_ERROR_MESSAGE, SECRET_ANSWER, user.getId()), e);
        } catch (InvalidCipherTextException e) {
            logger.error(String.format(PROV_USER_ENCRYPTION_ERROR_MESSAGE, SECRET_ANSWER, user.getId()), e);
        }

        try {
            if (user.getSecretQuestionId() != null) {
                user.setEncryptedSecretQuestionId(cryptHelper.encrypt(user.getSecretQuestionId(), encryptionVersionId, encryptionSalt));
            }
        } catch (GeneralSecurityException e) {
            logger.error(String.format(PROV_USER_ENCRYPTION_ERROR_MESSAGE, SECRET_QUESTION_ID, user.getId()), e);
        } catch (InvalidCipherTextException e) {
            logger.error(String.format(PROV_USER_ENCRYPTION_ERROR_MESSAGE, SECRET_QUESTION_ID, user.getId()), e);
        }

        try {
            if (user.getDisplayName() != null) {
                user.setEncryptedDisplayName(cryptHelper.encrypt(user.getDisplayName(), encryptionVersionId, encryptionSalt));
            }
        } catch (GeneralSecurityException e) {
            logger.error(String.format(PROV_USER_ENCRYPTION_ERROR_MESSAGE, DISPLAY_NAME, user.getId()), e);
        } catch (InvalidCipherTextException e) {
            logger.error(String.format(PROV_USER_ENCRYPTION_ERROR_MESSAGE, DISPLAY_NAME, user.getId()), e);
        }

        try {
            if (user.getApiKey() != null) {
                user.setEncryptedApiKey(cryptHelper.encrypt(user.getApiKey(), encryptionVersionId, encryptionSalt));
            }
        } catch (GeneralSecurityException e) {
            logger.error(String.format(PROV_USER_ENCRYPTION_ERROR_MESSAGE, API_KEY, user.getId()), e);
        } catch (InvalidCipherTextException e) {
            logger.error(String.format(PROV_USER_ENCRYPTION_ERROR_MESSAGE, API_KEY, user.getId()), e);
        }

        try {
            if (user.getPhonePin() != null) {
                user.setEncryptedPhonePin(cryptHelper.encrypt(user.getPhonePin(), encryptionVersionId, encryptionSalt));
            }
        } catch (GeneralSecurityException e) {
            logger.error(String.format(PROV_USER_ENCRYPTION_ERROR_MESSAGE, PHONE_PIN, user.getId()), e);
        } catch (InvalidCipherTextException e) {
            logger.error(String.format(PROV_USER_ENCRYPTION_ERROR_MESSAGE, PHONE_PIN, user.getId()), e);
        }
    }


    private void encryptFederatedUser(FederatedUser user) {
        String encryptionVersionId = getCryptoVersionId(user);
        String encryptionSalt = getCryptoSalt(user);
        user.setEncryptionVersion(encryptionVersionId);
        user.setSalt(encryptionSalt);

        try {
            if (user.getPhonePin() != null) {
                user.setEncryptedPhonePin(cryptHelper.encrypt(user.getPhonePin(), encryptionVersionId, encryptionSalt));
            }
        } catch (GeneralSecurityException e) {
            logger.error(String.format(FED_USER_ENCRYPTION_ERROR_MESSAGE, PHONE_PIN, user.getId()), e);
        } catch (InvalidCipherTextException e) {
            logger.error(String.format(FED_USER_ENCRYPTION_ERROR_MESSAGE, PHONE_PIN, user.getId()), e);
        }

    }

    private String getCryptoVersionId(EndUser user) {
        if(user instanceof User && StringUtils.isNotBlank(((User) user).getEncryptionVersion())) {
            return ((User) user).getEncryptionVersion();
        } else if (user instanceof FederatedUser && StringUtils.isNotBlank(((FederatedUser) user).getEncryptionVersion())) {
            return ((FederatedUser) user).getEncryptionVersion();
        }
        return propertiesService.getValue(ENCRYPTION_VERSION_ID);
    }

    private String getCryptoSalt(EndUser user) {
        if(user instanceof User && StringUtils.isNotBlank(((User) user).getSalt())) {
            return ((User) user).getSalt();
        } else if (user instanceof FederatedUser && StringUtils.isNotBlank(((FederatedUser) user).getSalt())) {
            return ((FederatedUser) user).getSalt();
        }
        return config.getString(CRYPTO_SALT);
    }

    @Override
    public void decryptUser(EndUser user) {
        if(user instanceof User) {
            decryptProvisionedUser((User) user);
        } else if (user instanceof FederatedUser) {
            decryptFederatedUser((FederatedUser) user);
        } else {
            logger.error(String.format("Unknown user type '%s' when setting decrypting " +
                    "user objects ", user.getClass().getName()));
            throw new IllegalStateException(String.format("Unknown user type '%s'", user.getClass().getName()));
        }
    }

    private void decryptProvisionedUser(User user) {
        String encryptionVersionId = getCryptoVersionId(user);
        String encryptionSalt = getCryptoSalt(user);

        try {
            if (user.getEncryptedSecretQuestion() != null) {
                user.setSecretQuestion(cryptHelper.decrypt(user.getEncryptedSecretQuestion(), encryptionVersionId, encryptionSalt));
            }
        } catch (GeneralSecurityException e) {
            logger.error(String.format(PROV_USER_DECRYPTION_ERROR_MESSAGE, SECRET_QUESTION, user.getId()), e);
        } catch (InvalidCipherTextException e) {
            logger.error(String.format(PROV_USER_DECRYPTION_ERROR_MESSAGE, SECRET_QUESTION, user.getId()), e);
        }

        try {
            if (user.getEncryptedSecretAnswer() != null) {
                user.setSecretAnswer(cryptHelper.decrypt(user.getEncryptedSecretAnswer(), encryptionVersionId, encryptionSalt));
            }
        } catch (GeneralSecurityException e) {
            logger.error(String.format(PROV_USER_DECRYPTION_ERROR_MESSAGE, SECRET_ANSWER, user.getId()), e);
        } catch (InvalidCipherTextException e) {
            logger.error(String.format(PROV_USER_DECRYPTION_ERROR_MESSAGE, SECRET_ANSWER, user.getId()), e);
        }

        try {
            if (user.getEncryptedSecretQuestionId() != null) {
                user.setSecretQuestionId(cryptHelper.decrypt(user.getEncryptedSecretQuestionId(), encryptionVersionId, encryptionSalt));
            }
        } catch (GeneralSecurityException e) {
            logger.error(String.format(PROV_USER_DECRYPTION_ERROR_MESSAGE, SECRET_QUESTION_ID, user.getId()), e);
        } catch (InvalidCipherTextException e) {
            logger.error(String.format(PROV_USER_DECRYPTION_ERROR_MESSAGE, SECRET_QUESTION_ID, user.getId()), e);
        }

        try {
            if (user.getEncryptedDisplayName() != null) {
                user.setDisplayName(cryptHelper.decrypt(user.getEncryptedDisplayName(), encryptionVersionId, encryptionSalt));
            }
        } catch (GeneralSecurityException e) {
            logger.error(String.format(PROV_USER_DECRYPTION_ERROR_MESSAGE, DISPLAY_NAME, user.getId()), e);
        } catch (InvalidCipherTextException e) {
            logger.error(String.format(PROV_USER_DECRYPTION_ERROR_MESSAGE, DISPLAY_NAME, user.getId()), e);
        }

        try {
            if (user.getEncryptedApiKey() != null) {
                user.setApiKey(cryptHelper.decrypt(user.getEncryptedApiKey(), encryptionVersionId, encryptionSalt));
            }
        } catch (GeneralSecurityException e) {
            logger.error(String.format(PROV_USER_DECRYPTION_ERROR_MESSAGE, API_KEY, user.getId()), e);
        } catch (InvalidCipherTextException e) {
            logger.error(String.format(PROV_USER_DECRYPTION_ERROR_MESSAGE, API_KEY, user.getId()), e);
        }

        try {
            if (user.getEncryptedPhonePin() != null) {
                user.setPhonePin(cryptHelper.decrypt(user.getEncryptedPhonePin(), encryptionVersionId, encryptionSalt));
            }
        } catch (GeneralSecurityException e) {
            logger.error(String.format(PROV_USER_DECRYPTION_ERROR_MESSAGE, PHONE_PIN, user.getId()), e);
        } catch (InvalidCipherTextException e) {
            logger.error(String.format(PROV_USER_DECRYPTION_ERROR_MESSAGE, PHONE_PIN, user.getId()), e);
        }
    }

    private void decryptFederatedUser(FederatedUser user) {
        String encryptionVersionId = getCryptoVersionId(user);
        String encryptionSalt = getCryptoSalt(user);

        try {
            if (user.getEncryptedPhonePin() != null) {
                user.setPhonePin(cryptHelper.decrypt(user.getEncryptedPhonePin(), encryptionVersionId, encryptionSalt));
            }
        } catch (GeneralSecurityException e) {
            logger.error(String.format(FED_USER_DECRYPTION_ERROR_MESSAGE, PHONE_PIN, user.getId()), e);
        } catch (InvalidCipherTextException e) {
            logger.error(String.format(FED_USER_DECRYPTION_ERROR_MESSAGE, PHONE_PIN, user.getId()), e);
        }
    }

    @Override
    public String getEncryptionVersionId() {
        return propertiesService.getValue(ENCRYPTION_VERSION_ID);
    }
}
