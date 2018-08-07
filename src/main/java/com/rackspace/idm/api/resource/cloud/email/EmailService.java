package com.rackspace.idm.api.resource.cloud.email;

import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailParseException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.MailSendException;

import javax.mail.MessagingException;
import java.util.Map;

/**
 * Email service for sending Identity related emails. Supports Velocity templates and sending simple text and
 * HTML/Multipart messages.
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

    /**
     * Sends a multi-part mime message email where subject and content are based on Velocity templates. Note: Both valid
     * text and HTML content must be supplied.
     *
     * @param emailConfig
     * @param velocityModel
     *
     * @throws MailParseException in case of failure when parsing the message
     * @throws MailAuthenticationException in case of authentication failure
     * @throws MailSendException in case of failure when sending the message
     * @throws MailPreparationException in case subject, content, or contentHtml are invalid
     * @throws MessagingException in case of failure when creating mime message
     */
    void sendTemplatedMultiPartMimeEmail(EmailConfig emailConfig, Map<String, Object> velocityModel) throws MessagingException;

    /**
     * Sends a multi-part mime message email where subject and content are based on Velocity templates. Note: Both valid
     * text and HTML content must be supplied.
     *
     * @param emailMetaProperties
     * @param subjectTemplateRelativePath
     * @param contentTemplateRelativePath
     * @param contentHtmlTemplateRelativePath
     * @param velocityModel
     *
     * @throws MailParseException in case of failure when parsing the message
     * @throws MailAuthenticationException in case of authentication failure
     * @throws MailSendException in case of failure when sending the message
     * @throws MailPreparationException in case subject, content, or contentHtml are invalid
     * @throws MessagingException in case of failure when creating mime message
     */
    void sendTemplatedMultiPartMimeEmail(EmailMetaProperties emailMetaProperties, String subjectTemplateRelativePath, String contentTemplateRelativePath, String contentHtmlTemplateRelativePath, Map<String, Object> velocityModel) throws MessagingException;

    /**
     * Sends a multi-part mime message email with the specified content.
     *
     * @param emailMetaProperties emailAddresses
     * @param subject             email subject
     * @param message             text message body
     * @param messageHtml         html message body
     *
     * @throws MailParseException in case of failure when parsing the message
     * @throws MailAuthenticationException in case of authentication failure
     * @throws MailSendException in case of failure when sending the message
     * @throws MessagingException in case of failure when creating mime message
     * @throws IllegalArgumentException if sender is null
     */
    void sendMultiPartMimeEmail(EmailMetaProperties emailMetaProperties, String subject, String message, String messageHtml) throws MessagingException;
}
