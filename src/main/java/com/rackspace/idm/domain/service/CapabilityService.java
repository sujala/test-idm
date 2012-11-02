package com.rackspace.idm.domain.service;

import com.rackspace.idm.domain.entity.Capabilities;
import com.rackspace.idm.domain.entity.Capability;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: 10/24/12
 * Time: 12:45 PM
 * To change this template use File | Settings | File Templates.
 */
public interface CapabilityService {
    void updateCapabilities(List<Capability> capabilities, String type, String version);
    List<Capability> getCapabilities(String type, String version);
    void removeCapabilities(String type, String version);
}
