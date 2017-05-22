# -*- coding: utf-8 -*

from tests.api.v2 import base
from tests.api.v2.models import factory
from tests.api.v2.models import responses
from tests.api.v2.schema import password as password_json

from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests
from tests.package.johny.v2 import client


class TestPasswordPolicy(base.TestBaseV2):
    """Tests Create, Get, Update, Delete password policies."""

    def setUp(self):
        super(TestPasswordPolicy, self).setUp()

        self.user_ids = []

        req_obj = requests.Domain(
            domain_name=self.generate_random_string(const.DOMAIN_PATTERN),
            domain_id=self.generate_random_string(const.ID_PATTERN),
            description=self.generate_random_string(const.DOMAIN_PATTERN),
            enabled=True)
        resp = self.identity_admin_client.add_domain(req_obj)
        self.assertEqual(resp.status_code, 201)
        self.domain_id = resp.json()[const.RAX_AUTH_DOMAIN][const.ID]

        password_policy = requests.PasswordPolicy(
            duration='PT1H', history_restriction=9)
        self.resp = self.identity_admin_client.add_update_password_policy(
            domain_id=self.domain_id, request_object=password_policy)

    def test_create_password_policy(self):
        self.assertEqual(self.resp.status_code, 200)
        self.assertSchema(self.resp, password_json.password_policy)

        # Add User to domain. Verify Auth returns password expiration header.
        add_user_object = factory.get_add_user_one_call_request_object(
            domainid=self.domain_id)
        resp = self.identity_admin_client.add_user(
            request_object=add_user_object)
        user_resp = responses.User(resp_json=resp.json())
        self.user_ids.append(user_resp.id)

        auth_req_obj = requests.AuthenticateWithPassword(
            user_name=user_resp.user_name,
            password=user_resp.password)

        user_admin_client = client.IdentityAPIClient(
            url=self.url,
            serialize_format=self.test_config.serialize_format,
            deserialize_format=self.test_config.deserialize_format)

        resp = user_admin_client.get_auth_token(request_object=auth_req_obj)
        self.assertEqual(resp.status_code, 200)
        self.assertIn(const.X_PASSWORD_EXPIRATION, resp.headers)

    def test_update_password_policy(self):
        self.assertEqual(self.resp.status_code, 200)

        # Update duration in password policy
        new_duration = 'PT24H'
        password_policy = requests.PasswordPolicy(
            duration=new_duration,
            history_restriction=9)
        resp = self.identity_admin_client.add_update_password_policy(
            domain_id=self.domain_id, request_object=password_policy)
        self.assertEqual(resp.status_code, 200)
        self.assertSchema(resp, password_json.password_policy)
        self.assertEqual(
            resp.json()[const.PASSWORD_POLICY][const.PASSWORD_DURATION],
            new_duration)

        # GET Updated Policy
        resp = self.identity_admin_client.get_password_policy(
            domain_id=self.domain_id)
        self.assertEqual(resp.status_code, 200)
        self.assertSchema(resp, password_json.password_policy)
        self.assertEqual(
            resp.json()[const.PASSWORD_POLICY][const.PASSWORD_DURATION],
            new_duration)

    def test_delete_password_policy(self):
        resp = self.identity_admin_client.delete_password_policy(
            domain_id=self.domain_id)
        self.assertEqual(resp.status_code, 204)

        resp = self.identity_admin_client.get_password_policy(
            domain_id=self.domain_id)
        self.assertEqual(resp.status_code, 404)

    def tearDown(self):
        super(TestPasswordPolicy, self).tearDown()
        for user_id in self.user_ids:
            self.identity_admin_client.delete_user(user_id=user_id)
        self.identity_admin_client.delete_domain(domain_id=self.domain_id)
