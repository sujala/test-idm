package com.rackspace.idm.domain.security.keystone

import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.entity.*
import com.rackspace.idm.domain.security.DefaultAETokenService
import com.rackspace.idm.domain.security.DefaultAETokenServiceBaseIntegrationTest
import com.rackspace.idm.domain.security.UnmarshallTokenException
import org.apache.commons.lang.StringUtils
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat

class V3ProvisionedUserTokenIntegrationTest extends DefaultAETokenServiceBaseIntegrationTest {
    /**
     * curl -X POST http://localhost:35357/v3/auth/tokens -H "content-type:application/json" -d '{
     "auth": {"identity": {"methods": ["password"],"password": {"user": {"id": "10046995","password": "Password1"}}}}}'
     -v | python -mjson.tool
     *
     * {
     "token": {
     "audit_ids": [
     "yoQM1MneR3WfrDuZU3QcgQ"
     ],
     "expires_at": "2015-09-09T03:52:43.976138Z",
     "extras": {},
     "issued_at": "2015-09-09T03:37:43.976166Z",
     "methods": [
     "password"
     ],
     "user": {
     "domain": {
     "id": "135792468",
     "name": "135792468"
     },
     "id": "10046995",
     "name": "testDefaultUser_doNotDelete"
     }
     }
     }
     * @return
     */
    def provisionedUserUnscopedToken = "AQD1Pcpjp2rYEWXPIhGH7Y_7XOIYbi81UY6E9vgzZqJDh5LGJeT721EPQK-8o_FGuB_xCzthD-MXCKu1vsqD4jYtdLQ-6ERe8J-caP9LcdmJ9pA0gsUl0lB8ddIZemUj8E0K3QZRWmjslA"

    /**
     *
     * Response from v3 auth:
     *
     * curl -X POST http://localhost:35357/v3/auth/tokens -H "content-type:application/json" -d '{
     "auth": {"identity": {"methods": ["password"],"password": {"user": {"id": "4e997592aad24e2183e51bd013f223c5","password": "Auth1234"}}}}}' -v | python -mjson.tool
     *
     * {
     "token": {
     "audit_ids": [
     "Prwky86ETFajvU9794nK1A"
     ],
     "catalog": [
     ...
     ],
     "expires_at": "2015-09-09T03:31:22.454996Z",
     "extras": {},
     "issued_at": "2015-09-09T03:16:22.455025Z",
     "methods": [
     "password"
     ],
     "project": {
     "domain": {
     "id": "78543988",
     "name": "78543988"
     },
     "id": "78543988",
     "name": "78543988"
     },
     "roles": [
     {
     "id": "6",
     "name": "compute:default"
     }
     ],
     "user": {
     "domain": {
     "id": "78543988",
     "name": "78543988"
     },
     "id": "4e997592aad24e2183e51bd013f223c5",
     "name": "keystone_user_admin"
     }
     }
     }
     *
     *
     * @return
     */
    def provisionedUserProjectScopedToken = "AQD1PcpjaOfGpQ85NXOLkpGA5nEwpYWllvKPxY1Z-GPEBTWqGPHMyI49Ybjn_ooh58fcjXwmU0qruQB3o5-pyJuYAqdl-bVsYNrLgsV20ubiJewWTNeNZkTteBK5d2TS7B_f0Y2WvIQA6JjUtfclKEo25IVWD7LlVCY"

    def setup() {
        reloadableConfig.setProperty(IdentityConfig.FEATURE_SUPPORT_V3_PROVISIONED_USER_TOKENS_PROP, true)
    }

    def "unmarshall fully populated v3 unscoped user token w/ non-uuid user id"() {
        String expireDateStr = "2015-09-09T03:52:43.976Z"; //note only millisecond precision
        DateTime expectedExpireDate = ISODateTimeFormat.dateTime().parseDateTime(expireDateStr);

        String expectedIssuedDateStr = "2015-09-09T03:37:43.976Z" //note only millisecond precision
        DateTime expectedIssuedDate = ISODateTimeFormat.dateTime().parseDateTime(expectedIssuedDateStr);

        String expectedIssuedToUserId = "10046995"

        when:
        UserScopeAccess unmarshalledScopeAccess = (UserScopeAccess) aeTokenService.unmarshallToken(provisionedUserUnscopedToken)

        then:
        unmarshalledScopeAccess != null
        unmarshalledScopeAccess.accessTokenString == provisionedUserUnscopedToken
        unmarshalledScopeAccess.accessTokenExp.equals(expectedExpireDate.toDate())
        unmarshalledScopeAccess.createTimestamp.equals(expectedIssuedDate.toDate())
        unmarshalledScopeAccess.clientId.equals(identityConfig.getStaticConfig().getCloudAuthClientId())
        unmarshalledScopeAccess.authenticatedBy.size() == 1
        unmarshalledScopeAccess.authenticatedBy.get(0).equals(AuthenticatedByMethodEnum.PASSWORD.getValue())
        unmarshalledScopeAccess.issuedToUserId.equals(expectedIssuedToUserId)

        //unused fields
        StringUtils.isEmpty(unmarshalledScopeAccess.scope)
        StringUtils.isEmpty(unmarshalledScopeAccess.clientRCN)
        unmarshalledScopeAccess.refreshTokenExp == null
        unmarshalledScopeAccess.userPasswordExpirationDate == null
        StringUtils.isEmpty(unmarshalledScopeAccess.refreshTokenString)
        StringUtils.isEmpty(unmarshalledScopeAccess.clientRCN)
    }

   def "unmarshall v3 project scoped user token"() {
        String expireDateStr = "2015-09-09T03:31:22.454Z"; //note only millisecond precision
        DateTime expectedExpireDate = ISODateTimeFormat.dateTime().parseDateTime(expireDateStr);

        String expectedIssuedDateStr = "2015-09-09T03:16:22.455Z" //note only millisecond precision
        DateTime expectedIssuedDate = ISODateTimeFormat.dateTime().parseDateTime(expectedIssuedDateStr);

        String expectedIssuedToUserId = "4e997592aad24e2183e51bd013f223c5"

        when:
        UserScopeAccess unmarshalledScopeAccess = (UserScopeAccess) aeTokenService.unmarshallToken(provisionedUserProjectScopedToken)

        then:
        unmarshalledScopeAccess != null
        unmarshalledScopeAccess.accessTokenString == provisionedUserProjectScopedToken
        unmarshalledScopeAccess.accessTokenExp.equals(expectedExpireDate.toDate())
        unmarshalledScopeAccess.createTimestamp.equals(expectedIssuedDate.toDate())
        unmarshalledScopeAccess.clientId.equals(identityConfig.getStaticConfig().getCloudAuthClientId())
        unmarshalledScopeAccess.authenticatedBy.size() == 1
        unmarshalledScopeAccess.authenticatedBy.get(0).equals(AuthenticatedByMethodEnum.PASSWORD.getValue())
        unmarshalledScopeAccess.issuedToUserId.equals(expectedIssuedToUserId)

        //unused fields
        StringUtils.isEmpty(unmarshalledScopeAccess.scope)
        StringUtils.isEmpty(unmarshalledScopeAccess.clientRCN)
        unmarshalledScopeAccess.refreshTokenExp == null
        unmarshalledScopeAccess.userPasswordExpirationDate == null
        StringUtils.isEmpty(unmarshalledScopeAccess.refreshTokenString)
        StringUtils.isEmpty(unmarshalledScopeAccess.clientRCN)
    }

    def "can only unmarshall v3 provisioned user token when feature enabled."() {
        when: "feature enabled"
        reloadableConfig.setProperty(IdentityConfig.FEATURE_SUPPORT_V3_PROVISIONED_USER_TOKENS_PROP, true)

        then: "can unmarshall provisioned user unscoped token"
        aeTokenService.unmarshallToken(provisionedUserUnscopedToken) != null

        and: "can unmarshall provisioned user project scoped token"
        aeTokenService.unmarshallToken(provisionedUserProjectScopedToken) != null

        when: "feature disabled and unmarshall unscoped"
        reloadableConfig.setProperty(IdentityConfig.FEATURE_SUPPORT_V3_PROVISIONED_USER_TOKENS_PROP, false)
        aeTokenService.unmarshallToken(provisionedUserUnscopedToken)

        then: "get UnmarshallTokenException"
        def uex = thrown(UnmarshallTokenException)
        uex.errorCode == DefaultAETokenService.ERROR_CODE_UNMARSHALL_INVALID_ENCRYPTION_SCHEME

        when: "feature disabled and unmarshall project scoped"
        reloadableConfig.setProperty(IdentityConfig.FEATURE_SUPPORT_V3_PROVISIONED_USER_TOKENS_PROP, false)
        aeTokenService.unmarshallToken(provisionedUserProjectScopedToken)

        then: "get UnmarshallTokenException"
        def pex = thrown(UnmarshallTokenException)
        pex.errorCode == DefaultAETokenService.ERROR_CODE_UNMARSHALL_INVALID_ENCRYPTION_SCHEME
    }
}
