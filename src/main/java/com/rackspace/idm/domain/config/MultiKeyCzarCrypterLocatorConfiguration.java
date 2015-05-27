package com.rackspace.idm.domain.config;

import com.rackspace.idm.domain.security.encrypters.FileSystemKeyCzarCrypterLocator;
import com.rackspace.idm.domain.security.encrypters.KeyCzarCrypterLocator;
import com.rackspace.idm.domain.security.encrypters.LDAPKeyCzarCrypterLocator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MultiKeyCzarCrypterLocatorConfiguration {

    public static final String FILE = "FILE";
    public static final String LDAP = "LDAP";

    @Bean
    @Autowired
    public KeyCzarCrypterLocator keyCzarCrypterLocator(IdentityConfig config) {
        if (LDAP.equalsIgnoreCase(config.getStaticConfig().getAETokenStorageType())) {
            return new LDAPKeyCzarCrypterLocator();
        } else {
            return new FileSystemKeyCzarCrypterLocator(config);
        }
    }
}
