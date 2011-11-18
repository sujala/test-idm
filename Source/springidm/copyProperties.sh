#!/bin/sh

if [ $# -ne 2 ]
then
    echo "usage: copyProperties.sh <host> <environment>"
else
    HOST=$1
    ENV=$2
    scp {./src/main/config/$ENV/idm.properties,./src/main/config/base.idm.properties,./src/main/config/$ENV/idm.secrets.properties,./src/main/config/log4j.xml} rack@$HOST:/etc/idm/
fi
