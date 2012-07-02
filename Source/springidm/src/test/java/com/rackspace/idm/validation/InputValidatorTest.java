package com.rackspace.idm.validation;

import com.rackspace.idm.api.error.ApiError;
import org.junit.Before;
import org.junit.Test;

import javax.validation.groups.Default;

import java.util.ArrayList;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

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
}
