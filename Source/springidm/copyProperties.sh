#!/bin/sh

BASEDIR=$(dirname $(readlink -f $0))

if [ $# -ne 3 ]
then
    echo "usage: copyPropertiesgcsh <host> <environment> <.ssh path>"
else
    HOST=$1
    ENV=$2
    SSHPATH=$3
    scp -i $SSHPATH/id_rsa $BASEDIR/src/main/config/$ENV/idm.properties gauth@$HOST:/home/gauth/config/
    scp -i $SSHPATH/id_rsa $BASEDIR/src/main/config/base.idm.properties gauth@$HOST:/home/gauth/config/
    scp -i $SSHPATH/id_rsa  $BASEDIR/src/main/config/$ENV/idm.secrets.properties gauth@$HOST:/home/gauth/config/
    scp -i $SSHPATH/id_rsa  $BASEDIR/src/main/config/log4j.xml gauth@$HOST:/home/gauth/config/
fi
