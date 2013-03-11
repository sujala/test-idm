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

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 2/13/13
 * Time: 3:14 PM
 * To change this template use File | Settings | File Templates.
 */
@Data
public class RoleForValidation {
    @Size(max = MAX)
    protected String id;
    @Size(max = MAX)
    protected String name;
    @Size(max = MAX)
    protected String serviceId;
    @Size(max = MAX)
    protected String tenantId;
    @Size(max = LONG_MAX)
    protected String description;

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
