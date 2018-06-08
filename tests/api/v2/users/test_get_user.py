# -*- coding: utf-8 -*
import copy
import ddt
from nose.plugins.attrib import attr
from qe_coverage.opencafe_decorators import unless_coverage
from random import randrange

from tests.api.utils import func_helper
from tests.api.v2 import base
from tests.api.v2.schema import users as users_json

from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


@ddt.ddt
class GetUsersForTenantTests(base.TestBaseV2):

    @classmethod
    @unless_coverage
    def setUpClass(cls):
        super(GetUsersForTenantTests, cls).setUpClass()
        cls.updated_schema = copy.deepcopy(users_json.list_users)
        cls.updated_schema['properties'][const.USERS][
            'items']['required'] += [const.RAX_AUTH_CONTACTID]
        secret_q = 'test q'
        secret_a = 'test a'
        cls.secret_qa = {'question': secret_q, 'answer': secret_a}

    @unless_coverage
    def setUp(self):

        super(GetUsersForTenantTests, self).setUp()
        self.user_name = self.generate_random_string(
            pattern=const.USER_NAME_PATTERN)
        self.domain_id = func_helper.generate_randomized_domain_id(
            client=self.identity_admin_client)
        self.contact_id = randrange(start=const.CONTACT_ID_MIN,
                                    stop=const.CONTACT_ID_MAX)
        add_user_req = requests.UserAdd(
            user_name=self.user_name, domain_id=self.domain_id,
            contact_id=self.contact_id, secret_qa=self.secret_qa)

        # create user admin w/ contact ID
        self.user_admin_client = self.generate_client(
            parent_client=self.identity_admin_client,
            request_object=add_user_req)

        # To iterate through clients in the tests
        self.clients = {
            'identity_admin': self.identity_admin_client,
            'user_admin': self.user_admin_client
        }

    @unless_coverage
    @ddt.data('identity_admin', 'user_admin')
    @attr(type='smoke_alpha')
    def test_get_users_for_tenant_by_contact_id(self, user_type):

        option = {
            'contactId': self.contact_id
        }
        get_user_resp = self.clients[user_type].list_users_for_tenant(
            tenant_id=self.domain_id, option=option)
        self.assertSchema(
            response=get_user_resp, json_schema=self.updated_schema)

        # Not checking for 1-length list as there can be multiple users
        # with same contact ID, especially in higher environments
        usernames = [user[const.USERNAME] for user in (
            get_user_resp.json()[const.USERS])]
        self.assertIn(self.user_name, usernames)

    @unless_coverage
    def tearDown(self):
        self.delete_client(self.user_admin_client)
        super(GetUsersForTenantTests, self).tearDown()

    @classmethod
    @unless_coverage
    def tearDownClass(cls):
        super(GetUsersForTenantTests, cls).tearDownClass()
