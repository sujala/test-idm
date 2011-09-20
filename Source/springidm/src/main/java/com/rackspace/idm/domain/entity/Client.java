package com.rackspace.idm.domain.entity;

import com.rackspace.idm.validation.MessageTexts;
import com.rackspace.idm.validation.RegexPatterns;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

public class Client implements Auditable {
    private static final long serialVersionUID = -3160754818606772239L;

    private ClientSecret clientSecret;
    
    private String uniqueId = null;
    
    private String clientId = null;
    @NotNull
    @Pattern(regexp = RegexPatterns.NOT_EMPTY, message = MessageTexts.NOT_EMPTY)
    private String rcn = null;

    @NotNull
    @Pattern(regexp = RegexPatterns.NOT_EMPTY, message = MessageTexts.NOT_EMPTY)
    private String name = null;

    @NotNull
    private ClientStatus status = null;
    
    private String id = null;

    private String orgInum = null;
    private Boolean locked = null;
    private Boolean softDeleted = null;
    
    private String scope = null;
    private String callBackUrl = null;
    private String title = null;
    private String description = null;

    public Client() {
    }

    public Client(String clientId, ClientSecret clientSecret, String name,
        String rcn, ClientStatus status) {
        this.clientId = clientId;
        this.rcn = rcn;
        this.clientSecret = clientSecret;
        this.name = name;
        this.status = status;
    }

    public void setUniqueId(String uniqueId) {
        if (uniqueId != null) {
            this.uniqueId = uniqueId;
        }
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public String getClientId() {
        return clientId;
    }
    
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
    
    public String getRCN() {
        return rcn;
    }
    
    public void setRCN(String rcn) {
        this.rcn = rcn;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setClientSecretObj(ClientSecret clientSecret) {
        if (clientSecret != null) {
            this.clientSecret = clientSecret;
        }
    }

    public ClientSecret getClientSecretObj() {
        return clientSecret;
    }

    public void setClientSecret(String secret) {
        if (secret != null) {
            this.clientSecret = ClientSecret.newInstance(secret);
        }
    }

    public String getClientSecret() {
        return this.clientSecret.getValue();
    }

    public void setName(String name) {
        if (name != null) {
            this.name = name;
        }
    }

    public String getOrgInum() {
        return orgInum;
    }

    public void setOrgInum(String orgInum) {
        if (orgInum != null) {
            this.orgInum = orgInum;
        }
    }

    public String getName() {
        return name;
    }

    public ClientStatus getStatus() {
        return status;
    }

    public void setStatus(ClientStatus status) {
        if (status != null) {
            this.status = status;
        }
    }

    public Boolean isSoftDeleted() {
        return softDeleted;
    }

    public void setSoftDeleted(Boolean softDeleted) {
        this.softDeleted = softDeleted;
    }

    public Boolean isLocked() {
        return locked;
    }

    public void setLocked(Boolean isLocked) {
        this.locked = isLocked;
    }
    
    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getCallBackUrl() {
        return callBackUrl;
    }

    public void setCallBackUrl(String callBackUrl) {
        this.callBackUrl = callBackUrl;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
    
    public void setDefaults() {
        this.setLocked(false);
        this.setSoftDeleted(false);
        this.setStatus(ClientStatus.ACTIVE);
    }
    
    public boolean isDisabled() {
        boolean disabled = false;
        disabled = this.isLocked() == null ? disabled : disabled || this.isLocked().booleanValue();
        disabled = this.isSoftDeleted() == null ? disabled : disabled || this.isSoftDeleted().booleanValue();
        disabled = this.getStatus() == null ? disabled : disabled || this.getStatus().equals(ClientStatus.INACTIVE);
        return disabled;
    }
    
    public void copyChanges(Client modifiedClient) {

        if (modifiedClient.getCallBackUrl() != null) {
            setCallBackUrl(modifiedClient.getCallBackUrl());
        }

        if (modifiedClient.getDescription() != null) {
            setDescription(modifiedClient.getDescription());
        }
        
        if (modifiedClient.getScope() != null) {
            setScope(modifiedClient.getScope());
        }

        if (modifiedClient.getTitle() != null) {
            setTitle(modifiedClient.getTitle());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Client client = (Client) o;

        if (callBackUrl != null ? !callBackUrl.equals(client.callBackUrl) : client.callBackUrl != null) return false;
        if (clientId != null ? !clientId.equals(client.clientId) : client.clientId != null) return false;
        if (clientSecret != null ? !clientSecret.equals(client.clientSecret) : client.clientSecret != null)
            return false;
        if (description != null ? !description.equals(client.description) : client.description != null) return false;
        if (id != null ? !id.equals(client.id) : client.id != null) return false;
        if (locked != null ? !locked.equals(client.locked) : client.locked != null) return false;
        if (name != null ? !name.equals(client.name) : client.name != null) return false;
        if (orgInum != null ? !orgInum.equals(client.orgInum) : client.orgInum != null) return false;
        if (rcn != null ? !rcn.equals(client.rcn) : client.rcn != null) return false;
        if (scope != null ? !scope.equals(client.scope) : client.scope != null) return false;
        if (softDeleted != null ? !softDeleted.equals(client.softDeleted) : client.softDeleted != null) return false;
        if (status != client.status) return false;
        if (title != null ? !title.equals(client.title) : client.title != null) return false;
        if (uniqueId != null ? !uniqueId.equals(client.uniqueId) : client.uniqueId != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = clientSecret != null ? clientSecret.hashCode() : 0;
        result = 31 * result + (uniqueId != null ? uniqueId.hashCode() : 0);
        result = 31 * result + (clientId != null ? clientId.hashCode() : 0);
        result = 31 * result + (rcn != null ? rcn.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (status != null ? status.hashCode() : 0);
        result = 31 * result + (id != null ? id.hashCode() : 0);
        result = 31 * result + (orgInum != null ? orgInum.hashCode() : 0);
        result = 31 * result + (locked != null ? locked.hashCode() : 0);
        result = 31 * result + (softDeleted != null ? softDeleted.hashCode() : 0);
        result = 31 * result + (scope != null ? scope.hashCode() : 0);
        result = 31 * result + (callBackUrl != null ? callBackUrl.hashCode() : 0);
        result = 31 * result + (title != null ? title.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return getAuditContext();
    }

    @Override
    public String getAuditContext() {
        String format = "ID=%s,clientId=%s,customerId=%s";
        return String.format(format, getId(), getClientId(), getRCN());
    }
}
