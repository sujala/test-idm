package com.rackspace.idm.domain.config.providers;

import junit.framework.TestCase;
import org.junit.Test;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: 7/18/12
 * Time: 3:46 PM
 * To change this template use File | Settings | File Templates.
 */
public class PackageClassDiscovererTest {

    private static Set<Class<?>> classes = new HashSet<Class<?>>();

    @Test
    public void findClassesIn_packageReturnsAllClasses() throws IOException, ClassNotFoundException {
        classes = PackageClassDiscoverer.findClassesIn("org.w3._2005.atom");
        Class<?> class1 = Class.forName("org.w3._2005.atom.Link");
        Class<?> class2 = Class.forName("org.w3._2005.atom.Relation");
        assertThat("classes",classes.contains(class1), equalTo(true));
        assertThat("classes",classes.contains(class2), equalTo(true));
    }
}
