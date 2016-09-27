#!/bin/bash -ex
export JACOCO_VERSION=${1:-"0.7.5.201505241946"}

#stop tomcat first
/bin/bash /etc/idm/stop_tomcat.sh

rm -f /tmp/jacoco_current.exec

cp /tmp/jacoco.exec /tmp/jacoco_current.exec

#start tomcat
/bin/bash /etc/idm/start_tomcat.sh

