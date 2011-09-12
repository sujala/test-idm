package com.rackspace.idm.api.converter;

import java.util.ArrayList;
import java.util.List;

import com.rackspace.api.idm.v1.ObjectFactory;
import com.rackspace.idm.domain.entity.ClientGroup;

public class GroupConverter {
    private final ObjectFactory of = new ObjectFactory();
    
    public ClientGroup toClientGroupDO(com.rackspace.api.idm.v1.ClientGroup jaxbGroup) {
        ClientGroup group = new ClientGroup();
        group.setName(jaxbGroup.getName());
        group.setClientId(jaxbGroup.getClientId());
        group.setCustomerId(jaxbGroup.getCustomerId());
        group.setType(jaxbGroup.getType());
        return group;
    }
    
    public List<ClientGroup> toClientGroupListDO(com.rackspace.api.idm.v1.ClientGroups jaxbGroups) {
        List<ClientGroup> groups = new ArrayList<ClientGroup>();

        for (com.rackspace.api.idm.v1.ClientGroup jaxbGroup : jaxbGroups.getClientGroups()) {
            groups.add(toClientGroupDO(jaxbGroup));
        }

        return groups;
    }

    public com.rackspace.api.idm.v1.ClientGroup toClientGroupJaxb(ClientGroup group) {
        com.rackspace.api.idm.v1.ClientGroup jaxbGroup = of.createClientGroup();
        
        if (group == null) {
            return null;
        }
        
        jaxbGroup.setClientId(group.getClientId());
        jaxbGroup.setCustomerId(group.getCustomerId());
        jaxbGroup.setName(group.getName());
        jaxbGroup.setType(group.getType());
        return jaxbGroup;
    }
    
    public com.rackspace.api.idm.v1.ClientGroups toClientGroupsJaxb(List<ClientGroup> groups) {
        com.rackspace.api.idm.v1.ClientGroups jaxbGroups = of.createClientGroups();
        
        if (groups == null || groups.size() == 0) {
            return jaxbGroups;
        }
        
        for (ClientGroup g : groups) {
            jaxbGroups.getClientGroups().add(toClientGroupJaxb(g));
        }
        
        return jaxbGroups;
    }
}
