package com.rackspace.idm.modules.endpointassignment.api.resource;

import org.apache.commons.lang.StringUtils;

/**
 * Constants for controlling the level of detail to return for a rule.
 */
public enum RuleDetailLevelEnum {
    MINIMUM("minimum"), BASIC("basic");

    private String level;

    RuleDetailLevelEnum(String level) {
        this.level = level;
    }

    public String getLevel() {
        return level;
    }


    public static RuleDetailLevelEnum fromString(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        for (RuleDetailLevelEnum ruleDetailLevelEnum : values()) {
            if (ruleDetailLevelEnum.level.equalsIgnoreCase(value)) {
                return ruleDetailLevelEnum;
            }
        }
        return null;
    }

}
