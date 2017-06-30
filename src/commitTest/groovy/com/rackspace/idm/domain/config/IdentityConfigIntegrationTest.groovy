package com.rackspace.idm.domain.config

import com.rackspace.idm.api.converter.cloudv20.IdentityPropertyValueConverter
import com.rackspace.idm.domain.security.TokenFormat
import com.rackspace.idm.domain.service.IdentityPropertyService
import com.rackspace.test.SingleTestConfiguration
import org.apache.commons.configuration.PropertiesConfiguration
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy
import org.junit.Rule
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.test.context.ContextConfiguration
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll
import testHelpers.SingletonConfiguration
import testHelpers.SingletonReloadableConfiguration
import testHelpers.SingletonTestFileConfiguration
import testHelpers.junit.ConditionalIgnoreRule
import testHelpers.junit.IgnoreByRepositoryProfile

@ContextConfiguration(classes=[SingletonTestFileConfiguration.class
        , IdentityConfig.class
        , MockServiceProvider.class])
class IdentityConfigIntegrationTest  extends Specification {

    @Autowired
    private IdentityConfig config;

    @Autowired
    private RepositoryProfileResolver profileResolver

    @Shared SingletonConfiguration staticIdmConfiguration = SingletonConfiguration.getInstance();
    @Shared SingletonReloadableConfiguration reloadableConfiguration = SingletonReloadableConfiguration.getInstance();

    @Rule
    public ConditionalIgnoreRule role = new ConditionalIgnoreRule()

    String testIpdLabeledUriAe = "http://www.test.com/ae"
    String testIpdLabeledUriUUID = "http://www.test.com/uuid"
    String testIpdLabeledUriNone = "http://www.test.com/none"

    public setupSpec(){
        staticIdmConfiguration.reset()
    }

    public cleanupSpec() {
        staticIdmConfiguration.reset()
    }

    def "reloadable properties exist"() {
        expect:
        config.getReloadableConfig() != null
        config.reloadableConfiguration  instanceof SingletonReloadableConfiguration
        config.reloadableConfiguration.idmPropertiesConfig.strategy instanceof FileChangedReloadingStrategy
        config.reloadableConfiguration.idmPropertiesConfig.strategy.refreshDelay == config.staticConfig.getReloadablePropertiesTTL() * 1000
    }

    def "reloadable properties works"() {
        given:
        def newProp = "new.property"
        def tempFile = File.createTempFile("temp",".tmp").with {
            deleteOnExit()
            return it
        }

        FileChangedReloadingStrategy strategy = new FileChangedReloadingStrategy();
        strategy.setRefreshDelay(0);
        PropertiesConfiguration localConfig = new PropertiesConfiguration();
        localConfig.setReloadingStrategy(strategy);
        localConfig.setFile(tempFile);
        localConfig.load();

        when: "load prop when doesn't exist"
        //nothing to do

        then:
        localConfig.getString(newProp) == null

        when: "add new prop"
        def newPropOrigValue = "value1"
        System.sleep(1000) //need to delay a second so lastmodifieddate of file will change
        writeProp(tempFile, newProp, newPropOrigValue)

        then: "when check again it exists"
        localConfig.getString(newProp) == newPropOrigValue

        when: "change value"
        def newPropOrigValue2 = "value2"
        System.sleep(1000) //need to delay a second so lastmodifieddate of file will change
        writeProp(tempFile, newProp, newPropOrigValue2)

        then: "when check again it is changed"
        localConfig.getString(newProp) == newPropOrigValue2
    }

    @Unroll
    def "Test correct user count limit per idp per domain retrieved when override: #overrideVal; defaultVal: #defaultValue; expectedVal: #expectedVal"() {
        given:
        String idp = "http://ran" + UUID.randomUUID().toString().replaceAll("-", "")
        String idpOverrideProp = String.format(IdentityConfig.IDENTITY_FEDERATED_IDP_MAX_USER_PER_DOMAIN_PROP_REG, idp)
        if (overrideVal == null) {
            reloadableConfiguration.clearProperty(idpOverrideProp)
        } else {
            reloadableConfiguration.setProperty(idpOverrideProp, overrideVal)
        }
        if (defaultVal == null) {
            reloadableConfiguration.clearProperty(IdentityConfig.IDENTITY_FEDERATED_IDP_MAX_USER_PER_DOMAIN_DEFAULT_PROP)
        } else {
            reloadableConfiguration.setProperty(IdentityConfig.IDENTITY_FEDERATED_IDP_MAX_USER_PER_DOMAIN_DEFAULT_PROP, defaultVal)
        }

        expect:
        config.getReloadableConfig().getIdentityFederationMaxUserCountPerDomainForIdp(idp) == expectedVal

        cleanup:
        reloadableConfiguration.reset()

        where:
        overrideVal | defaultVal | expectedVal
        10          | 5          | 10
        5           | 10         | 5
        null        | 4          | 4
        3           | null       | 3
        null        | null       | 1000
        "asdf"      | 8          | 8
        "asdf"      | "asdf"     | 1000
        3.14        | 2          | 3
    }

    def writeProp(file, name, val) {
        file.write(name + "=" + val)
    }

    /**
     * These tests expect the properties to exist in the idm property file
     * <ul>
     * <li>federated.provider.tokenFormat.http\://www.test.com/uuid=UUID</li>
     * <li>federated.provider.tokenFormat.http\://www.test.com/ae=AE</li>
     * </ul>
     * @return
     */
    def verifyRetrievingFederatedTokenFormatForIdpProperty() {
        //set default for when

        when: "retrieving the override property that is set to use AE tokens"
        def format = config.getReloadableConfig().getIdentityFederationRequestTokenFormatForIdp(testIpdLabeledUriAe)

        then: "get AE token format back"
        format == TokenFormat.AE

        when: "retrieving the override property that is set to use UUID tokens"
        format = config.getReloadableConfig().getIdentityFederationRequestTokenFormatForIdp(testIpdLabeledUriUUID)

        then: "get UUID token format back"
        if(profileResolver.getActiveRepositoryProfile() == SpringRepositoryProfileEnum.SQL) {
            assert format == TokenFormat.AE
        } else {
            assert format == TokenFormat.UUID
        }

        when: "override for that idp does not exist"
        reloadableConfiguration.setProperty(IdentityConfig.IDENTITY_FEDERATED_TOKEN_FORMAT_DEFAULT_PROP, TokenFormat.UUID.name())
        def formatDefUUID = config.getReloadableConfig().getIdentityFederationRequestTokenFormatForIdp(testIpdLabeledUriNone)

        reloadableConfiguration.setProperty(IdentityConfig.IDENTITY_FEDERATED_TOKEN_FORMAT_DEFAULT_PROP, TokenFormat.AE.name())
        def formatDefAe = config.getReloadableConfig().getIdentityFederationRequestTokenFormatForIdp(testIpdLabeledUriNone)

        then: "the default property is used"
        if(profileResolver.getActiveRepositoryProfile() == SpringRepositoryProfileEnum.SQL) {
            assert formatDefUUID == TokenFormat.AE
        } else {
            formatDefUUID == TokenFormat.UUID
        }
        formatDefAe == TokenFormat.AE
    }

    @IgnoreByRepositoryProfile(profile = SpringRepositoryProfileEnum.LDAP)
    def "force AE token use for SQL profile"() {
        expect:
        config.getReloadableConfig().getFeatureAETokensDecrypt() == IdentityConfig.FEATURE_AE_TOKENS_DECRYPT_SQL_OVERRIDE
        config.getReloadableConfig().getFeatureAETokensEncrypt() == IdentityConfig.FEATURE_AE_TOKENS_ENCRYPT_SQL_OVERRIDE
        config.getStaticConfig().getIdentityProvisionedTokenFormat() == IdentityConfig.IDENTITY_PROVISIONED_TOKEN_SQL_OVERRIDE
        config.getReloadableConfig().getIdentityFederationRequestTokenFormatForIdp() == IdentityConfig.IDENTITY_FEDERATED_IDP_TOKEN_FORMAT_SQL_OVERRIDE
    }

    @SingleTestConfiguration
    static class MockServiceProvider {
        @Bean
        public IdentityPropertyValueConverter identityPropertyValueConverter () {
            return  Mockito.mock(IdentityPropertyValueConverter.class);
        }
        @Bean
        public IdentityPropertyService identityPropertyService () {
            return  Mockito.mock(IdentityPropertyService.class);
        }
        @Bean
        public RepositoryProfileResolver repositoryProfileResolver () {
            return  Mockito.mock(RepositoryProfileResolver.class);
        }
    }
}
