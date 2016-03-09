package com.rackspace.idm.api.resource.cloud.email;

import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.mail.MailPreparationException;

import javax.mail.internet.InternetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class EmailMetaProperties {
    private List<String> recipients = new ArrayList<String>();

    @Getter
    private String sender;

    @Getter
    private String replyTo;

    public EmailMetaProperties(String recipient, String sender, String replyTo) {
        this(Arrays.asList(recipient), sender, replyTo);
    }

    public EmailMetaProperties(List<String> recipients, String sender, String replyTo) {
        if (CollectionUtils.isEmpty(recipients)) {
            throw new MailPreparationException("At least one recipient must be specified");
        }
        for (String recipient : recipients) {
            if (!isValidEmailAddr(recipient)) {
                throw new MailRecipientFormatException(recipient);
            }
            this.recipients.add(recipient);
        }

        if (StringUtils.isBlank(sender)) {
            throw new MailPreparationException("Sender must be specified");
        }
        if (!isValidEmailAddr(sender)) {
            throw new MailSenderFormatException(sender);
        }
        this.sender = sender;

        if (StringUtils.isNotBlank(replyTo)) {
            if (!isValidEmailAddr(replyTo)) {
                throw new MailReplyToFormatException(replyTo);
            }
            this.replyTo = replyTo;
        }

    }

    public static boolean isValidEmailAddr(String emailAddr) {
        try {
            InternetAddress addr = new InternetAddress(emailAddr, true);
        } catch (javax.mail.internet.AddressException ex) {
            return false;
        }
        return true;
    }

    public List<String> getRecipients() {
        return Collections.unmodifiableList(recipients);
    }

    public String[] getRecipientsAsArray() {
        return recipients.toArray(new String[recipients.size()]);
    }
}
