#!/usr/bin/env python

"""This contains all constants using in api test. """


"""URLs"""
USER_URL = '/users'
UPDATE_USER_URL = DELETE_USER_URL = GET_USER_URL = (
    '/users/{user_id}')
TOKEN_URL = '/tokens'
GET_TOKEN_URL = '/tokens/{token_id}'
DEVOPS_PROPS_URL = '/props'
DEVOPS_URL = '/idm/devops'
DOMAIN_URL = '/RAX-AUTH/domains'
CREDENTIALS_URL = "/users/{user_id}/OS-KSADM/credentials"
APIKEY_URL = CREDENTIALS_URL + "/RAX-KSKEY:apiKeyCredentials"
LIST_CREDENTIALS_URL = "/users/{user_id}/OS-KSADM/credentials"
ENDPOINT_TEMPLATE_URL = LIST_ENDPOINT_TEMPLATES_URL = (
    "/OS-KSCATALOG/endpointTemplates")
UPDATE_ENDPOINT_TEMPLATE_URL = GET_ENDPOINT_TEMPLATE_URL = (
    DELETE_ENDPOINT_TEMPLATE_URL) = (
    "/OS-KSCATALOG/endpointTemplates/{template_id}")
SERVICE_URL = "/OS-KSADM/services"
DELETE_SERVICE_URL = GET_SERVICE_URL = "/OS-KSADM/services/{service_id}"
TENANTS_URL = "/tenants"
ADD_ENDPOINT_TO_TENANT_URL = "/tenants/{tenant_id}/OS-KSCATALOG/endpoints"
DELETE_ENDPOINT_FROM_TENANT_URL = (
    "/tenants/{tenant_id}/OS-KSCATALOG/endpoints/{endpoint_template_id}")
LIST_ENDPOINTS_FOR_TOKEN_URL = "/tokens/{token_id}/endpoints"

ADD_TENANT_URL = LIST_TENANTS = '/tenants'
UPDATE_TENANT_URL = DELETE_TENANT_URL = GET_TENANT_URL = (
    '/tenants/{tenant_id}')
VALIDATE_TOKENS_URL = '/tokens/{0}'
LEGACY_FED_AUTH_URL = '/RAX-AUTH/saml-tokens'
NEW_FED_AUTH_URL = '/RAX-AUTH/federation/saml/auth'
ADMINS_OF_A_USER_URL = '/users/{user_id}/RAX-AUTH/admins'
FED_LOGOUT_URL = '/RAX-AUTH/federation/saml/logout'
ADD_ROLE_TO_USER_URL = '/users/{user_id}/roles/OS-KSADM/{role_id}'

"""Some Constanst for values"""
PASSWORD_PATTERN = "Password1[\d\w]{10}"
SUB_USER_PATTERN = "sub[\-]user[\d\w]{12}"
USER_NAME_PATTERN = "api[\-]test[\-][\d\w]{12}"
DOMAIN_API_TEST = "api-test"
DOMAIN_PATTERN = "[a-z]{8}"
API_KEY_PATTERN = "[a-f][0-9]{32}"
EMAIL_RANDOM = "randome@rackspace.com"
DOMAIN_TEST = "meow"
CONTENT_TYPE_VALUE = ACCEPT_ENCODING_VALUE = "application/{0}"
SERVICE_NAME_PATTERN = "service[\-][\w\d]{8}"
SERVICE_TYPE_PATTERN = "service[\-]type[\-][\w\d]{8}"
SERVICE_ID_PATTERN = "[\d]{8}"

"""Headers"""
X_AUTH_TOKEN = "X-Auth-Token"
X_USER_ID = "X-User-Id"

"""Some constants used for attributes"""
ACCESS = "access"
ADMINISTRATOR_ROLE = "administratorRole"
API_KEY = "apiKey"
ACCEPT = "Accept"
ACCEPT_ENCODING = "Accept-Encoding"
ASCII = "ascii"
ADMIN_URL = "adminURL"
ASSIGNMENT_TYPE = "assignmentType"
ASSIGNMENT_TYPE_MOSSO = "MOSSO"
ASSIGNMENT_TYPE_NAST = "NAST"
ASSIGNMENT_TYPE_MANUAL = "MANUAL"
AUTH = "auth"
CONTACTID = "contactId"
CONTENT_TYPE = "Content-Type"
DEFAULT = "default"
DEFAULT_REGION = "defaultRegion"
DESCRIPTION = "description"
DOMAIN = "domain"
DOMAINID = "domainId"
DISPLAY_NAME = "display-name"
EMAIL = "email"
ENABLED = "enabled"
ENDPOINTS = "endpoints"
ENDPOINT_TEMPLATE = "endpointTemplate"
GLOBAL = "global"
GROUPS = "groups"
ID = "id"
INTERNAL_URL = "internalURL"
NAME = "name"
NAST_PREFIX = "MossoCloudFS_"
PASSWORD = "password"
PROPAGATE = "propagate"
PROPERTIES = "properties"
PROP_VALUE = 'value'
PUBLIC_URL = "publicURL"
REGION = "region"
REQUIRED = "required"
CREATED = "created"
ROLE = "role"
ROLES = "roles"
ROLE_NAME = 'name'
SECRET_ANSWER = "answer"
SECRET_QA = "secretQA"
SECRET_QUESTION = "question"
SERVICE_CATALOG = "serviceCatalog"
SERVICE_ENDPOINTS = "endpoints"
SERVICE_ID = "serviceId"
SERVICE_NAME = "name"
SERVICE_TYPE = "type"
UPDATED = "updated"
USER = "user"
USERS = "users"
USERNAME = "username"
TENANT_ALIAS = "tenantAlias"
TOKEN = "token"
TOKEN_FORMAT = "tokenFormat"
TENANT = "tenant"
TENANTS = "tenants"

TYPE = "type"
VERSION_ID = "versionId"
VERSION_INFO = "versionInfo"
VERSION_LIST = "versionList"
XML = "xml"
JSON = "json"
CREDENTIALS = "credentials"
OS_KSADM_PASSWORD = 'OS-KSADM:password'

API_KEY_CREDENTIALS = "apiKeyCredentials"
OS_KSCATALOG_ENDPOINT_TEMPLATE = "OS-KSCATALOG:endpointTemplate"
OS_KSCATALOG_ENDPOINT_TEMPLATES = "OS-KSCATALOG:endpointTemplates"
FACTOR_TYPE = "factorType"
MULTI_FACTOR_ENABLED = "multiFactorEnabled"
MULTI_FACTOR_STATE = "multiFactorState"
OS_KSADM_NAMESPACE = "OS-KSADM"
PASSWORD_CREDENTIALS = "passwordCredentials"
RAX_KSGRP_NAMESPACE = "RAX-KSGRP"
NS_GROUPS = RAX_KSGRP_NAMESPACE + ":groups"
RAX_KSKEY_NAMESPACE = "RAX-KSKEY"
NS_PASSWORD = OS_KSADM_NAMESPACE + ":password"
RAX_KSQA_NAMESPACE = "RAX-KSQA"
NS_SECRETQA = RAX_KSQA_NAMESPACE + ":secretQA"
NS_API_KEY_CREDENTIALS = RAX_KSKEY_NAMESPACE + ":apiKeyCredentials"
RAX_AUTH = "RAX-AUTH"
RAX_AUTH_ASSIGNMENT_TYPE = "RAX-AUTH:assignmentType"
RAX_AUTH_DOMAIN = "RAX-AUTH:domain"
RAX_AUTH_DOMAIN_ID = "RAX-AUTH:domainId"
RAX_AUTH_CONTACTID = "RAX-AUTH:contactId"
RAX_AUTH_DEFAULT_REGION = "RAX-AUTH:defaultRegion"
RAX_AUTH_MULTI_FACTOR_ENABLED = "RAX-AUTH:multiFactorEnabled"
RAX_AUTH_MULTI_FACTOR_STATE = "RAX-AUTH:multiFactorState"
RAX_AUTH_USER_MULTI_FACTOR_ENFORCEMENT_LEVEL = (
    "RAX-AUTH:userMultiFactorEnforcementLevel")
RAX_AUTH_FACTOR_TYPE = "RAX-AUTH:factorType"
RAX_AUTH_ADMINISTRATOR_ROLE = "RAX-AUTH:administratorRole"
RAX_AUTH_PROPAGATE = "RAX-AUTH:propagate"
NS_SERVICE = OS_KSADM_NAMESPACE + ":service"
USER_MULTI_FACTOR_ENFORCEMENT_LEVEL = "userMultiFactorEnforcementLevel"
RAX_AUTH_PEDERATED_IDP = "RAX-AUTH:federatedIdp"

"""Some constants used for namespace"""
XMLNS = "http://docs.openstack.org/identity/api/v2.0"
XMLNS_OS_KSADM = "http://docs.openstack.org/identity/api/ext/OS-KSADM/v1.0"
XMLNS_RAX_AUTH = "http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0"
XMLNS_RAX_KSGRP = (
    "http://docs.rackspace.com/identity/api/ext/RAX-KSGRP/v1.0")
XMLNS_RAX_KSQA = "http://docs.rackspace.com/identity/api/ext/RAX-KSQA/v1.0"
XMLNS_RAX_KSKEY = (
    "http://docs.rackspace.com/identity/api/ext/RAX-KSKEY/v1.0")

"""CONSTANTS"""
USER_MANAGER_ROLE_ID = '7'
DC_LIST = ['DFW', 'SYD', 'IAD', 'HKG', 'LON', 'ORD']
RELOADABLE_PROP_FILE = 'idm.reloadable.properties'

"""FEATURE FLAGS"""
FEATURE_FLAG_FOR_DISABLING_SERVICE_NAME_TYPE = (
    "feature.endpoint.template.disable.name.type")
