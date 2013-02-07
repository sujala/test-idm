#!/bin/bash
echo "### Running JMeter performance test ###"

# Clear out old results
rm ./jmeter/results.jtl

# Run the tests
echo "## Running the tests"
jmeter -n -t ./jmeter/AuthValidate_JenkinsUseOnly.jmx -l ./jmeter/results.jtl
