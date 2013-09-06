package com.rackspace.idm.domain.config.providers;

/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: Apr 25, 2012
 * Time: 3:18:55 PM
 * To change this template use File | Settings | File Templates.
 */

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public final class PackageClassDiscoverer {

    private PackageClassDiscoverer() {}

    private static MetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory();

    public static Set<Class<?>> findClassesIn(String... packages) throws ClassNotFoundException, IOException {
        Set<Class<?>> classes = new HashSet<Class<?>>();
        for (String thePackage : packages) {
            String resourcePath = "classpath*:" + thePackage.replace(".", "/") + "/*.class";
            loadResources(resourcePath, classes, metadataReaderFactory);
        }

        Set<Class<?>> result = new HashSet<Class<?>>();

        for (Class<?> cs : classes) {
            if (!cs.isInterface()) {
               result.add(cs);
            }
        }

        return result;

    }

    private static void loadResources(String resourceLocation, Set<Class<?>> classes,
            MetadataReaderFactory metadataReaderFactory) throws IOException, ClassNotFoundException {
        Resource[] res = new PathMatchingResourcePatternResolver().getResources(resourceLocation);
        for (Resource resource : res) {
            if (resource.isReadable()) {
                MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(resource);
                classes.add(Class.forName(metadataReader.getAnnotationMetadata().getClassName()));

            }
        }
    }
}