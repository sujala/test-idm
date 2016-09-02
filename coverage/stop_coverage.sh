#!/bin/bash -ex
if [ -z $1 ]; then
    echo "Missing Identity Docker instance name."
    exit -1
fi
if [ -z $2 ]; then
    echo "Missing Identity version number." 
    exit -1
fi
export IDENTITY=$1
export IDENTITY_VERSION=$2
docker exec -it $IDENTITY bash -c "cd /etc/idm;rm -f stop_jacoco.sh"
docker cp stop_jacoco.sh $IDENTITY:/etc/idm/stop_jacoco.sh
docker exec -it $IDENTITY bash -c "cd /etc/idm;chmod +x ./stop_jacoco.sh"
docker exec -it $IDENTITY bash -c "cd /etc/idm;./stop_jacoco.sh"
mkdir -p target/classes
docker cp  $IDENTITY:/srv/tomcat7/webapps/idm/WEB-INF/lib/identity-$IDENTITY_VERSION.jar target/classes/.
docker cp $IDENTITY:/tmp/jacoco_current.exec target/jacoco.exec

