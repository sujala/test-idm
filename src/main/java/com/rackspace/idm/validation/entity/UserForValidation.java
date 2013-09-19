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

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 2/13/13
 * Time: 5:51 PM
 * To change this template use File | Settings | File Templates.
 */
@Getter
@Setter
public class UserForValidation {
    @Size(max = MAX)
    private String id;
    @Size(max = MAX)
    private String username;
    @Size(max = MAX)
    private String email;
    @Size(max = MAX)
    private String displayName;
    @Size(max = MAX)
    private String password;
    @Size(max = MAX)
    private String nastId;
    @Size(max = MAX)
    private String key;
    @Valid
    private BaseUrlRefListForValidation baseURLRefs;

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

}
