package com.rackspace.idm.api.resource.cloud;

import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.spi.spring.container.servlet.SpringServlet;
import com.sun.jersey.test.framework.AppDescriptor;
import com.sun.jersey.test.framework.JerseyTest;
import com.sun.jersey.test.framework.WebAppDescriptor;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import javax.ws.rs.ext.Provider;

/**
 * Runs a Jersey server for the duration of a test class. Jersey has a convenient test support class,
 * {@link com.sun.jersey.test.framework.JerseyTest}, but this starts and stops the server around each test, which
 * we found to be too slow for convenience. This class exposes the features of the Jersey class that we need but
 * makes our tests much more efficient by only stopping the server after a whole test class has run. To use it, extend
 * this class, and call {@link #ensureGrizzlyStarted(String, Class[])} in your test's &#064;Before method. Then use
 * the Jersey {@link com.sun.jersey.api.client.WebResource} returned by that method or by a call to {@link #resource()}
 * to make calls to the server.
 * <p/>
 * User: jonburgin
 * Date: Jun 17, 2010
 * Time: 11:21:24 AM
 */
abstract public class AbstractAroundClassJerseyTest {

    private static JerseyTest jerseyTest;
    private static WebResource resource;

    static synchronized public boolean isGrizzlyStarted() {
        return jerseyTest != null;
    }

    static synchronized public WebResource ensureGrizzlyStarted(final String contextConfigLocation, final Class<?>... clientConfigProviderClasses) throws Exception {
        if (!isGrizzlyStarted()) {
            return startGrizzly(contextConfigLocation, clientConfigProviderClasses);
        }
        return resource;
    }

    /**
     * If the test Jersey server isn't running, starts it. Also creates and makes available a {@link com.sun.jersey.api.client.Client
     * com.sun.jersey.api.client.Client} for talking to the server that it starts.
     *
     * @param contextConfigLocation       Spring context files to load in the same format as specified in the web.xml (whitespace-separated
     *                                    locations)
     * @param clientConfigProviderClasses {@link javax.ws.rs.ext.Provider javax.ws.rs.ext.Provider} classes that need to be added to the client
     *                                    which this method creates
     * @return a {@link com.sun.jersey.api.client.WebResource} set to interact with the server
     */
    static synchronized public WebResource startGrizzly(final String contextConfigLocation, final Class<?>... clientConfigProviderClasses) throws Exception {
        if (isGrizzlyStarted()) {
            throw new IllegalStateException("Grizzly is already started. Start and stop first.");
        }

        jerseyTest = new JerseyTest() {

            @Override
            protected AppDescriptor configure() {
                ClientConfig clientConfig = new DefaultClientConfig();
                for (Class<?> aClass : clientConfigProviderClasses) {
                    clientConfig.getClasses().add(aClass);
                }

                /*
                This was added because when converting to/from JSON on the client side, the custom providers were not loaded as message readers/writers for some unknown reason.
                This appears to be a bug in jersey, but I haven't had time to investigate fully. Supposedly setting the init param "com.sun.jersey.config.property.packages" as already done
                by the code should load all the custom idm providers, but it doesn't. For the time being, explicitly loading the json readers/writers works. For a longer
                 term solution just loading all the providers in the standard json writers/readers directories.
                 */
                ClassPathScanningCandidateComponentProvider scanner =
                        new ClassPathScanningCandidateComponentProvider(false);
                scanner.addIncludeFilter(new AnnotationTypeFilter(Provider.class));
                for (BeanDefinition bd : scanner.findCandidateComponents("com.rackspace.idm.api.resource.cloud.v20.json.writers")) {
                    try {
                        clientConfig.getClasses().add(Class.forName(bd.getBeanClassName()));
                    } catch (ClassNotFoundException e) {
                        //eat
                    }
                }
                for (BeanDefinition bd : scanner.findCandidateComponents("com.rackspace.idm.api.resource.cloud.v20.json.readers")) {
                    try {
                        clientConfig.getClasses().add(Class.forName(bd.getBeanClassName()));
                    } catch (ClassNotFoundException e) {
                        //eat
                    }
                }

                for (BeanDefinition bd : scanner.findCandidateComponents("com.rackspace.idm.api.resource.cloud.v11.json.writers")) {
                    try {
                        clientConfig.getClasses().add(Class.forName(bd.getBeanClassName()));
                    } catch (ClassNotFoundException e) {
                        //eat
                    }
                }
                for (BeanDefinition bd : scanner.findCandidateComponents("com.rackspace.idm.api.resource.cloud.v11.json.readers")) {
                    try {
                        clientConfig.getClasses().add(Class.forName(bd.getBeanClassName()));
                    } catch (ClassNotFoundException e) {
                        //eat
                    }
                }

                // Add in the endpoint assignment module json readers/writers
                for (BeanDefinition bd : scanner.findCandidateComponents("com.rackspace.idm.modules.endpointassignment.api.resource.json")) {
                    try {
                        clientConfig.getClasses().add(Class.forName(bd.getBeanClassName()));
                    } catch (ClassNotFoundException e) {
                        // Eat any problems as this is just test setup and is the common pattern by which these are added.
                    }
                }

                // Add in the endpoint assignment module json readers/writers
                for (BeanDefinition bd : scanner.findCandidateComponents("com.rackspace.idm.modules.usergroups.api.resource.json")) {
                    try {
                        clientConfig.getClasses().add(Class.forName(bd.getBeanClassName()));
                    } catch (ClassNotFoundException e) {
                        // Eat any problems as this is just test setup and is the common pattern by which these are added.
                    }
                }

                return new WebAppDescriptor.Builder()
                        .contextListenerClass(org.springframework.web.context.ContextLoaderListener.class)
                        .requestListenerClass(org.springframework.web.context.request.RequestContextListener.class)
                        .contextParam("contextConfigLocation", contextConfigLocation)
                        .clientConfig(clientConfig)
                        .servletClass(SpringServlet.class)
                        .initParam("com.sun.jersey.spi.container.ContainerRequestFilters",
                                "com.rackspace.idm.api.filter.DefaultAcceptHeaderFilter;" +
                                "com.rackspace.idm.api.filter.UriExtensionFilter;" +
                                "com.rackspace.idm.api.filter.IdentityRequestFilter;" +
                                "com.rackspace.idm.api.filter.AuthenticationFilter;" +
                                "com.sun.jersey.api.container.filter.GZIPContentEncodingFilter")
                        .initParam("com.sun.jersey.config.property.packages", "com.rackspace.idm")
                        .initParam("com.sun.jersey.spi.container.ContainerResponseFilters",
                                "com.sun.jersey.api.container.filter.GZIPContentEncodingFilter;" +
                                "com.rackspace.idm.api.filter.AuthenticationHeadersFilter")
                        .contextPath("")
                        .build();
            }
        };
        jerseyTest.setUp();
        jerseyTest.client().setConnectTimeout(null);
        jerseyTest.client().setReadTimeout(null);
        jerseyTest.client().setFollowRedirects(false);
        resource = jerseyTest.resource();
        return resource;
    }

    static synchronized public WebResource startOrRestartGrizzly(final String contextConfigLocation, final Class<?>... clientConfigProviderClasses) throws Exception {
        if (isGrizzlyStarted()) {
            stopGrizzly();
        }
        return ensureGrizzlyStarted(contextConfigLocation, clientConfigProviderClasses);
    }

    static synchronized public void stopGrizzly() throws Exception {
        if (jerseyTest != null) {
            jerseyTest.tearDown();
            jerseyTest = null;
        }
        resource = null;
    }

    public WebResource resource() {
        return resource;
    }
}

