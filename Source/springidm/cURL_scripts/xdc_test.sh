# User token from the local "data center"
curl http://127.0.0.1:8080/v1.0/token \
    --request POST \
	--header "Content-Type:application/json" \
	--header "Accept:application/json" \
    --data '{"grant_type":"PASSWORD", "client_id":"ABCDEF", "client_secret":"password", "username":"mkovacs", "password":"password"}' \
    --dump-header /tmp/curl_out_header.txt

#JEO-3e5d64de2c7e475894c90ff1c9da0fea


# User token from the DEV "data center"
curl http://10.127.7.166:8080/v1.0/token \
    --request POST \
	--header "Content-Type:application/json" \
	--header "Accept:application/json" \
    --data '{"grant_type":"PASSWORD", "client_id":"ABCDEF", "client_secret":"password", "username":"mkovacs", "password":"password"}' \
    --dump-header /tmp/curl_out_header.txt

#DEV-bf42bb246de14f369d295402fde35a96


# User token from the QA "data center"
curl http://10.127.7.164:8080/v1.0/token \
    --request POST \
	--header "Content-Type:application/json" \
	--header "Accept:application/json" \
    --data '{"grant_type":"PASSWORD", "client_id":"ABCDEF", "client_secret":"password", "username":"mkovacs", "password":"password"}' \
    --dump-header /tmp/curl_out_header.txt

#QA-dd9ef126375d417c86b6b86085fc2afc


# Client token for Customer IDM from the local "data center"
curl http://127.0.0.1:8080/v1.0/token \
    --request POST \
	--header "Content-Type:application/json" \
	--header "Accept:application/json" \
    --data '{"grant_type":"NONE", "client_id":"18e7a7032733486cd32f472d7bd58f709ac0d221", "client_secret":"password"}' \
    --dump-header /tmp/curl_out_header.txt

# JEO-9496f75ed29841f985ec60b42ee4d076


# Client token for Customer IDM from the QA "data center"
curl http://10.127.7.164:8080/v1.0/token \
    --request POST \
	--header "Content-Type:application/json" \
	--header "Accept:application/json" \
    --data '{"grant_type":"NONE", "client_id":"18e7a7032733486cd32f472d7bd58f709ac0d221", "client_secret":"password"}' \
    --dump-header /tmp/curl_out_header.txt

#QA-a831d25e2b9f4a9c88901ae31044513c


# Get the QA-issued token through the local "data center"
curl "http://127.0.0.1:8080/v1.0/token/QA-489b6071fcee4ce189065956010c77f0" \
    --request GET \
	--header "Content-Type:application/json" \
	--header "Accept:application/json" \
	--header "Authorization:OAuth JEO-8c2d2b0b50e74c758a95ed2230819411" \
    --dump-header /tmp/curl_out_header.txt


# Get serialized token from another "data center"
curl "http://127.0.0.1:8080/v1.0/token/QA-dd9ef126375d417c86b6b86085fc2afc" \
    --request GET \
	--header "Content-Type:application/json" \
	--header "Accept:application/octet-stream,application/json" \
	--header "Authorization:OAuth JEO-417dc078d0d84c57a493ccc20a218ecb" \
    --dump-header /tmp/curl_out_header.txt


# Validate token from another "data center"
curl "http://127.0.0.1:8080/v1.0/token/QA-dd9ef126375d417c86b6b86085fc2afc" \
    --request GET \
	--header "Content-Type:application/json" \
	--header "Accept:application/json" \
	--header "Authorization:OAuth JEO-417dc078d0d84c57a493ccc20a218ecb" \
    --dump-header /tmp/curl_out_header.txt


# The following only perform the local revocation portion. They are triggered by an IDM instance in another DC that initiates the global revocation.
# For example, a user or customer lock call would trigger the following calls

curl "http://127.0.0.1:8080/v1.0/token?querytype=customer&id=RACKSPACE" \
    --request DELETE \
	--header "Content-Type:application/json" \
	--header "Accept:application/json" \
	--header "Authorization:OAuth JEO-f80f001461a841b4b7825e99ccf8587a" \
    --dump-header /tmp/curl_out_header.txt

curl "http://127.0.0.1:8080/v1.0/token?querytype=owner&id=mkovacs" \
    --request DELETE \
	--header "Content-Type:application/json" \
	--header "Accept:application/json" \
	--header "Authorization:OAuth JEO-f80f001461a841b4b7825e99ccf8587a" \
    --dump-header /tmp/curl_out_header.txt