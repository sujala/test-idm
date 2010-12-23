package controllers;

import java.util.Map;
import javax.inject.Inject;
import models.Customer;
import models.CustomerType;
import models.Person;
import models.PersonRole;
import models.PhoneType;
import models.User;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.lang.StringUtils;
import play.mvc.*;
import services.CustomerDbService;
import services.PersonService;
import services.UserService;

public class Signup extends Controller {

    @Inject static CustomerDbService customerDbService;
    @Inject static UserService userService;
    @Inject static PersonService personService;

    public static void index() {
        stepConfigure();
    }

    public static void stepConfigure() {
        render();
    }

    public static void stepConfigureSubmitted(String selectedServices) {
        stepCreateAccount(selectedServices);
    }

    public static void stepCreateAccount(String selectedServices) {
        render(selectedServices);
    }

    public static void stepCreateAccountSubmitted(
            String fullname, String username, String password, String email,
            String oname, String street, String city, String state, String zip, String country,
            String phone)
        throws HttpException {

        String secretQuestion = " ";
        String secretAnswer = " ";
        String preferredLang = "en-us";
        String timezone = "-06:00";
        country = "usa";

        username = username.toLowerCase();
        Map nameParts = User.getNameParts(fullname);
        String firstname = (String)nameParts.get("firstname");
        String lastname = (String)nameParts.get("lastname");
        String middlename = (String)nameParts.get("middlename");

        // add customer
        Customer customer = new Customer(oname, CustomerType.ORGANIZATION);
        Customer addedCust = customerDbService.addCustomer(customer);

        if (StringUtils.isEmpty(addedCust.getCustomerNumber())) {
            error(500, "Error encountered creating the customer in the CiS system.");
        }

        // add person
        Person person = new Person(addedCust,
            firstname, lastname, middlename, email,
            null, null, null);

        person.addPhysicalAddress(street, city, state, zip, country, true);
        person.addPhone(PhoneType.BUSINESS, phone);
        person.addRole(PersonRole.PRIMARY);

        Person addedPerson = personService.addPerson(person);
        
        if (StringUtils.isEmpty(addedPerson.getPersonNumber())) {
            error("Error encountered adding person to the CiS system.");
        }

        String personNumber = addedPerson.getPersonNumber();
        String customerNumber = addedCust.getCustomerNumber();

        // add user
        User user = new User(personNumber, customerNumber,
            username, password,
            firstname, lastname, middlename,
            email, secretQuestion, secretAnswer,
            preferredLang, timezone);

        boolean useradded = false;
        try {
            useradded = userService.addUser(user);
        }
        catch (Exception ex) {
            useradded = false;
        }

        if (!useradded) {
            error(500, "Error encountered adding user");
        }

        signupCompleted();
    }

    public static void signupCompleted()
    {
        render();
    }
}