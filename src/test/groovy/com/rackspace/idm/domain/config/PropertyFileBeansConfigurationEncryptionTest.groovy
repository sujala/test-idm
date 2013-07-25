package com.rackspace.idm.domain.config

import com.rackspace.idm.util.SystemEnvPropertyConfigurationFactory
import org.apache.commons.configuration.PropertiesConfiguration
import spock.lang.Shared
import spock.lang.Specification

class PropertyFileBeansConfigurationEncryptionTest extends Specification {

    private static final String BASE_PROPERTIES = "com/rackspace/idm/domain/config/propertyfilebeansconfiguration/base.properties";
    private static final String ALGORITHM_NAME = "PBEWITHSHA1ANDDES"
    private static final String PROVIDER_NAME = "BC"
    private static final String PASSWORD = "password"

    private static final String UNENCRYPTED_PROPERTY = "unencrypted_property"
    private static final String ENCRYPTED_PROPERTY = "encrypted_property"

    @Shared PropertyFileConfiguration configFileLoader

    def setup(){
        configFileLoader = new PropertyFileConfiguration()
    }

    def "PropertyFileConfiguration returns encryption disabled based on System property"() {

        when: System.setProperty(PropertyFileConfiguration.PROPERTY_ENCRYPTION_DISABLED_PROP_NAME, "true")
        then: assert !configFileLoader.isPropertyEncryptionEnabled()

        when: System.setProperty(PropertyFileConfiguration.PROPERTY_ENCRYPTION_DISABLED_PROP_NAME, "TRUE")
        then: assert !configFileLoader.isPropertyEncryptionEnabled()

        when: System.setProperty(PropertyFileConfiguration.PROPERTY_ENCRYPTION_DISABLED_PROP_NAME, "false")
        then: assert configFileLoader.isPropertyEncryptionEnabled()

        when: System.setProperty(PropertyFileConfiguration.PROPERTY_ENCRYPTION_DISABLED_PROP_NAME, "asdf")
        then: assert configFileLoader.isPropertyEncryptionEnabled()

        when: System.clearProperty(PropertyFileConfiguration.PROPERTY_ENCRYPTION_DISABLED_PROP_NAME)
        then: assert configFileLoader.isPropertyEncryptionEnabled()

        cleanup:
        System.clearProperty(PropertyFileConfiguration.PROPERTY_ENCRYPTION_DISABLED_PROP_NAME)
    }

    def "Decryption occurs when disabled property set to false"() {
        System.setProperty(PropertyFileConfiguration.PROPERTY_ENCRYPTION_DISABLED_PROP_NAME, "false")
        System.setProperty(SystemEnvPropertyConfigurationFactory.PROPERTY_ENCRYPTION_ALGORITHM_ATTR_NAME, ALGORITHM_NAME)
        System.setProperty(SystemEnvPropertyConfigurationFactory.PROPERTY_ENCRYPTION_PASSWORD_ATTR_NAME, PASSWORD)
        System.setProperty(SystemEnvPropertyConfigurationFactory.PROPERTY_ENCRYPTION_PROVIDER_NAME_ATTR_NAME, PROVIDER_NAME)

        when:
            PropertiesConfiguration config = (PropertiesConfiguration) configFileLoader.readConfigFile(BASE_PROPERTIES)

        then:
            verifyEncryptionEnabledResults(config)
    }

    def "Decryption occurs when disabled property not set"() {
        System.clearProperty(PropertyFileConfiguration.PROPERTY_ENCRYPTION_DISABLED_PROP_NAME)
        System.setProperty(SystemEnvPropertyConfigurationFactory.PROPERTY_ENCRYPTION_ALGORITHM_ATTR_NAME, ALGORITHM_NAME)
        System.setProperty(SystemEnvPropertyConfigurationFactory.PROPERTY_ENCRYPTION_PASSWORD_ATTR_NAME, PASSWORD)
        System.setProperty(SystemEnvPropertyConfigurationFactory.PROPERTY_ENCRYPTION_PROVIDER_NAME_ATTR_NAME, PROVIDER_NAME)

        when:
        PropertiesConfiguration config = (PropertiesConfiguration) configFileLoader.readConfigFile(BASE_PROPERTIES)

        then:
        verifyEncryptionEnabledResults(config)
    }

    def "Decryption throws error when password does not match"() {
        System.clearProperty(PropertyFileConfiguration.PROPERTY_ENCRYPTION_DISABLED_PROP_NAME)
        System.setProperty(SystemEnvPropertyConfigurationFactory.PROPERTY_ENCRYPTION_ALGORITHM_ATTR_NAME, ALGORITHM_NAME)
        System.setProperty(SystemEnvPropertyConfigurationFactory.PROPERTY_ENCRYPTION_PASSWORD_ATTR_NAME, "wrong password")
        System.setProperty(SystemEnvPropertyConfigurationFactory.PROPERTY_ENCRYPTION_PROVIDER_NAME_ATTR_NAME, PROVIDER_NAME)

        when:
        PropertiesConfiguration config = (PropertiesConfiguration) configFileLoader.readConfigFile(BASE_PROPERTIES)

        then:
        thrown(IllegalStateException)
    }

    def "Decryption does not occur when encryption is disabled with encryption propreties set"() {
        System.setProperty(PropertyFileConfiguration.PROPERTY_ENCRYPTION_DISABLED_PROP_NAME, "true")
        System.setProperty(SystemEnvPropertyConfigurationFactory.PROPERTY_ENCRYPTION_ALGORITHM_ATTR_NAME, ALGORITHM_NAME)
        System.setProperty(SystemEnvPropertyConfigurationFactory.PROPERTY_ENCRYPTION_PASSWORD_ATTR_NAME, PASSWORD)
        System.setProperty(SystemEnvPropertyConfigurationFactory.PROPERTY_ENCRYPTION_PROVIDER_NAME_ATTR_NAME, PROVIDER_NAME)

        when:
        PropertiesConfiguration config = (PropertiesConfiguration) configFileLoader.readConfigFile(BASE_PROPERTIES)

        then:
        verifyEncryptionDisabledResults(config)
    }

    def "Decryption does not occur when encryption is disabled and no encryption properties provided"() {
        System.setProperty(PropertyFileConfiguration.PROPERTY_ENCRYPTION_DISABLED_PROP_NAME, "true")
        System.clearProperty(SystemEnvPropertyConfigurationFactory.PROPERTY_ENCRYPTION_ALGORITHM_ATTR_NAME)
        System.clearProperty(SystemEnvPropertyConfigurationFactory.PROPERTY_ENCRYPTION_PASSWORD_ATTR_NAME)
        System.clearProperty(SystemEnvPropertyConfigurationFactory.PROPERTY_ENCRYPTION_PROVIDER_NAME_ATTR_NAME)

        when:
        PropertiesConfiguration config = (PropertiesConfiguration) configFileLoader.readConfigFile(BASE_PROPERTIES)

        then:
        verifyEncryptionDisabledResults(config)
    }

    def "Decryption does not occur when encryption is disabled and password does not match"() {
        System.setProperty(PropertyFileConfiguration.PROPERTY_ENCRYPTION_DISABLED_PROP_NAME, "true")
        System.setProperty(SystemEnvPropertyConfigurationFactory.PROPERTY_ENCRYPTION_ALGORITHM_ATTR_NAME, ALGORITHM_NAME)
        System.setProperty(SystemEnvPropertyConfigurationFactory.PROPERTY_ENCRYPTION_PASSWORD_ATTR_NAME, "wrong password")
        System.setProperty(SystemEnvPropertyConfigurationFactory.PROPERTY_ENCRYPTION_PROVIDER_NAME_ATTR_NAME, PROVIDER_NAME)

        when:
        PropertiesConfiguration config = (PropertiesConfiguration) configFileLoader.readConfigFile(BASE_PROPERTIES)

        then:
        verifyEncryptionDisabledResults(config)
    }

    def void verifyEncryptionEnabledResults(config) {
        def encryptedProperyValue = config.getString(ENCRYPTED_PROPERTY)
        def unencryptedPropertyValue = config.getString(UNENCRYPTED_PROPERTY)

        assert encryptedProperyValue == "a string"
        assert unencryptedPropertyValue == "unencrypted"
    }

    def void verifyEncryptionDisabledResults(config) {
        def encryptedProperyValue = config.getString(ENCRYPTED_PROPERTY)
        def unencryptedPropertyValue = config.getString(UNENCRYPTED_PROPERTY)

        assert encryptedProperyValue == "ENC(OGEJz6kFatGHv+fTiQONLcnd2GLlbua2)"
        assert unencryptedPropertyValue == "unencrypted"
    }

}
