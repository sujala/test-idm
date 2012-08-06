package com.rackspace.idm.domain.entity;

import org.junit.Before;
import org.junit.Test;
import org.openstack.docs.identity.api.v2.Endpoint;

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

    @Test
    public void hashCode_endpointsAndUserDnNull_returnsHashCode() throws Exception {
        assertThat("hash code",endPoints.hashCode(),equalTo(961));
    }

    @Test
    public void hashCode_userDnNull_returnsHashCode() throws Exception {
        endPoints.setEndpoints(new ArrayList<String>());
        assertThat("hash code", endPoints.hashCode(), equalTo(992));
    }

    @Test
    public void hashCode_allFieldsPopulated_returnsHashCode() throws Exception {
        endPoints.setEndpoints(new ArrayList<String>());
        endPoints.setUserDN("userDn");
        assertThat("hash code", endPoints.hashCode(), equalTo(-836030059));
    }

    @Test
    public void equals_nullObject_returnsFalse() throws Exception {
        assertThat("equals",endPoints.equals(null),equalTo(false));
    }

    @Test
    public void equals_objectsAreDifferentClasses_returnsFalse() throws Exception {
        assertThat("equals",endPoints.equals(new Endpoint()),equalTo(false));
    }

    @Test
    public void equals_endpointsAreNullButOtherEndpointsNotNull_returnsFalse() throws Exception {
        EndPoints endPoints1 = new EndPoints();
        endPoints1.setEndpoints(new ArrayList<String>());
        assertThat("equals", endPoints.equals(endPoints1), equalTo(false));
    }

    @Test
    public void equals_endpointsExistAndNotEqual_returnsFalse() throws Exception {
        List<String> endpointList = new ArrayList<String>();
        endpointList.add("endpoint");
        EndPoints endPoints1 = new EndPoints();
        endPoints1.setEndpoints(endpointList);
        endPoints.setEndpoints(new ArrayList<String>(0));
        assertThat("equals", endPoints.equals(endPoints1), equalTo(false));
    }

    @Test
    public void equals_endpointsExistAndEqual_returnsTrue() throws Exception {
        List<String> endpointList = new ArrayList<String>();
        endpointList.add("endpoint");
        EndPoints endPoints1 = new EndPoints();
        endPoints1.setEndpoints(endpointList);
        endPoints.setEndpoints(endpointList);
        assertThat("equals", endPoints.equals(endPoints1), equalTo(true));
    }

    @Test
    public void equals_userDnNullButOtherUserDnNotNull_returnsFalse() throws Exception {
        EndPoints endPoints1 = new EndPoints();
        endPoints1.setUserDN("userDn");
        assertThat("equals", endPoints.equals(endPoints1), equalTo(false));
    }

    @Test
    public void equals_userDnsExistAndNotEqual_returnsFalse() throws Exception {
        EndPoints endPoints1 = new EndPoints();
        endPoints1.setUserDN("userDn");
        endPoints.setUserDN("anotherUserDn");
        assertThat("equals", endPoints.equals(endPoints1), equalTo(false));
    }

    @Test
    public void equals_userDnsExistAndEqual_returnsTrue() throws Exception {
        EndPoints endPoints1 = new EndPoints();
        endPoints1.setUserDN("userDn");
        endPoints.setUserDN("userDn");
        assertThat("equals", endPoints.equals(endPoints1), equalTo(true));
    }
}
