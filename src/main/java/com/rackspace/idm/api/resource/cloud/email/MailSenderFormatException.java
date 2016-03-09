package com.rackspace.idm.api.resource.cloud.email;

import lombok.Getter;
import org.springframework.mail.MailException;

public class MailSenderFormatException extends MailException {
    private static final String MSG_FORMAT = "The sender '%s' is invalid.";

    @Getter
    private String invalidSender;

    public MailSenderFormatException(String invalidSender) {
        super(String.format(MSG_FORMAT, invalidSender));
        this.invalidSender = invalidSender;
    }

    public MailSenderFormatException(String invalidSender, Throwable cause) {
        super(String.format(MSG_FORMAT, invalidSender), cause);
        this.invalidSender = invalidSender;
    }
}
