package com.rackspace.idm.validation.entity;

import lombok.Data;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 2/13/13
 * Time: 5:41 PM
 * To change this template use File | Settings | File Templates.
 */
@Data
public class DefaultRegionServicesForValidation {
    protected List<String> serviceName;

    @Valid
    public List<StringForValidation> getServiceNames() {
        List<StringForValidation> result = new ArrayList<StringForValidation>();
        if (serviceName != null) {
            for (String serviceNameItem : serviceName) {
                result.add(new StringForValidation(serviceNameItem));
            }
        }
        return result;
    }
}
