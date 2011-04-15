package com.rackspace.idm.domain.entity;

import java.util.Locale;

import com.rackspace.idm.validation.MessageTexts;
import com.rackspace.idm.validation.RegexPatterns;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.rackspace.idm.GlobalConstants;
import java.util.List;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

public class User extends BaseUser implements Auditable {
    private static final long serialVersionUID = 1347677880811855274L;

    @NotNull
    @Pattern(regexp = RegexPatterns.EMAIL_ADDRESS, message = MessageTexts.EMAIL)
    private String email = null;


    private UserCredential credential = new UserCredential();
    private String personId = null;

    private String uniqueId = null;
    private UserHumanName name = new UserHumanName();
    private UserLocale preference = new UserLocale();
    private String country = null;
    private String displayName = null;
    private String inum = null;
    private String iname = null;
    private Boolean locked = null;
    private String orgInum = null;
    private String apiKey = null;
    private UserStatus status = null;

    private Boolean softDeleted = null;
    private String region = null;
    
    private String nastId = null;
    private Integer mossoId = null;
    
    private DateTime created;
    private DateTime updated;
    
    private DateTime softDeletedTimestamp;
    
    private Boolean maxLoginFailuresExceded = null;
    
    public User() {
        // Needed by JAX-RS
    }

    @Deprecated
    public User(String username) {
        super(username);
    }

    @Deprecated
    public User(String username, String customerId, String email,
        UserHumanName name, UserLocale pref, UserCredential cred) {
        super(username, customerId);
        this.email = email;
        this.name = name;
        this.preference = pref;
        this.credential = cred;
    }

    @Deprecated
    public User(String username, String customerId, String email,
        UserHumanName name, UserLocale preference, UserCredential credential,
        String country, String displayName, String inum, String iname,
        String orgInum, String apiKey, UserStatus status,
        String personId) {
        super.setUsername(username);
        super.setCustomerId(customerId);
        this.email = email;
        this.name = name;
        this.preference = preference;
        this.credential = credential;
        this.country = country;
        this.displayName = displayName;
        this.inum = inum;
        this.iname = iname;
        this.orgInum = orgInum;
        this.apiKey = apiKey;
        this.status = status;
        this.personId = personId;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(String uniqueId) {
        if (uniqueId != null) {
            this.uniqueId = uniqueId;
        }
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        if (country != null) {
            this.country = country;
        }
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        if (region != null) {
            this.region = region;
        }
    }

    public Boolean isSoftDeleted() {
        return softDeleted;
    }

    public void setSoftDeleted(Boolean softDeleted) {
        this.softDeleted = softDeleted;
    }

    public String getDisplayName() {
        if (StringUtils.isBlank(displayName)) {
            return (name.getFirstname() + " " + name.getLastname()).trim();
        }
        return displayName;
    }

    public void setDisplayName(String displayName) {
        if (displayName != null) {
            this.displayName = displayName;
        }
    }

    public String getIname() {
        return iname;
    }

    public void setIname(String iname) {
        if (iname != null) {
            this.iname = iname;
        }
    }

    public Boolean isLocked() {
        return locked;
    }

    public void setLocked(Boolean isLocked) {
        this.locked = isLocked;
    }
    
    public Boolean isMaxLoginFailuresExceded() {
        return maxLoginFailuresExceded;
    }
    
    public void setMaxLoginFailuresExceded(Boolean maxLoginFailuresExceded) {
        this.maxLoginFailuresExceded = maxLoginFailuresExceded;
    }

    public String getOrgInum() {
        return orgInum;
    }

    public void setOrgInum(String orgInum) {
        if (orgInum != null) {
            this.orgInum = orgInum;
        }
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        if (apiKey != null) {
            this.apiKey = apiKey;
        }
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        if (status != null) {
            this.status = status;
        }
    }

    @Override
    public String getUsername() {
        return super.getUsername();
    }

    @Override
    public void setUsername(String username) {
        super.setUsername(username);
    }

    public void setPasswordObj(Password password) {
        if (password != null) {
            this.credential.setPassword(password);
        }
    }

    public Password getPasswordObj() {
        return credential.getPassword();
    }

    public void setPassword(String password) {
        if (password != null) {
            this.credential.setPassword(Password.newInstance(password));
        }
    }

    public String getPassword() {
        return credential.getPassword().getValue();
    }

    public String getPasswordNoPrefix() {
        return credential.getPassword().getValueNoPrefix();
    }

    public void setFirstname(String firstname) {
        if (firstname != null) {
            this.name.setFirstname(firstname);
        }
    }

    public String getFirstname() {
        return name.getFirstname();
    }

    public void setLastname(String lastname) {
        if (lastname != null) {
            this.name.setLastname(lastname);
        }
    }

    public String getLastname() {
        return name.getLastname();
    }

    public void setEmail(String email) {
        if (email != null) {
            this.email = email;
        }
    }

    public String getEmail() {
        return email;
    }

    public String getMiddlename() {
        return name.getMiddlename();
    }

    public void setMiddlename(String middlename) {
        if (middlename != null) {
            this.name.setMiddlename(middlename);
        }
    }

    public Locale getLocale() {
        return preference.getLocale();
    }

    public void setLocale(Locale preferredLang) {
        if (preferredLang != null) {
            this.preference.setLocale(preferredLang);
        }
    }

    public String getPreferredLang() {
        return preference.getPreferredLang();
    }

    public void setPreferredLang(String preferredLang) {
        if (StringUtils.isBlank(preferredLang)) {
            return;
        }
        preference.setPreferredLang(preferredLang);
    }

    public DateTimeZone getTimeZoneObj() {
        return preference.getTimeZone();
    }

    public void setTimeZoneObj(DateTimeZone timeZone) {
        if (timeZone != null) {
            this.preference.setTimeZone(timeZone);
        }
    }

    public String getTimeZone() {
        return preference.getTimeZone().toString();
    }

    public void setTimeZone(String timeZone) {
        if (StringUtils.isBlank(timeZone)) {
            return;
        }
        preference.setTimeZone(DateTimeZone.forID(timeZone));
    }

    // TODO jeo check all places where these are used!
    public String getSecretQuestion() {
        return credential.getSecretQuestion();
    }

    public void setSecretQuestion(String secretQuestion) {
        if (secretQuestion != null) {
            this.credential.setSecretQuestion(secretQuestion);
        }
    }

    public String getSecretAnswer() {
        return credential.getSecretAnswer();
    }

    public void setSecretAnswer(String secretAnswer) {
        if (secretAnswer != null) {
            this.credential.setSecretAnswer(secretAnswer);
        }
    }

    public String getInum() {
        return inum;
    }

    public void setInum(String inum) {
        if (inum != null) {
            this.inum = inum;
        }
    }

    public String getPersonId() {
        return personId;
    }

    public void setPersonId(String personId) {
        if (personId != null) {
            this.personId = personId;
        }
    }
    
    public String getNastId() {
        return nastId;
    }

    public void setNastId(String nastId) {
        this.nastId = nastId;
    }

    public Integer getMossoId() {
        return mossoId;
    }

    public void setMossoId(Integer mossoId) {
        this.mossoId = mossoId;
    }

    public DateTime getCreated() {
        return created;
    }

    public void setCreated(DateTime created) {
        this.created = created;
    }

    public DateTime getSoftDeleteTimestamp() {
        return softDeletedTimestamp;
    }

    public void setSoftDeletedTimestamp(DateTime softDeletedTimestamp) {
        this.softDeletedTimestamp = softDeletedTimestamp;
    }
    
    public DateTime getUpdated() {
        return updated;
    }

    public void setUpdated(DateTime updated) {
        this.updated = updated;
    }
    
    
    public boolean isDisabled() {
        boolean disabled = false;
        disabled = this.isLocked() == null ? disabled : disabled || this.isLocked().booleanValue();
        disabled = this.isSoftDeleted() == null ? disabled : disabled || this.isSoftDeleted().booleanValue();
        disabled = this.getStatus() == null ? disabled : disabled || this.getStatus().equals(UserStatus.INACTIVE);
        return disabled;
    }

    @Override
    public void setCustomerId(String customerId) {
        super.setCustomerId(customerId);
    }

    @Override
    public void setGroups(List<ClientGroup> groups) {
        super.setGroups(groups);
    }

    public void setDefaults() {
        if (this.preference.getLocale() == null) {
            this.setPreferredLang(GlobalConstants.USER_PREFERRED_LANG_DEFAULT);
        }

        if (this.getTimeZoneObj() == null) {
            this.setTimeZone(GlobalConstants.USER_TIME_ZONE_DEFAULT);
        }

        this.setLocked(false);
        this.setSoftDeleted(false);
        this.setStatus(UserStatus.ACTIVE);
    }

    public BaseUser getBaseUser() {
        BaseUser baseUser = new BaseUser();
        baseUser.setCustomerId(getCustomerId());
        baseUser.setUsername(getUsername());
        baseUser.setGroups(getGroups());
        return baseUser;
    }

    public void copyChanges(User modifiedUser) {

        if (!StringUtils.isBlank(modifiedUser.getPersonId())) {
            setPersonId(modifiedUser.getPersonId());
        }

        if (!StringUtils.isBlank(modifiedUser.getFirstname())) {
            setFirstname(modifiedUser.getFirstname());
        }

        if (!StringUtils.isBlank(modifiedUser.getMiddlename())) {
            setMiddlename(modifiedUser.getMiddlename());
        }

        if (!StringUtils.isBlank(modifiedUser.getLastname())) {
            setLastname(modifiedUser.getLastname());
        }

        if (!StringUtils.isBlank(modifiedUser.getDisplayName())) {
            setDisplayName(modifiedUser.getDisplayName());
        }

        if (!StringUtils.isBlank(modifiedUser.getEmail())) {
            setEmail(modifiedUser.getEmail());
        }

        if (!StringUtils.isBlank(modifiedUser.getPreferredLang())) {
            setPreferredLang(modifiedUser.getPreferredLang());
        }

        if (!StringUtils.isBlank(modifiedUser.getTimeZone())) {
            setTimeZone(modifiedUser.getTimeZone());
        }

        if (!StringUtils.isBlank(modifiedUser.getCountry())) {
            setCountry(modifiedUser.getCountry());
        }

        if (!StringUtils.isBlank(modifiedUser.getRegion())) {
            setRegion(modifiedUser.getRegion());
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((apiKey == null) ? 0 : apiKey.hashCode());
        result = prime * result + ((country == null) ? 0 : country.hashCode());
        result = prime * result + ((created == null) ? 0 : created.hashCode());
        result = prime * result
            + ((credential == null) ? 0 : credential.hashCode());
        result = prime * result
            + ((displayName == null) ? 0 : displayName.hashCode());
        result = prime * result + ((email == null) ? 0 : email.hashCode());
        result = prime * result + ((iname == null) ? 0 : iname.hashCode());
        result = prime * result + ((inum == null) ? 0 : inum.hashCode());
        result = prime * result + ((locked == null) ? 0 : locked.hashCode());
        result = prime * result + ((mossoId == null) ? 0 : mossoId.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((nastId == null) ? 0 : nastId.hashCode());
        result = prime * result + ((orgInum == null) ? 0 : orgInum.hashCode());
        result = prime
            * result
            + ((maxLoginFailuresExceded == null) ? 0 : maxLoginFailuresExceded
                .hashCode());
        result = prime * result
            + ((personId == null) ? 0 : personId.hashCode());
        result = prime * result
            + ((preference == null) ? 0 : preference.hashCode());
        result = prime * result + ((region == null) ? 0 : region.hashCode());
        result = prime * result
            + ((softDeleted == null) ? 0 : softDeleted.hashCode());
        result = prime * result + ((status == null) ? 0 : status.toString().hashCode());
        result = prime * result
            + ((uniqueId == null) ? 0 : uniqueId.hashCode());
        result = prime * result + ((updated == null) ? 0 : updated.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        User other = (User) obj;
        if (apiKey == null) {
            if (other.apiKey != null) {
                return false;
            }
        } else if (!apiKey.equals(other.apiKey)) {
            return false;
        }
        if (country == null) {
            if (other.country != null) {
                return false;
            }
        } else if (!country.equals(other.country)) {
            return false;
        }
        if (created == null) {
            if (other.created != null) {
                return false;
            }
        } else if (!created.equals(other.created)) {
            return false;
        }
        if (credential == null) {
            if (other.credential != null) {
                return false;
            }
        } else if (!credential.equals(other.credential)) {
            return false;
        }
        if (displayName == null) {
            if (other.displayName != null) {
                return false;
            }
        } else if (!displayName.equals(other.displayName)) {
            return false;
        }
        if (email == null) {
            if (other.email != null) {
                return false;
            }
        } else if (!email.equals(other.email)) {
            return false;
        }
        if (iname == null) {
            if (other.iname != null) {
                return false;
            }
        } else if (!iname.equals(other.iname)) {
            return false;
        }
        if (inum == null) {
            if (other.inum != null) {
                return false;
            }
        } else if (!inum.equals(other.inum)) {
            return false;
        }
        if (locked == null) {
            if (other.locked != null) {
                return false;
            }
        } else if (!locked.equals(other.locked)) {
            return false;
        }
        if (mossoId == null) {
            if (other.mossoId != null) {
                return false;
            }
        } else if (!mossoId.equals(other.mossoId)) {
            return false;
        }
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        if (nastId == null) {
            if (other.nastId != null) {
                return false;
            }
        } else if (!nastId.equals(other.nastId)) {
            return false;
        }
        if (orgInum == null) {
            if (other.orgInum != null) {
                return false;
            }
        } else if (!orgInum.equals(other.orgInum)) {
            return false;
        }
        if (maxLoginFailuresExceded == null) {
            if (other.maxLoginFailuresExceded != null) {
                return false;
            }
        } else if (!maxLoginFailuresExceded.equals(other.maxLoginFailuresExceded)) {
            return false;
        }
        if (personId == null) {
            if (other.personId != null) {
                return false;
            }
        } else if (!personId.equals(other.personId)) {
            return false;
        }
        if (preference == null) {
            if (other.preference != null) {
                return false;
            }
        } else if (!preference.equals(other.preference)) {
            return false;
        }
        if (region == null) {
            if (other.region != null) {
                return false;
            }
        } else if (!region.equals(other.region)) {
            return false;
        }
        if (softDeleted == null) {
            if (other.softDeleted != null) {
                return false;
            }
        } else if (!softDeleted.equals(other.softDeleted)) {
            return false;
        }
        if (status != other.status) {
            return false;
        }
        if (uniqueId == null) {
            if (other.uniqueId != null) {
                return false;
            }
        } else if (!uniqueId.equals(other.uniqueId)) {
            return false;
        }
        if (updated == null) {
            if (other.updated != null) {
                return false;
            }
        } else if (!updated.equals(other.updated)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "User [email=" + email + ", credential=" + credential
            + ", personId=" + personId + ", uniqueId=" + uniqueId + ", name="
            + name + ", preference=" + preference + ", country=" + country
            + ", displayName=" + displayName + ", inum=" + inum + ", iname="
            + iname + ", locked=" + locked + ", orgInum=" + orgInum
            + ", apiKey=" + apiKey + ", status=" + status + ", softDeleted=" + softDeleted + ", region=" + region
            + ", nastId=" + nastId + ", mossoId=" + mossoId + ", created="
            + created + ", updated=" + updated + ", passwordFailureLocked="
            + maxLoginFailuresExceded + ", username=" + getUsername()
            + ", customerId=" + getCustomerId() + ", groups=" + getGroups() + "]";
    }

    public static class Builder {
        private User user = null;

        public Builder() {
            user = new User();
        }

        public Builder setUsername(String username) {
            user.setUsername(username);
            return this;
        }

        public Builder setUniqueIds(String userName, String inum, String iname,
            String uniqueId) {
            user.setUsername(userName);
            user.inum = inum;
            user.iname = iname;
            user.uniqueId = uniqueId;

            return this;
        }

        public Builder setCisIds(String customerId, String personId) {
            user.setCustomerId(customerId);
            user.personId = personId;

            return this;
        }

        public Builder setNames(String firstName, String middleName,
            String lastName, String displayName) {
            user.setFirstname(firstName);
            user.setMiddlename(middleName);
            user.setLastname(lastName);
            user.displayName = displayName;

            return this;
        }

        public Builder setEmail(String email) {
            user.email = email;

            return this;
        }

        public Builder setLocale(String preferredLanguage, String timeZone,
            String country) {
            user.setPreferredLang(preferredLanguage);
            user.setTimeZone(timeZone);
            user.setCountry(country);

            return this;
        }

        public Builder setOrgInum(String orgInum) {
            user.orgInum = orgInum;

            return this;
        }

        public Builder setApiKey(String apiKey) {
            user.apiKey = apiKey;

            return this;
        }

        public Builder setFlags(UserStatus status, boolean isLocked) {
            user.status = status;
            user.locked = isLocked;

            return this;
        }

        public Builder setSecurityInfo(String password, String secretQuestion,
            String secretAnswer) {
            user.setPassword(password);
            user.setSecretQuestion(secretQuestion);
            user.setSecretAnswer(secretAnswer);

            return this;
        }

        public User build() {
            if (StringUtils.isBlank(user.getCustomerId())
                || StringUtils.isBlank(user.getUsername())
                || StringUtils.isBlank(user.getEmail())) {
                throw new IllegalStateException(
                    "Required paramters have not been satisfied.");
            }

            return user;
        }
    }
    
    @Override
    public String getAuditContext() {
        String format = "username=%s, customer=%s";
        return String.format(format, getUsername(), getCustomerId());
    }
    
}
