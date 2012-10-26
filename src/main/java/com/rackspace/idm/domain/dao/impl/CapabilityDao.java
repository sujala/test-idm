package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.domain.entity.Capabilities;
import com.rackspace.idm.domain.entity.Capability;

/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: 10/22/12
 * Time: 3:51 PM
 * To change this template use File | Settings | File Templates.
 */
public interface CapabilityDao {
    Capability getCapability(String capabilityId, String endpointTemplateId);
    Capabilities getCapabilities(String endpointTemplateId);
    void updateCapabilities(String endpointTemplateId, Capabilities capabilities);
    void removeCapabilities(String endpointTemplateId);
}
