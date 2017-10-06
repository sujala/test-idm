# -*- coding: utf-8 -*
import ddt

from tests.api.v2.domain import usergroups
from tests.api.v2.models import factory, responses
from tests.api.v2.schema import users, user_groups
from tests.package.johny import constants as const


@ddt.ddt
class ListUsersInUserGroup(usergroups.TestUserGroups):
    """
    Tests for List users in user group for a domain service
    """
    def setUp(self):
        super(ListUsersInUserGroup, self).setUp()
        self.user_admin_client = self.generate_client(
            parent_client=self.identity_admin_client,
            additional_input_data={'domain_id': self.domain_id})
        self.user_manager_client = self.generate_client(
            parent_client=self.user_admin_client,
            additional_input_data={'is_user_manager': True})
        self.clients = {
            'user_admin': self.user_admin_client,
            'user_manager': self.user_manager_client
        }
        self.domain_ids = []
        self.domain_ids.append(self.domain_id)

    @ddt.data('user_admin', 'user_manager')
    def test_list_users_in_user_group_for_domain(self, user_type):

        group_req = factory.get_add_user_group_request(self.domain_id)
        create_group_resp = self.user_admin_client.add_user_group_to_domain(
            domain_id=self.domain_id, request_object=group_req)
        self.assertEqual(create_group_resp.status_code, 201)
        group = responses.UserGroup(create_group_resp.json())

        # adding both users to the user group
        add_user_to_grp_resp = self.clients[user_type].add_user_to_user_group(
            user_id=self.user_admin_client.default_headers[const.X_USER_ID],
            group_id=group.id, domain_id=self.domain_id
        )
        self.assertEqual(add_user_to_grp_resp.status_code, 204)
        add_user_to_grp_resp = self.clients[user_type].add_user_to_user_group(
            user_id=self.user_manager_client.default_headers[const.X_USER_ID],
            group_id=group.id, domain_id=self.domain_id
        )
        self.assertEqual(add_user_to_grp_resp.status_code, 204)

        # List users in user group
        list_users_resp = (
            self.clients[user_type].list_users_in_user_group_for_domain(
                domain_id=self.domain_id, group_id=group.id))
        self.assertEqual(list_users_resp.status_code, 200)
        self.assertSchema(list_users_resp, json_schema=users.list_users)

        user_id_list = [user[const.ID] for user in list_users_resp.json()[
            const.USERS]]
        self.assertIn(
            self.user_admin_client.default_headers[const.X_USER_ID],
            user_id_list)
        self.assertIn(
            self.user_manager_client.default_headers[const.X_USER_ID],
            user_id_list)

        # Verify user removal
        self.verify_user_removal_from_user_group(
            group=group, user_type=user_type)

    def verify_user_removal_from_user_group(self, group, user_type):

        del_resp = self.clients[user_type].delete_user_from_user_group(
            domain_id=self.domain_id, group_id=group.id,
            user_id=self.user_admin_client.default_headers[const.X_USER_ID]
        )
        self.assertEqual(del_resp.status_code, 204)
        list_users_resp = (
            self.clients[user_type].list_users_in_user_group_for_domain(
                domain_id=self.domain_id, group_id=group.id))
        self.assertEqual(list_users_resp.status_code, 200)
        user_id_list = [user[const.ID] for user in list_users_resp.json()[
            const.USERS]]
        self.assertNotIn(
            self.user_admin_client.default_headers[const.X_USER_ID],
            user_id_list)
        self.assertIn(
            self.user_manager_client.default_headers[const.X_USER_ID],
            user_id_list)

    @ddt.data('user_admin', 'user_manager')
    def test_get_users_in_user_group(self, user_type):

        group_req = factory.get_add_user_group_request(self.domain_id)
        create_group_resp = self.user_admin_client.add_user_group_to_domain(
            domain_id=self.domain_id, request_object=group_req)
        self.assertEqual(create_group_resp.status_code, 201)
        group = responses.UserGroup(create_group_resp.json())

        # adding both users to the user group
        add_user_to_grp_resp = self.clients[user_type].add_user_to_user_group(
            user_id=self.user_admin_client.default_headers[const.X_USER_ID],
            group_id=group.id, domain_id=self.domain_id
        )
        self.assertEqual(add_user_to_grp_resp.status_code, 204)
        add_user_to_grp_resp = self.clients[user_type].add_user_to_user_group(
            user_id=self.user_manager_client.default_headers[const.X_USER_ID],
            group_id=group.id, domain_id=self.domain_id
        )
        self.assertEqual(add_user_to_grp_resp.status_code, 204)

        user_id = self.clients[user_type].default_headers[const.X_USER_ID]

        # List user groups for user
        list_groups_resp = (
            self.clients[user_type].list_user_groups_for_domain(
                domain_id=self.domain_id,
                requestslib_kwargs={
                    'params': {
                        'userId': user_id
                    }
                }
            ))
        self.assertEqual(list_groups_resp.status_code, 200)
        self.assertSchema(list_groups_resp,
                          json_schema=user_groups.list_user_groups_for_domain)

        user_group_id_list = [
            user_group[const.ID] for user_group in list_groups_resp.json()[
                const.RAX_AUTH_USER_GROUPS]]

        self.assertIn(
            group.id,
            user_group_id_list)

        # Delete users from user group
        delete_users_resp = (
            self.clients[user_type].delete_user_group_from_domain(
                domain_id=self.domain_id, group_id=group.id))

        self.assertEqual(delete_users_resp.status_code, 204)

        # List user groups for user
        list_groups_resp = (
            self.clients[user_type].list_user_groups_for_domain(
                domain_id=self.domain_id,
                requestslib_kwargs={
                    'params': {
                        'userId': user_id
                    }
                }
            ))
        self.assertEqual(list_groups_resp.status_code, 200)
        self.assertSchema(list_groups_resp,
                          json_schema=user_groups.list_user_groups_for_domain)

        user_group_id_list = [
            user_group[const.ID] for user_group in list_groups_resp.json()[
                const.RAX_AUTH_USER_GROUPS]]

        self.assertNotIn(
            group.id,
            user_group_id_list)

    def tearDown(self):
        super(ListUsersInUserGroup, self).tearDown()
        # Deleting the user manager first, so that domain can be
        # safely deleted in the subsequent user-admin client cleanup
        self.identity_admin_client.delete_user(
            self.user_manager_client.default_headers[const.X_USER_ID])

        # This deletes the domain which automatically deletes any user groups
        # in that domain. Hence, not explicitly deleting the user groups
        self.delete_client(self.user_admin_client,
                           parent_client=self.identity_admin_client)
