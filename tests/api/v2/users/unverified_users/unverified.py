# -*- coding: utf-8 -*
from tests.api.utils import func_helper
from tests.api.v2 import base
from tests.api.v2.schema import users as users_json

from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


class TestBaseUnverifiedUser(base.TestBaseV2):

    """Unverified user test base class. """

    @classmethod
    def setUpClass(cls):
        """Class level set up for the tests

        Create users needed for the tests and generate clients for those users.
        """
        super(TestBaseUnverifiedUser, cls).setUpClass()

        cls.rcn = cls.test_config.unverified_user_rcn

        # Add Domain w/ RCN
        cls.domain_id = func_helper.generate_randomized_domain_id(
            client=cls.identity_admin_client)
        dom_req = requests.Domain(
            domain_name=cls.domain_id, domain_id=cls.domain_id, rcn=cls.rcn)
        add_dom_resp = cls.identity_admin_client.add_domain(dom_req)
        assert add_dom_resp.status_code == 201, (
            'domain was not created successfully')

        cls.user_admin_client = cls.generate_client(
            parent_client=cls.identity_admin_client,
            additional_input_data={'domain_id': cls.domain_id})
        cls.users = []

    def setUp(self):
        super(TestBaseUnverifiedUser, self).setUp()

    def create_unverified_user(self):
        test_email = self.generate_random_string(
            pattern=const.UNVERIFIED_EMAIL_PATTERN)
        create_unverified_user_req = requests.UnverifiedUser(
            email=test_email)
        create_resp = self.user_admin_client.create_unverified_user(
            request_object=create_unverified_user_req)
        self.assertEqual(create_resp.status_code, 201)
        self.assertSchema(
            response=create_resp,
            json_schema=users_json.add_unverified_user)
        self.assertEqual(
            create_resp.json()[const.USER][const.EMAIL],
            test_email)
        self.users.append(create_resp.json()[const.USER][const.ID])

        return create_resp.json()[const.USER][const.ID]

    @base.base.log_tearDown_error
    def tearDown(self):
        # Delete all resources created in the tests

        super(TestBaseUnverifiedUser, self).tearDown()

    @classmethod
    def tearDownClass(cls):
        super(TestBaseUnverifiedUser, cls).tearDownClass()
        cls.delete_client(cls.user_admin_client)
