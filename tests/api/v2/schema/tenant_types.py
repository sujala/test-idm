"""Schema Definitions for tenant types.

This module will contain the json schema definitions for all API responses
defined in https://jira.rax.io/browse/CID-782
"""

from tests.package.johny import constants as const

tenant_type_item = {
            'type': 'object',
            'properties': {
                const.NAME: {'type': 'string'},
                const.DESCRIPTION: {'type': 'string'}},
            'required': [const.NAME, const.DESCRIPTION],
            'additionalProperties': False
}

add_tenant_type = {
    'type': 'object', 'properties': {
        const.RAX_AUTH_TENANT_TYPE: tenant_type_item},
    'required': [const.RAX_AUTH_TENANT_TYPE],
    'additionalProperties': False
}

get_tenant_type = add_tenant_type

list_tenant_types = {
    'type': 'object', 'properties':
        {
            const.RAX_AUTH_TENANT_TYPES: {
                'type': 'array', 'items': tenant_type_item}
        }
}
