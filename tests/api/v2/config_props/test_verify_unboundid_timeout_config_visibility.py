import ddt
from tests.api.v2 import base
from tests.api.v2.schema import unboundid as unboundid_json

from tests.package.johny import constants as const


@ddt.ddt
class TestListUnboundIdConfigTimeoutSettings(base.TestBaseV2):
    """
    List unboundId config
    """
    @classmethod
    def setUpClass(cls):
        """
        Class level set up for the tests
        """
        super(TestListUnboundIdConfigTimeoutSettings, cls).setUpClass()

    def setUp(self):
        super(TestListUnboundIdConfigTimeoutSettings, self).setUp()

    def test_verify_unboundid_timeout_settings_visibility(self):
        """
        Verify unboundid timeout settings visible in config
        JIRA CID-249 says these must be found in config
        """
        if not self.test_config.run_service_admin_tests:
            self.skipTest('Skipping Service Admin Tests per config value')
        resp = self.devops_client.get_devops_properties()
        self.assertEqual(resp.status_code, 200)
        self.assertSchema(response=resp,
                          json_schema=unboundid_json.config_list)

        idm_prop_names = [idm_property[const.NAME] for idm_property
                          in resp.json()[const.PROPERTIES]]
        for config_name in const.EXPECTED_UNBOUNDID_TIMEOUT_CONFIGS:
            self.assertTrue(config_name in idm_prop_names,
                            msg="Cannot find {0} in idm.properties".format(
                                config_name))

    def tearDown(self):
        super(TestListUnboundIdConfigTimeoutSettings, self).tearDown()
