#!/usr/bin/env python

'''This contains all constants using in api test. '''
'''V1.1 URLs'''
V11_AUTH_URL = '/auth'
V11_AUTH_ADMIN_URL = '/auth-admin'
V11_TOKEN_VALIDATION_URL = '/token/{token_id}'
V11_USER_URL = '/users'
V11_GET_USER_URL = V11_DELETE_USER_URL = (V11_USER_URL + '/{user_id}')

'''URLs'''
ADD_ENDPOINT_TO_TENANT_URL = '/tenants/{tenant_id}/OS-KSCATALOG/endpoints'
ADD_MULTIPLE_ROLES_TO_USER_URL = '/users/{user_id}/RAX-AUTH/roles'
ADD_OTP_DEVICE_URL = '/users/{user_id}/RAX-AUTH/multi-factor/otp-devices'
ADD_ROLE_TO_USER_FOR_TENANT_URL = DEL_ROLE_FROM_USER_FOR_TENANT_URL = (
    '/tenants/{tenant_id}/users/{user_id}/roles/OS-KSADM/{role_id}')
ADD_ROLE_TO_USER_URL = '/users/{user_id}/roles/OS-KSADM/{role_id}'
ADD_TENANT_ROLE_ASSIGNMENTS_TO_DELEGATION_AGREEMENT_URL = (
    '/RAX-AUTH/delegation-agreements/{da_id}/roles')
ADD_TENANT_ROLE_ASSIGNMENTS_TO_USER_GROUP_URL = (
    '/RAX-AUTH/domains/{domain_id}/groups/{group_id}/roles')
ADD_ROLE_TO_USER_GROUP_ON_TENANT_URL = (
    ADD_TENANT_ROLE_ASSIGNMENTS_TO_USER_GROUP_URL +
    '/{role_id}/tenants/{tenant_id}')
ADD_TENANT_TO_DOMAIN_URL = DELETE_TENANT_FROM_DOMAIN_URL = (
    '/RAX-AUTH/domains/{domain_id}/tenants/{tenant_id}')
ADD_TENANT_URL = LIST_TENANTS = '/tenants'
ADD_USER_DELEGATE_TO_DELEGATION_AGREEMENT_URL = (
    '/RAX-AUTH/delegation-agreements/{da_id}/delegates/users/{user_id}')
ADD_USER_GROUP_DELEGATE_TO_DELEGATION_AGREEMENT_URL = (
    '/RAX-AUTH/delegation-agreements/{da_id}/delegates/groups/{user_group_id}')
ADD_USER_GROUP_TO_DOMAIN_URL = '/RAX-AUTH/domains/{domain_id}/groups'
ADD_USER_TO_USER_GROUP_URL = (
    '/RAX-AUTH/domains/{domain_id}/groups/{group_id}/users/{user_id}')
UPDATE_USER_GROUP_URL = '/RAX-AUTH/domains/{domain_id}/groups/{group_id}'
ANALYZE_TOKEN_URL = '/tokens/analyze'
TENANT_TYPE_URL = '/RAX-AUTH/tenant-types'
RD_TENANT_TYPE_URL = '/RAX-AUTH/tenant-types/{name}'
ADMINS_OF_A_USER_URL = '/users/{user_id}/RAX-AUTH/admins'
CERTIFICATE_ADD_URL = ("/RAX-AUTH/federation/identity-providers/{idp_id}/"
                       "certificates")
CERTIFICATE_DELETE_URL = ("/RAX-AUTH/federation/identity-providers/{idp_id}/"
                          "certificates/{cert_id}")
CREDENTIALS_URL = "/users/{user_id}/OS-KSADM/credentials"
CHANGE_PASSWORD_URL = '/users/RAX-AUTH/change-pwd'
CREATE_UNVERIFIED_USER_URL = "/RAX-AUTH/invite/user"
DELETE_DOMAIN_URL = '/RAX-AUTH/domains/{domain_id}'
DELEGATION_AGREEMENTS_URL = '/RAX-AUTH/delegation-agreements'
DELEGATION_AGREEMENTS_RD_URL = '/RAX-AUTH/delegation-agreements/{da_id}'
DA_DELEGATES_URL = '/RAX-AUTH/delegation-agreements/{da_id}/delegates'
GET_DOMAIN_URL = DELETE_DOMAIN_URL
DELETE_ENDPOINT_FROM_TENANT_URL = (
    '/tenants/{tenant_id}/OS-KSCATALOG/endpoints/{endpoint_template_id}')
DELETE_OTP_DEVICE_URL = (
    '/users/{user_id}/RAX-AUTH/multi-factor/otp-devices/{device_id}')
DELETE_ROLE_FR_USER_URL = ADD_ROLE_TO_USER_URL
DELETE_ROLE_FROM_USER_GROUP_ON_TENANT_URL = (
    ADD_ROLE_TO_USER_GROUP_ON_TENANT_URL)
DELETE_ROLE_ON_DELEGATION_AGREEMENT_URL = (
    ADD_TENANT_ROLE_ASSIGNMENTS_TO_DELEGATION_AGREEMENT_URL + '/{role_id}')
DELETE_SERVICE_URL = GET_SERVICE_URL = '/OS-KSADM/services/{service_id}'
DELETE_TENANT_ROLE_ASSIGNMENTS_FROM_USER_GROUP_URL = (
    ADD_TENANT_ROLE_ASSIGNMENTS_TO_USER_GROUP_URL + '/{role_id}')
DELETE_TENANT_TYPE_TO_ENDPOINT_MAPPING_RULE_URL = (
    '/OS-KSCATALOG/endpointTemplates/RAX-AUTH/rules/{rule_id}')
DELETE_USER_DELEGATE_FROM_DELEGATION_AGREEMENT_URL = (
    ADD_USER_DELEGATE_TO_DELEGATION_AGREEMENT_URL)
DELETE_USER_GROUP_DELEGATE_FROM_DELEGATION_AGREEMENT_URL = (
    ADD_USER_GROUP_DELEGATE_TO_DELEGATION_AGREEMENT_URL)
DELETE_USER_FROM_USER_GROUP_URL = ADD_USER_TO_USER_GROUP_URL
DEVOPS_PROPS_URL = '/props'
DEVOPS_URL = '/devops'
DOMAIN_URL = '/RAX-AUTH/domains'
ENDPOINT_TEMPLATE_URL = LIST_ENDPOINT_TEMPLATES_URL = (
    '/OS-KSCATALOG/endpointTemplates')
FED_LOGOUT_URL = '/RAX-AUTH/federation/saml/logout'
FED_VALIDATE_LOGOUT_URL = '/RAX-AUTH/federation/saml/validate'
GET_ROLE_URL = DELETE_ROLE_URL = '/OS-KSADM/roles/{role_id}'
GET_TENANTS_IN_DOMAIN_URL = '/RAX-AUTH/domains/{domain_id}/tenants'
GET_TOKEN_URL = DELETE_TOKEN_URL = '/tokens/{token_id}'
GET_USER_API_CRED_URL = UPDATE_USER_API_CRED_URL = DELETE_USER_API_CRED_URL = (
    "/users/{user_id}/OS-KSADM/credentials/RAX-KSKEY:apiKeyCredentials")
GET_USERS_FOR_ROLE_URL = '/OS-KSADM/roles/{role_id}/RAX-AUTH/users'
GET_TENANT_TYPE_TO_ENDPOINT_MAPPING_RULE_URL = (
    DELETE_TENANT_TYPE_TO_ENDPOINT_MAPPING_RULE_URL)
GROUPS_URL = '/RAX-GRPADM/groups'
GET_GROUP_URL = '/RAX-GRPADM/groups/{group_id}'
UPDATE_GROUP_URL = '/RAX-GRPADM/groups/{group_id}'
ADD_USER_TO_GROUP_URL = '/RAX-GRPADM/groups/{group_id}/users/{user_id}'
GET_USERS_IN_GROUP_URL = '/RAX-GRPADM/groups/{group_id}/users'
REMOVE_USER_FROM_GROUP_URL = '/RAX-GRPADM/groups/{group_id}/users/{user_id}'
IDM_URL = '/idm'
IDP_URL = '/RAX-AUTH/federation/identity-providers'
IDP_RUD_URL = (IDP_URL + "/{idp_id}")
IDP_MAPPING_CR_URL = ("/RAX-AUTH/federation/identity-providers/{idp_id}/"
                      "mapping")
IDP_METADATA_RUD_URL = ("/RAX-AUTH/federation/identity-providers/{idp_id}/"
                        "metadata")
IMPERSONATION_URL = '/RAX-AUTH/impersonation-tokens'
INVITE = 'invite'
INVITE_UNVERIFIED_USER_URL = "/RAX-AUTH/invite/user/{user_id}/send"
UNVERIFIED_USER_URL = "/RAX-AUTH/invite/user/{user_id}"
UNVERIFIED_USER_ACCEPT_INVITE_URL = "/RAX-AUTH/invite/user/{user_id}/accept"
LEGACY_FED_AUTH_URL = '/RAX-AUTH/saml-tokens'
LIST_CREDENTIALS_URL = "/users/{user_id}/OS-KSADM/credentials"
LIST_DELEGATION_AGREEMENTS_URL = DELEGATION_AGREEMENTS_URL
LIST_ENDPOINTS_FOR_TOKEN_URL = '/tokens/{token_id}/endpoints'
LIST_GROUPS_URL = '/users/{user_id}/RAX-KSGRP'
LIST_EFFECTIVE_ROLES_FOR_USER_URL = '/users/{user_id}/RAX-AUTH/roles'
LIST_ROLES_FOR_USER_ON_TENANT_URL = (
    '/tenants/{tenant_id}/users/{user_id}/roles')
LIST_TENANTS_IN_DOMAIN_URL = '/RAX-AUTH/domains/{domainId}/tenants'
LIST_USERS_IN_DOMAIN_URL = '/RAX-AUTH/domains/{domainId}/users'
LIST_TENANT_ROLE_ASSIGNMENTS_FOR_DELEGATION_AGREEMENT_URL = (
    ADD_TENANT_ROLE_ASSIGNMENTS_TO_DELEGATION_AGREEMENT_URL)
LIST_TENANT_ROLE_ASSIGNMENTS_FOR_USER_GROUP_URL = (
    ADD_TENANT_ROLE_ASSIGNMENTS_TO_USER_GROUP_URL)
LIST_USER_ROLES_URL = '/users/{user_id}/roles'
LIST_USERS_FOR_TENANT_URL = '/tenants/{tenant_id}/users'
LIST_USER_GROUPS_FOR_DOMAIN_URL = ADD_USER_GROUP_TO_DOMAIN_URL
LIST_USERS_IN_USER_GROUP_FOR_DOMAIN_URL = (
    LIST_USER_GROUPS_FOR_DOMAIN_URL + '/{group_id}/users')
LIST_USERS_IN_DOMAIN_URL = '/RAX-AUTH/domains/{domain_id}/users'
MOVE_DOMAIN_TO_RCN_URL = '/RAX-AUTH/domains/{domain_id}/rcn/{rcn}'
NEW_FED_AUTH_URL = '/RAX-AUTH/federation/saml/auth'
PASSWORD_POLICY_URL = '/RAX-AUTH/domains/{domain_id}/password-policy'
ADMIN_CHANGE_URL = '/RAX-AUTH/domains/{domain_id}/domainAdministratorChange'
RESET_USER_API_KEY_URL = ('/users/{user_id}/OS-KSADM/credentials/'
                          'RAX-KSKEY:apiKeyCredentials/RAX-AUTH/reset')
RD_USER_GROUP_TO_DOMAIN_URL = ADD_USER_GROUP_TO_DOMAIN_URL + '/{group_id}'
ROLE_URL = '/OS-KSADM/roles'
ROLES_URL = '/OS-KSADM/roles'
SERVICE_URL = '/OS-KSADM/services'
TENANT_TYPE_TO_ENDPOINT_MAPPING_RULES_URL = (
    ENDPOINT_TEMPLATE_URL + '/RAX-AUTH/rules')
TENANTS_URL = '/tenants'
TOKEN_URL = '/tokens'
UPDATE_MFA_URL = '/users/{user_id}/RAX-AUTH/multi-factor'
UPDATE_USER_URL = DELETE_USER_URL = GET_USER_URL = '/users/{user_id}'
UPDATE_ENDPOINT_TEMPLATE_URL = GET_ENDPOINT_TEMPLATE_URL = (
    DELETE_ENDPOINT_TEMPLATE_URL) = (
    '/OS-KSCATALOG/endpointTemplates/{template_id}')
UPDATE_TENANT_URL = DELETE_TENANT_URL = GET_TENANT_URL = (
    '/tenants/{tenant_id}')
USER_URL = '/users'
VALIDATE_TOKENS_URL = '/tokens/{0}'
VERIFY_OTP_DEVICE_URL = (
    '/users/{user_id}/RAX-AUTH/multi-factor/otp-devices/{device_id}/verify')

'''File or Directory paths'''
PATH_TO_REPOSE_LOG = '/var/log/repose/current.log'
PATH_TO_USER_DELETE_LOG = '/var/log/idm/userDelete.log'

'''Some Constants for values'''
API_KEY_PATTERN = (
    '[a-f0-9]{8}[\-][a-f0-9]{4}[\-][a-f0-9]{4}[\-][a-f0-9]{4}[\-][a-f0-9]{12}')
BROKER = "BROKER"
CONTACT_ID_MIN = 10000000
CONTACT_ID_MAX = 99999999
CONTENT_TYPE_VALUE = ACCEPT_ENCODING_VALUE = 'application/{0}'
YAML_CONTENT_TYPE_VALUE = YAML_ACCEPT_ENCODING_VALUE = 'text/yaml'
DOMAIN_API_TEST = 'api-test'
DOMAIN_PATTERN = '[a-z]{8}'
DESC_PATTERN = '[a-zA-Z ,.]{10:200}'
DOMAIN_TEST = 'meow'
DELEGATION_AGREEMENT_NAME_PATTERN = 'DA[0-9]{7}'
EMAIL_RANDOM = 'randome@rackspace.com'
FED_USER_PATTERN = 'fed[\-]user[\d\w]{12}'

ID_PATTERN = '[\d]{8}'
IDENTITY_PRODUCT_ROLE_NAME_PATTERN = 'identity:testrole[\-][\d\w]{8}'
IDP_MAPPING_PATTERN = "[a-zA-Z]{{{mapping_size}}}"
ISSUER_PATTERN = 'test[\-]issuer[\-][\d\w]{12}'
ITEM_NOT_FOUND = "itemNotFound"
LOWER_CASE_LETTERS = '[a-z]{8}'
MD5_PATTERN = '[a-f][0-9]{40}'
MAPPING_RULE_DESCRIPTION_PATTERN = (
    'mapping[\-]rule[\-]description[\-][\w\d]{8}')
MAX_IDP_NAME_PATTERN = "[a-z]{255}"
IDP_NAME_PATTERN = "[a-zA-Z]{10:200}"
MIXED_CASE_LETTERS = '[A-Z][a-z]{8}'
MOSSO_TENANT_ID_PATTERN = '[\d]{7}'
NAST_TENANT_ID_PATTERN = 'NAST[\d]{6}'
NUMBERS_PATTERN = '[1-9]{1}[0-9]{8}'
NUMERIC_DOMAIN_ID_PATTERN = '[\-][1-9]{1}[0-9]{6}'
OTP_NAME_PATTERN = 'otp[\-][\d]{8}'
PASSWORD_PATTERN = 'Password1[\d\w]{10}'
RCN_PATTERN = 'RCN[\-][\d]{6}'
RACKER = "RACKER"
# 'RACKSPACE_DOMAIN' is to be used in racker-auth call
RACKSPACE_DOMAIN = 'Rackspace'
RCN = 'RCN'
ROLE_NAME_PATTERN = 'cid_test_role[\-][\d]{8}'
SECRETQ_PATTERN = 'SecretQ[\w]{15}'
SECRETA_PATTERN = 'SecretA[\w]{15}'
SERVICE_NAME_PATTERN = 'service[\-][\w\d]{8}'
SERVICE_TYPE_PATTERN = 'service[\-]type[\-][\w\d]{8}'
SERVICE_ID_PATTERN = '[\d]{8}'
SUB_USER_PATTERN = 'sub[\-]user[\d\w]{12}'
STANDARD = 'STANDARD'
USER_ADMIN_PATTERN = 'uadm_[\d\w]{12}'
TENANT_ID_PATTERN = '[\d]{8}'
TENANT_NAME_PATTERN = 'api[\-]test[\-]tenant[\-][\d\w]{8}'
TENANT_TYPE_PATTERN = 'ttype[a-z0-9]{10}'
UPPER_CASE_LETTERS = '[A-Z]{8}'
URL_PATTERN = 'http://www.rackspace.com/'
EMAIL_PATTERN = '[\w]{12}@[A-Za-z]{8}.com'
UNVERIFIED_EMAIL_PATTERN = '[A-Za-z]{12}@[A-Za-z]{8}.com'
USER_NAME_PATTERN = 'api[\-]test[\-][\d\w]{12}'
USER_MANAGER_NAME_PATTERN = 'user[\-]manager[\d\w]{12}'
USER_GROUP_NAME_PATTERN = 'user[\-]group[\-][\d\w]{10}'

'''Headers'''
ACCESS_CONTROL_REQUEST_HEADERS = 'Access-Control-Request-Headers'
ACCESS_CONTROL_REQUEST_METHOD = 'Access-Control-Request-Method'
ACCESS_CONTROL_ALLOW_ORIGIN = 'Access-Control-Allow-Origin'
ACCESS_CONTROL_ALLOW_CREDENTIALS = 'Access-Control-Allow-Credentials'
ACCESS_CONTROL_ALLOW_METHODS = 'Access-Control-Allow-Methods'
ACCESS_CONTROL_ALLOW_HEADERS = 'Access-Control-Allow-Headers'
ACCESS_CONTROL_EXPOSE_HEADERS = 'Access-Control-Expose-Headers'
ORIGIN = 'Origin'
X_AUTH_KEY = 'X-Auth-Key'
X_AUTH_TOKEN = 'X-Auth-Token'
X_AUTH_USER = 'X-Auth-User'
X_DOMAIN_ID = 'X-Domain-Id'
X_TENANT_ID = 'X-Tenant-Id'
X_USER_ID = 'X-User-Id'
X_USER_NAME = 'X-User-Name'
X_SESSION_ID = 'X-SessionId'
X_PASSWORD_EXPIRATION = 'X-Password-Expiration'
X_POWERED_BY = 'X-Powered-By'
X_STORAGE_PASS = 'X-Storage-Pass'
X_STORAGE_TOKEN = 'X-Storage-Token'
X_STORAGE_URL = 'X-Storage-Url'
X_STORAGE_USER = 'X-Storage-User'
X_SUBJECT_TOKEN = 'X-Subject-Token'
X_SERVER_MANAGEMENT_URL = 'X-Server-Management-Url'
X_CDN_MANAGEMENT_URL = 'X-CDN-Management-Url'
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
APPROVED_DOMAIN_GROUP_GLOBAL = 'GLOBAL'
AS_CONFIGURED_VALUE = 'asConfiguredValue'
ASSIGNMENT_TYPE = 'assignmentType'
ASSIGNMENT_TYPE_MOSSO = 'MOSSO'
ASSIGNMENT_TYPE_NAST = 'NAST'
ASSIGNMENT_TYPE_MANUAL = 'MANUAL'
AUTH = 'auth'
AUTHENTICATED_BY = 'authenticatedBy'
RAX_AUTH_AUTHENTICATED_BY = 'RAX-AUTH:authenticatedBy'
BAD_REQUEST = 'badRequest'
BASE_URL = 'baseURL'
BASE_URLS = 'baseURLs'
BASE_URL_REF = 'baseURLRef'
BASE_URL_REFS = 'baseURLRefs'
CREDENTIALS = 'credentials'
APPROVED_DOMAIN_GROUP = 'approvedDomainGroup'
APPROVED_DOMAIN_Ids = 'approvedDomainIds'
ATTACHMENT = 'attachment'
ATTACHMENTS = 'attachments'
AUDIT_DATA = 'auditData'
AUTHENTICATION_URL = 'authenticationUrl'
CODE = 'code'
CONFIG_PATH = 'configPath'
CONTACTID = 'contactId'
CONTENT_TYPE = 'Content-Type'
CREATED = 'created'
CREATION = 'creation'
DATA_CENTER = 'dataCenter'
DEFAULT = 'default'
DEFAULT_REGION = 'defaultRegion'
DEMOTE_USER_ID = 'demoteUserId'
DESCRIPTION = 'description'
DEFAULT_VALUE = 'defaultValue'
DELEGATE_ID = 'delegateId'
DELEGATE_REFERENCES = 'RAX-AUTH:delegateReferences'
DELEGATE_TYPE = 'delegateType'
DISPLAY_NAME = 'display-name'
DOMAIN = 'domain'
DOMAIN_ENABLED = 'domainEnabled'
DOMAIN_ADMIN_CHANGE = 'RAX-AUTH:domainAdministratorChange'
DOMAIN_ID = 'domainId'
V2_DOMAIN_ORIGIN = 'v2DomainOrigin'
EMAIL = 'email'
ENABLED = 'enabled'
ENDPOINT = 'endpoint'
ENDPOINTS = 'endpoints'
ENDPOINT_TEMPLATE = 'endpointTemplate'
EXPIRATION = 'expiration'
EXPIRES = 'expires'
EXPIRE_IN_SECONDS = 'expire-in-seconds'
EVENT_TIME = 'eventTime'
EVENT_TYPE = 'eventType'
FACTOR_TYPE = 'factorType'
FEDERATED_USER = 'FEDERATED_USER'
FEDERATION_TYPE = 'federationType'
FORBIDDEN = 'forbidden'
FOR_TENANTS = 'forTenants'
FORM_ENCODE = 'formEncode'
GLOBAL = 'global'
GROUP = 'group'
GROUPS = 'groups'
HREF = 'href'
ID = 'id'
IMPERSONATED_USER = 'impersonatedUser'
IMPERSONATE = 'IMPERSONATE'
IMPERSONATION = 'IMPERSONATION'
IDENTITY_FAULT = 'identityFault'
INITIATOR = 'initiator'
ITEMS = 'items'
RAX_AUTH_ISSUED = 'RAX-AUTH:issued'
ISSUER = 'issuer'
KEY = 'key'
KEY_URI = 'keyUri'
METHOD_LABEL = 'methodLabel'
INTERNAL_URL = 'internalURL'
MESSAGE = 'message'
MOSSO_CREDENTIALS = 'mossoCredentials'
MOSSO_ID = 'mossoId'
NAME = 'name'
NAST_CREDENTIALS = 'nastCredentials'
NAST_ID = 'nastId'
NAST_PREFIX = 'MossoCloudFS_'
NEW_PASSWORD = 'newPassword'
OBSERVER = 'observer'
ON_ROLE = 'onRole'
ON_ROLE_NAME = 'onRoleName'
ON_TENANT_ID = 'onTenantId'
OUTCOME = 'outcome'
PARENT_DELEGATION_AGREEMENT_ID = 'parentDelegationAgreementId'
PASSCODE = 'passcode'
PASSWORD = 'password'
PASSWORD_CREDENTIALS = 'passwordCredentials'
PASSWORD_DURATION = 'passwordDuration'
PASSWORD_EXPIRATION = 'passwordExpiration'
PASSWORD_HISTORY_RESTRICTION = 'passwordHistoryRestriction'
PASSWORD_POLICY = 'passwordPolicy'
PEM_ENCODED = 'pemEncoded'
PRINCIPAL_DOMAIN_ID = 'principalDomainId'
PRINCIPAL_ID = 'principalId'
PRINCIPAL_TYPE = 'principalType'
USER_GROUP = 'USER_GROUP'
PROMOTE_USER_ID = 'promoteUserId'
PROPAGATE = 'propagate'
PROPERTIES = 'properties'
PROP_VALUE = 'value'
PROP_VALUE_TYPE = 'valueType'
PROVISIONED_USER = 'PROVISIONED_USER'
PUBLIC_CERTIFICATES = 'publicCertificates'
PUBLIC_URL = 'publicURL'
QR_CODE = 'qrcode'
QUERY_STRING = 'queryString'
REGION = 'region'
REGISTRATION_CODE = 'registrationCode'
REQUEST_URL = 'requestURL'
REQUIRED = 'required'
REASON = 'reason'
REASON_CODE = 'reasonCode'
RESPONSE_MESSAGE = 'responseMessage'
ROLE = 'role'
ROLE_ASSIGNMENT_TYPE_BOTH = 'BOTH'
ROLE_ASSIGNMENT_TYPE_GLOBAL = 'GLOBAL'
ROLE_ASSIGNMENT_TYPE_TENANT = 'TENANT'
ROLE_ASSIGNMENTS = 'roleAssignments'
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
SESSION_TIMEOUT = 'sessionInactivityTimeout'
SOURCE = 'source'
SOURCES = 'sources'
SOURCE_TYPE = 'sourceType'
SUBAGREEMENT_NEST_LEVEL = 'subAgreementNestLevel'
TARGET = 'target'
TENANT = 'tenant'
RAX_AUTH_TENANT_TYPE = 'RAX-AUTH:tenantType'
RAX_AUTH_TENANT_TYPES = 'RAX-AUTH:tenantTypes'
TENANT_ALIAS = 'tenantAlias'
TENANT_ASSIGNMENTS = 'tenantAssignments'
TENANTS = 'tenants'
TENANT_ID = 'tenantId'
TENANT_NAME = 'tenantName'
TOKEN = 'token'
TOKEN_AUTH_BY_GROUPS = 'tokenAuthenticatedByGroups'
TOKEN_CREATED_BEFORE = 'tokenCreatedBefore'
TOKEN_FORMAT = 'tokenFormat'
TOKEN_ANALYSIS = 'tokenAnalysis'
TOKEN_DECRYPTABLE = 'tokenDecryptable'
TOKEN_EXPIRED = 'tokenExpired'
TOKEN_REVOKED = 'tokenRevoked'
TOKEN_VALID = 'tokenValid'
TRRS = 'trrs'
TYPE = 'type'
TYPE_URI = 'typeURI'
UPDATED = 'updated'
USER = 'user'
USER_ID = 'userId'
USERS = 'users'
USERNAME = 'username'
USER_NAME = 'userName'
USER_TYPE = 'userType'
UNLOCK = 'unlock'
V1_DEFAULT = 'v1Default'
VALUE = 'value'
VERIFIED = 'verified'
VERSION_ADDED = 'versionAdded'
VERSION_ID = 'versionId'
VERSION_INFO = 'versionInfo'
VERSION_LIST = 'versionList'
XML = 'xml'
XMLNS = 'xmlns'
X_WWW_FORM_URLENCODED = 'x-www-form-urlencoded'
JSON = 'json'
YAML = 'yaml'
OS_KSADM_PASSWORD = 'OS-KSADM:password'
SECRETQA = 'secretQA'

API_KEY_CREDENTIALS = 'apiKeyCredentials'
CADF_ATTACHMENTS = 'cadf:attachments'
CADF_ATTACHMENT = 'cadf:attachment'
CADF_CONTENT = 'cadf:content'
CADF_INITIATOR = 'cadf:initiator'
CADF_HOST = 'cadf:host'
CADF_REASON = 'cadf:reason'
DELEGATION_AGREEMENT = 'delegationAgreement'
DELEGATION_AGREEMENTS = 'delegationAgreements'
DELEGATION_AGREEMENT_ID = 'delegationAgreementId'
ENDPOINT_ASSIGNMENT_RULES = 'endpointAssignmentRules'
EMAIL_DOMAIN = 'emailDomain'
EMAIL_DOMAINS = 'emailDomains'
FEDERATED_IDP = 'federatedIdp'
RAX_AUTH_ENDPOINT_ASSIGNMENT_RULES = 'RAX-AUTH:endpointAssignmentRules'
OS_KSCATALOG_ENDPOINT_TEMPLATE = 'OS-KSCATALOG:endpointTemplate'
OS_KSCATALOG_ENDPOINT_TEMPLATES = 'OS-KSCATALOG:endpointTemplates'
MULTI_FACTOR_ENABLED = 'multiFactorEnabled'
MULTI_FACTOR_STATE = 'multiFactorState'
OS_KSADM_NAMESPACE = 'OS-KSADM'
NS_FEDERATED_IDP = 'RAX-AUTH:federatedIdp'
NS_GROUP = 'RAX-KSGRP:group'
NS_GROUPS = 'RAX-KSGRP:groups'
NS_IDENTITY_PROVIDER = 'RAX-AUTH:identityProvider'
NS_IDENTITY_PROVIDERS = 'RAX-AUTH:identityProviders'
RAX_GRPADMN_NAMESPACE = 'RAX-GRPADM'
RAX_KSKEY_NAMESPACE = 'RAX-KSKEY'
RAX_KSGRP_NAMESPACE = 'RAX-KSGRP'
NS_PASSWORD = OS_KSADM_NAMESPACE + ':password'
NS_PUBLIC_CERTIFICATE = 'RAX-AUTH:publicCertificate'
NS_SECRETQA = 'RAX-KSQA:secretQA'
NS_API_KEY_CREDENTIALS = 'RAX-KSKEY:apiKeyCredentials'
RAX_AUTH = 'RAX-AUTH'
NS_TYPES = 'RAX-AUTH:types'
RAX_AUTH_ASSIGNMENT_TYPE = 'RAX-AUTH:assignmentType'
RAX_AUTH_CHANGE_PASSWORD_CREDENTIALS = 'RAX-AUTH:changePasswordCredentials'
RAX_AUTH_DELEGATION_AGREEMENT = 'RAX-AUTH:' + DELEGATION_AGREEMENT
RAX_AUTH_DELEGATION_AGREEMENTS = 'RAX-AUTH:' + DELEGATION_AGREEMENTS
RAX_AUTH_DELEGATION_AGREEMENT_ID = 'RAX-AUTH:' + DELEGATION_AGREEMENT_ID
RAX_AUTH_DELEGATION_CREDENTIALS = 'RAX-AUTH:delegationCredentials'
RAX_AUTH_DOMAIN = 'RAX-AUTH:domain'
RAX_AUTH_DOMAIN_ID = 'RAX-AUTH:domainId'
NS_ADMINISTRATOR_ROLE = 'RAX-AUTH:administratorRole'
NS_PROPAGATE = RAX_AUTH + ':' + PROPAGATE
RAX_AUTH_CONTACTID = 'RAX-AUTH:contactId'
RAX_AUTH_DEFAULT_REGION = 'RAX-AUTH:defaultRegion'
RAX_AUTH_INVITE = 'RAX-AUTH:invite'
RAX_AUTH_MULTI_FACTOR = 'RAX-AUTH:multiFactor'
RAX_AUTH_MULTI_FACTOR_ENABLED = 'RAX-AUTH:multiFactorEnabled'
RAX_AUTH_UNVERIFIED = 'RAX-AUTH:unverified'
RAX_AUTH_MULTI_FACTOR_STATE = 'RAX-AUTH:multiFactorState'
RAX_AUTH_PASSWORD_EXPIRATION = 'RAX-AUTH:passwordExpiration'
RAX_AUTH_REGISTRATION_CODE = 'RAX-AUTH:registrationCode'
RAX_AUTH_USER_GROUP = 'RAX-AUTH:userGroup'
RAX_AUTH_USER_GROUPS = 'RAX-AUTH:userGroups'
RAX_AUTH_USER_MULTI_FACTOR_ENFORCEMENT_LEVEL = (
    'RAX-AUTH:userMultiFactorEnforcementLevel')
RAX_AUTH_FACTOR_TYPE = 'RAX-AUTH:factorType'
RAX_AUTH_ADMINISTRATOR_ROLE = 'RAX-AUTH:administratorRole'
RAX_AUTH_PROPAGATE = 'RAX-AUTH:propagate'
RAX_AUTH_ASSIGNMENT = 'RAX-AUTH:assignment'
RAX_AUTH_ROLE_ASSIGNMENTS = 'RAX-AUTH:' + ROLE_ASSIGNMENTS
RAX_AUTH_ROLE_TYPE = 'RAX-AUTH:roleType'
RAX_KSQA_NAMESPACE = 'RAX-KSQA'
RCN_LONG = 'rackspaceCustomerNumber'
RAX_AUTH_SESSION_TIMEOUT = 'RAX-AUTH:sessionInactivityTimeout'
RAX_AUTH_TOKEN_FORMAT = 'RAX-AUTH:tokenFormat'
NS_SERVICE = OS_KSADM_NAMESPACE + ':service'
NS_TENANT_TYPE_TO_ENDPOINT_MAPPING_RULE = (
    RAX_AUTH + ':tenantTypeEndpointRule')
TENANT_TYPE = 'tenantType'
TENANT_TYPE_CLOUD = 'cloud'
TENANT_TYPE_FAWS = 'faws'
TENANT_TYPE_PROTECTED_PREFIX = 'protectedprefix'
TENANT_TYPE_TO_ENDPOINT_MAPPING_RULE = 'tenantTypeEndpointRule'
TENANT_TYPE_TO_ENDPOINT_MAPPING_RULES = 'tenantTypeEndpointRules'
NS_SERVICES = OS_KSADM_NAMESPACE + ':services'
NS_IMPERSONATION = 'RAX-AUTH:impersonation'
EXCLUDED_TENANT_TYPES = [TENANT_TYPE_PROTECTED_PREFIX]

'''Query parameters'''
ALL = 'all'
PARAM_ROLE_ID = 'roleId'
PARAM_ROLE_NAME = 'roleName'
RESPONSE_DETAIL = 'responseDetail'
NS_PASSCODE_CREDENTIALS = RAX_AUTH + ':passcodeCredentials'
NS_OTP_DEVICE = RAX_AUTH + ':otpDevice'
NS_VERIFICATION_CODE = RAX_AUTH + ':verificationCode'
QUERY_PARAM_APPLY_RCN_ROLES = 'apply_rcn_roles'
QUERY_PARAM_DELEGATE = 'delegate'
QUERY_PARAM_PRINCIPAL = 'principal'
RELATIONSHIP = 'relationship'
USER_MULTI_FACTOR_ENFORCEMENT_LEVEL = 'userMultiFactorEnforcementLevel'
UA_AUDIT_DATA = 'ua:auditData'
UA_USER_NAME = 'ua:userName'
UA_REQUEST_URL = 'ua:requestURL'
UNVERIFIED = 'unverified'
VERIFIED = 'verified'
QUERY_PARAM_USER_TYPE = 'user_type'

'''Some constants used for namespace'''
XMLNS_V20 = 'http://docs.openstack.org/identity/api/v2.0'
XMLNS_V11 = 'http://docs.rackspacecloud.com/auth/api/v1.1'
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
AUTH_BY_LIST = ['APIKEY', 'PASSWORD', 'FEDERATED']
AUTH_BY_DELEGATION = 'DELEGATE'
AUTH_BY_FEDERATED = 'FEDERATED'
AUTH_BY_PWD = 'PASSWORD'
AUTH_BY_OTPPASSCODE = 'OTPPASSCODE'
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
IDP_MAPPING_POLICY_MAX_SIZE = "identity.provider.policy.max.kilobyte.size"
FEDERATION_IDENTITY_PROVIDER_DEFAULT_POLICY = (
    'federation.identity.provider.default.policy')
IDENTITY_PROPERTY = "identityProperty"
VALUE_TYPE = "valueType"
IDM_VERSION = "idmVersion"
RELOADABLE = "reloadable"
SEARCHABLE = "searchable"

'''Source Types'''
USER_SOURCE_TYPE = "USER"
USERGROUP_SOURCE_TYPE = "USERGROUP"
SYSTEM_SOURCE_TYPE = "SYSTEM"
TENANT_ASSIGNMENT_TYPE = "TENANT"
DOMAIN_ASSIGNMENT_TYPE = "DOMAIN"


'''ROLES'''
COMPUTE_ROLE_NAME = "compute:default"
COMPUTE_ROLE_ID = '6'
ENDPOINT_RULE_ADMIN_ROLE_NAME = 'identity:endpoint-rule-admin'
IDENTITY_ADMIN_ROLE_ID = '1'
IDENTITY_ADMIN_ROLE_NAME = 'identity:admin'
HIERARCHICAL_ADMIN_ROLE_NAME = 'admin'
HIERARCHICAL_BILLING_ADMIN_ROLE_NAME = 'billing:admin'
HIERARCHICAL_BILLING_OBSERVER_ROLE_NAME = 'billing:observer'
HIERARCHICAL_OBSERVER_ROLE_NAME = 'observer'
HIERARCHICAL_TICKET_OBSERVER_ROLE_NAME = 'ticketing:observer'
OBJECT_STORE_ROLE_NAME = 'object-store:default'
SERVICE_ADMIN_ROLE_ID = '4'
TENANT_ACCESS_ROLE_NAME = 'identity:tenant-access'
USER_MANAGE_ROLE_NAME = 'identity:user-manage'
USER_DEFAULT_ROLE_NAME = 'identity:default'
USER_ADMIN_ROLE_ID = '3'
USER_DEFAULT_ROLE_ID = '2'
USER_MANAGER_ROLE_ID = '7'
ROLE_RBAC1_NAME = 'rbacRole1'
PROVIDER_MANAGEMENT_ROLE_NAME = 'identity:identity-provider-manager'
PROVIDER_RO_ROLE_NAME = 'identity:identity-provider-read-only'
IDENTITY_INTERNAL_ROLE_NAME = 'identity:internal'
IDENTITY_PROPERTY_ADMIN_ROLE_NAME = 'identity:property-admin'
USER_ADMIN_ROLE_NAME = 'identity:user-admin'
RCN_ADMIN_ROLE_NAME = 'rcn:admin'
RCN_SWITCH_ROLE_NAME = 'identity:domain-rcn-switch'

GLOBAL_USER_ROLES_ROLE_NAME = 'identity:get-user-roles-global'
GLOBAL_USER_ROLES_ROLE_ID = '14'

'''FEATURE FLAGS'''
FEATURE_FLAG_FOR_ENDPOINTS_BASED_ON_RULES = (
    'feature.include.endpoints.based.on.rules')
AUTO_ASSIGN_ROLE_ON_DOMAIN_TENANTS_ROLE_NAME = (
    'auto.assign.role.on.domain.tenants.role.name')
FEATURE_FLAG_FOR_DISABLING_SERVICE_NAME_TYPE = (
    "feature.endpoint.template.disable.name.type")
TENANT_DEFAULT_DOMAIN = 'tenant.domainId.default'
FEATURE_LIST_SUPPORT_ADDITIONAL_ROLE_PROPERTIES = (
    'feature.list.support.additional.role.properties')

'''FLAGS'''
PASSWORD_LOCKOUT_RETRIES = 'ldap.auth.password.lockout.retries'
