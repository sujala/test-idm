package com.rackspace.idm.domain.entity;


public class UserHumanName {
    private String firstname = null;
    private String middlename = null;
    private String lastname = null;

    public UserHumanName() {
        // Needed by JAX-RS
    }

    public UserHumanName(String firstname, String middlename, String lastname) {
        this.firstname = firstname;
        this.middlename = middlename;
        this.lastname = lastname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setMiddlename(String middlename) {
        this.middlename = middlename;
    }

    public String getMiddlename() {
        return middlename;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public String getLastname() {
        return lastname;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((firstname == null) ? 0 : firstname.hashCode());
        result = prime * result
                + ((lastname == null) ? 0 : lastname.hashCode());
        result = prime * result
                + ((middlename == null) ? 0 : middlename.hashCode());
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
        UserHumanName other = (UserHumanName) obj;
        if (firstname == null) {
            if (other.firstname != null) {
                return false;
            }
        } else if (!firstname.equals(other.firstname)) {
            return false;
        }
        if (lastname == null) {
            if (other.lastname != null) {
                return false;
            }
        } else if (!lastname.equals(other.lastname)) {
            return false;
        }
        if (middlename == null) {
            if (other.middlename != null) {
                return false;
            }
        } else if (!middlename.equals(other.middlename)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "UserHumanName [firstname=" + firstname + ", middlename="
                + middlename + ", lastname=" + lastname + "]";
    }
}
