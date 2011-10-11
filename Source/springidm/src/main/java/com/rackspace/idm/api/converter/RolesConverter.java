package com.rackspace.idm.api.converter;

import java.util.ArrayList;
import java.util.List;

import com.rackspace.api.idm.v1.ObjectFactory;
import com.rackspace.idm.domain.entity.ClientRole;
import com.rackspace.idm.domain.entity.TenantRole;

public class RolesConverter {

    private final ObjectFactory objectFactory = new ObjectFactory();

    public RolesConverter() {
    }

    public com.rackspace.api.idm.v1.Roles toRoleJaxbFromClientRole(final List<ClientRole> clientRoles) {
    	// By default all lists should be paginated. No support for pagination for roles at this
    	// time, especially because of the data structure. So hacking here to conform to api.
    	com.rackspace.api.idm.v1.Roles jaxbRoles = initializeRoles();
    	if (clientRoles != null) {
        	for (ClientRole clientRole : clientRoles) {
        		jaxbRoles.getRoles().add(toRoleJaxbFromClientRole(clientRole));
        	}
        	
        	jaxbRoles.setLimit(jaxbRoles.getRoles().size());
        	jaxbRoles.setTotalRecords(jaxbRoles.getRoles().size());
        }
    	
    	return jaxbRoles;
    }
    
    public com.rackspace.api.idm.v1.Role toRoleJaxbFromClientRole(final ClientRole clientRole) {
    	com.rackspace.api.idm.v1.Role jaxbRole = objectFactory.createRole();
    	jaxbRole.setId(clientRole.getId());
		jaxbRole.setApplicationId(clientRole.getClientId());
		jaxbRole.setName(clientRole.getName());
		jaxbRole.setDescription(clientRole.getDescription());
		return jaxbRole;
    }
    
    public com.rackspace.api.idm.v1.Roles toRoleJaxbFromTenantRole(final List<TenantRole> tenantRoles) {
    	// By default all lists should be paginated. No support for pagination for roles at this
    	// time, especially because of the data structure. So hacking here to conform to api.
    	com.rackspace.api.idm.v1.Roles jaxbRoles = initializeRoles();
    	if (tenantRoles != null) {
        	for (TenantRole role : tenantRoles) {
        		jaxbRoles.getRoles().addAll(toRoleJaxbFromTenantRole(role));
        	}
        	
        	jaxbRoles.setLimit(jaxbRoles.getRoles().size());
        	jaxbRoles.setTotalRecords(jaxbRoles.getRoles().size());
        }
    	
    	return jaxbRoles;
    }
    
    public com.rackspace.api.idm.v1.Roles toRoleJaxbFromRoleString(final List<String> roles) {
    	// By default all lists should be paginated. No support for pagination for roles at this
    	// time, especially because of the data structure. So hacking here to conform to api.
    	com.rackspace.api.idm.v1.Roles jaxbRoles = initializeRoles();
     	if (roles != null) {
     		for (String role : roles) {
     	    	com.rackspace.api.idm.v1.Role jaxbRole = objectFactory.createRole();
	    		jaxbRole.setName(role);
	    		jaxbRoles.getRoles().add(jaxbRole);
     		}
     		
          	jaxbRoles.setLimit(roles.size());
        	jaxbRoles.setTotalRecords(roles.size());
     	}
     	
     	return jaxbRoles;
    }
    
    public ClientRole toClientRole(com.rackspace.api.idm.v1.Role jaxbRole) {
    	ClientRole clientRole = new ClientRole();
    	clientRole.setName(jaxbRole.getName());
    	clientRole.setClientId(jaxbRole.getApplicationId());
    	clientRole.setDescription(jaxbRole.getDescription());
    	clientRole.setId(jaxbRole.getId());
    	return clientRole;
    }
    
    List<com.rackspace.api.idm.v1.Role> toRoleJaxbFromTenantRole(final TenantRole tenantRole) {
    	// our domain model has one tenant role mapping to multiple tenants.
    	// the endpoint model has a role for each tenant. we want to flatten
    	// the domain model structure into the endpoint model structure. if
    	// domain model has no tenant role, just add the jaxb role without the tenant
    	List<com.rackspace.api.idm.v1.Role> jaxbRoles = new ArrayList<com.rackspace.api.idm.v1.Role>();
     	if (tenantRole.getTenantIds() != null) {
     		for (String tenantId : tenantRole.getTenantIds()) {
     			com.rackspace.api.idm.v1.Role jaxbRole = createJaxbRole(tenantRole);
	    		jaxbRole.setTenantId(tenantId);
	    		jaxbRoles.add(jaxbRole);
     		}
     	}
     	else {
     		com.rackspace.api.idm.v1.Role jaxbRole = createJaxbRole(tenantRole);
    		jaxbRoles.add(jaxbRole);
     	}
     	
     	return jaxbRoles;
    }
    
    private com.rackspace.api.idm.v1.Role createJaxbRole(TenantRole tenantRole) {
    	com.rackspace.api.idm.v1.Role jaxbRole = objectFactory.createRole();
    	jaxbRole.setId(tenantRole.getRoleRsId());
		jaxbRole.setName(tenantRole.getName());
		jaxbRole.setApplicationId(tenantRole.getClientId());
		return jaxbRole;
    }
    
    private com.rackspace.api.idm.v1.Roles initializeRoles() {
	    com.rackspace.api.idm.v1.Roles jaxbRoles = objectFactory.createRoles();
		jaxbRoles.setOffset(0);
		jaxbRoles.setLimit(0);
		return jaxbRoles;
    }
}
