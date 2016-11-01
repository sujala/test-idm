from tests.api import constants as const
from tests.api.v2.schema.users import user_item
"""Schema Definitions for Tokens Endpoints 
This module will contain the json schema definitions for all API responses
defined in
http://docs-internal.rackspace.com/auth/api/v2.0/auth-admin-devguide/content/Token_Calls.html # noqa
"""
tenant_item = {
    const.ID: {'type': 'string'},
    const.NAME: {'type': 'string'}
}

token_item = {
    const.ID: {'type': 'string'},
    const.EXPIRES: {'type': 'string'},
    const.TENANT: tenant_item,
    const.RAX_AUTH_AUTHENTICATED_BY: {
        "anyOf": const.AUTH_BY_LIST
    }
}

validate_token = {
    'type': 'object', 'properties': {
        const.ACCESS: {
            "token": token_item,
            "user": user_item
        }
    }
}
