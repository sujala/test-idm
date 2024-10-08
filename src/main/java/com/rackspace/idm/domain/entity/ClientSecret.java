package com.rackspace.idm.domain.entity;


public final class ClientSecret {

    private String value = null;
    private boolean isNew = false;

    private ClientSecret() {
        // Needed by JAX-RS
    }

    private ClientSecret(String value, boolean isNew) {
        this.value = value;
        this.isNew = isNew;
    }

    public boolean isNew() {
        return isNew;
    }

    public String getValue() {
        return value;
    }

    public static ClientSecret existingInstance(String existingSecret) {
        return new ClientSecret(existingSecret, false);
    }

    public static ClientSecret newInstance(String newSecret) {
        return new ClientSecret(newSecret, true);
    }
    
    public ClientSecret toExisting() {
        return new ClientSecret(value, false);
    }

    @Override
    public String toString() {
        return "ClientSecret [******]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        final int newPrime = 1231;
        final int oldPrime = 1237;
        int result = 1;
        result = prime * result + (isNew ? newPrime : oldPrime);
        result = prime * result + ((value == null) ? 0 : value.hashCode());
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
        ClientSecret other = (ClientSecret) obj;
        // If the secret has been saved, it has been hashed. Thus the values
        // between pre-save and post-save secrets cannot be compared.
        if (isNew == other.isNew) {
            if (value == null) {
                if (other.value != null) {
                    return false;
                }
            } else if (!value.equals(other.value)) {
                return false;
            }
        }
        return true;
    }
}
