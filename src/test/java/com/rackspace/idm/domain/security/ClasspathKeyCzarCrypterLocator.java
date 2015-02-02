package com.rackspace.idm.domain.security;

import com.rackspace.idm.domain.security.encrypters.FileSystemKeyCzarCrypterLocator;
import com.rackspace.idm.domain.security.encrypters.KeyCzarCrypterLocator;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.keyczar.Crypter;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

/**
 * Wraps the ConfigurationKeyCzarCrypterLocator with a locator that takes in a classpath location, translates that to an
 * absolute file location, and then sets the location used by the ConfigurationKeyCzarCrypterLocator with the location of that file.
 *
 * This is useful in test situations to avoid having to have an external file location.
 */
@Getter
@Setter
public class ClasspathKeyCzarCrypterLocator implements KeyCzarCrypterLocator {

    private FileSystemKeyCzarCrypterLocator configLocator;
    private Configuration keyConfiguration;

    public ClasspathKeyCzarCrypterLocator() {
        keyConfiguration = new PropertiesConfiguration();
    }

    public void setKeysClassPathLocation(String location) {
        ClassPathResource resource = new ClassPathResource(location);
        try {
            String absoluteKeysPath = resource.getFile().getAbsolutePath();
            keyConfiguration.setProperty(FileSystemKeyCzarCrypterLocator.SCOPE_ACCESS_ENCRYPTION_KEY_LOCATION_PROP_NAME, absoluteKeysPath);
            configLocator = new FileSystemKeyCzarCrypterLocator(keyConfiguration);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Crypter getCrypter() {
        return configLocator.getCrypter();
    }
}
