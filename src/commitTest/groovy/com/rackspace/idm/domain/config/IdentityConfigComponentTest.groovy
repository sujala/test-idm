package com.rackspace.idm.domain.config

import com.rackspace.docs.identity.api.ext.rax_auth.v1.DelegationAgreement
import com.rackspace.idm.api.converter.cloudv20.IdentityPropertyValueConverter
import com.rackspace.idm.domain.entity.IdentityProperty
import com.rackspace.idm.domain.security.TokenFormat
import com.rackspace.idm.domain.service.IdentityPropertyService
import com.rackspace.test.SingleTestConfiguration
import org.apache.commons.configuration.PropertiesConfiguration
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy
import org.apache.commons.lang.StringUtils
import org.apache.commons.lang3.RandomStringUtils
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

import static org.apache.http.HttpStatus.SC_CREATED
import static org.apache.http.HttpStatus.SC_CREATED
import static org.apache.http.HttpStatus.SC_FORBIDDEN
import static org.apache.http.HttpStatus.SC_FORBIDDEN
import static org.apache.http.HttpStatus.SC_FORBIDDEN
import static org.apache.http.HttpStatus.SC_FORBIDDEN
import static org.apache.http.HttpStatus.SC_FORBIDDEN
import static org.apache.http.HttpStatus.SC_FORBIDDEN

@ContextConfiguration(classes=[SingletonTestFileConfiguration.class
        , IdentityConfig.class
        , MockServiceProvider.class])
class IdentityConfigComponentTest extends Specification {

    @Autowired
    private IdentityConfig config

    @Autowired
    private IdentityPropertyService identityPropertyService

    @Shared SingletonConfiguration staticIdmConfiguration = SingletonConfiguration.getInstance()
    @Shared SingletonReloadableConfiguration reloadableConfiguration = SingletonReloadableConfiguration.getInstance()

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

    def "user.groups.max.in.domain: Verify hardcoded is set to 20"() {
        when: "set property to value"
        reloadableConfiguration.setProperty("user.groups.max.in.domain", 5)

        then: "Returns that value"
        config.getReloadableConfig().getMaxUsersGroupsPerDomain() == 5

        when: "property doesn't exist in reloadable"
        reloadableConfiguration.clearProperty("user.groups.max.in.domain")

        then: "Returns hardcoded default of 20"
        config.getReloadableConfig().getMaxUsersGroupsPerDomain() == 20
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
        assert format == TokenFormat.UUID

        when: "override for that idp does not exist"
        reloadableConfiguration.setProperty(IdentityConfig.IDENTITY_FEDERATED_TOKEN_FORMAT_DEFAULT_PROP, TokenFormat.UUID.name())
        def formatDefUUID = config.getReloadableConfig().getIdentityFederationRequestTokenFormatForIdp(testIpdLabeledUriNone)

        reloadableConfiguration.setProperty(IdentityConfig.IDENTITY_FEDERATED_TOKEN_FORMAT_DEFAULT_PROP, TokenFormat.AE.name())
        def formatDefAe = config.getReloadableConfig().getIdentityFederationRequestTokenFormatForIdp(testIpdLabeledUriNone)

        then: "the default property is used"
        formatDefUUID == TokenFormat.UUID
        formatDefAe == TokenFormat.AE
    }

    @Unroll
    def "getExplicitUserGroupEnabledDomains properly parses value retrieved from repo: value: #repoValue"() {
        given:
        def identityProp = repoValue != null ? new IdentityProperty().with {
            it.value = repoValue.bytes
            it.valueType = "String"
            it
        } : null

        Mockito.when(identityPropertyService.getIdentityPropertyByName(IdentityConfig.ENABLED_DOMAINS_FOR_USER_GROUPS_PROP)).thenReturn(identityProp)

        expect:
        config.getRepositoryConfig().getExplicitUserGroupEnabledDomains() == expectedList

        where:
        repoValue | expectedList
        "a,b"           | ["a","b"]
        "  a  ,  b "    | ["a","b"]
        "a,,b"          | ["a","b"]
        ",b"            | ["b"]
        ","             | []
        null            | []
    }

    @Unroll
    def "areDelegationAgreementsEnabledForRcn: rcnAllowedProp: '#rcnsAllowed' ; domainRcn: '#domainRcn' ; expectedResponse: '#expected'"() {
        def identityProp = rcnsAllowed != null ? new IdentityProperty().with {
            it.value = rcnsAllowed.bytes
            it.valueType = "String"
            it
        } : null

        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_DELEGATION_AGREEMENTS_FOR_ALL_RCNS_PROP, false)
        Mockito.when(identityPropertyService.getIdentityPropertyByName(IdentityConfig.ENABLE_RCNS_FOR_DELEGATION_AGREEMENTS_PROP)).thenReturn(identityProp)

        when: "RCNs not globally allowed"
        boolean allowed = config.getReloadableConfig().areDelegationAgreementsEnabledForRcn(domainRcn)

        then: "RCN must match without regard to case"
        allowed == expected

        when: "RCNs globally allowed"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_DELEGATION_AGREEMENTS_FOR_ALL_RCNS_PROP, true)
        allowed = config.getReloadableConfig().areDelegationAgreementsEnabledForRcn(domainRcn)

        then: "All RCNs will match regardless of RCNs allowed"
        allowed

        where:
        rcnsAllowed | domainRcn | expected
        "123"       | "123"     | true
        "456,123"   | "123"     | true
        "  456 ,  123  "   | "123"  | true // RCNs in list are trimmed
        "abc,def"   | "DEF"     | true // Show match is case insensitive
        "abc,DEF"   | "def"     | true // Show match is case insensitive
        "123"       | "234"     | false
        "123,456"   | "234"     | false
        "123"       | null      | false
        ""          | null      | false
        ""          | ""        | false // Empty rcnsAllowed means no rcn will match
        ""          | "123"     | false // Empty rcnsAllowed means no rcn will match
        null        | "123"     | false // Empty rcnsAllowed means no rcn will match
        null        | null    | false // Empty rcnsAllowed means no rcn will match
    }

    @SingleTestConfiguration
    static class MockServiceProvider {
        @Bean
        public IdentityPropertyValueConverter identityPropertyValueConverter () {
            return  new IdentityPropertyValueConverter()
        }
        @Bean
        public IdentityPropertyService identityPropertyService () {
            return  Mockito.mock(IdentityPropertyService.class)
        }
    }
}
