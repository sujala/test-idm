"""Schema Definitions for IDP Responses.

This module will contain the json schema definitions for all API responses
defined in
http://docs-internal.rackspace.com/auth/api/v2.0/auth-admin-devguide/content/Tenant_Calls.html
"""
from tests.package.johny import constants as const

CERTIFICATE_ITEM = {
    'type': 'object',
    'properties': {
        const.ID: {'type': 'string'},
        const.PEM_ENCODED: {'type': 'string'}
    }}

idp_item = {
    'type': 'object',
    'properties': {
        const.DESCRIPTION: {'type': 'string'},
        const.APPROVED_DOMAIN_Ids: {
            'type': 'array',
            'items': {'type': 'string'},
            "maxItems": 1},
        const.AUTHENTICATION_URL: {'type': 'string'},
        const.ID: {'type': 'string'},
        const.FEDERATION_TYPE: {
            'type': 'string',
            'enum': ['DOMAIN', const.BROKER]},
        const.ISSUER: {'type': 'string'}},
    'required': [
        const.DESCRIPTION, const.APPROVED_DOMAIN_Ids,
        const.AUTHENTICATION_URL, const.ID, const.FEDERATION_TYPE,
        const.ISSUER, const.NAME]
}

identity_provider = {
    'type': 'object',
    'properties': {
        const.NS_IDENTITY_PROVIDER: {
            'type': 'object',
            'properties': {
                const.DESCRIPTION: {'type': 'string'},
                const.APPROVED_DOMAIN_Ids: {
                    'type': 'array',
                    'items': {'type': 'string'},
                    "maxItems": 1},
                const.AUTHENTICATION_URL: {'type': 'string'},
                const.ID: {'type': 'string'},
                const.FEDERATION_TYPE: {
                    'type': 'string',
                    'enum': ['DOMAIN', const.BROKER]},
                const.ISSUER: {'type': 'string'},
                const.PUBLIC_CERTIFICATES: {
                    'type': 'array',
                    'items': CERTIFICATE_ITEM},
                const.NAME: {'type': 'string'}},
            'required': [
                const.DESCRIPTION, const.APPROVED_DOMAIN_Ids,
                const.AUTHENTICATION_URL, const.ID, const.FEDERATION_TYPE,
                const.ISSUER, const.PUBLIC_CERTIFICATES, const.NAME]
        }},
    'required': [const.NS_IDENTITY_PROVIDER],
    'additionalProperties': False}

list_idps = {
    'type': 'object', 'properties':
    {const.NS_IDENTITY_PROVIDERS:
        {'type': 'array', 'items': idp_item}},
    'required': [const.NS_IDENTITY_PROVIDERS],
    'additionalProperties': False}
