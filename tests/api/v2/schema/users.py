"""Schema Definitions for Users endpoints.

This module will contain the json schema definitions for all API responses
defined in
http://docs-internal.rackspace.com/auth/api/v2.0/auth-admin-devguide/content/User_Calls.html # noqa
"""

from tests.package.johny import constants as const

add_user = {
    'type': 'object', 'properties':
        {const.USER: {
            'type': 'object',
            'properties': {
                const.USERNAME: {'type': 'string'},
                const.EMAIL: {'type': 'string', 'format': 'email'},
                const.RAX_AUTH_MULTI_FACTOR_ENABLED: {'type': 'boolean'},
                const.RAX_AUTH_UNVERIFIED: {'type': 'boolean'},
                const.ENABLED: {'type': 'boolean'},
                const.RAX_AUTH_DEFAULT_REGION: {
                    'type': 'string',
                    'enum': const.DC_LIST},
                const.PASSWORD: {'type': 'string'},
                const.ID: {'type': 'string'},
                const.RAX_AUTH_DOMAIN_ID: {'type': 'string'},
                const.NS_PASSWORD: {'type': 'string'},
                const.NS_SECRETQA: {
                    'type': 'object',
                    'properties': {
                        const.SECRET_ANSWER: {'type': 'string'},
                        const.SECRET_QUESTION: {'type': 'string'}
                    }},
                const.ROLES: {'type': 'array'}},
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
                const.ID: {'type': 'string'},
                const.RAX_AUTH_MULTI_FACTOR_ENABLED: {'type': 'boolean'},
                const.RAX_AUTH_UNVERIFIED: {'type': 'boolean'},
                const.CREATED: {'type': 'string', 'format': 'dateTime'},
                const.FEDERATED_IDP: {'type': 'string'}},
            'required': [const.USERNAME, const.EMAIL, const.ENABLED,
                         const.RAX_AUTH_DEFAULT_REGION,
                         const.RAX_AUTH_DOMAIN_ID, const.CREATED,
                         const.RAX_AUTH_MULTI_FACTOR_ENABLED],
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
                const.RAX_AUTH_UNVERIFIED: {'type': 'boolean'},
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
                         const.CREATED],
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
        const.UPDATED: {'type': 'string', 'format': 'date'},
        const.NS_FEDERATED_IDP: {'type': 'string'},
        const.RAX_AUTH_MULTI_FACTOR_ENABLED: {'type': 'boolean'},
        const.RAX_AUTH_UNVERIFIED: {'type': 'boolean'},
        const.RAX_AUTH_FACTOR_TYPE: {'type': 'string'},
        const.RAX_AUTH_MULTI_FACTOR_STATE: {'type': 'string'},
        const.RAX_AUTH_DOMAIN_ID: {'type': 'string'},
        const.RAX_AUTH_CONTACTID: {'type': 'string'},
        const.RAX_AUTH_UNVERIFIED: {'type': 'boolean'},
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
        const.USERS: {'type': 'array', 'items': user_item, "uniqueItems": True}
    },
    'required': [const.USERS]
}

get_admins_of_user_item = {
    'type': 'object', 'properties': {
            const.USERNAME: {'type': 'string'},
            const.EMAIL: {'type': 'string', 'format': 'email'},
            const.RAX_AUTH_MULTI_FACTOR_ENABLED: {'type': 'boolean'},
            const.RAX_AUTH_UNVERIFIED: {'type': 'boolean'},
            const.ENABLED: {'type': 'boolean'},
            const.RAX_AUTH_DOMAIN_ID: {'type': 'string'},
            const.RAX_AUTH_CONTACTID: {'type': 'string'},
            const.RAX_AUTH_UNVERIFIED: {'type': 'boolean'},
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

add_unverified_user = {
    'type': 'object', 'properties':
        {const.USER: {
            'type': 'object',
            'properties': {
                const.EMAIL: {'type': 'string', 'format': 'email'},
                const.RAX_AUTH_UNVERIFIED: {'type': 'boolean'},
                const.ENABLED: {'type': 'boolean'},
                const.RAX_AUTH_DEFAULT_REGION: {
                    'type': 'string',
                    'enum': const.DC_LIST},
                const.ID: {'type': 'string'},
                const.RAX_AUTH_DOMAIN_ID: {'type': 'string'},
                },
            'required': [const.ENABLED, const.RAX_AUTH_UNVERIFIED,
                         const.RAX_AUTH_DEFAULT_REGION, const.ID,
                         const.RAX_AUTH_DOMAIN_ID, const.EMAIL
                         ],
            'additionalProperties': False}},
    'required': [const.USER],
    'additionalProperties': False
}

invite_unverified_user = {
    'type': 'object', 'properties':
        {const.RAX_AUTH_INVITE: {
            'type': 'object',
            'properties': {
                const.EMAIL: {'type': 'string', 'format': 'email'},
                const.REGISTRATION_CODE: {'type': 'string'},
                const.USER_ID: {'type': 'string'},
                const.CREATED: {'type': 'string', 'format': 'dateTime'},
                const.EXPIRES: {'type': 'string', 'format': 'dateTime'},
            },
            'required': [const.EMAIL, const.REGISTRATION_CODE,
                         const.USER_ID, const.CREATED],
            'additionalProperties': False}},
    'required': [const.RAX_AUTH_INVITE],
    'additionalProperties': False
}
