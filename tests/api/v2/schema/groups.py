""" Schema Definitions for groups end points
"""
from tests.api import constants as const

add_group = {
    'type': 'object', 'properties': {
        const.NS_GROUP: {
            'type': 'object', 'properties': {
                const.ID: {'type': 'string'},
                const.NAME: {'type': 'string'},
                const.DESCRIPTION: {'type': 'string'}},
            'required': [const.ID, const.NAME, const.DESCRIPTION],
            'additionalProperties': False}},
    'required': [const.NS_GROUP],
    'additionalProperties': False}
