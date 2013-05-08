#!/bin/bash

# Source (import) the include file with all of the deployment code.
. "$(dirname "$0")/_warhammer.cloudidentity.include"

function deploy_config() {
  # Base URL at which war file and zip file can be found.  Script assumes *.war.md5 and *.zip.md5 files exist in same directory.
  URLBASE="http://d-build1.iad2.corp.rackspace.com:8100/nexus/content/repositories/releases/com/rackspace/idm/${VERSION}"

  # Java war file containing binary code to be deployed. (Filename only, no path.  Should be available at ${URLBASE}/${WAR_FILE})
  WAR_FILE="idm-${VERSION}.war"

  # Zip file containing configuration data. (Filename only, no path.  Should be available at ${URLBASE}/${ZIP_FILE})
  ZIP_FILE="idm-${VERSION}-config.zip"

  # Path on server to file that should be replaced with newly deployed war.
  WAR_DEPLOY_FILE="/opt/glassfish/glassfish/domains/${DOMAIN}/autodeploy/idm.war"

  # Path on server to directory into which zip file should be extracted.
  ZIP_DEPLOY_DIR="/home/${DOMAIN}/config"

  # User to SSH into servers as.  WARNING: Hudson user must have publickey (passwordless) access to this user on all servers.
  SSH_USER="${DOMAIN}"

  # Directory on server to save copy of zip/war in (for rollback purposes).
  ARTIFACTS_DIR="/home/${DOMAIN}/artifacts"

}


# Pass all command-line args to deploy_app() function and let it handle the rest.
deploy_app "$@"
