package com.rackspace.idm.entities;

import com.rackspace.idm.validation.MessageTexts;
import com.rackspace.idm.validation.RegexPatterns;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.util.List;

public class Client extends BaseClient {
    private static final long serialVersionUID = -3160754818606772239L;

    private ClientSecret clientSecret;

    @NotNull
    @Pattern(regexp = RegexPatterns.NOT_EMPTY, message = MessageTexts.NOT_EMPTY)
    private String name = null;

    @NotNull
    private ClientStatus status = null;

    private String uniqueId = null;
    private String inum = null;
    private String iname = null;
    private Boolean isLocked = null;
    private String seeAlso = null;
    private Boolean softDeleted = null;
    private String owner = null;

    public Client() {
    }

    public Client(String clientId, ClientSecret clientSecret, String name,
                  String inum, String iname, String customerId, ClientStatus status,
                  String seeAlso, String owner) {
        super(clientId, customerId);
        this.clientSecret = clientSecret;
        this.name = name;
        this.inum = inum;
        this.iname = iname;
        this.status = status;
        this.seeAlso = seeAlso;
        this.owner = owner;
    }

    public void setUniqueId(String uniqueId) {
        if (uniqueId != null) {
            this.uniqueId = uniqueId;
        }
    }

    public String getUniqueId() {
        return uniqueId;
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

    public String getIname() {
        return iname;
    }

    public void setIname(String iname) {
        if (iname != null) {
            this.iname = iname;
        }
    }

    public ClientStatus getStatus() {
        return status;
    }

    public void setStatus(ClientStatus status) {
        if (status != null) {
            this.status = status;
        }
    }

    public String getSeeAlso() {
        return seeAlso;
    }

    public void setSeeAlso(String seeAlso) {
        if (seeAlso != null) {
            this.seeAlso = seeAlso;
        }
    }

    public Boolean getSoftDeleted() {
        return softDeleted;
    }

    public void setSoftDeleted(Boolean softDeleted) {
        this.softDeleted = softDeleted;
    }

    public Boolean getIsLocked() {
        return isLocked;
    }

    public void setIsLocked(Boolean isLocked) {
        this.isLocked = isLocked;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        if (owner != null) {
            this.owner = owner;
        }
    }

    public void setInum(String inum) {
        if (inum != null) {
            this.inum = inum;
        }
    }

    public String getInum() {
        return inum;
    }

    // The following overrides allow for a more permissive mutators in the child (Client)
    // while maintaining a more strict, immutable-ish characteristics in the parent (BaseClient).

    @Override
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    @Override
    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    @Override
    public void setPermissions(List<Permission> permissions) {
        // Make the setter more permissive here, but keep it restrictive in BaseClient
        this.permissions = permissions;
    }

    public void setDefaults() {
        this.setIsLocked(false);
        this.setSoftDeleted(false);
        this.setStatus(ClientStatus.ACTIVE);
    }

    public BaseClient getBaseClient() {
        BaseClient baseClient = getBaseClientWithoutClientPerms();
        baseClient.setPermissions(this.permissions);
        return baseClient;
    }

    public BaseClient getBaseClientWithoutClientPerms() {
        return new BaseClient(clientId, customerId);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result
                + ((clientSecret == null) ? 0 : clientSecret.hashCode());
        result = prime * result + ((iname == null) ? 0 : iname.hashCode());
        result = prime * result + ((inum == null) ? 0 : inum.hashCode());
        result = prime * result
                + ((isLocked == null) ? 0 : isLocked.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((owner == null) ? 0 : owner.hashCode());
        result = prime * result + ((seeAlso == null) ? 0 : seeAlso.hashCode());
        result = prime * result
                + ((softDeleted == null) ? 0 : softDeleted.hashCode());
        result = prime * result + ((status == null) ? 0 : status.hashCode());
        result = prime * result
                + ((uniqueId == null) ? 0 : uniqueId.hashCode());
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
        if (clientSecret == null) {
            if (other.clientSecret != null) {
                return false;
            }
        } else if (!clientSecret.equals(other.clientSecret)) {
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
        if (isLocked == null) {
            if (other.isLocked != null) {
                return false;
            }
        } else if (!isLocked.equals(other.isLocked)) {
            return false;
        }
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        if (owner == null) {
            if (other.owner != null) {
                return false;
            }
        } else if (!owner.equals(other.owner)) {
            return false;
        }
        if (seeAlso == null) {
            if (other.seeAlso != null) {
                return false;
            }
        } else if (!seeAlso.equals(other.seeAlso)) {
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
        return true;
    }

    @Override
    public String toString() {
        return "Client [clientSecret=" + clientSecret + ", name=" + name
                + ", status=" + status + ", uniqueId=" + uniqueId + ", inum="
                + inum + ", iname=" + iname + ", isLocked=" + isLocked
                + ", seeAlso=" + seeAlso + ", softDeleted=" + softDeleted
                + ", owner=" + owner + "]";
    }
}
