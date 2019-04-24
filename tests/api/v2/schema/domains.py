"""
Schema definition for Domains
All calls response under:
http://docs-internal.rackspace.com/auth/api/v2.0/auth-admin-devguide/content/Role_Calls.html
"""

from tests.package.johny import constants as const

domain_item = {
    'type': 'object',
    'properties': {
        const.ID: {'type': 'string'},
        const.NAME: {'type': 'string'},
        const.DESCRIPTION: {'type': 'string'},
        const.ENABLED: {'type': 'boolean'},
        const.RCN_LONG: {'type': 'string'},
        const.SESSION_TIMEOUT: {'type': 'string'},
        const.TYPE: {'type': 'string'}
    },
    'required': [const.NAME, const.ID, const.SESSION_TIMEOUT],
    'additionalProperties': False
}

domain = {
    'type': 'object',
    'properties': {
        const.RAX_AUTH_DOMAIN: domain_item
    },
    'required': [const.RAX_AUTH_DOMAIN],
    'additionalProperties': False
}
