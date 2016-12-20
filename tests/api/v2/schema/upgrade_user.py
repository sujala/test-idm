"""Schema Definition for upgradeUserToCloud."""
from tests.package.johny import constants as const

upgrade_user_to_cloud = {
    'type': 'object', 'properties':
    {const.USER: {
            'type': 'object',
            'properties': {
                const.RAX_AUTH_DOMAIN_ID: {'type': 'string'},
                const.ID: {'type': 'string'},
                const.ENABLED: {'type': 'boolean'},
                const.USERNAME: {'type': 'string'},
                const.EMAIL: {'type': 'string', 'format': 'email'},
                const.ROLES: {'type': 'array'},
                const.NS_GROUPS: {'type': 'array'},
                const.RAX_AUTH_MULTI_FACTOR_ENABLED: {'type': 'boolean'},
                const.NS_SECRETQA: {'type': 'object', 'properties': {
                    const.SECRET_QUESTION: {'type': 'string'},
                    const.SECRET_ANSWER: {'type': 'string'},
                }, 'required': [const.SECRET_QUESTION, const.SECRET_ANSWER],
                   'additionalProperties': False},
                const.RAX_AUTH_DEFAULT_REGION: {
                    'type': 'string',
                    'enum': const.DC_LIST}},
            'required': [const.RAX_AUTH_DOMAIN_ID, const.ID, const.ENABLED,
                         const.USERNAME, const.EMAIL,
                         const.RAX_AUTH_MULTI_FACTOR_ENABLED,
                         const.RAX_AUTH_DEFAULT_REGION],
            'additionalProperties': False}},
    'required': [const.USER],
    'additionalProperties': False}
