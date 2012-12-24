package testHelpers

import com.rackspace.idm.domain.entity.ClientScopeAccess
import com.rackspace.idm.domain.entity.UserScopeAccess

/**
 * Created with IntelliJ IDEA.
 * User: jacob
 * Date: 12/24/12
 * Time: 9:23 AM
 * To change this template use File | Settings | File Templates.
 */
class ScopeAccessFactory {
    def createClientScopeAccess() {
        return createClientScopeAccess("clientId", "tokenString", new Date())
    }

    def createClientScopeAccess(String clientId, String tokenString, Date tokenExp) {
        new ClientScopeAccess().with {
            it.clientId = clientId ? clientId : "clientId"
            it.accessTokenString = tokenString ? tokenString : "tokenString"
            it.accessTokenExp = tokenExp ? tokenExp : new Date()
            it.getLDAPEntry().DN = "accessToken=$it.accessTokenString,cn=TOKENS,clientId=$it.clientId"
            return it
        }
    }

    def createUserScopeAccess() {
        return createUserScopeAccess("tokenString", new Date(), "userRsId", "clientId")
    }

    def createUserScopeAccess(String tokenString, Date tokenExp, String userRsId, String clientId) {
        new UserScopeAccess().with {
            it.accessTokenString = tokenString ? tokenString : "tokenString"
            it.accessTokenExp = tokenExp ? tokenExp : new Date()
            it.userRsId = userRsId ? userRsId : "userRsId"
            it.clientId = clientId ? clientId : "clientId"
            it.getLDAPEntry().DN = "accessToken=$it.accessTokenString,cn=TOKENS,rsId=$it.userRsId,ou=users"
            return it
        }
    }
}
