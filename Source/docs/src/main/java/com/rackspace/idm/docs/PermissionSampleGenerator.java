package com.rackspace.idm.docs;

import java.io.IOException;

import javax.xml.bind.JAXBException;

import com.rackspace.idm.jaxb.Permission;
import com.rackspace.idm.jaxb.PermissionList;
import com.rackspace.idm.jaxb.Permissions;

public class PermissionSampleGenerator extends SampleGenerator {
    private PermissionSampleGenerator() {
        super();
    }
    
    public static void main(String[] args) throws JAXBException, IOException {
        PermissionSampleGenerator sampleGen = new PermissionSampleGenerator();

        sampleGen.marshalToFiles(sampleGen.getPermission(), "permission");
        sampleGen.marshalToFiles(sampleGen.getPermissions(), "permissions");
    }
    
    private Permission getPermission() {
        Permission permission = of.createPermission();
        
        permission.setPermissionId("addCustomer");
        permission.setClientId("IDM");
        permission.setCustomerId("RCN-000-000-000");
        permission.setType("application/text");
        permission.setValue("POST /customers");
        
        return permission;
    }
    
    private Permission getPermission2() {
        Permission permission = of.createPermission();
        
        permission.setPermissionId("getCustomer");
        permission.setClientId("IDM");
        permission.setCustomerId("RCN-000-000-000");
        permission.setType("application/text");
        permission.setValue("GET /customers");
        
        return permission;
    }
    
    private Permissions getPermissions() {
        Permissions permissions = of.createPermissions();
        PermissionList perms = of.createPermissionList();
        Permission permission = getPermission();
        Permission permission2 = getPermission2();
        perms.getPermissions().add(permission);
        perms.getPermissions().add(permission2);
        permissions.setDefined(perms);
        permissions.setGranted(perms);
        return permissions;
    }
    
}
