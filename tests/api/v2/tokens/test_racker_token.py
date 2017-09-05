# -*- coding: utf-8 -*

from tests.api.v2 import base
from tests.api.v2.schema import tokens as tokens_json

from tests.package.johny import constants as const
from tests.package.johny.v2 import client
from tests.package.johny.v2.models import requests


class TestRackerToken(base.TestBaseV2):

    """ Racker Token tests"""

    def setUp(self):
        super(TestRackerToken, self).setUp()
        self.racker_client = client.IdentityAPIClient(
            url=self.url,
            serialize_format=self.test_config.serialize_format,
            deserialize_format=self.test_config.deserialize_format)

        # Get Token with Racker user & password
        request_object = requests.AuthenticateAsRackerWithPassword(
            racker_name=self.identity_config.racker_username,
            racker_password=self.identity_config.racker_password)

        self.racker_auth_resp = self.racker_client.get_auth_token(
            request_object=request_object)

    def test_get_validate_racker_token(self):

        # racker_client = client.IdentityAPIClient(
        #     url=self.url,
        #     serialize_format=self.test_config.serialize_format,
        #     deserialize_format=self.test_config.deserialize_format)

        # # Get Token with Racker user & password
        # request_object = requests.AuthenticateAsRackerWithPassword(
        #     racker_name=self.identity_config.racker_username,
        #     racker_password=self.identity_config.racker_password)

        # resp = racker_client.get_auth_token(request_object=request_object)
        self.assertEqual(self.racker_auth_resp.status_code, 200)

        # Validate Racker Token
        auth_token = self.racker_auth_resp.json()[
            const.ACCESS][const.TOKEN][const.ID]
        self.racker_client.default_headers[const.X_AUTH_TOKEN] = auth_token

        resp = self.racker_client.validate_token(token_id=auth_token)
        self.assertEqual(resp.status_code, 200)

    def test_analyze_racker_token(self):
        token_id = self.racker_auth_resp.json()[
            const.ACCESS][const.TOKEN][const.ID]
        # The identity_admin used should have the 'analyze-token' role inorder
        # to use the analyze token endpoint, else will result in HTTP 403.
        self.identity_admin_client.default_headers[const.X_SUBJECT_TOKEN] = \
            token_id
        analyze_token_resp = self.identity_admin_client.analyze_token()
        self.assertEqual(analyze_token_resp.status_code, 200)
        self.assertSchema(
            response=analyze_token_resp,
            json_schema=tokens_json.analyze_token)

    def tearDown(self):
        super(TestRackerToken, self).tearDown()
