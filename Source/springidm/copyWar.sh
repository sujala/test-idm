#!/bin/sh

BASEDIR=$(dirname $(readlink -f $0))

if [ $# -ne 1 ]
then
    echo "usage: copyPropertiesgcsh <host>"
else
    HOST=$1
    scp $BASEDIR/target/(idm.*\.war) gauth@$HOST:/opt/glassfish/glassfish/domains/gauth/autodeploy/
fi
