# -*- coding: utf-8 -*
from nose.plugins.attrib import attr
from random import randrange
from qe_coverage.opencafe_decorators import tags, unless_coverage

from tests.api.v2 import base
from tests.api.v2.users.unverified_users import unverified
from tests.api.v2.schema import users as users_json

from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


class UnverifiedUsersTests(unverified.TestBaseUnverifiedUser):

    @classmethod
    @unless_coverage
    def setUpClass(cls):
        super(UnverifiedUsersTests, cls).setUpClass()

    @unless_coverage
    def setUp(self):
        super(UnverifiedUsersTests, self).setUp()

    @tags('positive', 'p0', 'regression')
    @attr(type='regression')
    def test_create_and_update_unverified_user(self):

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
        unverified_user_id = create_unverified_resp.json()[const.USER][
            const.ID]
        self.users.append(unverified_user_id)

        # Re-trying with same email fails for same domain
        create_unverified_resp = self.user_admin_client.create_unverified_user(
            request_object=create_unverified_user_req)
        self.assertEqual(create_unverified_resp.status_code, 409)

        contact_id = randrange(start=const.CONTACT_ID_MIN,
                               stop=const.CONTACT_ID_MAX)
        request_object = requests.UserUpdate(contact_id=contact_id)

        # User admin is not allowed to update contact id
        update_resp = self.user_admin_client.update_user(
            user_id=unverified_user_id, request_object=request_object
        )
        self.assertEqual(update_resp.status_code, 403)

        # Identity admin is allowed to update contact id
        update_resp = self.identity_admin_client.update_user(
            user_id=unverified_user_id, request_object=request_object
        )
        self.assertEqual(update_resp.status_code, 200)
        self.assertEqual(
            update_resp.json()[const.USER][const.RAX_AUTH_CONTACTID],
            str(contact_id))

        # update contact id again.
        another_contact_id = int(contact_id) - 1
        request_object = requests.UserUpdate(contact_id=another_contact_id)
        update_resp = self.identity_admin_client.update_user(
            user_id=unverified_user_id, request_object=request_object)
        self.assertEqual(update_resp.status_code, 200)
        self.assertEqual(
            update_resp.json()[const.USER][const.RAX_AUTH_CONTACTID],
            str(another_contact_id))

    @unless_coverage
    @base.base.log_tearDown_error
    def tearDown(self):
        super(UnverifiedUsersTests, self).tearDown()

    @classmethod
    @unless_coverage
    def tearDownClass(cls):
        super(UnverifiedUsersTests, cls).tearDownClass()
