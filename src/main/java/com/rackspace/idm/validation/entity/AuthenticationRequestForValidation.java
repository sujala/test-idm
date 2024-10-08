package com.rackspace.idm.validation.entity;

import lombok.Getter;
import lombok.Setter;

import javax.validation.Valid;
import javax.validation.constraints.Size;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.rackspace.idm.validation.entity.Constants.MAX;

@Getter
@Setter
public class AuthenticationRequestForValidation {
    @Size(max = MAX)
    private String tenantId;

    @Size(max = MAX)
    private String tenantName;

    @Valid
    private TokenForAuthenticationRequestForValidation token;

    private JAXBElementCredentialTypeForValidation credential;

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
