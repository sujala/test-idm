"""
Schema definition for Roles
All calls response under:
http://docs-internal.rackspace.com/auth/api/v2.0/auth-admin-devguide/content/Role_Calls.html
"""

from tests.package.johny import constants as const

role_item = {
    'type': 'object',
    'properties': {
        const.ID: {'type': 'string'},
        const.NAME: {'type': 'string'},
        const.DESCRIPTION: {'type': 'string'},
        const.SERVICE_ID: {'type': 'string'},
        const.RAX_AUTH_PROPAGATE: {'type': 'boolean'},
        const.RAX_AUTH_ADMINISTRATOR_ROLE: {'type': 'string'},
        const.RAX_AUTH_ASSIGNMENT: {'type': 'string'},
        const.RAX_AUTH_ROLE_TYPE: {'type': 'string'},
        const.NS_TYPES: {'type': 'array',
                         'items': {'type': 'string'}}
    },
    'required': [const.NAME, const.ID],
    'additionalProperties': False
}

add_role = {
    'type': 'object',
    'properties': {const.ROLE: role_item},
    'required': [const.ROLE],
    'additionalProperties': False
}

list_roles = {
    'type': 'object',
    'properties': {
        const.ROLES: {
            'type': 'array',
            'items': role_item
        }
    },
    'required': [const.ROLES],
    'additionalProperties': False
}
