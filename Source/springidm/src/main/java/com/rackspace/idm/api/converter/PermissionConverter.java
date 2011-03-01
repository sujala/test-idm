package com.rackspace.idm.api.converter;

import java.util.ArrayList;
import java.util.List;

import com.rackspace.idm.domain.entity.Permission;
import com.rackspace.idm.domain.entity.PermissionSet;
import com.rackspace.idm.jaxb.ObjectFactory;

public class PermissionConverter {

    protected ObjectFactory of = new ObjectFactory();

    public PermissionConverter() {
    }

    public Permission toPermissionDO(
        com.rackspace.idm.jaxb.Permission permission) {
        Permission perm = new Permission();
        perm.setClientId(permission.getClientId());
        perm.setCustomerId(permission.getCustomerId());
        perm.setPermissionId(permission.getPermissionId());
        perm.setType(permission.getType());
        perm.setValue(permission.getValue());
        return perm;
    }

    public List<Permission> toPermissionListDO(
        com.rackspace.idm.jaxb.PermissionList permissions) {
        List<Permission> perms = new ArrayList<Permission>();

        for (com.rackspace.idm.jaxb.Permission perm : permissions
            .getPermissions()) {
            perms.add(toPermissionDO(perm));
        }

        return perms;
    }

    public com.rackspace.idm.jaxb.Permission toPermissionJaxb(
        Permission permission) {
        com.rackspace.idm.jaxb.Permission perm = of.createPermission();
        perm.setClientId(permission.getClientId());
        perm.setCustomerId(permission.getCustomerId());
        perm.setPermissionId(permission.getPermissionId());
        perm.setType(permission.getType());
        perm.setValue(permission.getValue());
        return perm;
    }

    public com.rackspace.idm.jaxb.PermissionList toPermissionListJaxb(
        List<Permission> permissions) {
        
        if (permissions == null || permissions.size() < 1) {
            return null;
        }
        
        com.rackspace.idm.jaxb.PermissionList perms = of.createPermissionList();

        for (Permission perm : permissions) {
            perms.getPermissions().add(toPermissionJaxb(perm));
        }
        return perms;
    }

    public com.rackspace.idm.jaxb.Permissions toPermissionsJaxb(
        PermissionSet permset) {
        com.rackspace.idm.jaxb.Permissions perms = of.createPermissions();

        if (permset.getDefineds() != null && permset.getDefineds().size() > 0) {
            perms.setDefined(toPermissionListJaxb(permset.getDefineds()));
        }
        if (permset.getGranteds() != null && permset.getGranteds().size() > 0) {
            perms.setGranted(toPermissionListJaxb(permset.getGranteds()));
        }

        return perms;
    }
}
