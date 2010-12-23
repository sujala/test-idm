package com.rackspace.idm.services;

import java.util.List;

import org.apache.commons.mail.EmailException;

public interface EmailService {

    void sendEmail(List<String> recipients, String from, String subject, String message)
        throws EmailException;
}
