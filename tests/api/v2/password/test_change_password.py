# -*- coding: utf-8 -*
from nose.plugins.attrib import attr
from qe_coverage.opencafe_decorators import tags, unless_coverage

from tests.api.utils import func_helper
from tests.api.v2 import base
from tests.api.v2.models import factory
from tests.api.v2.models import responses

from tests.package.johny.v2.models import requests
from tests.package.johny.v2 import client


class TestChangePassword(base.TestBaseV2):
    """Tests Change password endpoint"""
    @unless_coverage
    def setUp(self):
        super(TestChangePassword, self).setUp()
        self.user_ids = []
        self.domain_ids = []

    @tags('positive', 'p0', 'smoke')
    @attr(type='smoke_alpha')
    def test_change_password(self):

        domain_id = func_helper.generate_randomized_domain_id(
            client=self.identity_admin_client)
        input_data = {
            'domain_id': domain_id
        }
        add_user_object = factory.get_add_user_request_object(
            input_data=input_data)
        resp = self.identity_admin_client.add_user(
            request_object=add_user_object)
        user_resp = responses.User(resp_json=resp.json())
        self.user_ids.append(user_resp.id)
        self.domain_ids.append(user_resp.domain_id)

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

    @base.base.log_tearDown_error
    @unless_coverage
    def tearDown(self):
        super(TestChangePassword, self).tearDown()
        for user_id in self.user_ids:
            resp = self.identity_admin_client.delete_user(user_id=user_id)
            self.assertEqual(
                resp.status_code, 204,
                msg='User with ID {0} failed to delete'.format(user_id))
        for domain_id in self.domain_ids:
            disable_domain_req = requests.Domain(enabled=False)
            self.identity_admin_client.update_domain(
                domain_id=domain_id, request_object=disable_domain_req)

            resp = self.identity_admin_client.delete_domain(
                domain_id=domain_id)
            self.assertEqual(
                resp.status_code, 204,
                msg='Domain with ID {0} failed to delete'.format(domain_id))
