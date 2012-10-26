package com.rackspace.idm.domain.entity;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: 10/22/12
 * Time: 4:20 PM
 * To change this template use File | Settings | File Templates.
 */
public class Capabilities {
    private List<Capability> capabilities;

    public List<Capability> getCapability() {
        if (capabilities == null) {
            capabilities = new ArrayList<Capability>();
        }
        return this.capabilities;
    }
}
