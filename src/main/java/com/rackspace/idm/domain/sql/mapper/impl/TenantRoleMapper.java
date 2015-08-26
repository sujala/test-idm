package com.rackspace.idm.domain.sql.mapper.impl;

import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.entity.TenantRole;
import com.rackspace.idm.domain.sql.entity.SqlRole;
import com.rackspace.idm.domain.sql.entity.SqlRoleRax;
import com.rackspace.idm.domain.sql.entity.SqlTenantRole;
import com.rackspace.idm.domain.sql.mapper.SqlMapper;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SQLComponent
public class TenantRoleMapper extends SqlMapper<TenantRole, SqlTenantRole> {

    private static final String FORMAT = "roleRsId=%s,cn=ROLES,rsId=%s,ou=users,o=rackspace,dc=rackspace,dc=com";

    /*
     *  Assign cloud-identity user's global roles to the "identity" tenant.
     *  Remove the "identity" tenant when building a tenantRole.
     */

    @Autowired
    private IdentityConfig config;

    @Override
    protected String getUniqueIdFormat() {
        return FORMAT;
    }

    @Override
    protected Object[] getIds(SqlTenantRole sqlTenantRole) {
        return new Object[] {sqlTenantRole.getRoleId(), sqlTenantRole.getActorId()};
    }

    public TenantRole fromSQL(List<SqlTenantRole> sqlTenantRoles){
        TenantRole tenantRole = null;
        for(SqlTenantRole sqlTenantRole : sqlTenantRoles){
            if(tenantRole == null){
                tenantRole = getTenantRole(sqlTenantRole);
            }
            String sqlTenantId =  sqlTenantRole.getTargetId();
            if(sqlTenantRole.getRoleId().equalsIgnoreCase(tenantRole.getRoleRsId())
                    && !tenantRole.getTenantIds().contains(sqlTenantId)){
                tenantRole.getTenantIds().add(sqlTenantId);
            }
        }
        return tenantRole;
    }

    public SqlTenantRole toSQL(TenantRole tenantRole){
        SqlTenantRole sqlTenantRole = super.toSQL(tenantRole);
        if (tenantRole.getTenantIds() == null || tenantRole.getTenantIds().size() == 0) {
            sqlTenantRole.setTargetId(config.getReloadableConfig().getIdentityRoleDefaultTenant());
        } else {
            sqlTenantRole.setTargetId(tenantRole.getTenantIds().iterator().next());
        }
        return sqlTenantRole;
    }

    public List<TenantRole> fromSQLList(List<SqlTenantRole> sqlTenantRoles){
        HashMap<TenantRoleId, TenantRole> tenantRoleHashMap = new HashMap<TenantRoleId, TenantRole>();
        for(SqlTenantRole sqlTenantRole : sqlTenantRoles){
            TenantRoleId roleIdUserId = new TenantRoleId(sqlTenantRole.getRoleId(), sqlTenantRole.getActorId());
            if(tenantRoleHashMap.containsKey(roleIdUserId)){
                TenantRole tenantRole = tenantRoleHashMap.get(roleIdUserId);
                tenantRole.getTenantIds().add(sqlTenantRole.getTargetId());
            } else{
                TenantRole tenantRole = getTenantRole(sqlTenantRole);
                tenantRoleHashMap.put(roleIdUserId, tenantRole);
            }
        }

        return new ArrayList<TenantRole>(tenantRoleHashMap.values());
    }

    public List<SqlTenantRole> toSQLList(TenantRole tenantRole){
        List<String> tenantids = new ArrayList<String>();
        if (tenantRole.getTenantIds().size() == 0) {
            tenantids.add(config.getReloadableConfig().getIdentityRoleDefaultTenant());
        } else {
            tenantids.addAll(tenantRole.getTenantIds());
        }

        List<SqlTenantRole> sqlTenantRoles = new ArrayList<SqlTenantRole>();
        for(String tenantId : tenantids){
            SqlTenantRole sqlTenantRole = new SqlTenantRole();
            sqlTenantRole.setActorId(tenantRole.getUserId());
            sqlTenantRole.setTargetId(tenantId);
            sqlTenantRole.setRoleId(tenantRole.getRoleRsId());
            if(tenantRole.getPropagate() != null) {
                sqlTenantRole.setInherited(tenantRole.getPropagate());
            }

            sqlTenantRoles.add(sqlTenantRole);
        }
        return sqlTenantRoles;
    }

    @Override
    public void overrideFields(Map<String, String> map){
        map.put("actorId", "userId");
        map.put("roleId", "roleRsId");
        map.put("inherited", "propagate");
    }

    private TenantRole getTenantRole(SqlTenantRole sqlTenantRole) {
        TenantRole tenantRole = super.fromSQL(sqlTenantRole);

        String tenantId = sqlTenantRole.getTargetId();
        if(!tenantId.equalsIgnoreCase(config.getReloadableConfig().getIdentityRoleDefaultTenant())){
            tenantRole.getTenantIds().add(sqlTenantRole.getTargetId());
        }

        SqlRole sqlRole = sqlTenantRole.getSqlRole();
        if(sqlRole != null){
            tenantRole.setName(sqlRole.getName());
        }

        SqlRoleRax sqlRoleRax = sqlTenantRole.getSqlRole().getRax();
        if(sqlRoleRax != null){
            tenantRole.setClientId(sqlRoleRax.getClientId());
            tenantRole.setDescription(sqlRoleRax.getDescription());
        }
        return tenantRole;
    }

    @Data
    private class TenantRoleId {
        private String actorId;
        private String roleId;

        public TenantRoleId(String actorId, String roleId) {
            this.actorId = actorId;
            this.roleId = roleId;
        }
    }
}
