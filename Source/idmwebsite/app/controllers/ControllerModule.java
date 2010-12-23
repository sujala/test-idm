package controllers;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import services.CustomerDbService;
import services.DefaultPersonService;
import services.DefaultUserService;
import services.IdmService;
import services.PersonService;
import services.RestCustomerDbService;
import services.RestIdmService;
import services.UserService;

public class ControllerModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(CustomerDbService.class).to(RestCustomerDbService.class);
        bind(IdmService.class).to(RestIdmService.class);
    }

    @Provides
    UserService provideUserService(IdmService idmService) {
        UserService userService = new DefaultUserService(idmService);
        return userService;
    }

    @Provides
    PersonService providePersonService(CustomerDbService customerDbService) {
        PersonService personService = new DefaultPersonService(customerDbService);
        return personService;
    }
}