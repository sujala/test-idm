#
# A set of CURL calls to demonstrate IDM operations specific to Cloud API's Auth service.
#
# Note that the API calls would be done over HTTPS in production environment.
#

#
# A work area to paste token from service call results.
# 
# Client Token: 5883af97003beea0641537cde12dccf531a1c057
# Expires at: 11:20
# 

# Acquire a token with clientID/clientSecret.
curl http://localhost:8080/idm/token \
    --request POST \
    --data "grant_type=none&client_id=1183ca858a25100bd8bbd68f5f82ebe2ec8dfa87&client_secret=4937ab1185350e1c9a306421ac6b8ded29b03db5" \
    --dump-header /tmp/curl_out_header.txt
# Expected response body:
# access_token=<token value>&expire_in=<expiration in seconds>

# Add a user for testing purpose. This API call is NOT part of what the Auth service currently uses.
curl http://localhost:8080/idm/users \
    --request POST \
    --header "Authorization: token token=5883af97003beea0641537cde12dccf531a1c057" \
    --data 'customerId=RCN-000-000-000&personId=RPN-000-123-4567&username=johneo&password=dontlookatme&apiKey=api_secret_bowwowwow321&firstname=John&lastname=Eo&email=johneo@example.com&middlename=helloworld' \
    --data '&secretQuestion=What is your favourite colour&secretAnswer=Yellow. No, blue' \
    --data '&preferredLang=en_US&timezone=America/Chicago' \
    --dump /tmp/curl_out_header.txt
# Expected response body:
#{customerId: "RCN-000-987-6543",username: "johneo",firstname: "John",middlename: "helloworld",lastname: "Eo",email: "johneo@example.com"}

# Authenticate user and acquire a token for him/her.
curl http://localhost:8080/idm/token \
    --request POST \
    --data "grant_type=basic&client_id=1183ca858a25100bd8bbd68f5f82ebe2ec8dfa87&client_secret=4937ab1185350e1c9a306421ac6b8ded29b03db5" \
    --data "&username=johneo&password=api_secret_bowwowwow321" \
    --dump-header /tmp/curl_out_header.txt
# Expected response body:
# access_token=<access token>&refresh_token=<refresh token>&expire_in=<access token expiration in seconds>

#
# User Token: 0f7b1b88cf324326fd34fb19943d5ca58e7a8d2d
# Expires at: 11:35
#

# Validate token
curl http://localhost:8080/idm/token/0f7b1b88cf324326fd34fb19943d5ca58e7a8d2d \
    --request GET \
    --header "Authorization: token token=5883af97003beea0641537cde12dccf531a1c057" \
    --dump-header /tmp/curl_out_header.txt
# Expected response body:
# {customerId: "RCN-000-987-6543",username: "johneo",firstname: "John",lastname: "Eo",email: "johneo@example.com"}

# Delete user that was created for testing purpose. This API call is NOT part of what the Auth service currently uses.
curl http://localhost:8080/idm/users/johneo \
    --request DELETE \
    --header "Authorization: token token=5883af97003beea0641537cde12dccf531a1c057" \
    --dump-header /tmp/curl_out_header.txt
