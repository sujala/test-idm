""" Schema Definitions for roles end points
"""
from tests.api import constants as const

add_role = {
    'type': 'object', 'properties': {
        const.ROLE: {
            'type': 'object', 'properties': {
                const.ID: {'type': 'string'},
                const.NAME: {'type': 'string'},
                const.DESCRIPTION: {'type': 'string'},
                const.SERVICE_ID: {'type': 'string'},
                const.NS_ADMINISTRATOR_ROLE: {'type': 'string'},
                const.NS_PROPAGATE: {'type': 'boolean'}},
            'required': [const.ID, const.NAME, const.DESCRIPTION],
            'additionalProperties': False}
        }, 'required': [const.ROLE], 'additionalProperties': False}
