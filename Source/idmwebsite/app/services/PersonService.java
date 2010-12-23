package services;

import models.Person;
import models.PersonRole;

public interface PersonService {

    Person addPerson(Person person);
    Person getPerson(String personId);
    boolean updateRole(Person person, PersonRole role);
}