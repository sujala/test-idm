package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.dao.impl.LdapCapabilityRepository;
import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.rackspace.idm.domain.entity.Capabilities;
import com.rackspace.idm.domain.entity.Capability;
import com.rackspace.idm.domain.service.CapabilityService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.exception.NotFoundException;
import com.unboundid.ldap.sdk.Filter;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: 10/24/12
 * Time: 12:52 PM
 * To change this template use File | Settings | File Templates.
 */
@Component
public class DefaultCapabilityService extends LdapRepository implements CapabilityService {

    @Autowired
    private LdapCapabilityRepository ldapCapabilityRepository;

    @Override
    public void updateCapabilities(List<Capability> capabilities, String type, String version) {
        if(capabilities == null || capabilities.size() == 0){
            throw new BadRequestException("Capabilities cannot be null or empty.");
        }
        if(StringUtils.isBlank(type)){
            throw new BadRequestException("Capabilities type cannot be null or empty");
        }
        if(StringUtils.isBlank(version)){
            throw new BadRequestException("Capabilities version cannot be null or empty");
        }
        validateCapabilities(capabilities, type, version);
        for(Capability capability : capabilities){
            capability.setRsId(ldapCapabilityRepository.getNextGroupId());
            ldapCapabilityRepository.addObject(capability);
        }
    }

    private void validateCapabilities(List<Capability> capabilities, String type, String version) {
        for (Capability capability : capabilities) {
            if (StringUtils.isBlank(capability.getId())) {
                throw new BadRequestException("Capability Id cannot be empty.");
            }
            if (StringUtils.isBlank(capability.getAction())) {
                String errMsg = String.format("Capability %s is missing action.", capability.getId());
                throw new BadRequestException(errMsg);
            }
            if (StringUtils.isBlank(capability.getName())) {
                String errMsg = String.format("Capability %s is missing name.", capability.getId());
                throw new BadRequestException(errMsg);
            }
            if (StringUtils.isBlank(capability.getUrl())) {
                String errMsg = String.format("Capability %s is missing url.", capability.getId());
                throw new BadRequestException(errMsg);
            }
            if (StringUtils.isBlank(capability.getDescription())) {
                capability.setDescription(null);
            }
            if (capability.getResources() == null || capability.getResources().isEmpty()) {
                capability.setResources(null);
            }
            capability.setType(type);
            capability.setVersion(version);
            Capability exist = ldapCapabilityRepository.getObject(createCapabilityFilter(capability.getId(), capability.getType(), capability.getVersion()));
            if(exist != null){
                String errMsg = String.format("Capability with id: %s, version: %s, and type: %s already exist."
                        ,exist.getId(), exist.getVersion(), exist.getType());
                throw new DuplicateException(errMsg);
            }
        }
        checkDuplicatesInCapabilities(capabilities);
    }

    private void checkDuplicatesInCapabilities(List<Capability> capabilities) {
        Capabilities noDup = new Capabilities();
        for(Capability capability : capabilities){
            if(!noDup.getCapability().contains(capability)){
                noDup.getCapability().add(capability);
            }else{
                String errMsg = String.format("Capability with id: %s, version: %s, and type: %s must be unique."
                        ,capability.getId() , capability.getVersion(), capability.getType());
                throw new DuplicateException(errMsg);
            }
        }
    }

    @Override
    public List<Capability> getCapabilities(String type, String version) {
        if(StringUtils.isBlank(version)){
            throw new BadRequestException("Capability's version cannot be null.");
        }
        if(StringUtils.isBlank(type)){
            throw new BadRequestException("Capability's type cannot be null.");
        }
        return ldapCapabilityRepository.getObjects(createCapabilitiesFilter(type, version));
    }

    @Override
    public void removeCapabilities(String type, String version) {
        if(StringUtils.isBlank(version)){
            throw new BadRequestException("Capability's version cannot be null.");
        }
        if(StringUtils.isBlank(type)){
            throw new BadRequestException("Capability's type cannot be null.");
        }
        List<Capability> capabilityList = ldapCapabilityRepository.getObjects(createCapabilitiesFilter(type, version));
        for(Capability capability : capabilityList){
            ldapCapabilityRepository.deleteObject(createCapabilityFilter(capability.getId(), capability.getType(), capability.getVersion()));
        }
    }

    private Filter createCapabilityFilter(String capabilityId, String type, String version) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_CAPABILITY_ID, capabilityId)
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_CAPABILITY)
                .addEqualAttribute(ATTR_VERSION_ID, version)
                .addEqualAttribute(ATTR_OPENSTACK_TYPE, type).build();
    }

    private Filter createCapabilitiesFilter(String type, String version) {
        Filter filter = new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_CAPABILITY)
                .addEqualAttribute(ATTR_VERSION_ID, version)
                .addEqualAttribute(ATTR_OPENSTACK_TYPE, type).build();
        return filter;
    }
}
