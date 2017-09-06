package com.rackspace.idm.domain.security

import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.domain.config.IdentityConfig

import com.rackspace.idm.domain.entity.AuthenticatedByMethodEnum
import com.rackspace.idm.domain.entity.ImpersonatedScopeAccess
import com.rackspace.idm.domain.entity.Racker
import com.rackspace.idm.domain.entity.RackerScopeAccess
import com.rackspace.idm.domain.entity.ScopeAccess
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.entity.UserScopeAccess
import com.rackspace.idm.domain.security.encrypters.KeyCzarAuthenticatedMessageProvider
import com.rackspace.idm.domain.security.encrypters.KeyCzarCrypterLocator
import com.rackspace.idm.domain.security.tokenproviders.TokenProvider
import com.rackspace.idm.domain.security.tokenproviders.globalauth.GlobalAuthTokenProvider
import com.rackspace.idm.domain.security.tokenproviders.globalauth.MessagePackTokenDataPacker
import com.rackspace.idm.domain.security.tokenproviders.keystone_keyczar.KeystoneAEMessagePackTokenDataPacker
import com.rackspace.idm.domain.security.tokenproviders.keystone_keyczar.KeystoneAETokenProvider
import com.rackspace.idm.domain.service.AETokenRevocationService
import com.rackspace.idm.domain.service.IdentityUserService
import com.rackspace.idm.domain.service.UserService
import org.apache.commons.collections.CollectionUtils
import org.apache.commons.configuration.Configuration
import org.apache.commons.configuration.PropertiesConfiguration
import org.joda.time.DateTime
import org.springframework.core.env.Environment
import spock.lang.Shared
import spock.lang.Specification
import testHelpers.EntityFactory
import testHelpers.FakeTicker

abstract class DefaultAETokenServiceBaseIntegrationTest extends Specification {
    @Shared EntityFactory entityFactory = new EntityFactory()
    @Shared IdentityUserService identityUserService
    @Shared UserService userService
    @Shared IdentityConfig identityConfig
    @Shared Configuration staticConfig
    @Shared Configuration reloadableConfig

    //keyczar encryption stuff
    @Shared KeyCzarCrypterLocator crypterLocator
    @Shared KeyCzarAuthenticatedMessageProvider amProvider

    //main controller service for AE tokens
    @Shared DefaultAETokenService aeTokenService;

    //global auth ae provider
    @Shared GlobalAuthTokenProvider globalAuthTokenProvider
    @Shared MessagePackTokenDataPacker globalAuthTokenDataPacker

    //keystone ae provider
    @Shared KeystoneAETokenProvider keystoneAETokenProvider
    @Shared KeystoneAEMessagePackTokenDataPacker keystoneAEMessagePackTokenDataPacker

    AETokenRevocationService aeTokenRevocationService


    def setupSpec() {
        crypterLocator = new ClasspathKeyCzarCrypterLocator();
        crypterLocator.setKeysClassPathLocation("/com/rackspace/idm/api/resource/cloud/v20/keys")

        staticConfig = new PropertiesConfiguration()
        staticConfig.setProperty(MessagePackTokenDataPacker.CLOUD_AUTH_CLIENT_ID_PROP_NAME, "aaa7cb17b52d4e1ca3cb5c7c5996cc3b")
        staticConfig.setProperty(IdentityConfig.FEATURE_AE_TOKENS_ENCRYPT, true)
        staticConfig.setProperty(IdentityConfig.FEATURE_AE_TOKENS_DECRYPT, true)
        staticConfig.setProperty(IdentityConfig.CACHED_AE_TOKEN_TTL_SECONDS_PROP, 60)

        reloadableConfig = new PropertiesConfiguration()

        identityConfig = new IdentityConfig()
        identityConfig.staticConfiguration = staticConfig
        identityConfig.reloadableConfiguration = reloadableConfig

        identityUserService = Mock()
        userService = Mock()
        aeTokenService = new DefaultAETokenService()

        amProvider = new KeyCzarAuthenticatedMessageProvider()
        amProvider.keyCzarCrypterLocator = crypterLocator

        //init global auth ae provider
        globalAuthTokenDataPacker = new MessagePackTokenDataPacker()
        globalAuthTokenDataPacker.identityConfig = identityConfig
        globalAuthTokenDataPacker.identityUserService = identityUserService
        globalAuthTokenDataPacker.provisionedUserService = userService
        globalAuthTokenDataPacker.aeTokenService = aeTokenService

        globalAuthTokenProvider = new GlobalAuthTokenProvider()
        globalAuthTokenProvider.authenticatedMessageProvider = amProvider
        globalAuthTokenProvider.tokenDataPacker = globalAuthTokenDataPacker
        globalAuthTokenProvider.identityConfig = identityConfig

        //init keystone ae provider
        keystoneAEMessagePackTokenDataPacker = new KeystoneAEMessagePackTokenDataPacker()
        keystoneAEMessagePackTokenDataPacker.identityConfig = identityConfig

        keystoneAETokenProvider = new KeystoneAETokenProvider()
        keystoneAETokenProvider.tokenDataPacker = keystoneAEMessagePackTokenDataPacker
        keystoneAETokenProvider.authenticatedMessageProvider = amProvider
        keystoneAETokenProvider.identityConfig = identityConfig

        //AE Cache
        AETokenCache aeTokenCache = new AETokenCache()
        aeTokenCache.identityConfig = identityConfig
        aeTokenCache.ticker = new FakeTicker()
        aeTokenCache.init()

        List<TokenProvider> tokenProviders = [globalAuthTokenProvider, keystoneAETokenProvider]
        aeTokenService.tokenProviders = tokenProviders
        aeTokenService.identityConfig = identityConfig
        aeTokenService.aeTokenCache = aeTokenCache
    }

    def void setup() {
        //for some reason need to initialize this here rather than setupSpec (event when defined as a shared variable).
        //when done in setupSpec couldn't set expectations on the mock from within feature methods. It didn't throw
        // error, just acted like the expectations were never set. I verified same object was being used so not sure why
        // it didn't work... recreating the revocationService for each feature method does allow me to set the expectations
        // from within the feature method though so switched to that.
        aeTokenRevocationService = Mock()
        aeTokenService.aeTokenCache.aeTokenRevocationService = aeTokenRevocationService
        aeTokenService.aeTokenRevocationService = aeTokenRevocationService
    }

    def void validateScopeAccessesEqual(ScopeAccess original, ScopeAccess toValidate) {
        assert original.getClass() == toValidate.getClass()
        if (original instanceof RackerScopeAccess) {
            validateRackerScopeAccessesEqual((RackerScopeAccess) original, (RackerScopeAccess) toValidate)
        } else if (original instanceof UserScopeAccess) {
            validateUserScopeAccessesEqual((UserScopeAccess) original, (UserScopeAccess) toValidate)
        } else if (original instanceof ImpersonatedScopeAccess) {
            validateImpersonationScopeAccessesEqual((ImpersonatedScopeAccess) original, (ImpersonatedScopeAccess) toValidate)
        } else {
            assert 1 != 1
        }
    }

    def void validateRackerScopeAccessesEqual(RackerScopeAccess original, RackerScopeAccess toValidate) {
        assert toValidate.rackerId == original.rackerId
        assert toValidate.clientId == original.clientId
        assert toValidate.accessTokenString == original.accessTokenString
        assert toValidate.accessTokenExp == original.accessTokenExp
        assert CollectionUtils.isEqualCollection(toValidate.authenticatedBy, original.authenticatedBy)
        assert toValidate.getUniqueId() == original.getUniqueId()
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
        assert usaImpersonatedUserToken.userRsId == original.rsImpersonatingRsId
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
            it.clientId = staticConfig.getString(MessagePackTokenDataPacker.CLOUD_AUTH_CLIENT_ID_PROP_NAME)
            it.clientRCN = "clientRCN"
            it.getAuthenticatedBy().addAll(authBy)
            return it
        }
    }

    def createRackerToken(Racker user, String tokenString =  UUID.randomUUID().toString(), Date expiration = new DateTime().plusDays(1).toDate(), List<String> authBy = [GlobalConstants.AUTHENTICATED_BY_PASSWORD]) {
        new RackerScopeAccess().with {
            it.accessTokenString = tokenString
            it.accessTokenExp = expiration
            it.rackerId = user.id
            it.clientId = staticConfig.getString(MessagePackTokenDataPacker.CLOUD_AUTH_CLIENT_ID_PROP_NAME)
            it.clientRCN = "clientRCN"
            it.getAuthenticatedBy().addAll(authBy)
            return it
        }
    }

}
