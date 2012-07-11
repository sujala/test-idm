package com.rackspace.idm.api.serviceprofile;

import com.rackspace.idm.domain.dao.ApiDocDao;
import com.rackspace.idm.exception.NotFoundException;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.UriInfo;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Created with IntelliJ IDEA.
 * User: Yung Lai
 * Date: 6/20/12
 * Time: 4:13 PM
 * To change this template use File | Settings | File Templates.
 */
public class CanonicalContractDescriptionBuilderTest {
    private CanonicalContractDescriptionBuilder canonicalContractDescriptionBuilder;
    private ApiDocDao apiDocDao;
    private ServiceDescriptionTemplateUtil serviceDescriptionTemplateUtil;

    @Before
    public void setUp() throws Exception {
        apiDocDao = mock(ApiDocDao.class);
        serviceDescriptionTemplateUtil = mock(ServiceDescriptionTemplateUtil.class);
        canonicalContractDescriptionBuilder = new CanonicalContractDescriptionBuilder(apiDocDao, serviceDescriptionTemplateUtil);
    }

    @Test
    public void buildInternalVersionPage_versionIdStartWithV1_returnBuild() throws Exception {
        canonicalContractDescriptionBuilder.buildInternalVersionPage("v1", null);
        verify(apiDocDao).getContent("/docs/v1.0/InternalVersionPage.xml");
        verify(serviceDescriptionTemplateUtil).build(anyString(), (UriInfo)eq(null));
    }

    @Test (expected = NotFoundException.class)
    public void buildInternalVersionPage_versionIdDoesNotStartWithV1_throwsNotFound() throws Exception {
        canonicalContractDescriptionBuilder.buildInternalVersionPage("v2", null);
    }

    @Test (expected = NotFoundException.class)
    public void buildPublicVersionPage_throwsNotFound() throws Exception {
        canonicalContractDescriptionBuilder.buildPublicVersionPage(null, null);
    }

    @Test
    public void build_doesTemplateBuild() throws Exception {
        canonicalContractDescriptionBuilder.build("patern", (UriInfo)null);
    }

    @Test
    public void build_validPatternAndUri_returnTemplate() throws Exception {
        String build = canonicalContractDescriptionBuilder.build("pattern", "uri");
        assertThat("string", build, not(""));
    }
}
