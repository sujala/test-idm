#!/bin/sh

BASEDIR=$(dirname $(readlink -f $0))

if [ $# -ne 1 ]
then
    echo "usage: copyWar.sh <host>"
else
    HOST=$1
    WAR=`find $BASEDIR | grep idm.*.war$`
    scp $WAR gauth@$HOST:/opt/glassfish/glassfish/domains/gauth/autodeploy/idm.war
fi
