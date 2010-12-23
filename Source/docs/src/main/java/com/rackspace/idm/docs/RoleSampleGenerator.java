package com.rackspace.idm.docs;

import java.io.IOException;

import javax.xml.bind.JAXBException;

import com.rackspace.idm.jaxb.Permission;
import com.rackspace.idm.jaxb.PermissionList;
import com.rackspace.idm.jaxb.Permissions;
import com.rackspace.idm.jaxb.Role;
import com.rackspace.idm.jaxb.Roles;

public class RoleSampleGenerator extends SampleGenerator {
    private RoleSampleGenerator() {
        super();
    }
    
    public static void main(String[] args) throws JAXBException, IOException {
        RoleSampleGenerator sampleGen = new RoleSampleGenerator();

        sampleGen.marshalToFiles(sampleGen.getRole(), "role");
        sampleGen.marshalToFiles(sampleGen.getRoles(), "roles");
    }
    
    private Role getRole() {
        Role role = of.createRole();
        
        role.setName("Admin");
        role.setType("RackspaceDefined");
        
        Permission permission = of.createPermission();
        permission.setPermissionId("AddUser");
        permission.setClientId("IDM");
        
        PermissionList permissions = of.createPermissionList();
        permissions.getPermissions().add(permission);
        
        role.setPermissions(permissions);
        
        return role;
    }
    
    private Role getRole2() {
        Role role = of.createRole();
        
        role.setName("HR");
        role.setType("CustomerDefined");
        
        Permission permission = of.createPermission();
        permission.setPermissionId("AddUser");
        permission.setClientId("IDM");
        
        PermissionList permissions = of.createPermissionList();
        permissions.getPermissions().add(permission);
        
        role.setPermissions(permissions);
        
        return role;
    }
    
    private Roles getRoles() {
        Roles roles = of.createRoles();
        Role role = getRole();
        roles.getRoles().add(role);
        Role role2 = getRole2();
        roles.getRoles().add(role2);
        return roles;
    }
    
}
