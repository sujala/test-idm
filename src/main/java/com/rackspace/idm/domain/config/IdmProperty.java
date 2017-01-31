package com.rackspace.idm.domain.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.StringUtils;

@Getter
@Setter
public class IdmProperty implements Comparable<IdmProperty> {
    private String id;
    private IdmPropertyType type;
    private String name;
    private String description;
    private Object value;
    private String valueType;
    private Object defaultValue;
    private String versionAdded;
    private String source;
    private boolean reloadable;

    @Override
    public int compareTo(IdmProperty o) {
        boolean thisVersionBlank = StringUtils.isBlank(versionAdded);
        boolean thatVersionBlank = StringUtils.isBlank(o.versionAdded);


        //null versions come first
        if (thisVersionBlank && !thatVersionBlank) {
            return -1;
        } else if (!thisVersionBlank && thatVersionBlank) {
            return 1;
        } else if (!thisVersionBlank && !thatVersionBlank) {
            String[] thatVersion = o.getVersionAdded().split("\\.");
            String[] thisVersion = versionAdded.split("\\.");

            //Returns a negative integer, zero, or a positive integer as this object is less than, equal to, or greater than the specified object.
            for (int i = 0; i < thisVersion.length; i++) {
                int thisVersionPart = convertVersionPartToInt(thisVersion[i]);
                int thatVersionPart = 0;
                if (thatVersion.length > i) {
                    //that version doesn't have this part. All previous matched so what to do? Assume this part is 0
                    thatVersionPart = convertVersionPartToInt(thatVersion[i]);
                }
                int comparison = Integer.compare(thisVersionPart, thatVersionPart);
                if (comparison != 0) {
                    return comparison;
                }
            }
        }

        //the versions are equal (or both blank). Order by name
        if (StringUtils.isNotBlank(name)) {
            return name.compareTo(o.getName());
        }

        //both names must be blank. Just say equal
        return 0;
    }

    private int convertVersionPartToInt(String part) {
        try {
            return Integer.valueOf(part);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

}
