package services;

import models.Person;
import models.PersonRole;

public class DefaultPersonService implements PersonService {

    private CustomerDbService customerDbService;

    public DefaultPersonService(CustomerDbService customerDbService) {
        this.customerDbService = customerDbService;
    }

    public Person addPerson(Person person) {
        Person addedPerson = customerDbService.addPerson(person);
        return addedPerson;
    }

    public Person getPerson(String personId) {
        return customerDbService.getPerson(personId);
    }

    public boolean updateRole(Person person, PersonRole role) {
        boolean success = customerDbService.updatePersonRole(person, role);
        return success;
    }
}
