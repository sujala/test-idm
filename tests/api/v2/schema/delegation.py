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

da_item = {
        'type': 'object', 'properties': {
            const.ID: {'type': 'string'},
            const.NAME: {'type': 'string'},
            const.DOMAIN_ID: {'type': 'string'},
            const.PRINCIPAL_ID: {'type': 'string'},
            const.PRINCIPAL_TYPE: {'type': 'string'},
            const.ALLOW_SUB_AGREEMENTS: {'type': 'boolean'},
            const.SUBAGREEMENT_NEST_LEVEL: {'type': 'integer'}
            },
        'required': [
            const.ID, const.NAME, const.DOMAIN_ID, const.PRINCIPAL_ID,
            const.PRINCIPAL_TYPE, const.ALLOW_SUB_AGREEMENTS, const.SUBAGREEMENT_NEST_LEVEL],
        # This is to verify that delegateId is not returned
        'additionalProperties': False
}

add_da = {
    'type': 'object', 'properties': {
        const.RAX_AUTH_DELEGATION_AGREEMENT: da_item
    },
    'required': [const.RAX_AUTH_DELEGATION_AGREEMENT]
}

list_da = {
    'type': 'object', 'properties': {
        const.RAX_AUTH_DELEGATION_AGREEMENTS: {
            'type': 'array', 'items': da_item, "uniqueItems": True}
    },
    'required': [const.RAX_AUTH_DELEGATION_AGREEMENTS]
}
