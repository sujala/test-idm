# -*- coding: utf-8 -*
import copy

from hypothesis import given, strategies
from nose.plugins.attrib import attr
import ddt
from qe_coverage.opencafe_decorators import unless_coverage

from tests.api.utils import header_validation
from tests.api.v2 import base
from tests.api.v2.models import factory
from tests.api.v2.schema import users as users_json

from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


@ddt.ddt
class TestAddUser(base.TestBaseV2):

    """Add User Tests
    Adds user_admin, sub_user, user_manage level users."""

    @classmethod
    @unless_coverage
    def setUpClass(cls):
        """Class level set up for the tests

        Create users needed for the tests and generate clients for those users.
        """
        super(TestAddUser, cls).setUpClass()

        domain_id = cls.generate_random_string(pattern='[\d]{7}')
        cls.user_admin_client = cls.generate_client(
            parent_client=cls.identity_admin_client,
            additional_input_data={'domain_id': domain_id})

        sub_user_name = cls.generate_random_string(
            pattern=const.SUB_USER_PATTERN)
        cls.user_client = cls.generate_client(
            parent_client=cls.user_admin_client,
            additional_input_data={
                'domain_id': domain_id,
                'user_name': sub_user_name})
        cls.add_input = {'domain_id': const.DOMAIN_TEST}
        cls.add_schema_fields = [const.NS_PASSWORD]

    @unless_coverage
    def setUp(self):
        super(TestAddUser, self).setUp()
        self.user_ids = []

    @unless_coverage
    @ddt.file_data('data_add_user_admin_user.json')
    def test_add_user_admin_user(self, test_data):
        '''Add user_admin type users

        test_data comes from a json data file that can contain various possible
        input combinations. Each of these data combination is a separate test
        case.
        NOTE: This test case illustrates providing test_data in a json file.
        @todo: The test_data file needs to be updated to include all possible
        data combinations.
        '''
        user_name = self.generate_random_string()
        input_data = test_data['additional_input']
        input_data['domain_id'] = self.generate_random_string(
            pattern='[\d]{7}')
        request_object = requests.UserAdd(user_name=user_name, **input_data)
        resp = self.identity_admin_client.add_user(request_object)
        self.assertEqual(resp.status_code, 201)
        self.user_ids.append(resp.json()[const.USER][const.ID])

        updated_json_schema = copy.deepcopy(users_json.add_user)
        updated_json_schema[const.PROPERTIES][const.USER][const.REQUIRED] = (
            users_json.add_user[const.PROPERTIES][const.USER][const.REQUIRED] +
            test_data['additional_schema_fields'])

        self.assertSchema(response=resp, json_schema=updated_json_schema)

        self.assertHeaders(
            resp, *self.header_validation_functions_HTTP_201)

        get_resp = self.identity_admin_client.get_user(
            resp.json()[const.USER][const.ID])

        self.assertSchema(response=get_resp, json_schema=users_json.get_user)

    @unless_coverage
    @ddt.file_data('data_add_user_w_mfa_attrs.json')
    def test_add_user_admin_user_w_mfa_attrs(self, test_data):
        '''Add user_admin type users

        test_data comes from a json data file that can contain various possible
        input combinations. Each of these data combination is a separate test
        case.
        NOTE: This test case illustrates providing test_data in a json file.
        '''
        mfa_input = test_data['mfa_input']
        data_list = self.generate_data_combinations(data_dict=mfa_input)
        # run multiple combination of mfa input attributes
        for ea_mfa_input in data_list:
            user_name = self.generate_random_string()
            add_input = self.add_input
            input_data = dict(add_input, **ea_mfa_input)
            input_data['domain_id'] = self.generate_random_string(
                pattern='[\d]{7}')
            request_object = requests.UserAdd(user_name=user_name,
                                              **input_data)
            resp = self.identity_admin_client.add_user(request_object)
            self.assertEqual(resp.status_code, 201,
                             "Fail to create user with {0}".format(input_data))
            self.user_ids.append(resp.json()[const.USER]['id'])

            updated_json_schema = copy.deepcopy(users_json.add_user)
            updated_json_schema[const.PROPERTIES][const.USER][
                const.REQUIRED] = (
                users_json.add_user[const.PROPERTIES][const.USER][
                    const.REQUIRED] + self.add_schema_fields)

            self.assertEqual(resp.json()[const.USER][
                const.RAX_AUTH_MULTI_FACTOR_ENABLED], False)
            self.assertSchema(response=resp, json_schema=updated_json_schema)
            self.assertHeaders(response=resp)

    @unless_coverage
    @ddt.file_data('data_add_user_w_mfa_attrs.json')
    def test_add_user_default_user_w_mfa_attrs(self, test_data):
        '''Add user_defaut type users

        test_data comes from a json data file that can contain various possible
        input combinations. Each of these data combination is a separate test
        case.
        NOTE: This test case illustrates providing test_data in a json file.
        '''
        mfa_input = test_data['mfa_input']
        data_list = self.generate_data_combinations(data_dict=mfa_input)
        # run multiple combination of mfa input attributes
        for ea_mfa_input in data_list:
            add_input = self.add_input
            input_data = dict(add_input, **ea_mfa_input)
            user_name = self.generate_random_string(
                pattern=const.SUB_USER_PATTERN)
            request_object = requests.UserAdd(
                user_name=user_name,
                **input_data
            )
            resp = self.user_admin_client.add_user(request_object)
            self.assertEqual(resp.status_code, 201,
                             "Fail to create user with {0}".format(input_data))
            self.user_ids.append(resp.json()[const.USER][const.ID])

            updated_json_schema = copy.deepcopy(users_json.add_user)
            updated_json_schema[const.PROPERTIES][const.USER][
                const.REQUIRED] = (
                users_json.add_user[const.PROPERTIES][const.USER][
                    const.REQUIRED] + self.add_schema_fields)

            self.assertEqual(resp.json()[const.USER][
                const.RAX_AUTH_MULTI_FACTOR_ENABLED], False)
            self.assertSchema(response=resp, json_schema=updated_json_schema)
            self.assertHeaders(response=resp)

    @unless_coverage
    @ddt.data(True, False, '')
    def test_add_user(self, enabled):
        """Add user_admin type users

        various possible test_data for the enabled field is defined in the line
        above the test method definition.Each of these data is run as a
        separate test case. This test case currently exists for
        illustrating a different method of providing test data.

        @todo: This can be merged into the previous test_case
        'test_add_user_admin_user'.
        """
        user_name = self.generate_random_string()
        password = self.generate_random_string(pattern=const.PASSWORD_PATTERN)
        request_object = requests.UserAdd(
            user_name=user_name,
            password=password,
            enabled=enabled,
            domain_id=const.DOMAIN_TEST
        )
        resp = self.identity_admin_client.add_user(request_object)
        self.assertEqual(resp.status_code, 201)
        self.user_ids.append(resp.json()[const.USER][const.ID])

        self.assertSchema(response=resp, json_schema=users_json.add_user)
        self.assertHeaders(
            resp, *self.header_validation_functions_HTTP_201)

    @unless_coverage
    @ddt.data(['  ', 'me@mail.com', True], ['വളം', 'me@mail.com', True],
              ['first last', 'valid@email.com', ''])
    @ddt.unpack
    @attr('skip_at_gate')
    def test_add_user_ddt(self, user_name, email_id, enabled):
        """Add user_admin type users

        various possible test_data for the enabled field is defined in the line
        above the test method definition.Each of these data is run as a
        separate test case. This test case currently exists for
        illustrating a different method of providing test data.

        @todo: This can be merged into the previous test_case
        'test_add_user_admin_user'."""
        request_object = requests.UserAdd(
            user_name=user_name,
            email=email_id,
            enabled=enabled,
            domain_id=const.DOMAIN_TEST
        )
        resp = self.identity_admin_client.add_user(request_object)
        self.assertEqual(resp.status_code, 400)

        # This fails on 'Connection' header being absent when run
        # in docker environment but passes in Staging
        self.assertHeaders(
            resp, *self.header_validation_functions_HTTP_400)

    @unless_coverage
    @given(strategies.text(), strategies.booleans())
    def test_add_user_hypothesis(self, user_name, enabled):
        """Property Based Testing

        Generate possible test inputs based on the definition of the input
        fields.
        """
        if not self.test_config.run_hypothesis_tests:
            self.skipTest('Skipping Hypothesis tests per config value')
        request_object = requests.UserAdd(
            user_name=user_name,
            email=const.EMAIL_RANDOM,
            enabled=enabled
        )
        resp = self.user_admin_client.add_user(request_object)
        self.assertEqual(resp.status_code, 400)
        self.assertHeaders(
            resp, *self.header_validation_functions_HTTP_400)

    @unless_coverage
    def tearDown(self):
        # Delete all users created in the tests
        for id in self.user_ids:
            resp = self.identity_admin_client.delete_user(user_id=id)
            self.assertEqual(resp.status_code, 204)
        super(TestAddUser, self).tearDown()

    @classmethod
    @unless_coverage
    def tearDownClass(cls):
        # Delete all users created in the setUpClass
        cls.identity_admin_client.delete_user(
            cls.user_client.default_headers[const.X_USER_ID])
        cls.delete_client(client=cls.user_admin_client,
                          parent_client=cls.identity_admin_client)
        super(TestAddUser, cls).tearDownClass()


@ddt.ddt
class TestAddUserExistingRolesTenants(base.TestBaseV2):
    """Add User With Existing Roles and Tenants Tests."""

    @classmethod
    @unless_coverage
    def setUpClass(cls):
        """Class level set up for the tests

        Create users needed for the tests and generate clients for those users.
        """
        super(TestAddUserExistingRolesTenants, cls).setUpClass()

    @unless_coverage
    def setUp(self):
        super(TestAddUserExistingRolesTenants, self).setUp()
        self.user_ids = []

    def one_user_validator(self, resp, test_data):

        if ("domain" in test_data and "enabled" in test_data["domain"] and
                not test_data["domain"]["enabled"]):
            self.assertEqual(resp.status_code, 403)
            return
        if ("domain" in test_data and "default" in test_data["domain"] and
                test_data["domain"]["default"]):
            self.assertEqual(resp.status_code, 400)
            return

        if ("domain" in test_data and "default" in test_data["domain"] and
                not test_data["domain"]["default"] and
                "users" in test_data["domain"] and
                test_data["domain"]["users"]):
            self.assertEqual(resp.status_code, 403)
            return
        # possible bug
        if ("tenant" in test_data and "in_domain" in test_data["tenant"] and
                not test_data["tenant"]["in_domain"]):
            self.assertEqual(resp.status_code, 400)
            return

        self.assertEqual(resp.status_code, 201)
        self.user_ids.append(resp.json()[const.USER][const.ID])

        updated_json_schema = copy.deepcopy(users_json.add_user)
        updated_json_schema[const.PROPERTIES][const.USER][const.REQUIRED] = (
            users_json.add_user[const.PROPERTIES][const.USER][const.REQUIRED] +
            test_data['additional_schema_fields'])

        self.assertSchema(response=resp, json_schema=updated_json_schema)

        self.assertHeaders(
            resp, *self.header_validation_functions_HTTP_201)

        # authenticate with new user
        if (not test_data["domain"]["enabled"]):
            username = resp.json()[const.USER][const.USERNAME]
            pw = resp.json()[const.USER][const.NS_PASSWORD]
            auth_obj = requests.AuthenticateWithPassword(user_name=username,
                                                         password=pw)
            auth_resp = self.identity_admin_client.get_auth_token(
                request_object=auth_obj)
            domain_id = resp.json()[const.USER][const.RAX_AUTH_DOMAIN_ID]
            for arole in auth_resp.json()[const.ACCESS][
                    const.USER][const.ROLES]:
                if (arole[const.NAME] == const.OBJECT_STORE_ROLE_NAME and
                        const.TENANT_ID in arole):
                    nast_tenant = arole[const.TENANT_ID]
                    self.assertEqual(const.NAST_PREFIX +
                                     "{0}".format(domain_id),
                                     nast_tenant)
                if (arole[const.NAME] == const.COMPUTE_ROLE_NAME and
                        const.TENANT_ID in arole):
                    mosso_tenant = arole[const.TENANT_ID]
                    self.assertEqual(domain_id,
                                     mosso_tenant)

    def get_validator(self, validator):
        if validator == "one_user_validator":
            return self.one_user_validator

    def create_domain(self, domain_req):
        dom = factory.get_domain_request_object(domain_req)
        dom_resp = self.identity_admin_client.add_domain(dom)
        domain_id = dom_resp.json()[const.RAX_AUTH_DOMAIN][const.ID]

        return domain_id

    def create_user(self, domain_id=None):
        if domain_id:
            request_object = factory.get_add_user_request_object_pull(
                domain_id=domain_id)
        else:
            request_object = factory.get_add_user_request_object()
        resp = self.identity_admin_client.add_user(
            request_object=request_object)
        user_json = resp.json()
        if const.USER in user_json and const.ID in user_json[const.USER]:
            self.user_ids.append(resp.json()[const.USER][const.ID])

        return resp

    def get_sqsa(self):
        secret_q = self.generate_random_string(pattern=const.SECRETQ_PATTERN)
        secret_a = self.generate_random_string(pattern=const.SECRETA_PATTERN)
        secret_qa = {
            const.SECRET_QUESTION: secret_q,
            const.SECRET_ANSWER: secret_a
        }
        return secret_qa

    def create_tenant(self, domain_id, tenant_req, secret_qa):
        # easiest to just create user and then delete tenants
        user_name_del = self.generate_random_string()
        ten_domain_id = domain_id
        if "in_domain" in tenant_req and not tenant_req["in_domain"]:
            ten_domain_id = self.generate_random_string(
                const.ID_PATTERN)
        request_object = requests.UserAdd(user_name=user_name_del,
                                          domain_id=ten_domain_id,
                                          secret_qa=secret_qa)

        resp = self.identity_admin_client.add_user(
            request_object=request_object)
        user_id = resp.json()[const.USER][const.ID]
        for arole in resp.json()[const.USER][const.ROLES]:
            if (arole[const.NAME] == const.OBJECT_STORE_ROLE_NAME and
                    const.TENANT_ID in arole):
                mosso_tenant = arole[const.TENANT_ID]
                mosso_tenant_role = arole
            if (arole[const.NAME] == const.COMPUTE_ROLE_NAME and
                    const.TENANT_ID in arole):
                nast_tenant = arole[const.TENANT_ID]
                nast_tenant_role = arole
        # remove not used tenants.
        if "existing" in tenant_req:
            if "mosso" not in tenant_req["existing"]:
                self.identity_admin_client.delete_tenant(mosso_tenant)
            if "nast" not in tenant_req["existing"]:
                self.identity_admin_client.delete_tenant(nast_tenant)
        else:
                self.identity_admin_client.delete_tenant(nast_tenant)
                self.identity_admin_client.delete_tenant(mosso_tenant)

        # delete user
        self.identity_admin_client.delete_user(user_id=user_id)

        result = []
        if "existing" in tenant_req and "mosso" in tenant_req["existing"]:
            result.append(mosso_tenant_role)
        if "existing" in tenant_req and "nast" in tenant_req["existing"]:
            result.append(nast_tenant_role)

        return result

    @unless_coverage
    @ddt.file_data('data_one_user_existing_domains_tenants.json')
    def test_add_user_admin_user_existing_doms_tenants(self, test_data):
        '''Add user_admin type users where the domains and tenants exist.'''
        user_name = self.generate_random_string()
        domain_req = test_data['domain']
        tenant_req = test_data['tenant']

        domain_id = const.ID_PATTERN
        # create domain
        create_domain = False
        if domain_req:
            if not ("default" in domain_req and domain_req["default"]):
                create_domain = True
            if create_domain:
                domain_id = self.create_domain(domain_req=domain_req)
            if "users" in domain_req and domain_req["users"]:
                if create_domain:
                    resp = self.create_user(domain_id=domain_id)
                    self.assertEqual(resp.status_code, 201)
                else:
                    resp = self.create_user()
                    self.assertEqual(resp.status_code, 201)
        secret_qa = self.get_sqsa()

        roles = []
        if tenant_req:
            roles = self.create_tenant(domain_id=domain_id,
                                       tenant_req=tenant_req,
                                       secret_qa=secret_qa)
        if create_domain:
            request_object = requests.UserAdd(user_name=user_name,
                                              domain_id=domain_id,
                                              secret_qa=secret_qa,
                                              )
        else:
            request_object = requests.UserAdd(user_name=user_name,
                                              secret_qa=secret_qa)
        request_object.roles = roles
        resp = self.identity_admin_client.add_user(
            request_object=request_object)
        if "validator" in test_data:
            self.get_validator(test_data["validator"])(resp, test_data)

    @unless_coverage
    def tearDown(self):
        # Delete all users created in the tests
        for id in self.user_ids:
            resp = self.identity_admin_client.delete_user(user_id=id)
            self.assertEqual(resp.status_code, 204)
        super(TestAddUserExistingRolesTenants, self).tearDown()

    @classmethod
    @unless_coverage
    def tearDownClass(cls):
        super(TestAddUserExistingRolesTenants, cls).tearDownClass()


@ddt.ddt
class TestServiceAdminLevelAddUser(base.TestBaseV2):

    """Add User Tests

    Adds identity_admin users using a service admin level user.
    All tests in this class require a service admin level user credentials.
    """
    @classmethod
    @unless_coverage
    def setUpClass(cls):
        """Class level set up for the tests

        Create users needed for the tests and generate clients for those users.
        """
        super(TestServiceAdminLevelAddUser, cls).setUpClass()

    @unless_coverage
    def setUp(self):
        super(TestServiceAdminLevelAddUser, self).setUp()
        if not self.test_config.run_service_admin_tests:
            self.skipTest('Skipping Service Admin Tests per config value')
        self.user_ids = []
        self.unexpected_headers_HTTP_201 = [
            header_validation.validate_transfer_encoding_header_not_present]
        self.header_validation_functions_HTTP_201 = (
            self.default_header_validations +
            self.unexpected_headers_HTTP_201 + [
                header_validation.validate_header_location,
                header_validation.validate_header_content_length])
        self.add_input = {'domain_id': const.DOMAIN_TEST}
        self.add_schema_fields = [const.NS_PASSWORD]

    @unless_coverage
    @ddt.file_data('data_add_identity_admin_user.json')
    def test_add_identity_admin_user(self, test_data):
        """Add identity admin level users

        Add identity admin level users with various test input combinations
        defined in the test data json file.
        """
        user_name = self.generate_random_string()
        input_data = test_data['additional_input']
        request_object = requests.UserAdd(
            user_name=user_name,
            **input_data
        )
        resp = self.service_admin_client.add_user(request_object)
        self.assertEqual(resp.status_code, 201)
        self.user_ids.append(resp.json()[const.USER][const.ID])

        updated_json_schema = copy.deepcopy(users_json.add_user)
        updated_json_schema[const.PROPERTIES][const.USER][const.REQUIRED] = (
            users_json.add_user[const.PROPERTIES][const.USER][const.REQUIRED] +
            test_data['additional_schema_fields'])
        self.assertSchema(response=resp, json_schema=updated_json_schema)

        self.assertHeaders(
            resp, *self.header_validation_functions_HTTP_201)

    @unless_coverage
    @ddt.file_data('data_add_user_w_mfa_attrs.json')
    @attr(type='skip_at_gate')
    def test_add_user_identity_admin_w_mfa_attrs(self, test_data):
        '''Add identity_admin type users'''
        mfa_input = test_data['mfa_input']
        data_list = self.generate_data_combinations(data_dict=mfa_input)
        # run multiple combination of mfa input attributes
        for ea_mfa_input in data_list:
            user_name = self.generate_random_string()
            add_input = self.add_input
            input_data = dict(add_input, **ea_mfa_input)
            request_object = requests.UserAdd(
                user_name=user_name,
                **input_data
            )
            resp = self.service_admin_client.add_user(request_object)
            self.assertEqual(resp.status_code, 201,
                             "Fail to create user with {0}".format(input_data))
            self.user_ids.append(resp.json()[const.USER][const.ID])

            updated_json_schema = copy.deepcopy(users_json.add_user)
            updated_json_schema[const.PROPERTIES][const.USER][
                const.REQUIRED] = (
                users_json.add_user[const.PROPERTIES][const.USER][
                    const.REQUIRED] + self.add_schema_fields)

            self.assertEqual(resp.json()[const.USER][
                const.RAX_AUTH_MULTI_FACTOR_ENABLED], False)
            self.assertSchema(response=resp, json_schema=updated_json_schema)
            self.assertHeaders(response=resp)

    @unless_coverage
    def tearDown(self):
        # Delete all users created in the tests
        for id in self.user_ids:
            resp = self.service_admin_client.delete_user(user_id=id)
            self.assertEqual(resp.status_code, 204)
        super(TestServiceAdminLevelAddUser, self).tearDown()

    @classmethod
    @unless_coverage
    def tearDownClass(cls):
        super(TestServiceAdminLevelAddUser, cls).tearDownClass()
