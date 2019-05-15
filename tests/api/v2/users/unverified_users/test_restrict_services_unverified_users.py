# -*- coding: utf-8 -*
import pytest
from qe_coverage.opencafe_decorators import tags, unless_coverage

from tests.api.utils import func_helper
from tests.api.v2 import base

from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


class RestrictServicesUnverifiedUsersTests(base.TestBaseV2):

    @classmethod
    @unless_coverage
    def setUpClass(cls):
        super(RestrictServicesUnverifiedUsersTests, cls).setUpClass()
        cls.rcn = cls.test_config.unverified_user_rcn

        # Add Domain w/ RCN
        cls.domain_id = func_helper.generate_randomized_domain_id(
            client=cls.identity_admin_client)
        dom_req = requests.Domain(
            domain_name=cls.domain_id,
            domain_id=cls.domain_id, rcn=cls.rcn)
        add_dom_resp = cls.identity_admin_client.add_domain(dom_req)
        assert add_dom_resp.status_code == 201, (
            'domain was not created successfully')
        cls.user_admin_client = cls.generate_client(
            parent_client=cls.identity_admin_client,
            additional_input_data={'domain_id': cls.domain_id})
        cls.users = []
        test_email = cls.generate_random_string(
            pattern=const.UNVERIFIED_EMAIL_PATTERN)
        create_unverified_user_req = requests.UnverifiedUser(
            email=test_email, domain_id=cls.domain_id)
        create_unverified_resp = cls.user_admin_client.create_unverified_user(
            request_object=create_unverified_user_req)
        cls.user_id = create_unverified_resp.json()[const.USER][const.ID]
        cls.users.append(cls.user_id)

    @unless_coverage
    def setUp(self):
        super(RestrictServicesUnverifiedUsersTests, self).setUp()

    @tags('negative', 'p1', 'regression')
    @pytest.mark.regression
    def test_reset_apikey(self):
        resp = self.identity_admin_client.reset_api_key(self.user_id)
        self.assertEqual(
            resp.status_code, 403)
        self.assertEqual(
            resp.json()[const.FORBIDDEN][const.MESSAGE],
            "Error code: 'GEN-006'; Operation not permitted"
            " on an unverified user.")

    @unless_coverage
    def tearDown(self):
        super(RestrictServicesUnverifiedUsersTests, self).tearDown()

    @classmethod
    @unless_coverage
    @base.base.log_tearDown_error
    def tearDownClass(cls):
        # Delete all users created in the setUpClass
        for id in cls.users:
            cls.identity_admin_client.delete_user(user_id=id)
        cls.delete_client(client=cls.user_admin_client,
                          parent_client=cls.identity_admin_client)
        super(RestrictServicesUnverifiedUsersTests, cls).tearDownClass()
