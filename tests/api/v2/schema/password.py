"""Schema definition for Password Policy."""

from tests.package.johny import constants as const

password_policy = {
    'type': 'object',
    'properties': {
        const.PASSWORD_POLICY: {
            'type': 'object',
            'properties': {
                const.PASSWORD_HISTORY_RESTRICTION: {'type': 'number'},
                const.PASSWORD_DURATION: {'type': 'string'}},
            'additionalProperties': False}},
    'required': [const.PASSWORD_POLICY],
    'additionalProperties': False}
