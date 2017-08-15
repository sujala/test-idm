package com.rackspace.idm.domain.service;

import com.rackspace.idm.GlobalConstants;
import org.apache.commons.lang.StringUtils;

import javax.ws.rs.core.MediaType;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/*
    List of acceptable formats for IDP's mapping policy.
 */
public enum IdpPolicyFormatEnum {
    JSON, XML, YAML;

    public static IdpPolicyFormatEnum fromValue(String mediaType) {
        if(StringUtils.isBlank(mediaType)) {
            return null;
        } else if (mediaType.equalsIgnoreCase(GlobalConstants.TEXT_YAML)) {
            return IdpPolicyFormatEnum.YAML;
        } else if (mediaType.equalsIgnoreCase(MediaType.APPLICATION_JSON)) {
            return IdpPolicyFormatEnum.JSON;
        } else if (mediaType.equalsIgnoreCase(MediaType.APPLICATION_XML)) {
            return IdpPolicyFormatEnum.XML;
        }

        return null;
    }

    public static Set<String> fromMediaTypes(List<MediaType> mediaTypes) {
        Set<String> idpPolicyFormatEnumList = new HashSet<>();
        for (MediaType mediaType : mediaTypes) {
            IdpPolicyFormatEnum idpPolicyFormatEnum = fromValue(mediaType.toString());
            if (idpPolicyFormatEnum != null) {
                idpPolicyFormatEnumList.add(idpPolicyFormatEnum.name());
            }
        }
        return idpPolicyFormatEnumList;
    }
}
