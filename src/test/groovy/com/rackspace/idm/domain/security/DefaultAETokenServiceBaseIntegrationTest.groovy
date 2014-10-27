package com.rackspace.idm.domain.security

import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.domain.entity.AuthenticatedByMethodEnum
import com.rackspace.idm.domain.entity.ImpersonatedScopeAccess
import com.rackspace.idm.domain.entity.ScopeAccess
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.entity.UserScopeAccess
import com.rackspace.idm.domain.security.encrypters.KeyCzarAuthenticatedMessageProvider
import com.rackspace.idm.domain.security.encrypters.KeyCzarCrypterLocator
import com.rackspace.idm.domain.security.packers.MessagePackTokenDataPacker
import com.rackspace.idm.domain.service.IdentityUserService
import com.rackspace.idm.domain.service.UserService
import org.apache.commons.collections.CollectionUtils
import org.apache.commons.configuration.Configuration
import org.apache.commons.configuration.PropertiesConfiguration
import org.joda.time.DateTime
import spock.lang.Shared
import spock.lang.Specification
import testHelpers.EntityFactory

abstract class DefaultAETokenServiceBaseIntegrationTest extends Specification {
    @Shared DefaultAETokenService aeTokenService;
    @Shared EntityFactory entityFactory = new EntityFactory()

    @Shared Configuration config
    @Shared IdentityUserService identityUserService
    @Shared MessagePackTokenDataPacker dataPacker
    @Shared KeyCzarAuthenticatedMessageProvider amProvider
    @Shared KeyCzarCrypterLocator crypterLocator
    @Shared UserService userService

    def setupSpec() {
        crypterLocator = new ClasspathKeyCzarCrypterLocator();
        crypterLocator.setKeysClassPathLocation("/com/rackspace/idm/api/resource/cloud/v20/keys")

        config = new PropertiesConfiguration()
        config.setProperty(MessagePackTokenDataPacker.CLOUD_AUTH_CLIENT_ID_PROP_NAME, "aaa7cb17b52d4e1ca3cb5c7c5996cc3b")

        identityUserService = Mock()
        userService = Mock()
        aeTokenService = new DefaultAETokenService()

        dataPacker = new MessagePackTokenDataPacker()
        dataPacker.config = config
        dataPacker.identityUserService = identityUserService
        dataPacker.provisionedUserService = userService
        dataPacker.aeTokenService = aeTokenService

        amProvider = new KeyCzarAuthenticatedMessageProvider()
        amProvider.keyCzarCrypterLocator = crypterLocator

        aeTokenService.tokenDataPacker = dataPacker
        aeTokenService.authenticatedMessageProvider = amProvider
    }


    def void validateUserScopeAccessesEqual(UserScopeAccess original, UserScopeAccess toValidate) {
        assert toValidate.userRsId == original.userRsId
        assert toValidate.clientId == original.clientId
        assert toValidate.accessTokenString == original.accessTokenString
        assert toValidate.accessTokenExp == original.accessTokenExp
        assert CollectionUtils.isEqualCollection(toValidate.authenticatedBy, original.authenticatedBy)
        assert toValidate.getUniqueId() == original.getUniqueId()
    }

    def void validateImpersonationScopeAccessesEqual(ImpersonatedScopeAccess original, ImpersonatedScopeAccess toValidate) {
        assert toValidate.scope == original.scope
        assert toValidate.clientId == original.clientId
        assert toValidate.accessTokenString == original.accessTokenString
        assert toValidate.accessTokenExp == original.accessTokenExp
        assert CollectionUtils.isEqualCollection(toValidate.authenticatedBy, original.authenticatedBy)
        assert toValidate.getUniqueId() == original.getUniqueId()

        //validate that the underlying impersonated user token is valid
        ScopeAccess impersonatedUserToken = aeTokenService.unmarshallToken(toValidate.impersonatingToken)
        assert impersonatedUserToken instanceof UserScopeAccess
        UserScopeAccess usaImpersonatedUserToken = (UserScopeAccess) impersonatedUserToken
        assert usaImpersonatedUserToken.userRsId == original.impersonatingRsId
        assert usaImpersonatedUserToken.accessTokenExp == original.accessTokenExp
        assert usaImpersonatedUserToken.authenticatedBy.size() == 1
        assert usaImpersonatedUserToken.authenticatedBy.get(0) == AuthenticatedByMethodEnum.IMPERSONATION.value
    }

    def void validateWebSafeToken(String webSafeToken) {
        assert webSafeToken != null
        assert webSafeToken.length() > 32
        assert webSafeToken.length() <= 250
    }

    def createProvisionedUserToken(User user, String tokenString =  UUID.randomUUID().toString(), Date expiration = new DateTime().plusDays(1).toDate(), List<String> authBy = [GlobalConstants.AUTHENTICATED_BY_PASSWORD]) {
        new UserScopeAccess().with {
            it.accessTokenString = tokenString
            it.accessTokenExp = expiration
            it.userRsId = user.id
            it.userRCN = "userRCN"
            it.clientId = config.getString(MessagePackTokenDataPacker.CLOUD_AUTH_CLIENT_ID_PROP_NAME)
            it.clientRCN = "clientRCN"
            it.getAuthenticatedBy().addAll(authBy)
            return it
        }
    }

}
