package com.rackspace.idm.api.serviceprofile;

import com.rackspace.idm.util.WadlTrieTests;
import freemarker.template.Configuration;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.UriInfo;

import java.net.URI;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created with IntelliJ IDEA.
 * User: Yung Lai
 * Date: 6/20/12
 * Time: 3:43 PM
 * To change this template use File | Settings | File Templates.
 */
public class ServiceDescriptionTemplateUtilTest {
    private ServiceDescriptionTemplateUtil serviceDescriptionTemplateUtil;
    private Configuration config;
    private UriInfo uriInfo;

    @Before
    public void setUp() throws Exception {
        config = new Configuration();
        uriInfo = mock(UriInfo.class);
        serviceDescriptionTemplateUtil = new ServiceDescriptionTemplateUtil(config);
        ServiceDescriptionTemplateUtil temp = new ServiceDescriptionTemplateUtil();
    }

    @Test
    public void build_invalidPatternAndUriInfo_returnsBlankString() throws Exception {
        String build = serviceDescriptionTemplateUtil.build("", (UriInfo) null);
        assertThat("string", build, equalTo(""));
    }

    @Test
    public void build_validPatternAndUriInfo_returnTemplate() throws Exception {
        URI uri = new URI("uri");
        when(uriInfo.getBaseUri()).thenReturn(uri);
        String build = serviceDescriptionTemplateUtil.build("pattern", uriInfo);
        assertThat("string", build, not(""));
    }

    @Test
    public void build_invalidPatternAndUri_returnsBlankString() throws Exception {
        String build = serviceDescriptionTemplateUtil.build(null, (String)null);
        assertThat("string", build, equalTo(""));
    }

    @Test
    public void build_validPatternAndUri_returnTemplate() throws Exception {
        String build = serviceDescriptionTemplateUtil.build("pattern", "uri");
        assertThat("string", build, not(""));
    }
}
