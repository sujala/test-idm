"""Schema Definitions for Tenants endpoints.

This module will contain the json schema definitions for all API responses
defined in
http://docs-internal.rackspace.com/auth/api/v2.0/auth-admin-devguide/content/Tenant_Calls.html
"""
import copy
from tests.api import constants as const

tenant_item = {
    'type': 'object',
    'properties': {
        const.NAME: {'type': 'string'},
        const.DESCRIPTION: {'type': 'string'},
        const.ENABLED: {'type': 'boolean'},
        const.ID: {'type': 'string'},
        const.RAX_AUTH_DOMAIN_ID: {'type': 'string'}},
    'required': [const.NAME, const.ENABLED, const.ID,
                 const.RAX_AUTH_DOMAIN_ID],
    'additionalProperties': False}

add_tenant = {
    'type': 'object', 'properties': {const.TENANT: tenant_item},
    'required': [const.TENANT],
    'additionalProperties': False}

update_tenant_item = copy.deepcopy(tenant_item)
update_tenant_item['required'] = [
    const.ENABLED, const.ID, const.RAX_AUTH_DOMAIN_ID]

update_tenant = {
    'type': 'object', 'properties': {const.TENANT: update_tenant_item},
    'required': [const.TENANT],
    'additionalProperties': False}

list_tenants = {
    'type': 'object', 'properties':
    {const.TENANTS: {'type': 'array', 'items': tenant_item}},
    'required': [const.TENANTS],
    'additionalProperties': False}
