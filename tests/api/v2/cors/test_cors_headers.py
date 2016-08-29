# -*- coding: utf-8 -*
from tests.api.utils import header_validation
from tests.api.v2 import base

class TestCorsHeaders(base.TestBaseV2):

    """Tests CORS headers"""
    @classmethod
    def setUpClass(cls):
        super(TestCorsHeaders, cls).setUpClass()

    def test_cors_headers_only_allow_for_correct_domain(self):
        request_origin = 'example.rackspace.com'
        headers = {'origin': request_origin}
        response = self.identity_admin_client.get_auth_token(
                self.identity_config.identity_admin_user_name,
                self.identity_config.identity_admin_password,
                headers=headers)

        assert response.status_code == 200
        header_validation.validate_header_access_control_allow_origin(
                request_origin)(response)


    def test_cors_headers_prevents_use_of_invalid_domain(self):
        request_origin = 'example.com'
        headers = {'origin': request_origin}
        response = self.identity_admin_client.get_auth_token(
                self.identity_config.identity_admin_user_name,
                self.identity_config.identity_admin_password,
                headers=headers)

        assert response.status_code == 403
