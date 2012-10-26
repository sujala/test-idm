package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.dao.EndpointDao;
import com.rackspace.idm.domain.dao.impl.CapabilityDao;
import com.rackspace.idm.domain.entity.Capabilities;
import com.rackspace.idm.domain.entity.Capability;
import com.rackspace.idm.domain.entity.CloudBaseUrl;
import com.rackspace.idm.domain.service.CapabilityService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.NotFoundException;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: 10/24/12
 * Time: 12:52 PM
 * To change this template use File | Settings | File Templates.
 */
@Component
public class DefaultCapabilityService implements CapabilityService{
    @Autowired
    private EndpointDao endpointDao;

    @Autowired
    private CapabilityDao capabilityDao;

    @Override
    public void updateCapabilities(String endpointTemplateId, Capabilities capabilities) {
        verifyEndpointTemplate(endpointTemplateId);
        validateCapabilities(capabilities);
        capabilityDao.updateCapabilities(endpointTemplateId, capabilities);
    }

    private void verifyEndpointTemplate(String endpointTemplateId) {
        Integer endpointId;
        try{
            endpointId = Integer.valueOf(endpointTemplateId);
        }catch(Exception ex){
            throw new BadRequestException("Endpoint template id must be a number.");
        }
        CloudBaseUrl cloudBaseUrl = endpointDao.getBaseUrlById(endpointId);
        if(cloudBaseUrl == null){
            String errMsg = String.format("Endpoint %s does not exist.",endpointTemplateId);
            throw new NotFoundException(errMsg);
        }
    }

    private void validateCapabilities(Capabilities capabilities) {
        for(Capability capability : capabilities.getCapability()){
            if(StringUtils.isBlank(capability.getCapabilityId())){
                throw new BadRequestException("Capability Id cannot be empty.");
            }
            if(StringUtils.isBlank(capability.getAction())){
                String errMsg = String.format("Capability %s is missing action.", capability.getCapabilityId());
                throw new BadRequestException(errMsg);
            }
            if(StringUtils.isBlank(capability.getName())){
                String errMsg = String.format("Capability %s is missing name.", capability.getCapabilityId());
                throw new BadRequestException(errMsg);
            }
            if(StringUtils.isBlank(capability.getUrl())){
                String errMsg = String.format("Capability %s is missing url.", capability.getCapabilityId());
                throw new BadRequestException(errMsg);
            }
            if(StringUtils.isBlank(capability.getDescription())){
                capability.setDescription(null);
            }
            if(capability.getResources().isEmpty()){
                capability.setResources(null);
            }
        }
    }

    @Override
    public Capabilities getCapabilities(String endpointTemplateId) {
        verifyEndpointTemplate(endpointTemplateId);
        return capabilityDao.getCapabilities(endpointTemplateId);
    }

    @Override
    public Capability getCapability(String capabilityId, String endpointTemplateId) {
        verifyEndpointTemplate(endpointTemplateId);
        Capability capability = capabilityDao.getCapability(capabilityId, endpointTemplateId);
        if(capability == null){
            String errMsg = String.format("Capability %s not found", capabilityId);
            throw new NotFoundException(errMsg);
        }
        return capability;
    }

    @Override
    public void removeCapabilities(String endpointTemplateId) {
        verifyEndpointTemplate(endpointTemplateId);
        capabilityDao.removeCapabilities(endpointTemplateId);
    }
}
