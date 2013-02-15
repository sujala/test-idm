package com.rackspace.idm.validation.entity;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.Size;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.rackspace.idm.validation.entity.Constants.*;

@Data
public class AuthenticationRequestForValidation {
    @Size(max = MAX)
    protected String tenantId;

    @Size(max = MAX)
    protected String tenantName;

    @Valid
    protected TokenForAuthenticationRequestForValidation token;

    protected JAXBElementCredentialTypeForValidation credential;

    private Map<QName, String> otherAttributes = new HashMap<QName, String>();

    @Valid
    public List<StringForValidation> getAttribute(){
        List<StringForValidation> attributes = new ArrayList<StringForValidation>();
        for(String value : otherAttributes.values()){
            StringForValidation attribute = new StringForValidation();
            attribute.setValue(value);
            attributes.add(attribute);
        }
        return attributes;
    }

    @Valid
    public CredentialTypeForValidation getCredentialType(){
        CredentialTypeForValidation result = null;
        if (credential != null) {
            result = credential.getValue();
        }

        return result;
    }

}
