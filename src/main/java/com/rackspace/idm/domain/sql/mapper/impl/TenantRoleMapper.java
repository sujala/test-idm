package com.rackspace.idm.domain.sql.mapper.impl;

import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.entity.TenantRole;
import com.rackspace.idm.domain.sql.entity.SqlRole;
import com.rackspace.idm.domain.sql.entity.SqlRoleRax;
import com.rackspace.idm.domain.sql.entity.SqlTenantRole;
import com.rackspace.idm.domain.sql.mapper.SqlMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class TenantRoleMapper extends SqlMapper<TenantRole, SqlTenantRole> {

    /*
     *  Assign cloud-identity user's global roles to the "identity" tenant.
     *  Remove the "identity" tenant when building a tenantRole.
     */

    @Autowired
    IdentityConfig config;

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
        sqlTenantRole.setTargetId(tenantRole.getTenantIds().iterator().next());
        return sqlTenantRole;
    }

    public List<TenantRole> fromSQLList(List<SqlTenantRole> sqlTenantRoles){
        HashMap<String, TenantRole> tenantRoleHashMap = new HashMap<String, TenantRole>();
        for(SqlTenantRole sqlTenantRole : sqlTenantRoles){
            String roleId = sqlTenantRole.getRoleId();
            if(tenantRoleHashMap.containsKey(roleId)){
                TenantRole tenantRole = tenantRoleHashMap.get(roleId);
                tenantRole.getTenantIds().add(sqlTenantRole.getTargetId());
            } else{
                TenantRole tenantRole = getTenantRole(sqlTenantRole);
                tenantRoleHashMap.put(roleId, tenantRole);
            }
        }

        return new ArrayList<TenantRole>(tenantRoleHashMap.values());
    }

    public List<SqlTenantRole> toSQLList(TenantRole tenantRole){
        List<SqlTenantRole> sqlTenantRoles = new ArrayList<SqlTenantRole>();
        for(String tenantId : tenantRole.getTenantIds()){
            SqlTenantRole sqlTenantRole = new SqlTenantRole();
            sqlTenantRole.setActorId(tenantRole.getUserId());
            if(tenantRole.getTenantIds().size() == 0){
                sqlTenantRole.setTargetId(config.getReloadableConfig().getIdentityRoleDefaultTenant());
            } else {
                sqlTenantRole.setTargetId(tenantId);
            }
            sqlTenantRole.setRoleId(tenantRole.getRoleRsId());
            sqlTenantRole.setInherited(tenantRole.getPropagate());

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
}
