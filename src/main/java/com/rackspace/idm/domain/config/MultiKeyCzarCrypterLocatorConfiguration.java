package com.rackspace.idm.domain.config;

import com.rackspace.idm.domain.security.encrypters.FileSystemKeyCzarCrypterLocator;
import com.rackspace.idm.domain.security.encrypters.KeyCzarCrypterLocator;
import com.rackspace.idm.domain.security.encrypters.LDAPKeyCzarCrypterLocator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MultiKeyCzarCrypterLocatorConfiguration {

    private static final String CONFIG = "feature.KeyCzarCrypterLocator.storage";
    private static final String FILE = "FILE";
    private static final String LDAP = "LDAP";

    @Bean
    @Autowired
    public KeyCzarCrypterLocator getKeyCzarCrypterLocator(org.apache.commons.configuration.Configuration config) {
        if (LDAP.equalsIgnoreCase(config.getString(CONFIG, FILE))) {
            return new LDAPKeyCzarCrypterLocator();
        } else {
            return new FileSystemKeyCzarCrypterLocator(config);
        }
    }

}
