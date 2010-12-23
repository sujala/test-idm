package com.rackspace.idm.validation;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.ws.rs.core.Response;

import com.rackspace.idm.errors.ApiError;

public class InputValidator {
    private Validator validator;

    public InputValidator(Validator validator) {
        this.validator = validator;
    }

    public <T> ApiError validate(T paramObj, Class<?>... validationGroup) {
        return validate(paramObj, Response.Status.BAD_REQUEST.getStatusCode(),
            validationGroup);
    }

    public <T> ApiError validate(T paramObj, int errorStatus,
        Class<?>... validationGroup) {
        Set<ConstraintViolation<T>> violations = validator.validate(paramObj,
            validationGroup);
        if (violations.size() == 0) {
            return null;
        }

        ApiError err = new ApiError();
        err.setStatusCode(errorStatus);
        // TODO Find out and use a standardized error message.
        err.setMessage("Invalid request: Missing or malformed parameter(s).");
        StringBuilder sb = new StringBuilder();
        for (ConstraintViolation<T> violation : violations) {
            sb.append(String.format("%s %s; ", violation.getPropertyPath(),
                violation.getMessage()));
        }
        err.setDetails(sb.toString());

        return err;
    }
}
