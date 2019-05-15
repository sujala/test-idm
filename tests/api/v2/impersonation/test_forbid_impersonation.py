# -*- coding: utf-8 -*

import pytest
from qe_coverage.opencafe_decorators import unless_coverage
import ddt

from tests.api.v2 import base
from tests.api.v2.models import factory, responses

from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


@ddt.ddt
class ForbidImpersonation(base.TestBaseV2):

    """
    Forbid Impersonation tests for users having 'identity:internal' role
    """
    @classmethod
    @unless_coverage
    def setUpClass(cls):
        """Class level set up for the tests
        Create users needed for the tests and generate clients for those users.
        """
        super(ForbidImpersonation, cls).setUpClass()
        cls.domain_id = cls.generate_random_string(
            pattern=const.NUMERIC_DOMAIN_ID_PATTERN)
        cls.user_admin_client = cls.generate_client(
            parent_client=cls.identity_admin_client,
            additional_input_data={'domain_id': cls.domain_id})
        if cls.test_config.run_service_admin_tests:
            option = {
                const.PARAM_ROLE_NAME: const.IDENTITY_INTERNAL_ROLE_NAME
            }
            list_resp = cls.service_admin_client.list_roles(option=option)
            cls.identity_internal_role_id = list_resp.json()[
                const.ROLES][0][const.ID]

        cls.racker_client = cls.generate_racker_client()
        cls.clients = {
            'identity_admin': cls.identity_admin_client,
            'racker': cls.racker_client
        }
        cls.domain_ids = []

    @unless_coverage
    def setUp(self):
        super(ForbidImpersonation, self).setUp()
        self.user_ids = []

    def create_user(self, parent_client, is_user_manager=False):

        input_data = {
            'domain_id': self.generate_random_string(
                pattern=const.NUMERIC_DOMAIN_ID_PATTERN)
        }
        request_object = factory.get_add_user_request_object(
            input_data=input_data)
        resp = parent_client.add_user(
            request_object=request_object)
        self.assertEqual(resp.status_code, 201)
        user = responses.User(resp.json())
        user_id, user_name, dom_id = (user.id, user.user_name, user.domain_id)
        self.user_ids.append(user_id)
        self.domain_ids.append(dom_id)
        if is_user_manager:
            self.identity_admin_client.add_role_to_user(
                user_id=user_id, role_id=const.USER_MANAGER_ROLE_ID)
        return user_id, user_name

    def call_impersonate_user(self, username, client):

        impersonation_request_obj = requests.ImpersonateUser(
            user_name=username)
        return client.impersonate_user(request_data=impersonation_request_obj)

    def verify_impersonation_when_user_has_no_internal_role(
            self, username, client):

        impersonate_resp = self.call_impersonate_user(
            username=username, client=client)
        self.assertEqual(impersonate_resp.status_code, 200)

    def verify_impersonation_when_user_has_internal_role(
            self, username, user_id, client):

        self.service_admin_client.add_role_to_user(
            user_id=user_id, role_id=self.identity_internal_role_id)
        impersonate_after_resp = self.call_impersonate_user(
            username=username, client=client)

        self.assertEqual(impersonate_after_resp.status_code, 400)
        # Error message isn't perfect. But, currently set so to avoid contract
        # changes.
        self.assertEqual(
            impersonate_after_resp.json()['badRequest']['message'],
            'User cannot be impersonated; No valid impersonation '
            'roles assigned')

    @unless_coverage
    @ddt.data('identity_admin', 'racker')
    def test_forbid_impersonation_of_user_admin(self, impersonator):

        if not self.test_config.run_service_admin_tests:
            self.skipTest('Skipping Service Admin Tests per config value')

        user_admin_id, user_admin_name = self.create_user(
            parent_client=self.identity_admin_client)
        self.verify_impersonation_when_user_has_no_internal_role(
            username=user_admin_name, client=self.clients[impersonator])
        self.verify_impersonation_when_user_has_internal_role(
            username=user_admin_name, user_id=user_admin_id,
            client=self.clients[impersonator])

    @unless_coverage
    @ddt.data(['identity_admin', True], ['racker', True],
              ['identity_admin', False], ['racker', False])
    @ddt.unpack
    @pytest.mark.skip_at_gate
    def test_forbid_impersonation_of_sub_user(
            self, impersonator, is_user_manager):
        """
        Combining the tests for default user and user-manager user in one
        test method using the parameter 'is_user_manager'. If it is set,
        the test code adds the 'user:manage' role to the user, which converts
        a default user into a user-manager user.
        """

        if not self.test_config.run_service_admin_tests:
            self.skipTest('Skipping Service Admin Tests per config value')

        sub_user_id, sub_user_name = self.create_user(
            parent_client=self.user_admin_client,
            is_user_manager=is_user_manager)
        self.verify_impersonation_when_user_has_no_internal_role(
            username=sub_user_name, client=self.clients[impersonator])
        self.verify_impersonation_when_user_has_internal_role(
            username=sub_user_name, user_id=sub_user_id,
            client=self.clients[impersonator])

    @unless_coverage
    def tearDown(self):

        # Delete all users created in the tests
        for id_ in self.user_ids:
            self.identity_admin_client.delete_user(user_id=id_)
        super(ForbidImpersonation, self).tearDown()

    @classmethod
    @unless_coverage
    def tearDownClass(cls):

        cls.delete_client(client=cls.user_admin_client)

        # Delete all domains created in this test-module
        for dom in set(cls.domain_ids):
            update_req = requests.Domain(domain_name=dom, domain_id=dom,
                                         enabled=False)
            cls.identity_admin_client.update_domain(
                domain_id=dom, request_object=update_req)
            cls.identity_admin_client.delete_domain(domain_id=dom)
        super(ForbidImpersonation, cls).tearDownClass()
