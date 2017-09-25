"""
Schema Definitions for User Groups.
"""

from tests.package.johny import constants as const

user_group_item = {
    'type': 'object', 'properties': {
        const.NAME: {'type': 'string'},
        const.ID: {'type': 'string'},
        const.DESCRIPTION: {'type': 'string'},
        const.DOMAIN_ID: {'type': 'string'}
    },
    'required': [const.DOMAIN_ID, const.ID, const.NAME],
    'additionalProperties': False
}

list_user_groups_for_domain = {
    'type': 'object', 'properties': {
        const.RAX_AUTH_USER_GROUPS: {
            'type': 'array', 'item': user_group_item
        }
    }
}

add_user_group_for_domain = {
    'type': 'object', 'properties': {
        const.RAX_AUTH_USER_GROUP: user_group_item
    },
    'required': [const.RAX_AUTH_USER_GROUP]
}

get_user_group_for_domain = add_user_group_for_domain
