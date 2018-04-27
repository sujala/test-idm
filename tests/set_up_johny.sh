#!/usr/bin/env bash

build_identity() {
    pushd ..
    ./gradlew clean build -x nonCommitTest -x commitTest -x commonTest -x test
    mv build/libs/*war build/libs/idm.war
    popd
}

setup_docker_configs() {
    mkdir -p $DOCKER_CONFIG_DIR
    pushd ..
    # copy over all of identity configs
    cp -a src/main/config/VAGRANT/. tests/$DOCKER_CONFIG_DIR
    # copy over log4j.xml
    cp src/main/config/log4j.xml tests/$DOCKER_CONFIG_DIR
    popd
}

start_docker() {
    docker-compose -p tests up -d
    echo "sleep time for repose & idm to spin up!"
    sleep 30
}

setup_test_configs() {
    mkdir -p $TESTS_CONFIG_DIR
    mkdir -p $TESTS_LOG_DIR
    # get service token and put it into api.conf
    TOKEN=$(docker run -ti --rm --network host dadean/curl-jq sh -c "curl -s -X POST http://localhost:8080/idm/cloud/v2.0/tokens -H 'cache-control: no-cache' -H 'content-type: application/json' -d '{ \"auth\": { \"passwordCredentials\":{ \"username\":\"AuthQE\", \"password\": \"Auth1234\" }}}' | jq -r .access.token.id")
    echo ${TOKEN}

    if [ -z "${TOKEN}" ]
    then
        echo "Cluster is unavailable.  Let's wait another 15 seconds"
        sleep 15
        setup_test_configs
    fi

    # copy api.conf over to ~/.identity/api.conf
    cp etc/api.conf $TESTS_CONFIG_DIR/api.conf
    sed -i -e "s|service_admin_auth_token=.*|service_admin_auth_token=${TOKEN}|g" $TESTS_CONFIG_DIR/api.conf
    sed -i -e "s|base_url=.*|base_url=http://localhost:8080|g" $TESTS_CONFIG_DIR/api.conf
}

TESTS_CONFIG_DIR=/tmp/.identity
TESTS_LOG_DIR=/tmp/.identity/logs
DOCKER_CONFIG_DIR=.identity/configs

build_identity
setup_docker_configs
start_docker
setup_test_configs
