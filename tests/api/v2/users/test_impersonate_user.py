# -*- coding: utf-8 -*
from tests.api.v2 import base
from tests.api.v2.models import factory
from tests.api.v2.models import responses

from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


class TestImpersonateUser(base.TestBaseV2):

    def setUp(self):
        super(TestImpersonateUser, self).setUp()
        self.user_ids = []
        self.domain_ids = []

        self.user_name = self.generate_random_string(
            pattern=const.USER_ADMIN_PATTERN)
        add_user_request_obj = factory.get_add_user_request_object(
            username=self.user_name)
        resp = self.identity_admin_client.add_user(
            request_object=add_user_request_obj)
        user = responses.User(resp.json())
        self.user_ids.append(user.id)
        self.domain_ids.append(user.domain_id)

        self.racker_client = self.generate_racker_client()

    def test_impersonate_user(self):
        '''Test for user impersonation.'''
        impersonation_request_obj = requests.ImpersonateUser(
            user_name=self.user_name)

        # Impersonate with racker client
        # See https://jira.rax.io/browse/CID-953
        resp = self.racker_client.impersonate_user(
            request_data=impersonation_request_obj)
        self.assertEqual(resp.status_code, 200)

        token_id = resp.json()[const.ACCESS][const.TOKEN][const.ID]

        # Validate impersonation token
        resp = self.identity_admin_client.validate_token(
            token_id=token_id)
        self.assertEqual(resp.status_code, 200)

    def tearDown(self):
        # Delete all users created in the tests
        for id_ in self.user_ids:
            resp = self.identity_admin_client.delete_user(user_id=id_)
            self.assertEqual(resp.status_code, 204)
        for id_ in self.domain_ids:
            resp = self.identity_admin_client.delete_domain(domain_id=id_)
            self.assertEqual(resp.status_code, 204)

        super(TestImpersonateUser, self).tearDown()
