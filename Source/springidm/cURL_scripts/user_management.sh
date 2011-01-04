#
# A set of CURL calls to demonstrate the user management portion of the IDM API.
# mkovacs = a client service developer, not an end user. Note that the client
# service has already obtained a client_id and client_secret from the IDM service.
#
# Note that the API calls would be done over HTTPS in production environment.
#

#
# A work area to paste token from service call results.
# 
# Client Token: 204fa51e9169354c0ed29929152d6898f515c58b
# Expires at: 11:20
# 

# Acquire a token with clientID/clientSecret.
curl http://localhost:8080/idm/token \
	--request POST \
	--header "Content-Type:application/xml" \
	--header "Accept:application/xml" \
	--data "<authCredentials><grantType>none</grantType><clientId>ABCDEF</clientId><clientSecret>password</clientSecret></authCredentials>" \
	--dump-header /tmp/curl_out_header.txt
	
curl http://localhost:8080/idm/token \
	--request POST \
	--header "Content-Type:application/json" \
	--header "Accept:application/json" \
	--data "{authCredentials:{grantType:none, clientId:ABCDEF, clientSecret:password}}" \
	--dump-header /tmp/curl_out_header.txt

# Get a token for the admin user
curl http://localhost:8080/idm/token \
    --request POST \
	--header "Content-Type:application/json" \
	--header "Accept:application/json" \
    --data '{"grant_type":"PASSWORD", "client_id":"ABCDEF", "client_secret":"password", "username":"mkovacs", "password":"password"}' \
    --dump-header /tmp/curl_out_header.txt

# Admin user token: 9d10e6946f5dfc896d4c471721108dee5313eba8

# Get a token for the test user
curl http://localhost:8080/idm/token \
    --request POST \
	--header "Content-Type:application/json" \
	--header "Accept:application/json" \
    --data "{authCredentials: {grantType:password, clientId:ABCDEF, clientSecret:password, username:testuser, password:password}}" \
    --dump-header /tmp/curl_out_header.txt

# Test user token: fcd7ce8798ba4b50a3a1753d4b82ff480007e43f

# Add a user as the admin user
curl http://localhost:8080/idm/users \
    --request POST \
	--header "Content-Type:application/json" \
	--header "Accept:application/json" \
    --header "Authorization: OAuth DFW-de7c701434ec63ed27a09d4fe6e789d0bdc96d7b" \
    --data "{newUser: {customerId:RCN-000-000-000, personId:RPN-000-123-4567, username:johneo, password:D0ntL00k@me, firstname:John, lastname:Eo, email:john.eo@rackspace.com, middlename:helloworld, secretQuestion:'What is your favourite colour', secretAnswer:'Yellow. No, blue', preferredLang:en_US, timezone:'America/Chicago'}}" \
    --dump /tmp/curl_out_header.txt


# Authenticate user and acquire a token for him/her.
curl http://localhost:8080/idm/token \
    --request POST \
	--header "Content-Type:application/json" \
	--header "Accept:application/json" \
    --data "{tokenRequest: {grantType:password, clientId:ABCDEF, clientSecret:password, username:johneo, password:D0ntL00k@me}}" \
    --dump-header /tmp/curl_out_header.txt

#
# User Token: 24f85b59a2f2c1755eb8591b3ef2c33ee0f620c9
# Expires at: 11:35
#

# Validate token
curl http://localhost:8080/idm/token/24f85b59a2f2c1755eb8591b3ef2c33ee0f620c9 \
    --request GET \
	--header "Content-Type:application/json" \
	--header "Accept:application/json" \
    --header "Authorization: OAuth 9d10e6946f5dfc896d4c471721108dee5313eba8" \
    --dump-header /tmp/curl_out_header.txt

# Reset Auth Service API key using the user's own key
curl http://localhost:8080/idm/users/johneo/apikey \
    --request POST \
	--header "Content-Type:application/json" \
	--header "Accept:application/json" \
    --header "Authorization: OAuth 24f85b59a2f2c1755eb8591b3ef2c33ee0f620c9" \
    --dump-header /tmp/curl_out_header.txt

# Reset Auth Service API key using the admin's key
curl http://localhost:8080/idm/users/johneo/apikey \
    --request POST \
	--header "Content-Type:application/json" \
	--header "Accept:application/json" \
    --header "Authorization: OAuth 9d10e6946f5dfc896d4c471721108dee5313eba8" \
    --dump-header /tmp/curl_out_header.txt

# Reset Auth Service API key using the testuser's key
curl http://localhost:8080/idm/users/johneo/apikey \
    --request POST \
	--header "Content-Type:application/json" \
	--header "Accept:application/json" \
    --header "Authorization: OAuth 8bd7781aa4f682372a28a3ffd139a60b688113c4" \
    --dump-header /tmp/curl_out_header.txt

# Get user details
curl http://localhost:8080/idm/users/johneo \
    --request GET \
	--header "Content-Type:application/json" \
	--header "Accept:application/json" \
    --header "Authorization: OAuth 9d10e6946f5dfc896d4c471721108dee5313eba8" \
    --dump-header /tmp/curl_out_header.txt


# Get all users
curl http://localhost:8080/idm/users \
    --request GET \
	--header "Content-Type:application/json" \
	--header "Accept:application/json" \
    --header "Authorization: OAuth 9d10e6946f5dfc896d4c471721108dee5313eba8" \
    --dump-header /tmp/curl_out_header.txt

# Update user
curl http://localhost:8080/idm/users/johneo \
    --request PUT \
	--header "Content-Type:application/json" \
	--header "Accept:application/json" \
    --header "Authorization: OAuth 9d10e6946f5dfc896d4c471721108dee5313eba8" \
    --data "{user:{customerId:RCN-000-AAA-6543, firstname:Sir, lastname:Phil, email:sirphil@example.com, middlename:notacoward, secretQuestion:'What is your favourite pie', secretAnswer:3.141592, preferredLang:en_US, timezone:'America/Phoenix'}}" \
    --dump-header /tmp/curl_out_header.txt

#Generate password recovery email
curl http://localhost:8081/idm/customers/RCN-000-000-000/users/mkovacs/password/recoveryemail \
    --request POST \
	--header "Content-Type:application/xml" \
	--header "Accept:application/xml" \
    --header "Authorization: OAuth DFW-cf7109478bd49f7a98c2d2ce7f696de504fcc625" \
    --data '<passwordRecovery xmlns="http://docs.rackspacecloud.com/idm/api/v1.0" callbackUrl="http://cp.rackspace.com/password_recovery" templateUrl="http://localhost:8080/template/test_template.html"  from="noreply@idm.rackspace.com" replyTo="" subject="Forgotten Password Information" ><customParams><customParam name="username" value="bob.the.builder" /><customParam name="serviceName" value="RackFantastico"/><customParam name="securityEmail" value="dont.botherme@example.com"/><customParam name="secNotifyEamilSubject" value="Unrequested password change!"/><customParam name="I shouldnt be here." value="nope."/></customParams></passwordRecovery>' \
    --dump /tmp/curl_out_header.txt


curl http://localhost:8080/idm/customers/RCN-000-000-000/users/johneo/password/recoveryemail \
    --request POST \
	--header "Content-Type:application/json" \
	--header "Accept:application/json" \
    --header "Authorization: OAuth DFW-cf7109478bd49f7a98c2d2ce7f696de504fcc625" \
    --data "{passwordRecovery: {callbackUrl:'http://cp.rackspace.com/password_recovery', templateUrl:'', from:'noreply@idm.rackspace.com', replyTo:'', subject:'Forgotten Password Information'}}" \
    --dump /tmp/curl_out_header.txt

#Recovery token: 3846b840bebe8f100a4b9b270d93065e7c5c0fa7

curl http://localhost:8080/idm/users/johneo/password?recovery=true \
    --request PUT \
	--header "Content-Type:application/json" \
	--header "Accept:application/json" \
    --header "Authorization: OAuth 3846b840bebe8f100a4b9b270d93065e7c5c0fa7" \
    --data "{passwords: {newPassword:'d0nTl00k@mE'}}" \
    --dump /tmp/curl_out_header.txt

#Get a toke for the user using the new password
curl http://localhost:8080/idm/token \
    --request POST \
	--header "Content-Type:application/json" \
	--header "Accept:application/json" \
    --data "{tokenRequest: {grantType:password, clientId:ABCDEF, clientSecret:password, username:johneo, password:d0nTl00k@mE}}" \
    --dump-header /tmp/curl_out_header.txt

# Delete user
curl http://localhost:8080/idm/users/johneo \
    --request DELETE \
	--header "Content-Type:application/json" \
	--header "Accept:application/json" \
    --header "Authorization: OAuth 9d10e6946f5dfc896d4c471721108dee5313eba8" \
    --dump-header /tmp/curl_out_header.txt

# Set role
curl http://localhost:8080/idm/users/devdatta/roles \
    --request POST \
        --header "Content-Type:application/json" \
        --header "Accept:application/json" \
    --header "Authorization: OAuth d6fdd27916dc85c93c4e25eaecd806d15cd3129f" \
    --data "{roleParam:{roleName:Admin}}"

# Delete role
curl http://localhost:8080/idm/users/devdatta1/roles \
    --request DELETE \
        --header "Content-Type:application/json" \
        --header "Accept:application/json" \
    --header "Authorization: OAuth d6fdd27916dc85c93c4e25eaecd806d15cd3129f" \
    --data "{roleParam:{roleName:Admin}}"


