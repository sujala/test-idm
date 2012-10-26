package com.rackspace.idm.domain.service;

import com.rackspace.idm.domain.entity.Capabilities;
import com.rackspace.idm.domain.entity.Capability;

/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: 10/24/12
 * Time: 12:45 PM
 * To change this template use File | Settings | File Templates.
 */
public interface CapabilityService {
    void updateCapabilities(String endpointTemplateId, Capabilities capabilities);
    Capabilities getCapabilities(String endpointTemplateId);
    Capability getCapability(String capabilityId, String endpointTemplateId);
    void removeCapabilities(String endpointTemplateId);
}
