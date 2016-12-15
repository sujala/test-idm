""" Schema Definitions for groups end points. """
from tests.package.johny import constants as const

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

list_groups = {
    'type': 'object', 'properties': {
        const.NS_GROUPS: {
            const.ITEMS: {
                'type': 'object', 'properties': {
                    const.NAME: {'type': 'string'},
                    const.DESCRIPTION: {'type': 'string'},
                    const.ID: {'type': 'string'}},
                'required': [const.NAME, const.DESCRIPTION, const.ID],
                'additionalProperties': False}},
    }, 'required': [const.NS_GROUPS], 'additionalProperties': False}
