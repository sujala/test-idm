package com.rackspace.idm.event;

import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.Set;

public enum NewRelicCustomAttributesEnum {
    //COMMON
    REQUEST_ID("requestId")
    , RESOURCE_TYPE("resourceType")
    , RESOURCE_NAME("resourceName")
    , RESOURCE_PATH("resourcePath")
    , KEYWORDS("keyWords")
    , NODE_NAME("nodeName")
    , REMOTE_IP("remoteIp")
    , FORWARDED_IP("forwardedForIp")
    , QUERY_PARAMS("queryParams")
    , IDM_VERSION("idmVersion")

    // AUTH and PRIVATE RESOURCES
    , CALLER_USERNAME("callerUsername")

    // PRIVATE RESOURCES ONLY
    , CALLER_TOKEN("callerToken")
    , CALLER_ID("callerId")
    , CALLER_USER_TYPE("callerUserType")
    , EFFECTIVE_CALLER_TOKEN("effectiveCallerToken")
    , EFFECTIVE_CALLER_ID("effectiveCallerId")
    , EFFECTIVE_CALLER_USERNAME("effectiveCallerUsername")
    , EFFECTIVE_CALLER_USER_TYPE("effectiveCallerUserType")
    ;

    private static String WILDCARD = "*";

    @Getter
    private String newRelicAttributeName;

    NewRelicCustomAttributesEnum(String newRelicAttributeName) {
        this.newRelicAttributeName = newRelicAttributeName;
    }

    public static NewRelicCustomAttributesEnum fromNewRelicAttributeName(String val) {
        for (NewRelicCustomAttributesEnum newRelicCustomAttributesEnum : values()) {
            if (newRelicCustomAttributesEnum.newRelicAttributeName.equalsIgnoreCase(val)) {
                return newRelicCustomAttributesEnum;
            }
        }
        return null;
    }

    public static boolean doesListIncludeNewRelicAttributeWithWildcardSupport(String newRelicAttributeName, Set<String> attributeList) {
        NewRelicCustomAttributesEnum nrEnum = fromNewRelicAttributeName(newRelicAttributeName);
        return doesListIncludeNewRelicAttributeWithWildcardSupport(nrEnum, attributeList);
    }

    public static boolean doesListIncludeNewRelicAttributeWithWildcardSupport(NewRelicCustomAttributesEnum nrEnum, Set<String> attributeList) {
        if (nrEnum == null) {
            return false;
        } else {
            return nrEnum.amIInListWithWildcardSupport(attributeList);
        }
    }

    public boolean amIInListWithWildcardSupport(Set<String> attributeList) {
        boolean found = false;
        if (CollectionUtils.isEmpty(attributeList)) {
            found = false;
        } else if (attributeList.contains(WILDCARD)) {
            found = true;
        } else {
            for (String attributeInList : attributeList) {
                if (newRelicAttributeName.equalsIgnoreCase(attributeInList)) {
                    found = true;
                    break;
                }
            }
        }

        return found;
    }
}
