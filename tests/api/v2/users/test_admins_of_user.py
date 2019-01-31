# -*- coding: utf-8 -*
from nose.plugins.attrib import attr
import ddt
import time
from qe_coverage.opencafe_decorators import tags, unless_coverage

from tests.api.utils import data_file_iterator
from tests.api.utils import saml_helper
from tests.api.v2 import base
from tests.api.v2.schema import users as users_json

from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


ERROR_MESSAGE_USER_NOT_FOUND = 'User {0} not found'


@ddt.ddt
class TestAdminsOfUser(base.TestBaseV2):

    @classmethod
    @unless_coverage
    def setUpClass(cls):

        super(TestAdminsOfUser, cls).setUpClass()

        cls.common_user_admin_client = cls.generate_client(
            parent_client=cls.identity_admin_client,
            additional_input_data={
                'domain_id': cls.generate_random_string(pattern='[\d]{7}')})
        cls.test_email = "random@rackspace.com"
        cls.issuer = 'http://identityqe.rackspace.com'
        cls.idp_id = 'identityqe'
        cls.racker_issuer = 'http://racker.rackspace.com'

    @unless_coverage
    @data_file_iterator.data_file_provider((
        "default_mapping_policy.yaml",
    ))
    def update_mapping_policy_for_idp(self, mapping):
        resp_put_manager = self.identity_admin_client.add_idp_mapping(
                idp_id=self.idp_id,
                request_data=mapping,
                content_type=const.YAML)
        assert resp_put_manager.status_code == 204

    @unless_coverage
    def setUp(self):
        super(TestAdminsOfUser, self).setUp()
        self.domain_ids = []
        self.update_mapping_policy_for_idp()

    @unless_coverage
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
        self.domain_ids.append(domain_id)

        password = resp.json()[const.USER][const.OS_KSADM_PASSWORD]
        auth_obj = requests.AuthenticateWithPassword(
            user_name=user_name, password=password
        )
        auth = self.identity_admin_client.get_auth_token(
            request_object=auth_obj)
        user_admin_token = auth.json()[const.ACCESS][const.TOKEN][const.ID]
        user_admin_client = self.generate_client(token=user_admin_token)

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

        output_format = 'xml'
        if base64_url_encode:
            output_format = 'formEncode'
        cert = saml_helper.create_saml_assertion_v2(
            domain=domain_id, username=subject, issuer=self.issuer,
            email=self.test_email, response_flavor='v2DomainOrigin',
            output_format=output_format)

        auth = self.identity_admin_client.auth_with_saml(
            saml=cert, content_type=content_type,
            base64_url_encode=base64_url_encode, new_url=new_url)
        fed_user_id = auth.json()[const.ACCESS][const.USER][const.ID]
        fed_user_auth_token = auth.json()[const.ACCESS][const.TOKEN][const.ID]
        fed_user_client = self.generate_client(token=fed_user_auth_token)

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

    @unless_coverage
    @ddt.file_data('data_get_admins_for_fed_user.json')
    @attr('skip_at_gate')
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
        self.domain_ids.append(domain_id)

        subject = self.generate_random_string(
            pattern='fed[\-]user[\-][\d\w]{12}')
        fed_input_data = test_data['fed_input']
        base64_url_encode = fed_input_data['base64_url_encode']
        new_url = fed_input_data['new_url']
        content_type = fed_input_data['content_type']

        output_format = 'xml'
        if base64_url_encode:
            output_format = 'formEncode'
        cert = saml_helper.create_saml_assertion_v2(
            domain=domain_id, username=subject, issuer=self.issuer,
            email=self.test_email, response_flavor='v2DomainOrigin',
            output_format=output_format)

        auth = self.identity_admin_client.auth_with_saml(
            saml=cert, content_type=content_type,
            base64_url_encode=base64_url_encode, new_url=new_url)
        fed_user_id = auth.json()[const.ACCESS][const.USER][const.ID]
        fed_user_auth_token = auth.json()[const.ACCESS][const.TOKEN][const.ID]
        fed_user_client = self.generate_client(token=fed_user_auth_token)

        # creating second fed user under same domain
        subject_2 = self.generate_random_string(
            pattern='fed[\-]user[\-][\d\w]{12}')

        cert = saml_helper.create_saml_assertion_v2(
            domain=domain_id, username=subject_2, issuer=self.issuer,
            email=self.test_email, response_flavor='v2DomainOrigin',
            output_format=output_format)

        auth = self.identity_admin_client.auth_with_saml(
            saml=cert, content_type=content_type,
            base64_url_encode=base64_url_encode, new_url=new_url)
        fed_user_id_2 = auth.json()[const.ACCESS][const.USER][const.ID]
        fed_user_auth_token_2 = auth.json()[const.ACCESS][const.TOKEN][
            const.ID]
        fed_user_client_2 = self.generate_client(token=fed_user_auth_token_2)

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

    @unless_coverage
    @ddt.file_data('data_get_admins_for_fed_user.json')
    @attr(type='skip_at_gate')
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
        self.domain_ids.append(domain_id)

        subject = self.generate_random_string(
            pattern='fed[\-]user[\-][\d\w]{12}')
        fed_input_data = test_data['fed_input']
        base64_url_encode = fed_input_data['base64_url_encode']
        new_url = fed_input_data['new_url']
        content_type = fed_input_data['content_type']

        output_format = 'xml'
        if base64_url_encode:
            output_format = 'formEncode'
        cert = saml_helper.create_saml_assertion_v2(
            domain=domain_id, username=subject, issuer=self.issuer,
            email=self.test_email, response_flavor='v2DomainOrigin',
            output_format=output_format)

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

    @unless_coverage
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
        self.domain_ids.append(domain_id)

        password = resp.json()[const.USER][const.OS_KSADM_PASSWORD]
        auth_obj = requests.AuthenticateWithPassword(
            user_name=user_name, password=password
        )
        auth = self.identity_admin_client.get_auth_token(
            request_object=auth_obj)
        user_admin_token = auth.json()[const.ACCESS][const.TOKEN][const.ID]
        user_admin_client = self.generate_client(token=user_admin_token)

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

        output_format = 'xml'
        if base64_url_encode:
            output_format = 'formEncode'
        cert = saml_helper.create_saml_assertion_v2(
            domain=domain_id, username=subject, issuer=self.issuer,
            email=self.test_email, response_flavor='v2DomainOrigin',
            output_format=output_format)

        auth = self.identity_admin_client.auth_with_saml(
            saml=cert, content_type=content_type,
            base64_url_encode=base64_url_encode, new_url=new_url)
        fed_user_id = auth.json()[const.ACCESS][const.USER][const.ID]
        fed_user_auth_token = auth.json()[const.ACCESS][const.TOKEN][const.ID]
        fed_user_client = self.generate_client(token=fed_user_auth_token)

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

    @unless_coverage
    @ddt.file_data('data_get_admins_for_fed_user.json')
    @attr(type='skip_at_gate')
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
        self.domain_ids.append(domain_id)

        password = resp.json()[const.USER][const.OS_KSADM_PASSWORD]
        auth_obj = requests.AuthenticateWithPassword(
            user_name=user_name, password=password
        )
        auth = self.identity_admin_client.get_auth_token(
            request_object=auth_obj)
        user_admin_token = auth.json()[const.ACCESS][const.TOKEN][const.ID]
        user_admin_client = self.generate_client(token=user_admin_token)

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

        output_format = 'xml'
        if base64_url_encode:
            output_format = 'formEncode'
        cert = saml_helper.create_saml_assertion_v2(
            domain=domain_id, username=subject, issuer=self.issuer,
            email=self.test_email, response_flavor='v2DomainOrigin',
            output_format=output_format)

        auth = self.identity_admin_client.auth_with_saml(
            saml=cert, content_type=content_type,
            base64_url_encode=base64_url_encode, new_url=new_url)
        fed_user_id = auth.json()[const.ACCESS][const.USER][const.ID]
        fed_user_auth_token = auth.json()[const.ACCESS][const.TOKEN][const.ID]
        fed_user_client = self.generate_client(token=fed_user_auth_token)

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

    @unless_coverage
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
        self.domain_ids.append(domain_id)

        subject = self.generate_random_string(
            pattern='fed[\-]user[\-][\d\w]{12}')
        fed_input_data = test_data['fed_input']
        base64_url_encode = fed_input_data['base64_url_encode']
        new_url = fed_input_data['new_url']
        content_type = fed_input_data['content_type']
        fed_user_lifetime = 5

        output_format = 'xml'
        if base64_url_encode:
            output_format = 'formEncode'
        cert = saml_helper.create_saml_assertion_v2(
            domain=domain_id, username=subject, issuer=self.issuer,
            email=self.test_email, response_flavor='v2DomainOrigin',
            seconds_to_expiration=fed_user_lifetime,
            output_format=output_format)

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

        # create logoutv2 saml for user
        logout_v2_saml = saml_helper.create_saml_logout_v2(
            issuer=self.issuer, name_id=subject
        )

        # validate logoutv2 saml for user
        logout_validate_r = self.identity_admin_client.validate_logout_saml(
            saml=logout_v2_saml)
        self.assertEqual(logout_validate_r.status_code, 200)

        logout_response = self.identity_admin_client.logout_with_saml(
            saml=logout_v2_saml)
        self.assertEqual(logout_response.status_code, 200)

        # Get admins of fed user using identity admin's token
        admins_of_fed_user = self.identity_admin_client.get_admins_for_a_user(
            user_id=fed_user_id)
        self.assertEqual(admins_of_fed_user.status_code, 404)
        self.assertEqual(admins_of_fed_user.json()['itemNotFound']['message'],
                         ERROR_MESSAGE_USER_NOT_FOUND.format(fed_user_id))

    @unless_coverage
    @ddt.file_data('data_get_admins_for_fed_user.json')
    @attr(type='skip_at_gate')
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
        self.domain_ids.append(domain_id)

        subject = self.generate_random_string(
            pattern='fed[\-]user[\-][\d\w]{12}')
        fed_input_data = test_data['fed_input']
        base64_url_encode = fed_input_data['base64_url_encode']
        new_url = fed_input_data['new_url']
        content_type = fed_input_data['content_type']

        output_format = 'xml'
        if base64_url_encode:
            output_format = 'formEncode'
        cert = saml_helper.create_saml_assertion_v2(
            domain=domain_id, username=subject, issuer=self.issuer,
            email=self.test_email, response_flavor='v2DomainOrigin',
            output_format=output_format)

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

    @unless_coverage
    @ddt.file_data('data_get_admins_for_fed_user.json')
    @attr(type='skip_at_gate')
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
        self.domain_ids.append(domain_id)

        subject = self.generate_random_string(
            pattern='fed[\-]user[\-][\d\w]{12}')
        fed_input_data = test_data['fed_input']
        base64_url_encode = fed_input_data['base64_url_encode']
        new_url = fed_input_data['new_url']
        content_type = fed_input_data['content_type']

        output_format = 'xml'
        if base64_url_encode:
            output_format = 'formEncode'
        cert = saml_helper.create_saml_assertion_v2(
            domain=domain_id, username=subject, issuer=self.issuer,
            email=self.test_email, response_flavor='v2DomainOrigin',
            output_format=output_format)

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

    @tags('negative', 'p1', 'regression')
    def test_racker_saml_logout(self):
        #  Logging out federated rackers is not supported
        logout_v2_saml = saml_helper.create_saml_logout_v2(
            issuer=self.racker_issuer,
            name_id=self.identity_config.racker_username
        )
        logout_response = self.identity_admin_client.logout_with_saml(
            saml=logout_v2_saml)
        self.assertEqual(logout_response.status_code, 400)

    @unless_coverage
    def tearDown(self):
        # Delete all users created in the tests
        for dom in self.domain_ids:
            users = self.identity_admin_client.list_users_in_domain(
                domain_id=dom)
            user_ids_ = [user[const.ID] for user in users.json()[const.USERS]]
            for id_ in user_ids_:
                self.identity_admin_client.delete_user(user_id=id_)
        super(TestAdminsOfUser, self).tearDown()

    @classmethod
    @unless_coverage
    def tearDownClass(cls):
        cls.delete_client(client=cls.common_user_admin_client,
                          parent_client=cls.service_admin_client)
        super(TestAdminsOfUser, cls).tearDownClass()
