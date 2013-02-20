#!/bin/bash

BASEDIR=$(dirname $(readlink -f $0))

source $HOME/.bash_aliases

curl -X GET https://d-api2.cidm.iad2.corp.rackspace.com -H "Accept: application/json" --verbose --insecure

# Sleep for 120 seconds
sleep 120

echo "### Running JMeter performance test ###"

# Clear out old results
rm $BASEDIR/jmeter/results.jtl
rm $BASEDIR/jmeter.log

# Run the tests
echo "## Running the tests"
jmeter -n -p $BASEDIR/jmeter/user.properties -t $BASEDIR/jmeter/AuthValidate_JenkinsUseOnly.jmx -l $BASEDIR/jmeter/results.jtl
