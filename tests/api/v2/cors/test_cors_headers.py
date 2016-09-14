# -*- coding: utf-8 -*
from tests.api.utils import header_validation
from tests.api.v2 import base

class TestCorsHeaders(base.TestBaseV2):

    """Tests CORS headers"""
    @classmethod
    def setUpClass(cls):
        super(TestCorsHeaders, cls).setUpClass()

    def test_cors_headers_only_allow_for_post_to_tokens(self):
        request_origin = 'example.rackspace.com'
        headers = {'origin': request_origin}
        # Get a token
        response = self.identity_admin_client.get_auth_token(
                self.identity_config.identity_admin_user_name,
                self.identity_config.identity_admin_password,
                headers=headers)

        # Success
        assert response.status_code == 200
        header_validation.validate_header_access_control_allow_origin(
                request_origin)(response)

        # Make a different API call (using list domains because that
        # should always return 200 for an Identity admin w/o CORS)
        response = self.identity_admin_client.list_domains(headers=headers)

        # Error
        assert response.status_code == 403

