package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.entity.Application;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.EncryptionService;
import com.rackspace.idm.domain.service.PropertiesService;
import com.rackspace.idm.util.CryptHelper;
import org.apache.commons.configuration.Configuration;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.security.GeneralSecurityException;

@Component
public class DefaultEncryptionService implements EncryptionService {

    public static final String ENCRYPTION_VERSION_ID = "encryptionVersionId";

    @Autowired
    CryptHelper cryptHelper;

    @Autowired
    Configuration config;

    @Autowired
    PropertiesService propertiesService;

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
            if (user.getSecretAnswer() != null) {
                user.setEncryptedSecretAnswer(cryptHelper.encrypt(user.getSecretAnswer(), encryptionVersionId, encryptionSalt));
            }
            if (user.getSecretQuestionId() != null) {
                user.setEncryptedSecretQuestionId(cryptHelper.encrypt(user.getSecretQuestionId(), encryptionVersionId, encryptionSalt));
            }
            if (user.getFirstname() != null) {
                user.setEncryptedFirstName(cryptHelper.encrypt(user.getFirstname(), encryptionVersionId, encryptionSalt));
            }
            if (user.getLastname() != null) {
                user.setEncryptedLastname(cryptHelper.encrypt(user.getLastname(), encryptionVersionId, encryptionSalt));
            }
            if (user.getDisplayName() != null) {
                user.setEncryptedDisplayName(cryptHelper.encrypt(user.getDisplayName(), encryptionVersionId, encryptionSalt));
            }
            if (user.getApiKey() != null) {
                user.setEncryptedApiKey(cryptHelper.encrypt(user.getApiKey(), encryptionVersionId, encryptionSalt));
            }
            if (user.getPassword() != null) {
                user.setEncryptedClearPassword(cryptHelper.encrypt(user.getPassword(), encryptionVersionId, encryptionSalt));
            }
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        } catch (InvalidCipherTextException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void encryptApplication(Application application) {
        String encryptionVersionId = getEncryptionVersionId(application);
        String encryptionSalt = getEncryptionSalt(application);
        application.setEncryptionVersion(encryptionVersionId);
        application.setSalt(encryptionSalt);

        try {
            if (application.getClearPassword() != null) {
                application.setClearPasswordBytes(cryptHelper.encrypt(application.getClearPassword(), encryptionVersionId, encryptionSalt));
            }
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        } catch (InvalidCipherTextException e) {
            e.printStackTrace();
        }
    }

    private String getEncryptionSalt(User user) {
        if (user.getSalt() == null) {
            return cryptHelper.generateSalt();
        } else {
            return user.getSalt();
        }
    }

    private String getEncryptionSalt(Application application) {
        if (application.getSalt() == null) {
            return cryptHelper.generateSalt();
        } else {
            return application.getSalt();
        }
    }

    private String getEncryptionVersionId(User user) {
        if (user.getEncryptionVersion() == null) {
            return propertiesService.getValue(ENCRYPTION_VERSION_ID);
        } else {
            return user.getEncryptionVersion();
        }
    }

    private String getEncryptionVersionId(Application application) {
        if (application.getEncryptionVersion() == null) {
            return propertiesService.getValue(ENCRYPTION_VERSION_ID);
        } else {
            return application.getEncryptionVersion();
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
            if (user.getEncryptedSecretAnswer() != null) {
                user.setSecretAnswer(cryptHelper.decrypt(user.getEncryptedSecretAnswer(), encryptionVersionId, encryptionSalt));
            }
            if (user.getEncryptedSecretQuestionId() != null) {
                user.setSecretQuestionId(cryptHelper.decrypt(user.getEncryptedSecretQuestionId(), encryptionVersionId, encryptionSalt));
            }
            if (user.getEncryptedFirstName() != null) {
                user.setFirstname(cryptHelper.decrypt(user.getEncryptedFirstName(), encryptionVersionId, encryptionSalt));
            }
            if (user.getEncryptedLastname() != null) {
                user.setLastname(cryptHelper.decrypt(user.getEncryptedLastname(), encryptionVersionId, encryptionSalt));
            }
            if (user.getEncryptedDisplayName() != null) {
                user.setDisplayName(cryptHelper.decrypt(user.getEncryptedDisplayName(), encryptionVersionId, encryptionSalt));
            }
            if (user.getEncryptedApiKey() != null) {
                user.setApiKey(cryptHelper.decrypt(user.getEncryptedApiKey(), encryptionVersionId, encryptionSalt));
            }
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        } catch (InvalidCipherTextException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void decryptApplication(Application application) {
        String encryptionVersionId = getDecryptionVersionId(application);
        String encryptionSalt = getDecryptionSalt(application);

        try {
            if (application.getClearPasswordBytes() != null) {
                application.setClearPassword(cryptHelper.decrypt(application.getClearPasswordBytes(), encryptionVersionId, encryptionSalt));
            }
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
        } catch (InvalidCipherTextException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getEncryptionVersionId() {
        return propertiesService.getValue(ENCRYPTION_VERSION_ID);
    }

    private String getDecryptionVersionId(User user) {
        if (user.getEncryptionVersion() == null) {
            return "0";
        } else {
            return user.getEncryptionVersion();
        }
    }

    private String getDecryptionSalt(User user) {
        if (user.getSalt() == null) {
            return config.getString("crypto.salt");
        } else {
            return user.getSalt();
        }
    }

    private String getDecryptionVersionId(Application application) {
        if (application.getEncryptionVersion() == null) {
            return "0";
        } else {
            return application.getEncryptionVersion();
        }
    }

    private String getDecryptionSalt(Application application) {
        if (application.getSalt() == null) {
            return config.getString("crypto.salt");
        } else {
            return application.getSalt();
        }
    }
}
