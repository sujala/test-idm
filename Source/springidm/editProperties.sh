#!/bin/sh

BASEDIR=$(dirname $(readlink -f $0))

if [ $# -ne 2 ] 
then
    echo "usage: editProperties.sh <current> <update>"
else
    sed -e "s/$1/$2/g" $BASEDIR/src/main/config/DEV/idm.properties > $BASEDIR/src/main/config/DEV/temp
    mv $BASEDIR/src/main/config/DEV/temp $BASEDIR/src/main/config/DEV/idm.properties
fi
