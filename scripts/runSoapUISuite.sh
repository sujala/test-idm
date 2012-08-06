#!/bin/sh

BASEDIR=$(dirname $(readlink -f $0))

if [ $# -ne 1 ]
then
    echo "usage: runSoapUISuite <profile>"
else
    PROFILE=$1
    cd $BASEDIR/../
    mvn eviware:maven-soapui-plugin:test -P$1
fi
