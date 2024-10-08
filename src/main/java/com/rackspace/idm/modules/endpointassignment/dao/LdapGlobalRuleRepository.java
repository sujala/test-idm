package com.rackspace.idm.modules.endpointassignment.dao;

import com.rackspace.idm.annotation.LDAPComponent;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.dao.DaoGetEntityType;
import com.rackspace.idm.domain.dao.impl.LdapGenericRepository;
import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.rackspace.idm.exception.SizeLimitExceededException;
import com.rackspace.idm.modules.endpointassignment.Constants;
import com.rackspace.idm.modules.endpointassignment.entity.Rule;
import com.rackspace.idm.modules.endpointassignment.entity.TenantTypeRule;
import com.unboundid.ldap.sdk.*;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@LDAPComponent
public class LdapGlobalRuleRepository extends LdapGenericRepository<Rule> implements GlobalRuleDao, DaoGetEntityType {

    public static final String SIZE_EXCEEDED_ERROR_MESSAGE = "Aborting loading all assignment rules. Result size exceeded max directory limit";
    public static final String SIZE_EXCEEDED_EXCEPTION_MESSAGE = "Result size exceeded.";

    private static Filter TENANT_TYPE_RULE_CLASS_FILTER = Filter.createEqualityFilter(ATTR_OBJECT_CLASS, TenantTypeRule.OBJECT_CLASS);

    private static List<Filter> ALL_ENDPOINT_ASSIGNMENT_CLASS_FILTERS = Arrays.asList(TENANT_TYPE_RULE_CLASS_FILTER);

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    TenantTypeRuleDao ldapTenantTypeRuleRepository;

    @Autowired
    IdentityConfig identityConfig;

    @Override
    public Rule getById(String id) {
        return getObject(searchFilterGetRuleById(id, ALL_ENDPOINT_ASSIGNMENT_CLASS_FILTERS), SearchScope.SUB);
    }

    @Override
    public List<Rule> findAll() {
        try {
            List<Rule> rules = getUnpagedUnsortedObjects(searchFilterGetAllRulesByClass(ALL_ENDPOINT_ASSIGNMENT_CLASS_FILTERS), getBaseDn(), SearchScope.SUB, identityConfig.getReloadableConfig().getMaxDirectoryPageSize());
            return rules;
        } catch (LDAPSearchException ldapEx) {
            if (ldapEx.getResultCode() == ResultCode.SIZE_LIMIT_EXCEEDED) {
                logger.debug(SIZE_EXCEEDED_ERROR_MESSAGE, ldapEx);
                throw new SizeLimitExceededException(SIZE_EXCEEDED_EXCEPTION_MESSAGE, ldapEx);
            } else {
                throw new IllegalStateException(ldapEx);
            }
        }
    }

    @Override
    public void deleteEndpointAssignmentRule(String id) {
        deleteObject(searchFilterGetRuleById(id, ALL_ENDPOINT_ASSIGNMENT_CLASS_FILTERS));
    }

    @Override
    public List<Rule> findByTenantTypes(Set<String> types) {
        try {
            List<Rule> rules = getUnpagedUnsortedObjects(searchFilterGetRuleByTypes(types, ALL_ENDPOINT_ASSIGNMENT_CLASS_FILTERS), getBaseDn(), SearchScope.SUB);
            return rules;
        } catch (LDAPSearchException ldapEx) {
            if (ldapEx.getResultCode() == ResultCode.SIZE_LIMIT_EXCEEDED) {
                logger.debug(SIZE_EXCEEDED_ERROR_MESSAGE, ldapEx);
                throw new SizeLimitExceededException(SIZE_EXCEEDED_EXCEPTION_MESSAGE, ldapEx);
            } else {
                throw new IllegalStateException(ldapEx);
            }
        }

    }

    @Override
    public String getBaseDn() {
        return Constants.ENDPOINT_ASSIGNMENT_RULE_BASE_DN;
    }

    @Override
    public Class getEntityType(SearchResultEntry entry) {
        //NOTE: order precedence is important. Base classes should be further down in the check list.
        Attribute objClass = entry.getAttribute(ATTR_OBJECT_CLASS);
        if (objClass.hasValue(TenantTypeRule.OBJECT_CLASS)) {
            return TenantTypeRule.class;
        } else {
            throw new IllegalStateException("Unrecognized endpoint rule type");
        }
    }

    @Override
    public List<Rule> findEndpointAssignmentRulesForEndpointTemplateId(String endpointTemplateId) {
        if (!StringUtils.isNumeric(endpointTemplateId)) {
            return new ArrayList<>();
        }

        try {
            List<Rule> rules = getUnpagedUnsortedObjects(searchFilterGetRulesByEndpointTemplateId(endpointTemplateId, ALL_ENDPOINT_ASSIGNMENT_CLASS_FILTERS), getBaseDn(), SearchScope.SUB);
            return rules;
        } catch (LDAPSearchException ldapEx) {
            if (ldapEx.getResultCode() == ResultCode.SIZE_LIMIT_EXCEEDED) {
                logger.debug(SIZE_EXCEEDED_ERROR_MESSAGE, ldapEx);
                throw new SizeLimitExceededException(SIZE_EXCEEDED_EXCEPTION_MESSAGE, ldapEx);
            } else {
                throw new IllegalStateException(ldapEx);
            }
        }

    }

    private Filter searchFilterGetRuleById(String id, List<Filter> classFilterList) {
        Filter classFilter = Filter.createORFilter(classFilterList);
        return Filter.createANDFilter(classFilter, Filter.createEqualityFilter(Rule.LDAP_ATTRIBUTE_CN, id));
    }

    private Filter searchFilterGetAllRulesByClass(List<Filter> classFilterList) {
        Filter classFilter = Filter.createORFilter(classFilterList);
        return classFilter;
    }

    private Filter searchFilterGetRuleByTypes(Set<String> types, List<Filter> classFilterList) {
        Filter classFilter = Filter.createORFilter(classFilterList);

        List<Filter> typeFilterList = new ArrayList<>();

        for (String type : types) {
            typeFilterList.add(Filter.createEqualityFilter(TenantTypeRule.LDAP_ATTRIBUTE_TYPE, type));
        }

        return Filter.createANDFilter(classFilter, Filter.createORFilter(typeFilterList));
    }

    private Filter searchFilterGetRulesByEndpointTemplateId(String endpointTemplateId, List<Filter> classFilterList) {
        Filter classFilter = Filter.createORFilter(classFilterList);
        return Filter.createANDFilter(classFilter, Filter.createEqualityFilter(LdapRepository.ATTR_MEMBER, TenantTypeRule.convertEndpointTemplateIdToUniqueId(Integer.parseInt(endpointTemplateId))));
    }

}
