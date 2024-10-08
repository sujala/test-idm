# -*- coding: utf-8 -*
import copy

import pytest
from qe_coverage.opencafe_decorators import tags, unless_coverage

from tests.api.utils import func_helper
from tests.api.v2 import base
from tests.api.v2.models import factory
from tests.api.v2.models import responses
from tests.api.v2.schema import tokens as tokens_json

from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


class TestImpersonateUser(base.TestBaseV2):

    @classmethod
    @unless_coverage
    def setUpClass(cls):
        super(TestImpersonateUser, cls).setUpClass()

    @unless_coverage
    def setUp(self):
        super(TestImpersonateUser, self).setUp()
        self.user_ids = []
        self.domain_ids = []

        self.user_name = self.generate_random_string(
            pattern=const.USER_ADMIN_PATTERN)
        domain_id = func_helper.generate_randomized_domain_id(
            client=self.identity_admin_client)
        input_data = {
            'domain_id': domain_id
        }
        add_user_request_obj = factory.get_add_user_request_object(
            username=self.user_name, input_data=input_data)
        resp = self.identity_admin_client.add_user(
            request_object=add_user_request_obj)
        user = responses.User(resp.json())
        self.user_ids.append(user.id)
        self.domain_ids.append(user.domain_id)

        self.racker_client = self.generate_racker_client()

    @tags('positive', 'p0', 'regression')
    @pytest.mark.skip_at_gate
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

        modified_schema = copy.deepcopy(tokens_json.impersonation_item)
        modified_schema['properties'][const.ACCESS]['properties'][const.USER][
            'required'].remove(const.RAX_AUTH_PHONE_PIN)
        self.assertSchema(response=resp,
                          json_schema=modified_schema)

        # Validate impersonation token
        resp = self.identity_admin_client.validate_token(
            token_id=token_id)
        self.assertEqual(resp.status_code, 200)
        modified_validate_schema = copy.deepcopy(
            tokens_json.impersonation_item)
        modified_validate_schema['properties'][const.ACCESS]['properties'][
            const.USER]['required'].remove(const.RAX_AUTH_PHONE_PIN)
        self.assertSchema(response=resp,
                          json_schema=modified_validate_schema)

    @tags('positive', 'p1', 'regression')
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

    @base.base.log_tearDown_error
    @unless_coverage
    def tearDown(self):
        # Delete all users created in the tests
        for id_ in self.user_ids:
            resp = self.identity_admin_client.delete_user(user_id=id_)
            self.assertEqual(resp.status_code, 204,
                             msg='User with ID {0} failed to delete'.format(
                                 id_))
        for id_ in self.domain_ids:
            # Disable domain and delete domain
            domain_object = requests.Domain(
                domain_name=id_, enabled=False)
            update_domain_resp = self.identity_admin_client.update_domain(
                domain_id=id_, request_object=domain_object)
            self.assertEqual(update_domain_resp.status_code, 200)
            resp = self.identity_admin_client.delete_domain(domain_id=id_)
            self.assertEqual(resp.status_code, 204,
                             msg='Domain with ID {0} failed to delete'.format(
                                 id_))

        super(TestImpersonateUser, self).tearDown()

    @classmethod
    @unless_coverage
    def tearDownClass(cls):
        super(TestImpersonateUser, cls).tearDownClass()
