# -*- coding: utf-8 -*
import ddt
from tests.api.v2 import base
from tests.api.v2.models import factory

from tests.package.johny import constants as const
from tests.package.johny.v2 import client
from tests.package.johny.v2.models import requests


@ddt.ddt
class TestUpdateIDP(base.TestBaseV2):
    """
    Test update IDP
    1. Allow users to update an IDP name
    2. The name must meet the same validations as setting the name on creation
    3. The specified name must be returned in the response as specified in the
    request, in the exact case in which it was originally provided
    """
    @classmethod
    def setUpClass(cls):
        """Class level set up for the tests

        Create users needed for the tests and generate clients for those users.
        """
        super(TestUpdateIDP, cls).setUpClass()
        user_name = cls.generate_random_string()
        password = cls.generate_random_string(const.PASSWORD_PATTERN)
        request_object = requests.UserAdd(user_name=user_name,
                                          password=password)
        cls.service_admin_client.add_user(request_object)

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
        super(TestUpdateIDP, self).setUp()
        self.provider_ids = []
        self.provider_id, self.provider_name = self.add_idp_user()

    def add_idp_user(self):
        request_object = factory.get_add_idp_request_object()
        resp = self.idp_ia_client.create_idp(request_object)
        self.assertEquals(resp.status_code, 201)
        provider_id = resp.json()[const.NS_IDENTITY_PROVIDER][const.ID]
        self.provider_ids.append(provider_id)
        provider_name = resp.json()[const.NS_IDENTITY_PROVIDER][const.NAME]
        return provider_id, provider_name

    def test_update_idp_valid_name(self):
        """Test update with valid name randomly generate name with
        alphanumeric, '.', and '-' characters in range from 1 to 255
        """
        idp_name = self.generate_random_string(pattern='[a-zA-Z0-9.\-]{:255}')
        idp_obj = requests.IDP(idp_name=idp_name)
        resp = self.idp_ia_client.update_idp(idp_id=self.provider_id,
                                             request_object=idp_obj)
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(resp.json()[const.NS_IDENTITY_PROVIDER][const.NAME],
                         idp_name)

    @ddt.data("?test", "test_update_idp", "test*", "*", "$test#", "test@cid")
    def test_update_idp_name_with_invalid_characters(self, idp_name):
        """Update idp name with a invalid name"""
        common_error_msg = (
            "Error code: 'GEN-005'; Identity provider name"
            " must consist of only alphanumeric, '.', and '-' characters.")
        idp_obj = requests.IDP(idp_name=idp_name)
        resp = self.idp_ia_client.update_idp(idp_id=self.provider_id,
                                             request_object=idp_obj)
        self.assertEqual(resp.status_code, 400)
        self.assertEqual(resp.json()[const.BAD_REQUEST][const.MESSAGE],
                         common_error_msg)

    def test_update_idp_name_with_empty_string(self):
        """Update with empty string"""
        error_msg = (
            "Error code: 'GEN-001'; 'name' is a required attribute")
        empty_str = " "
        idp_obj = requests.IDP(idp_name=empty_str)
        resp = self.idp_ia_client.update_idp(idp_id=self.provider_id,
                                             request_object=idp_obj)
        self.assertEqual(resp.status_code, 400)
        self.assertEqual(resp.json()[const.BAD_REQUEST][const.MESSAGE],
                         error_msg)

    def test_update_idp_name_with_exceed_max_lenght(self):
        """Update name with exceed max lenght"""
        error_msg = (
            "Error code: 'GEN-002'; name length cannot exceed 255 characters")
        larg_name = self.generate_random_string(pattern='[a-zA-Z0-9.\-]{256}')
        idp_obj = requests.IDP(idp_name=larg_name)
        resp = self.idp_ia_client.update_idp(idp_id=self.provider_id,
                                             request_object=idp_obj)
        self.assertEqual(resp.status_code, 400)
        self.assertEqual(resp.json()[const.BAD_REQUEST][const.MESSAGE],
                         error_msg)

    def test_update_idp_name_with_exist_name(self):
        """Update with name already exist"""
        _, new_provider_name = self.add_idp_user()
        error_msg = ("Error code: 'FED_IDP-005';"
                     " Identity provider with name {0} already "
                     "exist.".format(new_provider_name))
        idp_obj = requests.IDP(idp_name=new_provider_name)
        resp = self.idp_ia_client.update_idp(idp_id=self.provider_id,
                                             request_object=idp_obj)
        self.assertEqual(resp.status_code, 400)
        self.assertEqual(resp.json()[const.BAD_REQUEST][const.MESSAGE],
                         error_msg)

    def tearDown(self):
        # Delete all providers created in the tests
        for id_ in self.provider_ids:
            self.idp_ia_client.delete_idp(idp_id=id_)
        super(TestUpdateIDP, self).tearDown()

    @classmethod
    def tearDownClass(cls):
        # Delete all users created in the setUpClass
        cls.delete_client(client=cls.idp_ia_client,
                          parent_client=cls.service_admin_client)
        super(TestUpdateIDP, cls).tearDownClass()
