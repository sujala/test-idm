package com.rackspace.idm.api.resource;

import com.rackspace.idm.api.error.ApiError;
import com.rackspace.idm.domain.entity.FilterParam;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.validation.InputValidator;
import com.sun.jersey.core.provider.EntityHolder;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.notNull;
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

    @Test
    public void validateDomainObject_apiErrorIsNull_succeeds() throws Exception {
        when(inputValidator.validate(anyObject())).thenReturn(null);
        parentResource.validateDomainObject(new Object());
    }

    @Test
    public void createFilterBuilder_createsNewObject() throws Exception {
        ParentResource.FilterBuilder filterBuilder = parentResource.createFilterBuilder();
        assertThat("filter builder", filterBuilder, notNullValue());
    }

    @Test
    public void addFilter_valueIsNotBlank_addsFilterParam() throws Exception {
        ParentResource.FilterBuilder filterBuilder = parentResource.createFilterBuilder();
        FilterParam.FilterParamName paramName = null;
        filterBuilder.addFilter(paramName, "value");
        assertThat("filter", filterBuilder.getFilters().length, equalTo(1));
    }

    @Test
    public void addFilter_valueIsBlank_doesNotAddFilterParam() throws Exception {
        ParentResource.FilterBuilder filterBuilder = parentResource.createFilterBuilder();
        FilterParam.FilterParamName paramName = null;
        filterBuilder.addFilter(paramName, "");
        assertThat("filter", filterBuilder.getFilters().length, equalTo(0));
    }

    @Test
    public void getFilters_returnsFilterList() throws Exception {
        ParentResource.FilterBuilder filterBuilder = parentResource.createFilterBuilder();
        FilterParam.FilterParamName paramName = null;
        filterBuilder.addFilter(paramName, "value");
        FilterParam[] filters = filterBuilder.getFilters();
        assertThat("filter", filters.length, equalTo(1));
    }
}
