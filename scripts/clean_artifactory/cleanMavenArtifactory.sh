#!/bin/sh

if [ $# -ne 4 ]; then
    echo "usage: $0 [artifactory_endpoint] [service_account_username] [service_account_password] [do_not_delete_artifacts_file]"
    exit 1
fi

ARTIFACTORY=$1
USERNAME=$2
PASSWORD=$3
DO_NOT_DELETE_FILE=$4

list_artifacts() {
    curl $ARTIFACTORY/artifactory/api/search/artifact?name=idm\&repos=identity-maven-local -q | jq .results[].uri | grep -E 'idm-[0-9]+.[0-9]+.[0-9]+-[0-9]+.*.pom' -o | sed 's/.pom//' | sed 's/idm-//'
}

delete_artifact() {
    echo "deleting artifact $1"
    curl -X DELETE $ARTIFACTORY/artifactory/identity-maven-local/com/rackspace/idm/$1 -H "X-JFrog-Art-Api: ${PASSWORD}"
}

artifacts=$(list_artifacts)

for artifact in $artifacts; do
    if grep -q $artifact "$DO_NOT_DELETE_FILE"; then
        echo "not deleting $artifact"
    else
        delete_artifact $artifact
    fi
done
