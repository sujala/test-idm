# -*- coding: utf-8 -*
import ddt

from tests.api.v2 import base
from tests.api import constants as const
from tests.api.v2.models import factory

"""
Test verify:
    1. Allow identity product roles (e.g. those prefixed with "identity:") to
        be assigned to users on a tenant
    1.1 This excludes identity user-classification roles
        (identity:service-admin, identity:admin, identity:user-admin,
        identity:default)
    1.2 This also excludes the feature role identity:user-manage

    2. When an attempt is made to assign one of the excluded roles to a user
        on a tenant, the existing functionality of returning a 403 must be
        preserved.
"""


@ddt.ddt
class TestAddIdentityProdRoleToUserOnTenant(base.TestBaseV2):

    @classmethod
    def setUpClass(cls):
        super(TestAddIdentityProdRoleToUserOnTenant, cls).setUpClass()
        cls.user_admin_client = cls.generate_client(
            parent_client=cls.identity_admin_client,
            additional_input_data={'domain_id': const.DOMAIN_API_TEST})

    def setUp(self):
        super(TestAddIdentityProdRoleToUserOnTenant, self).setUp()
        self.role_ids = []
        self.user_ids = []
        self.tenant_ids = []

    def create_user(self, parent_client):
        request_object = factory.get_add_user_request_object()
        resp = parent_client.add_user(
            request_object=request_object)
        self.assertEqual(resp.status_code, 201)
        user_id = resp.json()[const.USER][const.ID]
        self.user_ids.append(user_id)
        return user_id, resp

    def create_tenant(self):
        tenant_types = ['type1']
        tenant_name = self.generate_random_string(
            pattern=const.NUMBERS_PATTERN)
        tenant_req = factory.get_add_tenant_request_object(
            tenant_name=tenant_name, tenant_types=tenant_types)
        resp = self.identity_admin_client.add_tenant(tenant=tenant_req)
        self.assertEqual(resp.status_code, 201)
        tenant_id = resp.json()[const.TENANT][const.ID]
        self.tenant_ids.append(tenant_id)
        return tenant_id, resp

    def create_role(self):
        role_name = self.generate_random_string(
            pattern=const.IDENTITY_PRODUCT_ROLE_NAME_PATTERN)
        role_object = factory.get_add_role_request_object(role_name=role_name)
        resp = self.identity_admin_client.add_role(request_object=role_object)
        self.assertEqual(resp.status_code, 201)
        role_id = resp.json()[const.ROLE][const.ID]
        self.role_ids.append(role_id)
        return role_id

    def get_role_by_name(self, role_name):
        """return a role id"""
        role_id = None
        option = {'roleName': role_name}
        resp = self.service_admin_client.list_roles(option=option)
        self.assertEqual(resp.status_code, 200)
        if resp.json()[const.ROLES]:
            role_id = resp.json()[const.ROLES][0][const.ID]
        return role_id

    @ddt.data('identity-admin', 'user-admin', 'user-default')
    def test_add_identity_product_role_to_user_admin_on_tenant(self, user):
        """
        Verify allow to add identity product roles to user on tenant
        Test step:
            - create user admin
            - create tenant
            - get identity Service
            - create identity product role
            - add role to user on tenant
            - auth and verify role is added
            - remove role from user for tenant
            - auth and verify role is removed
        :return:
        """
        if user is 'identity-admin':
            user_id = self.identity_admin_client.default_headers[
                const.X_USER_ID]
            user_name = self.identity_config.identity_admin_user_name
            password = self.identity_config.identity_admin_password
        elif user is 'user-default':
            # create sub user
            user_id, user_resp = self.create_user(
                parent_client=self.user_admin_client)
            user_name = user_resp.json()[const.USER][const.USERNAME]
            password = user_resp.json()[const.USER][const.NS_PASSWORD]
        else:
            user_id, user_resp = self.create_user(
                parent_client=self.identity_admin_client)
            user_name = user_resp.json()[const.USER][const.USERNAME]
            password = user_resp.json()[const.USER][const.NS_PASSWORD]

        # create tenant
        tenant_id, tenant_resp = self.create_tenant()

        # create identity product role
        role_id = self.create_role()

        # add role to user on tenant
        add_resp = self.identity_admin_client.add_role_to_user_for_tenant(
            tenant_id=tenant_id, user_id=user_id, role_id=role_id
        )
        self.assertEqual(add_resp.status_code, 200)

        # verify role added
        auth_resp = self.identity_admin_client.get_auth_token(
            user=user_name, password=password)
        self.assertEqual(auth_resp.status_code, 200)
        self.assertIn(role_id, str(auth_resp.json()[const.ACCESS][const.USER][
            const.ROLES]))

        # delete role from tenant
        del_resp = self.identity_admin_client.delete_role_from_user_for_tenant(
            tenant_id=tenant_id, user_id=user_id, role_id=role_id
        )
        self.assertEqual(del_resp.status_code, 204)

        # verify role is removed
        auth_resp = self.identity_admin_client.get_auth_token(
            user=user_name, password=password)
        self.assertEqual(auth_resp.status_code, 200)
        self.assertNotIn(role_id, str(auth_resp.json()[const.ACCESS][
                                          const.USER][const.ROLES]))

    @ddt.data('identity-admin', 'user-admin', 'user-default')
    def test_add_identity_tenant_access_role(self, user):
        """
        Verify able to add 'identity:tenant-access' role to user for tenant
        :param user:
        :return:
        """
        if user is 'identity-admin':
            user_id = self.identity_admin_client.default_headers[
                const.X_USER_ID]
            user_name = self.identity_config.identity_admin_user_name
            password = self.identity_config.identity_admin_password
        elif user is 'user-default':
            # create sub user
            user_id, user_resp = self.create_user(
                parent_client=self.user_admin_client)
            user_name = user_resp.json()[const.USER][const.USERNAME]
            password = user_resp.json()[const.USER][const.NS_PASSWORD]
        else:
            user_id, user_resp = self.create_user(
                parent_client=self.identity_admin_client)
            user_name = user_resp.json()[const.USER][const.USERNAME]
            password = user_resp.json()[const.USER][const.NS_PASSWORD]

        # create tenant
        tenant_id, tenant_resp = self.create_tenant()

        # get tenant-access role
        role_id = self.get_role_by_name(
            role_name=const.TENANT_ACCESS_ROLE_NAME)

        if role_id:
            # add role to user on tenant
            add_resp = self.service_admin_client.add_role_to_user_for_tenant(
                tenant_id=tenant_id, user_id=user_id, role_id=role_id
            )
            self.assertEqual(add_resp.status_code, 200)

            # verify role added
            auth_resp = self.identity_admin_client.get_auth_token(
                user=user_name, password=password)
            self.assertEqual(auth_resp.status_code, 200)
            self.assertIn(role_id, str(auth_resp.json()[const.ACCESS][
                                           const.USER][const.ROLES]))

            # delete role from tenant
            del_resp = (
                self.identity_admin_client.delete_role_from_user_for_tenant(
                    tenant_id=tenant_id, user_id=user_id, role_id=role_id
                )
            )
            self.assertEqual(del_resp.status_code, 204)

            # verify role is removed
            auth_resp = self.identity_admin_client.get_auth_token(
                user=user_name, password=password)
            self.assertEqual(auth_resp.status_code, 200)
            self.assertNotIn(role_id, str(auth_resp.json()[const.ACCESS][
                                              const.USER][const.ROLES]))

    @ddt.data('identity:default', 'identity:admin', 'identity:user-admin',
              'identity:service-admin', 'identity:user-manager')
    def test_add_identity_user_classification_and_feature_roles(self,
                                                                role_name):
        """
        Verify not allow to add user-classification roles
        :param role_name:
        :return: 403
        """
        if not self.test_config.run_service_admin_tests:
            self.skipTest('Skipping Service Admin Tests per config value')
        # create user admin
        user_id, user_resp = self.create_user(
            parent_client=self.identity_admin_client)

        # create tenant
        tenant_id, tenant_resp = self.create_tenant()

        # get role id
        if role_name == const.SERVICE_ADMIN_ROLE_NAME:
            role_id = const.SERVICE_ADMIN_ROLE_ID
        elif role_name == const.USER_MANAGER_ROLE_NAME:
            role_id = const.USER_MANAGER_ROLE_ID
        else:
            role_id = self.get_role_by_name(role_name=role_name)

        # add role to user on tenant
        add_resp = self.service_admin_client.add_role_to_user_for_tenant(
            tenant_id=tenant_id, user_id=user_id, role_id=role_id
        )
        self.assertEqual(add_resp.status_code, 403)

    def tearDown(self):
        # clean up resources
        for id_ in self.user_ids:
            self.identity_admin_client.delete_user(user_id=id_)
        for id_ in self.tenant_ids:
            self.identity_admin_client.delete_tenant(tenant_id=id_)
        for id_ in self.role_ids:
            self.identity_admin_client.delete_role(role_id=id_)

    @classmethod
    def tearDownClass(cls):
        cls.delete_client(client=cls.user_admin_client)
