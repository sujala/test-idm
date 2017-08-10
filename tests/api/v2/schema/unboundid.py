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
                    }
                },
                "required": [
                    const.DESCRIPTION,
                    const.NAME,
                    const.VALUE,
                    const.VERSION_ADDED
                ]
            }
        },
    },
    "required": [
        const.CONFIG_PATH,
        const.PROPERTIES
    ]
}
