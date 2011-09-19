package com.rackspace.idm.domain.entity;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import com.rackspace.idm.validation.MessageTexts;
import com.rackspace.idm.validation.RegexPatterns;

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
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result
            + ((callBackUrl == null) ? 0 : callBackUrl.hashCode());
        result = prime * result
            + ((clientSecret == null) ? 0 : clientSecret.hashCode());
        result = prime * result
            + ((description == null) ? 0 : description.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((locked == null) ? 0 : locked.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((orgInum == null) ? 0 : orgInum.hashCode());
        result = prime * result + ((scope == null) ? 0 : scope.hashCode());
        result = prime * result
            + ((softDeleted == null) ? 0 : softDeleted.hashCode());
        result = prime * result + ((status == null) ? 0 : status.hashCode());
        result = prime * result + ((title == null) ? 0 : title.hashCode());
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
        Client other = (Client) obj;
        if (callBackUrl == null) {
            if (other.callBackUrl != null) {
                return false;
            }
        } else if (!callBackUrl.equals(other.callBackUrl)) {
            return false;
        }
        if (clientSecret == null) {
            if (other.clientSecret != null) {
                return false;
            }
        } else if (!clientSecret.equals(other.clientSecret)) {
            return false;
        }
        if (description == null) {
            if (other.description != null) {
                return false;
            }
        } else if (!description.equals(other.description)) {
            return false;
        }
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        if (locked == null) {
            if (other.locked != null) {
                return false;
            }
        } else if (!locked.equals(other.locked)) {
            return false;
        }
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        if (orgInum == null) {
            if (other.orgInum != null) {
                return false;
            }
        } else if (!orgInum.equals(other.orgInum)) {
            return false;
        }
        if (scope == null) {
            if (other.scope != null) {
                return false;
            }
        } else if (!scope.equals(other.scope)) {
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
        if (title == null) {
            if (other.title != null) {
                return false;
            }
        } else if (!title.equals(other.title)) {
            return false;
        }
        return true;
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
