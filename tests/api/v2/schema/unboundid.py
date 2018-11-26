""" Schema Definitions for unboundid config list
"""
from tests.package.johny import constants as const

config_list = {
    "type": "object",
    const.PROPERTIES: {
        const.CONFIG_PATH: {
            "type": "string"
        },
        const.PROPERTIES: {
            "type": "array",
            const.ITEMS: {
                "type": "object",
                const.PROPERTIES: {
                    const.DESCRIPTION: {
                        "type": "string"
                    },
                    const.NAME: {
                        "type": "string"
                    },
                    const.VALUE: {
                        "type": ["boolean", "array", "number", "string",
                                 "null"]
                    },
                    const.VERSION_ADDED: {
                        "type": "string"
                    },
                    const.DEFAULT_VALUE: {
                        "type": ["boolean", "null", "number", "string"]
                    },
                    const.PROP_VALUE_TYPE: {
                        "type": "string"
                    },
                    const.SOURCE: {
                        "type": "string"
                    },
                    const.RELOADABLE: {
                        "type": "boolean"
                    },
                    const.AS_CONFIGURED_VALUE: {
                        "type": ["boolean", "array", "number", "string",
                                 "null"]
                    }
                },
                "required": [
                    const.DESCRIPTION,
                    const.NAME,
                    const.VALUE,
                    const.VERSION_ADDED,
                    const.SOURCE,
                    const.RELOADABLE,
                    const.AS_CONFIGURED_VALUE
                ],
            }
        },
    },
    "required": [
        const.CONFIG_PATH,
        const.PROPERTIES
    ]
}
