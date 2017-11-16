package com.rackspace.idm.domain.service;

import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * The defined 'user types' within identity. Every end user within identity is classified as one of these user types.
 */
public enum RoleLevelEnum {
    LEVEL_0(0)
    , LEVEL_50(50)
    , LEVEL_100(100)
    , LEVEL_500(500)
    , LEVEL_750(750)
    , LEVEL_900(900)
    , LEVEL_1000(1000)
    , LEVEL_2000(2000);

    private int levelAsInt; //weight

    private RoleLevelEnum(int levelAsInt) {
        this.levelAsInt = levelAsInt;
    }


    public int getLevelAsInt() {
        return levelAsInt;
    }

    /**
     * Returns whether the provided role has a higher weight than this role.
     * @param that
     * @return
     */
    public boolean isLowerWeightThan(RoleLevelEnum that) {
        if (that == null) {
            return true;
        }
        return getLevelAsInt() < that.levelAsInt;
    }

    public static RoleLevelEnum fromInt(int level) {
        for (RoleLevelEnum roleLevelEnum : values()) {
            if (roleLevelEnum.levelAsInt == level) {
                return roleLevelEnum;
            }
        }
        return null;
    }
}
