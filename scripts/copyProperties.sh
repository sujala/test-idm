#!/bin/sh

BASEDIR=$(dirname $(readlink -f $0))

if [ $# -ne 2 ]
then
    echo "usage: copyProperties.sh <host> <environment>"
else
    HOST=$1
    ENV=$2
    scp $BASEDIR/../src/main/config/$ENV/idm.properties gauth@$HOST:/home/gauth/config/
    scp $BASEDIR/../src/main/config/base.idm.properties gauth@$HOST:/home/gauth/config/
    scp $BASEDIR/../src/main/config/$ENV/idm.secrets.properties gauth@$HOST:/home/gauth/config/
    scp $BASEDIR/../src/main/config/log4j.xml gauth@$HOST:/home/gauth/config/
fi
