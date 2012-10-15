package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.Policy;
import com.rackspace.idm.exception.BadRequestException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.CharUtils;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: 10/15/12
 * Time: 3:12 PM
 * To change this template use File | Settings | File Templates.
 */
@Component
public class PolicyValidator {

    public void validatePolicyName(String policyName) {
        String name = "";
        if (policyName != null) {
            name = policyName.trim();
        }
        //alpha numberic with spaces
        if (StringUtils.isEmpty(name)) {
            throw new BadRequestException("Policy name cannot be empty.");
        }

        Pattern alphaNumberic = Pattern.compile("[a-zA-Z0-9 ]*");
        if (!alphaNumberic.matcher(name).matches()) {
            throw new BadRequestException("Policy name has invalid characters; only alphanumeric characters with spaces are allowed.");
        }

        if (!CharUtils.isAsciiAlpha(name.charAt(0))) {
            throw new BadRequestException("Policy name must begin with an alphabetic character.");
        }
    }

}
