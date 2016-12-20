# -*- coding: utf-8 -*

"""
1. A user must be implicitly granted the "identity:tenant-access" role to all
    tenants within the user's domain without the user having to be explicitly
    granted the role
1.1 The exception to this rule is the default domain. A user belonging to the
    default domain (identified by reloadable prop tenant.domainId.default)
    must not receive implicit access to any tenants associated with the default
    domain

2. The relationship must be demonstrated through the following service calls:
2.1 v2.0 List Tenants - Must return all tenants within the user's domain
2.2 v2.0 List Users on Tenant - The user is listed for every tenant within
    domain
2.3 v2.0 List Roles for User on Tenant - The identity:tenant-access role is
    returned for the user on every tenant within user's domain
2.4 v2.0 Authenticate/Validate - The user has the identity:tenant-access role
    on every tenant within domain and receive appropriate service catalog
    entries for those tenants
2.5 v2.0 Auth w/ Tenant - The user can specify any tenant within domain and
    authenticate against it
2.6 v2.0 List Endpoints for Token - The user must receive appropriate service
    catalog entries for all tenants within the user's domain

3. The implicit assignment must be controlled via a feature flag. When
    disabled, the default, users are NOT implicitly granted the
    identity:tenant-access roles on all tenants within the user's domain
"""
import collections

from tests.api.v2 import base
from tests.api.v2.models import factory

from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


class TestUserImplicitlyGrantedTenantAccessRole(base.TestBaseV2):
    """
    Test implicitly grant indentity:tenant-access role
    """
    @classmethod
    def setUpClass(cls):
        super(TestUserImplicitlyGrantedTenantAccessRole, cls).setUpClass()
        # skip if not service admin tests
        if not cls.test_config.run_service_admin_tests:
            cls.skipTest('Skipping Service Admin Tests per config value')
        cls.feature_flag_value, cls.feature_flag_default_value = (
            cls.get_feature_flag_value_and_default_value(flag_name=(
                const.FEATURE_AUTO_ASSIGN_ROLE_ON_DOMAIN_TENANTS)))

        # get auto assign role name set
        cls.auto_assign_role_name, cls.auto_assign_role_name_default = (
            cls.get_feature_flag_value_and_default_value(flag_name=(
                const.AUTO_ASSIGN_ROLE_ON_DOMAIN_TENANTS_ROLE_NAME)))

        # get tenant default domain
        cls.tenant_default_domain_value, _ = (
            cls.get_feature_flag_value_and_default_value(flag_name=(
                const.TENANT_DEFAULT_DOMAIN)))

        # get tenant access role id
        cls.tenant_access_role_id = cls.get_role_by_name(
            role_name=const.TENANT_ACCESS_ROLE_NAME)

    @classmethod
    def get_feature_flag_value_and_default_value(self, flag_name):
        feature_flag_resp = (
            self.devops_client.get_devops_properties(flag_name))
        feature_flag_value = feature_flag_resp.json()[
            const.IDM_RELOADABLE_PROPERTIES][0][const.VALUE]
        feature_flag_default_value = feature_flag_resp.json()[
            const.IDM_RELOADABLE_PROPERTIES][0][const.DEFAULT_VALUE]
        return feature_flag_value, feature_flag_default_value

    @classmethod
    def get_role_by_name(self, role_name):
        # get tenant access role id
        get_role_resp = self.service_admin_client.get_role_by_name(
            role_name=role_name)
        tenant_access_role_id = get_role_resp.json()[const.ROLES][0][
            const.ID]
        return tenant_access_role_id

    def setUp(self):
        super(TestUserImplicitlyGrantedTenantAccessRole, self).setUp()
        self.user_ids = []
        self.tenant_ids = []
        self.domain_ids = []

    def create_user_one_call(self):
        domain_id = self.generate_random_string(pattern=const.NUMBERS_PATTERN)
        add_obj = factory.get_add_user_request_object_pull(domain_id=domain_id)
        resp = self.identity_admin_client.add_user(request_object=add_obj)
        self.assertEqual(resp.status_code, 201)
        user_id = resp.json()[const.USER][const.ID]
        self.user_ids.append(user_id)
        self.domain_ids.append(domain_id)
        self.tenant_ids.append(domain_id)
        return user_id, resp

    def create_user(self):
        """regular"""
        user_name = self.generate_random_string(
            pattern=const.USER_NAME_PATTERN)
        domain_id = self.generate_random_string(pattern=const.NUMBERS_PATTERN)
        request_object = requests.UserAdd(user_name=user_name,
                                          domain_id=domain_id)
        resp = self.identity_admin_client.add_user(
            request_object=request_object)
        self.assertEqual(resp.status_code, 201)
        user_id = resp.json()[const.USER][const.ID]
        self.user_ids.append(user_id)
        return user_id, resp

    def create_tenant(self, domain_id=None):
        tenant_name = self.generate_random_string(
            pattern=const.NUMBERS_PATTERN)
        tenant_req = factory.get_add_tenant_request_object(
            tenant_name=tenant_name, domain_id=domain_id)
        resp = self.identity_admin_client.add_tenant(tenant=tenant_req)
        self.assertEqual(resp.status_code, 201)
        tenant_id = resp.json()[const.TENANT][const.ID]
        self.tenant_ids.append(tenant_id)
        return tenant_id, resp

    def verify_user_belong_to_default_domain(self, domain_id, tenant_id):
        """
        If tenant created with default domain,
        Verify user belong to default domain not implicitly grant tenant access
        role (AC: 1.1 coverage)
        :param domain_id:
        :return:
        """
        # list tenant in default domain
        tenants_resp = self.identity_admin_client.get_tenants_in_domain(
            domain_id=domain_id)
        self.assertEqual(tenants_resp.status_code, 200)
        self.assertIn(domain_id, str(tenants_resp.json()[const.TENANTS]))

        # list users in deault domain
        users_default_domain_resp = (
            self.identity_admin_client.list_users_in_domain(
                domain_id=domain_id))
        self.assertEqual(users_default_domain_resp.status_code, 200)
        pick_user_id = users_default_domain_resp.json()[const.USERS][0][
            const.ID]

        # get api key for user
        api_resp = self.identity_admin_client.get_api_key(user_id=pick_user_id)
        self.assertEqual(api_resp.status_code, 200)
        pick_username = api_resp.json()[const.NS_API_KEY_CREDENTIALS][
            const.USERNAME]
        api_key = api_resp.json()[const.NS_API_KEY_CREDENTIALS][const.API_KEY]

        # auth with apikey
        auth_obj = requests.AuthenticateWithApiKey(user_name=pick_username,
                                                   api_key=api_key)
        api_auth_resp = self.identity_admin_client.get_auth_token(
            request_object=auth_obj)
        self.assertEqual(api_auth_resp.status_code, 200)
        user_token = api_auth_resp.json()[const.ACCESS][const.TOKEN][const.ID]

        # check tenant access role not include in resp
        self.assertNotIn(const.TENANT_ACCESS_ROLE_NAME,
                         str(api_auth_resp.json()[
                                 const.ACCESS][const.USER][const.ROLES]))

        # validate token
        validate_resp = self.identity_admin_client.validate_token(
            token_id=user_token)
        self.assertEqual(validate_resp.status_code, 200)

        # check tenant access role not include in resp
        self.assertNotIn(const.TENANT_ACCESS_ROLE_NAME,
                         str(validate_resp.json()[
                                 const.ACCESS][const.USER][const.ROLES]))

        # List users for tenant
        users_resp = self.service_admin_client.list_users_for_tenant(
            tenant_id=tenant_id)
        self.assertEqual(users_resp.status_code, 200)

        # verify user not list
        self.assertNotIn(pick_user_id, str(users_resp.json()[const.USERS]))

        # list role for user in tenant
        roles_resp = (
            self.service_admin_client.list_roles_for_user_on_tenant(
                tenant_id=tenant_id, user_id=pick_user_id)
        )
        self.assertEqual(roles_resp.status_code, 200)

        # verify tenant access role not in role list
        self.assertNotIn(const.TENANT_ACCESS_ROLE_NAME, roles_resp.json()[
            const.ROLES])

    def verify_endpoint_for_tenant_from_service_catalog(self, auth_resp,
                                                        tenant_id):
        """Verify if endpoints are not repeated/duplicated for each tenant
            for tenant since it explicitly add tenant access role from auth
            resp
        """
        # get endpoint list for tenant
        endpoints = []
        service_catalog = auth_resp.json()[const.ACCESS][const.SERVICE_CATALOG]
        for endpoint in service_catalog:
            if endpoint[const.ENDPOINTS][0][const.TENANT_ID] == tenant_id:
                endpoints.append(endpoint[const.NAME])
        # check for duplicate in list of endpoint name for tenant
        for item, count in collections.Counter(endpoints).items():
            self.assertFalse(count > 1, "Duplicatate item {0}".format(item))

    def verify_auth_w_password(self, user_name, password):
        """
        Authenticate with user name and password
        Verify tenant access role with feature flag true/false
        :param user_name:
        :param password:
        :return: mosso_tenant, nast_tenant, and user_token for next validation
        """
        auth_obj = requests.AuthenticateWithPassword(user_name=user_name,
                                                     password=password)
        auth_resp = self.identity_admin_client.get_auth_token(auth_obj)
        self.assertEqual(auth_resp.status_code, 200)
        user_token = auth_resp.json()[const.ACCESS][const.TOKEN][const.ID]

        # check if tenants return in auth call
        mosso_tenant = None
        nast_tenant = None
        if const.TENANT in str(auth_resp.json()[const.ACCESS][const.TOKEN]):
            mosso_tenant = auth_resp.json()[const.ACCESS][const.TOKEN][
                const.TENANT][const.ID]
            for item in auth_resp.json()[const.ACCESS][const.USER][
                             const.ROLES]:
                if item[const.NAME] == const.OBJECT_STORE_ROLE_NAME:
                    nast_tenant = item[const.TENANT_ID]

        if (self.feature_flag_value | self.feature_flag_default_value) and (
                    self.auto_assign_role_name == (
                        const.TENANT_ACCESS_ROLE_NAME)):
            # verify auth resp include tenant access role
            self.assertIn(const.TENANT_ACCESS_ROLE_NAME,
                          str(auth_resp.json()[
                                  const.ACCESS][const.USER][const.ROLES]))
        else:
            # verify auth response not contain tenant access role
            self.assertNotIn(const.TENANT_ACCESS_ROLE_NAME,
                             str(auth_resp.json()[
                                     const.ACCESS][const.USER][const.ROLES]))

        if mosso_tenant:
            # verify endpoint no duplicate for mosso tanant
            self.verify_endpoint_for_tenant_from_service_catalog(
                auth_resp=auth_resp, tenant_id=mosso_tenant)

        if nast_tenant:
            # verify endpoint no duplicate for mosso tanant
            self.verify_endpoint_for_tenant_from_service_catalog(
                auth_resp=auth_resp, tenant_id=nast_tenant)

        return mosso_tenant, nast_tenant, user_token

    def verify_validate_token(self, token):
        """
        Validate user token
        verify tenant access role with feature flag true/false
        :param token:
        :return: None
        """
        validate_resp = self.identity_admin_client.validate_token(
            token_id=token)
        self.assertEqual(validate_resp.status_code, 200)
        # when feature flag set to true
        if (self.feature_flag_value | self.feature_flag_default_value) and (
            self.auto_assign_role_name == (
                        const.TENANT_ACCESS_ROLE_NAME)):
            # verify validate resp include tenant access role
            self.assertIn(const.TENANT_ACCESS_ROLE_NAME,
                          str(validate_resp.json()[
                                  const.ACCESS][const.USER][const.ROLES]))
        else:
            # verify validate resp not include tenant access role
            self.assertNotIn(const.TENANT_ACCESS_ROLE_NAME,
                             str(validate_resp.json()[
                                  const.ACCESS][const.USER][const.ROLES]))

    def verify_auth_w_tenant_and_token(self, tenant_id, token):
        """
        Auth with tenant (mosso or nast) and token
        Verify tenant access with feature flag true/false
        :param tenant_id:
        :param token:
        :return: None
        """
        auth_obj = requests.AuthenticateAsTenantWithToken(token_id=token,
                                                          tenant_id=tenant_id)
        auth_tenant_n_token_resp = (
            self.identity_admin_client.get_auth_token(request_object=auth_obj)
        )
        self.assertEqual(auth_tenant_n_token_resp.status_code, 200)

        if (self.feature_flag_value | self.feature_flag_default_value) and (
            self.auto_assign_role_name == (
                        const.TENANT_ACCESS_ROLE_NAME)):
            self.assertIn(const.TENANT_ACCESS_ROLE_NAME,
                          str(auth_tenant_n_token_resp.json()[
                                  const.ACCESS][const.USER][const.ROLES]))
        else:
            self.assertNotIn(const.TENANT_ACCESS_ROLE_NAME,
                             str(auth_tenant_n_token_resp.json()[
                                     const.ACCESS][const.USER][const.ROLES]))

    def verify_auth_w_new_tenant_token(self, tenant_id, token):
        """
        Auth with new tenant (not default) and token
        Verify tenant access with feature flag true/false
        :param tenant_id:
        :param token:
        :return: None
        """
        auth_obj = requests.AuthenticateAsTenantWithToken(tenant_id=tenant_id,
                                                          token_id=token)
        auth_tenant_n_token_resp = (
            self.identity_admin_client.get_auth_token(request_object=auth_obj)
        )

        if (self.feature_flag_value | self.feature_flag_default_value) and (
            self.auto_assign_role_name == (
                        const.TENANT_ACCESS_ROLE_NAME)):
            self.assertIn(const.TENANT_ACCESS_ROLE_NAME,
                          str(auth_tenant_n_token_resp.json()[
                                  const.ACCESS][const.USER][const.ROLES]))
            self.assertEqual(auth_tenant_n_token_resp.status_code, 200)
        else:
            self.assertEqual(auth_tenant_n_token_resp.status_code, 401)

    def verify_auth_w_username_password_n_tenant(self, username, password,
                                                 tenant_id):
        """
        Authenticate with username password and tenant (mosso or nast)
        verify tenant access with feature flag true/false
        :param username:
        :param password:
        :param tenant_id:
        :return: None
        """
        auth_obj = requests.AuthenticateWithPassword(
            user_name=username, password=password, tenant_id=tenant_id)
        auth_username_pwd_tenant_resp = (
            self.identity_admin_client.get_auth_token(request_object=auth_obj)
        )
        self.assertEqual(auth_username_pwd_tenant_resp.status_code, 200)

        if (self.feature_flag_value | self.feature_flag_default_value) and (
                    self.auto_assign_role_name == (
                        const.TENANT_ACCESS_ROLE_NAME)):
            self.assertIn(const.TENANT_ACCESS_ROLE_NAME,
                          str(auth_username_pwd_tenant_resp.json()[
                                  const.ACCESS][const.USER][const.ROLES]))
        else:
            self.assertNotIn(const.TENANT_ACCESS_ROLE_NAME,
                             str(auth_username_pwd_tenant_resp.json()[
                                  const.ACCESS][const.USER][const.ROLES]))

    def verify_auth_w_username_password_n_new_tenant(self, username, password,
                                                     tenant_id):
        """
        Authenticate with username password and tenant (not mosso or nast)
        verify tenant access with feature flag true/false
        :param username:
        :param password:
        :param tenant_id:
        :return: None
        """
        auth_obj = requests.AuthenticateWithPassword(
            user_name=username, password=password, tenant_id=tenant_id)
        auth_username_pwd_tenant_resp = (
            self.identity_admin_client.get_auth_token(request_object=auth_obj)
        )

        if (self.feature_flag_value | self.feature_flag_default_value) and (
                    self.auto_assign_role_name == (
                        const.TENANT_ACCESS_ROLE_NAME)):
            self.assertIn(const.TENANT_ACCESS_ROLE_NAME,
                          str(auth_username_pwd_tenant_resp.json()[
                                  const.ACCESS][const.USER][const.ROLES]))
            self.assertEqual(auth_username_pwd_tenant_resp.status_code, 200)
        else:
            self.assertEqual(auth_username_pwd_tenant_resp.status_code, 401)

    def verify_auth_w_username_password_n_invalid_tenant(self, username,
                                                         password, tenant_id):
        """
        Auth with username password with invalid or delete tenant
        :param username:
        :param password:
        :param tenant_id:
        :return:
        """
        auth_obj = requests.AuthenticateWithPassword(
            user_name=username, password=password, tenant_id=tenant_id)
        auth_username_pwd_tenant_resp = (
            self.identity_admin_client.get_auth_token(request_object=auth_obj)
        )
        self.assertEqual(auth_username_pwd_tenant_resp.status_code, 401)

    def verify_list_endpoint_for_user(self, token, mosso_tenant=None,
                                      nast_tenant=None, new_tenant=None):
        """Verify endpoint list for The user must receive appropriate service
        catalog entries for all tenants within the user's domain.
        """
        endpoints_resp = (
            self.identity_admin_client.list_endpoints_for_token(
                token=token)
        )
        self.assertEqual(endpoints_resp.status_code, 200)

        ep_list_for_mosso = const.LIST_ENDPOINT_NAMES_FOR_MOSSO_TENANT
        ep_list_for_nast = const.LIST_ENDPOINT_NAMES_FOR_NAST_TENANT

        if mosso_tenant:
            mosso_endpoints = []
            # get all endpoints for mosso tehant
            for endpoint in endpoints_resp.json()[const.ENDPOINTS]:
                if endpoint[const.TENANT_ID] == mosso_tenant:
                    mosso_endpoints.append(endpoint)

            for name in ep_list_for_mosso:
                self.assertIn(name, str(mosso_endpoints))

        if nast_tenant:
            nast_endpoints = []
            # get all endpoints for nast tehant
            for endpoint in endpoints_resp.json()[const.ENDPOINTS]:
                if endpoint[const.TENANT_ID] == nast_tenant:
                    nast_endpoints.append(endpoint)

            for name in ep_list_for_nast:
                self.assertIn(name, str(nast_endpoints))

        if new_tenant:
            new_tenant_endpoints = []
            # get endpoints associated with new tenants
            for endpoint in endpoints_resp.json()[const.ENDPOINTS]:
                if endpoint[const.TENANT_ID] == new_tenant:
                    new_tenant_endpoints.append(endpoint)

            # sine new tenant doesn't have endpoint associated or role with
            # endpoint mapping to
            self.assertEqual(new_tenant_endpoints, [])

    def verify_list_tenant(self, token, mosso_tenant=None, nast_tenant=None,
                           new_tenant=None):
        """
        List tenants
        Check user with feature flag true/false
        :param tenant_id:
        :return: None
        """
        client = self.generate_client(token=token)
        tenant_resp = client.list_tenants()
        self.assertEqual(tenant_resp.status_code, 200)
        # verify return all tenants within the user's domain
        if mosso_tenant:
            found_mosso_tenant = False
            for tenant in tenant_resp.json()[const.TENANTS]:
                if tenant[const.ID] == mosso_tenant:
                    found_mosso_tenant = True
            self.assertTrue(found_mosso_tenant)
        if nast_tenant:
            found_nast_tenant = False
            for tenant in tenant_resp.json()[const.TENANTS]:
                if tenant[const.ID] == nast_tenant:
                    found_nast_tenant = True
            self.assertTrue(found_nast_tenant)
        if new_tenant:
            if (self.feature_flag_value |
                    self.feature_flag_default_value) and (
                        self.auto_assign_role_name == (
                            const.TENANT_ACCESS_ROLE_NAME)
            ):
                self.assertIn(new_tenant,
                              str(tenant_resp.json()[const.TENANTS]))
            else:
                self.assertNotIn(new_tenant,
                                 str(tenant_resp.json()[const.TENANTS]))

    def verify_list_users_for_tenant(self, token, tenant_id, user_id,
                                     resp_code):
        """
        List users for tenants
        Check response with feature flag true/false
        also depend on token response will different
        :param token:
        :param tenant_id:
        :return:
        """
        client = self.generate_client(token=token)

        users_tenant_resp = client.list_users_for_tenant(
            tenant_id=tenant_id)

        if (self.feature_flag_value | self.feature_flag_default_value) and (
                    self.auto_assign_role_name == (
                        const.TENANT_ACCESS_ROLE_NAME)):
            self.assertEqual(users_tenant_resp.status_code, 200)
            self.assertIn(user_id,
                          str(users_tenant_resp.json()[const.USERS]))
        else:
            if resp_code == 200:
                self.assertEqual(users_tenant_resp.status_code, resp_code)
                self.assertIn(user_id,
                              str(users_tenant_resp.json()[const.USERS]))
            else:
                self.assertEqual(users_tenant_resp.status_code, resp_code)

    def verify_list_users_for_tenant_w_identity_admin(self, tenant_id,
                                                      user_id):
        """
        List users for new tenant with identity admin
        Check response with feature flag true/false
        :param tenant_id:
        :param user_id:
        :return:
        """
        users_tenant_resp = self.identity_admin_client.list_users_for_tenant(
            tenant_id=tenant_id)
        self.assertEqual(users_tenant_resp.status_code, 200)
        if (self.feature_flag_value | self.feature_flag_default_value) and (
                    self.auto_assign_role_name == (
                        const.TENANT_ACCESS_ROLE_NAME)):
            self.assertIn(user_id, str(users_tenant_resp.json()[const.USERS]))
        else:
            self.assertNotIn(user_id, str(users_tenant_resp.json()[
                                             const.USERS]))

    def verify_list_users_for_tenant_w_identity_admin_post_del(self, tenant_id,
                                                               user_id):
        """
        List users for new tenant with identity admin
        Check response with feature flag true/false
        :param tenant_id:
        :param user_id:
        :return:
        """
        users_new_tenant_resp_post = (
            self.identity_admin_client.list_users_for_tenant(
                tenant_id=tenant_id)
        )
        self.assertEqual(users_new_tenant_resp_post.status_code, 200)
        self.assertNotIn(user_id, str(users_new_tenant_resp_post.json()[
                                          const.USERS]))

    def verify_list_roles_for_user_on_tenant(self, tenant_id, user_id):
        """
        List roles for user on tenant
        Check response with feature flag true/false
        :param tenant_id:
        :param user_id:
        :return:
        """
        roles_tenant_resp = (
            self.service_admin_client.list_roles_for_user_on_tenant(
                tenant_id=tenant_id, user_id=user_id)
        )
        self.assertEqual(roles_tenant_resp.status_code, 200)

        if (self.feature_flag_value | self.feature_flag_default_value) and (
                    self.auto_assign_role_name == (
                        const.TENANT_ACCESS_ROLE_NAME)):
            # verify List roles for user on tenant resp include tenant access
            # since contract list roles only contain role ids
            # get tenant access role id to verify
            self.assertIn(self.tenant_access_role_id,
                          str(roles_tenant_resp.json()[const.ROLES]))
        else:
            self.assertNotIn(self.tenant_access_role_id,
                             str(roles_tenant_resp.json()[const.ROLES]))

    def verify_list_roles_for_user_on_tenant_post_del(self, tenant_id,
                                                      user_id):
        """
        List roles for user on tenant
        Check response with feature flag true/false
        :param tenant_id:
        :param user_id:
        :return:
        """
        roles_resp_post = (
            self.service_admin_client.list_roles_for_user_on_tenant(
                tenant_id=tenant_id, user_id=user_id)
        )
        self.assertEqual(roles_resp_post.status_code, 200)
        self.assertNotIn(self.tenant_access_role_id,
                         str(roles_resp_post.json()[const.ROLES]))

    def test_implicitly_grant_tenant_access_role_create_user_one_call(self):
        """
        Test verify implicitly grant identity tenant access role to user on all
        tenants within user domain
        Test with steps:
        - create user one call logic (with mosso and nast tenants added)
        - create tenant
        - verify exception rule for default domain (AC 1.1)
        - add tenant to user domain
        - verify: (AC 1, 2.1 to 2.6 and 3 coverage)
            - auth,
            - validate token,
            - auth with mosso and token
            - auth with nast and token
            - auth with new tenant and token
            - auth with mosso tenant
            - auth with nast tenant
            - auth with new tenant
            - list endpoints for user
            - list tenants
            - list users for tenant (for all tenants)
            - list roles for user on tenant
            - remove new tenant from user domain
            - auth with new tenant
            - list user for new tenant
        :return:
        """
        # create user
        user_id, user_resp = self.create_user_one_call()
        user_name = user_resp.json()[const.USER][const.USERNAME]
        password = user_resp.json()[const.USER][const.NS_PASSWORD]
        domain_id = user_resp.json()[const.USER][const.RAX_AUTH_DOMAIN_ID]

        # create tenant with default domain
        tenant_id, tenant_resp = self.create_tenant()
        # get default domain
        default_domain = tenant_resp.json()[const.TENANT][
            const.RAX_AUTH_DOMAIN_ID]

        # verify belong to default domain not implicitly add tenant-access
        if self.tenant_default_domain_value == default_domain:
            self.verify_user_belong_to_default_domain(
                domain_id=default_domain, tenant_id=tenant_id)

        # add tenant to user domain
        add_resp = self.identity_admin_client.add_tenant_to_domain(
            domain_id=domain_id, tenant_id=tenant_id)
        self.assertEqual(add_resp.status_code, 204)

        # verify tenant access with authentication
        mosso_tenant, nast_tenant, user_token = self.verify_auth_w_password(
            user_name=user_name, password=password)

        # verify tenant access with Validate token
        self.verify_validate_token(token=user_token)

        # verify tenant access when Auth with mosso tenant and token
        self.verify_auth_w_tenant_and_token(tenant_id=mosso_tenant,
                                            token=user_token)

        # verify tenant access when Auth with nast tenant and token
        self.verify_auth_w_tenant_and_token(tenant_id=nast_tenant,
                                            token=user_token)

        # verify tenant access when Auth with new tenant and token
        self.verify_auth_w_new_tenant_token(tenant_id=tenant_id,
                                            token=user_token)

        # verify Auth with username, password, and mosso tenant
        self.verify_auth_w_username_password_n_tenant(username=user_name,
                                                      password=password,
                                                      tenant_id=mosso_tenant)

        # verify Auth username, password, and nast tenant
        self.verify_auth_w_username_password_n_tenant(username=user_name,
                                                      password=password,
                                                      tenant_id=nast_tenant)

        # verify Auth username, password, and new tenant
        self.verify_auth_w_username_password_n_new_tenant(
            username=user_name, password=password, tenant_id=tenant_id)

        # List endpoints for token, The user must receive appropriate service
        # catalog entries for all tenants within the user's domain
        self.verify_list_endpoint_for_user(token=user_token,
                                           mosso_tenant=mosso_tenant,
                                           nast_tenant=nast_tenant,
                                           new_tenant=tenant_id)

        # verify List tenants
        self.verify_list_tenant(token=user_token, mosso_tenant=mosso_tenant,
                                nast_tenant=nast_tenant, new_tenant=tenant_id)

        # verify List users for mosso tenant
        self.verify_list_users_for_tenant(token=user_token,
                                          tenant_id=mosso_tenant,
                                          user_id=user_id, resp_code=200)

        # verify list users for nast tenant
        self.verify_list_users_for_tenant(token=user_token,
                                          tenant_id=nast_tenant,
                                          user_id=user_id, resp_code=200)

        # verify list users for new tenant
        self.verify_list_users_for_tenant(token=user_token,
                                          tenant_id=tenant_id,
                                          user_id=user_id, resp_code=403)

        # verify List roles for user on mosso tenant
        self.verify_list_roles_for_user_on_tenant(tenant_id=mosso_tenant,
                                                  user_id=user_id)

        # verify List roles for user on nast tenant
        self.verify_list_roles_for_user_on_tenant(tenant_id=nast_tenant,
                                                  user_id=user_id)

        # verify List roles for user on new tenant
        self.verify_list_roles_for_user_on_tenant(tenant_id=tenant_id,
                                                  user_id=user_id)

        # remove new tenant from domain
        del_resp = self.identity_admin_client.delete_tenant_from_domain(
            domain_id=domain_id, tenant_id=tenant_id)
        self.assertEqual(del_resp.status_code, 204)

        # verify auth with deleted tenant again regardless feature flag
        self.verify_auth_w_username_password_n_invalid_tenant(
            username=user_name, password=password, tenant_id=tenant_id)

        # verify get users for tenant after delete regardless feature flag
        self.verify_list_users_for_tenant_w_identity_admin_post_del(
            tenant_id=tenant_id, user_id=user_id)

        # List roles for user on tenant post delele regardless feature flag
        self.verify_list_roles_for_user_on_tenant_post_del(
            tenant_id=tenant_id, user_id=user_id)

    def test_implicitly_grant_tenant_access_role_new_tenant(self):
        """
        Test with regular steps:
        - create user (no mosso tenant or nast tenant added)
        - create tenant
        - add tenant to user domain
        - verify:
            - auth,
            - validate token,
            - auth as tenant and token
            - auth with new tenant
            - list endpoints for user
            - list tenants
            - list users for tenant
            - list roles for user on tenant
        :return:
        """
        # create user
        user_id, user_resp = self.create_user()
        user_name = user_resp.json()[const.USER][const.USERNAME]
        password = user_resp.json()[const.USER][const.NS_PASSWORD]
        domain_id = user_resp.json()[const.USER][const.RAX_AUTH_DOMAIN_ID]

        # create tenant
        tenant_id, tenant_resp = self.create_tenant(domain_id=domain_id)

        # add tenant to user domain
        add_resp = self.identity_admin_client.add_tenant_to_domain(
            domain_id=domain_id, tenant_id=tenant_id)
        self.assertEqual(add_resp.status_code, 204)

        # get tenant for domain
        get_tenant_resp = self.identity_admin_client.get_tenants_in_domain(
            domain_id=domain_id)
        self.assertEqual(get_tenant_resp.status_code, 200)
        self.assertIn(tenant_id, str(get_tenant_resp.json()[const.TENANTS]))

        # verify authenticate
        _, _, user_token = self.verify_auth_w_password(user_name=user_name,
                                                       password=password)

        # verify Validate token
        self.verify_validate_token(token=user_token)

        # verify auth with token and new tenant
        self.verify_auth_w_new_tenant_token(tenant_id=tenant_id,
                                            token=user_token)

        # verify Auth with username, password and new tenant
        self.verify_auth_w_username_password_n_new_tenant(
            username=user_name, password=password, tenant_id=tenant_id)

        # List endpoints for user
        self.verify_list_endpoint_for_user(token=user_token,
                                           new_tenant=tenant_id)

        # verify List tenants
        self.verify_list_tenant(token=user_token, new_tenant=tenant_id)

        # verify list users for tenant with own token
        self.verify_list_users_for_tenant(token=user_token,
                                          tenant_id=tenant_id,
                                          user_id=user_id, resp_code=403)

        # verify List users for tenant with identity admin
        self.verify_list_users_for_tenant_w_identity_admin(
            tenant_id=tenant_id, user_id=user_id)

        # List roles for user on tenant
        self.verify_list_roles_for_user_on_tenant(tenant_id=tenant_id,
                                                  user_id=user_id)

    def tearDown(self):
        # Delete all resources created in the tests
        for id_ in self.user_ids:
            self.identity_admin_client.delete_user(user_id=id_)
        for id_ in self.tenant_ids:
            self.identity_admin_client.delete_tenant(tenant_id=id_)
        for id_ in self.domain_ids:
            self.identity_admin_client.delete_domain(domain_id=id_)
        super(TestUserImplicitlyGrantedTenantAccessRole, self).tearDown()
