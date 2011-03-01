package com.rackspace.idm.api.converter;

import java.util.ArrayList;
import java.util.List;

import com.rackspace.idm.domain.entity.ClientGroup;
import com.rackspace.idm.jaxb.ObjectFactory;

public class GroupConverter {
    protected ObjectFactory of = new ObjectFactory();
    
    public ClientGroup toClientGroupDO(com.rackspace.idm.jaxb.ClientGroup jaxbGroup) {
        ClientGroup group = new ClientGroup();
        group.setName(jaxbGroup.getName());
        group.setClientId(jaxbGroup.getClientId());
        group.setCustomerId(jaxbGroup.getCustomerId());
        return group;
    }
    
    public List<ClientGroup> toClientGroupListDO(com.rackspace.idm.jaxb.ClientGroups jaxbGroups) {
        List<ClientGroup> groups = new ArrayList<ClientGroup>();

        for (com.rackspace.idm.jaxb.ClientGroup jaxbGroup : jaxbGroups.getClientGroups()) {
            groups.add(toClientGroupDO(jaxbGroup));
        }

        return groups;
    }

    public com.rackspace.idm.jaxb.ClientGroup toClientGroupJaxb(ClientGroup group) {
        com.rackspace.idm.jaxb.ClientGroup jaxbGroup = of.createClientGroup();
        
        if (group == null) {
            return null;
        }
        
        jaxbGroup.setClientId(group.getClientId());
        jaxbGroup.setCustomerId(group.getCustomerId());
        jaxbGroup.setName(group.getName());
        return jaxbGroup;
    }
    
    public com.rackspace.idm.jaxb.ClientGroups toClientGroupsJaxb(List<ClientGroup> groups) {
        com.rackspace.idm.jaxb.ClientGroups jaxbGroups = of.createClientGroups();
        
        if (groups == null || groups.size() == 0) {
            return jaxbGroups;
        }
        
        for (ClientGroup g : groups) {
            jaxbGroups.getClientGroups().add(toClientGroupJaxb(g));
        }
        
        return jaxbGroups;
    }
}
