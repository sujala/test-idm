#!/usr/bin/env bash

if (( $# != 1 )) ; then
    echo "Usage $0 <build_release>"
    echo "build_release - a boolean param specifying if the build should create a release (non-snapshot) artifact."
    exit 1
fi


if [ $1 == true ] ; then
    BUILD_RELEASE=true
else
    BUILD_RELEASE=false
fi

set +x
ls -alh /tmp/secrets
source /tmp/secrets/secrets.sh
echo artifactory_user=$ARTIFACTORY_USER >> gradle.properties
echo artifactory_password=$ARTIFACTORY_PASSWORD >> gradle.properties
set -x
./gradlew clean build -x test -x nonCommitTest -x commitTest -x commonTest artifactoryPublish --stacktrace -Pbuild_release=$BUILD_RELEASE
