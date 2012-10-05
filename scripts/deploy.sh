#!/bin/bash

BASEDIR=$(dirname $(readlink -f $0))

if [ $# -ne 1 ]
then
    echo "usage: $0 <test1|test2|dev1|dev2>"
    exit
fi

DEPLOY="$1"

if [ $DEPLOY == "test1" ]
then
    HOST=t-api1.cidm.iad2.corp.rackspace.com

    $BASEDIR/editProperties.sh useCloudAuth=false useCloudAuth=true
    $BASEDIR/editProperties.sh gaIsSourceOfTruth=true gaIsSourceOfTruth=false
    $BASEDIR/copyProperties.sh $HOST TEST
fi

if [ $DEPLOY == "test2" ]
then
    HOST=t-api2.cidm.iad2.corp.rackspace.com

    $BASEDIR/editProperties.sh useCloudAuth=true useCloudAuth=false
    $BASEDIR/editProperties.sh gaIsSourceOfTruth=false gaIsSourceOfTruth=true
    $BASEDIR/copyProperties.sh $HOST TEST
fi

if [ $DEPLOY == "dev1" ]
then
    HOST=d-api1.cidm.iad2.corp.rackspace.com

    $BASEDIR/editProperties.sh useCloudAuth=false useCloudAuth=true
    $BASEDIR/editProperties.sh gaIsSourceOfTruth=true gaIsSourceOfTruth=false
    $BASEDIR/copyProperties.sh $HOST DEV
fi

if [ $DEPLOY == "dev2" ]
then
    HOST=d-api2.cidm.iad2.corp.rackspace.com

    $BASEDIR/editProperties.sh useCloudAuth=true useCloudAuth=false
    $BASEDIR/editProperties.sh gaIsSourceOfTruth=false gaIsSourceOfTruth=true
    $BASEDIR/copyProperties.sh $HOST DEV
fi

if [ -n "$HOST" ]
then
    ssh gauth@$HOST './runUndeploySteps.sh /opt/glassfish/glassfish/domains/gauth'
    $BASEDIR/copyWar.sh $HOST
else
    echo "'$DEPLOY' environment is not defined"
fi
