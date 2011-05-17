package com.rackspace.idm.api.converter;

import java.util.List;

import com.rackspace.idm.domain.entity.DefinedPermission;
import com.rackspace.idm.domain.entity.GrantedPermission;
import com.rackspace.idm.domain.entity.Permission;
import com.rackspace.idm.jaxb.ObjectFactory;

public class PermissionConverter {

    private final ObjectFactory of = new ObjectFactory();

    public PermissionConverter() {
    }

//    public PermissionEntity toPermissionDO(com.rackspace.idm.jaxb.Permission permJaxb) {
//        PermissionEntity permDo = new PermissionEntity();
//        permDo.setClientId(permJaxb.getClientId());
//        permDo.setCustomerId(permJaxb.getCustomerId());
//        permDo.setPermissionId(permJaxb.getPermissionId());
//        permDo.setPermissionType(permJaxb.getType());
//        permDo.setValue(permJaxb.getValue());
//        permDo.setTitle(permJaxb.getTitle());
//        permDo.setDescription(permJaxb.getDescription());
//        permDo.setGrantedByDefault(permJaxb.isGrantedByDefault());
//        permDo.setEnabled(permJaxb.isEnabled());
//        return permDo;
//    }
//    
//    public PermissionEntity toPermissionObjectDO(com.rackspace.idm.jaxb.Permission permJaxb) {
//        PermissionEntity permDo = new PermissionEntity();
//        permDo.setClientId(permJaxb.getClientId());
//        permDo.setCustomerId(permJaxb.getCustomerId());
//        permDo.setPermissionId(permJaxb.getPermissionId());
//        permDo.setPermissionType(permJaxb.getType());
//        permDo.setValue(permJaxb.getValue());
//        permDo.setTitle(permJaxb.getTitle());
//        permDo.setDescription(permJaxb.getDescription());
//        permDo.setGrantedByDefault(permJaxb.isGrantedByDefault());
//        permDo.setEnabled(permJaxb.isEnabled());
//        return permDo;
//    }
    
    public DefinedPermission toDefinedPermissionObjectDO(com.rackspace.idm.jaxb.Permission permJaxb) {
        DefinedPermission permDo = new DefinedPermission();
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
    
    public GrantedPermission toGrantedPermissionObjectDO(com.rackspace.idm.jaxb.Permission permJaxb) {
        GrantedPermission permDo = new GrantedPermission();
        permDo.setClientId(permJaxb.getClientId());
        permDo.setCustomerId(permJaxb.getCustomerId());
        permDo.setPermissionId(permJaxb.getPermissionId());
        return permDo;
    }

//    public List<PermissionEntity> toPermissionListDO(com.rackspace.idm.jaxb.Permissions permissions) {
//        List<PermissionEntity> perms = new ArrayList<PermissionEntity>();
//
//        for (com.rackspace.idm.jaxb.Permission perm : permissions.getPermissions()) {
//            perms.add(toPermissionDO(perm));
//        }
//
//        return perms;
//    }
    
    public com.rackspace.idm.jaxb.Permission toPermissionJaxb(Permission permDo) {
        com.rackspace.idm.jaxb.Permission permJaxb = of.createPermission();
        permJaxb.setClientId(permDo.getClientId());
        permJaxb.setCustomerId(permDo.getCustomerId());
        permJaxb.setPermissionId(permDo.getPermissionId());
        
        if (permDo instanceof DefinedPermission) {
            permJaxb.setType(((DefinedPermission) permDo).getPermissionType());
            permJaxb.setValue(((DefinedPermission) permDo).getValue());
            permJaxb.setTitle(((DefinedPermission) permDo).getTitle());
            permJaxb.setDescription(((DefinedPermission) permDo).getDescription());
            permJaxb.setGrantedByDefault(((DefinedPermission) permDo).getGrantedByDefault());
            permJaxb.setEnabled(((DefinedPermission) permDo).getEnabled());
        }

        return permJaxb;
    }

    public com.rackspace.idm.jaxb.Permissions toPermissionListJaxb(List<Permission> permissions) {

        if (permissions == null || permissions.size() < 1) {
            return null;
        }

        com.rackspace.idm.jaxb.Permissions perms = of.createPermissions();

        for (Permission perm : permissions) {
            perms.getPermissions().add(toPermissionJaxb(perm));
        }
        return perms;
    }

    public DefinedPermission toDefinedPermissionDO(com.rackspace.idm.jaxb.Permission permJaxb) {
        DefinedPermission permDo = new DefinedPermission();
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

    public Object toDefinedPermissionListJaxb(List<DefinedPermission> defineds) {
        if (defineds == null || defineds.size() < 1) {
            return null;
        }

        com.rackspace.idm.jaxb.Permissions perms = of.createPermissions();

        for (DefinedPermission perm : defineds) {
            perms.getPermissions().add(toPermissionJaxb(perm));
        }
        return perms;
    }
}
