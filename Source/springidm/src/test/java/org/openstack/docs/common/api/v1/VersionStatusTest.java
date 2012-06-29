package org.openstack.docs.common.api.v1;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Created with IntelliJ IDEA.
 * User: Yung Lai
 * Date: 6/29/12
 * Time: 3:27 PM
 * To change this template use File | Settings | File Templates.
 */
public class VersionStatusTest {
    private VersionStatus versionStatus;

    @Before
    public void setUp() throws Exception {
        versionStatus = VersionStatus.ALPHA;
    }

    @Test
    public void value_returnsStringValue() throws Exception {
        String result = versionStatus.value();
        assertThat("string", result, equalTo("ALPHA"));
    }

    @Test
    public void fromValue_returnsValue() throws Exception {
        VersionStatus result = VersionStatus.fromValue("BETA");
        assertThat("string", result.value(), equalTo("BETA"));
    }
}
