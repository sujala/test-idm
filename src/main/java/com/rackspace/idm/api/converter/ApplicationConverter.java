package com.rackspace.idm.api.converter;

import com.rackspace.api.idm.v1.ApplicationList;
import com.rackspace.api.idm.v1.ObjectFactory;
import com.rackspace.idm.domain.entity.Application;
import org.apache.commons.lang.StringUtils;

import javax.xml.bind.JAXBElement;

public class ApplicationConverter {

    private final ObjectFactory objectFactory = new ObjectFactory();

    private final RolesConverter rolesConverter;
    
    public ApplicationConverter(RolesConverter rolesConverter) {
    	this.rolesConverter = rolesConverter;
    }

    public Application toClientDO(com.rackspace.api.idm.v1.Application jaxbClient) {
        Application application = new Application();
        application.setClientId(jaxbClient.getClientId());
        application.setRCN(jaxbClient.getCustomerId());
        application.setName(jaxbClient.getName());
        application.setCallBackUrl(jaxbClient.getCallBackUrl());
        application.setTitle(jaxbClient.getTitle());
        application.setDescription(jaxbClient.getDescription());
        application.setScope(jaxbClient.getScope());
        
        if (jaxbClient.isEnabled() != null) {
        	application.setEnabled(jaxbClient.isEnabled());
        }
        
        if (jaxbClient.getSecretCredentials() != null
            && !StringUtils.isBlank(jaxbClient.getSecretCredentials()
                .getClientSecret())) {
            application.setClientSecret(jaxbClient.getSecretCredentials()
                .getClientSecret());
        }

        return application;
    }

    public JAXBElement<com.rackspace.api.idm.v1.Application> toClientJaxbWithoutPermissionsOrCredentials(
        Application client) {
        return toClientJaxb(client, false);
    }

    public JAXBElement<com.rackspace.api.idm.v1.Application> toClientJaxbWithPermissionsAndCredentials(
        Application client) {
        return toClientJaxb(client, true);
    }

    public JAXBElement<ApplicationList> toClientListJaxb(com.rackspace.idm.domain.entity.Applications clients) {
        if (clients == null || clients.getClients() == null) {
            return null;
        }

        ApplicationList returnedClients = objectFactory.createApplicationList();

        for (Application client : clients.getClients()) {
            returnedClients.getApplication().add(
                toClientJaxbWithoutPermissionsOrCredentials(client).getValue());
        }
        
        returnedClients.setLimit(clients.getLimit());
        returnedClients.setOffset(clients.getOffset());
        returnedClients.setTotalRecords(clients.getTotalRecords());

        return objectFactory.createApplications(returnedClients);
    }
    
    /**
     * converts the application list, but only displays minimum amount of data. More detailed information
     * should be retrieved directly from teh resource
     */
    public JAXBElement<com.rackspace.api.idm.v1.ApplicationList> toApplicationJaxbMin(com.rackspace.idm.domain.entity.Applications applications) {
        if (applications == null || applications.getClients() == null) {
            return null;
        }

        com.rackspace.api.idm.v1.ApplicationList returnedClients = objectFactory.createApplicationList();
        for (Application client : applications.getClients()) {
            returnedClients.getApplication().add(toClientJaxbMin(client).getValue());
        }
        
        returnedClients.setLimit(applications.getLimit());
        returnedClients.setOffset(applications.getOffset());
        returnedClients.setTotalRecords(applications.getTotalRecords());

        return objectFactory.createApplications(returnedClients);
    }

    JAXBElement<com.rackspace.api.idm.v1.Application> toClientJaxbMin(Application application) {
        
    	com.rackspace.api.idm.v1.Application returnedApplication = objectFactory.createApplication();

        returnedApplication.setClientId(application.getClientId());
        returnedApplication.setCustomerId(application.getRCN());
        returnedApplication.setName(application.getName());
        returnedApplication.setDescription(application.getDescription());

        return objectFactory.createApplication(returnedApplication);
    }
    JAXBElement<com.rackspace.api.idm.v1.Application> toClientJaxb(Application client, boolean includeCredentials) {
       
    	com.rackspace.api.idm.v1.Application returnedApplication = objectFactory.createApplication();

        returnedApplication.setClientId(client.getClientId());
        returnedApplication.setCustomerId(client.getRCN());
        returnedApplication.setEnabled(client.isEnabled());
        returnedApplication.setName(client.getName());
        returnedApplication.setCallBackUrl(client.getCallBackUrl());
        returnedApplication.setTitle(client.getTitle());
        returnedApplication.setDescription(client.getDescription());
        returnedApplication.setScope(client.getScope());

        if (includeCredentials && client.getClientSecretObj() != null
            && !StringUtils.isBlank(client.getClientSecret())) {

            com.rackspace.api.idm.v1.ApplicationSecretCredentials creds = objectFactory
                .createApplicationSecretCredentials();

            creds.setClientSecret(client.getClientSecret());
            returnedApplication.setSecretCredentials(creds);
        }

        return objectFactory.createApplication(returnedApplication);
    }

    public JAXBElement<com.rackspace.api.idm.v1.Application> toApplicationJaxbFromApplication(Application client) {
    	if (client == null) {
    		return null;
    	}
    	
        com.rackspace.api.idm.v1.Application jaxbApplication = objectFactory.createApplication();
        jaxbApplication.setClientId(client.getClientId());
        jaxbApplication.setCustomerId(client.getRCN());
        jaxbApplication.setName(client.getName());
        jaxbApplication.setRoles(rolesConverter.toRoleJaxbFromTenantRole(client.getRoles()).getValue());

        return objectFactory.createApplication(jaxbApplication);
    }   
}
