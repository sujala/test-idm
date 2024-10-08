package com.rackspace.idm.domain.entity;

public class ClientAuthenticationResult extends AuthenticationResult {

    private Application client;
    
    public ClientAuthenticationResult(Application client, boolean authenticated) {
        super(authenticated);
        this.client = client;
    }

    public Application getClient() {
        return client;
    }

    public void setClient(Application client) {
        this.client = client;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((client == null) ? 0 : client.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }
        ClientAuthenticationResult other = (ClientAuthenticationResult) obj;
        if (client == null) {
            if (other.client != null) {
                return false;
            }
        } else if (!client.equals(other.client)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "ClientAuthenticationResult [client=" + client
            + ", authenticated=" + isAuthenticated() + "]";
    }
}
