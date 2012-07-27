package com.rackspace.idm.api.resource.cloud.v11;

import com.rackspacecloud.docs.auth.api.v1.Credentials;
import com.rackspacecloud.docs.auth.api.v1.NastCredentials;
import org.apache.commons.lang.StringUtils;
import com.rackspace.idm.exception.BadRequestException;
import org.springframework.stereotype.Component;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 6/18/12
 * Time: 10:53 AM
 */
@Component
public class CredentialValidator {

    public void validateCredential(Credentials credential){
        if(credential instanceof NastCredentials){
            final String nastId = ((NastCredentials) credential).getNastId();
            final String key = ((NastCredentials) credential).getKey();
            if(StringUtils.isBlank(key)){
                throw new BadRequestException("Expecting apiKey");
            }
            if(StringUtils.isBlank(nastId)){
                throw new BadRequestException("Expecting nastId");
            }
        }
    }
}
