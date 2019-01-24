from nose.plugins.attrib import attr
from qe_coverage.opencafe_decorators import tags, unless_coverage

from tests.api.utils.create_cert import create_self_signed_cert
from tests.api.utils import func_helper, saml_helper

from tests.api.v2 import base
from tests.api.v2.federation import federation
from tests.api.v2.models import factory, responses

from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


class TestFedUserManagerViaUserGroups(federation.TestBaseFederation):

    """Tests for Fed User's global roles."""

    @classmethod
    @unless_coverage
    def setUpClass(cls):
        """Class level set up for the tests

        Create users needed for the tests and generate clients for those users.
        """
        super(TestFedUserManagerViaUserGroups, cls).setUpClass()
        if cls.test_config.use_domain_for_user_groups:
            cls.domain_id = cls.test_config.domain_id
        else:
            cls.domain_id = func_helper.generate_randomized_domain_id(
                client=cls.identity_admin_client)
        cls.user_admin_client = cls.generate_client(
            parent_client=cls.identity_admin_client,
            additional_input_data={'domain_id': cls.domain_id})
        cls.user_admin_client.serialize_format = 'xml'
        cls.user_admin_client.default_headers[
            const.CONTENT_TYPE] = 'application/xml'

    @unless_coverage
    def setUp(self):
        super(TestFedUserManagerViaUserGroups, self).setUp()
        # get tenant access role id
        get_role_resp = self.identity_admin_client.get_role_by_name(
            role_name=const.TENANT_ACCESS_ROLE_NAME)
        self.tenant_access_role_id = get_role_resp.json()[const.ROLES][0][
            const.ID]
        self.group_ids = []

    def create_role(self):

        role_req = factory.get_add_role_request_object(
            administrator_role=const.USER_MANAGE_ROLE_NAME)
        add_role_resp = self.identity_admin_client.add_role(
            request_object=role_req)
        self.assertEqual(add_role_resp.status_code, 201)
        role = responses.Role(add_role_resp.json())
        self.role_ids.append(role.id)
        return role

    def generate_tenants_assignment_dict(self, on_role, *for_tenants):

        tenant_assignment_request = {
            const.ON_ROLE: on_role,
            const.FOR_TENANTS: list(for_tenants)
        }
        return tenant_assignment_request

    @tags('positive', 'p0', 'regression')
    @attr(type='regression')
    def test_accesses_for_fed_user_manager_via_user_group(self):

        (pem_encoded_cert, cert_path, _, key_path,
         f_print) = create_self_signed_cert()

        provider_id = self.add_idp_with_metadata_return_id(
            cert_path=cert_path, api_client=self.user_admin_client)
        self.update_mapping_policy(
            idp_id=provider_id,
            client=self.user_admin_client,
            file_path='yaml/mapping_policy_with_groups.yaml')

        subject = self.generate_random_string(
            pattern='fed[\-]user[\-][\d\w]{12}')

        # create user groups for domain
        group_one = self.create_and_add_user_group_to_domain(
            self.user_admin_client, self.domain_id)
        self.group_ids.append((group_one.id, self.domain_id))

        user_manager_role = responses.Role(self.user_admin_client.get_role(
            role_id=const.USER_MANAGER_ROLE_ID).json())

        # create tenant assignments request dicts
        tenant_assignment_req_1 = self.generate_tenants_assignment_dict(
            user_manager_role.id, "*")

        tenants_role_assignment_req_1 = requests.TenantRoleAssignments(
            tenant_assignment_req_1)

        # assign roles to user group
        client_default_serialize_format = \
            self.user_admin_client.serialize_format
        self.user_admin_client.serialize_format = const.JSON
        assignment_resp = (
            self.user_admin_client.add_tenant_role_assignments_to_user_group(
                domain_id=self.domain_id, group_id=group_one.id,
                request_object=tenants_role_assignment_req_1))
        self.assertEqual(assignment_resp.status_code, 200)

        self.user_admin_client.serialize_format = \
            client_default_serialize_format

        # create saml assertion with a user-group
        cert_with_group = saml_helper.create_saml_assertion_v2(
            domain=self.domain_id, username=subject, issuer=self.issuer,
            email=self.test_email, private_key_path=key_path,
            public_key_path=cert_path, response_flavor='v2DomainOrigin',
            output_format='formEncode',
            seconds_to_expiration=1440,
            groups=[group_one.name])

        # auth with saml
        auth = self.identity_admin_client.auth_with_saml(
            saml=cert_with_group, content_type=const.X_WWW_FORM_URLENCODED,
            base64_url_encode=True, new_url=True)
        self.assertEqual(auth.status_code, 200)
        self.assertSchema(auth, json_schema=self.updated_fed_auth_schema)

        # grab all the roles
        list_role_ids = [
            role[const.ID] for role in auth.json()[
                const.ACCESS][const.USER][const.ROLES]]

        # user-manager role is assigned to user via user group
        self.assertIn(user_manager_role.id, list_role_ids)

        # Add checks for idp management
        fed_user_token = auth.json()[const.ACCESS][const.TOKEN][const.ID]
        fed_user_client = self.generate_client(token=fed_user_token)
        get_idp_resp = fed_user_client.get_idp(idp_id=provider_id)
        # check if fed user can now get own idp...with having user-manager role
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
        resp = self.identity_admin_client.list_users_in_domain(
            domain_id=self.domain_id)
        users = resp.json()[const.USERS]
        # listing only non user-admin users, to delete those before
        # user-admin is deleted
        user_ids = [user[const.ID] for user in users if user[
            const.ID] != self.user_admin_client.default_headers[
            const.X_USER_ID]]

        for user_id in user_ids:
            resp = self.identity_admin_client.delete_user(user_id=user_id)
            assert resp.status_code == 204, \
                'User with ID {0} failed to delete'.format(user_id)

        for group_id, domain_id in self.group_ids:
            resp = self.identity_admin_client.delete_user_group_from_domain(
                group_id=group_id, domain_id=domain_id)
            self.assertEqual(
                resp.status_code, 204,
                msg='User group with ID {0} failed to delete'.format(
                    group_id))
        self.delete_client(self.user_admin_client)
        super(TestFedUserManagerViaUserGroups, self).tearDown()

    @classmethod
    @unless_coverage
    def tearDownClass(cls):
        super(TestFedUserManagerViaUserGroups, cls).tearDownClass()
