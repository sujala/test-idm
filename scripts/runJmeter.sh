#!/bin/bash

BASEDIR=$(dirname $(readlink -f $0))

source $HOME/.bash_aliases

echo "### Running JMeter performance test ###"

# Clear out old results
rm $BASEDIR/jmeter/results.jtl
rm $BASEDIR/jmeter.log

# Run the tests
echo "## Running the tests"
jmeter -n -p $BASEDIR/jmeter/user.properties -t $BASEDIR/jmeter/AuthValidate_JenkinsUseOnly.jmx -l $BASEDIR/jmeter/results.jtl
