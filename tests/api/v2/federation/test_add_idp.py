# -*- coding: utf-8 -*
from tests.api.v2 import base
from tests.api.v2.models import factory

from tests.package.johny import constants as const
from tests.package.johny.v2 import client
from tests.package.johny.v2.models import requests


class TestAddIDP(base.TestBaseV2):

    """Add IDP Tests
    Currently only tests which involve setting the name."""

    @classmethod
    def setUpClass(cls):
        """Class level set up for the tests

        Create users needed for the tests and generate clients for those users.
        """
        super(TestAddIDP, cls).setUpClass()
        user_name = cls.generate_random_string()
        password = cls.generate_random_string(const.PASSWORD_PATTERN)
        request_object = requests.UserAdd(user_name=user_name,
                                          password=password)
        resp = cls.service_admin_client.add_user(request_object)

        req_obj = requests.AuthenticateWithPassword(
            user_name=user_name,
            password=password)

        cls.idp_ia_client = client.IdentityAPIClient(
            url=cls.url,
            serialize_format=cls.test_config.serialize_format,
            deserialize_format=cls.test_config.deserialize_format)

        resp = cls.idp_ia_client.get_auth_token(request_object=req_obj)
        identity_admin_auth_token = resp.json()[const.ACCESS][const.TOKEN][
            const.ID]
        identity_admin_id = resp.json()[const.ACCESS][const.USER][const.ID]
        cls.idp_ia_client.default_headers[const.X_AUTH_TOKEN] = (
            identity_admin_auth_token)
        cls.idp_ia_client.default_headers[const.X_USER_ID] = (
            identity_admin_id
        )

        if cls.test_config.run_service_admin_tests:
            option = {
                const.PARAM_ROLE_NAME: const.PROVIDER_MANAGEMENT_ROLE_NAME
            }
            list_resp = cls.service_admin_client.list_roles(option=option)
            mapping_rules_role_id = list_resp.json()[const.ROLES][0][const.ID]
            cls.service_admin_client.add_role_to_user(
                user_id=identity_admin_id, role_id=mapping_rules_role_id)

    def setUp(self):
        super(TestAddIDP, self).setUp()
        self.provider_ids = []

    def test_add_idp_with_name(self):
        '''Add with a name
        '''
        if not self.test_config.run_service_admin_tests:
            self.skipTest('Skipping Service Admin Tests per config value')

        request_object = factory.get_add_idp_request_object()
        resp = self.idp_ia_client.create_idp(request_object)
        self.assertEquals(resp.status_code, 201)
        self.provider_ids.append(resp.json()[
            const.NS_IDENTITY_PROVIDER][const.ID])
        self.assertEquals(resp.json()[const.NS_IDENTITY_PROVIDER][const.NAME],
                          request_object.idp_name)

    def test_add_idp_with_no_name(self):
        '''Add with empty  name
        '''
        if not self.test_config.run_service_admin_tests:
            self.skipTest('Skipping Service Admin Tests per config value')
        request_object = factory.get_add_idp_request_object()
        request_object.idp_name = None
        print request_object
        resp = self.idp_ia_client.create_idp(request_object)
        self.assertEquals(resp.status_code, 400)
        self.assertEquals(resp.json()[const.BAD_REQUEST][const.MESSAGE],
                          "Error code: 'GEN-001'; 'name' is a required"
                          " attribute")

    def test_add_idp_with_empty_name(self):
        '''Add with empty  name
        '''
        if not self.test_config.run_service_admin_tests:
            self.skipTest('Skipping Service Admin Tests per config value')
        request_object = factory.get_add_idp_request_object()
        request_object.idp_name = ""
        resp = self.idp_ia_client.create_idp(request_object)
        self.assertEquals(resp.status_code, 400)
        self.assertEquals(resp.json()[const.BAD_REQUEST][const.MESSAGE],
                          "Error code: 'GEN-001'; 'name' is a required"
                          " attribute")

    def test_add_idp_with_dup_name(self):
        '''Add with dup name
        '''
        if not self.test_config.run_service_admin_tests:
            self.skipTest('Skipping Service Admin Tests per config value')

        request_object = factory.get_add_idp_request_object()
        resp = self.idp_ia_client.create_idp(request_object)
        self.assertEquals(resp.status_code, 201)
        self.provider_ids.append(resp.json()[
            const.NS_IDENTITY_PROVIDER][const.ID])
        resp = self.idp_ia_client.create_idp(request_object)
        self.assertEquals(resp.status_code, 409)
        self.assertEquals(resp.json()[const.BAD_REQUEST][const.MESSAGE],
                          "Error code: 'FED_IDP-005'; Identity provider with "
                          "name {0} already exist.".format(
                              request_object.idp_name))

    def test_add_idp_name_max_length(self):
        '''Add with bad characters in name
        '''
        if not self.test_config.run_service_admin_tests:
            self.skipTest('Skipping Service Admin Tests per config value')
        request_object = factory.get_add_idp_request_object()
        request_object.idp_name = self.generate_random_string(
            const.MAX_IDP_NAME_PATTERN)
        resp = self.idp_ia_client.create_idp(request_object)
        self.assertEquals(resp.status_code, 201)
        self.provider_ids.append(resp.json()[
            const.NS_IDENTITY_PROVIDER][const.ID])

        # verify name wasn't truncated
        get_name_resp = self.idp_ia_client.get_idp(idp_id=resp.json()[
            const.NS_IDENTITY_PROVIDER][const.ID])
        get_name = get_name_resp.json()[const.NS_IDENTITY_PROVIDER][const.NAME]
        self.assertEquals(get_name, request_object.idp_name)

        # Try with longer name
        request_object.idp_name += "B"
        resp = self.idp_ia_client.create_idp(request_object)
        self.assertEquals(resp.status_code, 400)
        self.assertEquals(resp.json()[const.BAD_REQUEST][const.MESSAGE],
                          "Error code: 'GEN-002'; name length cannot exceed "
                          "255 characters")

    def check_bad_name(self, name):
        """ Helper method to isolate repeating bad name code. """
        if not self.test_config.run_service_admin_tests:
            self.skipTest('Skipping Service Admin Tests per config value')
        request_object = factory.get_add_idp_request_object()
        request_object.idp_name = name
        resp = self.idp_ia_client.create_idp(request_object)
        self.assertEquals(resp.status_code, 400)
        self.assertEquals(resp.json()[const.BAD_REQUEST][const.MESSAGE],
                          "Error code: 'GEN-005'; Identity provider name must"
                          " consist of only alphanumeric, '.', and '-'"
                          " characters.")

    def test_add_idp_with_bad_char(self):
        '''Add with bad characters in name
        '''
        self.check_bad_name(name="DSAFDSFA#@$@$#@$AFAS")

    def test_add_idp_with_spaces_at_end(self):
        '''Add with spaces at the end of the name.
        '''
        self.check_bad_name(
            name=self.generate_random_string(const.IDP_NAME_PATTERN) + "  ")

    def test_add_idp_with_spaces_at_the_beginning(self):
        '''Add with spaces at the beginning of the name.
        '''
        self.check_bad_name(
            name="  " + self.generate_random_string(const.IDP_NAME_PATTERN))

    def test_add_idp_with_name_get_idp(self):
        '''Verify get provider by id has name attribute
        '''
        if not self.test_config.run_service_admin_tests:
            self.skipTest('Skipping Service Admin Tests per config value')

        request_object = factory.get_add_idp_request_object()
        resp = self.idp_ia_client.create_idp(request_object)
        self.assertEquals(resp.status_code, 201)
        self.provider_ids.append(resp.json()[
            const.NS_IDENTITY_PROVIDER][const.ID])

        get_name_resp = self.idp_ia_client.get_idp(idp_id=resp.json()[
            const.NS_IDENTITY_PROVIDER][const.ID])
        get_name = get_name_resp.json()[const.NS_IDENTITY_PROVIDER][const.NAME]
        self.assertEquals(get_name, request_object.idp_name)

    def test_add_idp_with_name_list_idp(self):
        '''Verify list providers has name attribute
        '''
        if not self.test_config.run_service_admin_tests:
            self.skipTest('Skipping Service Admin Tests per config value')

        request_object = factory.get_add_idp_request_object()
        resp = self.idp_ia_client.create_idp(request_object)
        self.assertEquals(resp.status_code, 201)
        self.provider_ids.append(resp.json()[
            const.NS_IDENTITY_PROVIDER][const.ID])

        idps = self.idp_ia_client.list_idp().json()[
            const.NS_IDENTITY_PROVIDERS]
        found = False
        for idp in idps:
            idp_name = idp[const.NAME]
            if idp_name == request_object.idp_name:
                found = True
        self.assertEquals(found, True)

    def tearDown(self):
        # Delete all providers created in the tests
        for id_ in self.provider_ids:
            self.idp_ia_client.delete_idp(idp_id=id_)
        super(TestAddIDP, self).tearDown()

    @classmethod
    def tearDownClass(cls):
        # Delete all users created in the setUpClass
        cls.delete_client(client=cls.idp_ia_client,
                          parent_client=cls.service_admin_client)
        super(TestAddIDP, cls).tearDownClass()
