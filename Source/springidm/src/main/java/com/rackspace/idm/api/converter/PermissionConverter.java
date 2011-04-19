package com.rackspace.idm.api.converter;

import java.util.ArrayList;
import java.util.List;

import com.rackspace.idm.domain.entity.Permission;
import com.rackspace.idm.domain.entity.PermissionSet;
import com.rackspace.idm.jaxb.ObjectFactory;

public class PermissionConverter {

    private ObjectFactory of = new ObjectFactory();

    public PermissionConverter() {
    }

    public Permission toPermissionDO(com.rackspace.idm.jaxb.Permission permJaxb) {
        Permission permDo = new Permission();
        permDo.setClientId(permJaxb.getClientId());
        permDo.setCustomerId(permJaxb.getCustomerId());
        permDo.setPermissionId(permJaxb.getPermissionId());
        permDo.setType(permJaxb.getType());
        permDo.setValue(permJaxb.getValue());
        permDo.setTitle(permJaxb.getTitle());
        permDo.setDescription(permJaxb.getDescription());
        permDo.setGrantedByDefault(permJaxb.isGrantedByDefault());
        permDo.setEnabled(permJaxb.isEnabled());
        return permDo;
    }

    public List<Permission> toPermissionListDO(com.rackspace.idm.jaxb.PermissionList permissions) {
        List<Permission> perms = new ArrayList<Permission>();

        for (com.rackspace.idm.jaxb.Permission perm : permissions.getPermissions()) {
            perms.add(toPermissionDO(perm));
        }

        return perms;
    }

    public com.rackspace.idm.jaxb.Permission toPermissionJaxb(Permission permDo) {
        com.rackspace.idm.jaxb.Permission permJaxb = of.createPermission();
        permJaxb.setClientId(permDo.getClientId());
        permJaxb.setCustomerId(permDo.getCustomerId());
        permJaxb.setPermissionId(permDo.getPermissionId());
        permJaxb.setType(permDo.getType());
        permJaxb.setValue(permDo.getValue());
        permJaxb.setTitle(permDo.getTitle());
        permJaxb.setDescription(permDo.getDescription());
        permJaxb.setGrantedByDefault(permDo.getGrantedByDefault());
        permJaxb.setEnabled(permDo.getEnabled());
        return permJaxb;
    }

    public com.rackspace.idm.jaxb.PermissionList toPermissionListJaxb(List<Permission> permissions) {

        if (permissions == null || permissions.size() < 1) {
            return null;
        }

        com.rackspace.idm.jaxb.PermissionList perms = of.createPermissionList();

        for (Permission perm : permissions) {
            perms.getPermissions().add(toPermissionJaxb(perm));
        }
        return perms;
    }

    public com.rackspace.idm.jaxb.Permissions toPermissionsJaxb(PermissionSet permset) {
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
