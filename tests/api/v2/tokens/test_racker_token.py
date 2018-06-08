# -*- coding: utf-8 -*
from nose.plugins.attrib import attr
from qe_coverage.opencafe_decorators import tags, unless_coverage

from tests.api.v2 import base
from tests.api.v2.schema import tokens as tokens_json

from tests.package.johny import constants as const
from tests.package.johny.v2 import client
from tests.package.johny.v2.models import requests


class TestRackerToken(base.TestBaseV2):

    """ Racker Token tests"""

    @unless_coverage
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

    @attr(type='smoke_no_log_alpha')
    @tags('positive', 'p0', 'smoke')
    def test_get_validate_racker_token(self):
        """
        Using a separate tag 'smoke_no_log_alpha' for this test, because
        we want to run it separately from other tests in Staging and avoid
        creating log file by using --nologcapture option of nosetests.
        Currently, we do not have a way to mask the credentials in the test
        log files. Hence, for time being, running this test by preventing
        log file creation. If something fails, console output can still help
        and we can mask credentials from appearing in the console output.
        """

        self.assertEqual(self.racker_auth_resp.status_code, 200)

        # Validate Racker Token
        auth_token = self.racker_auth_resp.json()[
            const.ACCESS][const.TOKEN][const.ID]
        self.racker_client.default_headers[const.X_AUTH_TOKEN] = auth_token

        resp = self.racker_client.validate_token(token_id=auth_token)
        self.assertEqual(resp.status_code, 200)

    @tags('positive', 'p0', 'regression')
    def test_analyze_racker_token(self):
        token_id = self.racker_auth_resp.json()[
            const.ACCESS][const.TOKEN][const.ID]
        # The identity_admin user should have the 'analyze-token' role in order
        # to use the analyze token endpoint, else will result in HTTP 403.
        self.identity_admin_client.default_headers[const.X_SUBJECT_TOKEN] = \
            token_id
        analyze_token_resp = self.identity_admin_client.analyze_token()
        self.assertEqual(analyze_token_resp.status_code, 200)
        self.assertSchema(
            response=analyze_token_resp,
            json_schema=tokens_json.analyze_token)

    @unless_coverage
    def tearDown(self):
        super(TestRackerToken, self).tearDown()
