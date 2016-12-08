# -*- coding: utf-8 -*
import ddt

from random import randrange
from tests.api import constants as const
from tests.api.v2 import base
from tests.api.v2.models import factory
from tests.api.v2.schema import tokens as tokens_json


@ddt.ddt
class TestRevokeToken(base.TestBaseV2):

    """ Validate Token test"""

    @classmethod
    def setUpClass(cls):
        """Class level set up for the tests
        Create users needed for the tests and generate clients for those users.
        """
        super(TestRevokeToken, cls).setUpClass()
        cls.DOMAIN_ID_TEST = cls.generate_random_string(const.DOMAIN_PATTERN)
        cls.contact_id = randrange(start=const.CONTACT_ID_MIN,
                                   stop=const.CONTACT_ID_MAX)
        cls.user_admin_client = cls.generate_client(
            parent_client=cls.identity_admin_client,
            additional_input_data={'domain_id': cls.DOMAIN_ID_TEST,
                                   'contact_id': cls.contact_id})

    def setUp(self):
        super(TestRevokeToken, self).setUp()
        self.user_ids = []

    def create_user(self):
        """create a new
        :return: username, password
        """
        user_obj = factory.get_add_user_request_object()
        user_resp = self.identity_admin_client.add_user(
            request_object=user_obj)
        self.assertEqual(user_resp.status_code, 201)
        user_id = user_resp.json()[const.USER][const.ID]
        self.user_ids.append(user_id)
        username = user_resp.json()[const.USER][const.USERNAME]
        password = user_resp.json()[const.USER][const.NS_PASSWORD]
        return username, password

    def test_revoke_own_token(self):
        """A user can revoke their own authentication token by submitting the
        DELETE request without specifying the tokenId parameter.
        """
        # validate token
        uac = self.user_admin_client
        resp = self.user_admin_client.validate_token(
            token_id=uac.default_headers[const.X_AUTH_TOKEN])
        self.assertEqual(resp.status_code, 200)
        self.assertSchema(response=resp,
                          json_schema=tokens_json.validate_token)

        # revoke token
        revoke_resp = self.user_admin_client.revoke_token()
        self.assertEqual(revoke_resp.status_code, 204)

        # validate token after revoke with it own token
        uac = self.user_admin_client
        val_resp = self.user_admin_client.validate_token(
            token_id=uac.default_headers[const.X_AUTH_TOKEN])
        self.assertEqual(val_resp.status_code, 401)

        # validate token after revoke with it identity admin
        uac = self.user_admin_client
        val_resp = self.identity_admin_client.validate_token(
            token_id=uac.default_headers[const.X_AUTH_TOKEN])
        self.assertEqual(val_resp.status_code, 404)

    def test_revoke_user_token(self):
        """
        Identity and User administrators can revoke the token for another
        user by including the tokenId parameter in the request
        DELETE /v2.0/tokens/{token_id}
        :return:
        """
        # create and auth with new user and password
        username, password = self.create_user()
        auth_resp = self.identity_admin_client.get_auth_token(
            user=username, password=password)
        self.assertEqual(auth_resp.status_code, 200)
        user_token_id = auth_resp.json()[const.ACCESS][const.TOKEN][const.ID]

        # validate token
        val_resp = self.identity_admin_client.validate_token(
            token_id=user_token_id)
        self.assertEqual(val_resp.status_code, 200)

        # revoke token
        revoke_resp = self.identity_admin_client.revoke_token(
            token_id=user_token_id)
        self.assertEqual(revoke_resp.status_code, 204)

        # validate after token be revoken
        val_resp = self.identity_admin_client.validate_token(
            token_id=user_token_id)
        self.assertEqual(val_resp.status_code, 404)

    def tearDown(self):
        # Delete users
        for _id in self.user_ids:
            self.identity_admin_client.delete_user(user_id=_id)
        super(TestRevokeToken, self).tearDown()

    @classmethod
    def tearDownClass(cls):
        # Delete all users created in the setUpClass
        cls.delete_client(client=cls.user_admin_client,
                          parent_client=cls.identity_admin_client)
        super(TestRevokeToken, cls).tearDownClass()
