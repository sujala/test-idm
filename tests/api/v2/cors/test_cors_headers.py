# -*- coding: utf-8 -*
import ddt
from qe_coverage.opencafe_decorators import tags, unless_coverage

from tests.api.utils import header_validation
from tests.api.v2 import base

from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


@ddt.ddt
class TestCorsHeaders(base.TestBaseV2):

    """Tests CORS headers"""

    @classmethod
    @unless_coverage
    def setUpClass(cls):
        super(TestCorsHeaders, cls).setUpClass()

    @unless_coverage
    def setUp(self):
        super(TestCorsHeaders, self).setUp()

    @tags('negative', 'p1', 'regression')
    def test_cors_headers_only_allow_for_post_to_tokens(self):
        request_origin = 'http://example.rackspace.com'
        headers = {const.ORIGIN: request_origin}
        # Get a token
        req_obj = requests.AuthenticateWithPassword(
            user_name=self.identity_config.identity_admin_user_name,
            password=self.identity_config.identity_admin_password
        )
        response = self.identity_admin_client.get_auth_token(
            request_object=req_obj, headers=headers)

        self.assertEqual(response.status_code, 200)
        header_validation.validate_header_access_control_allow_origin(
            request_origin)(response)

        # Make a different API call (using list domains because that
        # should always return 200 for an Identity admin w/o CORS)
        response = self.identity_admin_client.list_domains(headers=headers)

        # Error
        self.assertEqual(response.status_code, 403)

    # TODO: add test with other token calls when support client available

    @unless_coverage
    @ddt.data("OPTIONS", "POST", "GET", "HEAD", "DELETE", "PUT", "PATCH")
    def test_cors_headers_verify_allow_methods(self, method):
        request_origin = "test.rackspace.com"
        headers = {const.ORIGIN: request_origin,
                   const.ACCESS_CONTROL_REQUEST_METHOD: method}
        # Verify method
        response = self.identity_admin_client.verify_cors(
            method="OPTIONS", url=const.TOKEN_URL, headers=headers
        )
        # only allow "OPTIONS", "POST" and "GET
        if method in ("OPTIONS", "POST", "GET"):
            self.assertEqual(response.status_code, 200)
            header_validation.validate_header_access_control_allow_origin(
                request_origin)(response)
            header_validation.validate_header_access_control_allow_methods(
                method)(response)
            header_validation.validate_header_access_control_allow_credentials(
                'true')(response)
        else:
            self.assertEqual(response.status_code, 403)

    @unless_coverage
    @ddt.data("OPTIONS", "POST", "GET", "HEAD", "DELETE", "PUT", "PATCH")
    def test_cors_headers_all_cors_headers(self, method):
        request_origin = "test.rackspace.com"
        req_headers = 'vary, x-auth-token, accept'
        headers = {const.ORIGIN: request_origin,
                   const.ACCESS_CONTROL_REQUEST_METHOD: method,
                   const.ACCESS_CONTROL_REQUEST_HEADERS: req_headers}

        expected_resp_headers = [const.ACCESS_CONTROL_ALLOW_ORIGIN,
                                 const.ACCESS_CONTROL_ALLOW_CREDENTIALS,
                                 const.ACCESS_CONTROL_ALLOW_HEADERS,
                                 const.ACCESS_CONTROL_ALLOW_METHODS]
        # Verify method
        response = self.identity_admin_client.verify_cors(
            method="OPTIONS", url=const.TOKEN_URL, headers=headers
        )
        if method in ("OPTIONS", "POST", "GET"):
            self.assertEqual(response.status_code, 200)
            header_validation.validate_header_access_control_allow_headers(
                values=req_headers
            )
            header_validation.validate_header_not_present(
                unexpected_headers=[const.ACCESS_CONTROL_EXPOSE_HEADERS])
            header_validation.validate_expected_headers(
                expected_headers=expected_resp_headers)(response)
        else:
            self.assertEqual(response.status_code, 403)

    @unless_coverage
    @ddt.data("OPTIONS", "POST", "GET", "HEAD", "DELETE", "PUT", "PATCH")
    def test_without_origin_header(self, method):
        """
        Pass through cors filter
        :param method:
        :return:
        """
        headers = {const.ACCESS_CONTROL_REQUEST_METHOD: method}
        unexpected_headers = [const.ACCESS_CONTROL_ALLOW_ORIGIN,
                              const.ACCESS_CONTROL_ALLOW_METHODS]
        # Verify method
        response = self.identity_admin_client.verify_cors(
            method="OPTIONS", url=const.TOKEN_URL, headers=headers
        )
        self.assertEqual(response.status_code, 204)
        header_validation.validate_header_vary(const.ORIGIN)(response)
        header_validation.validate_header_vary(
            const.ACCESS_CONTROL_REQUEST_HEADERS)(response)
        header_validation.validate_header_vary(
            const.ACCESS_CONTROL_REQUEST_METHOD)(response)
        # cors headers not included
        header_validation.validate_header_not_present(
            unexpected_headers=unexpected_headers)(response)

    @unless_coverage
    def tearDown(self):
        super(TestCorsHeaders, self).tearDown()

    @classmethod
    @unless_coverage
    def tearDownClass(cls):
        super(TestCorsHeaders, cls).tearDownClass()
