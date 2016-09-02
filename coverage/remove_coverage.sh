#!/bin/bash -ex
if [ -z $1 ]; then
    echo "Missing Identity Docker instance name."
    exit -1
fi
export IDENTITY=$1

docker exec -it $IDENTITY bash -c "cd /etc/idm;rm -f remove_jacoco.sh"
docker cp remove_jacoco.sh $IDENTITY:/etc/idm/remove_jacoco.sh
docker exec -it $IDENTITY bash -c "cd /etc/idm;chmod 777 ./remove_jacoco.sh"
docker exec -it $IDENTITY bash -c "cd /etc/idm;./remove_jacoco.sh"
