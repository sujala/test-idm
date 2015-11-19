package com.rackspace.idm.domain.config

import com.rackspace.idm.domain.security.TokenFormat
import org.apache.commons.configuration.PropertiesConfiguration
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy
import org.junit.Rule
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll
import testHelpers.SingletonConfiguration
import testHelpers.SingletonReloadableConfiguration
import testHelpers.junit.ConditionalIgnoreRule
import testHelpers.junit.IgnoreByRepositoryProfile

@ContextConfiguration(locations = "classpath:app-config.xml")
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

    def "test fallback for invalid property set for FEATURE_USER_DISABLED_BY_TENANTS_ENABLED_PROP"() {
        given:
        def tempFile = File.createTempFile("temp",".tmp").with {
            deleteOnExit()
            return it
        }
        def reloadablePropertiesFile = config.reloadableConfiguration.idmPropertiesConfig.file
        config.reloadableConfiguration.idmPropertiesConfig.file = tempFile

        when:
        writeProp(tempFile, IdentityConfig.FEATURE_USER_DISABLED_BY_TENANTS_ENABLED_PROP, propertyValue)
        config.reloadableConfiguration.idmPropertiesConfig.refresh()
        boolean returnedProp = config.reloadableConfig.getFeatureUserDisabledByTenantsEnabled()

        then:
        returnedProp == returnValue

        cleanup:
        config.reloadableConfiguration.idmPropertiesConfig.file = reloadablePropertiesFile
        config.reloadableConfiguration.idmPropertiesConfig.refresh()

        where:
        propertyValue | returnValue
        true          | true
        false         | false
        "not_valid"   | IdentityConfig.FEATURE_USER_DISABLED_BY_TENANTS_ENABLED_DEFAULT
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
        config.getStaticConfig().getFeatureAETokensDecrypt() == IdentityConfig.FEATURE_AE_TOKENS_DECRYPT_SQL_OVERRIDE
        config.getStaticConfig().getFeatureAETokensEncrypt() == IdentityConfig.FEATURE_AE_TOKENS_ENCRYPT_SQL_OVERRIDE
        config.getStaticConfig().getIdentityRackerTokenFormat() == IdentityConfig.IDENTITY_RACKER_TOKEN_SQL_OVERRIDE
        config.getStaticConfig().getIdentityProvisionedTokenFormat() == IdentityConfig.IDENTITY_PROVISIONED_TOKEN_SQL_OVERRIDE
        config.getReloadableConfig().getIdentityFederationRequestTokenFormatForIdp() == IdentityConfig.IDENTITY_FEDERATED_IDP_TOKEN_FORMAT_SQL_OVERRIDE
    }

}
