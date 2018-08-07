package com.rackspace.idm.api.resource.cloud.email;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailParseException;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.Map;

@Component
public class EmailServiceImpl implements EmailService {

    private static Logger LOG = Logger.getLogger(EmailServiceImpl.class);

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private VelocityUtils velocityUtils;

    @Override
    public void sendTemplatedTextEmail(EmailConfig emailConfig, Map<String, Object> velocityModel) {
        sendTemplatedTextEmail(emailConfig.getEmailMetaProperties(), emailConfig.getSubjectVelocityTemplatePath(), emailConfig.getContentVelocityTemplatePath(), velocityModel);
    }

    @Override
    public void sendTemplatedTextEmail(EmailMetaProperties emailMetaProperties, String subjectTemplateRelativePath, String contentTemplateRelativePath, Map<String, Object> velocityModel) {
        String subject = resolveEmailTemplate(subjectTemplateRelativePath, velocityModel);
        String content = resolveEmailTemplate(contentTemplateRelativePath, velocityModel);

        sendTextEmail(emailMetaProperties, subject, content);
    }

    /**
     * Sends the subject and message to a list of e-mail addresses.
     *
     * @param emailMetaProperties emailAddresses
     * @param subject        email subject
     * @param message        message body
     * @throws MailParseException in case of failure when parsing the message
     * @throws MailAuthenticationException in case of authentication failure
     * @throws MailSendException in case of failure when sending the message
     */
    @Override
    public void sendTextEmail(EmailMetaProperties emailMetaProperties, String subject, String message) {
        if (StringUtils.isBlank(subject)) {
            throw new MailPreparationException("Can not send an empty subject email.");
        }
        if (StringUtils.isBlank(message)) {
            throw new MailPreparationException("Can not send an empty content email.");
        }

        SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
        simpleMailMessage.setTo(emailMetaProperties.getRecipientsAsArray());

        // use the default sending if no sender specified
        simpleMailMessage.setFrom(emailMetaProperties.getSender());
        simpleMailMessage.setReplyTo(emailMetaProperties.getReplyTo());
        simpleMailMessage.setSubject(subject);
        simpleMailMessage.setText(message);

        mailSender.send(simpleMailMessage);
    }

    @Override
    public void sendTemplatedMultiPartMimeEmail(EmailConfig emailConfig, Map<String, Object> velocityModel) throws MessagingException {
        sendTemplatedMultiPartMimeEmail(emailConfig.getEmailMetaProperties(), emailConfig.getSubjectVelocityTemplatePath(), emailConfig.getContentVelocityTemplatePath(), emailConfig.getContentHtmlVelocityTemplatePath(), velocityModel);
    }

    @Override
    public void sendTemplatedMultiPartMimeEmail(EmailMetaProperties emailMetaProperties, String subjectTemplateRelativePath, String contentTemplateRelativePath, String contentHtmlTemplateRelativePath, Map<String, Object> velocityModel) throws MessagingException {
        String subject = resolveEmailTemplate(subjectTemplateRelativePath, velocityModel);
        String content = resolveEmailTemplate(contentTemplateRelativePath, velocityModel);
        String contentHtml = resolveEmailTemplate(contentHtmlTemplateRelativePath, velocityModel);

        sendMultiPartMimeEmail(emailMetaProperties, subject, content, contentHtml);
    }

    @Override
    public void sendMultiPartMimeEmail(EmailMetaProperties emailMetaProperties, String subject, String message, String messageHtml) throws MessagingException {
        if (StringUtils.isBlank(subject)) {
            throw new MailPreparationException("Can not send an empty subject email.");
        }
        if (StringUtils.isBlank(message) || StringUtils.isBlank(messageHtml)) {
            throw new MailPreparationException("Can not send multi-part email, both text and html messages are required.");
        }

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, true);
        mimeMessageHelper.setTo(emailMetaProperties.getRecipientsAsArray());
        mimeMessageHelper.setFrom(emailMetaProperties.getSender());

        // ReplyTo will be default to sender if not set
        if (StringUtils.isNotBlank(emailMetaProperties.getReplyTo())) {
            mimeMessageHelper.setReplyTo(emailMetaProperties.getReplyTo());
        }

        mimeMessageHelper.setSubject(subject);
        mimeMessageHelper.setText(message, messageHtml);

        mailSender.send(mimeMessageHelper.getMimeMessage());
    }

    private String resolveEmailTemplate(String templateLocation, Map model) {
        LOG.debug("Resolving template at location '" + templateLocation + "'");

        String result = velocityUtils.mergeTemplateIntoString(templateLocation, model);
        return result;
    }

}
