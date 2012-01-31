package com.rackspace.idm.domain.entity;

import com.rackspace.idm.GlobalConstants;
import com.rackspace.idm.validation.MessageTexts;
import com.rackspace.idm.validation.RegexPatterns;
import org.apache.commons.lang.StringUtils;
import org.hibernate.validator.constraints.Length;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.util.List;
import java.util.Locale;

public class User implements Auditable {
    private static final long serialVersionUID = 1347677880811855274L;
    
    private String uniqueId = null;
    
    private String id = null;

    @NotNull
    @Length(min = 1, max = 32)
    @Pattern(regexp = RegexPatterns.USERNAME, message = MessageTexts.USERNAME)
    private String username = null;

    private String customerId = null;

    private String email = null;

    private UserCredential credential = new UserCredential();
    private String personId = null;

    private UserHumanName name = new UserHumanName();
    private UserLocale preference = new UserLocale();
    private String country = null;
    private String displayName = null;
    private String apiKey = null;
    private String region = null;
    
    private String nastId = null;
    private Integer mossoId = null;
    
    private DateTime created;
    private DateTime updated;
    
    private DateTime softDeletedTimestamp;
    
    private Boolean maxLoginFailuresExceded = null;
    
    private String secureId = null;
    
    private Boolean enabled = null;

    private List<TenantRole> roles = null;

    private String domainId = null;

    public User() {
        // Needed by JAX-RS
    }

    public User(String username) {
        this.username = username;
    }

    @Deprecated
    public User(String username, String customerId, String email,
        UserHumanName name, UserLocale pref, UserCredential cred) {
        this.username = username;
        this.customerId = customerId;
        this.email = email;
        this.name = name;
        this.preference = pref;
        this.credential = cred;
    }

    @Deprecated
    public User(String username, String customerId, String email,
        UserHumanName name, UserLocale preference, UserCredential credential,
        String country, String displayName, String inum, String iname,
        String orgInum, String apiKey,
        String personId) {
        this.username = username;
        this.customerId = customerId;
        this.email = email;
        this.name = name;
        this.preference = preference;
        this.credential = credential;
        this.country = country;
        this.displayName = displayName;
        this.apiKey = apiKey;
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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getSecureId() {
        return secureId;
    }

    public void setSecureId(String secureId) {
        this.secureId = secureId;
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

    public Boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean isMaxLoginFailuresExceded() {
        return maxLoginFailuresExceded;
    }

    public void setMaxLoginFailuresExceded(Boolean maxLoginFailuresExceded) {
        this.maxLoginFailuresExceded = maxLoginFailuresExceded;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        if (apiKey != null) {
            this.apiKey = apiKey;
        }
    }

    public void setPasswordObj(Password password) {
        if (password != null) {
            this.credential.setPassword(password);
        }
    }

    public boolean hasEmptyPassword() {
     	return getPasswordObj() == null || StringUtils.isBlank(getPasswordObj().getValue());
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
        if (preference.getTimeZone() == null) {
            return null;
        }
        return preference.getTimeZone().toString();
    }

    public void setTimeZone(String timeZone) {
        if (StringUtils.isBlank(timeZone)) {
            return;
        }
        preference.setTimeZone(DateTimeZone.forID(timeZone));
    }

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
    	return this.enabled == null ? true : !this.enabled;
    }

    public void setDefaults() {
        if (this.preference.getLocale() == null) {
            this.setPreferredLang(GlobalConstants.USER_PREFERRED_LANG_DEFAULT);
        }

        if (this.getTimeZoneObj() == null) {
            this.setTimeZone(GlobalConstants.USER_TIME_ZONE_DEFAULT);
        }

        this.setEnabled(true);
    }

    public void copyChanges(User modifiedUser) {

    	if (modifiedUser.getCustomerId() != null) {
    		setCustomerId(modifiedUser.getCustomerId());
    	}

    	if (modifiedUser.isEnabled() != null) {
    		setEnabled(modifiedUser.isEnabled());
    	}

        if (modifiedUser.getPersonId() != null) {
            setPersonId(modifiedUser.getPersonId());
        }

        if (modifiedUser.getFirstname() != null) {
            setFirstname(modifiedUser.getFirstname());
        }

        if (modifiedUser.getMiddlename() != null) {
            setMiddlename(modifiedUser.getMiddlename());
        }

        if (modifiedUser.getLastname() != null) {
            setLastname(modifiedUser.getLastname());
        }

        if (modifiedUser.getDisplayName() != null) {
            setDisplayName(modifiedUser.getDisplayName());
        }

        if (modifiedUser.getEmail() != null) {
            setEmail(modifiedUser.getEmail());
        }

        if (modifiedUser.getPreferredLang() != null) {
            setPreferredLang(modifiedUser.getPreferredLang());
        }

        if (modifiedUser.getTimeZone() != null) {
            setTimeZone(modifiedUser.getTimeZone());
        }

        if (modifiedUser.getCountry() != null) {
            setCountry(modifiedUser.getCountry());
        }

        if (modifiedUser.getRegion() != null) {
            setRegion(modifiedUser.getRegion());
        }
        if(modifiedUser.getPassword() != null){
            setPassword(modifiedUser.getPassword());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) {return false;}

        User user = (User) o;

        if (apiKey != null ? !apiKey.equals(user.apiKey) : user.apiKey != null) {return false;}
        if (country != null ? !country.equals(user.country) : user.country != null) {return false;}
        if (created != null ? !created.equals(user.created) : user.created != null) {return false;}
        if (credential != null ? !credential.equals(user.credential) : user.credential != null) {return false;}
        if (customerId != null ? !customerId.equals(user.customerId) : user.customerId != null) {return false;}
        if (displayName != null ? !displayName.equals(user.displayName) : user.displayName != null) {return false;}
        if (email != null ? !email.equals(user.email) : user.email != null) {return false;}
        if (id != null ? !id.equals(user.id) : user.id != null) {return false;}
        if (maxLoginFailuresExceded != null ? !maxLoginFailuresExceded.equals(user.maxLoginFailuresExceded) : user.maxLoginFailuresExceded != null){
            return false;
        }
        if (mossoId != null ? !mossoId.equals(user.mossoId) : user.mossoId != null) {return false;}
        if (name != null ? !name.equals(user.name) : user.name != null) {return false;}
        if (nastId != null ? !nastId.equals(user.nastId) : user.nastId != null) {return false;}
        if (personId != null ? !personId.equals(user.personId) : user.personId != null) {return false;}
        if (preference != null ? !preference.equals(user.preference) : user.preference != null) {return false;}
        if (region != null ? !region.equals(user.region) : user.region != null) {return false;}
        if (secureId != null ? !secureId.equals(user.secureId) : user.secureId != null) {return false;}
        if (softDeletedTimestamp != null ? !softDeletedTimestamp.equals(user.softDeletedTimestamp) : user.softDeletedTimestamp != null){
            return false;
        }
        if (uniqueId != null ? !uniqueId.equals(user.uniqueId) : user.uniqueId != null) {return false;}
        if (updated != null ? !updated.equals(user.updated) : user.updated != null) {return false;}
        if (username != null ? !username.equals(user.username) : user.username != null) {return false;}

        return true;
    }

    @Override
    public int hashCode() {
        int result = uniqueId != null ? uniqueId.hashCode() : 0;
        result = 31 * result + (id != null ? id.hashCode() : 0);
        result = 31 * result + (username != null ? username.hashCode() : 0);
        result = 31 * result + (customerId != null ? customerId.hashCode() : 0);
        result = 31 * result + (email != null ? email.hashCode() : 0);
        result = 31 * result + (credential != null ? credential.hashCode() : 0);
        result = 31 * result + (personId != null ? personId.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (preference != null ? preference.hashCode() : 0);
        result = 31 * result + (country != null ? country.hashCode() : 0);
        result = 31 * result + (displayName != null ? displayName.hashCode() : 0);
        result = 31 * result + (apiKey != null ? apiKey.hashCode() : 0);
        result = 31 * result + (region != null ? region.hashCode() : 0);
        result = 31 * result + (nastId != null ? nastId.hashCode() : 0);
        result = 31 * result + (mossoId != null ? mossoId.hashCode() : 0);
        result = 31 * result + (created != null ? created.hashCode() : 0);
        result = 31 * result + (updated != null ? updated.hashCode() : 0);
        result = 31 * result + (softDeletedTimestamp != null ? softDeletedTimestamp.hashCode() : 0);
        result = 31 * result + (maxLoginFailuresExceded != null ? maxLoginFailuresExceded.hashCode() : 0);
        result = 31 * result + (secureId != null ? secureId.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return getAuditContext();
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
            user.setUniqueId(uniqueId);

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

        public Builder setApiKey(String apiKey) {
            user.apiKey = apiKey;

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

	public List<TenantRole> getRoles() {
		return roles;
	}

	public void setRoles(List<TenantRole> roles) {
		this.roles = roles;
	}

    public String getDomainId() {
        return domainId;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }
}
