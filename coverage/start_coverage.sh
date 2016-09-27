#!/bin/bash -ex
if [ -z $1 ]; then
    echo "Missing Identity Docker instance name."
    exit -1
fi
export IDENTITY=$1
export JACOCO_VERSION=${2:-"0.7.5.201505241946"}


docker cp setup_jacoco.sh $IDENTITY:/etc/idm/setup_jacoco.sh
docker cp start_tomcat.sh $IDENTITY:/etc/idm/start_tomcat.sh
docker cp stop_tomcat.sh $IDENTITY:/etc/idm/stop_tomcat.sh

docker exec -it $IDENTITY bash -c "cd /etc/idm;chmod 777 ./setup_jacoco.sh"
docker exec -it $IDENTITY bash -c "cd /etc/idm;./setup_jacoco.sh $JACOCO_VERSION"

