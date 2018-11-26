from nose.plugins.attrib import attr
from qe_coverage.opencafe_decorators import tags, unless_coverage

from tests.api.v2 import base
from tests.api.v2.schema import unboundid as unboundid_json

from tests.package.johny import constants as const


class TestConfigProperties(base.TestBaseV2):
    """
    Get config properties tests
    """
    @classmethod
    @unless_coverage
    def setUpClass(cls):
        """
        Class level set up for the tests
        """
        super(TestConfigProperties, cls).setUpClass()

    @unless_coverage
    def setUp(self):
        super(TestConfigProperties, self).setUp()

    @tags('positive', 'p1', 'regression')
    def test_verify_unboundid_timeout_settings_visibility(self):
        """
        Verify unboundid timeout settings visible in config
        JIRA CID-249 says these must be found in config
        """
        resp = self.devops_client.get_devops_properties()
        self.assertEqual(resp.status_code, 200)

        idm_prop_names = [idm_property[const.NAME] for idm_property
                          in resp.json()[const.PROPERTIES]]
        for config_name in const.EXPECTED_UNBOUNDID_TIMEOUT_CONFIGS:
            self.assertTrue(config_name in idm_prop_names,
                            msg="Cannot find {0} in idm.properties".format(
                                config_name))

    @tags('positive', 'p1', 'regression')
    @attr(type='regression')
    def test_get_config_properties(self):
        """
        Verify the response for get config properties api service
        """
        resp = self.devops_client.get_devops_properties()
        self.assertEqual(resp.status_code, 200)
        self.assertSchema(response=resp,
                          json_schema=unboundid_json.config_list)

    @unless_coverage
    def tearDown(self):
        super(TestConfigProperties, self).tearDown()

    @classmethod
    @unless_coverage
    def tearDownClass(cls):
        super(TestConfigProperties, cls).tearDownClass()
