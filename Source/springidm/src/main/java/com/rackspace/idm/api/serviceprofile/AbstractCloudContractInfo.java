package com.rackspace.idm.api.serviceprofile;

import com.rackspace.idm.jaxb.Contract;
import com.rackspace.idm.jaxb.Version;
import com.rackspace.idm.jaxb.VersionChoiceList;


public abstract class AbstractCloudContractInfo extends AbstractContractInfo {
	
	public AbstractCloudContractInfo(ServiceProfileUtil util, ServiceProfileConfig config) {
		super(util, config);
	}
	
    public Contract createContractInfo() {
    	Version version1_0 = createContractV1_0Version();
    	Version version1_1 = createContractV1_1Version();
    	Version version2_0 = createContractV2_0Version();
    	
    	VersionChoiceList versions = new VersionChoiceList();
    	versions.getVersions().add(version1_0);
       	versions.getVersions().add(version1_1);
    	versions.getVersions().add(version2_0);
    	
    	Contract contract = new Contract();
    	contract.setName("cloud auth");
    	contract.setVersions(versions);
    	
    	return contract;
    }

    public abstract Version createContractV1_0Version();
    public abstract Version createContractV1_1Version(); 
    public abstract Version createContractV2_0Version(); 
}
