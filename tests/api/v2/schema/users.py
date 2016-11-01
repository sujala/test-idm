from tests.api import constants as const

"""Schema Definitions for Users endpoints.

This module will contain the json schema definitions for all API responses
defined in
http://docs-internal.rackspace.com/auth/api/v2.0/auth-admin-devguide/content/User_Calls.html # noqa
"""

add_user = {
    'type': 'object', 'properties':
        {const.USER: {
            'type': 'object',
            'properties': {
                const.USERNAME: {'type': 'string'},
                const.EMAIL: {'type': 'string', 'format': 'email'},
                const.RAX_AUTH_MULTI_FACTOR_ENABLED: {'type': 'boolean'},
                const.ENABLED: {'type': 'boolean'},
                const.RAX_AUTH_DEFAULT_REGION: {
                    'type': 'string',
                    'enum': const.DC_LIST},
                const.PASSWORD: {'type': 'string'},
                const.ID: {'type': 'string'},
                const.RAX_AUTH_DOMAIN_ID: {'type': 'string'},
                const.NS_PASSWORD: {'type': 'string'}},
            'required': [const.USERNAME, const.ENABLED,
                         const.RAX_AUTH_DEFAULT_REGION, const.ID,
                         const.RAX_AUTH_DOMAIN_ID,
                         const.RAX_AUTH_MULTI_FACTOR_ENABLED],
            'additionalProperties': False}},
    'required': [const.USER],
    'additionalProperties': False
}

# Response schema differs from the response documented in
# https://developer.rackspace.com/docs/cloud-identity/v2/
# developer-guide/#get-user-by-id
get_user = {
    'type': 'object', 'properties':
        {const.USER: {
            'type': 'object',
            'properties': {
                const.USERNAME: {'type': 'string'},
                const.EMAIL: {'type': 'string', 'format': 'email'},
                const.ENABLED: {'type': 'boolean'},
                const.RAX_AUTH_DOMAIN_ID: {'type': 'string'},
                const.RAX_AUTH_DEFAULT_REGION: {
                    'type': 'string',
                    'enum': const.DC_LIST},
                const.ID: {'type': 'string'}},
            'required': [const.USERNAME, const.EMAIL, const.ENABLED,
                         const.RAX_AUTH_DEFAULT_REGION,
                         const.RAX_AUTH_DOMAIN_ID],
            'additionalProperties': False}},
    'required': [const.USER],
    'additionalProperties': False
}

update_user = {
    'type': 'object', 'properties':
    {const.USER: {
            'type': 'object',
            'properties': {
                const.USERNAME: {'type': 'string'},
                const.EMAIL: {'type': 'string', 'format': 'email'},
                const.RAX_AUTH_MULTI_FACTOR_ENABLED: {'type': 'boolean'},
                const.ENABLED: {'type': 'boolean'},
                const.RAX_AUTH_DOMAIN_ID: {'type': 'string'},
                const.RAX_AUTH_CONTACTID: {'type': 'string'},
                const.RAX_AUTH_DEFAULT_REGION: {
                    'type': 'string',
                    'enum': const.DC_LIST},
                const.ID: {'type': 'string'},
                const.CREATED: {'type': 'string', 'format': 'dateTime'},
                const.UPDATED: {'type': 'string', 'format': 'dateTime'}},
            'required': [const.USERNAME, const.ID, const.ENABLED,
                         const.RAX_AUTH_DEFAULT_REGION,
                         const.RAX_AUTH_DOMAIN_ID,
                         const.RAX_AUTH_MULTI_FACTOR_ENABLED,
                         const.CREATED, const.UPDATED],
            'additionalProperties': False}},
    'required': [const.USER],
    'additionalProperties': False
}

user_item = {
    'type': 'object', 'properties':
    {
        const.USERNAME: {'type': 'string'},
        const.EMAIL: {'type': 'string', 'format': 'email'},
        const.ENABLED: {'type': 'boolean'},
        const.CREATED: {'type': 'string', 'format': 'date'},
        const.NS_FEDERATED_IDP: {'type': 'string'},
        const.RAX_AUTH_MULTI_FACTOR_ENABLED: {'type': 'boolean'},
        const.RAX_AUTH_FACTOR_TYPE: {'type': 'string'},
        const.RAX_AUTH_MULTI_FACTOR_STATE: {'type': 'string'},
        const.RAX_AUTH_DOMAIN_ID: {'type': 'string'},
        const.RAX_AUTH_CONTACTID: {'type': 'string'},
        const.RAX_AUTH_DEFAULT_REGION: {
            'type': 'string',
            'enum': const.DC_LIST},
        const.ID: {'type': 'string'}
    },
    "required": [const.USERNAME, const.ENABLED,
                 const.RAX_AUTH_DEFAULT_REGION,
                 const.ID, const.RAX_AUTH_DOMAIN_ID,
                 const.RAX_AUTH_MULTI_FACTOR_ENABLED],
    'additionalProperties': False
}

list_users = {
    'type': 'object', 'properties': {
        const.USERS: {'type': 'array', 'items': user_item}
    }
}

get_admins_of_user_item = {
    'type': 'object', 'properties': {
            const.USERNAME: {'type': 'string'},
            const.EMAIL: {'type': 'string', 'format': 'email'},
            const.RAX_AUTH_MULTI_FACTOR_ENABLED: {'type': 'boolean'},
            const.ENABLED: {'type': 'boolean'},
            const.RAX_AUTH_DOMAIN_ID: {'type': 'string'},
            const.RAX_AUTH_CONTACTID: {'type': 'string'},
            const.RAX_AUTH_DEFAULT_REGION: {
                'type': 'string',
                'enum': const.DC_LIST},
            const.ID: {'type': 'string'},
            const.CREATED: {'type': 'string', 'format': 'dateTime'}},
    'required': [const.USERNAME, const.ID, const.ENABLED,
                 const.RAX_AUTH_DEFAULT_REGION, const.RAX_AUTH_DOMAIN_ID,
                 const.RAX_AUTH_MULTI_FACTOR_ENABLED,
                 const.CREATED],
    'additionalProperties': False}

get_admins_of_user = {
    'type': 'object', 'properties': {
        const.USERS: {
            'type': 'array', 'items': get_admins_of_user_item
        }
    }
}
