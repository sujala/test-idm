#!/usr/bin/env bash

# build identity
pushd .. && ./gradlew build -x nonCommitTest -x commitTest -x test && popd
# create ~/.identity directory
mkdir -p /tmp/.identity/configs
# copy over all of identity configs
pushd .. && cp -a src/main/config/VAGRANT/. tests/.identity/configs/ && popd
# copy over log4j.xml
pushd .. && cp src/main/config/log4j.xml tests/.identity/configs/ && popd
# docker compose up
docker-compose -p tests up -d
# sleep for 60 seconds to give repose time to spin up!
sleep 60
# get service token and put it into api.conf
TOKEN=$(docker run -ti --rm --network tests_default dadean/curl-jq sh -c "curl -s -X POST http://tests_repose_1:8080/idm/cloud/v2.0/tokens -H 'cache-control: no-cache' -H 'content-type: application/json' -d '{ \"auth\": { \"passwordCredentials\":{ \"username\":\"AuthQE\", \"password\": \"Auth1234\" }}}' | jq -r .access.token.id")
echo ${TOKEN}
# copy api.conf over to ~/.identity/api.conf
cp etc/api.conf /tmp/.identity/api.conf
sed -i -e "s|service_admin_auth_token=.*|service_admin_auth_token=${TOKEN}|g" /tmp/.identity/api.conf
sed -i -e "s|base_url=.*|base_url=http://localhost:8080|g" /tmp/.identity/api.conf
# set required env vars
export CAFE_CONFIG_FILE_PATH=/tmp/.identity/api.conf
export CAFE_ROOT_LOG_PATH=/tmp/.identity/logs
export CAFE_TEST_LOG_PATH=/tmp/.identity/logs
