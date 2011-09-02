package com.rackspace.idm.api.serviceprofile;

import com.rackspace.idm.jaxb.Contract;
import com.rackspace.idm.jaxb.Link;
import com.rackspace.idm.jaxb.Relation;


public abstract class AbstractContractInfo {
	
	protected final ServiceProfileUtil util;
	protected final ServiceProfileConfig config;
    
	public AbstractContractInfo(ServiceProfileUtil util, ServiceProfileConfig config) {
		this.util = util;
		this.config = config;
	}

	public abstract Contract createContractInfo();
	
    Link createLink(Relation rel, String type, String href, String title) {
    	return util.createLink(rel, type, href, title);
    }
}
