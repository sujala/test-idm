package com.rackspace.idm.services;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.mail.EmailException;
import org.junit.Test;

import com.rackspace.idm.domain.entity.EmailSettings;

public class EmailServiceTests {
    
    @Test
    public void shouldSendEmail() {
        
        int smtpPort = 465;
        String smtpHost = "secure.emailsrvr.com";
        String smtpUsername = "testuser@dev.webmail.us";
        String smtpPassword = "secret77";
        boolean debug = false;
        boolean useSSL = true;
        boolean useTSL = true;
        
        EmailSettings emailSettings = new EmailSettings(smtpPort, smtpHost, smtpUsername, smtpPassword,
            debug, useSSL, useTSL);
        
        EmailService emailService = new DefaultEmailService(emailSettings);
        
        List<String> recipients = new ArrayList<String>();
        recipients.add("huey@webmail.us");
        
        String from = "idmtest@example.com";
        String subject = "Test email from SpringIDM api";
        String message = "Hi!  This is a test message!";
        
        try {
            emailService.sendEmail(recipients, from, subject, message);
        } catch (EmailException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
