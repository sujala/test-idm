package com.rackspace.idm.domain.entity;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Created with IntelliJ IDEA.
 * User: ryan5034
 * Date: 7/11/12
 * Time: 12:32 PM
 * To change this template use File | Settings | File Templates.
 */
public class CloudBaseUrlTest {

    CloudBaseUrl cloudBaseUrl;

    @Before
    public void setUp() throws Exception {
        cloudBaseUrl = new CloudBaseUrl();
    }

    @Test
    public void hashCode_allFieldsNull_returnsHashCode() throws Exception {
        assertThat("hash code",cloudBaseUrl.hashCode(),equalTo(-510534177));
    }

    @Test
    public void hashCode_adminUrlNotNull_returnsHashCode() throws Exception {
        cloudBaseUrl.setAdminUrl("adminUrl");
        assertThat("hash code", cloudBaseUrl.hashCode(), equalTo(1533237631));
    }

    @Test
    public void hashCode_adminUrlAndBaseUrlIdNotNull_returnsHashCode() throws Exception {
        cloudBaseUrl.setAdminUrl("adminUrl");
        cloudBaseUrl.setBaseUrlId(123);
        assertThat("hash code", cloudBaseUrl.hashCode(), equalTo(-520931100));
    }

    @Test
    public void hashCode_adminUrlAndBaseUrlIdAndBaseURlTypeNotNull_returnsHashCode() throws Exception {
        cloudBaseUrl.setAdminUrl("adminUrl");
        cloudBaseUrl.setBaseUrlId(123);
        cloudBaseUrl.setBaseUrlType("baseUrlType");
        assertThat("hash code", cloudBaseUrl.hashCode(), equalTo(-1918234084));
    }

    @Test
    public void hashCode_adminUrlAndBaseUrlIdAndBaseURlTypeAndDefNotNull_returnsHashCode() throws Exception {
        cloudBaseUrl.setAdminUrl("adminUrl");
        cloudBaseUrl.setBaseUrlId(123);
        cloudBaseUrl.setBaseUrlType("baseUrlType");
        cloudBaseUrl.setDef(true);
        assertThat("hash code", cloudBaseUrl.hashCode(), equalTo(-1931196947));
    }

    @Test
    public void hashCode_adminUrlAndBaseUrlIdAndBaseURlTypeAndDefAndEnabledNotNull_returnsHashCode() throws Exception {
        cloudBaseUrl.setAdminUrl("adminUrl");
        cloudBaseUrl.setBaseUrlId(123);
        cloudBaseUrl.setBaseUrlType("baseUrlType");
        cloudBaseUrl.setDef(true);
        cloudBaseUrl.setEnabled(true);
        assertThat("hash code", cloudBaseUrl.hashCode(), equalTo(-2070162436));
    }

    @Test
    public void hashCode_adminUrlAndBaseUrlIdAndBaseURlTypeAndDefAndEnabledAndGlobalNotNull_returnsHashCode() throws Exception {
        cloudBaseUrl.setAdminUrl("adminUrl");
        cloudBaseUrl.setBaseUrlId(123);
        cloudBaseUrl.setBaseUrlType("baseUrlType");
        cloudBaseUrl.setDef(true);
        cloudBaseUrl.setEnabled(true);
        cloudBaseUrl.setGlobal(true);
        assertThat("hash code", cloudBaseUrl.hashCode(), equalTo(834848781));
    }

    @Test
    public void hashCode_adminUrlAndBaseUrlIdAndBaseURlTypeAndDefAndEnabledAndGlobalAndInternalUrlNotNull_returnsHashCode() throws Exception {
        cloudBaseUrl.setAdminUrl("adminUrl");
        cloudBaseUrl.setBaseUrlId(123);
        cloudBaseUrl.setBaseUrlType("baseUrlType");
        cloudBaseUrl.setDef(true);
        cloudBaseUrl.setEnabled(true);
        cloudBaseUrl.setGlobal(true);
        cloudBaseUrl.setInternalUrl("internalUrl");
        assertThat("hash code", cloudBaseUrl.hashCode(), equalTo(66381695));
    }

    @Test
    public void hashCode_publicUrlAndUniqueIdAndVersionIdAndVersionInfoAndVersionListAreNull_returnsHashCode() throws Exception {
        cloudBaseUrl.setAdminUrl("adminUrl");
        cloudBaseUrl.setBaseUrlId(123);
        cloudBaseUrl.setBaseUrlType("baseUrlType");
        cloudBaseUrl.setDef(true);
        cloudBaseUrl.setEnabled(true);
        cloudBaseUrl.setGlobal(true);
        cloudBaseUrl.setInternalUrl("internalUrl");
        cloudBaseUrl.setOpenstackType("openstackType");
        assertThat("hash code", cloudBaseUrl.hashCode(), equalTo(-458787417));
    }

    @Test
    public void hashCode_uniqueIdAndVersionIdAndVersionInfoAndVersionListAreNull_returnsHashCode() throws Exception {
        cloudBaseUrl.setAdminUrl("adminUrl");
        cloudBaseUrl.setBaseUrlId(123);
        cloudBaseUrl.setBaseUrlType("baseUrlType");
        cloudBaseUrl.setDef(true);
        cloudBaseUrl.setEnabled(true);
        cloudBaseUrl.setGlobal(true);
        cloudBaseUrl.setInternalUrl("internalUrl");
        cloudBaseUrl.setOpenstackType("openstackType");
        cloudBaseUrl.setPublicUrl("publicUrl");
        assertThat("hash code", cloudBaseUrl.hashCode(), equalTo(1416707949));
    }

    @Test
    public void hashCode_versionIdAndVersionInfoAndVersionListAreNull_returnsHashCode() throws Exception {
        cloudBaseUrl.setAdminUrl("adminUrl");
        cloudBaseUrl.setBaseUrlId(123);
        cloudBaseUrl.setBaseUrlType("baseUrlType");
        cloudBaseUrl.setDef(true);
        cloudBaseUrl.setEnabled(true);
        cloudBaseUrl.setGlobal(true);
        cloudBaseUrl.setInternalUrl("internalUrl");
        cloudBaseUrl.setOpenstackType("openstackType");
        cloudBaseUrl.setPublicUrl("publicUrl");
        cloudBaseUrl.setUniqueId("uniqueId");
        assertThat("hash code", cloudBaseUrl.hashCode(), equalTo(-524249311));
    }

    @Test
    public void hashCode_versionInfoAndVersionListAreNull_returnsHashCode() throws Exception {
        cloudBaseUrl.setAdminUrl("adminUrl");
        cloudBaseUrl.setBaseUrlId(123);
        cloudBaseUrl.setBaseUrlType("baseUrlType");
        cloudBaseUrl.setDef(true);
        cloudBaseUrl.setEnabled(true);
        cloudBaseUrl.setGlobal(true);
        cloudBaseUrl.setInternalUrl("internalUrl");
        cloudBaseUrl.setOpenstackType("openstackType");
        cloudBaseUrl.setPublicUrl("publicUrl");
        cloudBaseUrl.setUniqueId("uniqueId");
        cloudBaseUrl.setVersionId("123");
        assertThat("hash code", cloudBaseUrl.hashCode(), equalTo(-477458221));
    }

    @Test
    public void hashCode_versionListAreNull_returnsHashCode() throws Exception {
        cloudBaseUrl.setAdminUrl("adminUrl");
        cloudBaseUrl.setBaseUrlId(123);
        cloudBaseUrl.setBaseUrlType("baseUrlType");
        cloudBaseUrl.setDef(true);
        cloudBaseUrl.setEnabled(true);
        cloudBaseUrl.setGlobal(true);
        cloudBaseUrl.setInternalUrl("internalUrl");
        cloudBaseUrl.setOpenstackType("openstackType");
        cloudBaseUrl.setPublicUrl("publicUrl");
        cloudBaseUrl.setUniqueId("uniqueId");
        cloudBaseUrl.setVersionId("123");
        cloudBaseUrl.setVersionInfo("versionInfo");
        assertThat("hash code", cloudBaseUrl.hashCode(), equalTo(-600441875));
    }

    @Test
    public void hashCode_allFieldsPopulated_returnsHashCode() throws Exception {
        cloudBaseUrl.setAdminUrl("adminUrl");
        cloudBaseUrl.setBaseUrlId(123);
        cloudBaseUrl.setBaseUrlType("baseUrlType");
        cloudBaseUrl.setDef(true);
        cloudBaseUrl.setEnabled(true);
        cloudBaseUrl.setGlobal(true);
        cloudBaseUrl.setInternalUrl("internalUrl");
        cloudBaseUrl.setOpenstackType("openstackType");
        cloudBaseUrl.setPublicUrl("publicUrl");
        cloudBaseUrl.setUniqueId("uniqueId");
        cloudBaseUrl.setVersionId("123");
        cloudBaseUrl.setVersionInfo("versionInfo");
        cloudBaseUrl.setVersionList("versionList");
        cloudBaseUrl.setRegion("someRegion");
        cloudBaseUrl.setServiceName("someService");
        assertThat("hash code", cloudBaseUrl.hashCode(), equalTo(-1116460004));
    }

    @Test
    public void equals_objectIsNull_returnsFalse() throws Exception {
        assertThat("equals",cloudBaseUrl.equals(null),equalTo(false));
    }

    @Test
    public void equals_objectsAreDifferentClass_returnsFalse() throws Exception {
        assertThat("equals",cloudBaseUrl.equals(new CloudEndpoint()),equalTo(false));
    }

    @Test
    public void equals_adminUrlIsNullButOtherAdminUrlNotNull_returnsFalse() throws Exception {
        CloudBaseUrl cloudBaseUrl1 = new CloudBaseUrl();
        cloudBaseUrl1.setAdminUrl("adminUrl");
        assertThat("equals",cloudBaseUrl.equals(cloudBaseUrl1),equalTo(false));
    }

    @Test
    public void equals_bothAdminUrlsExistButNotEqual_returnsFalse() throws Exception {
        CloudBaseUrl cloudBaseUrl1 = new CloudBaseUrl();
        cloudBaseUrl1.setAdminUrl("adminUrl");
        cloudBaseUrl.setAdminUrl("anotherAdminUrl");
        assertThat("equals",cloudBaseUrl.equals(cloudBaseUrl1),equalTo(false));
    }

    @Test
    public void equals_bothAdminUrlsExistAndEqual_returnsTrue() throws Exception {
        CloudBaseUrl cloudBaseUrl1 = new CloudBaseUrl();
        cloudBaseUrl1.setAdminUrl("adminUrl");
        cloudBaseUrl.setAdminUrl("adminUrl");
        assertThat("equals",cloudBaseUrl.equals(cloudBaseUrl1),equalTo(true));
    }

    @Test
    public void equals_baseUrlIdIsNullButOtherBaseUrlIdNotNull_returnsFalse() throws Exception {
        CloudBaseUrl cloudBaseUrl1 = new CloudBaseUrl();
        cloudBaseUrl1.setBaseUrlId(123);
        assertThat("equals",cloudBaseUrl.equals(cloudBaseUrl1),equalTo(false));
    }

    @Test
    public void equals_bothBaseUrlIdsExistAndNotEqual_returnsFalse() throws Exception {
        CloudBaseUrl cloudBaseUrl1 = new CloudBaseUrl();
        cloudBaseUrl1.setBaseUrlId(123);
        cloudBaseUrl.setBaseUrlId(456);
        assertThat("equals",cloudBaseUrl.equals(cloudBaseUrl1),equalTo(false));
    }

    @Test
    public void equals_bothBaseUrlIdsExistAndEqual_returnsTrue() throws Exception {
        CloudBaseUrl cloudBaseUrl1 = new CloudBaseUrl();
        cloudBaseUrl1.setBaseUrlId(123);
        cloudBaseUrl.setBaseUrlId(123);
        assertThat("equals",cloudBaseUrl.equals(cloudBaseUrl1),equalTo(true));
    }

    @Test
    public void equals_baseUrlTypeIsNullButOtherBaseUrlTypeNotNull_returnsFalse() throws Exception {
        CloudBaseUrl cloudBaseUrl1 = new CloudBaseUrl();
        cloudBaseUrl1.setBaseUrlType("baseUrlType");
        assertThat("equals",cloudBaseUrl.equals(cloudBaseUrl1),equalTo(false));
    }

    @Test
    public void equals_bothBaseUrlTypesExistButNotEqual_returnsFalse() throws Exception {
        CloudBaseUrl cloudBaseUrl1 = new CloudBaseUrl();
        cloudBaseUrl1.setBaseUrlType("baseUrlType");
        cloudBaseUrl.setBaseUrlType("anotherBaseUrlType");
        assertThat("equals",cloudBaseUrl.equals(cloudBaseUrl1),equalTo(false));
    }

    @Test
    public void equals_bothBaseUrlTypesExistAndEqual_returnsTrue() throws Exception {
        CloudBaseUrl cloudBaseUrl1 = new CloudBaseUrl();
        cloudBaseUrl1.setBaseUrlType("baseUrlType");
        cloudBaseUrl.setBaseUrlType("baseUrlType");
        assertThat("equals",cloudBaseUrl.equals(cloudBaseUrl1),equalTo(true));
    }

    @Test
    public void equals_defIsNullButOtherDefNotNull_returnsFalse() throws Exception {
        CloudBaseUrl cloudBaseUrl1 = new CloudBaseUrl();
        cloudBaseUrl1.setDef(true);
        assertThat("equals",cloudBaseUrl.equals(cloudBaseUrl1),equalTo(false));
    }

    @Test
    public void equals_bothDefsExistAndNotEqual_returnsFalse() throws Exception {
        CloudBaseUrl cloudBaseUrl1 = new CloudBaseUrl();
        cloudBaseUrl1.setDef(true);
        cloudBaseUrl.setDef(false);
        assertThat("equals",cloudBaseUrl.equals(cloudBaseUrl1),equalTo(false));
    }

    @Test
    public void equals_bothDefsExistAndEqual_returnsTrue() throws Exception {
        CloudBaseUrl cloudBaseUrl1 = new CloudBaseUrl();
        cloudBaseUrl1.setDef(true);
        cloudBaseUrl.setDef(true);
        assertThat("equals",cloudBaseUrl.equals(cloudBaseUrl1),equalTo(true));
    }

    @Test
    public void equals_enabledNullButOtherEnabledNotNull_returnsFalse() throws Exception {
        CloudBaseUrl cloudBaseUrl1 = new CloudBaseUrl();
        cloudBaseUrl1.setEnabled(true);
        assertThat("equals",cloudBaseUrl.equals(cloudBaseUrl1),equalTo(false));
    }

    @Test
    public void equals_bothEnabledExistAndNotEqual_returnsFalse() throws Exception {
        CloudBaseUrl cloudBaseUrl1 = new CloudBaseUrl();
        cloudBaseUrl1.setEnabled(true);
        cloudBaseUrl.setEnabled(false);
        assertThat("equals",cloudBaseUrl.equals(cloudBaseUrl1),equalTo(false));
    }

    @Test
    public void equals_bothEnabledExistAndEqual_returnsTrue() throws Exception {
        CloudBaseUrl cloudBaseUrl1 = new CloudBaseUrl();
        cloudBaseUrl1.setEnabled(true);
        cloudBaseUrl.setEnabled(true);
        assertThat("equals",cloudBaseUrl.equals(cloudBaseUrl1),equalTo(true));
    }

    @Test
    public void equals_globalNullButOtherGlobalNotNull_returnsFalse() throws Exception {
        CloudBaseUrl cloudBaseUrl1 = new CloudBaseUrl();
        cloudBaseUrl1.setGlobal(true);
        assertThat("equals",cloudBaseUrl.equals(cloudBaseUrl1),equalTo(false));
    }

    @Test
    public void equals_bothGlobalsExistAndNotEqual_returnsFalse() throws Exception {
        CloudBaseUrl cloudBaseUrl1 = new CloudBaseUrl();
        cloudBaseUrl1.setGlobal(true);
        cloudBaseUrl.setGlobal(false);
        assertThat("equals",cloudBaseUrl.equals(cloudBaseUrl1),equalTo(false));
    }

    @Test
    public void equals_bothGlobalsExistAndEqual_returnsTrue() throws Exception {
        CloudBaseUrl cloudBaseUrl1 = new CloudBaseUrl();
        cloudBaseUrl1.setGlobal(true);
        cloudBaseUrl.setGlobal(true);
        assertThat("equals",cloudBaseUrl.equals(cloudBaseUrl1),equalTo(true));
    }

    @Test
    public void equals_internalUrlNullButOtherInternalUrlNotNull_returnsFalse() throws Exception {
        CloudBaseUrl cloudBaseUrl1 = new CloudBaseUrl();
        cloudBaseUrl1.setInternalUrl("internalUrl");
        assertThat("equals",cloudBaseUrl.equals(cloudBaseUrl1),equalTo(false));
    }

    @Test
    public void equals_bothInternalUrlsExistAndNotEqual_returnsFalse() throws Exception {
        CloudBaseUrl cloudBaseUrl1 = new CloudBaseUrl();
        cloudBaseUrl1.setInternalUrl("internalUrl");
        cloudBaseUrl.setInternalUrl("anotherInternalUrl");
        assertThat("equals",cloudBaseUrl.equals(cloudBaseUrl1),equalTo(false));
    }

    @Test
    public void equals_bothInternalUrlsExistAndEqual_returnsTrue() throws Exception {
        CloudBaseUrl cloudBaseUrl1 = new CloudBaseUrl();
        cloudBaseUrl1.setInternalUrl("internalUrl");
        cloudBaseUrl.setInternalUrl("internalUrl");
        assertThat("equals",cloudBaseUrl.equals(cloudBaseUrl1),equalTo(true));
    }

    @Test
    public void equals_openstackTypeNullButOtherOpenstackTypeNotNull_returnsFalse() throws Exception {
        CloudBaseUrl cloudBaseUrl1 = new CloudBaseUrl();
        cloudBaseUrl1.setOpenstackType("openStackType");
        assertThat("equals",cloudBaseUrl.equals(cloudBaseUrl1),equalTo(false));
    }

    @Test
    public void equals_bothOpenstackTypesExistAndNotEqual_returnsFalse() throws Exception {
        CloudBaseUrl cloudBaseUrl1 = new CloudBaseUrl();
        cloudBaseUrl1.setOpenstackType("openstackType");
        cloudBaseUrl.setOpenstackType("anotherOpenstackType");
        assertThat("equals",cloudBaseUrl.equals(cloudBaseUrl1),equalTo(false));
    }

    @Test
    public void equals_bothOpenstackTypeExistAndEqual_returnsTrue() throws Exception {
        CloudBaseUrl cloudBaseUrl1 = new CloudBaseUrl();
        cloudBaseUrl1.setOpenstackType("openstackType");
        cloudBaseUrl.setOpenstackType("openstackType");
        assertThat("equals",cloudBaseUrl.equals(cloudBaseUrl1),equalTo(true));
    }

    @Test
    public void equals_publicUrlTypeNullButOtherPublicUrlNotNull_returnsFalse() throws Exception {
        CloudBaseUrl cloudBaseUrl1 = new CloudBaseUrl();
        cloudBaseUrl1.setPublicUrl("publicUrl");
        assertThat("equals",cloudBaseUrl.equals(cloudBaseUrl1),equalTo(false));
    }

    @Test
    public void equals_bothPublicUrlsExistAndNotEqual_returnsFalse() throws Exception {
        CloudBaseUrl cloudBaseUrl1 = new CloudBaseUrl();
        cloudBaseUrl1.setPublicUrl("publicUrl");
        cloudBaseUrl.setPublicUrl("anotherPublicUrl");
        assertThat("equals",cloudBaseUrl.equals(cloudBaseUrl1),equalTo(false));
    }

    @Test
    public void equals_bothPublicUrlsExistAndEqual_returnsTrue() throws Exception {
        CloudBaseUrl cloudBaseUrl1 = new CloudBaseUrl();
        cloudBaseUrl1.setPublicUrl("publicUrl");
        cloudBaseUrl.setPublicUrl("publicUrl");
        assertThat("equals",cloudBaseUrl.equals(cloudBaseUrl1),equalTo(true));
    }

    @Test
    public void equals_regionNullButOtherRegionNotNull_returnsFalse() throws Exception {
        CloudBaseUrl cloudBaseUrl1 = new CloudBaseUrl();
        cloudBaseUrl1.setRegion("region");
        assertThat("equals",cloudBaseUrl.equals(cloudBaseUrl1),equalTo(false));
    }

    @Test
    public void equals_bothRegionsExistAndNotEqual_returnsFalse() throws Exception {
        CloudBaseUrl cloudBaseUrl1 = new CloudBaseUrl();
        cloudBaseUrl1.setRegion("region");
        cloudBaseUrl.setRegion("anotherRegion");
        assertThat("equals",cloudBaseUrl.equals(cloudBaseUrl1),equalTo(false));
    }

    @Test
    public void equals_bothRegionsExistAndEqual_returnsTrue() throws Exception {
        CloudBaseUrl cloudBaseUrl1 = new CloudBaseUrl();
        cloudBaseUrl1.setRegion("region");
        cloudBaseUrl.setRegion("region");
        assertThat("equals",cloudBaseUrl.equals(cloudBaseUrl1),equalTo(true));
    }

    @Test
    public void equals_serviceNameNullButOtherServiceNameNotNull_returnsFalse() throws Exception {
        CloudBaseUrl cloudBaseUrl1 = new CloudBaseUrl();
        cloudBaseUrl1.setServiceName("serviceName");
        assertThat("equals",cloudBaseUrl.equals(cloudBaseUrl1),equalTo(false));
    }

    @Test
    public void equals_bothServiceNamesExistAndNotEqual_returnsFalse() throws Exception {
        CloudBaseUrl cloudBaseUrl1 = new CloudBaseUrl();
        cloudBaseUrl1.setServiceName("serviceName");
        cloudBaseUrl.setServiceName("anotherServiceName");
        assertThat("equals",cloudBaseUrl.equals(cloudBaseUrl1),equalTo(false));
    }

    @Test
    public void equals_bothServiceNamesExistAndEqual_returnsTrue() throws Exception {
        CloudBaseUrl cloudBaseUrl1 = new CloudBaseUrl();
        cloudBaseUrl1.setServiceName("serviceName");
        cloudBaseUrl.setServiceName("serviceName");
        assertThat("equals",cloudBaseUrl.equals(cloudBaseUrl1),equalTo(true));
    }

    @Test
    public void equals_uniqueIdNullButOtherUniqueIdNotNull_returnsFalse() throws Exception {
        CloudBaseUrl cloudBaseUrl1 = new CloudBaseUrl();
        cloudBaseUrl1.setUniqueId("uniqueId");
        assertThat("equals",cloudBaseUrl.equals(cloudBaseUrl1),equalTo(false));
    }

    @Test
    public void equals_bothUniqueIdsExistAndNotEqual_returnsFalse() throws Exception {
        CloudBaseUrl cloudBaseUrl1 = new CloudBaseUrl();
        cloudBaseUrl1.setUniqueId("uniqueId");
        cloudBaseUrl.setUniqueId("anotherUniqueId");
        assertThat("equals",cloudBaseUrl.equals(cloudBaseUrl1),equalTo(false));
    }

    @Test
    public void equals_bothUniqueIdsExistAndEqual_returnsTrue() throws Exception {
        CloudBaseUrl cloudBaseUrl1 = new CloudBaseUrl();
        cloudBaseUrl1.setUniqueId("uniqueId");
        cloudBaseUrl.setUniqueId("uniqueId");
        assertThat("equals",cloudBaseUrl.equals(cloudBaseUrl1),equalTo(true));
    }

    @Test
    public void equals_versionIdNullButOtherVersionIdNotNull_returnsFalse() throws Exception {
        CloudBaseUrl cloudBaseUrl1 = new CloudBaseUrl();
        cloudBaseUrl1.setVersionId("versionId");
        assertThat("equals",cloudBaseUrl.equals(cloudBaseUrl1),equalTo(false));
    }

    @Test
    public void equals_bothVersionIdsExistAndNotEqual_returnsFalse() throws Exception {
        CloudBaseUrl cloudBaseUrl1 = new CloudBaseUrl();
        cloudBaseUrl1.setVersionId("versionId");
        cloudBaseUrl.setVersionId("anotherVersionId");
        assertThat("equals",cloudBaseUrl.equals(cloudBaseUrl1),equalTo(false));
    }

    @Test
    public void equals_bothVersionIdsExistAndEqual_returnsTrue() throws Exception {
        CloudBaseUrl cloudBaseUrl1 = new CloudBaseUrl();
        cloudBaseUrl1.setVersionId("versionId");
        cloudBaseUrl.setVersionId("versionId");
        assertThat("equals",cloudBaseUrl.equals(cloudBaseUrl1),equalTo(true));
    }

    @Test
    public void equals_versionInfoNullButOtherVersionInfoNotNull_returnsFalse() throws Exception {
        CloudBaseUrl cloudBaseUrl1 = new CloudBaseUrl();
        cloudBaseUrl1.setVersionInfo("versionInfo");
        assertThat("equals",cloudBaseUrl.equals(cloudBaseUrl1),equalTo(false));
    }

    @Test
    public void equals_bothVersionInfoExistAndNotEqual_returnsFalse() throws Exception {
        CloudBaseUrl cloudBaseUrl1 = new CloudBaseUrl();
        cloudBaseUrl1.setVersionInfo("versionInfo");
        cloudBaseUrl.setVersionInfo("anotherVersionInfo");
        assertThat("equals",cloudBaseUrl.equals(cloudBaseUrl1),equalTo(false));
    }

    @Test
    public void equals_bothVersionInfoExistAndEqual_returnsTrue() throws Exception {
        CloudBaseUrl cloudBaseUrl1 = new CloudBaseUrl();
        cloudBaseUrl1.setVersionInfo("versionInfo");
        cloudBaseUrl.setVersionInfo("versionInfo");
        assertThat("equals",cloudBaseUrl.equals(cloudBaseUrl1),equalTo(true));
    }

    @Test
    public void equals_versionListNullButOtherVersionListNotNull_returnsFalse() throws Exception {
        CloudBaseUrl cloudBaseUrl1 = new CloudBaseUrl();
        cloudBaseUrl1.setVersionList("versionList");
        assertThat("equals",cloudBaseUrl.equals(cloudBaseUrl1),equalTo(false));
    }

    @Test
    public void equals_bothVersionListExistAndNotEqual_returnsFalse() throws Exception {
        CloudBaseUrl cloudBaseUrl1 = new CloudBaseUrl();
        cloudBaseUrl1.setVersionList("versionList");
        cloudBaseUrl.setVersionList("anotherVersionList");
        assertThat("equals",cloudBaseUrl.equals(cloudBaseUrl1),equalTo(false));
    }

    @Test
    public void equals_bothVersionListExistAndEqual_returnsTrue() throws Exception {
        CloudBaseUrl cloudBaseUrl1 = new CloudBaseUrl();
        cloudBaseUrl1.setVersionList("versionList");
        cloudBaseUrl.setVersionList("versionList");
        assertThat("equals",cloudBaseUrl.equals(cloudBaseUrl1),equalTo(true));
    }
}
