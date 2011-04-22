package com.rackspace.idm.util;

import java.io.InputStream;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;


public class WadlTrieTests {

    private WadlTrie trie;


    @Before
    public void setup() {
        final InputStream is = System.class.getResourceAsStream("/application.wadl");
        trie = new WadlTrie(is);
    }

    @Test
    public void shouldPrintTrie() {
        final String wadl = trie.toString();
        Assert.assertNotNull(wadl);
        System.out.println(wadl);
    }

    @Test
    public void shouldFindPermission() {
        final Object permissionFor = trie.getPermissionFor(new String[] {"root" , "/", "users", "mkovacs", "GET"});
        Assert.assertEquals(permissionFor, "getUserByUsername");
    }

    @Test
    public void shouldNotFindPermission() {
        final Object permissionFor = trie.getPermissionFor(new String[] {"root" , "/", "DELETE"});
        Assert.assertNull(permissionFor);
    }

    @Test
    public void shouldFindPermissionForMultipleWildcards() {
        final Object permissionFor = trie.getPermissionFor(new String[] { "root" , "/", "users", "mkovacs", "baseurlrefs", "wildcard", "GET"});
        Assert.assertEquals(permissionFor, "getBaseUrlRef");
    }
}
