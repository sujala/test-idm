# -*- coding: utf-8 -*
import ddt
from random import randrange

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
        cls.DOMAIN_ID_TEST = cls.generate_random_string(const.DOMAIN_PATTERN)
        cls.contact_id = randrange(start=const.CONTACT_ID_MIN,
                                   stop=const.CONTACT_ID_MAX)
        cls.user_admin_client = cls.generate_client(
            parent_client=cls.identity_admin_client,
            additional_input_data={'domain_id': cls.DOMAIN_ID_TEST,
                                   'contact_id': cls.contact_id})

    def setUp(self):
        super(TestValidateToken, self).setUp()

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

    @classmethod
    def tearDownClass(cls):
        # Delete all users created in the setUpClass
        cls.delete_client(client=cls.user_admin_client,
                          parent_client=cls.identity_admin_client)
        super(TestValidateToken, cls).tearDownClass()
