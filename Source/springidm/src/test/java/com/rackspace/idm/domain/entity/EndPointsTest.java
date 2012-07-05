package com.rackspace.idm.domain.entity;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * Created with IntelliJ IDEA.
 * User: kurt
 * Date: 7/3/12
 * Time: 4:26 PM
 * To change this template use File | Settings | File Templates.
 */
public class EndPointsTest {

    EndPoints endPoints;

    @Before
    public void setUp() throws Exception {
        endPoints = new EndPoints();
    }

    @Test
    public void getsetUserDN() throws Exception {
        assertThat("endpoints userDn", endPoints.getUserDN(), nullValue());
        endPoints.setUserDN("userDn");
        assertThat("endpoints userDn", endPoints.getUserDN(), equalTo("userDn"));
    }

    @Test
    public void getsetEndpoints() throws Exception {
        assertThat("endpoints endpoints", endPoints.getEndpoints(), nullValue());
        List<String> endpoints = new ArrayList<String>();
        endPoints.setEndpoints(endpoints);
        assertThat("endpoints endpoints", endPoints.getEndpoints(), equalTo(endpoints));
    }

    @Test
    public void getsetUsername() throws Exception {
        assertThat("endpoints username", endPoints.getUsername(), nullValue());
        endPoints.setUsername("name");
        assertThat("endpoints name", endPoints.getUsername(), equalTo("name"));
    }

    @Test
    public void getsetNastId() throws Exception {
        assertThat("endpoints nast id", endPoints.getNastId(), nullValue());
        endPoints.setNastId("nastId");
        assertThat("endpoints nast id", endPoints.getNastId(), equalTo("nastId"));
    }

    @Test
    public void getsetMossoId() throws Exception {
        assertThat("endpoints mosso", endPoints.getMossoId(), nullValue());
        endPoints.setMossoId(12345);
        assertThat("endpoints mosso", endPoints.getMossoId(), equalTo(12345));
    }
}
