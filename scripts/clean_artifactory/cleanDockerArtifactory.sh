#!/bin/sh

if [ $# -ne 3 ]; then
    echo "usage: $0 [artifactory_endpoint] [service_account_password] [do_not_delete_artifacts_file]"
    exit 1
fi

ARTIFACTORY=$1
PASSWORD=$2
DO_NOT_DELETE_FILE=$3
DOCKER_REPOS="ca-directory cloud-feeds cloud-identity dynamodb edirectory openldap postfix repose"

list_image_tags() {
    image=$1
    curl $ARTIFACTORY/artifactory/api/docker/identity-docker-local/v2/${image}/tags/list -H "X-JFrog-Art-Api: ${PASSWORD}" | jq .tags[] | grep -E '[0-9]+.[0-9]+.[0-9]+-[0-9]+[-SNAPSHOT]*' -o
}

delete_artifact() {
    image=$1
    tag=$2
    echo "deleting image with tag $tag"
    curl -X DELETE $ARTIFACTORY/artifactory/identity-docker-local/$image/$tag -H "X-JFrog-Art-Api: ${PASSWORD}"
}

for image in $DOCKER_REPOS; do
    echo "########## $image ##########"
    tags=$(list_image_tags $image)

    for tag in $tags; do
        if grep -q $tag "$DO_NOT_DELETE_FILE"; then
            echo "not deleting tag $tag"
        else
            delete_artifact $image $tag
        fi
    done
done
