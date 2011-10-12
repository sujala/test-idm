package com.rackspace.idm.util;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;


public class WadlTrieTests {

    private final class MyUriInfo implements UriInfo {
        @Override
        public UriBuilder getRequestUriBuilder() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public URI getRequestUri() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public MultivaluedMap<String, String> getQueryParameters(boolean decode) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public MultivaluedMap<String, String> getQueryParameters() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public List<PathSegment> getPathSegments(boolean decode) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public List<PathSegment> getPathSegments() {
            final List<PathSegment> psegs = new ArrayList<PathSegment>();

            final String[] paths = new String[] { "v1.0", "customers", "wildcard", "users", "mkovacs" };

            for (final String string : paths) {
                psegs.add(
                        new PathSegment() {
                            @Override
                            public String getPath() {
                                return string;
                            }

                            @Override
                            public MultivaluedMap<String, String> getMatrixParameters() {
                                // TODO Auto-generated method stub
                                return null;
                            }
                        } );
            }
            return psegs;
        }

        @Override
        public MultivaluedMap<String, String> getPathParameters(boolean decode) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public MultivaluedMap<String, String> getPathParameters() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getPath(boolean decode) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getPath() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public List<String> getMatchedURIs(boolean decode) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public List<String> getMatchedURIs() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public List<Object> getMatchedResources() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public UriBuilder getBaseUriBuilder() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public URI getBaseUri() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public UriBuilder getAbsolutePathBuilder() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public URI getAbsolutePath() {
            // TODO Auto-generated method stub
            return null;
        }
    }

    private WadlTrie trie;


    @Before
    public void setup() {
        trie = new WadlTrie();
    }

    @Test
    public void shouldPrintTrie() {
        final String wadl = trie.toString();
        Assert.assertNotNull(wadl);
        System.out.println(wadl);
    }

    @Test
    public void shouldNotFindPermission() {
        final Object permissionFor = trie.getPermissionFor("root" , "/", "DELETE");
        Assert.assertNull(permissionFor);
    }
}
