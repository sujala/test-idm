package com.rackspace.idm.api.resource.cloud.email;

import lombok.Getter;
import org.springframework.mail.MailException;

public class MailReplyToFormatException extends MailException {
    private static final String MSG_FORMAT = "The reply address '%s' is invalid.";

    @Getter
    private String invalidReplyTo;

    public MailReplyToFormatException(String invalidReplyTo) {
        super(String.format(MSG_FORMAT, invalidReplyTo));
        this.invalidReplyTo = invalidReplyTo;
    }

    public MailReplyToFormatException(String invalidReplyTo, Throwable cause) {
        super(String.format(MSG_FORMAT, invalidReplyTo), cause);
        this.invalidReplyTo = invalidReplyTo;
    }
}
