package controllers;

import javax.inject.Inject;
import models.Customer;
import models.CustomerType;
import models.Person;
import models.PersonRole;
import models.PhoneType;
import models.User;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpException;
import play.mvc.*;
import services.CustomerDbService;
import services.PersonService;
import services.UserService;

public class CloudSignup extends Controller {

    @Inject static CustomerDbService customerDbService;
    @Inject static UserService userService;
    @Inject static PersonService personService;

    public static void index() {
        stepConfigure("", "", "", "");
    }

    public static void stepConfigure(String errorMsg, String username, String useremail, String password) {
        render(errorMsg, username, useremail, password);
    }

    public static void stepConfigureSubmitted(String selectedServices,
            String username, String useremail, String password) {

        /*
        boolean userExists = false;
        try {
            User user = userService.getUser(username);
            if (user != null) {
                userExists = true;
            }
        }
        catch (HttpException ex){
            userExists = true;
        }

        if (userExists) {
            String errorMsg = "The username is alread taken, please select another username.";
            stepConfigure(errorMsg, username, useremail, password);
        }
        else {
            stepInfo(selectedServices, username, useremail, password);
        }
        */
        stepInfo(selectedServices, username, useremail, password);
    }

    public static void stepInfo(String selectedServices,
            String username, String useremail, String password) {
        render(selectedServices, username, useremail, password);
    }

    public static void stepInfoSubmitted(String selectedServices,
            String username, String useremail, String password,
            String organization, String firstname, String lastname, String email, String phone,
            String street, String city, String state, String zip, String country) {

        stepCreateAccount(selectedServices, username, useremail, password,
                organization, firstname, lastname, email, phone,
                street, city, state, zip, country);
    }

    public static void stepCreateAccount(String selectedServices,
            String username, String useremail, String password,
            String organization, String firstname, String lastname, String email, String phone,
            String street, String city, String state, String zip, String country) {

        render(selectedServices,
                username, useremail, password,
                organization, firstname, lastname, email, phone,
                street, city, state, zip, country);
    }

    public static void stepCreateAccountSubmitted(String selectedServices,
            String username, String useremail, String password,
            String organization, String firstname, String lastname, String email, String phone,
            String street, String city, String state, String zip, String country)
        throws HttpException {

        String secretQuestion = " ";
        String secretAnswer = " ";
        String preferredLang = "en-us";
        String timezone = "-06:00";
        String middlename = "";

        username = username.toLowerCase();

        // add customer
        Customer customer = new Customer(organization, CustomerType.ORGANIZATION);
        Customer addedCust = customerDbService.addCustomer(customer);

        if (StringUtils.isEmpty(addedCust.getCustomerNumber())) {
            error(500, "Error encountered creating the customer in the CiS system.");
        }

        // add person
        Person person = new Person(addedCust,
            firstname, lastname, middlename, useremail,
            null, null, null);

        person.addPhysicalAddress(street, city, state, zip, country, true);
        person.addPhone(PhoneType.BUSINESS, phone);
        person.addRole(PersonRole.PRIMARY);

        Person addedPerson = personService.addPerson(person);
        
        if (StringUtils.isEmpty(addedPerson.getPersonNumber())) {
            error("Error encountered adding person to the CiS system.");
        }

        // update person role
        boolean roleUpdated = personService.updateRole(person, PersonRole.PRIMARY);
        if (!roleUpdated) {
            error("Error updating person's role.");
        }

        String personNumber = addedPerson.getPersonNumber();
        String customerNumber = addedCust.getCustomerNumber();

        // add user
        User user = new User(personNumber, customerNumber,
            username, password,
            firstname, lastname, middlename,
            useremail, secretQuestion, secretAnswer,
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