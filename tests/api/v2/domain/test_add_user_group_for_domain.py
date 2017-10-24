# -*- coding: utf-8 -*
import ddt
from nose.plugins.attrib import attr

from tests.api.v2 import base
from tests.api.v2.schema import user_groups
from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


@ddt.ddt
class AddUserGroupForDomain(base.TestBaseV2):
    """
    Tests for Add/Get user group for a domain service
    """
    @classmethod
    def setUpClass(cls):
        super(AddUserGroupForDomain, cls).setUpClass()
        if cls.test_config.use_domain_for_user_groups:
            cls.domain_id = cls.test_config.domain_id
        else:
            cls.domain_id = cls.generate_random_string(pattern='[\d]{7}')
        cls.user_admin_client = cls.generate_client(
            parent_client=cls.identity_admin_client,
            additional_input_data={'domain_id': cls.domain_id})
        cls.user_manager_client = cls.generate_client(
            parent_client=cls.user_admin_client,
            additional_input_data={'is_user_manager': True})
        cls.clients = {
            'user_admin': cls.user_admin_client,
            'user_manager': cls.user_manager_client
        }

    @ddt.data('user_admin', 'user_manager')
    @attr(type='smoke_alpha')
    def test_add_and_get_user_group_for_domain(self, user_type):

        group_name = self.generate_random_string(
            pattern=const.USER_GROUP_NAME_PATTERN)
        group_desc = self.generate_random_string(
            pattern=const.DESC_PATTERN)
        add_user_group_to_domain_req = requests.domainUserGroup(
            group_name=group_name, domain_id=self.domain_id,
            description=group_desc)
        add_user_group_resp = self.clients[user_type].add_user_group_to_domain(
            domain_id=self.domain_id,
            request_object=add_user_group_to_domain_req)
        self.assertEqual(add_user_group_resp.status_code, 201)
        self.assertSchema(add_user_group_resp,
                          json_schema=user_groups.add_user_group_for_domain)
        group_id = add_user_group_resp.json()[
            const.RAX_AUTH_USER_GROUP][const.ID]

        # Get a user group for a domain
        get_user_group_resp = self.clients[
            user_type].get_user_group_for_domain(domain_id=self.domain_id,
                                                 group_id=group_id)
        self.assertEqual(get_user_group_resp.status_code, 200)
        self.assertSchema(get_user_group_resp,
                          json_schema=user_groups.get_user_group_for_domain)

    @classmethod
    def tearDownClass(cls):
        # Not calling 'log_tearDown_error' as delete_client() method is
        # already wrapped with it. So, any cleanup failures will be caught.
        super(AddUserGroupForDomain, cls).tearDownClass()
        # Deleting the user manager first, so that domain can be
        # safely deleted in the subsequent user-admin client cleanup
        cls.identity_admin_client.delete_user(
            cls.user_manager_client.default_headers[const.X_USER_ID])

        # This deletes the domain which automatically deletes any user groups
        # in that domain. Hence, not explicitly deleting the user groups
        cls.delete_client(cls.user_admin_client,
                          parent_client=cls.identity_admin_client)
