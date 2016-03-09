package com.rackspace.idm.api.resource.cloud.email;

import lombok.Getter;
import org.springframework.mail.MailException;

public class MailRecipientFormatException extends MailException {
    private static final String MSG_FORMAT = "The recipient '%s' is invalid.";

    @Getter
    private String invalidRecipient;

    public MailRecipientFormatException(String invalidRecipient) {
        super(String.format(MSG_FORMAT, invalidRecipient));
        this.invalidRecipient = invalidRecipient;
    }

    public MailRecipientFormatException(String invalidRecipient, Throwable cause) {
        super(String.format(MSG_FORMAT, invalidRecipient), cause);
        this.invalidRecipient = invalidRecipient;
    }
}
