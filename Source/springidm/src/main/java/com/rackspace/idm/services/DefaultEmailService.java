package com.rackspace.idm.services;

import java.util.List;

import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;

import com.rackspace.idm.domain.entity.EmailSettings;

public class DefaultEmailService implements EmailService {
    
    private int smtpPort;
    private String smtpHost;
    private String smtpUsername;
    private String smtpPassword;
    
    private boolean isDebug;
    private boolean useSSL;
    private boolean useTSL;
    
    public DefaultEmailService(EmailSettings emailSettings) {
        this.smtpPort = emailSettings.getSmtpPort();
        this.smtpHost = emailSettings.getSmtpHost();
        this.smtpUsername = emailSettings.getSmtpUsername();
        this.smtpPassword = emailSettings.getSmtpPassword();
        this.isDebug = emailSettings.isDebug();
        this.useSSL = emailSettings.getUseSSL();
        this.useTSL = emailSettings.getUseTSL();
    }

    public void sendEmail(List<String> recipients, String from, String subject,
        String message) throws EmailException {
        
        Email email = new SimpleEmail();
        email.setSmtpPort(this.smtpPort);
        email.setAuthenticator(new DefaultAuthenticator(this.smtpUsername, this.smtpPassword));       
        email.setHostName(this.smtpHost);
        
        email.setFrom(from);
        email.setSubject(subject);
        email.setMsg(message);
        
        for(String toAddress : recipients) {
            email.addTo(toAddress);
        }
        
        email.setDebug(this.isDebug);
        email.setSSL(this.useSSL);
        email.setTLS(this.useTSL);
        email.send();
    }
}
