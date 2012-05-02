#!/bin/sh

BASEDIR=$(dirname $(readlink -f $0))

if [ $# -ne 2 ]
then
    echo "usage: copyProperties.sh <host> <war.version>"
else
    HOST=$1
    VER=$2
    scp $BASEDIR/target/$VER gauth@$HOST:/opt/glassfish/glassfish/domains/gauth/autodeploy/
fi
