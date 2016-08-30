from tests.api import constants as const

"""Schema Definitions for Users endpoints.

This module will contain the json schema definitions for all API responses
defined in
http://docs-internal.rackspace.com/auth/api/v2.0/auth-admin-devguide/content/User_Calls.html # noqa
"""

add_user = {
    'type': 'object', 'properties':
    {'user': {
            'type': 'object',
            'properties': {
                'username': {'type': 'string'},
                'email': {'type': 'string', 'format': 'email'},
                'RAX-AUTH:multiFactorEnabled': {'type': 'boolean'},
                'enabled': {'type': 'boolean'},
                'RAX-AUTH:defaultRegion': {
                    'type': 'string',
                    'enum': ['DFW', 'SYD', 'IAD', 'HKG', 'LON', 'ORD']},
                'password': {'type': 'string'},
                'id': {'type': 'string'},
                'RAX-AUTH:domainId': {'type': 'string'},
                'OS-KSADM:password': {'type': 'string'}},
            'required': ['username', 'enabled', 'RAX-AUTH:defaultRegion', 'id',
                         'RAX-AUTH:domainId', 'RAX-AUTH:multiFactorEnabled'],
            'additionalProperties': False}},
    'required': ['user'],
    'additionalProperties': False}

# Response schema differs from the response documented in
# https://developer.rackspace.com/docs/cloud-identity/v2/
# developer-guide/#get-user-by-id
get_user = {
    'type': 'object', 'properties':
    {'user': {
            'type': 'object',
            'properties': {
                'username': {'type': 'string'},
                'email': {'type': 'string', 'format': 'email'},
                'RAX-AUTH:multiFactorEnabled': {'type': 'boolean'},
                'enabled': {'type': 'boolean'},
                'RAX-AUTH:domainId': {'type': 'string'},
                'RAX-AUTH:contactId': {'type': 'string'},
                'RAX-AUTH:defaultRegion': {
                    'type': 'string',
                    'enum': ['DFW', 'SYD', 'IAD', 'HKG', 'LON', 'ORD']},
                'id': {'type': 'string'},
                'created': {'type': 'string', 'format': 'dateTime'},
                'updated': {'type': 'string', 'format': 'dateTime'}},
            'required': ['username', 'id', 'email', 'enabled',
                         'RAX-AUTH:defaultRegion', 'RAX-AUTH:domainId',
                         'RAX-AUTH:multiFactorEnabled'],
            'additionalProperties': False}},
    'required': ['user'],
    'additionalProperties': False}

update_user = {
    'type': 'object', 'properties':
    {const.USER: {
            'type': 'object',
            'properties': {
                const.USERNAME: {'type': 'string'},
                const.EMAIL: {'type': 'string', 'format': 'email'},
                const.RAX_AUTH_MULTI_FACTOR_ENABLED: {'type': 'boolean'},
                const.ENABLED: {'type': 'boolean'},
                const.RAX_AUTH_DOMAIN: {'type': 'string'},
                const.RAX_AUTH_CONTACTID: {'type': 'string'},
                const.RAX_AUTH_DEFAULT_REGION: {
                    'type': 'string',
                    'enum': ['DFW', 'SYD', 'IAD', 'HKG', 'LON', 'ORD']},
                const.ID: {'type': 'string'},
                const.CREATED: {'type': 'string', 'format': 'dateTime'},
                const.UPDATED: {'type': 'string', 'format': 'dateTime'}},
            'required': [const.USERNAME, const.ID, const.ENABLED,
                         const.RAX_AUTH_DEFAULT_REGION, const.RAX_AUTH_DOMAIN,
                         const.RAX_AUTH_MULTI_FACTOR_ENABLED,
                         const.CREATED, const.UPDATED],
            'additionalProperties': False}},
    'required': [const.USER],
    'additionalProperties': False}
