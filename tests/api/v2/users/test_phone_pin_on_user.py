# -*- coding: utf-8 -*
import pytest

from qe_coverage.opencafe_decorators import tags, unless_coverage

from tests.api.utils import func_helper
import tests.api.base as api_base
from tests.api.v2 import base

from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


class TestPhonePinOnUser(base.TestBaseV2):
    """
    Tests for verifying,

    i. that new user creation adds a phone pin on the user.
    ii. reset phone pin functionality.
    iii. that new users can be added with a custom/specific phone pin.
    """

    @classmethod
    @unless_coverage
    def setUpClass(cls):
        """
        Class level set up for the tests
        Create users needed for the tests and generate clients for those users.
        """
        super(TestPhonePinOnUser, cls).setUpClass()

    @unless_coverage
    def setUp(self):
        super(TestPhonePinOnUser, self).setUp()
        self.identity_admin_ids = []
        self.user_admin_clients = []
        self.sub_user_ids = []

    @tags('positive', 'p0', 'smoke')
    @pytest.mark.smoke_alpha
    def test_user_admin_phone_pin(self):
        """
        Test to verify,
        1. add user admin call generates phone pin for user
        2. phone pin can/cannot be reset based on caller
        """
        user_name = self.generate_random_string()
        password = self.generate_random_string(pattern=const.PASSWORD_PATTERN)
        input_data = {
            'domain_id': self.generate_random_string(pattern='[\d]{7}')}
        request_object = requests.UserAdd(user_name=user_name,
                                          password=password, **input_data)
        resp = self.identity_admin_client.add_user(request_object)
        self.assertEqual(resp.status_code, 201)
        user_id = resp.json()[const.USER][const.ID]

        auth_req_obj = requests.AuthenticateWithPassword(
            user_name=user_name, password=password
        )
        resp = self.identity_admin_client.get_auth_token(
            request_object=auth_req_obj)
        phone_pin = resp.json()[
            const.ACCESS][const.USER][const.RAX_AUTH_PHONE_PIN]
        user_admin_token = resp.json()[const.ACCESS][const.TOKEN][const.ID]

        user_admin_client = self.generate_client(token=user_admin_token)
        self.user_admin_clients.append(user_admin_client)

        # verify phone pin exists for the user-admin when called on self
        get_user_admin_resp = user_admin_client.get_user(user_id=user_id)
        self.assertIn(const.RAX_AUTH_PHONE_PIN, get_user_admin_resp.json()[
            const.USER])
        phone_pin_length = len(
            get_user_admin_resp.json()[const.USER][const.RAX_AUTH_PHONE_PIN])
        self.assertEqual(phone_pin_length, 6)

        # verify phone pin is not returned for the user-admin when called with
        # identity-admin.
        get_user_admin_resp = self.identity_admin_client.get_user(
            user_id=user_id)
        self.assertNotIn(const.RAX_AUTH_PHONE_PIN, get_user_admin_resp.json()[
            const.USER])

        # verify if phone pin is not returned for list users
        list_users_resp = self.identity_admin_client.list_users()
        self.assertNotIn(const.RAX_AUTH_PHONE_PIN, list_users_resp.json()[
            const.USERS])

        # verify if phone pin is not returned
        resp = self.identity_admin_client.list_users(
            option={'domain_id': input_data['domain_id']})
        self.assertNotIn(const.RAX_AUTH_PHONE_PIN, resp.json()[
            const.USERS])

        # verify if phone pin is not returned for list users in domain
        resp = self.identity_admin_client.list_users_in_domain(
            domain_id=input_data['domain_id'])
        self.assertNotIn(const.RAX_AUTH_PHONE_PIN, resp.json()[
            const.USERS])

        # verify if phone pin is not returned with impersonation token
        impersonation_request_obj = requests.ImpersonateUser(
            user_name=user_name)

        # Get Impersonation Token
        resp = self.identity_admin_client.impersonate_user(
            request_data=impersonation_request_obj)
        self.assertEqual(resp.status_code, 200)

        token_id = resp.json()[const.ACCESS][const.TOKEN][const.ID]

        imp_client = self.generate_client(token=token_id)

        # verify if phone pin is not returned
        get_user_resp = imp_client.get_user(
            user_id=user_id)
        self.assertNotIn(const.RAX_AUTH_PHONE_PIN, get_user_resp.json()[
            const.USER])

        # Reset phone pin on user admin
        resp = self.identity_admin_client.reset_phone_pin(user_id=user_id)
        self.assertEqual(resp.status_code, 204)
        resp = self.identity_admin_client.get_auth_token(
            request_object=auth_req_obj)
        new_phone_pin = resp.json()[
            const.ACCESS][const.USER][const.RAX_AUTH_PHONE_PIN]
        self.assertNotEqual(new_phone_pin, phone_pin)

        # Cannot Reset own phone PIN
        resp = user_admin_client.reset_phone_pin(user_id=user_id)
        self.assertEqual(resp.status_code, 403)

    @tags('positive', 'p0', 'smoke')
    @pytest.mark.smoke_alpha
    def test_default_user_phone_pin(self):
        """
        Test to verify,
        1. add default user call generates phone pin for user.
        2. phone pin can/cannot be reset based on caller.
        """
        domain_id = func_helper.generate_randomized_domain_id(
            client=self.identity_admin_client)
        user_admin_client = self.generate_client(
            parent_client=self.identity_admin_client,
            additional_input_data={'domain_id': domain_id})
        self.user_admin_clients.append(user_admin_client)

        sub_user_name = self.generate_random_string(
            pattern=const.SUB_USER_PATTERN)
        sub_user_client = self.generate_client(
            parent_client=user_admin_client,
            additional_input_data={
                'domain_id': domain_id,
                'user_name': sub_user_name})
        sub_user_id = sub_user_client.default_headers[const.X_USER_ID]
        self.sub_user_ids.append(sub_user_id)
        get_user_resp = sub_user_client.get_user(user_id=sub_user_id)

        # check if phone pin exists for the sub-user when called on self
        self.assertIn(const.RAX_AUTH_PHONE_PIN, get_user_resp.json()[
            const.USER])
        phone_pin = get_user_resp.json()[const.USER][const.RAX_AUTH_PHONE_PIN]
        self.assertEqual(len(phone_pin), 6)
        get_user_resp = user_admin_client.get_user(user_id=sub_user_id)
        self.assertNotIn(const.RAX_AUTH_PHONE_PIN, get_user_resp.json()[
            const.USER])

        # Reset phone pin on user admin
        resp = user_admin_client.reset_phone_pin(user_id=sub_user_id)
        self.assertEqual(resp.status_code, 204)
        get_user_resp = sub_user_client.get_user(user_id=sub_user_id)
        new_phone_pin = get_user_resp.json()[
            const.USER][const.RAX_AUTH_PHONE_PIN]
        self.assertNotEqual(new_phone_pin, phone_pin)

        # Cannot Reset own phone PIN
        resp = sub_user_client.reset_phone_pin(user_id=sub_user_id)
        self.assertEqual(resp.status_code, 403)

    @tags('positive', 'p1', 'smoke')
    @pytest.mark.smoke_alpha
    @api_base.skip_if_no_service_admin_available
    def test_create_identity_admin_with_specific_phone_pin(self):
        """
        CID-1947:
        - Create a new identity admin, with a phone pin in the request
        - Check the phone pin is not returned in the create response
        - Check the phone pin verifies successfully
        """
        username = "identity-admin-%s" % self.generate_random_string()
        request_object = requests.UserAdd(
            user_name=username,
            domain_id=func_helper.generate_randomized_domain_id(
                client=self.service_admin_client
            ),
            email="%s@example.com" % username,
            enabled=True,
            phone_pin='134256',
        )
        self._create_user_with_phone_pin(
            # note: assuming this service_admin_client has phone-pin-admin
            client=self.service_admin_client,
            request_object=request_object,
            expected_roles=[const.IDENTITY_ADMIN_ROLE_NAME],
        )

    @tags('positive', 'p0', 'smoke')
    @pytest.mark.smoke_alpha
    def test_create_user_admin_with_specific_phone_pin(self):
        """
        CID-1947
        - Create a new user-admin, with a phone pin in the request
        - Check the phone pin is not returned in the create response
        - Check the phone pin verifies successfully
        """
        username = "user-admin-%s" % self.generate_random_string()
        request_object = requests.UserAdd(
            user_name=username,
            domain_id=func_helper.generate_randomized_domain_id(
                client=self.identity_admin_client,
            ),
            email="%s@example.com" % username,
            enabled=True,
            phone_pin='522436',
        )
        self._create_user_with_phone_pin(
            # note: assuming this identity_admin_client has phone-pin-admin
            client=self.identity_admin_client,
            request_object=request_object,
            expected_roles=[const.USER_ADMIN_ROLE_NAME],
        )

    @tags('positive', 'p1', 'smoke')
    @pytest.mark.smoke_alpha
    @api_base.skip_if_no_service_admin_available
    def test_create_sub_users_with_specific_phone_pin(self):
        """
        CID-1947
        - Create a user-admin and a user-manager with identity:phone-pin-admin
        - Use each of these to create a sub-user providing a phone pin in the
          create request
        - Check the phone pin is not returned in the create response
        - Check the phone pin verifies successfully
        """
        # Create a user-admin
        domain_id = func_helper.generate_randomized_domain_id(
            client=self.identity_admin_client)
        user_admin_client = self.generate_client(
            parent_client=self.identity_admin_client,
            additional_input_data={'domain_id': domain_id})
        self.user_admin_clients.append(user_admin_client)

        # Create a user-manager, using the user-admin
        user_manager_client = self.generate_client(
            parent_client=user_admin_client,
            additional_input_data={
                'domain_id': domain_id,
                'is_user_manager': True,
            })
        self.user_admin_clients.append(user_manager_client)

        # Add the phone-pin-admin role to the user-manager and user-admin
        # note: 403 unless we do this with the service_admin_client?
        phone_pin_role_id = self.get_role_id_by_name(
            self.service_admin_client, const.PHONE_PIN_ADMIN_ROLE_NAME,
        )
        for client in [user_admin_client, user_manager_client]:
            add_role_resp = self.service_admin_client.add_role_to_user(
                user_id=client.default_headers[const.X_USER_ID],
                role_id=phone_pin_role_id,
            )
            self.assertEqual(add_role_resp.status_code, 200)

        # Test that both user-admin and user-manage that also have
        # phone-pin-admin can create sub-users with phone pins
        for client in [user_admin_client, user_manager_client]:
            username = "default-%s" % self.generate_random_string()
            request_object = requests.UserAdd(
                user_name=username,
                domain_id=domain_id,
                email="%s@example.com" % username,
                enabled=True,
                phone_pin='791032',
            )
            self._create_user_with_phone_pin(
                client=client,
                request_object=request_object,
                expected_roles=[const.USER_DEFAULT_ROLE_NAME],
            )

    def _create_user_with_phone_pin(self, client, request_object,
                                    expected_roles):
        """
        :param client: the client to use to add the new user
        :param request_object: the AddUser object, for the request
        :param expected_roles: validate these role names exist on the new user
        """
        create_resp = client.add_user(request_object)
        self.assertEqual(create_resp.status_code, 201)

        # Store the user id for cleanup
        user_id = create_resp.json()[const.USER][const.ID]
        if const.IDENTITY_ADMIN_ROLE_NAME in expected_roles:
            self.identity_admin_ids.append(user_id)
        else:
            self.sub_user_ids.append(user_id)

        # Response should not contain the phone pin
        self.assertNotIn(const.RAX_AUTH_PHONE_PIN,
                         create_resp.json()[const.USER])

        # Authenticate as the new user
        user_name = create_resp.json()[const.USER][const.USERNAME]
        auth_obj = requests.AuthenticateWithPassword(
            user_name=user_name,
            password=create_resp.json()[const.USER][const.OS_KSADM_PASSWORD],
        )
        auth_resp = client.get_auth_token(auth_obj)
        self.assertEqual(auth_resp.status_code, 200)

        # Check that the user was created with the correct role for the test
        actual_roles = auth_resp.json()[const.ACCESS][const.USER][const.ROLES]
        for role in expected_roles:
            if not any(actual['name'] == role for actual in actual_roles):
                self.fail(
                    "Test not valid: Role '%s' not found in roles for user %s."
                    "\nRoles = %s" % (role, user_name, actual_roles)
                )

        # Check the phone pin is correct
        self.assertEqual(
            auth_resp.json()[const.ACCESS][const.USER][
                const.RAX_AUTH_PHONE_PIN],
            request_object.phone_pin,
        )

        # Verify the phone pin
        verify_resp = self.service_admin_client.verify_phone_pin_for_user(
            user_id=create_resp.json()[const.USER][const.ID],
            request_object=requests.PhonePin(
                phone_pin=request_object.phone_pin
            )
        )
        self.assertEqual(verify_resp.status_code, 200)
        self.assertTrue(verify_resp.json()[const.RAX_AUTH_VERIFY_PIN_RESULT])

    @unless_coverage
    @base.base.log_tearDown_error
    def tearDown(self):
        cleanup_failures = []

        # Delete all users created in the tests
        for id_ in reversed(self.sub_user_ids):
            delete_resp = self.identity_admin_client.delete_user(user_id=id_)
            if delete_resp.status_code != 204:
                cleanup_failures.append(
                    'User with ID {0} failed to delete'.format(id_)
                )
        for client in reversed(self.user_admin_clients):
            try:
                # note: delete_client also,
                #    - deletes any tenants in the domain, and
                #    - deletes the associated domain
                # the domain deletion will fail until all users in the domain
                # are deleted, so we may get an "error" message in tearDown.log
                # if there are multiple users - but everything is cleaned up
                # eventually.
                self.delete_client(client)
            except Exception as e:
                cleanup_failures.append(str(e))
        for id_ in reversed(self.identity_admin_ids):
            delete_resp = self.service_admin_client.delete_user(user_id=id_)
            if delete_resp.status_code != 204:
                cleanup_failures.append(
                    'Identity Admin with ID {0} failed to delete'.format(id_)
                )

        self.assertEqual(
            len(cleanup_failures), 0,
            "ERRORS:\n" + "\n".join(cleanup_failures)
        )

        super(TestPhonePinOnUser, self).tearDown()

    @classmethod
    @unless_coverage
    @base.base.log_tearDown_error
    def tearDownClass(cls):
        super(TestPhonePinOnUser, cls).tearDownClass()
