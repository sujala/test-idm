package com.rackspace.idm.api.serviceprofile;

import javax.ws.rs.core.MediaType;

import com.rackspace.idm.jaxb.Contract;
import com.rackspace.idm.jaxb.Link;
import com.rackspace.idm.jaxb.Relation;
import com.rackspace.idm.jaxb.ServiceModel;
import com.rackspace.idm.jaxb.ServiceProfile;

public class ServiceProfileInfo {

    private final ServiceProfileUtil util;
    private final ServiceProfileConfig config;
    private final CloudPublicContractInfo cloudPublicContractInfo;
    private final CloudInternalContractInfo cloudInternalContractInfo;
    private final InternalContractInfo internalContractInfo;
    
	public ServiceProfileInfo(ServiceProfileConfig config) {
		this.config = config;
		this.util = new ServiceProfileUtil();
		this.cloudPublicContractInfo = new CloudPublicContractInfo(util, config);
		this.cloudInternalContractInfo = new CloudInternalContractInfo(util, config);
		this.internalContractInfo = new InternalContractInfo(util, config);
	}
	
    public ServiceProfile createInternalServiceProfile() {
    	Link serviceRegistryLink = util.createLink(Relation.DESCRIBEDBY, MediaType.TEXT_HTML, "http://serviceregistry.rackspace.com/services/idm", null); 

    	Contract internalContract = internalContractInfo.createContractInfo();
    	Contract cloudInternalContract = cloudInternalContractInfo.createContractInfo();
    	
    	ServiceProfile serviceProfile = createBaseServiceProfile();
    	serviceProfile.getLinks().add(serviceRegistryLink);
    	serviceProfile.getContracts().add(internalContract);
    	serviceProfile.getContracts().add(cloudInternalContract);
    	
    	return serviceProfile;
    }
    
    public ServiceProfile createExternalServiceProfile() {
      	Contract cloudPublicContract = cloudPublicContractInfo.createContractInfo();
      	
    	ServiceProfile serviceProfile = createBaseServiceProfile();
    	serviceProfile.getContracts().add(cloudPublicContract);
    	
    	return serviceProfile;
    }
    
    private ServiceProfile createBaseServiceProfile() {
    	Link selfLink = util.createLink(Relation.SELF, null, config.getBaseUrl(), null); 
    	
    	ServiceProfile serviceProfile = new ServiceProfile();
    	serviceProfile.setName("Customer Identity Management");
    	serviceProfile.setCanonicalName("idm");
    	serviceProfile.setDnsZone("idm.api.rackspace.com");
    	serviceProfile.setServiceModel(ServiceModel.UTILITY);
    	serviceProfile.setShortDescription("Allows users access Rackspace resources and systems.");
    	serviceProfile.setDetailedDescription("The global auth api allows Rackspace clients to obtain tokens that can be used to access resources in Rackspace. It also allows clients manage identities and delegate access to resources.");
    	serviceProfile.getLinks().add(selfLink);
    	
    	return serviceProfile;
    }
}
