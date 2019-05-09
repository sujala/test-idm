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

check_enum = [
    const.PASSED, const.FAILED, const.SKIPPED, const.DISABLED,
    const.INDETERMINATE_RETRY, const.INDETERMINATE_ERROR]

password_validation = {
    'type': 'object',
    'properties': {
        const.RAX_AUTH_VALIDATE_PASSWORD: {
            'type': 'object',
            'properties': {
                const.VALID: {
                    'type': 'string',
                    'enum': [
                        const.TRUE, const.FALSE, const.INDETERMINATE]},
                const.BLACKLIST_CHECK: {
                    'type': 'string', 'enum': check_enum},
                const.BLACKLIST_CHECK_MSG: {'type': 'string'},
                const.NON_PASSING_CHECKS: {'type': 'array'},
                const.COMPOSITION_CHECK: {
                    'type': 'string', 'enum': check_enum},
                const.COMPOSITION_CHECK_MSG: {'type': 'string'}
                },
            'additionalProperties': False}},
    'required': [const.RAX_AUTH_VALIDATE_PASSWORD],
    'additionalProperties': False}
