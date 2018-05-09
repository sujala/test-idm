# -*- coding: utf-8 -*
import ddt
from nose.plugins.attrib import attr

from tests.api.v2.models import factory, responses
from tests.api.v2.schema import users, user_groups
from tests.api.v2.user_groups import usergroups
from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


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
    @attr(type='smoke_alpha')
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

        # Updating user by adding contact id for it. Verifying that the user
        # stays in the user group after update.
        self.verify_user_group_membership_after_user_update(
            user_type=user_type, group=group,
            user_id=self.user_manager_client.default_headers[const.X_USER_ID])

        # Verify user removal
        self.verify_user_removal_from_user_group(
            group=group, user_type=user_type)

        # Verifying that once user is removed from group, updating that user
        # won't add it to user group. Passing in user-admin user as that is
        # the user being removed from user group in the last step
        self.verify_user_group_membership_after_user_update(
            user_type=user_type, group=group,
            user_id=self.user_admin_client.default_headers[const.X_USER_ID],
            negative=True)

    @attr(type='regression')
    def test_users_in_user_group_after_user_update(self):

        group_req = factory.get_add_user_group_request(self.domain_id)
        create_group_resp = self.user_admin_client.add_user_group_to_domain(
            domain_id=self.domain_id, request_object=group_req)
        self.assertEqual(create_group_resp.status_code, 201)
        group = responses.UserGroup(create_group_resp.json())

        # adding a user to the user group
        add_user_to_grp_resp = self.user_admin_client.add_user_to_user_group(
            user_id=self.user_manager_client.default_headers[const.X_USER_ID],
            group_id=group.id, domain_id=self.domain_id
        )
        self.assertEqual(add_user_to_grp_resp.status_code, 204)

        # disable one of the user and verify that the user stays in the group
        update_user_object = requests.UserUpdate(enabled=False)
        update_user_resp = self.identity_admin_client.update_user(
            user_id=self.user_manager_client.default_headers[const.X_USER_ID],
            request_object=update_user_object)
        self.assertEqual(update_user_resp.status_code, 200)

        # List users in user group
        list_users_resp = (
            self.user_admin_client.list_users_in_user_group_for_domain(
                domain_id=self.domain_id, group_id=group.id))
        self.assertEqual(list_users_resp.status_code, 200)
        self.assertSchema(list_users_resp, json_schema=users.list_users)

        user_id_list = [user[const.ID] for user in list_users_resp.json()[
            const.USERS]]
        self.assertIn(
            self.user_manager_client.default_headers[const.X_USER_ID],
            user_id_list)

    def verify_user_group_membership_after_user_update(
            self, user_type, group, user_id, negative=False):

        contact_id = self.generate_random_string(
            pattern='fed[\-]user[\-]contact[\-][\d]{12}')
        update_user_object = requests.UserUpdate(contact_id=contact_id)
        add_contact_resp = self.identity_admin_client.update_user(
            user_id=user_id,
            request_object=update_user_object)
        self.assertEqual(add_contact_resp.status_code, 200)

        # List users in user group
        list_users_resp = (
            self.clients[user_type].list_users_in_user_group_for_domain(
                domain_id=self.domain_id, group_id=group.id))
        self.assertEqual(list_users_resp.status_code, 200)
        self.assertSchema(list_users_resp, json_schema=users.list_users)

        user_id_list = [user[const.ID] for user in list_users_resp.json()[
            const.USERS]]
        if negative:
            self.assertNotIn(user_id, user_id_list)
        else:
            self.assertIn(user_id, user_id_list)

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
    @attr(type='smoke_alpha')
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

        # Not calling 'log_tearDown_error' as delete_client() method is
        # already wrapped with it. So, any cleanup failures will be caught.
        super(ListUsersInUserGroup, self).tearDown()
        # Deleting the user manager first, so that domain can be
        # safely deleted in the subsequent user-admin client cleanup
        self.identity_admin_client.delete_user(
            self.user_manager_client.default_headers[const.X_USER_ID])

        # This deletes the domain which automatically deletes any user groups
        # in that domain. Hence, not explicitly deleting the user groups
        self.delete_client(self.user_admin_client,
                           parent_client=self.identity_admin_client)
