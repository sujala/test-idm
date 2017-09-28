# -*- coding: utf-8 -*
from tests.api.v2 import base
from tests.api.v2.models import factory
from tests.api.v2.models import responses
from tests.api.v2.schema import tokens as tokens_json

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
        self.assertSchema(response=resp,
                          json_schema=tokens_json.impersonation_item)

        # Validate impersonation token
        resp = self.identity_admin_client.validate_token(
            token_id=token_id)
        self.assertEqual(resp.status_code, 200)
        self.assertSchema(response=resp,
                          json_schema=tokens_json.validate_token)

    def test_analyze_impersonation_token(self):
        '''Test for analyze user impersonation token.'''
        impersonation_request_obj = requests.ImpersonateUser(
            user_name=self.user_name)

        # Get Impersonation Token
        resp = self.identity_admin_client.impersonate_user(
            request_data=impersonation_request_obj)
        self.assertEqual(resp.status_code, 200)

        token_id = resp.json()[const.ACCESS][const.TOKEN][const.ID]

        # Analyze Token
        # The identity_admin used should have the 'analyze-token' role inorder
        # to use the analyze token endpoint, else will result in HTTP 403.
        self.identity_admin_client.default_headers[const.X_SUBJECT_TOKEN] = \
            token_id
        analyze_token_resp = self.identity_admin_client.analyze_token()
        self.assertEqual(analyze_token_resp.status_code, 200)
        self.assertSchema(response=analyze_token_resp,
                          json_schema=tokens_json.analyze_token)

    def tearDown(self):
        # Delete all users created in the tests
        for id_ in self.user_ids:
            resp = self.identity_admin_client.delete_user(user_id=id_)
            self.assertEqual(resp.status_code, 204)
        for id_ in self.domain_ids:
            # Disable domain and delete domain
            domain_object = requests.Domain(
                domain_name=id_, enabled=False)
            update_domain_resp = self.identity_admin_client.update_domain(
                domain_id=id_, request_object=domain_object)
            self.assertEqual(update_domain_resp.status_code, 200)
            resp = self.identity_admin_client.delete_domain(domain_id=id_)
            self.assertEqual(resp.status_code, 204)

        super(TestImpersonateUser, self).tearDown()
