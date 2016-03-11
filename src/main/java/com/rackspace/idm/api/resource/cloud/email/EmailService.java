package com.rackspace.idm.api.resource.cloud.email;

import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailParseException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.MailSendException;

import java.util.Map;

/**
 * Email service for sending Identity related emails. Supports Velocity templates and sending simple messages. Currently
 * only sends text messages. HTML/Multipart messages are not currently supported.
 */
public interface EmailService {

    /**
     * Sends an email where subject and content are based on Velocity templates
     *
     * @param emailConfig
     * @param velocityModel
     *
     * @throws MailParseException in case of failure when parsing the message
     * @throws MailAuthenticationException in case of authentication failure
     * @throws MailSendException in case of failure when sending the message
     * @throws MailPreparationException in case subject or content are invalid
     */
    void sendTemplatedTextEmail(EmailConfig emailConfig, Map<String, Object> velocityModel);


    /**
     * Sends an email where subject and content are based on Velocity templates
     *
     * @param emailMetaProperties
     * @param subjectTemplateRelativePath
     * @param contentTemplateRelativePath
     * @param velocityModel
     *
     * @throws MailParseException in case of failure when parsing the message
     * @throws MailAuthenticationException in case of authentication failure
     * @throws MailSendException in case of failure when sending the message
     * @throws MailPreparationException in case subject or content are invalid
     */
    void sendTemplatedTextEmail(EmailMetaProperties emailMetaProperties, String subjectTemplateRelativePath, String contentTemplateRelativePath, Map<String, Object> velocityModel);

    /**
     * Sends an email with the specified content
     *
     * @param emailMetaProperties emailAddresses
     * @param subject             email subject
     * @param message             message body
     *
     * @throws MailParseException          in case of failure when parsing the message
     * @throws MailAuthenticationException in case of authentication failure
     * @throws MailSendException           in case of failure when sending the message
     */
    void sendTextEmail(EmailMetaProperties emailMetaProperties, String subject, String message);

}