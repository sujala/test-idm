package com.rackspace.idm.domain.entity;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Created with IntelliJ IDEA.
 * User: yung5027
 * Date: 7/27/12
 * Time: 12:38 PM
 * To change this template use File | Settings | File Templates.
 */
public class FilterParamTest {
    FilterParam filterParam;

    @Before
    public void setUp() throws Exception {
        filterParam = new FilterParam();
    }

    @Test
    public void getValue_returnsValue() throws Exception {
        filterParam.setValue("test");
        Object result = filterParam.getValue();
        assertThat("value", result.toString(), equalTo("test"));
    }
}
