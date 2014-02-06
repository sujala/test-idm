package testHelpers

import com.rackspace.api.idm.v1.AuthCredentials
import com.rackspace.api.idm.v1.User
import org.springframework.stereotype.Component

@Component
class FoundationFactory {

    def createAuthCredentials(String clientId, String clientSecret) {
        //This should not be done, but foundation api is deprecated and will soon be removed
        return '{"authCredentials":{"grant_type": "CLIENT_CREDENTIALS","client_id": "'+ clientId + '","client_secret": "'+ clientSecret+'"}}'
    }

    def createUser() {
        return '{"user":{"customerId" : "RCN-999-999-999", "displayName" : "testUser", "email" : "test@example.com", "firstName" : "test", "lastName" : "test", "personId" : "RPN-999-999-999"}}'
    }
}
