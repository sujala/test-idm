#!/bin/bash -ex
export JACOCO_VERSION=${1:-"0.7.5.201505241946"}
# Cleanup.
rm -f /tmp/jacoco.exec


if [[ ! -e /etc/idm/jacoco_setup ]] ; then
    mkdir -p /etc/idm/jacoco
    cd /etc/idm/jacoco 
    wget http://search.maven.org/remotecontent?filepath=org/jacoco/jacoco/${JACOCO_VERSION}/jacoco-${JACOCO_VERSION}.zip -O jacoco.zip
    yes | unzip jacoco.zip
    echo do jacoco "$(grep  "JACOCO" "/usr/share/tomcat7/bin/setenv.sh")"
    if [[ ! -z $(grep "JACOCO" "/usr/share/tomcat7/bin/setenv.sh") ]] 
    then
        echo "Jacoco already set up in Catalina"
    else
        echo "Setting up Jacoco in Catalina."
        echo 'export JACOCO="-javaagent:/etc/idm/jacoco/lib/jacocoagent.jar=destfile=/tmp/jacoco.exec,append=true,includes=*"' >> /usr/share/tomcat7/bin/setenv.sh
        echo 'export CATALINA_OPTS="$CATALINA_OPTS $JACOCO"' >> /usr/share/tomcat7/bin/setenv.sh
    fi
    touch /etc/idm/jacoco_setup
fi

# restart tomcat.
# Tomcat restarts can be flaky, so best to split it up and check 
# whether it was stopped/started.
/bin/bash /etc/idm/stop_tomcat.sh
/bin/bash /etc/idm/start_tomcat.sh

# verify file started
if [[ -e /tmp/jacoco.exec ]] ; then
    echo "Started successfully."
else
    echo "Startup Failed."

fi
