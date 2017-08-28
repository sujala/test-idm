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
        const.RAX_AUTH_ISSUED_AT: {'type': 'string', 'format': 'dateTime'}
    },
    # Attribute for 'issued_at' to be added back once CID-1082 is implemented
    'required': [const.ID, const.EXPIRES, const.RAX_AUTH_AUTHENTICATED_BY]
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

auth = copy.deepcopy(validate_token)
auth['properties'][const.ACCESS]['properties'][const.SERVICE_CATALOG] = {
    'type': 'array'}
auth['properties'][const.ACCESS]['required'] += [const.SERVICE_CATALOG]
