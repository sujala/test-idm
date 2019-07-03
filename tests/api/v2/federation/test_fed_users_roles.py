import ddt
import pytest
from qe_coverage.opencafe_decorators import tags, unless_coverage

from tests.api.utils.create_cert import create_self_signed_cert
from tests.api.utils import func_helper
from tests.api.utils import saml_helper
from tests.api.v2 import base
from tests.api.v2.federation import federation
from tests.package.johny.v2.models import requests
from tests.package.johny import constants as const
from tests.api.v2.models import factory, responses
import os
import re


@ddt.ddt
class TestFedUserGlobalRoles(federation.TestBaseFederation):

    """Tests for Fed User's global roles."""

    @classmethod
    @unless_coverage
    def setUpClass(cls):
        """Class level set up for the tests

        Create users needed for the tests and generate clients for those users.
        """
        super(TestFedUserGlobalRoles, cls).setUpClass()
        cls.domain_id = func_helper.generate_randomized_domain_id(
            client=cls.identity_admin_client)
        cls.user_admin_client = cls.generate_client(
            parent_client=cls.identity_admin_client,
            additional_input_data={'domain_id': cls.domain_id})
        cls.user_admin_client.serialize_format = 'xml'
        cls.user_admin_client.default_headers[
            const.CONTENT_TYPE] = 'application/xml'

        cls.domain_ids = []

        cls.domain_ids.append(cls.domain_id)

    @unless_coverage
    def setUp(self):
        super(TestFedUserGlobalRoles, self).setUp()
        self.users = []
        self.role_ids = []

    def create_role(self):

        role_req = factory.get_add_role_request_object(
            administrator_role=const.USER_MANAGE_ROLE_NAME)
        add_role_resp = self.identity_admin_client.add_role(
            request_object=role_req)
        self.assertEqual(add_role_resp.status_code, 201)
        role = responses.Role(add_role_resp.json())
        self.role_ids.append(role.id)
        return role

    @tags('positive', 'p0', 'smoke')
    @pytest.mark.smoke_alpha
    def test_fed_user_global_roles(self):
        """
        Test to List fed user's global roles.
        """
        (pem_encoded_cert, cert_path, _, key_path,
         f_print) = create_self_signed_cert()

        provider_id = self.add_idp_with_metadata_return_id(
            cert_path=cert_path, api_client=self.user_admin_client)
        self.update_mapping_policy(idp_id=provider_id,
                                   client=self.user_admin_client)

        subject = self.generate_random_string(
            pattern='fed[\-]user[\-][\d\w]{12}')

        cert = saml_helper.create_saml_assertion_v2(
            domain=self.domain_id, username=subject, issuer=self.issuer,
            email=self.test_email, private_key_path=key_path,
            public_key_path=cert_path, response_flavor=const.V2_DOMAIN_ORIGIN,
            output_format=const.FORM_ENCODE)

        auth = self.identity_admin_client.auth_with_saml(
            saml=cert, content_type=const.X_WWW_FORM_URLENCODED,
            base64_url_encode=True, new_url=True)
        self.assertEqual(auth.status_code, 200)
        self.assertSchema(auth, json_schema=self.updated_fed_auth_schema)

        fed_user_id = auth.json()[const.ACCESS][const.USER][const.ID]
        fed_user_client = self.generate_client(
            token=auth.json()[const.ACCESS][const.TOKEN][const.ID])
        self.users.append(fed_user_id)

        self.validate_list_fed_users_global_roles(
            user_id=fed_user_id, client=self.user_admin_client)
        self.validate_list_fed_users_global_roles(
            user_id=fed_user_id, client=fed_user_client)

    def validate_list_fed_users_global_roles(self, user_id, client):
        list_resp = client.list_roles_for_user(
            user_id=user_id)
        self.assertEqual(list_resp.status_code, 200)
        role_ids = [role[const.ID] for role in list_resp.json()[const.ROLES]]
        self.assertIn(const.USER_DEFAULT_ROLE_ID, role_ids)

    @tags('positive', 'p0', 'regression')
    @pytest.mark.regression
    def test_fed_auth_with_v2_saml(self):

        (pem_encoded_cert, cert_path, _, key_path,
         f_print) = create_self_signed_cert()

        provider_id = self.add_idp_with_metadata_return_id(
            cert_path=cert_path, api_client=self.user_admin_client)
        self.update_mapping_policy(idp_id=provider_id,
                                   client=self.user_admin_client)

        subject = self.generate_random_string(
            pattern='fed[\-]user[\-][\d\w]{12}')

        cert = saml_helper.create_saml_assertion_v2(
            domain=self.domain_id, username=subject, issuer=self.issuer,
            roles=[const.USER_MANAGE_ROLE_NAME],
            email=self.test_email, private_key_path=key_path,
            public_key_path=cert_path, response_flavor=const.V2_DOMAIN_ORIGIN,
            output_format=const.FORM_ENCODE)

        # Get fed auth token
        auth = self.identity_admin_client.auth_with_saml(
            saml=cert, content_type=const.X_WWW_FORM_URLENCODED,
            base64_url_encode=True, new_url=True)
        fed_user_id = auth.json()[const.ACCESS][const.USER][const.ID]
        self.users.append(fed_user_id)
        role_names = [role[const.NAME] for role in auth.json()[const.ACCESS]
                          [const.USER][const.ROLES]]
        role_ids = [role[const.ID] for role in auth.json()[const.ACCESS]
                        [const.USER][const.ROLES]]

        # verify user-manager role is assigned to user
        self.assertIn(const.USER_MANAGE_ROLE_NAME, role_names)
        self.assertIn(const.USER_MANAGER_ROLE_ID, role_ids)

        fed_user_client = self.generate_client(
            token=auth.json()[const.ACCESS][const.TOKEN][const.ID])

        role = self.create_role()

        # getting role accessible to user-manager
        resp = fed_user_client.get_role_by_name(role.name)
        self.assertEqual(resp.json()[const.ROLES][0][const.NAME],
                         role.name)

        # check if fed user can now get own idp...with having user-manager role
        get_idp_resp = fed_user_client.get_idp(idp_id=provider_id)
        self.assertEqual(get_idp_resp.status_code, 200)

        list_idp_resp = fed_user_client.list_idp()
        # check if fed user can now get own idp...with having user-manager role
        self.assertEqual(list_idp_resp.status_code, 200)

        # Check if list effective roles for user is successful
        fed_user_id = auth.json()[const.ACCESS][const.USER][const.ID]
        eff_roles_resp = fed_user_client.list_effective_roles_for_user(
            user_id=fed_user_id)
        self.assertEqual(eff_roles_resp.status_code, 200)

        # list global roles for a user
        list_role_for_user_resp = fed_user_client.list_roles_for_user(
             user_id=fed_user_id)
        self.assertEqual(list_role_for_user_resp.status_code, 200)

        # Keeping update idp at last as it revokes fed user manager's token
        # as we are calling 'disable' idp
        update_idp_req = requests.IDP(idp_id=provider_id, enabled=False)
        update_resp = fed_user_client.update_idp(
            idp_id=provider_id, request_object=update_idp_req)
        self.assertEqual(update_resp.status_code, 200)

    @base.base.log_tearDown_error
    @unless_coverage
    def tearDown(self):
        for user_id in self.users:
            resp = self.identity_admin_client.delete_user(user_id=user_id)
            self.assertEqual(
                resp.status_code, 204,
                msg='User with ID {0} failed to delete'.format(user_id))
        for id_ in self.role_ids:
            resp = self.identity_admin_client.delete_role(role_id=id_)
            self.assertEqual(
                resp.status_code, 204,
                msg='Role with ID {0} failed to delete'.format(id_))
        super(TestFedUserGlobalRoles, self).tearDown()

    @classmethod
    @unless_coverage
    def tearDownClass(cls):
        cls.delete_client(cls.user_admin_client)
        super(TestFedUserGlobalRoles, cls).tearDownClass()
