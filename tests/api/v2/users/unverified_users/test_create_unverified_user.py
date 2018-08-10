# -*- coding: utf-8 -*
from qe_coverage.opencafe_decorators import tags, unless_coverage


from tests.api.utils import func_helper
from tests.api.v2 import base
from tests.api.v2.schema import users as users_json

from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


class UnverifiedUsersTests(base.TestBaseV2):

    @classmethod
    @unless_coverage
    def setUpClass(cls):
        super(UnverifiedUsersTests, cls).setUpClass()
        cls.rcn = cls.test_config.unverified_user_rcn

        # Add Domain w/ RCN
        cls.domain_id = func_helper.generate_randomized_domain_id(
            client=cls.identity_admin_client)
        dom_req = requests.Domain(
            domain_name=cls.domain_id, domain_id=cls.domain_id, rcn=cls.rcn)
        add_dom_resp = cls.identity_admin_client.add_domain(dom_req)
        assert add_dom_resp.status_code == 201, (
            'domain was not created successfully')

    @unless_coverage
    def setUp(self):
        super(UnverifiedUsersTests, self).setUp()
        self.user_admin_client = self.generate_client(
            parent_client=self.identity_admin_client,
            additional_input_data={'domain_id': self.domain_id})
        self.users = []

    @tags('positive', 'p0', 'regression')
    def test_create_unverified_user(self):

        test_email = self.generate_random_string(
            pattern=const.UNVERIFIED_EMAIL_PATTERN)
        create_unverified_user_req = requests.UnverifiedUser(
            email=test_email)
        create_unverified_resp = self.user_admin_client.create_unverified_user(
            request_object=create_unverified_user_req)
        self.assertEqual(create_unverified_resp.status_code, 201)
        self.assertSchema(response=create_unverified_resp,
                          json_schema=users_json.add_unverified_user)
        self.assertEqual(
            create_unverified_resp.json()[const.USER][const.EMAIL], test_email)
        self.users.append(create_unverified_resp.json()[const.USER][const.ID])

        # Re-trying with same email fails for same domain
        create_unverified_resp = self.user_admin_client.create_unverified_user(
            request_object=create_unverified_user_req)
        self.assertEqual(create_unverified_resp.status_code, 409)

    @unless_coverage
    @base.base.log_tearDown_error
    def tearDown(self):
        super(UnverifiedUsersTests, self).tearDown()
        for user_id in self.users:
            resp = self.user_admin_client.delete_user(user_id=user_id)
            self.assertEqual(
                resp.status_code, 204,
                msg='User with ID {0} failed to delete'.format(user_id))
        self.delete_client(self.user_admin_client)

    @classmethod
    @unless_coverage
    def tearDownClass(cls):
        super(UnverifiedUsersTests, cls).tearDownClass()
