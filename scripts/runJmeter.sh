#!/bin/bash
echo "### Running JMeter performance test ###"

# Clear out old results
rm ./jmeter/jmeter.jtl

# Run the tests
echo "## Running the tests"
cd "jmeter"

jmeter -n -t AuthValidate_JenkinsUseOnly.jmx -l my_results.jtl
