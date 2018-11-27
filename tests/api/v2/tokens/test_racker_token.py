# -*- coding: utf-8 -*
from nose.plugins.attrib import attr
from qe_coverage.opencafe_decorators import tags, unless_coverage

from tests.api.v2 import base
from tests.api.v2.schema import tokens as tokens_json

from tests.package.johny import constants as const


class TestRackerToken(base.TestBaseV2):

    """ Racker Token tests"""
    @classmethod
    @unless_coverage
    def setUpClass(cls):
        super(TestRackerToken, cls).setUpClass()
        domain_id = cls.generate_random_string(pattern=const.DOMAIN_PATTERN)
        cls.user_admin_client = cls.generate_client(
            parent_client=cls.identity_admin_client,
            additional_input_data={'domain_id': domain_id})

    @unless_coverage
    def setUp(self):
        super(TestRackerToken, self).setUp()
        self.racker_client = self.generate_racker_client()
        self.racker_token = self.racker_client.default_headers[
            const.X_AUTH_TOKEN]

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

        resp = self.racker_client.validate_token(token_id=self.racker_token)
        self.assertEqual(resp.status_code, 200)

    @attr(type='smoke_no_log_alpha')
    @tags('positive', 'p1', 'smoke')
    def test_revoke_racker_token(self):
        # validate token using identity admin client, before revoke
        val_resp = self.identity_admin_client.validate_token(
            token_id=self.racker_token)
        self.assertEqual(val_resp.status_code, 200)

        # revoke racker token
        revoke_resp = self.racker_client.revoke_token()
        self.assertEqual(revoke_resp.status_code, 204)

        # validate token using identity admin client, after revoke
        val_resp = self.identity_admin_client.validate_token(
            token_id=self.racker_token)
        self.assertEqual(val_resp.status_code, 404)

    @attr(type='smoke_no_log_alpha')
    @tags('positive', 'p0', 'smoke')
    def test_revoke_racker_token_by_id(self):
        # validate token using identity admin client, before revoke
        val_resp = self.identity_admin_client.validate_token(
            token_id=self.racker_token)
        self.assertEqual(val_resp.status_code, 200)

        # revoke token by id
        revoke_resp = self.racker_client.revoke_token(
            token_id=self.racker_token)
        self.assertEqual(revoke_resp.status_code, 204)

        # validate token using identity admin client, after revoke
        val_resp = self.identity_admin_client.validate_token(
            token_id=self.racker_token)
        self.assertEqual(val_resp.status_code, 404)

    @attr(type='regression')
    @tags('positive', 'p0', 'regression')
    def test_identity_admin_can_revoke_racker_token(self):
        # validate token using identity admin client, before revoke
        val_resp = self.identity_admin_client.validate_token(
            token_id=self.racker_token)
        self.assertEqual(val_resp.status_code, 200)

        # revoke token by id
        revoke_resp = self.identity_admin_client.revoke_token(
            token_id=self.racker_token)
        self.assertEqual(revoke_resp.status_code, 204)

        # validate token using identity admin client, after revoke
        val_resp = self.identity_admin_client.validate_token(
            token_id=self.racker_token)
        self.assertEqual(val_resp.status_code, 404)

    @attr(type='regression')
    @tags('positive', 'p1', 'regression')
    def test_user_admin_cannot_revoke_racker_token(self):
        # revoke token by id
        revoke_resp = self.user_admin_client.revoke_token(
            token_id=self.racker_token)
        self.assertEqual(revoke_resp.status_code, 403)

    @tags('positive', 'p0', 'regression')
    def test_analyze_racker_token(self):
        # The identity_admin user should have the 'analyze-token' role in order
        # to use the analyze token endpoint, else will result in HTTP 403.
        self.identity_admin_client.default_headers[const.X_SUBJECT_TOKEN] = \
            self.racker_token
        analyze_token_resp = self.identity_admin_client.analyze_token()
        self.assertEqual(analyze_token_resp.status_code, 200)
        self.assertSchema(
            response=analyze_token_resp,
            json_schema=tokens_json.analyze_token)

    @unless_coverage
    def tearDown(self):
        super(TestRackerToken, self).tearDown()

    @classmethod
    @unless_coverage
    def tearDownClass(cls):
        cls.delete_client(client=cls.user_admin_client,
                          parent_client=cls.identity_admin_client)
        super(TestRackerToken, cls).tearDownClass()
