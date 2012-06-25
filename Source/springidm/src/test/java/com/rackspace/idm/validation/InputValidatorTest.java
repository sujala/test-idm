package com.rackspace.idm.validation;

import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.spy;

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

}
