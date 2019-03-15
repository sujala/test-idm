#!/bin/sh

if [ $# -ne 3 ]; then
    echo "usage: $0 [artifactory_endpoint] [service_account_password] [do_not_delete_artifacts_file]"
    exit 1
fi

ARTIFACTORY=$1
PASSWORD=$2
DO_NOT_DELETE_FILE=$3

list_artifacts() {
    curl $ARTIFACTORY/artifactory/api/search/artifact?name=idm\&repos=identity-maven-local -q | jq .results[].uri | grep -E 'idm-[0-9]+.[0-9]+.[0-9]+-[0-9]+.*.pom' -o | sed 's/.pom//' | sed 's/idm-//'
}

delete_artifact() {
    echo "deleting artifact $1"
    curl -X DELETE $ARTIFACTORY/artifactory/identity-maven-local/com/rackspace/idm/$1 -H "X-JFrog-Art-Api: ${PASSWORD}"
}

get_latest_artifact() {
    curl -s -L $1/artifactory/identity-maven-local/com/rackspace/idm/maven-metadata.xml \
       | grep "<version>.*</version>" \
       | tail -n 1 \
       | sed -e "s#\(.*\)\(<version>\)\(.*\)\(</version>\)\(.*\)#\3#g"
}

artifacts=$(list_artifacts)

do_not_delete=$(cat $DO_NOT_DELETE_FILE; get_latest_artifact $ARTIFACTORY)

for artifact in $artifacts; do
    if echo "$do_not_delete" | grep -q $artifact ; then
        echo "not deleting $artifact"
    else
        delete_artifact $artifact
    fi
done
