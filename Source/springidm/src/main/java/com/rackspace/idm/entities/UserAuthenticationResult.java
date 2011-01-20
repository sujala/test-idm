package com.rackspace.idm.entities;

public class UserAuthenticationResult extends AuthenticationResult {

    private BaseUser user;

    public UserAuthenticationResult(BaseUser user, boolean authenticated) {
        super(authenticated);
        this.user = user;
    }
    public BaseUser getUser() {
        return user;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((user == null) ? 0 : user.hashCode());
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
        UserAuthenticationResult other = (UserAuthenticationResult) obj;
        if (user == null) {
            if (other.user != null) {
                return false;
            }
        } else if (!user.equals(other.user)) {
            return false;
        }
        return true;
    }


}
