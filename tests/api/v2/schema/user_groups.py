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
            'type': 'array', 'items': user_group_item
        }
    },
    'required': [const.RAX_AUTH_USER_GROUPS]
}

add_user_group_for_domain = {
    'type': 'object', 'properties': {
        const.RAX_AUTH_USER_GROUP: user_group_item
    },
    'required': [const.RAX_AUTH_USER_GROUP]
}

get_user_group_for_domain = add_user_group_for_domain

tenant_assignment_item = {
    'type': 'object', 'properties': {
        const.ON_ROLE: {'type': 'string'},
        const.ON_ROLE_NAME: {'type': 'string'},
        const.FOR_TENANTS: {'type': 'array', 'items': {'type': 'string'},
                            "uniqueItems": True}
    },
    'required': [const.ON_ROLE, const.FOR_TENANTS, const.ON_ROLE_NAME]
}

tenant_assignments_item = {
    'type': 'object', 'properties': {
        const.TENANT_ASSIGNMENTS: {
            'type': 'array', 'items': tenant_assignment_item
        }
    },
    'required': [const.TENANT_ASSIGNMENTS]
}

tenants_role_assignments_for_user_group = {
    'type': 'object', 'properties': {
        const.RAX_AUTH_ROLE_ASSIGNMENTS: tenant_assignments_item
    },
    'required': [const.RAX_AUTH_ROLE_ASSIGNMENTS]
}
