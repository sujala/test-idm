import ddt
from tests.api.v2 import base
from tests.api.v2.schema import credentials as credentials_json
from tests.api.v2.models import factory

from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


@ddt.ddt
class TestCredentials(base.TestBaseV2):
    """
    Credentials
    """
    def setUp(self):
        super(TestCredentials, self).setUp()
        self.user_ids = []
        self.user_id, self.testusername = self.create_user()

    def create_user(self):
        request_object = factory.get_add_user_request_object()
        resp = self.identity_admin_client.add_user(request_object)
        self.assertEqual(resp.status_code, 201)
        user_id = resp.json()[const.USER][const.ID]
        self.user_ids.append(user_id)
        username = resp.json()[const.USER][const.USERNAME]
        return user_id, username

    def test_list_credentials(self):
        resp = self.identity_admin_client.list_credentials(self.user_id)
        self.assertEqual(resp.status_code, 200)
        self.assertSchema(response=resp,
                          json_schema=credentials_json.list_credentials)
        self.assertEqual(self.testusername, resp.json()[const.CREDENTIALS][0]
                         [const.NS_API_KEY_CREDENTIALS][const.USERNAME])
        self.assertIsNotNone(resp.json()[const.CREDENTIALS][0]
                             [const.NS_API_KEY_CREDENTIALS][const.API_KEY])

    def test_add_password_credentials(self):
        password = self.generate_random_string(
                        pattern=const.PASSWORD_PATTERN)
        input_data = {const.USERNAME: self.testusername,
                      const.PASSWORD: password}
        req_obj = requests.PasswordCredentialsAdd(**input_data)
        resp = self.identity_admin_client.add_password_credentials(
                                                        self.user_id,
                                                        req_obj)
        self.assertSchema(response=resp,
                          json_schema=credentials_json.add_password_response)
        self.assertEqual(resp.status_code, 201)
        self.assertEqual(resp.json()[const.PASSWORD_CREDENTIALS]
                                    [const.USERNAME], self.testusername)
        self.assertEqual(resp.json()[const.PASSWORD_CREDENTIALS]
                                    [const.PASSWORD], password)
        req_obj = requests.AuthenticateWithPassword(
            user_name=self.testusername,
            password=password
        )
        resp = self.identity_admin_client.get_auth_token(
            request_object=req_obj
        )
        self.assertEqual(resp.status_code, 200)

    def test_get_apikey(self):
        resp = self.identity_admin_client.get_api_key(self.user_id)
        self.assertEqual(resp.status_code, 200)
        self.assertSchema(response=resp,
                          json_schema=credentials_json.get_apikey_response)
        self.assertIsNotNone(resp.json()[const.NS_API_KEY_CREDENTIALS]
                             [const.API_KEY])
        self.assertEqual(resp.json()[const.NS_API_KEY_CREDENTIALS]
                         [const.USERNAME], self.testusername)

    def test_reset_apikey(self):
        resp = self.identity_admin_client.get_api_key(self.user_id)
        self.assertEqual(resp.status_code, 200)
        previous_apikey = (resp.json()[const.NS_API_KEY_CREDENTIALS]
                                      [const.API_KEY])
        resp = self.identity_admin_client.reset_api_key(self.user_id)
        self.assertEqual(resp.status_code, 200)
        new_apikey = (resp.json()[const.NS_API_KEY_CREDENTIALS]
                                 [const.API_KEY])
        self.assertIsNotNone(new_apikey)
        self.assertFalse(previous_apikey == new_apikey,
                         msg="The API key should be different after reset!")

    def test_delete_apikey(self):
        resp = self.identity_admin_client.delete_api_key(self.user_id)
        self.assertEqual(resp.status_code, 204)
        resp = self.identity_admin_client.list_credentials(self.user_id)
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(resp.json()[const.CREDENTIALS], [])
        resp = self.identity_admin_client.get_api_key(self.user_id)
        self.assertEqual(resp.status_code, 404)

    def test_update_apikey(self):
        apikey = self.generate_random_string(pattern=const.API_KEY_PATTERN)
        request_object = requests.ApiKeyCredentialsUpdate(self.testusername,
                                                          apikey)
        resp = self.identity_admin_client.update_api_key(self.user_id,
                                                         request_object)
        self.assertEqual(resp.status_code, 200)
        self.assertSchema(response=resp,
                          json_schema=credentials_json.get_apikey_response)
        self.assertEqual(resp.json()[const.NS_API_KEY_CREDENTIALS]
                                    [const.API_KEY], apikey)
        resp = self.identity_admin_client.get_api_key(self.user_id)
        self.assertEqual(resp.status_code, 200)
        self.assertSchema(response=resp,
                          json_schema=credentials_json.get_apikey_response)
        self.assertEqual(resp.json()[const.NS_API_KEY_CREDENTIALS]
                                    [const.API_KEY], apikey)

    def tearDown(self):
        # Delete all users created in the tests
        for user_id in self.user_ids:
            self.service_admin_client.delete_user(user_id=user_id)
        super(TestCredentials, self).tearDown()
