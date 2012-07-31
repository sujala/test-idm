package com.rackspace.idm.exception;

import javax.xml.bind.JAXBException;

@SuppressWarnings("serial")
public class BadRequestException extends IdmException {
    public BadRequestException() {
        super();
    }

    public BadRequestException(String message) {
        super(message);
    }

    public BadRequestException(Throwable cause) {
        super(cause);
    }

    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
