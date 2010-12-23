package models;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import play.db.jpa.Model;


public class Person extends Model {

    String personNumber;
    Customer customer;

    String firstname;
    String lastname;
    String middlename;
    String email;
    List<String> additionalEmails;

    List<PhysicalAddress> physicalAddresses;
    List<Phone> phones;
    List<PersonRole> roles;
    
    public Person(Customer customer,
            String firstname, String lastname, String middlename, String email,
            List<PhysicalAddress> physicalAddresses,
            List<Phone> phones,
            List<PersonRole> roles) {

        this.customer = customer;

        this.firstname = firstname;
        this.lastname = lastname;
        this.middlename = middlename;
        this.email = email;

        this.physicalAddresses = physicalAddresses;
        this.phones = phones;
        this.roles = roles;
    }

    public static Person newInstance(String xmlSource) {
        Person person = null;

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document xmlDoc = builder.parse(new InputSource(new StringReader(xmlSource)));


        }
        catch(Exception ex) {
            person = null;
        }

        return person;
    }

    public String getPersonNumber() {
        return personNumber;
    }

    public void setPersonNumber(String personNumber) {
        this.personNumber = personNumber;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public String getMiddlename() {
        return middlename;
    }

    public void setMiddlename(String middlename) {
        this.middlename = middlename;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public List<PhysicalAddress> getPhysicalAddresses() {
        return physicalAddresses;
    }

    public void setPhysicalAddress(List<PhysicalAddress> physicalAddresses) {
        this.physicalAddresses = physicalAddresses;
    }

    public PhysicalAddress getPhysicalAddress(int index) {
        return physicalAddresses.get(index);
    }

    public void addPhysicalAddress(PhysicalAddress physicalAddress) {
        if (physicalAddresses == null) {
            physicalAddresses = new ArrayList<PhysicalAddress>();
        }
        physicalAddresses.add(physicalAddress);
    }

    public void addPhysicalAddress(String street, String city, String state, String zip, String country, boolean isPrimary) {
        if (physicalAddresses == null) {
            physicalAddresses = new ArrayList<PhysicalAddress>();
        }

        PhysicalAddress physicalAddress = new PhysicalAddress(street, city, state,
                zip, country, isPrimary);
        physicalAddresses.add(physicalAddress);
    }

    public void deletePhysicalAddress(int index) {
        physicalAddresses.remove(index);
    }

    public void clearPhysicalAddresses() {
        physicalAddresses.clear();
    }

    public List<Phone> getPhones() {
        return phones;
    }

    public Phone getPhone(int index) {
        return phones.get(index);
    }

    public void addPhone(Phone phone) {
        if (phones == null) {
            phones = new ArrayList<Phone>();
        }
        phones.add(phone);
    }

    public void addPhone(PhoneType type, String number) {
        if (phones == null) {
            phones = new ArrayList<Phone>();
        }
        Phone phone = new Phone(type, number);
        phones.add(phone);
    }

    public void deletePhone(int index) {
        phones.remove(index);
    }

    public void clearPhones() {
        phones.clear();
    }

    public List<String> getAdditionalEmails() {
        return additionalEmails;
    }

    public String getAdditionalEmail(int index) {
        return additionalEmails.get(index);
    }

    public void addAdditionalEmail(String email) {
        if (additionalEmails == null) {
            additionalEmails = new ArrayList<String>();
        }
        additionalEmails.add(email);
    }

    public void deleteAdditionalEmail(int index) {
        additionalEmails.remove(index);
    }

    public void clearAdditionalEmails() {
        additionalEmails.clear();
    }

    public List<PersonRole> getRoles() {
        return roles;
    }

    public void setRoles(List<PersonRole> roles) {
        this.roles = roles;
    }

    public void addRole(PersonRole role) {
       if (roles == null) {
           roles = new ArrayList<PersonRole>();
       }
       roles.add(role);
    }

    public void deleteRole(int index) {
        roles.remove(index);
    }

    public void clearRoles() {
        roles.clear();
    }

    public static Map getNameParts(String fullname) {

        String[] nameParts = fullname.split(" ");
        String fname = nameParts[0];
        String lname = "";
        String mname = "";
        if (nameParts.length > 2) {
            lname = nameParts[nameParts.length - 1];
            for(int i=1; i < nameParts.length-2; i++) {
                mname += nameParts[i];
            }
        }
        else if (nameParts.length == 2) {
            lname = nameParts[1];
            mname = " ";
        }

        Map names = new HashMap();
        names.put("firstname", fname);
        names.put("lastname", lname);
        names.put("middlename", mname);

        return names;
    }

    public String getXml() {

        StringBuilder sb = new StringBuilder();

        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        sb.append("<ns2:person xmlns:ns2=\"http://rackspace.com/foundation/service/customer\">");
        
        sb.append(String.format("<first-name>%s</first-name>", firstname));
        sb.append(String.format("<last-name>%s</last-name>", lastname));
        sb.append(String.format("<middle-name>%s</middle-name>", middlename));

        sb.append("<addresses>");

        if (physicalAddresses != null) {
            for(PhysicalAddress physicalAddress : physicalAddresses) {
                sb.append(String.format("<address is-primary=\"%s\">", physicalAddress.isPrimary));
                sb.append(String.format("<street>%s</street>", physicalAddress.street));
                sb.append(String.format("<city>%s</city>", physicalAddress.city));
                sb.append(String.format("<state>%s</state>", physicalAddress.state));
                sb.append(String.format("<country>%s</country>", physicalAddress.country));
                sb.append(String.format("<zipcode>%s</zipcode>", physicalAddress.zip));
                sb.append("</address>");
            }
        }
        sb.append("</addresses>");

        sb.append("<phones>");
        if (phones != null) {
            for(Phone phone : phones) {
                sb.append("<phone>");
                sb.append(String.format("<number>%s</number>", phone.number));
                sb.append(String.format("<type>%s</type>", phone.type));
                sb.append("</phone>");
            }
        }
        sb.append("</phones>");

        sb.append(String.format("<primary-email-address>%s</primary-email-address>", email));
        sb.append("<additional-email-addresses>");
        if (additionalEmails != null) {
            for(String additionalEmail : additionalEmails) {
                sb.append(String.format("<email_address>%s</email_address>", additionalEmail));
            }
        }
        sb.append("</additional-email-addresses>");

        sb.append("<customer-roles>");
        sb.append("<customer-role>");

        sb.append("<customer-summary>");
        sb.append("<customer-identifier>");
        sb.append(String.format("<number>%s</number>", customer != null ?
            customer.customerNumber : StringUtils.EMPTY));
        sb.append("</customer-identifier>");
        sb.append(String.format("<name>%s</name>", customer != null ? customer.name : StringUtils.EMPTY));
        sb.append(String.format("<customer-type>%s</customer-type>",
                customer != null ? customer.type : CustomerType.ORGANIZATION));
        sb.append("</customer-summary>");

        sb.append("<roles>");
        if (roles != null) {
            for(PersonRole role : roles) {
                sb.append(String.format("<role>%s</role>", role));
            }
        }
        sb.append("</roles>");

        sb.append("</customer-role>");
        sb.append("</customer-roles>");

        sb.append("</ns2:person>");

        return sb.toString();
    }
}