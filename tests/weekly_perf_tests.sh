#!/bin/bash

# Run performance tests against LON staging identity

PERF_URL=https://scheduler-identity-perf.devapps.rsi.rackspace.net
TEST_ID=81
EXECUTION_NAME="Weekly staging test"
AGENT_NAME="docker.artifacts.rackspace.net/ollie-agent-gatling:latest"

echo "Start Weekly Test"

execution_id=$(curl $PERF_URL/executions \
  -H "Content-Type: application/json" \
  -d '{"execution_name": "'"$EXECUTION_NAME"'", "test_id": "'"$TEST_ID"'", "tags": ["weekly", "lon-staging"], "image_name": "'"$AGENT_NAME"'"}' | python -c 'import sys, json; print json.load(sys.stdin)["id"]')

while true; do
  status=$(curl $PERF_URL/executions/$execution_id | python -c 'import sys, json; print json.load(sys.stdin)["status"]')
  echo "Weekly Test Status for $execution_id: $status"
  if [ "$status" == \"Completed\" ] || [ "$status" == \"Errored\" ]; then break; fi
  sleep 60  
done

echo "Test Finished for $execution_id: $status"

if [ "$status" == \"Completed\" ]
then
   exit 0
else
   exit 1
fi