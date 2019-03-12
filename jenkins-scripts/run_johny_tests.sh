#!/bin/bash -x

check_status() {
    endpoint=$1
    tries=$2
    TEST_STATUS=`curl -so /dev/null -w '%{response_code}' $endpoint`
    while [[ "$TEST_STATUS" != "200" ]]; do
        echo "waiting for identity test cluster to set up ..."
        echo "current status: $TEST_STATUS against $endpoint with try $tries"
        sleep 10
        tries=$((tries + 1))

        if [[ $tries == 20 ]]; then
            echo "FAILED TO START IDENTITY TEST CLUSTER"
            exit 1
        fi
        TEST_STATUS=`curl -so /dev/null -w '%{response_code}' $endpoint`
    done
}
cd tests
TEST_CONFIG_DIR=/tmp/.identity
mkdir -p $TEST_CONFIG_DIR
mkdir -p $TEST_CONFIG_DIR/logs
printenv
pip install -r api/requirements.txt
pip install opencafe
pip install tox
cafe-config init || true
cafe-config plugins install http
ls -latr
TEST_ENDPOINT=https://repose-${SANDBOX_NAME}-${NAMESPACE_NAME}.iad.devapps.rsi.rackspace.net
check_status $TEST_ENDPOINT/idm/cloud
TOKEN=`curl -s -X POST "${TEST_ENDPOINT}/idm/cloud/v2.0/tokens" -H 'content-type: application/json' -d "{ \"auth\": { \"passwordCredentials\":{ \"username\":\"AuthQE\", \"password\": \"Auth1234\" }}}" | python  -c 'import sys, json; print json.load(sys.stdin)["access"]["token"]["id"]'`
cp etc/api.conf $TEST_CONFIG_DIR/api.conf
sed -i -e "s|service_admin_auth_token=.*|service_admin_auth_token=${TOKEN}|g" $TEST_CONFIG_DIR/api.conf
sed -i -e "s|base_url=.*|base_url=${TEST_ENDPOINT}|g" $TEST_CONFIG_DIR/api.conf
sed -i -e "s|internal_url=.*|internal_url=${TEST_ENDPOINT}|g" $TEST_CONFIG_DIR/api.conf
sed -i -e "s|nosetests --with-xunit --xunit-file=nosetests.xml|nosetests -a '!skip_at_gate' --with-xunit --xunit-file=nosetests.xml|g" tox.ini

export CAFE_CONFIG_FILE_PATH=$TEST_CONFIG_DIR/api.conf
export CAFE_ROOT_LOG_PATH=$TEST_CONFIG_DIR/logs
export CAFE_TEST_LOG_PATH=$TEST_CONFIG_DIR/logs
tox
