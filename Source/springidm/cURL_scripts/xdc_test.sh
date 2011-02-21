curl http://10.127.7.164:8080/v1.0/token \
    --request POST \
	--header "Content-Type:application/json" \
	--header "Accept:application/json" \
    --data '{"grant_type":"PASSWORD", "client_id":"ABCDEF", "client_secret":"password", "username":"mkovacs", "password":"password"}' \
    --dump-header /tmp/curl_out_header.txt

QA-a5fb44e7204442e1a6346082b72e6c69


curl http://10.127.7.166:8080/v1.0/token \
    --request POST \
	--header "Content-Type:application/json" \
	--header "Accept:application/json" \
    --data '{"grant_type":"PASSWORD", "client_id":"ABCDEF", "client_secret":"password", "username":"mkovacs", "password":"password"}' \
    --dump-header /tmp/curl_out_header.txt

DEV-585a458ff1344258b44b9293e79fad42


curl http://127.0.0.1:8080/v1.0/token \
    --request POST \
	--header "Content-Type:application/json" \
	--header "Accept:application/json" \
    --data '{"grant_type":"PASSWORD", "client_id":"ABCDEF", "client_secret":"password", "username":"mkovacs", "password":"password"}' \
    --dump-header /tmp/curl_out_header.txt

JEO-3e5d64de2c7e475894c90ff1c9da0fea


curl http://127.0.0.1:8080/v1.0/token \
    --request POST \
	--header "Content-Type:application/json" \
	--header "Accept:application/json" \
    --data '{"grant_type":"NONE", "client_id":"18e7a7032733486cd32f472d7bd58f709ac0d221", "client_secret":"password"}' \
    --dump-header /tmp/curl_out_header.txt

JEO-f80f001461a841b4b7825e99ccf8587a


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