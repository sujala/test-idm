package com.rackspace.idm.api.converter;

import java.util.ArrayList;
import java.util.List;

import com.rackspace.idm.entities.Role;
import com.rackspace.idm.jaxb.ObjectFactory;

public class RoleConverter {

    private PermissionConverter permissionConverter;
    protected ObjectFactory of = new ObjectFactory();

    public RoleConverter(PermissionConverter permissionConverter) {
        this.permissionConverter = permissionConverter;
    }

    public Role toRoleDO(com.rackspace.idm.jaxb.Role jaxbRole) {
        Role roleDO = new Role();
        roleDO.setName(jaxbRole.getName());
        roleDO.setType(jaxbRole.getType());

        if (jaxbRole.getPermissions() != null
            && jaxbRole.getPermissions().getPermissions().size() > 0) {
            roleDO.setPermissions(permissionConverter
                .toPermissionListDO(jaxbRole.getPermissions()));
        }

        return roleDO;
    }

    public List<Role> toRoleListDO(com.rackspace.idm.jaxb.Roles jaxbRoles) {
        List<Role> roles = new ArrayList<Role>();

        for (com.rackspace.idm.jaxb.Role jaxbRole : jaxbRoles.getRoles()) {
            roles.add(toRoleDO(jaxbRole));
        }

        return roles;
    }

    public com.rackspace.idm.jaxb.Role toRoleJaxbWithoutPermissions(Role role) {
        return toRoleJaxb(role, false);
    }

    public com.rackspace.idm.jaxb.Role toRoleJaxbWithPermissions(Role role) {
        return toRoleJaxb(role, true);
    }

    private com.rackspace.idm.jaxb.Role toRoleJaxb(Role role,
        boolean includePermissions) {
        com.rackspace.idm.jaxb.Role jaxbRole = of.createRole();
        jaxbRole.setName(role.getName());
        jaxbRole.setType(role.getType());

        if (includePermissions && role.getPermissions() != null
            && role.getPermissions().size() > 0) {

            com.rackspace.idm.jaxb.PermissionList perms = permissionConverter
                .toPermissionListJaxb(role.getPermissions());

            jaxbRole.setPermissions(perms);
        }

        return jaxbRole;
    }

    public com.rackspace.idm.jaxb.Roles toRolesJaxb(List<Role> Roles) {
        if (Roles == null || Roles.size() < 1) {
            return null;
        }
        com.rackspace.idm.jaxb.Roles roles = of.createRoles();

        for (Role role : Roles) {
            roles.getRoles().add(toRoleJaxbWithoutPermissions(role));
        }
        return roles;
    }
}
