#!/usr/bin/env python

"""
This contains all constants using in api test
"""

"""Some contants used for test in general"""
USER_URL = '/users'
UPDATE_USER_URL = DELETE_USER_URL = GET_USER_URL = (
    '/users/{user_id}')
TOKEN_URL = '/tokens'
GET_TOKEN_URL = '/tokens/{token_id}'
DOMAIN_URL = '/RAX-AUTH/domains'
LIST_CREDENTIALS_URL = "/users/{user_id}/OS-KSADM/credentials"

"""Some Constanst for values"""
PASSWORD_PATTERN = "Password1[\d\w]{10}"
SUB_USER_PATTERN = "sub[\-]user[\d\w]{12}"
USER_NAME_PATTERN = "api[\-]test[\-][\d\w]{12}"
DOMAIN_API_TEST = "api-test"
EMAIL_RANDOM = "randome@rackspace.coom"
DOMAIN_TEST = "meow"
CONTENT_TYPE_VALUE = ACCEPT_ENCODING_VALUE = "application/{0}"

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
AUTH = "auth"
CONTACTID = "contactId"
CONTENT_TYPE = "Content-Type"
CREATED = "created"
UPDATED = "updated"
DOMAIN = "domain"
DOMAINID = "domainId"
DISPLAY_NAME = "display_name"
EMAIL = "email"
ENABLED = "enabled"
ENDPOINTS = "endpoints"
DEFAULT_REGION = "defaultRegion"
DESCRIPTION = "description"
ID = "id"
NAME = "name"
PASSWORD = "password"
PROPAGATE = "propagate"
PROPERTIES = "properties"
REQUIRED = "required"
ROLE = "role"
ROLES = "roles"
SERVICE_CATALOG = "serviceCatalog"
SERVICE_ID = "serviceId"
USER = "user"
USERS = "users"
USERNAME = "username"
TOKEN = "token"
TOKEN_FORMAT = "tokenFormat"
TYPE = "type"
XML = "xml"
JSON = "json"

API_KEY_CREDENTIALS = "apiKeyCredentials"
PASSWORD_CREDENTIALS = "passwordCredentials"
MULTI_FACTOR_ENABLED = "multiFactorEnabled"
MULTI_FACTOR_STATE = "multiFactorState"
FACTOR_TYPE = "factorType"
USER_MULTI_FACTOR_ENFORCEMENT_LEVEL = "userMultiFactorEnforcementLevel"
OS_KSADM_NAMESPACE = "OS-KSADM"
RAX_KSKEY_NAMESPACE = "RAX-KSKEY"
NS_PASSWORD = OS_KSADM_NAMESPACE + ":password"
NS_API_KEY_CREDENTIALS = RAX_KSKEY_NAMESPACE + ":apiKeyCredentials"
RAX_AUTH = "RAX-AUTH"
RAX_AUTH_DOMAIN = "RAX-AUTH:domainId"
RAX_AUTH_CONTACTID = "RAX-AUTH:contactId"
RAX_AUTH_DEFAULT_REGION = "RAX-AUTH:defaultRegion"
RAX_AUTH_MULTI_FACTOR_ENABLED = "RAX-AUTH:multiFactorEnabled"
RAX_AUTH_MULTI_FACTOR_STATE = "RAX-AUTH:multiFactorState"
RAX_AUTH_USER_MULTI_FACTOR_ENFORCEMENT_LEVEL = (
    "RAX-AUTH:userMultiFactorEnforcementLevel")
RAX_AUTH_FACTOR_TYPE = "RAX-AUTH:factorType"
RAX_AUTH_ADMINISTRATOR_ROLE = "RAX-AUTH:administratorRole"
RAX_AUTH_PROPAGATE = "RAX-AUTH:propagate"

"""Some constants used for namespace"""
XMLNS = "http://docs.openstack.org/identity/api/v2.0"
XMLNS_OS_KSADM = "http://docs.openstack.org/identity/api/ext/OS-KSADM/v1.0"
XMLNS_RAX_AUTH = "http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0"
XMLNS_RAX_KSGRP = (
    "http://docs.rackspace.com/identity/api/ext/RAX-KSGRP/v1.0")
XMLNS_RAX_KSQA = "http://docs.rackspace.com/identity/api/ext/RAX-KSQA/v1.0"
XMLNS_RAX_KSKEY = (
    "http://docs.rackspace.com/identity/api/ext/RAX-KSKEY/v1.0")
