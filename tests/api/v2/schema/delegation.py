"""
Schema Definitions for Delegation.
"""

from tests.package.johny import constants as const

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

tenants_role_assignments = {
    'type': 'object', 'properties': {
        const.RAX_AUTH_ROLE_ASSIGNMENTS: tenant_assignments_item
    },
    'required': [const.RAX_AUTH_ROLE_ASSIGNMENTS]
}
