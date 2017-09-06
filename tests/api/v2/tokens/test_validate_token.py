# -*- coding: utf-8 -*
import ddt
from nose.plugins.attrib import attr
from random import randrange

from tests.api.utils import func_helper
from tests.api.v2 import base
from tests.api.v2.schema import tokens as tokens_json

from tests.package.johny import constants as const


@ddt.ddt
class TestValidateToken(base.TestBaseV2):

    """ Validate Token test"""

    @classmethod
    def setUpClass(cls):
        """Class level set up for the tests
        Create users needed for the tests and generate clients for those users.
        """
        super(TestValidateToken, cls).setUpClass()
        domain_id = func_helper.generate_randomized_domain_id(
            client=cls.identity_admin_client)
        cls.contact_id = randrange(start=const.CONTACT_ID_MIN,
                                   stop=const.CONTACT_ID_MAX)
        cls.user_admin_client = cls.generate_client(
            parent_client=cls.identity_admin_client,
            additional_input_data={'domain_id': domain_id,
                                   'contact_id': cls.contact_id})

    def setUp(self):
        super(TestValidateToken, self).setUp()

    @attr(type='smoke_alpha')
    def test_validate_token_reports_contact_id(self):
        """Check for contact id in validate token response
        """
        uac = self.user_admin_client
        resp = self.user_admin_client.validate_token(
            token_id=uac.default_headers[const.X_AUTH_TOKEN])
        self.assertEqual(resp.status_code, 200)
        self.assertSchema(response=resp,
                          json_schema=tokens_json.validate_token)
        self.assertEqual(resp.json()[const.ACCESS][
            const.USER][const.RAX_AUTH_CONTACTID], str(self.contact_id))

    def test_analyze_user_token(self):
        user_token = self.user_admin_client.default_headers[const.X_AUTH_TOKEN]

        # The identity_admin used should have the 'analyze-token' role inorder
        # to use the analyze token endpoint.
        self.identity_admin_client.default_headers[const.X_SUBJECT_TOKEN] = \
            user_token
        analyze_token_resp = self.identity_admin_client.analyze_token()
        self.assertEqual(analyze_token_resp.status_code, 200)
        self.assertSchema(response=analyze_token_resp,
                          json_schema=tokens_json.analyze_valid_token)

    def test_analyze_invalid_token(self):
        # The identity_admin used should have the 'analyze-token' role inorder
        # to use the analyze token endpoint.
        self.identity_admin_client.default_headers[const.X_SUBJECT_TOKEN] = \
            'apples_bananas'
        analyze_token_resp = self.identity_admin_client.analyze_token()
        self.assertEqual(analyze_token_resp.status_code, 200)
        self.assertSchema(response=analyze_token_resp,
                          json_schema=tokens_json.analyze_token)

    @classmethod
    def tearDownClass(cls):
        # Delete all users created in the setUpClass
        cls.delete_client(client=cls.user_admin_client,
                          parent_client=cls.identity_admin_client)
        super(TestValidateToken, cls).tearDownClass()
