package com.rackspace.idm.api.resource.cloud;

import com.rackspace.idm.api.filter.UriExtensionFilter;
import com.rackspace.idm.domain.dao.impl.InMemoryLdapIntegrationTest;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.spi.spring.container.servlet.SpringServlet;
import org.junit.AfterClass;
import com.sun.jersey.test.framework.AppDescriptor;
import com.sun.jersey.test.framework.JerseyTest;
import com.sun.jersey.test.framework.WebAppDescriptor;

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
abstract public class AbstractAroundClassJerseyTest extends InMemoryLdapIntegrationTest{

    private static JerseyTest jerseyTest;
    private static WebResource resource;

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
    static public WebResource ensureGrizzlyStarted(final String contextConfigLocation, final Class<?>... clientConfigProviderClasses) throws Exception {
        if (jerseyTest != null) {
            return resource;
        }
        jerseyTest = new JerseyTest() {

            @Override
            protected AppDescriptor configure() {
                ClientConfig clientConfig = new DefaultClientConfig();
                for (Class<?> aClass : clientConfigProviderClasses) {
                    clientConfig.getClasses().add(aClass);
                }

                return new WebAppDescriptor.Builder()
                        .contextListenerClass(org.springframework.web.context.ContextLoaderListener.class)
                        .requestListenerClass(org.springframework.web.context.request.RequestContextListener.class)
                        .contextParam("contextConfigLocation", contextConfigLocation)
                        .clientConfig(clientConfig)
                        .servletClass(SpringServlet.class)
                        .initParam("com.sun.jersey.spi.container.ContainerRequestFilters",
                        "com.rackspace.idm.api.filter.UriExtensionFilter")
                        .initParam("com.sun.jersey.config.property.packages",
                        "com.rackspace.idm;org.codehaus.jackson.jaxrs")
                        .contextPath("")
                        .build();
            }
        };
        jerseyTest.setUp();
        resource = jerseyTest.resource();
        return resource;
    }

    public WebResource resource() {
        return resource;
    }

    @AfterClass
    public static void afterClass() throws Exception {
        jerseyTest.tearDown();
        jerseyTest = null;
    }
}

