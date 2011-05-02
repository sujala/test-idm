package com.rackspace.idm.domain.entity;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import com.rackspace.idm.validation.MessageTexts;
import com.rackspace.idm.validation.RegexPatterns;

public class Client extends BaseClient implements Auditable {
    private static final long serialVersionUID = -3160754818606772239L;

    private ClientSecret clientSecret;

    @NotNull
    @Pattern(regexp = RegexPatterns.NOT_EMPTY, message = MessageTexts.NOT_EMPTY)
    private String name = null;

    @NotNull
    private ClientStatus status = null;

    private String inum = null;
    private String iname = null;
    private String orgInum = null;
    private Boolean locked = null;
    private Boolean softDeleted = null;

    public Client() {
    }

    public Client(String clientId, ClientSecret clientSecret, String name,
        String inum, String iname, String customerId, ClientStatus status) {
        super(clientId, customerId);
        this.clientSecret = clientSecret;
        this.name = name;
        this.inum = inum;
        this.iname = iname;
        this.status = status;
    }

    @Override
    public void setUniqueId(String uniqueId) {
        if (uniqueId != null) {
            super.setUniqueId(uniqueId);
        }
    }

    @Override
    public String getUniqueId() {
        return super.getUniqueId();
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

    public void setInum(String inum) {
        if (inum != null) {
            this.inum = inum;
        }
    }

    public String getInum() {
        return inum;
    }

    // The following overrides allow for a more permissive mutators in the child
    // (Client)
    // while maintaining a more strict, immutable-ish characteristics in the
    // parent (BaseClient).

    @Override
    public void setClientId(String clientId) {
        super.setClientId(clientId);
    }

    @Override
    public void setCustomerId(String customerId) {
        super.setCustomerId(customerId);
    }
    
    public void setDefaults() {
        this.setLocked(false);
        this.setSoftDeleted(false);
        this.setStatus(ClientStatus.ACTIVE);
    }

    public BaseClient getBaseClient() {
        BaseClient baseClient = getBaseClientWithoutClientPerms();
        return baseClient;
    }

    public BaseClient getBaseClientWithoutClientPerms() {
        return new BaseClient(getClientId(),getCustomerId());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result
            + ((clientSecret == null) ? 0 : clientSecret.hashCode());
        result = prime * result + ((iname == null) ? 0 : iname.hashCode());
        result = prime * result + ((inum == null) ? 0 : inum.hashCode());
        result = prime * result + ((locked == null) ? 0 : locked.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((orgInum == null) ? 0 : orgInum.hashCode());
        result = prime * result
            + ((softDeleted == null) ? 0 : softDeleted.hashCode());
        result = prime * result + ((status == null) ? 0 : status.hashCode());
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
        return true;
    }

    @Override
    public String toString() {
        return "Client [clientSecret=" + clientSecret + ", name=" + name
            + ", status=" + status + ", inum=" + inum + ", iname=" + iname
            + ", orgInum=" + orgInum + ", locked=" + locked + ", softDeleted="
            + softDeleted + "]";
    }

    @Override
    public String getAuditContext() {
        String format = "clientId=%s,customerId=%s";
        return String.format(format, getClientId(), getCustomerId());
    }
}
