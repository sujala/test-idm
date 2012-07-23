package com.rackspace.idm.util;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriInfo;

import java.util.ArrayList;

import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class WadlTreeTests {


    private WadlTree tree;


    @Before
    public void setup() {
        tree = new WadlTree();
    }

    @Test
    public void shouldPrintTrie() {
        final String wadl = tree.toString();
        Assert.assertNotNull(wadl);
        System.out.println(wadl);
    }

    @Test
    public void constructor_throwsException_stillSucceeds() throws Exception {
        new WadlTree(null);
    }

    @Test
    public void shouldNotFindPermission() {
        final Object permissionFor = tree.getPermissionFor("root" , "/", "DELETE");
        Assert.assertNull(permissionFor);
    }

    @Test
    public void shouldFailWithoutException() throws Exception {
        new WadlTree(null);
        assertTrue("No exception thrown", true);
    }

    @Test
    public void getPermissionFor_withoutError() throws Exception {
        UriInfo uriInfo = mock(UriInfo.class);
        ArrayList<PathSegment> pathSegments = new ArrayList<PathSegment>();
        PathSegment pathSegment = mock(PathSegment.class);
        pathSegments.add(pathSegment);
        when(uriInfo.getPathSegments()).thenReturn(pathSegments);

        tree.getPermissionFor("GET", uriInfo);
        assertTrue("No exception thrown", true);
    }

    @Test
    public void getPermissionFor_withZeroElements_withoutError() throws Exception {
        UriInfo uriInfo = mock(UriInfo.class);
        ArrayList<PathSegment> pathSegments = new ArrayList<PathSegment>();
        when(uriInfo.getPathSegments()).thenReturn(pathSegments);

        tree.getPermissionFor("GET", uriInfo);
        assertTrue("No exception thrown", true);
    }
}
