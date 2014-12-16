package com.rackspace.idm.domain.config

import com.rackspace.idm.domain.security.TokenFormat
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import spock.lang.Shared
import spock.lang.Specification
import testHelpers.SingletonConfiguration

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
