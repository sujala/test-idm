package testHelpers

import org.springframework.stereotype.Component

@Component
class FoundationFactory {

    def createAuthCredentials(String clientId, String clientSecret) {
        //This should not be done, but foundation api is deprecated and will soon be removed
        return '{"authCredentials":{"grant_type": "CLIENT_CREDENTIALS","client_id": "'+ clientId + '","client_secret": "'+ clientSecret+'"}}'
    }

    def createUserAuthCredentials(String clientId, String clientSecret, String username, String password) {
        return '{"authCredentials":{"grant_type": "CLIENT_CREDENTIALS","client_id": "'+ clientId + '","client_secret": "'+ clientSecret+'", "username": "'+username+'", "password":"'+password+'"}}'
    }

    def createRackerAuthCredentials(String clientId, String clientSecret, String racker, String password) {
        return '{"rackerCredentials":{"grant_type": "CLIENT_CREDENTIALS","client_id": "'+ clientId + '","client_secret": "'+ clientSecret+'", "username": "'+racker+'", "password":"'+password+'"}}'
    }

    def createUser() {
        def username = getRandomUUID('foundationTestUser').substring(0,31).trim()
        return '{"user": {"customerId": "RCN-999-999-999", "displayName": "testUser", "email": "test@example.org", "firstName": "test", "lastName": "test", "passwordCredentials": { "currentPassword": { "password": "Password1"} },"personId": "RPN-999-999-999", "prefLanguage": "US_en", "region": "SAT", "secret": { "secretAnswer": "Yes", "secretQuestion": "Is this a question?" }, "timeZone": "America/Chicago", "username": "'+ username +'" }}'
    }

    def getRandomUUID(prefix='') {
        String.format("%s%s", prefix, UUID.randomUUID().toString().replace('-', ''))
    }

    def createTenant() {
        def name = getRandomUUID('foundationTenant')
        return '{"tenant" : { "id": "'+ name.concat(":001") +'","name": "' + name +'", "display-name" : "'+ name +'", "enabled" : true}}'
    }

}
