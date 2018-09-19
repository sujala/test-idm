package com.rackspace.idm.domain.config

import com.rackspace.idm.Constants
import com.rackspace.idm.api.converter.cloudv20.IdentityPropertyValueConverter
import com.rackspace.idm.domain.entity.IdentityProperty
import com.rackspace.idm.domain.entity.ImmutableIdentityProperty
import com.rackspace.idm.domain.service.IdentityPropertyService
import com.rackspace.test.SingleTestConfiguration
import org.apache.commons.collections4.CollectionUtils
import org.apache.commons.configuration.PropertiesConfiguration
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy
import org.apache.commons.lang.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.test.context.ContextConfiguration
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll
import spock.mock.DetachedMockFactory
import testHelpers.SingletonConfiguration
import testHelpers.SingletonReloadableConfiguration
import testHelpers.SingletonTestFileConfiguration

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

    def "role.assignments.max.tenant.assignments.per.request: Verify hardcoded is set to 10"() {
        when: "set property to value"
        reloadableConfiguration.setProperty("role.assignments.max.tenant.assignments.per.request", 5)

        then: "Returns that value"
        config.getReloadableConfig().getRoleAssignmentsMaxTenantAssignmentsPerRequest() == 5

        when: "property doesn't exist in reloadable"
        reloadableConfiguration.clearProperty("role.assignments.max.tenant.assignments.per.request")

        then: "Returns hardcoded default of 10"
        config.getReloadableConfig().getRoleAssignmentsMaxTenantAssignmentsPerRequest() == 10
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

    @Unroll
    def "getExplicitUserGroupEnabledDomains properly parses value retrieved from repo: value: #repoValue"() {
        given:

        def identityProp = repoValue != null ? new ImmutableIdentityProperty(new IdentityProperty().with {
            it.value = repoValue.bytes
            it.valueType = "String"
            it
        }) : null

        identityPropertyService.getImmutableIdentityPropertyByName(IdentityConfig.ENABLED_DOMAINS_FOR_USER_GROUPS_PROP) >> identityProp

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
        def identityProp = rcnsAllowed != null ? new ImmutableIdentityProperty(new IdentityProperty().with {
            it.value = rcnsAllowed.bytes
            it.valueType = "String"
            it
        }) : null

        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_DELEGATION_AGREEMENTS_FOR_ALL_RCNS_PROP, false)
        identityPropertyService.getImmutableIdentityPropertyByName(IdentityConfig.ENABLE_RCNS_FOR_DELEGATION_AGREEMENTS_PROP) >> identityProp

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
        null        | null      | false // Empty rcnsAllowed means no rcn will match
        ","         | "123"     | false // Empty rcnsAllowed means no rcn will match
    }

    def "getTenantVisibilityRoleMap: Returns maps appropriately"() {
        def t1 = "t1"
        def t2 = "t2"
        def t3 = "t3"
        def t4 = "t4"

        def role1 = "role1"
        def role2 = "role2:yippy"
        def role3 = "role3:skippy"
        reloadableConfiguration.setProperty(String.format(IdentityConfig.TENANT_ROLE_WHITELIST_VISIBILITY_FILTER_PROP_REG, t1), StringUtils.join([role1], ","))
        reloadableConfiguration.setProperty(String.format(IdentityConfig.TENANT_ROLE_WHITELIST_VISIBILITY_FILTER_PROP_REG, t2), StringUtils.join([role1, role2, role3], ","))
        reloadableConfiguration.setProperty(String.format(IdentityConfig.TENANT_ROLE_WHITELIST_VISIBILITY_FILTER_PROP_REG, t3), "")

        when: "get role map"
        Map<String, Set<String>> map = config.getReloadableConfig().getTenantTypeRoleWhitelistFilterMap()

        then:
        map != null

        and: "whitelists for each tenant type are set correctly"
        map.get(t1) != null
        CollectionUtils.isEqualCollection(map.get(t1), [role1])
        CollectionUtils.isEqualCollection(map.get(t2), [role1, role2, role3])
        CollectionUtils.isEmpty(map.get(t3))
    }

    @Unroll
    def "getTenantVisibilityRoleMap: Normalizes string #testSet to #expected"() {
        def t1 = "t1"

        reloadableConfiguration.setProperty(String.format(IdentityConfig.TENANT_ROLE_WHITELIST_VISIBILITY_FILTER_PROP_REG, t1), testSet)

        when: "get role map"
        Map<String, Set<String>> map = config.getReloadableConfig().getTenantTypeRoleWhitelistFilterMap()

        then:
        map != null

        and: "whitelists for each tenant type are set correctly"
        map.get(t1) != null
        CollectionUtils.isEqualCollection(map.get(t1), expected)

        where:
        testSet             | expected
        "role1"             | ["role1"] // Standard
        "role1,role1,role2" | ["role1", "role2"] // Removes duplicate elements
        ",role1, "          | [",role1, "] // if any split item doesn't contain value, doesn't split
        ",role1, role2"     | [",role1, role2"]  // if any split item doesn't contain value, doesn't split
        "role1, role2"      | ["role1", "role2"]  // if all split values contain non-empty string, is split to array
        ",role1"            | [",role1"] // Removes empty elements
    }

    def "getTenantVisibilityRoleMap: Load canned tenant type"() {
        Map<String, Set<String>> map = config.getReloadableConfig().getTenantTypeRoleWhitelistFilterMap()

        List<String> expectedRoles = Arrays.asList(StringUtils.split("identity:service-admin,identity:admin,identity:user-admin,identity:user-manage,admin,observer,creator,ticketing:admin,ticketing:observer,billing:admin,billing:observer", ","))

        when: "get whitelist map for canned type"
        Set<String> whitelist = map.get(Constants.TENANT_TYPE_WHITELIST_TEST)

        then:
        whitelist != null

        and: "Contains all expected roles"
        CollectionUtils.isEqualCollection(whitelist, expectedRoles)
    }

    def "retrievingRepository properties from cache is feature flagged"() {

        when:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_CACHE_REPOSITORY_PROPERTIES_PROP, false)
        config.getRepositoryConfig().getExplicitUserGroupEnabledDomains()

        then:
        0 * identityPropertyService.getImmutableIdentityPropertyByName(_)
        1 * identityPropertyService.getIdentityPropertyByName(_)

        when:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_CACHE_REPOSITORY_PROPERTIES_PROP, true)
        config.getRepositoryConfig().getExplicitUserGroupEnabledDomains()

        then:
        1 * identityPropertyService.getImmutableIdentityPropertyByName(_)
        0 * identityPropertyService.getIdentityPropertyByName(_)
    }

    @SingleTestConfiguration
    static class MockServiceProvider {
        private DetachedMockFactory factory = new DetachedMockFactory()

        @Bean
        public IdentityPropertyValueConverter identityPropertyValueConverter () {
            return  new IdentityPropertyValueConverter()
        }

        @Bean
        public IdentityPropertyService identityPropertyService () {
            return  factory.Mock(IdentityPropertyService)
        }
    }
}
