package com.rackspace.idm.docs;

import java.io.IOException;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import com.rackspace.idm.faults.BadRequest;
import com.rackspace.idm.faults.CustomerIdConflict;
import com.rackspace.idm.faults.EmailConflict;
import com.rackspace.idm.faults.Forbidden;
import com.rackspace.idm.faults.IdmFault;
import com.rackspace.idm.faults.ItemNotFound;
import com.rackspace.idm.faults.PasswordValidationFault;
import com.rackspace.idm.faults.ServiceUnavailable;
import com.rackspace.idm.faults.Unauthorized;
import com.rackspace.idm.faults.UserDisabled;
import com.rackspace.idm.faults.UsernameConflict;

public class FaultSampleGenerator extends SampleGenerator {
    private FaultSampleGenerator() {
        super();
    }

    public static void main(String[] args) throws JAXBException, IOException {
        FaultSampleGenerator sampleGen = new FaultSampleGenerator();

        sampleGen.marshalToFiles(sampleGen.getGeneralIdmFault(), "idm_fault");
        sampleGen.marshalToFiles(sampleGen.getUserDisabledFault(), "user_disabled");
        sampleGen.marshalToFiles(sampleGen.getBadRequest(), "bad_request");
        sampleGen.marshalToFiles(sampleGen.getUnauthorized(), "unauthorized");
        sampleGen.marshalToFiles(sampleGen.getItemNotFound(), "item_not_found");
        sampleGen.marshalToFiles(sampleGen.getForbidden(), "forbidden");
        sampleGen.marshalToFiles(sampleGen.getUsernameConflict(), "username_conflict");
        sampleGen.marshalToFiles(sampleGen.getEmailConflict(), "email_conflict");
        sampleGen.marshalToFiles(sampleGen.getCustomerIdConflict(), "customer_conflict");
        sampleGen.marshalToFiles(sampleGen.getServiceUnavailable(), "service_unavailable");
        sampleGen.marshalToFiles(sampleGen.getPasswordValidationFault(), "password_validation_fault");
    }
    
    private IdmFault getGeneralIdmFault() {
        IdmFault fault = new IdmFault();
        fault.setCode(500);
        fault.setMessage("Fault");
        fault.setDetails("Error Details...");
        return fault;
    }
    
    private UserDisabled getUserDisabledFault() {
        UserDisabled fault = new UserDisabled();
        fault.setCode(403);
        fault.setMessage("The user has been disabled.");
        fault.setDetails("Error Details...");
        return fault;
    }
    
    private BadRequest getBadRequest() {
        BadRequest fault = new BadRequest();
        fault.setCode(400);
        fault.setMessage("Bad Request!");
        fault.setDetails("Error Details...");
        return fault;
    }
    
    private Unauthorized getUnauthorized() {
        Unauthorized fault = new Unauthorized();
        fault.setCode(401);
        fault.setMessage("Oh no you don't.");
        fault.setDetails("Error Details...");
        return fault;
    }
    
    private ItemNotFound getItemNotFound() {
        ItemNotFound fault = new ItemNotFound();
        fault.setCode(404);
        fault.setMessage("Item not found.");
        fault.setDetails("Error Details...");
        return fault;
    }
    
    private Forbidden getForbidden() {
        Forbidden fault = new Forbidden();
        fault.setCode(403);
        fault.setMessage("Forbidden");
        fault.setDetails("Error Details...");
        return fault;
    }
    
    private UsernameConflict getUsernameConflict() {
        UsernameConflict fault = new UsernameConflict();
        fault.setCode(409);
        fault.setMessage("Username already taken.");
        fault.setDetails("Error Details...");
        return fault;
    }
    
    private EmailConflict getEmailConflict() {
        EmailConflict fault = new EmailConflict();
        fault.setCode(409);
        fault.setMessage("Email Address already taken.");
        fault.setDetails("Error Details...");
        return fault;
    }
    
    private CustomerIdConflict getCustomerIdConflict() {
        CustomerIdConflict fault = new CustomerIdConflict();
        fault.setCode(409);
        fault.setMessage("CustomerId already taken.");
        fault.setDetails("Error Details...");
        return fault;
    }
    
    private ServiceUnavailable getServiceUnavailable() {
        ServiceUnavailable fault = new ServiceUnavailable();
        fault.setCode(503);
        fault.setMessage("The IdM Service is Unavailable at the moment.");
        fault.setDetails("Error Details...");
        return fault;
    }
    
    private PasswordValidationFault getPasswordValidationFault() {
        PasswordValidationFault fault = new PasswordValidationFault();
        fault.setCode(404);
        fault.setMessage("The password failed validation.");
        fault.setDetails("Error Details...");
        return fault;
    }
}
