#!/usr/bin/env bash

SANDBOX_NAME=$(echo "${SANDBOX_NAME}"| tr [:lower:] [:upper:] | tr '-' '_')

IDM_CONFIG_FILE=src/main/config/VAGRANT/idm.properties
CA_DIR_HOST_VAR="CA_DIRECTORY_${SANDBOX_NAME}_SERVICE_HOST"
CA_DIR_PORT_VAR="CA_DIRECTORY_${SANDBOX_NAME}_SERVICE_PORT"
ACTIVE_DIR_HOST_VAR="ACTIVE_DIRECTORY_${SANDBOX_NAME}_SERVICE_HOST"
ACTIVE_DIR_PORT_VAR="ACTIVE_DIRECTORY_${SANDBOX_NAME}_SERVICE_PORT"
DYNAMODB_HOST_VAR="DYNAMODB_${SANDBOX_NAME}_SERVICE_HOST"
DYNAMODB_PORT_VAR="DYNAMODB_${SANDBOX_NAME}_SERVICE_PORT"

env

sed -i -e "s|ldap.serverList=.*|ldap.serverList=${!CA_DIR_HOST_VAR}:${!CA_DIR_PORT_VAR}|g" ${IDM_CONFIG_FILE}
sed -i -e "s|racker.auth.ldap.server=.*|racker.auth.ldap.server=${!ACTIVE_DIR_HOST_VAR}|g" ${IDM_CONFIG_FILE}
sed -i -e "s|racker.auth.ldap.server.port=.*|racker.auth.ldap.server.port=${!ACTIVE_DIR_PORT_VAR}|g" ${IDM_CONFIG_FILE}
sed -i -e "s|dynamo.db.service.endpoint=.*|dynamo.db.service.endpoint=http://${!DYNAMODB_HOST_VAR}:${!DYNAMODB_PORT_VAR}|g" ${IDM_CONFIG_FILE}

cat ${IDM_CONFIG_FILE}

./gradlew clean build --profile
