# -*- coding: utf-8 -*
from nose.plugins.attrib import attr
from qe_coverage.opencafe_decorators import tags, unless_coverage

from tests.api.v2.models import factory
from tests.api.v2.schema import user_groups
from tests.api.v2.user_groups import usergroups
from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


class UserGroupsInDomain(usergroups.TestUserGroups):
    """
    Tests for user groups in a domain services.
    """
    @unless_coverage
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

    @tags('positive', 'p0', 'smoke')
    @attr(type='smoke_alpha')
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

    @tags('positive', 'p0', 'smoke')
    @attr(type='smoke_alpha')
    def test_update_user_group_in_a_domain(self):

        user_group = self.add_user_group()
        group_id = user_group[const.RAX_AUTH_USER_GROUP][const.ID]

        updated_group_name = self.generate_random_string(
            pattern=const.USER_GROUP_NAME_PATTERN)
        update_user_group_req = requests.domainUserGroup(
            group_name=updated_group_name)
        update_resp = self.user_admin_client.update_user_group(
            domain_id=self.domain_id, group_id=group_id,
            request_object=update_user_group_req)
        self.assertEqual(update_resp.status_code, 200)
        self.assertEqual(
            update_resp.json()[const.RAX_AUTH_USER_GROUP][const.NAME],
            updated_group_name)

        get_resp = self.user_admin_client.get_user_group_for_domain(
            domain_id=self.domain_id, group_id=group_id)
        self.assertEqual(
            get_resp.json()[const.RAX_AUTH_USER_GROUP][const.NAME],
            updated_group_name)

    @tags('positive', 'p0', 'smoke')
    @attr(type='smoke_alpha')
    def test_delete_user_group_in_a_domain(self):

        user_group = self.add_user_group()
        group_id = user_group[const.RAX_AUTH_USER_GROUP][const.ID]
        delete_resp = self.user_admin_client.delete_user_group_from_domain(
            domain_id=self.domain_id, group_id=group_id)
        self.assertEqual(delete_resp.status_code, 204)

        get_resp = self.user_admin_client.get_user_group_for_domain(
            domain_id=self.domain_id, group_id=group_id)
        self.assertEqual(get_resp.status_code, 404)

    @tags('positive', 'p1', 'regression')
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

    @unless_coverage
    def tearDown(self):

        # Not calling 'log_tearDown_error' as delete_client() method is
        # already wrapped with it. So, any cleanup failures will be caught.
        super(UserGroupsInDomain, self).tearDown()
        # This deletes the domain which automatically deletes any user groups
        # in that domain. Hence, not explicitly deleting the user groups.
        self.delete_client(
            self.user_admin_client,
            parent_client=self.identity_admin_client)
