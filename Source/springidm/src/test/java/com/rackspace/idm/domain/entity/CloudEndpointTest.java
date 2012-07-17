package com.rackspace.idm.domain.entity;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Created with IntelliJ IDEA.
 * User: ryan5034
 * Date: 7/11/12
 * Time: 10:00 AM
 * To change this template use File | Settings | File Templates.
 */
public class CloudEndpointTest {

    CloudEndpoint cloudEndpoint;

    @Before
    public void setUp() throws Exception {
        cloudEndpoint = new CloudEndpoint();
    }

    @Test
    public void hashCode_allFieldsNullAndV1NotPreferred_returnsHashCode() throws Exception {
        cloudEndpoint.setV1preferred(false);
        assertThat("returns a hash code", cloudEndpoint.hashCode(), equalTo(28630388));
    }

    @Test
    public void hashCode_baseUrlNotNullAndV1NotPreferred_returnsHashCode() throws Exception {
        cloudEndpoint.setV1preferred(false);
        cloudEndpoint.setBaseUrl(new CloudBaseUrl());
        assertThat("returns a hash code", cloudEndpoint.hashCode(), equalTo(-380193837));
    }

    @Test
    public void hashCode_baseUrlAndMossoIdNotNullAndV1NotPreferred_returnsHashCode() throws Exception {
        cloudEndpoint.setV1preferred(false);
        cloudEndpoint.setBaseUrl(new CloudBaseUrl());
        cloudEndpoint.setMossoId(123);
        assertThat("returns a hash code", cloudEndpoint.hashCode(), equalTo(-376529544));
    }

    @Test
    public void hashCode_userNameNullAndV1NotPreferred_returnsHashCode() throws Exception {
        cloudEndpoint.setV1preferred(false);
        cloudEndpoint.setBaseUrl(new CloudBaseUrl());
        cloudEndpoint.setMossoId(123);
        cloudEndpoint.setNastId("123");
        assertThat("returns a hash code", cloudEndpoint.hashCode(), equalTo(-329738454));
    }

    @Test
    public void hashCode_allFieldsPopulatedAndV1NotPreferred_returnsHashCode() throws Exception {
        cloudEndpoint.setV1preferred(false);
        cloudEndpoint.setBaseUrl(new CloudBaseUrl());
        cloudEndpoint.setMossoId(123);
        cloudEndpoint.setNastId("123");
        cloudEndpoint.setUsername("username");
        assertThat("returns a hash code", cloudEndpoint.hashCode(), equalTo(23079188));
    }

    @Test
    public void hashCode_allFieldsPopulatedAndV1Preferred_returnsHashCode() throws Exception {
        cloudEndpoint.setV1preferred(true);
        cloudEndpoint.setBaseUrl(new CloudBaseUrl());
        cloudEndpoint.setMossoId(123);
        cloudEndpoint.setNastId("123");
        cloudEndpoint.setUsername("username");
        assertThat("returns a hash code", cloudEndpoint.hashCode(), equalTo(23079182));
    }

    @Test
    public void equals_objectsAreTheSame_returnsTrue() throws Exception {
        assertThat("equals",cloudEndpoint.equals(cloudEndpoint),equalTo(true));
    }

    @Test
    public void equals_objectIsNull_returnsFalse() throws Exception {
        assertThat("equals",cloudEndpoint.equals(null),equalTo(false));
    }

    @Test
    public void equals_objectsAreDifferentClasses_returnsFalse() throws Exception {
        assertThat("equals",cloudEndpoint.equals(new User()),equalTo(false));
    }

    @Test
    public void equals_baseUrlIsNullButOtherBaseUrlIsNotNull_returnsFalse() throws Exception {
        CloudEndpoint cloudEndpoint1 = new CloudEndpoint();
        cloudEndpoint1.setBaseUrl(new CloudBaseUrl());
        assertThat("equals", cloudEndpoint.equals(cloudEndpoint1), equalTo(false));
    }

    @Test
    public void equals_baseUrlsAreNotNullButNotEqual_returnsFalse() throws Exception {
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setBaseUrlId(123);
        CloudEndpoint cloudEndpoint1 = new CloudEndpoint();
        cloudEndpoint1.setBaseUrl(new CloudBaseUrl());
        cloudEndpoint.setBaseUrl(cloudBaseUrl);
        assertThat("equals", cloudEndpoint.equals(cloudEndpoint1), equalTo(false));
    }

    @Test
    public void equals_baseUrlsAreNotNullAndEqual_returnsTrue() throws Exception {
        CloudBaseUrl cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setBaseUrlId(123);
        CloudEndpoint cloudEndpoint1 = new CloudEndpoint();
        cloudEndpoint1.setBaseUrl(cloudBaseUrl);
        cloudEndpoint.setBaseUrl(cloudBaseUrl);
        assertThat("equals", cloudEndpoint.equals(cloudEndpoint1), equalTo(true));
    }

    @Test
    public void equals_nastIdIsNullButOtherNastIdIsNotNull_returnsFalse() throws Exception {
        CloudEndpoint cloudEndpoint1 = new CloudEndpoint();
        cloudEndpoint1.setNastId("nastId");
        assertThat("equals", cloudEndpoint.equals(cloudEndpoint1), equalTo(false));
    }

    @Test
    public void equals_nastIdsExistButNotEqual_returnsFalse() throws Exception {
        cloudEndpoint.setNastId("456");
        CloudEndpoint cloudEndpoint1 = new CloudEndpoint();
        cloudEndpoint1.setNastId("123");
        assertThat("equals", cloudEndpoint.equals(cloudEndpoint1), equalTo(false));
    }

    @Test
    public void equals_nastIdsExistAndAreEqual_returnsTrue() throws Exception {
        cloudEndpoint.setNastId("123");
        CloudEndpoint cloudEndpoint1 = new CloudEndpoint();
        cloudEndpoint1.setNastId("123");
        assertThat("equals", cloudEndpoint.equals(cloudEndpoint1), equalTo(true));
    }

    @Test
    public void equals_usernameIsNullButOtherUsernameIsNotNull_returnsFalse() throws Exception {
        CloudEndpoint cloudEndpoint1 = new CloudEndpoint();
        cloudEndpoint1.setUsername("username");
        assertThat("equals", cloudEndpoint.equals(cloudEndpoint1), equalTo(false));
    }

    @Test
    public void equals_userNamesExistButNotEqual_returnsFalse() throws Exception {
        cloudEndpoint.setUsername("rclements");
        CloudEndpoint cloudEndpoint1 = new CloudEndpoint();
        cloudEndpoint1.setUsername("jsmith");
        assertThat("equals", cloudEndpoint.equals(cloudEndpoint1), equalTo(false));
    }

    @Test
    public void equals_userNamesExistAndEqual_returnsTrue() throws Exception {
        cloudEndpoint.setUsername("jsmith");
        CloudEndpoint cloudEndpoint1 = new CloudEndpoint();
        cloudEndpoint1.setUsername("jsmith");
        assertThat("equals", cloudEndpoint.equals(cloudEndpoint1), equalTo(true));
    }

    @Test
    public void equals_v1PreferredNotEqual_returnsTrue() throws Exception {
        cloudEndpoint.setV1preferred(false);
        CloudEndpoint cloudEndpoint1 = new CloudEndpoint();
        cloudEndpoint1.setV1preferred(true);
        assertThat("equals", cloudEndpoint.equals(cloudEndpoint1), equalTo(false));
    }

    @Test
    public void equals_v1PreferredEqual_returnsTrue() throws Exception {
        cloudEndpoint.setV1preferred(true);
        CloudEndpoint cloudEndpoint1 = new CloudEndpoint();
        cloudEndpoint1.setV1preferred(true);
        assertThat("equals", cloudEndpoint.equals(cloudEndpoint1), equalTo(true));
    }
}
