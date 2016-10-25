package com.rackspace.idm.modules.endpointassignment.service;

import com.rackspace.idm.ErrorCodes;
import com.rackspace.idm.domain.entity.CloudBaseUrl;
import com.rackspace.idm.domain.service.EndpointService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.modules.endpointassignment.dao.GlobalRuleDao;
import com.rackspace.idm.modules.endpointassignment.dao.TenantTypeRuleDao;
import com.rackspace.idm.modules.endpointassignment.entity.Rule;
import com.rackspace.idm.modules.endpointassignment.entity.TenantTypeRule;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
public class DefaultRuleService implements RuleService {

    @Autowired
    TenantTypeRuleDao tenantTypeRuleDao;

    @Autowired
    GlobalRuleDao globalRuleDao;

    @Autowired
    private EndpointService endpointService;

    private Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Override
    public void deleteEndpointAssignmentRule(String ruleId) {
        try {
            globalRuleDao.deleteEndpointAssignmentRule(ruleId);
        } catch (NotFoundException e) {
            throw new NotFoundException("The specified rule does not exist");
        }
    }

    @Override
    public Rule getEndpointAssignmentRule(String ruleId) {
        return globalRuleDao.getById(ruleId);
    }

    @Override
    public List<Rule> findAllEndpointAssignmentRules() {
        return globalRuleDao.findAll();
    }

    @Override
    public List<Rule> findEndpointAssignmentRulesForTenantType(Set<String> types) {
        return globalRuleDao.findByTenantTypes(types);
    }

    @Override
    public Rule addEndpointAssignmentRule(Rule endpointAssignmentRule) {
        if (endpointAssignmentRule instanceof TenantTypeRule) {
            TenantTypeRule rule = (TenantTypeRule) endpointAssignmentRule;
            return addTenantTypeRule(rule);
        }
        throw new IllegalArgumentException(String.format("The supplied rule of type '%s' is not supported", endpointAssignmentRule.getClass().getSimpleName()));
    }

    private TenantTypeRule addTenantTypeRule(TenantTypeRule tenantTypeRule) {
        //perform validation of the rule
        validate(tenantTypeRule);

        //On create, both an empty string and null mean the same thing - the attribute should NOT be set.
        if (StringUtils.isBlank(tenantTypeRule.getDescription())) {
            tenantTypeRule.setDescription(null);
        }

        //validate the linked endpoint templates exist
        for (String id : tenantTypeRule.getEndpointTemplateIds()) {
            CloudBaseUrl cloudBaseUrl = endpointService.getBaseUrlById(id);
            if (cloudBaseUrl == null) {
                throw new BadRequestException(String.format("Endpoint template '%s' does not exist", id), ErrorCodes.ERROR_CODE_EP_MISSING_ENDPOINT);
            }
        }

        tenantTypeRule.setId(UUID.randomUUID().toString().replace("-", ""));
        tenantTypeRuleDao.addEndpointAssignmentRule(tenantTypeRule);
        return tenantTypeRule;
    }

    private void validate(Object obj) {
        List<String> messages = new ArrayList<String>();
        Set<ConstraintViolation<Object>> violations = validator.validate(obj);
        for (ConstraintViolation<Object> violation : violations) {
            messages.add(String.format("%s: %s", violation.getPropertyPath(), violation.getMessage()));
        }
        if (messages.size() > 0) {
            throw new BadRequestException(StringUtils.join(messages, "; "));
        }
    }
}
