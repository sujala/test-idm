package com.rackspace.idm.validation;

import com.rackspace.idm.api.error.ApiError;
import org.hibernate.validator.engine.ConstraintViolationImpl;
import org.junit.Before;
import org.junit.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.validation.groups.Default;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

/**
 * Created with IntelliJ IDEA.
 * User: kurt
 * Date: 6/25/12
 * Time: 1:37 PM
 * To change this template use File | Settings | File Templates.
 */
public class InputValidatorTest {
    InputValidator inputValidator;
    InputValidator spy;

    @Before
    public void setUp() throws Exception {
        inputValidator = new InputValidator();
        spy = spy(inputValidator);
    }

    @Test
    public void validate_callsValidate_with400Status() throws Exception {
        Object paramObj = new Object();
        spy.validate(paramObj);
        verify(spy).validate(paramObj, 400);
    }

    @Test
    public void validate_withErrorStatusSet_withNoConstraints_returnsNull() throws Exception {
        Object paramObj = new Object();
        ApiError error = inputValidator.validate(paramObj, 400);
        assertThat("error", error, nullValue());
    }

    @Test
    public void validate_violationsExist_returnsError() throws Exception {
        Validator validator = mock(Validator.class);
        inputValidator.setValidator(validator);
        Object paramObj = new Object();
        Class[] group = new Class[0];
        Set<ConstraintViolation<Object>> violations = new HashSet<ConstraintViolation<Object>>();
        violations.add(new ConstraintViolationImpl<Object>("messageTemplate","message",Object.class,null,null,paramObj,null,null,null));
        violations.add(new ConstraintViolationImpl<Object>("messageTemplate","second message",Object.class,null,null,paramObj,null,null,null));
        when(validator.validate(paramObj,group)).thenReturn(violations);
        ApiError error = inputValidator.validate(paramObj,0,group);
        assertThat("error code",error.getCode(),equalTo(0));
        assertThat("error message",error.getMessage(),equalTo("Invalid request: Missing or malformed parameter(s)."));
        assertThat("error details",error.getDetails().contains("null second message;"), equalTo(true));
        assertThat("error details",error.getDetails().contains("null message; "), equalTo(true));
    }
}
