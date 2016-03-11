package com.rackspace.idm.api.resource.cloud.email;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.*;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

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

    private String resolveEmailTemplate(String templateLocation, Map model) {
        LOG.debug("Resolving template at location '" + templateLocation + "'");

        String result = velocityUtils.mergeTemplateIntoString(templateLocation, model);
        return result;
    }

}
