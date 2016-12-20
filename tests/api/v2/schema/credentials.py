""" Schema Definitions for credentials end points

http://docs-internal.rackspace.com/auth/api/v2.0/auth-admin-devguide/content/GET_admin-listCredentials_v2.0_users__userId__OS-KSADM_credentials_User_Calls.html
"""
from tests.package.johny import constants as const

list_credentials = {
    'type': 'object', 'properties': {
        const.CREDENTIALS: {
            'type': 'array', 'properties': {
                const.NS_API_KEY_CREDENTIALS: {
                    'type': 'object', 'properties': {
                            const.USERNAME: {'type': 'string'},
                            const.API_KEY: {'type': 'string'}
                    },
                    'required': [const.USERNAME, const.API_KEY],
                    'additionalProperties': False
                }
            },
            'required': [const.NS_API_KEY_CREDENTIALS],
            'additionalProperties': False
        }
    },
    'required': [const.CREDENTIALS],
    'additionalProperties': False
}

add_password_response = {
    'type': 'object', 'properties': {
        const.PASSWORD_CREDENTIALS: {
            'type': 'object', 'properties': {
                const.USERNAME: {'type': 'string'},
                const.PASSWORD: {'type': 'string'}
             },
            'required': [const.USERNAME, const.PASSWORD],
            'additionalProperties': False
        }
    },
    'required': [const.PASSWORD_CREDENTIALS],
    'additionalProperties': False
}

get_apikey_response = {
    'type': 'object', 'properties': {
        const.NS_API_KEY_CREDENTIALS: {
            'type': 'object', 'properties': {
                const.USERNAME: {'type': 'string'},
                const.API_KEY: {'type': 'string'}
             },
            'required': [const.USERNAME, const.API_KEY],
            'additionalProperties': False
        }
    },
    'required': [const.NS_API_KEY_CREDENTIALS],
    'additionalProperties': False
}
