from tests.api import constants as const
"""
Schema definition for Roles
All calls response under:
http://docs-internal.rackspace.com/auth/api/v2.0/auth-admin-devguide/content/Role_Calls.html
"""
role_item = {
    'type': 'object',
    'properties': {
        const.ID: {'type': 'string'},
        const.NAME: {'type': 'string'},
        const.DESCRIPTION: {'type': 'string'},
        const.SERVICE_ID: {'type': 'string'},
        const.RAX_AUTH_PROPAGATE: {'type': 'boolean'},
        const.RAX_AUTH_ADMINISTRATOR_ROLE: {'type': 'string'},
        const.RAX_AUTH_ASSIGNMENT: {'type': 'string'}
    },
    'requires': [const.NAME, const.ID, const.DESCRIPTION],
    'additionalProperties': False
}

add_role = {
    'type': 'object',
    'properties': {const.ROLE: role_item},
    'requires': [const.ROLE],
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
    'requires': [const.ROLES],
    'additionalProperties': False
}
