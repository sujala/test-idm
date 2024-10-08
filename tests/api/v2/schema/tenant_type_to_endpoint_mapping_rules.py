"""Schema Definitions for tenant type to endpoint mapping rules endpoints.

This module will contain the json schema definitions for all API responses
defined in https://jira.rax.io/browse/CID-362
"""

from tests.package.johny import constants as const

endpoint_minimum_item = {
    'type': 'object', 'properties':
    {const.ID: {'type': 'integer'}}
}

endpoint_basic_item = {
    'type': 'object',
            'properties': {
                const.ID: {'type': 'integer'},
                const.SERVICE_NAME: {'type': 'string'},
                const.SERVICE_TYPE: {'type': 'string'},
                const.ENABLED: {'type': 'boolean'},
                const.REGION: {'type': 'string'},
                const.PUBLIC_URL: {'type': 'string', 'format': 'uri'}},
            'required': [const.ID, const.SERVICE_NAME, const.SERVICE_TYPE,
                         const.REGION, const.PUBLIC_URL,
                         const.ENABLED],
            'additionalProperties': False
}

add_tenant_type_to_endpoint_mapping_rule = {
    'type': 'object', 'properties':
    {const.NS_TENANT_TYPE_TO_ENDPOINT_MAPPING_RULE: {
            'type': 'object',
            'properties': {
                const.DESCRIPTION: {'type': 'string'},
                const.OS_KSCATALOG_ENDPOINT_TEMPLATES: {
                    'type': 'array', 'items': endpoint_minimum_item},
                const.ID: {'type': 'string'},
                const.TENANT_TYPE: {'type': 'string',
                                    'pattern': "[0-9a-z]{1,15}"}},
            'required': [const.TENANT_TYPE, const.ID],
            'additionalProperties': False}},
    'required': [const.NS_TENANT_TYPE_TO_ENDPOINT_MAPPING_RULE],
    'additionalProperties': False
}

tenant_type_to_endpoint_mapping_rule_basic = {
    'type': 'object', 'properties':
    {const.NS_TENANT_TYPE_TO_ENDPOINT_MAPPING_RULE: {
            'type': 'object',
            'properties': {
                const.DESCRIPTION: {'type': 'string'},
                const.OS_KSCATALOG_ENDPOINT_TEMPLATES: {
                    'type': 'array', 'items': endpoint_basic_item},
                const.ID: {'type': 'string'},
                const.TENANT_TYPE: {'type': 'string',
                                    'pattern': "[0-9a-z]{1,15}"}},
            'required': [const.TENANT_TYPE, const.ID],
            'additionalProperties': False}},
    'required': [const.NS_TENANT_TYPE_TO_ENDPOINT_MAPPING_RULE],
    'additionalProperties': False
}

list_tenant_type_to_endpoint_mapping_rules_item = {
    'type': 'object', 'properties': {
        const.DESCRIPTION: {'type': 'string'},
        const.ID: {'type': 'string'},
        const.TENANT_TYPE: {'type': 'string',
                                    'pattern': "[0-9a-z]{1,15}"}},
            'required': [const.TENANT_TYPE, const.ID],
            'additionalProperties': False
}

list_tenant_type_to_endpoint_mapping_rules = {
    'type': 'object', 'properties':
    {
            const.RAX_AUTH_ENDPOINT_ASSIGNMENT_RULES:
            {
                'type': 'object', 'properties': {
                    const.TENANT_TYPE_TO_ENDPOINT_MAPPING_RULES: {
                        'type': 'array',
                        'items': (
                            list_tenant_type_to_endpoint_mapping_rules_item)
                    }
                },
                'required': [const.TENANT_TYPE_TO_ENDPOINT_MAPPING_RULES]
            }
    },
    'required': [const.RAX_AUTH_ENDPOINT_ASSIGNMENT_RULES],
    'additionalProperties': False
}

list_dummy_tenant_type_to_endpoint_mapping_rules_item = {
    'type': 'object', 'properties':
    {const.NS_TENANT_TYPE_TO_ENDPOINT_MAPPING_RULE: {
            'type': 'object',
            'properties': {
                const.DESCRIPTION: {'type': 'string'},
                const.ID: {'type': 'string'},
                const.TENANT_TYPE: {'type': 'string',
                                    'pattern': "[0-9a-z]{1,15}"}},
            'required': [const.TENANT_TYPE, const.ID],
            'additionalProperties': False}},
    'required': [const.NS_TENANT_TYPE_TO_ENDPOINT_MAPPING_RULE],
    'additionalProperties': False
}
