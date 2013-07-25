package com.rackspace.idm.util

import org.apache.commons.configuration.PropertiesConfiguration
import org.jasypt.encryption.StringEncryptor
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor
import spock.lang.Shared
import spock.lang.Specification

/**
 */
class EncryptedPropertiesConfigurationTest  extends Specification {
    @Shared PropertiesConfiguration encryptedPropertiesConfiguration

    private static final String BASE_PROPERTIES = "com/rackspace/idm/util/encryptedpropertiesconfiguration/base.properties";

    /*
    Using a low quality encryption algorithm that does not require unlimited strength jurisdiction policy jars to be
    installed
    */
    private static final String ALGORITHM_NAME = "PBEWITHMD5ANDDES"
    private static final String PROVIDER_NAME = "BC"
    private static final String PASSWORD = "password"

    def "Retrieve unencrypted string" (){
        when:
         def unencrypted_property = encryptedPropertiesConfiguration.getString("unencrypted_property")

        then:
        unencrypted_property == "unencrypted"
    }

    def "Retrieve unencrypted boolean" (){
        when:
        def unencrypted_boolean = encryptedPropertiesConfiguration.getBoolean("unencrypted_boolean")

        then:
        unencrypted_boolean == false
    }

    def "Retrieve encrypted string property" (){
        when:
        def encrypted_string = encryptedPropertiesConfiguration.getString("encrypted_string")

        then:
        encrypted_string == "a string"
    }

    def "Retrieve multiline encrypted string array" (){
        when:
        def encrypted_multiline_array = encryptedPropertiesConfiguration.getStringArray("encrypted_multiline_array")

        then:
        encrypted_multiline_array[0] == "item1"
        encrypted_multiline_array[1] == "item2"
    }

    def "Retrieve single line encrypted string array" (){
        when:
        def encrypted_array = encryptedPropertiesConfiguration.getStringArray("encrypted_array")

        then:
        encrypted_array[0] == "item1"
        encrypted_array[1] == "item2"
    }

    def "Retrieve included file unencrypted string" (){
        when:
        def included_prop_unencrypted_string = encryptedPropertiesConfiguration.getString("included_prop_unencrypted_string")

        then:
        included_prop_unencrypted_string == "unencrypted"
    }

    def "Retrieve included file encrypted string" (){
        when:
        def included_prop_encrypted_string = encryptedPropertiesConfiguration.getString("included_prop_encrypted_string")

        then:
        included_prop_encrypted_string == "encrypted"
    }

    def "Encrypted string with escaped characters"() {
        when:
        def encrypted_escaped = encryptedPropertiesConfiguration.getString("encrypted_escaped")

        then:
        encrypted_escaped == "cn=admin,dc=rackspace,dc=com"
    }

    def "List of ints returns as strings"() {
        when:
        def str_list = encryptedPropertiesConfiguration.getList("int_list")

        then:
        str_list.contains(1000.toString())
    }

    def "Single int returns as string list"() {
        when:
        def int_list_oneitem = encryptedPropertiesConfiguration.getList("int_list_oneitem")

        then:
        int_list_oneitem.contains(1.toString())
    }


    def "Empty list returns non-null"() {
        when:
        def empty_list = encryptedPropertiesConfiguration.getList("empty_list")

        then:
        empty_list != null
    }

    def "Can decrypt encrypted strings that are surrounded with ENC()"() {
        when:
        def encrypted_encryptedBlock = encryptedPropertiesConfiguration.getString("encrypted_encryptedBlock")

        then:
        encrypted_encryptedBlock == "ENC(rawstring)"
    }

    def setup() {
        setupMock()
    }

    def setupMock(){
        StringEncryptor encryptor = new StandardPBEStringEncryptor();
        encryptor.setPassword(PASSWORD)
        encryptor.setAlgorithm(ALGORITHM_NAME)
        encryptor.setProviderName(PROVIDER_NAME)

        EncryptedIOFactory encryptedIOFactory = new EncryptedIOFactory(encryptor)

        encryptedPropertiesConfiguration = new PropertiesConfiguration()
        encryptedPropertiesConfiguration.setIOFactory(encryptedIOFactory)
        encryptedPropertiesConfiguration.setFileName(BASE_PROPERTIES)
        encryptedPropertiesConfiguration.load()
    }

}
