#!/bin/bash

touch /opt/results/run-report.log

inotifywait -q -m -e close_write /opt/results/run-report.log |
while read -r filename event
do
  echo "Run the report"
  result_path="$(cat /opt/results/run-report.log)"
  endpoint="$(cat /opt/results/endpoint)"
  echo "Run the report on $result_path and send results to $endpoint"
  /opt/gatling-2.2/bin/gatling.sh -ro $result_path
  curl -X PUT $endpoint -v
done
  
