package com.rackspace.idm.domain.entity;

import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.unboundid.ldap.sdk.ReadOnlyEntry;
import com.unboundid.ldap.sdk.persist.FilterUsage;
import com.unboundid.ldap.sdk.persist.LDAPEntryField;
import com.unboundid.ldap.sdk.persist.LDAPField;
import com.unboundid.ldap.sdk.persist.LDAPObject;
import lombok.Data;
import org.apache.commons.lang.StringUtils;
import org.dozer.Mapping;

import java.util.List;
import java.util.regex.*;
import java.util.regex.Pattern;

@Data
@LDAPObject(structuralClass= LdapRepository.OBJECTCLASS_RACKER)
public class Racker implements BaseUser, FederatedBaseUser {

    //TODO: Not sure why this property is needed. Look into and remove if not necessary
    private String uniqueId;

    @LDAPEntryField()
    private ReadOnlyEntry ldapEntry;

    @Mapping("id")
    @LDAPField(attribute=LdapRepository.ATTR_RACKER_ID,
            objectClass=LdapRepository.OBJECTCLASS_RACKER,
            inRDN=true,
            filterUsage= FilterUsage.ALWAYS_ALLOWED,
            requiredForEncode=true)
    private String rackerId;

    private Boolean enabled;

    private String username;

    private List<String> rackerRoles;

    @Override
    public String getAuditContext() {
        return String.format("Racker(%s)", rackerId);
    }

    public boolean isDisabled() {
        return this.enabled == null ? false : !this.enabled;
    }

    @Override
    public String getUniqueId() {
        if (ldapEntry == null) {
            return null;
        } else {
            return ldapEntry.getDN();
        }
    }

    /**
     * BaseUser requires this method, but it's irrelevant in context of Racker so return null
     *
     * @return
     */
    public String getDomainId(){
        return null;
    }

    @Override
    public String getId() {
        return getRackerId();
    }

    /**
     * Alternative name for setting the rackerId
     * @param id
     */
    @Override
    public void setId(String id) {
        rackerId = id;
    }

    public boolean isFederatedRacker() {
        return isFederatedRackerId(rackerId);
    }

    /**
     * Return the identity provider URI (part of rackerId suffixed to end of '@' or null
     *
     * @return
     */
    public String getFederatedIdpUri() {
        return getIdpUriFromFederatedId(rackerId);
    }

    public String getFederatedUserName() {
        if (StringUtils.isBlank(rackerId)) {
            return null;
        }
        return getUsernameFromFederatedId(rackerId);
    }

    public String getUsername() {
        String finalUserName = username;
        if (StringUtils.isBlank(username) && StringUtils.isNotBlank(rackerId)) {
            FederatedRackerIdParts parts = new FederatedRackerIdParts(rackerId);
            if (parts.isValidFederatedRackerId()) {
                finalUserName = parts.username;
            }
        }
        return finalUserName;
    }

    /**
     * A given id is considered a "federated" racker id if it matces the federated regex and the idp is not blank.
     * @param id
     * @return
     */
    public static boolean isFederatedRackerId(String id) {
        FederatedRackerIdParts splitId = new FederatedRackerIdParts(id);
        return splitId.isValidFederatedRackerId();
    }

    public static String asFederatedRackerId(String rackerUsername, String federatedIdpUri) {
        FederatedRackerIdParts parts = new FederatedRackerIdParts(rackerUsername, federatedIdpUri);
        if (!parts.isValidFederatedRackerId()) {
            throw new IllegalArgumentException(String.format("Provided username '%s' and idp '%s' are not valid for federated racker id", rackerUsername, federatedIdpUri));
        }
        return parts.asRackerId();
    }

    public static String getIdpUriFromFederatedId(String id) {
        FederatedRackerIdParts splitId = new FederatedRackerIdParts(id);
        return splitId.idpUri;
    }

    public static String getUsernameFromFederatedId(String id) {
        FederatedRackerIdParts splitId = new FederatedRackerIdParts(id);
        return splitId.username;
    }

    private static class FederatedRackerIdParts {
        public static final String FEDERATED_RACKER_ID_PATTERN = "%s@%s";
        public static final java.util.regex.Pattern RACKER_ID_PATTERN = Pattern.compile("^(.+)@(.+)$");

        private String username;
        private String idpUri;

        public FederatedRackerIdParts(String rackerId) {
            if (StringUtils.isNotBlank(rackerId)) {
                Matcher matcher = RACKER_ID_PATTERN.matcher(rackerId);
                boolean matchFound = matcher.find();
                if (matchFound) {
                    username = matcher.group(1);
                    idpUri = matcher.group(2);
                }
            }
        }

        public FederatedRackerIdParts(String username, String idpUri) {
            this.username = username;
            this.idpUri = idpUri;
        }

        public boolean isValidFederatedRackerId() {
            return StringUtils.isNotBlank(idpUri) && StringUtils.isNotBlank(username);
        }

        public String asRackerId() {
            if (!isValidFederatedRackerId()) {
                throw new IllegalStateException("Can not generate federated racker id from current state. Null idp or username");
            }
            return String.format(FEDERATED_RACKER_ID_PATTERN, username, idpUri);
        }
    }
}
