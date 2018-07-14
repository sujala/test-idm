# -*- coding: utf-8 -*
from nose.plugins.attrib import attr
from qe_coverage.opencafe_decorators import tags, unless_coverage
from tests.api.v1_0 import base
from tests.api.utils import header_validation
from tests.package.johny import constants as const


class TestAuthentication(base.TestBaseV10):
    """
        Authentication with v1.0
    """
    @classmethod
    @unless_coverage
    def setUpClass(cls):
        super(TestAuthentication, cls).setUpClass()
        cls.username = cls.identity_config.identity_admin_user_name
        cls.api_key = cls.identity_config.identity_admin_apikey

    @unless_coverage
    def setUp(self):
        super(TestAuthentication, self).setUp()

    def verify_expect_headers(self, resp):
        """
        Not include x-storage-token, x-storage-url, and x-cdn-management-token
        because test user doesn't have cloudFiles in service catalog
        """
        expected_headers = [const.X_AUTH_TOKEN,
                            const.X_SERVER_MANAGEMENT_URL,
                            const.X_TENANT_ID]
        header_validation.validate_expected_headers(
            expected_headers=expected_headers)(resp)

        for header in expected_headers:
            self.assertIsNotNone(resp.headers[header])

    @tags('positive', 'p0', 'smoke')
    @attr(type='smoke')
    def test_base_authentication_username_and_key(self):
        normal_response_codes = [200, 204]
        auth_resp = self.identity_client.authenticate(
            x_auth_user=self.username,
            x_auth_key=self.api_key)

        self.assertIn(auth_resp.status_code, normal_response_codes,
                      msg='Auth v1.0 expected {0} received {1}'.format(
                          normal_response_codes,
                          auth_resp.status_code))
        self.verify_expect_headers(resp=auth_resp)
        header_validation.validate_username_header_not_present(auth_resp)

    @tags('positive', 'p0', 'smoke')
    @attr(type='smoke')
    def test_base_authentication_username_and_key_storage(self):
        normal_response_codes = [200, 204]
        auth_resp = self.identity_client.authenticate_storage(
            x_storage_user=self.username,
            x_storage_pass=self.api_key)

        self.assertIn(auth_resp.status_code, normal_response_codes,
                      msg='Auth v1.0 expected {0} received {1}'.format(
                          normal_response_codes,
                          auth_resp.status_code))
        self.verify_expect_headers(resp=auth_resp)
        header_validation.validate_username_header_not_present(auth_resp)

    @tags('negative', 'p1', 'regression')
    @attr('skip_at_gate')
    def test_authentication_username_and_key_invalid_creds(self):
        normal_response_codes = [401]
        unexpected_headers = [const.X_USER_NAME, const.X_TENANT_ID]
        auth_resp = self.identity_client.authenticate_storage(
            x_storage_user=self.username,
            x_storage_pass="invalid")

        self.assertIn(auth_resp.status_code, normal_response_codes,
                      msg='Auth v1.0 expected {0} received {1}'.format(
                          normal_response_codes,
                          auth_resp.status_code))
        header_validation.validate_header_not_present(
            unexpected_headers=unexpected_headers)(auth_resp)

    @unless_coverage
    def tearDown(self):
        super(TestAuthentication, self).tearDown()

    @classmethod
    @unless_coverage
    def tearDownClass(cls):
        super(TestAuthentication, cls).tearDownClass()
