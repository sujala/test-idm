package services;

import models.Customer;
import models.Person;
import models.PersonRole;

public interface CustomerDbService {

    Customer addCustomer(Customer customer);
    Customer getCustomer(String customerNumber);

    Person addPerson(Person person);
    Person getPerson(String personNumber);

    boolean updatePersonRole(Person person, PersonRole role);
}
