# -*- coding: utf-8 -*
import ddt
import json
import pytest
from qe_coverage.opencafe_decorators import tags, unless_coverage

from tests.api.utils import func_helper
from tests.api.v2 import base
from tests.api import base as api_base
from tests.api.v2.schema import roles as roles_json, users as users_json
from tests.api.v2.models import factory

from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


@ddt.ddt
class TestRoleApiCalls(base.TestBaseV2):
    """
    Test all Role Api calls
    1. add role
    2. get role
    3, delete role
    4, list roles
    5, list global roles for user
    6, add role to user
    7, delete role from user
    8, list users for role
    """
    @classmethod
    @unless_coverage
    def setUpClass(cls):
        super(TestRoleApiCalls, cls).setUpClass()
        domain_id = func_helper.generate_randomized_domain_id(
            client=cls.identity_admin_client, pattern='[\d]{7}')
        cls.user_admin_client = cls.generate_client(
            parent_client=cls.identity_admin_client,
            additional_input_data={'domain_id': domain_id})

    @unless_coverage
    def setUp(self):
        super(TestRoleApiCalls, self).setUp()
        self.role_ids = []
        self.service_ids = []
        self.user_ids = []
        self.domain_ids = []
        self.sub_user_ids = []

    def create_service(self):
        request_object = factory.get_add_service_object()
        resp = self.service_admin_client.add_service(
            request_object=request_object)
        self.assertEqual(resp.status_code, 201)
        service_id = resp.json()[const.NS_SERVICE][const.ID]
        self.service_ids.append(service_id)
        return service_id

    def create_admin_user(self):
        domain_id = func_helper.generate_randomized_domain_id(
            client=self.identity_admin_client)
        input_data = {
            'domain_id': domain_id
        }
        request_object = factory.get_add_user_request_object(
            input_data=input_data)
        resp = self.identity_admin_client.add_user(
            request_object=request_object)
        self.assertEqual(resp.status_code, 201)
        user_id = resp.json()[const.USER][const.ID]
        self.user_ids.append(user_id)
        domain_id = resp.json()[const.USER][const.RAX_AUTH_DOMAIN_ID]
        self.domain_ids.append(domain_id)
        return user_id

    def create_role(self):
        role_object = factory.get_add_role_request_object()
        resp = self.identity_admin_client.add_role(request_object=role_object)
        self.assertEqual(resp.status_code, 201)
        role_id = resp.json()[const.ROLE][const.ID]
        self.role_ids.append(role_id)
        return role_id

    def create_default_user(self):
        # create sub user to test
        sub_username = "sub_" + self.generate_random_string()
        request_object = requests.UserAdd(sub_username)
        resp = self.user_admin_client.add_user(request_object=request_object)
        sub_user_id = resp.json()[const.USER][const.ID]
        self.sub_user_ids.append(sub_user_id)
        return sub_user_id

    @unless_coverage
    @ddt.file_data('data_add_role.json')
    @pytest.mark.smoke_alpha
    def test_add_role_by_identity_admin(self, test_data):
        """Add Role, Get Role
        Using data read from json file
        """
        role_name = self.generate_random_string(
            pattern=const.ROLE_NAME_PATTERN)

        additional_input = test_data['additional_input']
        requests_object = requests.RoleAdd(role_name=role_name,
                                           **additional_input)
        resp = self.identity_admin_client.add_role(
            request_object=requests_object)

        self.assertEqual(resp.status_code, 201)
        role_id = resp.json()[const.ROLE][const.ID]
        self.role_ids.append(role_id)
        self.assertSchema(response=resp, json_schema=roles_json.add_role)

        # get role with role id
        resp = self.identity_admin_client.get_role(role_id=role_id)
        self.assertEqual(resp.status_code, 200)
        self.assertSchema(response=resp, json_schema=roles_json.add_role)

    @unless_coverage
    @ddt.file_data('data_add_role.json')
    @api_base.skip_if_no_service_admin_available
    @pytest.mark.skip_at_gate
    def test_add_role_w_new_service_by_identity_admin(self, test_data):
        """Add Role with new service, Get Role
        Using data read from json file
        """
        new_service_id = self.create_service()
        role_name = self.generate_random_string(
            pattern=const.ROLE_NAME_PATTERN)

        additional_input = test_data['additional_input']
        requests_object = requests.RoleAdd(role_name=role_name,
                                           service_id=new_service_id,
                                           **additional_input)
        resp = self.identity_admin_client.add_role(
            request_object=requests_object)

        self.assertEqual(resp.status_code, 201)
        role_id = resp.json()[const.ROLE][const.ID]
        self.role_ids.append(role_id)
        self.assertSchema(response=resp, json_schema=roles_json.add_role)

        # additional assertions
        self.assertEqual(resp.json()[const.ROLE][const.SERVICE_ID],
                         new_service_id)

    @unless_coverage
    @ddt.data('{}', '{"limit": 5}', '{"marker": 10}',
              '{"limit": 10, "marker": 5}')
    @pytest.mark.smoke_alpha
    def test_list_roles_api(self, test_data):
        """List Roles"""
        option = json.loads(test_data)
        resp = self.identity_admin_client.list_roles(option=option)
        self.assertEqual(resp.status_code, 200)
        self.assertSchema(response=resp, json_schema=roles_json.list_roles)

        if 'limit' in option:
            self.assertLessEqual(len(resp.json()[const.ROLES]),
                                 option['limit'])

    @unless_coverage
    @ddt.data('{}', '{"limit": 5}', '{"marker": 10}',
              '{"limit": 10, "marker": 5}')
    @api_base.skip_if_no_service_admin_available
    @pytest.mark.skip_at_gate
    def test_list_roles_api_w_service_admin(self, test_data):
        """List Roles with service admin"""
        option = json.loads(test_data)
        resp = self.service_admin_client.list_roles(option=option)
        self.assertEqual(resp.status_code, 200)
        self.assertSchema(response=resp, json_schema=roles_json.list_roles)

        if 'limit' in option:
            self.assertLessEqual(len(resp.json()[const.ROLES]),
                                 option['limit'])

    @tags('negative', 'p1', 'regression')
    def test_list_roles_by_name_and_negative_limit(self):
        """Verify limit must not be negative
        """
        role_name = self.generate_random_string(
            pattern=const.ROLE_NAME_PATTERN)

        requests_object = requests.RoleAdd(role_name=role_name)
        resp = self.identity_admin_client.add_role(
            request_object=requests_object)

        self.assertEqual(resp.status_code, 201)
        role_id = resp.json()[const.ROLE][const.ID]
        self.role_ids.append(role_id)

        resp = self.identity_admin_client.list_roles(
                option={'roleName': role_name, 'limit': -10})
        self.assertEqual(resp.status_code, 400)
        self.assertEqual(
            resp.json()[const.BAD_REQUEST][const.MESSAGE],
            "Limit must be non negative")

    @tags('positive', 'p1', 'smoke')
    @pytest.mark.smoke_alpha
    def test_get_roles_for_identity_admin(self):
        """
        Get role for self identity admin
        """
        user_id = self.identity_admin_client.default_headers[const.X_USER_ID]
        resp = self.identity_admin_client.list_roles_for_user(
            user_id=user_id)
        self.assertEqual(resp.status_code, 200)
        self.assertSchema(response=resp, json_schema=roles_json.list_roles)

    @tags('positive', 'p0', 'smoke')
    @pytest.mark.smoke_alpha
    def test_add_and_delete_role_from_user(self):
        """
        Test Add role to User and Delete role from user
         create a new user and a new role
         add role to user.
        :return:
        """
        role_id = self.create_role()
        user_id = self.create_admin_user()

        # add role to user
        resp = self.identity_admin_client.add_role_to_user(
            role_id=role_id, user_id=user_id)
        self.assertEqual(resp.status_code, 200)

        # verify get roles for user
        resp = self.identity_admin_client.list_roles_for_user(
            user_id=user_id
        )
        self.assertEqual(resp.status_code, 200)
        self.assertIn(role_id, str(resp.json()[const.ROLES]))

        # Delete role from user
        resp = self.identity_admin_client.delete_role_from_user(
            role_id=role_id, user_id=user_id
        )
        self.assertEqual(resp.status_code, 204)

        # verify role is removed from list
        resp = self.identity_admin_client.list_roles_for_user(
            user_id=user_id
        )
        self.assertEqual(resp.status_code, 200)
        self.assertNotIn(role_id, str(resp.json()[const.ROLES]))

    @tags('positive', 'p1', 'regression')
    def test_add_and_delete_role_from_default_user(self):
        """
        Test Add role to Default User and Delete role from user
        """
        role_name = self.generate_random_string(
            pattern=const.ROLE_NAME_PATTERN)

        # create role with propagate=false
        requests_object = requests.RoleAdd(
            role_name=role_name,
            administrator_role=const.USER_MANAGE_ROLE_NAME,
            propagate=False)
        resp = self.identity_admin_client.add_role(
            request_object=requests_object)

        self.assertEqual(resp.status_code, 201)
        role_id = resp.json()[const.ROLE][const.ID]

        # create default user
        user_id = self.create_default_user()

        # add role to user
        resp = self.identity_admin_client.add_role_to_user(
            role_id=role_id, user_id=user_id)
        self.assertEqual(resp.status_code, 200)

        # verify get roles for user
        resp = self.identity_admin_client.list_roles_for_user(
            user_id=user_id
        )
        self.assertEqual(resp.status_code, 200)
        self.assertIn(role_id, str(resp.json()[const.ROLES]))

        # Delete role from user
        resp = self.identity_admin_client.delete_role_from_user(
            role_id=role_id, user_id=user_id
        )
        self.assertEqual(resp.status_code, 204)

        # verify role is removed from list
        resp = self.identity_admin_client.list_roles_for_user(
            user_id=user_id
        )
        self.assertEqual(resp.status_code, 200)
        self.assertNotIn(role_id, str(resp.json()[const.ROLES]))

    @tags('positive', 'p0', 'smoke')
    @pytest.mark.smoke_alpha
    def test_list_users_for_role(self):
        """
        Test list users for specific role
          create a new role, a new user, add new role to user
          list users for role verify the new user in return list
        :return:
        """
        # create new role
        role_id = self.create_role()

        # create 3 new users add role to each
        new_user_ids = []
        for i in range(3):
            user_id = self.create_admin_user()
            new_user_ids.append(user_id)
            resp = self.identity_admin_client.add_role_to_user(
                role_id=role_id, user_id=user_id)
            self.assertEqual(resp.status_code, 200)

        # List users for role {role_id}
        resp = self.identity_admin_client.get_users_for_role(role_id=role_id)
        self.assertEqual(resp.status_code, 200)
        # check schema using list user schema
        self.assertSchema(response=resp, json_schema=users_json.list_users)
        # check user in the list
        for user in new_user_ids:
            self.assertIn(user, str(resp.json()[const.USERS]))

    @tags('negative', 'p1', 'regression')
    @pytest.mark.skip_at_gate
    def test_delete_identity_classification_role_from_user(self):

        user_id = self.create_admin_user()
        delete_role_resp = self.identity_admin_client.delete_role_from_user(
            user_id=user_id, role_id=const.USER_ADMIN_ROLE_ID)
        self.assertEqual(delete_role_resp.status_code, 403)
        self.assertEqual(
            delete_role_resp.json()[const.FORBIDDEN][const.MESSAGE],
            "Cannot delete identity user-type roles from a user.")

    @tags('negative', 'p1', 'regression')
    @pytest.mark.skip_at_gate
    def test_delete_identity_classification_role(self):

        delete_role_resp = self.identity_admin_client.delete_role(
            role_id=const.USER_DEFAULT_ROLE_ID)
        self.assertEqual(delete_role_resp.status_code, 403)
        self.assertEqual(
            delete_role_resp.json()[const.FORBIDDEN][const.MESSAGE],
            "Identity user type roles cannot be deleted")

    @base.base.log_tearDown_error
    @unless_coverage
    def tearDown(self):
        for id_ in self.sub_user_ids:
            resp = self.identity_admin_client.delete_user(user_id=id_)
            self.assertEqual(
                resp.status_code, 204,
                msg='User with ID {0} failed to delete'.format(id_))
        for id_ in self.user_ids:
            resp = self.identity_admin_client.delete_user(user_id=id_)
            self.assertEqual(
                resp.status_code, 204,
                msg='User with ID {0} failed to delete'.format(id_))
        for id_ in self.role_ids:
            resp = self.identity_admin_client.delete_role(role_id=id_)
            self.assertEqual(
                resp.status_code, 204,
                msg='Role with ID {0} failed to delete'.format(id_))
        if self.test_config.run_service_admin_tests:
            for id_ in self.service_ids:
                self.service_admin_client.delete_service(service_id=id_)
        for id_ in self.domain_ids:
            disable_domain_req = requests.Domain(enabled=False)
            self.identity_admin_client.update_domain(
                domain_id=id_, request_object=disable_domain_req)

            resp = self.identity_admin_client.delete_domain(domain_id=id_)
            self.assertEqual(
                resp.status_code, 204,
                msg='Domain with ID {0} failed to delete'.format(
                    id_))
        super(TestRoleApiCalls, self).tearDown()

    @classmethod
    @unless_coverage
    def tearDownClass(cls):
        super(TestRoleApiCalls, cls).tearDownClass()
