from qe_coverage.opencafe_decorators import tags, unless_coverage


from tests.api.utils import func_helper
from tests.api.v2 import base
from tests.api.v2.schema import users as users_json

from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


class InviteUnverifiedUserTest(base.TestBaseV2):

    @classmethod
    @unless_coverage
    def setUpClass(cls):
        super(InviteUnverifiedUserTest, cls).setUpClass()
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
        super(InviteUnverifiedUserTest, self).setUp()
        self.ua_client = self.generate_client(
            parent_client=self.identity_admin_client,
            additional_input_data={'domain_id': self.domain_id})
        self.users = []

    def create_unverified_user(self):

        test_email = self.generate_random_string(
            pattern=const.UNVERIFIED_EMAIL_PATTERN)
        create_unverified_user_req = requests.UnverifiedUser(
            email=test_email)
        create_resp = self.ua_client.create_unverified_user(
            request_object=create_unverified_user_req)
        self.assertEqual(create_resp.status_code, 201)
        self.assertSchema(response=create_resp,
                          json_schema=users_json.add_unverified_user)
        self.assertEqual(
            create_resp.json()[const.USER][const.EMAIL],
            test_email)
        self.users.append(create_resp.json()[const.USER][const.ID])

        return create_resp.json()[const.USER][const.ID]

    @tags('positive', 'p0', 'regression')
    def test_invite_unverified_user(self):

        user_id = self.create_unverified_user()

        invite_resp = self.ua_client.invite_unverified_user(
            user_id=user_id)
        self.assertEqual(invite_resp.status_code, 200)
        self.assertSchema(response=invite_resp,
                          json_schema=users_json.invite_unverified_user)
        self.assertEqual(
            invite_resp.json()[const.RAX_AUTH_INVITE][const.USER_ID],
            user_id)

        get_user_resp = self.ua_client.get_user(user_id=user_id)
        self.assertEqual(invite_resp.status_code, 200)
        self.assertEqual(
            get_user_resp.json()[const.USER][const.RAX_AUTH_UNVERIFIED],
            True)
        self.assertEqual(
            get_user_resp.json()[const.USER][const.ENABLED],
            False)

    @unless_coverage
    @base.base.log_tearDown_error
    def tearDown(self):
        super(InviteUnverifiedUserTest, self).tearDown()
        for user_id in self.users:
            resp = self.ua_client.delete_user(user_id=user_id)
            self.assertEqual(
                resp.status_code, 204,
                msg='User with ID {0} failed to delete'.format(user_id))
        self.delete_client(self.ua_client)

    @classmethod
    @unless_coverage
    def tearDownClass(cls):
        super(InviteUnverifiedUserTest, cls).tearDownClass()
