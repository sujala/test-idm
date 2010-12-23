package models;

public class PhysicalAddress {
    String street;
    String city;
    String state;
    String zip;
    String country;
    boolean isPrimary;

    public PhysicalAddress(String street, String city, String state,
            String zip, String country, boolean isPrimary) {

        this.street = street;
        this.city = city;
        this.state = state;
        this.zip = zip;
        this.country = country;
        this.isPrimary = isPrimary;
    }

    public void setStreet(String street) {
        this.street = street;
    }
    public String getStreet() {
        return street;
    }

    public void setCity(String city) {
        this.city = city;
    }
    public String getCity() {
        return city;
    }

    public void setState(String state) {
        this.state = state;
    }
    public String getState() {
        return state;
    }

    public void setZip(String zip) {
        this.zip = zip;
    }
    public String getZip() {
        return zip;
    }

    public void setCountry(String country) {
        this.country = country;
    }
    public String getCountry() {
        return country;
    }

    public void setIsPrimary(boolean isPrimary) {
        this.isPrimary = isPrimary;
    }
    public boolean getIsPrimary() {
        return isPrimary;
    }
}
