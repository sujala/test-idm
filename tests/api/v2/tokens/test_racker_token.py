# -*- coding: utf-8 -*

from tests.api.v2 import base

from tests.package.johny import constants as const
from tests.package.johny.v2 import client
from tests.package.johny.v2.models import requests


class TestRackerToken(base.TestBaseV2):

    """ Racker Token tests"""

    def setUp(self):
        super(TestRackerToken, self).setUp()

    def test_get_validate_racker_token(self):

        racker_client = client.IdentityAPIClient(
            url=self.url,
            serialize_format=self.test_config.serialize_format,
            deserialize_format=self.test_config.deserialize_format)

        # Get Token with Racker user & password
        request_object = requests.AuthenticateAsRackerWithPassword(
            racker_name=self.identity_config.racker_username,
            racker_password=self.identity_config.racker_password)

        resp = racker_client.get_auth_token(request_object=request_object)
        self.assertEqual(resp.status_code, 200)

        # Validate Racker Token
        auth_token = resp.json()[const.ACCESS][const.TOKEN][const.ID]
        racker_client.default_headers[const.X_AUTH_TOKEN] = auth_token

        resp = racker_client.validate_token(token_id=auth_token)
        self.assertEqual(resp.status_code, 200)

    def tearDown(self):
        super(TestRackerToken, self).tearDown()
