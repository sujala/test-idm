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

password_validation = {
    'type': 'object',
    'properties': {
        const.RAX_AUTH_VALIDATE_PASSWORD: {
            'type': 'object',
            'properties': {
                const.VALID: {'type': 'string', 'enum': ['TRUE', 'FALSE']},
                const.BLACKLIST_CHECK: {'type': 'string'},
                const.BLACKLIST_CHECK_MSG: {'type': 'string'},
                const.NON_PASSING_CHECKS: {'type': 'array'},
                const.COMPOSITION_CHECK: {'type': 'string'},
                const.COMPOSITION_CHECK_MSG: {'type': 'string'}
                },
            'additionalProperties': False}},
    'required': [const.RAX_AUTH_VALIDATE_PASSWORD],
    'additionalProperties': False}
