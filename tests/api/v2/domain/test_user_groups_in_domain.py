# -*- coding: utf-8 -*
from tests.api.v2.domain import usergroups
from tests.api.v2.models import factory
from tests.api.v2.schema import user_groups
from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


class UserGroupsInDomain(usergroups.TestUserGroups):
    """
    Tests for user groups in a domain services.
    """

    def setUp(self):
        super(UserGroupsInDomain, self).setUp()
        self.user_admin_client = self.generate_client(
            parent_client=self.identity_admin_client,
            additional_input_data={'domain_id': self.domain_id})

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
        return user_group_resp.json()

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

        # List call with query param
        group_name = list_resp.json()[const.RAX_AUTH_USER_GROUPS][0][
            const.NAME]
        option = {
            const.NAME: group_name
        }
        list_resp = self.user_admin_client.list_user_groups_for_domain(
            self.domain_id, option=option)
        self.assertEqual(len(list_resp.json()[const.RAX_AUTH_USER_GROUPS]), 1)
        self.assertEqual(
            list_resp.json()[const.RAX_AUTH_USER_GROUPS][0][const.NAME],
            group_name)
        self.assertSchema(
            response=list_resp,
            json_schema=user_groups.list_user_groups_for_domain)

    def test_delete_user_group_in_a_domain(self):

        user_group = self.add_user_group()
        group_id = user_group[const.RAX_AUTH_USER_GROUP][const.ID]
        delete_resp = self.user_admin_client.delete_user_group_from_domain(
            domain_id=self.domain_id, group_id=group_id)
        self.assertEqual(delete_resp.status_code, 204)

        get_resp = self.user_admin_client.get_user_group_for_domain(
            domain_id=self.domain_id, group_id=group_id)
        self.assertEqual(get_resp.status_code, 404)

    def test_failed_domain_deletion_does_not_remove_user_group(self):

        group_req = factory.get_add_user_group_request(self.domain_id)
        create_group_resp = self.user_admin_client.add_user_group_to_domain(
            domain_id=self.domain_id, request_object=group_req)
        self.assertEqual(create_group_resp.status_code, 201)
        # Attempt to delete an enabled domain. Expect a 400
        del_resp = self.identity_admin_client.delete_domain(
            domain_id=self.domain_id)
        self.assertEqual(del_resp.status_code, 400)

        # Verify that the user group still exists for that domain
        list_resp = self.user_admin_client.list_user_groups_for_domain(
            domain_id=self.domain_id)
        self.assertEqual(len(list_resp.json()[const.RAX_AUTH_USER_GROUPS]), 1)

    def tearDown(self):
        super(UserGroupsInDomain, self).tearDown()
        # This deletes the domain which automatically deletes any user groups
        # in that domain. Hence, not explicitly deleting the user groups.
        self.delete_client(
            self.user_admin_client,
            parent_client=self.identity_admin_client)
