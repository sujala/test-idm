"""Schema Definitions for Endpoint template endpoints.

This module will contain the json schema definitions for all API responses
defined in
http://docs-internal.rackspace.com/auth/api/v2.0/auth-admin-devguide/content/Endpoint_Template_Calls.html
"""
from tests.package.johny import constants as const

add_endpoint_template = {
    'type': 'object', 'properties':
    {const.OS_KSCATALOG_ENDPOINT_TEMPLATE: {
            'type': 'object',
            'properties': {
                const.ID: {'type': 'integer'},
                const.SERVICE_NAME: {'type': 'string'},
                const.SERVICE_TYPE: {'type': 'string'},
                const.SERVICE_ID: {'type': 'string'},
                const.RAX_AUTH_ASSIGNMENT_TYPE: {
                    'type': 'string',
                    'enum': [const.ASSIGNMENT_TYPE_MOSSO,
                             const.ASSIGNMENT_TYPE_NAST,
                             const.ASSIGNMENT_TYPE_MANUAL]},
                const.DEFAULT: {'type': 'boolean'},
                const.ENABLED: {'type': 'boolean'},
                const.GLOBAL: {'type': 'boolean'},
                const.REGION: {'type': 'string'},
                const.PUBLIC_URL: {'type': 'string', 'format': 'uri'},
                const.INTERNAL_URL: {'type': 'string', 'format': 'uri'},
                const.ADMIN_URL: {'type': 'string', 'format': 'uri'},
                const.VERSION_ID: {'type': 'string'},
                const.VERSION_INFO: {'type': 'string'},
                const.VERSION_LIST: {'type': 'string'}},
            'required': [const.ID, const.SERVICE_NAME, const.SERVICE_TYPE,
                         const.REGION, const.SERVICE_ID,
                         const.RAX_AUTH_ASSIGNMENT_TYPE],
            'additionalProperties': False}},
    'required': [const.OS_KSCATALOG_ENDPOINT_TEMPLATE],
    'additionalProperties': False}

list_endpoint_template_item = {
    'type': 'object', 'properties': {
        const.ID: {'type': 'integer'},
        const.SERVICE_NAME: {'type': 'string'},
        const.SERVICE_TYPE: {'type': 'string'},
        const.SERVICE_ID: {'type': 'string'},
        const.RAX_AUTH_ASSIGNMENT_TYPE: {
            'type': 'string',
            'enum': [const.ASSIGNMENT_TYPE_MOSSO,
                     const.ASSIGNMENT_TYPE_NAST,
                     const.ASSIGNMENT_TYPE_MANUAL]},
        const.TENANT_ALIAS: {'type': 'string'},
        const.DEFAULT: {'type': 'boolean'},
        const.ENABLED: {'type': 'boolean'},
        const.GLOBAL: {'type': 'boolean'},
        const.REGION: {'type': 'string'},
        const.PUBLIC_URL: {'type': 'string', 'format': 'uri'},
        const.INTERNAL_URL: {'type': 'string', 'format': 'uri'},
        const.ADMIN_URL: {'type': 'string', 'format': 'uri'},
        const.VERSION_ID: {'type': 'string'},
        const.VERSION_INFO: {'type': 'string'},
        const.VERSION_LIST: {'type': 'string'}},

    'required': [const.ID, const.SERVICE_NAME, const.SERVICE_TYPE,
                 const.REGION, const.RAX_AUTH_ASSIGNMENT_TYPE],
    # It is expected that each template will show 'serviceId', apart from
    # the above 'required' ones. Staging has some templates not
    # having 'serviceId'. Devs are looking into it & may need a cleanup
    # script. 'global' & 'enabled' are observed on all endpoints, both in
    # local & staging env but they are not required

    'additionalProperties': False}

list_endpoint_templates = {
    'type': 'object', 'properties': {
        const.OS_KSCATALOG_ENDPOINT_TEMPLATES: {
            'type': 'array', 'items': list_endpoint_template_item}
    }
}
