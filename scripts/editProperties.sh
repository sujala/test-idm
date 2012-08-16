#!/bin/sh

BASEDIR=$(dirname $(readlink -f $0))

if [ $# -ne 3 ]
then
    echo "usage: editProperties.sh <current> <update>"
else
    sed -e "s/$1/$2/g" $BASEDIR/../src/main/config/base.idm.properties > $BASEDIR/../src/main/config/temp
    mv $BASEDIR/../src/main/config/temp $BASEDIR/../src/main/config/base.idm.properties
fi
