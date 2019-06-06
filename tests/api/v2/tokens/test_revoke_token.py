# -*- coding: utf-8 -*
import pytest
from qe_coverage.opencafe_decorators import tags, unless_coverage
from random import randrange

from tests.api.v2 import base
from tests.api.v2.models import factory
from tests.api.v2.schema import tokens as tokens_json

from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


class TestRevokeToken(base.TestBaseV2):

    """ Validate Token test"""

    @classmethod
    @unless_coverage
    def setUpClass(cls):
        """Class level set up for the tests
        Create users needed for the tests and generate clients for those users.
        """
        super(TestRevokeToken, cls).setUpClass()

    @unless_coverage
    def setUp(self):
        super(TestRevokeToken, self).setUp()
        self.user_ids = []
        self.domain_ids = []

        domain_id = self.generate_random_string(const.DOMAIN_PATTERN)
        contact_id = randrange(start=const.CONTACT_ID_MIN,
                               stop=const.CONTACT_ID_MAX)
        self.user_admin_client = self.generate_client(
            parent_client=self.identity_admin_client,
            additional_input_data={'domain_id': domain_id,
                                   'contact_id': contact_id})

        self.user_ids.append(self.user_admin_client.default_headers[
                                 const.X_USER_ID])
        self.domain_ids.append(domain_id)

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
        domain_id = user_resp.json()[const.USER][const.RAX_AUTH_DOMAIN_ID]
        self.domain_ids.append(domain_id)
        return username, password

    @pytest.mark.smoke_alpha
    @tags('positive', 'p0', 'smoke')
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

        # validate token after revoke with its own token
        uac = self.user_admin_client
        val_resp = self.user_admin_client.validate_token(
            token_id=uac.default_headers[const.X_AUTH_TOKEN])
        self.assertEqual(val_resp.status_code, 401)

        # validate token using identity admin client, after revoke
        val_resp = self.identity_admin_client.validate_token(
            token_id=uac.default_headers[const.X_AUTH_TOKEN])
        self.assertEqual(val_resp.status_code, 404)

    @tags('positive', 'p0', 'regression')
    def test_analyze_revoked_token(self):
        user_token = self.user_admin_client.default_headers[const.X_AUTH_TOKEN]

        # revoke token
        revoke_resp = self.user_admin_client.revoke_token()
        self.assertEqual(revoke_resp.status_code, 204)

        # Analyze Revoked Token
        # The identity_admin used should have the 'analyze-token' role inorder
        # to use the analyze token endpoint.
        self.identity_admin_client.default_headers[const.X_SUBJECT_TOKEN] = \
            user_token
        analyze_token_resp = self.identity_admin_client.analyze_token()
        self.assertEqual(analyze_token_resp.status_code, 200)
        self.assertSchema(response=analyze_token_resp,
                          json_schema=tokens_json.analyze_token_revoked)

    @pytest.mark.smoke_alpha
    @tags('positive', 'p0', 'smoke')
    def test_revoke_user_token(self):
        """
        Identity and User administrators can revoke the token for another
        user by including the tokenId parameter in the request
        DELETE /v2.0/tokens/{token_id}
        :return:
        """
        # create and auth with new user and password
        username, password = self.create_user()
        auth_obj = requests.AuthenticateWithPassword(user_name=username,
                                                     password=password)
        auth_resp = self.identity_admin_client.get_auth_token(
            request_object=auth_obj)
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

        # validate after token be revoked
        val_resp = self.identity_admin_client.validate_token(
            token_id=user_token_id)
        self.assertEqual(val_resp.status_code, 404)

    @base.base.log_tearDown_error
    @unless_coverage
    def tearDown(self):
        # Delete users & domains
        for _id in self.user_ids:
            resp = self.identity_admin_client.delete_user(user_id=_id)
            self.assertEqual(
                resp.status_code, 204,
                msg='User with ID {0} failed to delete'.format(_id))
        for _id in self.domain_ids:
            # Disable domain prior to delete.
            disable_domain_req = requests.Domain(enabled=False)
            self.identity_admin_client.update_domain(
                domain_id=_id, request_object=disable_domain_req)

            resp = self.identity_admin_client.delete_domain(domain_id=_id)
            self.assertEqual(
                resp.status_code, 204,
                msg='Domain with ID {0} failed to delete'.format(_id))

        super(TestRevokeToken, self).tearDown()

    @classmethod
    @unless_coverage
    def tearDownClass(cls):
        super(TestRevokeToken, cls).tearDownClass()
