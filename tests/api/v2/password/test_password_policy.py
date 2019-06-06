# -*- coding: utf-8 -*
import pytest
from qe_coverage.opencafe_decorators import tags, unless_coverage

from tests.api.utils import func_helper
from tests.api.v2 import base
from tests.api.v2.models import factory
from tests.api.v2.models import responses
from tests.api.v2.schema import password as password_json

from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests
from tests.package.johny.v2 import client


class TestPasswordPolicy(base.TestBaseV2):
    """Tests Create, Get, Update, Delete password policies."""
    @classmethod
    @unless_coverage
    def setUpClass(cls):
        super(TestPasswordPolicy, cls).setUpClass()

    @unless_coverage
    def setUp(self):
        super(TestPasswordPolicy, self).setUp()

        self.user_ids = []

        domain_id = func_helper.generate_randomized_domain_id(
            client=self.identity_admin_client)
        req_obj = requests.Domain(
            domain_name=self.generate_random_string(const.DOMAIN_PATTERN),
            domain_id=domain_id,
            description=self.generate_random_string(const.DOMAIN_PATTERN),
            enabled=True)
        resp = self.identity_admin_client.add_domain(req_obj)
        self.assertEqual(resp.status_code, 201)
        self.domain_id = resp.json()[const.RAX_AUTH_DOMAIN][const.ID]

        password_policy = requests.PasswordPolicy(
            duration='PT1H', history_restriction=9)
        self.resp = self.identity_admin_client.add_update_password_policy(
            domain_id=self.domain_id, request_object=password_policy)

    def verify_get_user_response(self, get_user_resp):

        self.assertIn(const.RAX_AUTH_PASSWORD_EXPIRATION,
                      get_user_resp.json()[const.USER])
        self.assertIsNotNone(get_user_resp.json()[const.USER][
                               const.RAX_AUTH_PASSWORD_EXPIRATION])

    @tags('positive', 'p0', 'smoke')
    @pytest.mark.smoke_alpha
    def test_create_password_policy(self):
        self.assertEqual(self.resp.status_code, 200)
        self.assertSchema(self.resp, password_json.password_policy)

        # Add User to domain. Verify Auth returns password expiration header.
        input_data = {
            'domain_id': self.domain_id
        }
        add_user_object = factory.get_add_user_request_object(
            input_data=input_data)
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

        # Verify get user returns password expiration
        user_admin_client.default_headers[const.X_AUTH_TOKEN] = resp.json()[
            const.ACCESS][const.TOKEN][const.ID]

        get_user_resp = user_admin_client.get_user(user_resp.id)
        self.verify_get_user_response(get_user_resp=get_user_resp)
        option = {
            const.NAME: user_resp.user_name
        }
        get_user_by_name_resp = user_admin_client.list_users(option=option)
        self.verify_get_user_response(get_user_resp=get_user_by_name_resp)

    @tags('positive', 'p0', 'smoke')
    @pytest.mark.smoke_alpha
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

    @tags('positive', 'p0', 'smoke')
    @pytest.mark.smoke_alpha
    def test_delete_password_policy(self):
        resp = self.identity_admin_client.delete_password_policy(
            domain_id=self.domain_id)
        self.assertEqual(resp.status_code, 204)

        resp = self.identity_admin_client.get_password_policy(
            domain_id=self.domain_id)
        self.assertEqual(resp.status_code, 404)

    @base.base.log_tearDown_error
    @unless_coverage
    def tearDown(self):
        super(TestPasswordPolicy, self).tearDown()
        for user_id in self.user_ids:
            resp = self.identity_admin_client.delete_user(user_id=user_id)
            self.assertEqual(
                resp.status_code, 204,
                msg='User with ID {0} failed to delete'.format(user_id))

        disable_domain_req = requests.Domain(enabled=False)
        self.identity_admin_client.update_domain(
            domain_id=self.domain_id, request_object=disable_domain_req)
        resp = self.identity_admin_client.delete_domain(
            domain_id=self.domain_id)
        self.assertEqual(resp.status_code, 204,
                         msg='Domain with ID {0} failed to delete'.format(
                           self.domain_id))

    @classmethod
    @unless_coverage
    def tearDownClass(cls):
        super(TestPasswordPolicy, cls).tearDownClass()
