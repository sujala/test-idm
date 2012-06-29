package com.rackspace.docs.identity.api.ext.rax_ksgrp.v1;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * Created with IntelliJ IDEA.
 * User: ryan5034
 * Date: 6/29/12
 * Time: 3:03 PM
 * To change this template use File | Settings | File Templates.
 */
public class TenantGroupTest {

    TenantGroup tenantGroup;

    @Before
    public void setUp() throws Exception {
        tenantGroup = new TenantGroup();
    }

    @Test
    public void getTenantId_setTenantId_behavesCorrectly() throws Exception {
        assertThat("returns null",tenantGroup.getTenantId(),nullValue());
        tenantGroup.setTenantId("value");
        assertThat("string",tenantGroup.getTenantId(),equalTo("value"));

    }
}
