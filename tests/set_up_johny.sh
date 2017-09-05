#!/usr/bin/env bash

# build identity
pushd .. && ./gradlew build -x nonCommitTest -x commitTest -x test && popd
# copy over log4j.xml
pushd .. && cp src/main/config/log4j.xml src/main/config/VAGRANT/ && popd
# docker compose up
docker-compose -p tests up -d
# sleep for 60 seconds to give repose time to spin up!
sleep 60
# get service token and put it into api.conf
TOKEN=$(docker run -ti --rm --network tests_default dadean/curl-jq sh -c "curl -s -X POST http://tests_repose_1:8080/idm/cloud/v2.0/tokens -H 'cache-control: no-cache' -H 'content-type: application/json' -d '{ \"auth\": { \"passwordCredentials\":{ \"username\":\"AuthQE\", \"password\": \"Auth1234\" }}}' | jq -r .access.token.id")
echo ${TOKEN}
sed -i -e "s@replace_with_auth_token_if_run_service_admin_tests_True@${TOKEN}@g" etc/api.conf
sed -i -e "s@https://identity.api.rackspacecloud.com@http://localhost:8080@g" etc/api.conf
sed -i -e "s@localhost_reposeB_1@tests_repose_1@g" etc/api.conf
sed -i -e "s@localhost_identityB_1@tests_identity_1@g" etc/api.conf
# set required env vars
export CAFE_CONFIG_FILE_PATH=~/.identity/api.conf
export CAFE_ROOT_LOG_PATH=~/.identity/logs
export CAFE_TEST_LOG_PATH=~/.identity/logs
# create ~/.identity directory
mkdir -p ~/.identity
# copy api.conf over to ~/.identity/api.conf
cp etc/api.conf ~/.identity/api.conf