# -*- coding: utf-8 -*
from tests.api.v2 import base
from tests.api.v2.schema import user_groups
from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


class ListUserGroupsInDomain(base.TestBaseV2):
    """
    Tests for list user groups in a domain service
    """
    @classmethod
    def setUpClass(cls):
        super(ListUserGroupsInDomain, cls).setUpClass()
        cls.domain_id = cls.generate_random_string(pattern='[\d]{7}')
        cls.user_admin_client = cls.generate_client(
            parent_client=cls.identity_admin_client,
            additional_input_data={'domain_id': cls.domain_id})

        cls.domain_ids = []
        cls.domain_ids.append(cls.domain_id)

    def add_user_group(self):

        group_name = self.generate_random_string(
            pattern=const.USER_GROUP_NAME_PATTERN)
        group_desc = self.generate_random_string(
            pattern=const.DESC_PATTERN)
        add_user_group_to_domain_req = requests.domainUserGroup(
            group_name=group_name, domain_id=self.domain_id,
            description=group_desc)
        user_group_resp = self.user_admin_client.add_user_group_to_domain(
            domain_id=self.domain_id,
            request_object=add_user_group_to_domain_req)
        self.assertEqual(user_group_resp.status_code, 201)

    def test_list_user_groups_in_a_domain(self):

        for i in xrange(2):
            self.add_user_group()

        list_resp = self.user_admin_client.list_user_groups_for_domain(
            self.domain_id)
        self.assertEqual(list_resp.status_code, 200)
        self.assertSchema(
            response=list_resp,
            json_schema=user_groups.list_user_groups_for_domain)
        self.assertEqual(len(list_resp.json()[const.RAX_AUTH_USER_GROUPS]), 2)

    @classmethod
    def tearDownClass(cls):
        super(ListUserGroupsInDomain, cls).tearDownClass()
        # This deletes the domain which automatically deletes any user groups
        # in that domain. Hence, not explicitly deleting the user groups
        cls.delete_client(cls.user_admin_client,
                          parent_client=cls.identity_admin_client)
