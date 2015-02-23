package com.rackspace.idm.domain.config

import com.rackspace.idm.domain.security.TokenFormat
import org.apache.commons.configuration.ConfigurationException
import org.apache.commons.configuration.PropertiesConfiguration
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy
import org.apache.commons.io.IOUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ContextConfiguration
import spock.lang.Shared
import spock.lang.Specification
import testHelpers.SingletonConfiguration
import testHelpers.SingletonReloadableConfiguration

@ContextConfiguration(locations = "classpath:app-config.xml")
class IdentityConfigIntegrationTest  extends Specification {
    @Autowired
    private IdentityConfig config;

    @Shared SingletonConfiguration staticIdmConfiguration = SingletonConfiguration.getInstance();

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
        def format = config.getIdentityFederatedUserTokenFormatForIdp(testIpdLabeledUriAe)

        then: "get AE token format back"
        format == TokenFormat.AE

        when: "retrieving the override property that is set to use UUID tokens"
        format = config.getIdentityFederatedUserTokenFormatForIdp(testIpdLabeledUriUUID)

        then: "get UUID token format back"
        format == TokenFormat.UUID

        when: "override for that idp does not exist"
        staticIdmConfiguration.setProperty(IdentityConfig.IDENTITY_FEDERATED_TOKEN_FORMAT_DEFAULT_PROP, TokenFormat.UUID.name())
        def formatDefUUID = config.getIdentityFederatedUserTokenFormatForIdp(testIpdLabeledUriNone)

        staticIdmConfiguration.setProperty(IdentityConfig.IDENTITY_FEDERATED_TOKEN_FORMAT_DEFAULT_PROP, TokenFormat.AE.name())
        def formatDefAe = config.getIdentityFederatedUserTokenFormatForIdp(testIpdLabeledUriNone)

        then: "the default property is used"
        formatDefUUID == TokenFormat.UUID
        formatDefAe == TokenFormat.AE
    }
}
