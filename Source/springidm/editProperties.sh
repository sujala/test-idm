#!/bin/sh

BASEDIR=$(dirname $(readlink -f $0))

if [ $# -ne 2 ] 
then
    echo "usage: editProperties.sh <current> <update>"
else
    sed -e "s/$1/$2/g" src/main/config/base.idm.properties > src/main/config/temp
    mv src/main/config/temp src/main/config/base.idm.properties
fi
