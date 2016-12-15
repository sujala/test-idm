""" Schema Definitions for unboundid config list
"""
from tests.package.johny import constants as const

config_list = {
    "type": "object",
    "properties": {
        const.IDM_RELOADABLE_PROPERTIES: {
            "type": "array",
            const.ITEMS: {
                "type": "object",
                "properties": {
                    const.DESCRIPTION: {
                        "type": "string"
                    },
                    const.NAME: {
                        "type": "string"
                    },
                    const.VALUE: {
                        "type": ["boolean", "array", "number", "string"]
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
                    const.VERSION_ADDED,
                    const.DEFAULT_VALUE
                ]
            }
        },
        const.CONFIG_PATH: {
            "type": "string"
        },
        const.IDM_PROPERTIES: {
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
                        "type": ["boolean", "array", "number", "string"]
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
                    const.VERSION_ADDED,
                    const.DEFAULT_VALUE
                ]
            }
        }
    },
    "required": [
        const.IDM_RELOADABLE_PROPERTIES,
        const.CONFIG_PATH,
        const.IDM_PROPERTIES
    ]
}
