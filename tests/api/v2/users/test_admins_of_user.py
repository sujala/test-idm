# -*- coding: utf-8 -*
import ddt
import time

from tests.api import constants as const
from tests.api.utils import saml_helper
from tests.api.v2 import base
from tests.api.v2.models import requests
from tests.api.v2.schema import users as users_json

ERROR_MESSAGE_USER_NOT_FOUND = 'User {0} not found'


@ddt.ddt
class TestAdminsOfUser(base.TestBaseV2):

    @classmethod
    def setUpClass(cls):

        super(TestAdminsOfUser, cls).setUpClass()

        cls.common_user_admin_client = cls.generate_client(
            parent_client=cls.identity_admin_client,
            additional_input_data={
                'domain_id': cls.generate_random_string(pattern='[\d]{7}')})
        cls.test_email = "random@rackspace.com"
        cls.issuer = 'http://identityqe.rackspace.com'

    def setUp(cls):
        cls.user_ids = []

    @ddt.file_data('data_get_admins_for_fed_user.json')
    def test_admins_of_fed_user(self, test_data):

        user_name = self.generate_random_string()
        domain_id = self.generate_random_string(pattern='[\d]{7}')
        user_object = requests.UserAdd(
            user_name=user_name,
            domain_id=domain_id)
        resp = self.identity_admin_client.add_user(user_object)
        self.assertEqual(resp.status_code, 201)
        domain_id = resp.json()[const.USER][const.RAX_AUTH_DOMAIN_ID]
        user_id = resp.json()[const.USER][const.ID]
        self.user_ids.append(user_id)

        password = resp.json()[const.USER][const.OS_KSADM_PASSWORD]
        auth = self.identity_admin_client.get_auth_token(
            user=user_name, password=password)
        user_admin_token = auth.json()[const.ACCESS][const.TOKEN][const.ID]
        user_admin_client = self.generate_client_with_x_auth_token(
            x_auth_token=user_admin_token)

        # Get admins of user-admin user using its own token
        resp = user_admin_client.get_admins_for_a_user(user_id)
        self.assertEqual(resp.status_code, 200)
        self.assertSchema(
            response=resp, json_schema=users_json.get_admins_of_user)

        subject = self.generate_random_string(
            pattern='fed[\-]user[\-][\d\w]{12}')
        fed_input_data = test_data['fed_input']
        base64_url_encode = fed_input_data['base64_url_encode']
        new_url = fed_input_data['new_url']
        content_type = fed_input_data['content_type']

        cert = saml_helper.create_saml_assertion(
            domain=domain_id, subject=subject, issuer=self.issuer,
            email=self.test_email, base64_url_encode=base64_url_encode)

        auth = self.identity_admin_client.auth_with_saml(
            saml=cert, content_type=content_type,
            base64_url_encode=base64_url_encode, new_url=new_url)
        fed_user_id = auth.json()[const.ACCESS][const.USER][const.ID]
        fed_user_auth_token = auth.json()[const.ACCESS][const.TOKEN][const.ID]
        fed_user_client = self.generate_client_with_x_auth_token(
            x_auth_token=fed_user_auth_token)

        # Get admins of fed user using identity admin's token
        admins_of_fed_user = self.identity_admin_client.get_admins_for_a_user(
            user_id=fed_user_id)
        self.assertEqual(admins_of_fed_user.status_code, 200)
        admin_user_id = admins_of_fed_user.json()[const.USERS][0][const.ID]
        self.assertSchema(response=admins_of_fed_user,
                          json_schema=users_json.get_admins_of_user)

        self.assertEqual(admin_user_id, user_id)

        # Get admins of fed user using user admin's token
        admins_of_fed_user = user_admin_client.get_admins_for_a_user(
            user_id=fed_user_id)
        self.assertEqual(admins_of_fed_user.status_code, 200)
        admin_user_id = admins_of_fed_user.json()[const.USERS][0][const.ID]
        self.assertEqual(admin_user_id, user_id)
        self.assertSchema(response=admins_of_fed_user,
                          json_schema=users_json.get_admins_of_user)

        # Get admins of fed user using fed user's token
        admins_of_fed_user = fed_user_client.get_admins_for_a_user(
            user_id=fed_user_id)
        self.assertEqual(admins_of_fed_user.status_code, 200)
        admin_user_id = admins_of_fed_user.json()[const.USERS][0][const.ID]
        self.assertEqual(admin_user_id, user_id)
        self.assertSchema(response=admins_of_fed_user,
                          json_schema=users_json.get_admins_of_user)

        # Get admins of user admin user using fed user's token
        admins_using_fed_user = fed_user_client.get_admins_for_a_user(
            user_id=user_id)
        self.assertEqual(admins_using_fed_user.status_code, 403)
        self.assertEqual(admins_using_fed_user.json()['forbidden'][
                             'message'], 'Not Authorized')

        # Get admins of fed user using user admin token from other domain
        admins_using_fed_user = (
            self.common_user_admin_client.get_admins_for_a_user(
                user_id=fed_user_id))
        self.assertEqual(admins_using_fed_user.status_code, 403)
        self.assertEqual(admins_using_fed_user.json()['forbidden'][
                             'message'], 'Not Authorized')

        # Get admins of user admin using user admin token from other domain
        admins_using_fed_user = (
            self.common_user_admin_client.get_admins_for_a_user(
                user_id=user_id))
        self.assertEqual(admins_using_fed_user.status_code, 403)
        self.assertEqual(admins_using_fed_user.json()['forbidden'][
                             'message'], 'Not Authorized')

    @ddt.file_data('data_get_admins_for_fed_user.json')
    def test_admins_of_fed_user_using_another_fed_user(self, test_data):

        user_name = self.generate_random_string()
        domain_id = self.generate_random_string(pattern='[\d]{7}')
        user_object = requests.UserAdd(
            user_name=user_name,
            domain_id=domain_id
        )
        resp = self.identity_admin_client.add_user(user_object)
        self.assertEqual(resp.status_code, 201)
        domain_id = resp.json()[const.USER][const.RAX_AUTH_DOMAIN_ID]
        user_id = resp.json()[const.USER][const.ID]
        self.user_ids.append(user_id)

        subject = self.generate_random_string(
            pattern='fed[\-]user[\-][\d\w]{12}')
        fed_input_data = test_data['fed_input']
        base64_url_encode = fed_input_data['base64_url_encode']
        new_url = fed_input_data['new_url']
        content_type = fed_input_data['content_type']

        cert = saml_helper.create_saml_assertion(
            domain=domain_id, subject=subject, issuer=self.issuer,
            email=self.test_email, base64_url_encode=base64_url_encode)

        auth = self.identity_admin_client.auth_with_saml(
            saml=cert, content_type=content_type,
            base64_url_encode=base64_url_encode, new_url=new_url)
        fed_user_id = auth.json()[const.ACCESS][const.USER][const.ID]
        fed_user_auth_token = auth.json()[const.ACCESS][const.TOKEN][const.ID]
        fed_user_client = self.generate_client_with_x_auth_token(
            x_auth_token=fed_user_auth_token)

        # creating second fed user under same domain
        subject_2 = self.generate_random_string(
            pattern='fed[\-]user[\-][\d\w]{12}')

        cert = saml_helper.create_saml_assertion(
            domain=domain_id, subject=subject_2, issuer=self.issuer,
            email=self.test_email, base64_url_encode=base64_url_encode)

        auth = self.identity_admin_client.auth_with_saml(
            saml=cert, content_type=content_type,
            base64_url_encode=base64_url_encode, new_url=new_url)
        fed_user_id_2 = auth.json()[const.ACCESS][const.USER][const.ID]
        fed_user_auth_token_2 = auth.json()[const.ACCESS][const.TOKEN][
            const.ID]
        fed_user_client_2 = self.generate_client_with_x_auth_token(
            x_auth_token=fed_user_auth_token_2)

        # Get admins of second fed user using first fed user's token
        admins_of_fed_user_2 = fed_user_client.get_admins_for_a_user(
            user_id=fed_user_id_2)
        self.assertEqual(admins_of_fed_user_2.status_code, 403)
        self.assertEqual(admins_of_fed_user_2.json()['forbidden'][
                             'message'], 'Not Authorized')

        # Get admins of first fed user using second fed user's token
        admins_using_fed_user = fed_user_client_2.get_admins_for_a_user(
            user_id=fed_user_id)
        self.assertEqual(admins_using_fed_user.status_code, 403)
        self.assertEqual(admins_using_fed_user.json()['forbidden'][
                             'message'], 'Not Authorized')

    @ddt.file_data('data_get_admins_for_fed_user.json')
    def test_disabled_admin_of_fed_user(self, test_data):

        user_name = self.generate_random_string()
        domain_id = self.generate_random_string(pattern='[\d]{7}')
        user_object = requests.UserAdd(
            user_name=user_name,
            domain_id=domain_id
        )
        resp = self.identity_admin_client.add_user(user_object)
        self.assertEqual(resp.status_code, 201)
        domain_id = resp.json()[const.USER][const.RAX_AUTH_DOMAIN_ID]
        user_id = resp.json()[const.USER][const.ID]
        self.user_ids.append(user_id)

        subject = self.generate_random_string(
            pattern='fed[\-]user[\-][\d\w]{12}')
        fed_input_data = test_data['fed_input']
        base64_url_encode = fed_input_data['base64_url_encode']
        new_url = fed_input_data['new_url']
        content_type = fed_input_data['content_type']

        cert = saml_helper.create_saml_assertion(
            domain=domain_id, subject=subject, issuer=self.issuer,
            email=self.test_email, base64_url_encode=base64_url_encode)

        auth = self.identity_admin_client.auth_with_saml(
            saml=cert, content_type=content_type,
            base64_url_encode=base64_url_encode, new_url=new_url)
        fed_user_id = auth.json()[const.ACCESS][const.USER][const.ID]
        fed_user_auth_token = auth.json()[const.ACCESS][const.TOKEN][const.ID]

        # Disable the user admin user
        update_user_object = requests.UserUpdate(enabled=False)
        update_user_resp = self.identity_admin_client.update_user(
            user_id=user_id, request_object=update_user_object)
        self.assertEqual(update_user_resp.status_code, 200)

        # Get admins of fed user using identity admin's token
        admins_of_fed_user = self.identity_admin_client.get_admins_for_a_user(
            user_id=fed_user_id)
        self.assertEqual(admins_of_fed_user.status_code, 200)
        admin_users = admins_of_fed_user.json()[const.USERS]
        self.assertEqual(admin_users, [])

        # Validate fed user's token
        validate_resp = self.identity_admin_client.validate_token(
            token_id=fed_user_auth_token)
        self.assertEqual(validate_resp.status_code, 404)

    @ddt.file_data('data_get_admins_for_fed_user.json')
    def test_admin_of_fed_user_using_default_user(self, test_data):

        user_name = self.generate_random_string()
        domain_id = self.generate_random_string(pattern='[\d]{7}')
        user_object = requests.UserAdd(
            user_name=user_name,
            domain_id=domain_id
        )
        resp = self.identity_admin_client.add_user(user_object)
        self.assertEqual(resp.status_code, 201)
        domain_id = resp.json()[const.USER][const.RAX_AUTH_DOMAIN_ID]
        user_id = resp.json()[const.USER][const.ID]
        self.user_ids.append(user_id)

        password = resp.json()[const.USER][const.OS_KSADM_PASSWORD]
        auth = self.identity_admin_client.get_auth_token(
            user=user_name, password=password)
        user_admin_token = auth.json()[const.ACCESS][const.TOKEN][const.ID]
        user_admin_client = self.generate_client_with_x_auth_token(
            x_auth_token=user_admin_token)

        sub_user_name = self.generate_random_string(
            pattern='sub[\-]user[\d\w]{12}')
        sub_user_client = self.generate_client(
            parent_client=user_admin_client,
            additional_input_data={
                'domain_id': domain_id,
                'user_name': sub_user_name})

        option = {
            'name': sub_user_name
        }
        list_resp = self.identity_admin_client.list_users(option=option)
        sub_user_id = list_resp.json()[const.USER][const.ID]

        subject = self.generate_random_string(
            pattern='fed[\-]user[\-][\d\w]{12}')
        fed_input_data = test_data['fed_input']
        base64_url_encode = fed_input_data['base64_url_encode']
        new_url = fed_input_data['new_url']
        content_type = fed_input_data['content_type']

        cert = saml_helper.create_saml_assertion(
            domain=domain_id, subject=subject, issuer=self.issuer,
            email=self.test_email, base64_url_encode=base64_url_encode)

        auth = self.identity_admin_client.auth_with_saml(
            saml=cert, content_type=content_type,
            base64_url_encode=base64_url_encode, new_url=new_url)
        fed_user_id = auth.json()[const.ACCESS][const.USER][const.ID]
        fed_user_auth_token = auth.json()[const.ACCESS][const.TOKEN][const.ID]
        fed_user_client = self.generate_client_with_x_auth_token(
            x_auth_token=fed_user_auth_token)

        # Get admins of fed user using default user's token
        admins_of_fed_user = sub_user_client.get_admins_for_a_user(
            user_id=fed_user_id)
        self.assertEqual(admins_of_fed_user.status_code, 403)
        self.assertEqual(admins_of_fed_user.json()['forbidden'][
                             'message'], 'Not Authorized')

        # Get admins of sub-user using fed user's token
        admins_of_sub_user = fed_user_client.get_admins_for_a_user(
            user_id=sub_user_id)
        self.assertEqual(admins_of_sub_user.status_code, 403)
        self.assertEqual(admins_of_sub_user.json()['forbidden'][
                             'message'], 'Not Authorized')

        # Get admins of sub-user using self token
        admins_of_sub_user = sub_user_client.get_admins_for_a_user(
            user_id=sub_user_id)
        self.assertEqual(admins_of_sub_user.status_code, 200)
        admin_user_id = admins_of_sub_user.json()[const.USERS][0][const.ID]
        self.assertSchema(response=admins_of_sub_user,
                          json_schema=users_json.get_admins_of_user)
        self.assertEqual(admin_user_id, user_id)

    @ddt.file_data('data_get_admins_for_fed_user.json')
    def test_admin_of_fed_user_using_user_manager(self, test_data):

        user_name = self.generate_random_string()
        domain_id = self.generate_random_string(pattern='[\d]{7}')
        user_object = requests.UserAdd(
            user_name=user_name,
            domain_id=domain_id
        )
        resp = self.identity_admin_client.add_user(user_object)
        self.assertEqual(resp.status_code, 201)
        domain_id = resp.json()[const.USER][const.RAX_AUTH_DOMAIN_ID]
        user_id = resp.json()[const.USER][const.ID]
        self.user_ids.append(user_id)

        password = resp.json()[const.USER][const.OS_KSADM_PASSWORD]
        auth = self.identity_admin_client.get_auth_token(
            user=user_name, password=password)
        user_admin_token = auth.json()[const.ACCESS][const.TOKEN][const.ID]
        user_admin_client = self.generate_client_with_x_auth_token(
            x_auth_token=user_admin_token)

        user_manager_name = self.generate_random_string(
            pattern='user[\-]manager[\d\w]{12}')
        user_manager_client = self.generate_client(
            parent_client=user_admin_client,
            additional_input_data={
                'domain_id': domain_id,
                'user_name': user_manager_name,
                'is_user_manager': True})

        option = {
            'name': user_manager_name
        }
        list_resp = self.identity_admin_client.list_users(option=option)
        user_manager_id = list_resp.json()[const.USER][const.ID]

        subject = self.generate_random_string(
            pattern='fed[\-]user[\-][\d\w]{12}')
        fed_input_data = test_data['fed_input']
        base64_url_encode = fed_input_data['base64_url_encode']
        new_url = fed_input_data['new_url']
        content_type = fed_input_data['content_type']

        cert = saml_helper.create_saml_assertion(
            domain=domain_id, subject=subject, issuer=self.issuer,
            email=self.test_email, base64_url_encode=base64_url_encode)

        auth = self.identity_admin_client.auth_with_saml(
            saml=cert, content_type=content_type,
            base64_url_encode=base64_url_encode, new_url=new_url)
        fed_user_id = auth.json()[const.ACCESS][const.USER][const.ID]
        fed_user_auth_token = auth.json()[const.ACCESS][const.TOKEN][const.ID]
        fed_user_client = self.generate_client_with_x_auth_token(
            x_auth_token=fed_user_auth_token)

        # Get admins of fed user using user manager's token
        # This should be a 200 and hence is a defect. This behavior may
        # change once CID-334 is fixed
        admins_of_fed_user = user_manager_client.get_admins_for_a_user(
            user_id=fed_user_id)
        self.assertEqual(admins_of_fed_user.status_code, 403)
        self.assertEqual(admins_of_fed_user.json()['forbidden'][
                             'message'], 'Not Authorized')

        # Get admins of user manager using fed user's token
        admins_of_user_manager = fed_user_client.get_admins_for_a_user(
            user_id=user_manager_id)
        self.assertEqual(admins_of_user_manager.status_code, 403)
        self.assertEqual(admins_of_user_manager.json()['forbidden'][
                             'message'], 'Not Authorized')

        # Get admins of user manager self token
        admins_of_user_manager = user_manager_client.get_admins_for_a_user(
            user_id=user_manager_id)
        self.assertEqual(admins_of_user_manager.status_code, 200)
        admin_user_id = admins_of_user_manager.json()[const.USERS][0][const.ID]
        self.assertSchema(response=admins_of_user_manager,
                          json_schema=users_json.get_admins_of_user)
        self.assertEqual(admin_user_id, user_id)

    @ddt.file_data('data_get_admins_for_fed_user.json')
    def test_admins_of_expired_fed_user(self, test_data):

        user_name = self.generate_random_string()
        domain_id = self.generate_random_string(pattern='[\d]{7}')
        user_object = requests.UserAdd(
            user_name=user_name,
            domain_id=domain_id
        )
        resp = self.identity_admin_client.add_user(user_object)
        self.assertEqual(resp.status_code, 201)
        domain_id = resp.json()[const.USER][const.RAX_AUTH_DOMAIN_ID]
        user_id = resp.json()[const.USER][const.ID]
        self.user_ids.append(user_id)

        subject = self.generate_random_string(
            pattern='fed[\-]user[\-][\d\w]{12}')
        fed_input_data = test_data['fed_input']
        base64_url_encode = fed_input_data['base64_url_encode']
        new_url = fed_input_data['new_url']
        content_type = fed_input_data['content_type']
        fed_user_lifetime = 3

        cert = saml_helper.create_saml_assertion(
            domain=domain_id, subject=subject, issuer=self.issuer,
            email=self.test_email, base64_url_encode=base64_url_encode,
            seconds_to_expiration=fed_user_lifetime)

        auth = self.identity_admin_client.auth_with_saml(
            saml=cert, content_type=content_type,
            base64_url_encode=base64_url_encode, new_url=new_url)
        fed_user_id = auth.json()[const.ACCESS][const.USER][const.ID]
        fed_user_auth_token = auth.json()[const.ACCESS][const.TOKEN][const.ID]
        time.sleep(fed_user_lifetime + 1)

        # Get admins of fed user using identity admin's token. Fed user
        # doesn't expire but the token expires with 'seconds_to_expiration'
        # parameter
        admins_of_fed_user = self.identity_admin_client.get_admins_for_a_user(
            user_id=fed_user_id)
        self.assertEqual(admins_of_fed_user.status_code, 200)
        admin_user_id = admins_of_fed_user.json()[const.USERS][0][const.ID]
        self.assertSchema(response=admins_of_fed_user,
                          json_schema=users_json.get_admins_of_user)
        self.assertEqual(admin_user_id, user_id)

        # Validate fed user's token
        validate_resp = self.identity_admin_client.validate_token(
            token_id=fed_user_auth_token)
        self.assertEqual(validate_resp.status_code, 404)

        # create logout saml for user
        logout_saml = saml_helper.create_saml_logout(
            issuer=self.issuer, name_id=subject, base64_url_encode=True)

        logout_response = self.identity_admin_client.logout_with_saml(
            saml=logout_saml)
        self.assertEqual(logout_response.status_code, 200)

        # Get admins of fed user using identity admin's token
        admins_of_fed_user = self.identity_admin_client.get_admins_for_a_user(
            user_id=fed_user_id)
        self.assertEqual(admins_of_fed_user.status_code, 404)
        self.assertEqual(admins_of_fed_user.json()['itemNotFound']['message'],
                         ERROR_MESSAGE_USER_NOT_FOUND.format(fed_user_id))

    @ddt.file_data('data_get_admins_for_fed_user.json')
    def test_deleted_admin_of_fed_user(self, test_data):

        user_name = self.generate_random_string()
        domain_id = self.generate_random_string(pattern='[\d]{7}')
        user_object = requests.UserAdd(
            user_name=user_name,
            domain_id=domain_id
        )
        resp = self.identity_admin_client.add_user(user_object)
        self.assertEqual(resp.status_code, 201)
        domain_id = resp.json()[const.USER][const.RAX_AUTH_DOMAIN_ID]
        user_id = resp.json()[const.USER][const.ID]
        self.user_ids.append(user_id)

        subject = self.generate_random_string(
            pattern='fed[\-]user[\-][\d\w]{12}')
        fed_input_data = test_data['fed_input']
        base64_url_encode = fed_input_data['base64_url_encode']
        new_url = fed_input_data['new_url']
        content_type = fed_input_data['content_type']

        cert = saml_helper.create_saml_assertion(
            domain=domain_id, subject=subject, issuer=self.issuer,
            email=self.test_email, base64_url_encode=base64_url_encode)

        auth = self.identity_admin_client.auth_with_saml(
            saml=cert, content_type=content_type,
            base64_url_encode=base64_url_encode, new_url=new_url)
        fed_user_id = auth.json()[const.ACCESS][const.USER][const.ID]
        fed_user_auth_token = auth.json()[const.ACCESS][const.TOKEN][const.ID]

        # Delete the user admin user
        delete_user_resp = self.identity_admin_client.delete_user(
            user_id=user_id)
        self.assertEqual(delete_user_resp.status_code, 204)

        # Get admins of fed user using identity admin's token
        admins_of_fed_user = self.identity_admin_client.get_admins_for_a_user(
            user_id=fed_user_id)
        self.assertEqual(admins_of_fed_user.status_code, 200)
        admin_users = admins_of_fed_user.json()[const.USERS]
        self.assertEqual(admin_users, [])

        # Validate fed user's token
        # It is a defect that token is still valid. This behavior may change
        # once CIDMDEV-1078 is fixed.
        validate_resp = self.identity_admin_client.validate_token(
            token_id=fed_user_auth_token)
        self.assertEqual(validate_resp.status_code, 200)

    @ddt.file_data('data_get_admins_for_fed_user.json')
    def test_admin_of_fed_user_with_disabled_domain(self, test_data):

        user_name = self.generate_random_string()
        domain_id = self.generate_random_string(pattern='[\d]{7}')
        user_object = requests.UserAdd(
            user_name=user_name,
            domain_id=domain_id
        )
        resp = self.identity_admin_client.add_user(user_object)
        self.assertEqual(resp.status_code, 201)
        domain_id = resp.json()[const.USER][const.RAX_AUTH_DOMAIN_ID]
        user_id = resp.json()[const.USER][const.ID]
        self.user_ids.append(user_id)

        subject = self.generate_random_string(
            pattern='fed[\-]user[\-][\d\w]{12}')
        fed_input_data = test_data['fed_input']
        base64_url_encode = fed_input_data['base64_url_encode']
        new_url = fed_input_data['new_url']
        content_type = fed_input_data['content_type']

        cert = saml_helper.create_saml_assertion(
            domain=domain_id, subject=subject, issuer=self.issuer,
            email=self.test_email, base64_url_encode=base64_url_encode)

        auth = self.identity_admin_client.auth_with_saml(
            saml=cert, content_type=content_type,
            base64_url_encode=base64_url_encode, new_url=new_url)
        fed_user_id = auth.json()[const.ACCESS][const.USER][const.ID]
        fed_user_auth_token = auth.json()[const.ACCESS][const.TOKEN][const.ID]

        # Disable the domain
        domain_object = requests.Domain(
            domain_name=domain_id, enabled=False)
        update_domain_resp = self.identity_admin_client.update_domain(
            domain_id=domain_id, request_object=domain_object)
        self.assertEqual(update_domain_resp.status_code, 200)

        # Get admins of fed user using identity admin's token
        admins_of_fed_user = self.identity_admin_client.get_admins_for_a_user(
            user_id=fed_user_id)
        self.assertEqual(admins_of_fed_user.status_code, 200)
        admin_user_id = admins_of_fed_user.json()[const.USERS][0][const.ID]
        self.assertSchema(response=admins_of_fed_user,
                          json_schema=users_json.get_admins_of_user)
        self.assertEqual(admin_user_id, user_id)

        # Validate fed user's token
        validate_resp = self.identity_admin_client.validate_token(
            token_id=fed_user_auth_token)
        self.assertEqual(validate_resp.status_code, 404)

    def tearDown(self):
        # Delete all users created in the tests
        for id_ in self.user_ids:
            self.service_admin_client.delete_user(user_id=id_)
        super(TestAdminsOfUser, self).tearDown()
