#!/bin/bash -ex

# Stop TC.
/bin/bash /etc/idm/stop_tomcat.sh

# Go ahead and remove JACOCO anyway, next time tomcat is restarted it will
# get removed.
sed -i.bak '/JACOCO/d' /usr/share/tomcat7/bin/setenv.sh

cd /etc/idm
rm -f jacoco_setup
rm -rf jacoco

/bin/bash /etc/idm/start_tomcat.sh

