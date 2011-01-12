package com.rackspace.idm.entities;

public class ClientAuthenticationResult extends AuthenticationResult {

    private Client client;

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
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
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
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
            + ", authenticated=" + authenticated + "]";
    }
}
