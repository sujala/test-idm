# -*- coding: utf-8 -*

from tests.api.v2 import base
from tests.api.v2.models import factory
from tests.api.v2.models import responses

from tests.package.johny.v2.models import requests
from tests.package.johny.v2 import client


class TestChangePassword(base.TestBaseV2):
    """Tests Change password endpoint"""

    def setUp(self):
        super(TestChangePassword, self).setUp()
        self.user_ids = []

    def test_change_password(self):

        add_user_object = factory.get_add_user_one_call_request_object()
        resp = self.identity_admin_client.add_user(
            request_object=add_user_object)
        user_resp = responses.User(resp_json=resp.json())
        self.user_ids.append(user_resp.id)

        user_admin_client = client.IdentityAPIClient(
            url=self.url,
            serialize_format=self.test_config.serialize_format,
            deserialize_format=self.test_config.deserialize_format)

        new_password = 'CatMeow123!'
        change_password_req_obj = requests.ChangePassword(
            user_name=user_resp.user_name,
            current_password=user_resp.password,
            new_password=new_password)
        resp = user_admin_client.change_password(
            request_object=change_password_req_obj)
        self.assertEqual(resp.status_code, 204)

        # Verify password is updated - Auth with new password
        auth_req_obj = requests.AuthenticateWithPassword(
            user_name=user_resp.user_name,
            password=new_password)
        resp = user_admin_client.get_auth_token(request_object=auth_req_obj)
        self.assertEqual(resp.status_code, 200)

    def tearDown(self):
        super(TestChangePassword, self).tearDown()
        for user_id in self.user_ids:
            self.identity_admin_client.delete_user(user_id=user_id)
