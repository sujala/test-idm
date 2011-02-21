# User token obtained at QA "data center"
curl http://10.127.7.164:8080/v1.0/token \
    --request POST \
	--header "Content-Type:application/json" \
	--header "Accept:application/json" \
    --data '{"grant_type":"PASSWORD", "client_id":"ABCDEF", "client_secret":"password", "username":"mkovacs", "password":"password"}' \
    --dump-header /tmp/curl_out_header.txt

QA-ccd7116145ba4dcdb8b3dc75c514938a


# User token obtained at DEV "data center"
curl http://10.127.7.166:8080/v1.0/token \
    --request POST \
	--header "Content-Type:application/json" \
	--header "Accept:application/json" \
    --data '{"grant_type":"PASSWORD", "client_id":"ABCDEF", "client_secret":"password", "username":"mkovacs", "password":"password"}' \
    --dump-header /tmp/curl_out_header.txt

DEV-5ea467eacf2d4c27b67376ade873e9b8


# User token obtained at local "data center"
curl http://127.0.0.1:8080/v1.0/token \
    --request POST \
	--header "Content-Type:application/json" \
	--header "Accept:application/json" \
    --data '{"grant_type":"PASSWORD", "client_id":"ABCDEF", "client_secret":"password", "username":"mkovacs", "password":"password"}' \
    --dump-header /tmp/curl_out_header.txt

JEO-e30b3a39c526408a89f7fa37688fc4de


# Customer IDM client token obtained at local "data center"
curl http://127.0.0.1:8080/v1.0/token \
    --request POST \
	--header "Content-Type:application/json" \
	--header "Accept:application/json" \
    --data '{"grant_type":"NONE", "client_id":"18e7a7032733486cd32f472d7bd58f709ac0d221", "client_secret":"password"}' \
    --dump-header /tmp/curl_out_header.txt

JEO-b62f99eb26e9422f80e6289d7583c145


# Global token revocation call
curl "http://127.0.0.1:8080/v1.0/token/JEO-e30b3a39c526408a89f7fa37688fc4de" \
    --request DELETE \
	--header "Content-Type:application/json" \
	--header "Accept:application/json" \
	--header "Authorization:OAuth JEO-b62f99eb26e9422f80e6289d7583c145" \
    --dump-header /tmp/curl_out_header.txt


# This isn't going to trigger global revocation. It should only be invoked by IDM only.
curl "http://127.0.0.1:8080/v1.0/token?querytype=customer&id=RACKSPACE" \
    --request DELETE \
	--header "Content-Type:application/json" \
	--header "Accept:application/json" \
	--header "Authorization:OAuth JEO-b62f99eb26e9422f80e6289d7583c145" \
    --dump-header /tmp/curl_out_header.txt

# This isn't going to trigger global revocation. It should only be invoked by IDM only.
curl "http://127.0.0.1:8080/v1.0/token?querytype=owner&id=mkovacs" \
    --request DELETE \
	--header "Content-Type:application/json" \
	--header "Accept:application/json" \
	--header "Authorization:OAuth JEO-f80f001461a841b4b7825e99ccf8587a" \
    --dump-header /tmp/curl_out_header.txt