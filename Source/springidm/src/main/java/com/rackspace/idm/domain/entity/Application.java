package com.rackspace.idm.domain.entity;

import java.util.List;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import com.rackspace.idm.validation.MessageTexts;
import com.rackspace.idm.validation.RegexPatterns;

public class Application implements Auditable {
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
    
    private String openStackType = null;
    
    private String scope = null;
    private String callBackUrl = null;
    private String title = null;
    private String description = null;
    private Boolean enabled = null;

    private Boolean useForDefaultRegion = null;

    private List<TenantRole> roles = null;
    public Application() {
    }

    public Application(String clientId, ClientSecret clientSecret, String name, String rcn) {
        this.clientId = clientId;
        this.rcn = rcn;
        this.clientSecret = clientSecret;
        this.name = name;
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

    public String getOpenStackType() {
        return openStackType;
    }

    public void setOpenStackType(String openStackType) {
        this.openStackType = openStackType;
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

    public String getName() {
        return name;
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
        this.setEnabled(true);
    }

    public void copyChanges(Application modifiedClient) {

    	if (modifiedClient.getRCN() != null) {
    		setRCN(modifiedClient.getRCN());
    	}

    	if (modifiedClient.isEnabled() != null) {
    		setEnabled(modifiedClient.isEnabled());
    	}

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
        int result = 1;
        result = prime * result
            + ((callBackUrl == null) ? 0 : callBackUrl.hashCode());
        result = prime * result
            + ((clientId == null) ? 0 : clientId.hashCode());
        result = prime * result
            + ((clientSecret == null) ? 0 : clientSecret.hashCode());
        result = prime * result
            + ((description == null) ? 0 : description.hashCode());
        result = prime * result + ((enabled == null) ? 0 : enabled.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result
            + ((openStackType == null) ? 0 : openStackType.hashCode());
        result = prime * result + ((rcn == null) ? 0 : rcn.hashCode());
        result = prime * result + ((roles == null) ? 0 : roles.hashCode());
        result = prime * result + ((scope == null) ? 0 : scope.hashCode());
        result = prime * result + ((title == null) ? 0 : title.hashCode());
        result = prime * result
            + ((uniqueId == null) ? 0 : uniqueId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Application other = (Application) obj;
        if (callBackUrl == null) {
            if (other.callBackUrl != null) {
                return false;
            }
        } else if (!callBackUrl.equals(other.callBackUrl)) {
            return false;
        }
        if (clientId == null) {
            if (other.clientId != null) {
                return false;
            }
        } else if (!clientId.equals(other.clientId)) {
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
        if (enabled == null) {
            if (other.enabled != null) {
                return false;
            }
        } else if (!enabled.equals(other.enabled)) {
            return false;
        }
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        if (openStackType == null) {
            if (other.openStackType != null) {
                return false;
            }
        } else if (!openStackType.equals(other.openStackType)) {
            return false;
        }
        if (rcn == null) {
            if (other.rcn != null) {
                return false;
            }
        } else if (!rcn.equals(other.rcn)) {
            return false;
        }
        if (roles == null) {
            if (other.roles != null) {
                return false;
            }
        } else if (!roles.equals(other.roles)) {
            return false;
        }
        if (scope == null) {
            if (other.scope != null) {
                return false;
            }
        } else if (!scope.equals(other.scope)) {
            return false;
        }
        if (title == null) {
            if (other.title != null) {
                return false;
            }
        } else if (!title.equals(other.title)) {
            return false;
        }
        if (uniqueId == null) {
            if (other.uniqueId != null) {
                return false;
            }
        } else if (!uniqueId.equals(other.uniqueId)) {
            return false;
        }
        return true;
    }

    public List<TenantRole> getRoles() {
		return roles;
	}

	public void setRoles(List<TenantRole> roles) {
		this.roles = roles;
	}

	@Override
    public String toString() {
        return getAuditContext();
    }

    @Override
    public String getAuditContext() {
        String format = "clientId=%s,customerId=%s";
        return String.format(format, getClientId(), getRCN());
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean isEnabled() {
        return enabled;
    }

    public Boolean getUseForDefaultRegion() {
        return useForDefaultRegion;
    }

    public void setUseForDefaultRegion(Boolean useForDefaultRegion) {
        this.useForDefaultRegion = useForDefaultRegion;
    }
}
