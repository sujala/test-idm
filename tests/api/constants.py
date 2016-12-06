#!/usr/bin/env python

'''This contains all constants using in api test. '''

'''URLs'''
ADD_ROLE_TO_USER_FOR_TENANT_URL = DEL_ROLE_FROM_USER_FOR_TENANT_URL = (
    '/tenants/{tenant_id}/users/{user_id}/roles/OS-KSADM/{role_id}')
ADD_TENANT_TO_DOMAIN_URL = DELETE_TENANT_FROM_DOMAIN_URL = (
    '/RAX-AUTH/domains/{domain_id}/tenants/{tenant_id}')
GET_TENANTS_IN_DOMAIN_URL = '/RAX-AUTH/domains/{domain_id}/tenants'
USER_URL = '/users'
ROLES_URL = '/OS-KSADM/roles'
GROUPS_URL = '/RAX-GRPADM/groups'
UPGRADE_USER_TO_CLOUD_URL = '/users/RAX-AUTH/upgradeUserToCloud'
UPDATE_USER_URL = DELETE_USER_URL = GET_USER_URL = '/users/{user_id}'
TOKEN_URL = '/tokens'
GET_TOKEN_URL = '/tokens/{token_id}'
DEVOPS_PROPS_URL = '/props'
DEVOPS_URL = '/idm/devops'
DELETE_DOMAIN_URL = '/RAX-AUTH/domains/{domain_id}'
DOMAIN_URL = '/RAX-AUTH/domains'
CREDENTIALS_URL = "/users/{user_id}/OS-KSADM/credentials"
GET_USER_API_CRED_URL = UPDATE_USER_API_CRED_URL = DELETE_USER_API_CRED_URL = (
    "/users/{user_id}/OS-KSADM/credentials/RAX-KSKEY:apiKeyCredentials")
LIST_CREDENTIALS_URL = "/users/{user_id}/OS-KSADM/credentials"
ENDPOINT_TEMPLATE_URL = LIST_ENDPOINT_TEMPLATES_URL = (
    '/OS-KSCATALOG/endpointTemplates')
UPDATE_ENDPOINT_TEMPLATE_URL = GET_ENDPOINT_TEMPLATE_URL = (
    DELETE_ENDPOINT_TEMPLATE_URL) = (
    '/OS-KSCATALOG/endpointTemplates/{template_id}')
SERVICE_URL = '/OS-KSADM/services'
DELETE_SERVICE_URL = GET_SERVICE_URL = '/OS-KSADM/services/{service_id}'
TENANTS_URL = '/tenants'
ADD_ENDPOINT_TO_TENANT_URL = '/tenants/{tenant_id}/OS-KSCATALOG/endpoints'
TENANT_TYPE_TO_ENDPOINT_MAPPING_RULES_URL = (
    ENDPOINT_TEMPLATE_URL + '/RAX-AUTH/rules')
GET_TENANT_TYPE_TO_ENDPOINT_MAPPING_RULE_URL = (
    ENDPOINT_TEMPLATE_URL + '/RAX-AUTH/rules/{rule_id}')
DELETE_TENANT_TYPE_TO_ENDPOINT_MAPPING_RULE_URL = (
    GET_TENANT_TYPE_TO_ENDPOINT_MAPPING_RULE_URL)
DELETE_ENDPOINT_FROM_TENANT_URL = (
    '/tenants/{tenant_id}/OS-KSCATALOG/endpoints/{endpoint_template_id}')
LIST_ENDPOINTS_FOR_TOKEN_URL = '/tokens/{token_id}/endpoints'
ADD_TENANT_URL = LIST_TENANTS = '/tenants'
LIST_ROLES_FOR_USER_ON_TENANT_URL = (
    '/tenants/{tenant_id}/users/{user_id}/roles')
LIST_TENANTS_IN_DOMAIN_URL = '/RAX-AUTH/domains/{domainId}/tenants'
LIST_USERS_FOR_TENANT_URL = '/tenants/{tenant_id}/users'
LIST_USERS_IN_DOMAIN_URL = '/RAX-AUTH/domains/{domain_id}/users'
UPDATE_TENANT_URL = DELETE_TENANT_URL = GET_TENANT_URL = (
    '/tenants/{tenant_id}')

VALIDATE_TOKENS_URL = '/tokens/{0}'
LEGACY_FED_AUTH_URL = '/RAX-AUTH/saml-tokens'
NEW_FED_AUTH_URL = '/RAX-AUTH/federation/saml/auth'
ADMINS_OF_A_USER_URL = '/users/{user_id}/RAX-AUTH/admins'
FED_LOGOUT_URL = '/RAX-AUTH/federation/saml/logout'
ADD_ROLE_TO_USER_URL = '/users/{user_id}/roles/OS-KSADM/{role_id}'

DELETE_ROLE_FR_USER_URL = ADD_ROLE_TO_USER_URL
ROLE_URL = '/OS-KSADM/roles'
GET_ROLE_URL = DELETE_ROLE_URL = '/OS-KSADM/roles/{role_id}'
LIST_USER_ROLES_URL = '/users/{user_id}/roles'
GET_USERS_FOR_ROLE_URL = '/OS-KSADM/roles/{role_id}/RAX-AUTH/users'
RESET_USER_API_KEY_URL = ('/users/{user_id}/OS-KSADM/credentials/'
                          'RAX-KSKEY:apiKeyCredentials/RAX-AUTH/reset')
ADD_OTP_DEVICE_URL = '/users/{user_id}/RAX-AUTH/multi-factor/otp-devices'
DELETE_OTP_DEVICE_URL = (
    '/users/{user_id}/RAX-AUTH/multi-factor/otp-devices/{device_id}')
UPDATE_MFA_URL = '/users/{user_id}/RAX-AUTH/multi-factor'
VERIFY_OTP_DEVICE_URL = (
    '/users/{user_id}/RAX-AUTH/multi-factor/otp-devices/{device_id}/verify')
PATH_TO_REPOSE_LOG = '/var/log/repose/current.log'
UNBOUNDID_CONFIG_URL = '/idm/devops/props'

'''Some Constanst for values'''
API_KEY_PATTERN = '[a-f][0-9]{32}'
CONTACT_ID_MIN = 10000000
CONTACT_ID_MAX = 99999999
CONTENT_TYPE_VALUE = ACCEPT_ENCODING_VALUE = 'application/{0}'
DOMAIN_API_TEST = 'api-test'
DOMAIN_PATTERN = '[a-z]{8}'
DOMAIN_TEST = 'meow'
EMAIL_RANDOM = 'randome@rackspace.com'
ID_PATTERN = '[\d]{8}'
IDENTITY_PRODUCT_ROLE_NAME_PATTERN = 'identity:testrole[\-][\d\w]{8}'
LOWER_CASE_LETTERS = '[a-z]{8}'
MD5_PATTERN = '[a-f][0-9]{40}'
MIXED_CASE_LETTERS = '[A-Z][a-z]{8}'
MOSSO_TENANT_ID_PATTERN = '[\d]{6}'
NAST_TENANT_ID_PATTERN = 'NAST[\d]{6}'
NUMBERS_PATTERN = '[1-9]{1}[0-9]{8}'
OTP_NAME_PATTERN = 'otp[\-][\d]{8}'
PASSWORD_PATTERN = 'Password1[\d\w]{10}'
ROLE_NAME_PATTERN = 'cid_test_role[\-][\d]{8}'
SERVICE_NAME_PATTERN = 'service[\-][\w\d]{8}'
SERVICE_TYPE_PATTERN = 'service[\-]type[\-][\w\d]{8}'
SERVICE_ID_PATTERN = '[\d]{8}'
SUB_USER_PATTERN = 'sub[\-]user[\d\w]{12}'
USER_ADMIN_PATTERN = 'uadm_[\d\w]{12}'
TENANT_ID_PATTERN = '[\d]{8}'
TENANT_NAME_PATTERN = 'api[\-]test[\-]tenant[\-][\d\w]{8}'
NUMERIC_DOMAIN_ID_PATTERN = '[1-9]{1}[0-9]{6}'
UPPER_CASE_LETTERS = '[A-Z]{8}'
USER_NAME_PATTERN = 'api[\-]test[\-][\d\w]{12}'
MAPPING_RULE_DESCRIPTION_PATTERN = (
    'mapping[\-]rule[\-]description[\-][\w\d]{8}')
TENANT_TYPE_PATTERN = 'ttype[a-z0-9]{10}'
USER_MANAGER_NAME_PATTERN = 'user[\-]manager[\d\w]{12}'

'''Headers'''
ACCESS_CONTROL_REQUEST_HEADERS = 'access-control-request-headers'
ACCESS_CONTROL_REQUEST_METHOD = 'access-control-request-method'
ACCESS_CONTROL_ALLOW_ORIGIN = 'Access-Control-Allow-Origin'
ACCESS_CONTROL_ALLOW_CREDENTIALS = 'access-control-allow-credentials'
ACCESS_CONTROL_ALLOW_METHODS = 'Access-Control-Allow-Methods'
ACCESS_CONTROL_ALLOW_HEADERS = 'access-control-allow-headers'
ACCESS_CONTROL_EXPOSE_HEADERS = 'access-control-expose-headers'
ORIGIN = 'origin'
X_AUTH_TOKEN = 'X-Auth-Token'
X_DOMAIN_ID = 'X-Domain_Id'
X_TENANT_ID = 'X-Tenant_Id'
X_USER_ID = 'X-User-Id'
X_SESSION_ID = 'X-SessionId'
WWW_AUTHENTICATE = 'www-authenticate'

'''Some constants used for attributes'''
ACCESS = 'access'
ADMINISTRATOR_ROLE = 'administratorRole'
API_KEY = 'apiKey'
ACCEPT = 'Accept'
ACCEPT_ENCODING = 'Accept-Encoding'
ASCII = 'ascii'
ADMIN_URL = 'adminURL'
ADDRESS = 'address'
ACTION = 'action'
AGENT = 'agent'
ASSIGNMENT_TYPE = 'assignmentType'
ASSIGNMENT_TYPE_MOSSO = 'MOSSO'
ASSIGNMENT_TYPE_NAST = 'NAST'
ASSIGNMENT_TYPE_MANUAL = 'MANUAL'
AUTH = 'auth'
RAX_AUTH_AUTHENTICATED_BY = 'RAX-AUTH:authenticatedBy'
BAD_REQUEST = 'badRequest'
CREDENTIALS = 'credentials'
ATTACHMENT = 'attachment'
ATTACHMENTS = 'attachments'
AUDIT_DATA = 'auditData'
CODE = 'code'
CONFIG_PATH = 'configPath'
CONTACTID = 'contactId'
CONTENT_TYPE = 'Content-Type'
CREATED = 'created'
DATA_CENTER = 'dataCenter'
DEFAULT = 'default'
DEFAULT_REGION = 'defaultRegion'
DESCRIPTION = 'description'
DEFAULT_VALUE = 'defaultValue'
DISPLAY_NAME = 'display-name'
DOMAIN = 'domain'
DOMAINID = 'domainId'
EMAIL = 'email'
ENABLED = 'enabled'
ENDPOINTS = 'endpoints'
ENDPOINT_TEMPLATE = 'endpointTemplate'
EXPIRES = 'expires'
EVENT_TIME = 'eventTime'
EVENT_TYPE = 'eventType'
FACTOR_TYPE = 'factorType'
GLOBAL = 'global'
GROUP = 'group'
GROUPS = 'groups'
ID = 'id'
INITIATOR = 'initiator'
ITEMS = 'items'
KEY_URI = 'keyUri'
METHOD_LABEL = 'methodLabel'
INTERNAL_URL = 'internalURL'
MESSAGE = 'message'
NAME = 'name'
NAST_PREFIX = 'MossoCloudFS_'
OBSERVER = 'observer'
OUTCOME = 'outcome'
QR_CODE = 'qrcode'
QUERY_STRING = 'queryString'
PASSCODE = 'passcode'
PASSWORD = 'password'
PROPAGATE = 'propagate'
PROPERTIES = 'properties'
PROP_VALUE = 'value'
PUBLIC_URL = 'publicURL'
REGION = 'region'
REQUEST_URL = 'requestURL'
REQUIRED = 'required'
REASON = 'reason'
REASON_CODE = 'reasonCode'
RESPONSE_MESSAGE = 'responseMessage'
ROLE = 'role'
ROLES = 'roles'
ROLE_NAME = 'name'
SECRET_ANSWER = 'answer'
SECRET_QA = 'secretQA'
SECRET_QUESTION = 'question'
SERVICE = 'service'
SERVICE_CATALOG = 'serviceCatalog'
SERVICE_ENDPOINTS = 'endpoints'
SERVICE_ID = 'serviceId'
SERVICE_NAME = 'name'
SERVICE_TYPE = 'type'
TARGET = 'target'
TENANT = 'tenant'
TENANT_ALIAS = 'tenantAlias'
TENANTS = 'tenants'
TENANT_ID = 'tenantId'
TENANT_NAME = 'tenantName'
TOKEN = 'token'
TOKEN_FORMAT = 'tokenFormat'
TYPE = 'type'
TYPE_URI = 'typeURI'
UPDATED = 'updated'
USER = 'user'
USERS = 'users'
USERNAME = 'username'
USER_NAME = 'userName'
UNLOCK = 'unlock'
VALUE = 'value'
VERIFIED = 'verified'
VERSION_ADDED = 'versionAdded'
VERSION_ID = 'versionId'
VERSION_INFO = 'versionInfo'
VERSION_LIST = 'versionList'
XML = 'xml'
JSON = 'json'
OS_KSADM_PASSWORD = 'OS-KSADM:password'
SECRETQA = 'secretQA'

API_KEY_CREDENTIALS = 'apiKeyCredentials'
CADF_ATTACHMENTS = 'cadf:attachments'
CADF_ATTACHMENT = 'cadf:attachment'
CADF_CONTENT = 'cadf:content'
CADF_INITIATOR = 'cadf:initiator'
CADF_HOST = 'cadf:host'
CADF_REASON = 'cadf:reason'
ENDPOINT_ASSIGNMENT_RULES = 'endpointAssignmentRules'
RAX_AUTH_ENDPOINT_ASSIGNMENT_RULES = 'RAX-AUTH:endpointAssignmentRules'
OS_KSCATALOG_ENDPOINT_TEMPLATE = 'OS-KSCATALOG:endpointTemplate'
OS_KSCATALOG_ENDPOINT_TEMPLATES = 'OS-KSCATALOG:endpointTemplates'
MULTI_FACTOR_ENABLED = 'multiFactorEnabled'
MULTI_FACTOR_STATE = 'multiFactorState'
OS_KSADM_NAMESPACE = 'OS-KSADM'
PASSWORD_CREDENTIALS = 'passwordCredentials'
NS_FEDERATED_IDP = 'RAX-AUTH:federatedIdp'
NS_GROUP = 'RAX-KSGRP:group'
NS_GROUPS = 'RAX-KSGRP:groups'
RAX_KSKEY_NAMESPACE = 'RAX-KSKEY'
RAX_KSGRP_NAMESPACE = 'RAX-KSGRP'
RAX_GRPADMN_NAMESPACE = 'RAX-GRPADM'
NS_PASSWORD = OS_KSADM_NAMESPACE + ':password'
RAX_KSQA_NAMESPACE = 'RAX-KSQA'
NS_SECRETQA = 'RAX-KSQA:secretQA'
NS_API_KEY_CREDENTIALS = 'RAX-KSKEY:apiKeyCredentials'
RAX_AUTH = 'RAX-AUTH'
NS_TYPES = 'RAX-AUTH:types'
RAX_AUTH_ASSIGNMENT_TYPE = 'RAX-AUTH:assignmentType'
RAX_AUTH_DOMAIN = 'RAX-AUTH:domain'
RAX_AUTH_DOMAIN_ID = 'RAX-AUTH:domainId'
NS_ADMINISTRATOR_ROLE = 'RAX-AUTH:administratorRole'
NS_PROPAGATE = RAX_AUTH + ':' + PROPAGATE
RAX_AUTH_CONTACTID = 'RAX-AUTH:contactId'
RAX_AUTH_DEFAULT_REGION = 'RAX-AUTH:defaultRegion'
RAX_AUTH_MULTI_FACTOR = 'RAX-AUTH:multiFactor'
RAX_AUTH_MULTI_FACTOR_ENABLED = 'RAX-AUTH:multiFactorEnabled'
RAX_AUTH_MULTI_FACTOR_STATE = 'RAX-AUTH:multiFactorState'
RAX_AUTH_USER_MULTI_FACTOR_ENFORCEMENT_LEVEL = (
    'RAX-AUTH:userMultiFactorEnforcementLevel')
RAX_AUTH_FACTOR_TYPE = 'RAX-AUTH:factorType'
RAX_AUTH_ADMINISTRATOR_ROLE = 'RAX-AUTH:administratorRole'
RAX_AUTH_PROPAGATE = 'RAX-AUTH:propagate'
NS_SERVICE = OS_KSADM_NAMESPACE + ':service'
NS_TENANT_TYPE_TO_ENDPOINT_MAPPING_RULE = (
    RAX_AUTH + ':tenantTypeEndpointRule')
TENANT_TYPE = 'tenantType'
TENANT_TYPE_TO_ENDPOINT_MAPPING_RULE = 'tenantTypeEndpointRule'
TENANT_TYPE_TO_ENDPOINT_MAPPING_RULES = 'tenantTypeEndpointRules'
NS_SERVICES = OS_KSADM_NAMESPACE + ':services'

'''Query parameters'''
PARAM_ROLE_NAME = 'roleName'
RESPONSE_DETAIL = 'responseDetail'
RAX_AUTH_PEDERATED_IDP = 'RAX-AUTH:federatedIdp'
NS_PASSCODE_CREDENTIALS = RAX_AUTH + ':passcodeCredentials'
NS_OTP_DEVICE = RAX_AUTH + ':otpDevice'
NS_VERIFICATION_CODE = RAX_AUTH + ':verificationCode'
USER_MULTI_FACTOR_ENFORCEMENT_LEVEL = 'userMultiFactorEnforcementLevel'
UA_AUDIT_DATA = 'ua:auditData'
UA_USER_NAME = 'ua:userName'
UA_REQUEST_URL = 'ua:requestURL'

'''Some constants used for namespace'''
XMLNS = 'http://docs.openstack.org/identity/api/v2.0'
XMLNS_OS_KSADM = 'http://docs.openstack.org/identity/api/ext/OS-KSADM/v1.0'
XMLNS_RAX_AUTH = 'http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0'
XMLNS_RAX_KSGRP = (
    'http://docs.rackspace.com/identity/api/ext/RAX-KSGRP/v1.0')
XMLNS_RAX_KSQA = 'http://docs.rackspace.com/identity/api/ext/RAX-KSQA/v1.0'
XMLNS_RAX_KSKEY = (
    'http://docs.rackspace.com/identity/api/ext/RAX-KSKEY/v1.0')
CADF = 'http://schemas.dmtf.org/cloud/audit/1.0/event'
UA = 'http://feeds.api.rackspacecloud.com/cadf/user-access-event'

'''CONSTANTS'''
AUTH_BY_LIST = ['APIKEY']
DC_LIST = ['DFW', 'SYD', 'IAD', 'HKG', 'LON', 'ORD']
LIST_ENDPOINT_NAMES_FOR_MOSSO_TENANT = ['cloudMonitoring',
                                        'cloudLoadBalancers',
                                        'cloudBlockStoragePreprod',
                                        'cloudDatabases', 'cloudDNS',
                                        'cloudServers', 'cloudServersPreprod']
LIST_ENDPOINT_NAMES_FOR_NAST_TENANT = ['cloudFiles', 'cloudFilesCDN']

'''PROPERTIES'''
IDM_RELOADABLE_PROPERTIES = 'idm.reloadable.properties'
IDM_PROPERTIES = 'idm.properties'
EXPECTED_UNBOUNDID_TIMEOUT_CONFIGS = [
    'ldap.server.pool.age.max',
    'ldap.server.pool.create.if.necessary',
    'ldap.server.pool.max.wait.time',
    'ldap.server.pool.health.check.interval',
    'ldap.server.pool.check.connection.age.on.release',
    'ldap.server.pool.allow.concurrent.socketfactory.use']

'''ROLES'''
ENDPOINT_RULE_ADMIN_ROLE_NAME = 'identity:endpoint-rule-admin'
IDENTITY_ADMIN_ROLE_ID = '1'
OBJECT_STORE_ROLE_NANE = 'object-store:default'
SERVICE_ADMIN_ROLE_ID = '4'
TENANT_ACCESS_ROLE_NAME = 'identity:tenant-access'
USER_ADMIN_ROLE_ID = '2'
USER_DEFAULT_ROLE_ID = '3'
USER_MANAGER_ROLE_ID = '7'

'''FEATURE FLAGS'''
FEATURE_FLAG_FOR_ENDPOINTS_BASED_ON_RULES = (
    'feature.include.endpoints.based.on.rules')
AUTO_ASSIGN_ROLE_ON_DOMAIN_TENANTS_ROLE_NAME = (
    'auto.assign.role.on.domain.tenants.role.name')
FEATURE_AUTO_ASSIGN_ROLE_ON_DOMAIN_TENANTS = (
    'feature.auto.assign.role.on.domain.tenants')
FEATURE_FLAG_FOR_DISABLING_SERVICE_NAME_TYPE = (
    "feature.endpoint.template.disable.name.type")
FEATURE_FLAG_ALLOW_TENANT_NAME_UPDATE = (
    'feature.allow.tenant.name.to.be.changed.via.update.tenant')
FEATURE_GLOBAL_ENDPOINTS_FOR_ALL_ROLES_ENABLED = (
    'feature.global.endpoints.for.all.roles.enabled')
FEATURE_RESTRICT_USER_MANAGER_LIST_USERS_USAGE = (
    'feature.restrict.user.manager.list.users.usage')
FEATURE_RESTRICT_USER_MANAGER_LIST_USERS_BY_EMAIL_USAGE = (
    'feature.restrict.user.manager.list.users.by.email.usage')
FEATURE_RESTRICT_USER_MANAGER_LIST_USERS_BY_NAME_USAGE = (
    'feature.restrict.user.manager.list.users.by.name.usage')
TENANT_DEFAULT_DOMAIN = 'tenant.domainId.default'
