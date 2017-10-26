"""Schema Definitions for Tokens Endpoints 
This module will contain the json schema definitions for all API responses
defined in
http://docs-internal.rackspace.com/auth/api/v2.0/auth-admin-devguide/content/Token_Calls.html # noqa
"""
import copy

from tests.package.johny import constants as const


tenant_item = {
    'type': 'object', 'properties': {
        const.ID: {'type': 'string'},
        const.NAME: {'type': 'string'}
    },
    'required': [const.ID, const.NAME]
}

token_item = {
    'type': 'object', 'properties': {
        const.ID: {'type': 'string'},
        const.EXPIRES: {'type': 'string'},
        const.TENANT: tenant_item,
        const.RAX_AUTH_AUTHENTICATED_BY: {
            'type': 'array', 'items': {'type': 'string'}
        },
        const.RAX_AUTH_ISSUED: {'type': 'string', 'format': 'dateTime'}
    },
    'required': [const.ID, const.EXPIRES, const.RAX_AUTH_AUTHENTICATED_BY,
                 const.RAX_AUTH_ISSUED]
}

user_item = {
    'type': 'object', 'properties': {
        const.ID: {'type': 'string'},
        const.NAME: {'type': 'string'},
        const.RAX_AUTH_DEFAULT_REGION: {'type': 'string',
                                        'enum': const.DC_LIST},
        const.ROLES: {'type': 'array'},
        const.RAX_AUTH_SESSION_TIMEOUT: {'type': 'string'}},
    'required': [const.RAX_AUTH_DEFAULT_REGION, const.ROLES, const.ID,
                 const.NAME]
}

validate_token = {
    'type': 'object', 'properties': {
        const.ACCESS: {
            'properties': {
                'token': token_item,
                'user': user_item
            },
            'required': ['token', 'user']
        }
    },
    'required': [const.ACCESS]
}

endpoint_item = {
    'type': 'object', 'properties': {
        const.TENANT_ID: {'type': 'string'},
        const.PUBLIC_URL: {'type': 'string'},
        const.INTERNAL_URL: {'type': 'string'},
        const.REGION: {'type': 'string'},
        const.VERSION_LIST: {'type': 'string'},
        const.VERSION_INFO: {'type': 'string'},
        const.VERSION_ID: {'type': 'string'}
    },
    'required': [const.TENANT_ID, const.PUBLIC_URL]
}

service_catalog_item = {
    'type': 'object',
    'properties': {
        const.ENDPOINTS: {'type': 'array', 'items': endpoint_item},
        const.NAME: {'type': 'string'},
        const.TYPE: {'type': 'string'}
    },
    'required': [const.ENDPOINTS, const.NAME, const.TYPE],
    'additionalProperties': False
}
auth = copy.deepcopy(validate_token)
auth['properties'][const.ACCESS]['properties'][const.SERVICE_CATALOG] = {
    'type': 'array', 'items': service_catalog_item}
auth['properties'][const.ACCESS]['required'] += [const.SERVICE_CATALOG]

analyze_token_item = {
    const.TOKEN: {'type': 'string'},
    const.EXPIRATION: {'type': 'string'},
    const.CREATION: {'type': 'string'},
    const.TYPE: {
        'type': 'string',
        'enum': ['USER', const.IMPERSONATION, const.RACKER]},
    const.AUTHENTICATED_BY: {
        'type': 'array',
        'items': {'type': 'string', 'enum': const.AUTH_BY_LIST}}
}

analyze_token_user_item = {
    const.ID: {'type': 'string'},
    const.USERNAME: {'type': 'string'},
    const.DOMAIN: {'type': 'string'},
    const.ENABLED: {'type': 'boolean'},
    const.DOMAIN_ENABLED: {'type': 'boolean'},
    const.TYPE: {
        'type': 'string',
        'enum': [const.PROVISIONED_USER, const.RACKER, const.FEDERATED_USER]}
}

trr_item = {
    'type': 'object',
    'properties': {
        const.ID: {'type': 'string'},
        const.TOKEN_AUTH_BY_GROUPS: {'type': 'string'},
        const.TOKEN_CREATED_BEFORE: {'type': 'string'},
        const.TOKEN: {'type': 'string'}
    }
}

analyze_token = {
    'type': 'object',
    'properties': {
        const.TOKEN_ANALYSIS: {
            'type': 'object',
            'properties': {
                const.TOKEN_VALID: {'type': 'boolean'},
                const.TOKEN_EXPIRED: {'type': 'boolean'},
                const.TOKEN_REVOKED: {'type': 'boolean'},
                const.TOKEN: {'type': 'object',
                              'properties': analyze_token_item},
                const.USER: {'type': 'object',
                             'properties': analyze_token_user_item},
                const.TOKEN_DECRYPTABLE: {'type': 'boolean'},
                const.TRRS: {
                    'type': 'array',
                    'items': trr_item,
                    'minItems': 0}},
            'required': [
                const.TOKEN_VALID, const.TOKEN_EXPIRED, const.TOKEN_REVOKED,
                const.TOKEN_DECRYPTABLE]}
    },
    'required': [const.TOKEN_ANALYSIS]
}

analyze_valid_token = copy.deepcopy(analyze_token)
analyze_valid_token[
    'properties'][const.TOKEN_ANALYSIS]['required'].extend(
    [const.TOKEN, const.USER])

impersonated_user_item = {
    const.ID: {'type': 'string'},
    const.USERNAME: {'type': 'string'},
    const.DOMAIN: {'type': 'string'},
    const.ENABLED: {'type': 'boolean'},
    const.DOMAIN_ENABLED: {'type': 'boolean'},
    const.FEDERATED_IDP: {'type': 'string'},
    const.TYPE: {'type': 'string', 'enum': [const.FEDERATED_USER]}
}

analyze_token_fed_user_impersonation = copy.deepcopy(analyze_token)
analyze_token_fed_user_impersonation[
    'properties'][const.TOKEN_ANALYSIS][const.IMPERSONATED_USER] = \
    impersonated_user_item
analyze_token_fed_user_impersonation[
    'properties'][const.TOKEN_ANALYSIS]['required'].extend(
    [const.IMPERSONATED_USER, const.TOKEN, const.USER])


analyze_token_revoked = copy.deepcopy(analyze_token)
analyze_token_revoked[
    'properties'][const.TOKEN_ANALYSIS]['required'].extend(
    [const.TOKEN, const.USER, const.TRRS])

impersonation_item = copy.deepcopy(validate_token)
impersonation_item['properties'][const.ACCESS]['required'].remove('user')
del impersonation_item['properties'][const.ACCESS]['properties']['user']
