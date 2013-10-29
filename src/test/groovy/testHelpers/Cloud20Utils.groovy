package testHelpers

import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.User
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import spock.lang.Shared

class Cloud20Utils {

    def Cloud20Methods cloud20

    static def SERVICE_ADMIN_USERNAME = "authQE"
    static def SERVICE_ADMIN_PASSWORD = "Auth1234"
    static def v2Factory = new V2Factory()

    def Cloud20Utils(Cloud20Methods cloud20) {
        this.cloud20 = cloud20
    }

    def createUser(kwargs = [displayName:"display", email:"email@email.com", enabled:true, defaultRegion:null, domainId:null], token, username, password) {
        def user = cloud20.createUser(token, v2Factory.createUserForCreate(username, kwargs.displayName, kwargs.email, kwargs.enabled, kwargs.defaultRegion, kwargs.domainId, password)).getEntity(User)
        assert(user.id != null)
        user
    }

    def getToken(username, password) {
        def response = cloud20.authenticatePassword(username, password)
        assert(response.status == 200)
        response.getEntity(AuthenticateResponse).value.token.id
    }

    def getServiceAdminToken() {
        getToken(SERVICE_ADMIN_USERNAME, SERVICE_ADMIN_PASSWORD)
    }

    def static createRandomString() {
        UUID.randomUUID().toString().replace('-',"")
    }

}
