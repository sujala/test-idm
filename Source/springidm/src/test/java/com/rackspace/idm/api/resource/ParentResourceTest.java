package com.rackspace.idm.api.resource;

import com.rackspace.idm.api.error.ApiError;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.validation.InputValidator;
import com.sun.jersey.core.provider.EntityHolder;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created with IntelliJ IDEA.
 * User: Yung Lai
 * Date: 6/15/12
 * Time: 4:44 PM
 * To change this template use File | Settings | File Templates.
 */
public class ParentResourceTest {
    private ParentResource parentResource;
    private InputValidator inputValidator;


    @Before
    public void setUp() throws Exception {
        inputValidator = mock(InputValidator.class);
        parentResource = new ParentResource(inputValidator);
    }

    @Test (expected = BadRequestException.class)
    public void validateRequestBody_holderDoesNotHaveEntity_throwsBadRequest() throws Exception {
        EntityHolder<?> holder = new EntityHolder<Object>();
        parentResource.validateRequestBody(holder);
    }

    @Test (expected = BadRequestException.class)
    public void validateDomainObject_apiErrorIsNotNull_throwsBadRequest() throws Exception {
        when(inputValidator.validate(anyObject())).thenReturn(new ApiError());
        parentResource.validateDomainObject(new Object());
    }
}
