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
    public void shouldFindPermission() {
        final Object permissionFor = trie.getPermissionFor(new String[] {"root" , "/", "users", "mkovacs", "GET"});
        Assert.assertNotNull(permissionFor);
    }

    @Test
    public void shouldNotFindPermission() {
        final Object permissionFor = trie.getPermissionFor(new String[] {"root" , "/", "bad", "GET"});
        Assert.assertNull(permissionFor);
    }

    @Test
    public void shouldFindPermissionForMultipleWildcards() {
        final Object permissionFor = trie.getPermissionFor(new String[] { "root" , "/", "users", "mkovacs", "baseurlrefs", "wildcard", "GET"});
        Assert.assertNotNull(permissionFor);
    }
}
