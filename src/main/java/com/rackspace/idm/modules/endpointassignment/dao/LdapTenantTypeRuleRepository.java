package com.rackspace.idm.modules.endpointassignment.dao;

import com.rackspace.idm.annotation.LDAPComponent;
import com.rackspace.idm.domain.dao.impl.LdapGenericRepository;
import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.rackspace.idm.modules.endpointassignment.Constants;
import com.rackspace.idm.modules.endpointassignment.entity.TenantTypeRule;
import com.unboundid.ldap.sdk.Filter;

/**
 * Responsible for storing and retrieving Tenant Type Endpoint Assignment Rules to/from the LDAP repository
 */
@LDAPComponent
public class LdapTenantTypeRuleRepository extends LdapGenericRepository<TenantTypeRule> implements TenantTypeRuleDao {

    @Override
    public TenantTypeRule getById(String id) {
        return getObject(searchByIdFilter(id));
    }

    @Override
    public void addEndpointAssignmentRule(TenantTypeRule endpointAssignmentRule) {
        addObject(endpointAssignmentRule);
    }

    @Override
    public void updateEndpointAssignmentRule(TenantTypeRule endpointAssignmentRule) {
        updateObjectAsIs(endpointAssignmentRule);
    }

    @Override
    public void deleteEndpointAssignmentRule(TenantTypeRule endpointAssignmentRule) {
        deleteObject(endpointAssignmentRule);
    }

    @Override
    public int countRulesByTenantType(String tenantType) {
        return countObjects(searchByTenantType(tenantType));
    }

    private Filter searchByIdFilter(String id) {
        return new LdapSearchBuilder()
                .addEqualAttribute(TenantTypeRule.LDAP_ATTRIBUTE_CN, id)
                .addEqualAttribute(ATTR_OBJECT_CLASS, TenantTypeRule.OBJECT_CLASS).build();
    }

    private Filter searchByTenantType(String tenantType) {
        return new LdapSearchBuilder()
                .addEqualAttribute(LdapRepository.ATTR_TYPE, tenantType)
                .addEqualAttribute(ATTR_OBJECT_CLASS, TenantTypeRule.OBJECT_CLASS).build();
    }

    @Override
    public String getBaseDn() {
        return Constants.ENDPOINT_ASSIGNMENT_RULE_BASE_DN;
    }

    @Override
    public String getLdapEntityClass(){
        return TenantTypeRule.OBJECT_CLASS;
    }

    @Override
    public String getSortAttribute() {
        return TenantTypeRule.LDAP_ATTRIBUTE_CN;
    }
}
