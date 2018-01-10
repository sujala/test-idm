package com.rackspace.idm.modules.endpointassignment.entity;

import com.rackspace.idm.ErrorCodes;
import com.rackspace.idm.domain.dao.UniqueId;
import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.rackspace.idm.domain.entity.Auditable;
import com.rackspace.idm.domain.entity.EndUser;
import com.rackspace.idm.domain.entity.Tenant;
import com.rackspace.idm.modules.endpointassignment.Constants;
import com.rackspace.idm.validation.Validator20;
import com.unboundid.ldap.sdk.DN;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.persist.FilterUsage;
import com.unboundid.ldap.sdk.persist.LDAPDNField;
import com.unboundid.ldap.sdk.persist.LDAPField;
import com.unboundid.ldap.sdk.persist.LDAPObject;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections.CollectionUtils;
import org.hibernate.validator.constraints.Length;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.*;

@Getter
@Setter
@LDAPObject(structuralClass = TenantTypeRule.OBJECT_CLASS, superiorClass={ "groupOfNames",
        "top" })
public class TenantTypeRule implements Auditable, UniqueId, Rule {

    public static final String OBJECT_CLASS = "rsTenantTypeEndpointAssignmentRule";
    public static final String LDAP_ATTRIBUTE_TYPE = LdapRepository.ATTR_RS_TYPE;
    public static final String LDAP_ATTRIBUTE_ENDPOINT_TEMPLATE_IDS = "rsEndpointTemplateIds";
    public static final String LDAP_ATTRIBUTE_MEMBER = LdapRepository.ATTR_MEMBER;

    @LDAPDNField
    private String uniqueId;

    @LDAPField(attribute = LDAP_ATTRIBUTE_CN, objectClass = OBJECT_CLASS, inRDN = true, requiredForEncode = true)
    private String id;

    @Length(min = 0, max = 255)
    @LDAPField(attribute = LDAP_ATTRIBUTE_DESCRIPTION, objectClass = OBJECT_CLASS, requiredForEncode = false)
    private String description;

    @NotNull()
    @Length(min=1, max = 16)
    @LDAPField(attribute = LDAP_ATTRIBUTE_TYPE, objectClass = OBJECT_CLASS, requiredForEncode = false)
    private String tenantType;

    @Size(min = 0, max = 1000)
    @LDAPField(attribute=LdapRepository.ATTR_MEMBER,
            objectClass = OBJECT_CLASS,
            filterUsage= FilterUsage.CONDITIONALLY_ALLOWED)
    private Set<String> endpointTemplateMembers;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public String getAuditContext() {
        String format = "ruleId=%s";
        return String.format(format, getId());
    }

    @Override
    public String toString() {
        return getAuditContext();
    }

    /**
     * Adds an endpoint template (CloudBaseUrl) with the specified ID to the list of endpoint templates associated with
     * this rule.
     *
     * @param endpointTemplateIdentifier the string representation of the DN template. This can be retrieved via
     * {@link CloudBaseUrl#getUniqueId()}
     */
    public void addEndpointTemplate(int endpointTemplateIdentifier) {
        if (endpointTemplateMembers == null) {
            endpointTemplateMembers = new HashSet<String>(1);
        }
        endpointTemplateMembers.add(TenantTypeRule.convertEndpointTemplateIdToUniqueId(endpointTemplateIdentifier));
    }

    /**
     * Removes the endpoint template (CloubBaseUrl) with the specified ID from the list of endpoint templates associated
     * with this rule.
     *
     * @param endpointTemplateIdentifier
     * @return true - if the endpoint was associated with the rule and therefore removed; false otherwise
     */
    public boolean removeEndpointTemplate(int endpointTemplateIdentifier) {
        if (CollectionUtils.isNotEmpty(endpointTemplateMembers)) {
            return endpointTemplateMembers.remove(TenantTypeRule.convertEndpointTemplateIdToUniqueId(endpointTemplateIdentifier));
        }
        return false;
    }

    public List<String> getEndpointTemplateIds() {
        List<String> ids = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(endpointTemplateMembers)) {
            for (String endpointTemplateMember : endpointTemplateMembers) {
                try {
                    DN dn = new DN(endpointTemplateMember);
                    ids.add(dn.getRDN().getAttributeValues()[0]);
                } catch (LDAPException e) {
                    //Log the error, but an error with one endpoint must not cause the whole rule to fail
                    String msg = ErrorCodes.generateErrorCodeFormattedMessage(ErrorCodes.ERROR_CODE_EP_MISSING_ENDPOINT, String.format(
                            "Tenant rule '%s' has an invalid endpoint template with DN '%s'", getId(), endpointTemplateMember));
                    logger.error(msg);
                }
            }
        }
        return ids;
    }

    public static String convertEndpointTemplateIdToUniqueId(int id) {
        try {
            DN dn = new DN("rsId=" + id + "," + Constants.ENDPOINT_TEMPLATE_BASE_DN);
            return dn.toString();
        } catch (LDAPException e) {
            throw new IllegalArgumentException(String.format("The endpoint template id '%s' could not be converted to a unique id", id));
        }
    }

    /**
     * Whether or not this particular tenant rule would apply to the specified tenant
     *
     * @param tenant
     * @return
     */
    @Override
    public boolean matches(EndUser user, Tenant tenant) {
        return tenant.getTypes().contains(getTenantType());
    }

    /**
     * Tenant Type rules are an all or nothing based on the tenant contained the rule's tenant type. If matched,
     * all endpoints in the rule apply.
     *
     * @param user
     * @param tenant
     * @return
     */
    @Override
    public List<String> matchingEndpointTemplateIds(EndUser user, Tenant tenant) {
        if (matches(user, tenant)) {
            return getEndpointTemplateIds();
        }
        return Collections.emptyList();
    }

}