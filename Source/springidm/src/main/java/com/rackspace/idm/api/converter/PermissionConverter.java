package com.rackspace.idm.api.converter;

import java.util.ArrayList;
import java.util.List;

import com.rackspace.idm.domain.entity.PermissionEntity;
import com.rackspace.idm.jaxb.ObjectFactory;

public class PermissionConverter {

    private final ObjectFactory of = new ObjectFactory();

    public PermissionConverter() {
    }

    public PermissionEntity toPermissionDO(com.rackspace.idm.jaxb.Permission permJaxb) {
        PermissionEntity permDo = new PermissionEntity();
        permDo.setClientId(permJaxb.getClientId());
        permDo.setCustomerId(permJaxb.getCustomerId());
        permDo.setPermissionId(permJaxb.getPermissionId());
        permDo.setPermissionType(permJaxb.getType());
        permDo.setValue(permJaxb.getValue());
        permDo.setTitle(permJaxb.getTitle());
        permDo.setDescription(permJaxb.getDescription());
        permDo.setGrantedByDefault(permJaxb.isGrantedByDefault());
        permDo.setEnabled(permJaxb.isEnabled());
        return permDo;
    }
    
    public PermissionEntity toPermissionObjectDO(com.rackspace.idm.jaxb.Permission permJaxb) {
        PermissionEntity permDo = new PermissionEntity();
        permDo.setClientId(permJaxb.getClientId());
        permDo.setCustomerId(permJaxb.getCustomerId());
        permDo.setPermissionId(permJaxb.getPermissionId());
        permDo.setPermissionType(permJaxb.getType());
        permDo.setValue(permJaxb.getValue());
        permDo.setTitle(permJaxb.getTitle());
        permDo.setDescription(permJaxb.getDescription());
        permDo.setGrantedByDefault(permJaxb.isGrantedByDefault());
        permDo.setEnabled(permJaxb.isEnabled());
        return permDo;
    }

    public List<PermissionEntity> toPermissionListDO(com.rackspace.idm.jaxb.Permissions permissions) {
        List<PermissionEntity> perms = new ArrayList<PermissionEntity>();

        for (com.rackspace.idm.jaxb.Permission perm : permissions.getPermissions()) {
            perms.add(toPermissionDO(perm));
        }

        return perms;
    }
    
    public com.rackspace.idm.jaxb.Permission toPermissionJaxb(PermissionEntity permDo) {
        com.rackspace.idm.jaxb.Permission permJaxb = of.createPermission();
        permJaxb.setClientId(permDo.getClientId());
        permJaxb.setCustomerId(permDo.getCustomerId());
        permJaxb.setPermissionId(permDo.getPermissionId());
        permJaxb.setType(permDo.getPermissionType());
        permJaxb.setValue(permDo.getValue());
        permJaxb.setTitle(permDo.getTitle());
        permJaxb.setDescription(permDo.getDescription());
        permJaxb.setGrantedByDefault(permDo.getGrantedByDefault());
        permJaxb.setEnabled(permDo.getEnabled());
        return permJaxb;
    }

    public com.rackspace.idm.jaxb.Permissions toPermissionListJaxb(List<PermissionEntity> permissions) {

        if (permissions == null || permissions.size() < 1) {
            return null;
        }

        com.rackspace.idm.jaxb.Permissions perms = of.createPermissions();

        for (PermissionEntity perm : permissions) {
            perms.getPermissions().add(toPermissionJaxb(perm));
        }
        return perms;
    }
}
