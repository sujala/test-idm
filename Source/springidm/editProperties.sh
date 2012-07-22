#!/bin/sh

BASEDIR=$(dirname $(readlink -f $0))

if [ $# -ne 3 ]
then
    echo "usage: editProperties.sh <current> <update> <ENV>"
else
    sed -e "s/$1/$2/g" $BASEDIR/src/main/config/$3/idm.properties > $BASEDIR/src/main/config/$3/temp
    mv $BASEDIR/src/main/config/$3/temp $BASEDIR/src/main/config/$3/idm.properties
fi
