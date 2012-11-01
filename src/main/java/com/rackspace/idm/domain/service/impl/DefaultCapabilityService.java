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
    public void updateCapabilities(Capabilities capabilities) {
        if(capabilities == null || capabilities.getCapability().size() == 0){
            throw new BadRequestException("Capabilities cannot be null or empty.");
        }
        validateCapabilities(capabilities);
        for(Capability capability : capabilities.getCapability()){
            capability.setRsId(ldapCapabilityRepository.getNextGroupId());
            ldapCapabilityRepository.addObject(capability);
        }
    }

    private void validateCapabilities(Capabilities capabilities) {
        for (Capability capability : capabilities.getCapability()) {
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
            if (StringUtils.isBlank(capability.getVersion())) {
                String errMsg = String.format("Capability %s is missing version.", capability.getId());
                throw new BadRequestException(errMsg);
            }
            if (StringUtils.isBlank(capability.getType())) {
                String errMsg = String.format("Capability %s is missing openStackType.", capability.getId());
                throw new BadRequestException(errMsg);
            }
            if (StringUtils.isBlank(capability.getDescription())) {
                capability.setDescription(null);
            }
            if (capability.getResources() == null || capability.getResources().isEmpty()) {
                capability.setResources(null);
            }
            Capability exist = ldapCapabilityRepository.getObject(createCapabilityFilter(capability.getId(), capability.getVersion(), capability.getType()));
            if(exist != null){
                String errMsg = String.format("Capability with id: %s, version: %s, and OpenStackType: %s already exist."
                        ,exist.getId(), exist.getVersion(), exist.getType());
                throw new DuplicateException(errMsg);
            }
        }
        checkDuplicatesInCapabilities(capabilities);
    }

    private void checkDuplicatesInCapabilities(Capabilities capabilities) {
        Capabilities noDup = new Capabilities();
        for(Capability capability : capabilities.getCapability()){
            if(!noDup.getCapability().contains(capability)){
                noDup.getCapability().add(capability);
            }else{
                String errMsg = String.format("Capability with id: %s, version: %s, and OpenStackType: %s must be unique."
                        ,capability.getId() , capability.getVersion(), capability.getType());
                throw new DuplicateException(errMsg);
            }
        }
    }

    @Override
    public Capabilities getCapabilities(String version, String openStackType) {
        if(StringUtils.isBlank(version)){
            throw new BadRequestException("Capability's version cannot be null.");
        }
        if(StringUtils.isBlank(openStackType)){
            throw new BadRequestException("Capability's type cannot be null.");
        }
        List<Capability> capabilityList = ldapCapabilityRepository.getObjects(createCapabilitiesFilter(version, openStackType));
        Capabilities capabilities = new Capabilities();
        capabilities.getCapability().addAll(capabilityList);
        return capabilities;
    }

    @Override
    public Capability getCapability(String capabilityId, String version, String openStackType) {
        if(StringUtils.isBlank(capabilityId)){
            throw new BadRequestException("Capability's id cannot be null.");
        }
        if(StringUtils.isBlank(version)){
            throw new BadRequestException("Capability's version cannot be null.");
        }
        if(StringUtils.isBlank(openStackType)){
            throw new BadRequestException("Capability's type cannot be null.");
        }
        Capability capability = ldapCapabilityRepository.getObject(createCapabilityFilter(capabilityId,version,openStackType));
        if(capability == null){
            String errMsg = String.format("Capability with id: %s, version: %s, and OpenStackType: %s is not found."
                        ,capabilityId , version, openStackType);
            throw new NotFoundException(errMsg);
        }
        return capability;
    }

    @Override
    public void removeCapabilities(String version, String openStackType) {
        if(StringUtils.isBlank(version)){
            throw new BadRequestException("Capability's version cannot be null.");
        }
        if(StringUtils.isBlank(openStackType)){
            throw new BadRequestException("Capability's type cannot be null.");
        }
        List<Capability> capabilityList = ldapCapabilityRepository.getObjects(createCapabilitiesFilter(version, openStackType));
        for(Capability capability : capabilityList){
            ldapCapabilityRepository.deleteObject(createCapabilityFilter(capability.getId(), capability.getVersion(), capability.getType()));
        }
    }

    private Filter createCapabilityFilter(String capabilityId, String version, String openStackType) {
        Filter filter = new LdapSearchBuilder()
                .addEqualAttribute(ATTR_CAPABILITY_ID, capabilityId)
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_CAPABILITY)
                .addEqualAttribute(ATTR_VERSION_ID, version)
                .addEqualAttribute(ATTR_OPENSTACK_TYPE, openStackType).build();
        return filter;
    }

    private Filter createCapabilitiesFilter(String version, String openStackType) {
        Filter filter = new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_CAPABILITY)
                .addEqualAttribute(ATTR_VERSION_ID, version)
                .addEqualAttribute(ATTR_OPENSTACK_TYPE, openStackType).build();
        return filter;
    }
}
