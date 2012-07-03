package com.rackspace.idm.util;

import com.rackspace.idm.domain.entity.OpenstackEndpoint;
import com.rackspacecloud.docs.auth.api.v1.ServiceCatalog;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Created with IntelliJ IDEA.
 * User: kurt
 * Date: 7/3/12
 * Time: 3:47 PM
 * To change this template use File | Settings | File Templates.
 */
public class CloudAuthServiceCatalogFactoryTest {

    @Test(expected = IllegalArgumentException.class)
    public void createNew_withNullList_throwsIllegalArgumentException() throws Exception {
            new CloudAuthServiceCatalogFactory().createNew(null);
    }


    @Test
    public void processService_withNullBaseUrls_DoesNotModifyCatalog() throws Exception {
        ServiceCatalog serviceCatalog = mock(ServiceCatalog.class);
        CloudAuthServiceCatalogFactory.processService(serviceCatalog, new OpenstackEndpoint());
        verify(serviceCatalog, never()).getService();
    }
}
