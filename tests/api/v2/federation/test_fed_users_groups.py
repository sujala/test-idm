from tests.api.utils.create_cert import create_self_signed_cert
from tests.api.utils import saml_helper
from tests.api.v2.federation import federation
from tests.api.v2.models import factory, responses

from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


class TestFedUserGroups(federation.TestBaseFederation):

    """Tests for Fed User's global roles."""

    @classmethod
    def setUpClass(cls):
        """Class level set up for the tests

        Create users needed for the tests and generate clients for those users.
        """
        super(TestFedUserGroups, cls).setUpClass()
        if cls.test_config.use_domain_for_user_groups:
            cls.domain_id = cls.test_config.domain_id
        else:
            cls.domain_id = cls.generate_random_string(pattern='[\d]{7}')
        cls.user_admin_client = cls.generate_client(
            parent_client=cls.identity_admin_client,
            additional_input_data={'domain_id': cls.domain_id})
        cls.user_admin_client.serialize_format = 'xml'
        cls.user_admin_client.default_headers[
            const.CONTENT_TYPE] = 'application/xml'

        cls.domain_ids = []

        cls.domain_ids.append(cls.domain_id)

    def setUp(self):
        super(TestFedUserGroups, self).setUp()
        # get tenant access role id
        get_role_resp = self.identity_admin_client.get_role_by_name(
            role_name=const.TENANT_ACCESS_ROLE_NAME)
        self.tenant_access_role_id = get_role_resp.json()[const.ROLES][0][
            const.ID]
        self.users = []
        self.role_ids = []
        self.tenant_ids = []
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

    def create_tenant(self):
        tenant_req = factory.get_add_tenant_object(domain_id=self.domain_id)
        add_tenant_resp = self.identity_admin_client.add_tenant(
            tenant=tenant_req)
        self.assertEqual(add_tenant_resp.status_code, 201)
        tenant = responses.Tenant(add_tenant_resp.json())
        self.tenant_ids.append(tenant.id)
        return tenant

    def generate_tenants_assignment_dict(self, on_role, *for_tenants):

        tenant_assignment_request = {
            const.ON_ROLE: on_role,
            const.FOR_TENANTS: list(for_tenants)
        }
        return tenant_assignment_request

    def test_fed_user_groups(self):
        """
        Test to List fed user's groups:
        1. test with saml response that contains valid groups.
        2. test with saml response that contains 1 valid and 1 invalid group
        3. test with saml response without groups
        """
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
        group_two = self.create_and_add_user_group_to_domain(
            self.user_admin_client, self.domain_id)
        self.group_ids.append((group_two.id, self.domain_id))

        # create roles
        role_1 = self.create_role()
        role_2 = self.create_role()
        role_3 = self.create_role()

        # create tenant
        tenant_1 = self.create_tenant()
        tenant_2 = self.create_tenant()

        # create tenant assignments request dicts
        tenant_assignment_req_1 = self.generate_tenants_assignment_dict(
            role_1.id, tenant_1.id)
        tenant_assignment_req_2 = self.generate_tenants_assignment_dict(
            role_2.id, "*")
        tenant_assignment_req_3 = self.generate_tenants_assignment_dict(
            role_3.id, tenant_2.id)

        tenants_role_assignment_req_1 = requests.TenantRoleAssignments(
            tenant_assignment_req_1, tenant_assignment_req_2)
        tenants_role_assignment_req_2 = requests.TenantRoleAssignments(
            tenant_assignment_req_3)

        # assign roles to user group
        client_default_serialize_format = \
            self.user_admin_client.serialize_format
        self.user_admin_client.serialize_format = const.JSON
        assignment_resp = (
            self.user_admin_client.add_tenant_role_assignments_to_user_group(
                domain_id=self.domain_id, group_id=group_one.id,
                request_object=tenants_role_assignment_req_1))
        self.assertEqual(assignment_resp.status_code, 200)

        assignment_resp = (
            self.user_admin_client.add_tenant_role_assignments_to_user_group(
                domain_id=self.domain_id, group_id=group_two.id,
                request_object=tenants_role_assignment_req_2))
        self.assertEqual(assignment_resp.status_code, 200)
        self.user_admin_client.serialize_format = \
            client_default_serialize_format

        # create saml assertion with 2 groups
        cert_with_groups = saml_helper.create_saml_assertion_v2(
            domain=self.domain_id, username=subject, issuer=self.issuer,
            email=self.test_email, private_key_path=key_path,
            public_key_path=cert_path, response_flavor='v2DomainOrigin',
            output_format='formEncode',
            seconds_to_expiration=1440,
            groups=[group_one.name, group_two.name])

        # auth with saml
        auth = self.identity_admin_client.auth_with_saml(
            saml=cert_with_groups, content_type=const.X_WWW_FORM_URLENCODED,
            base64_url_encode=True, new_url=True)
        self.assertEqual(auth.status_code, 200)
        self.assertSchema(auth, json_schema=self.updated_fed_auth_schema)

        # grab all the roles
        list_role_ids = [
            role[const.ID] for role in auth.json()[
                const.ACCESS][const.USER][const.ROLES]]

        # 6 roles - 1 default, 2 tenant access, 3 roles from tenants
        self.assertEqual(len(list_role_ids), 6)
        # role is assigned to user on a tenant via user group
        self.assertIn(role_1.id, list_role_ids)
        # role is assigned to user on a tenant via user group
        self.assertIn(role_2.id, list_role_ids)
        # role is assigned to user on a tenant via user group
        self.assertIn(role_3.id, list_role_ids)
        # role implicitly to user for all tenants within domain
        self.assertIn(self.tenant_access_role_id, list_role_ids)
        # identity:default role assigned
        self.assertIn(const.USER_DEFAULT_ROLE_ID, list_role_ids)

        # check users in group
        fed_user_id = auth.json()[const.ACCESS][const.USER][const.ID]

        self.identity_admin_client.list_users_in_user_group_for_domain(
            domain_id=self.domain_id, group_id=group_two.id
        )

        # check group one has fed user id
        resp = self.identity_admin_client.list_users_in_user_group_for_domain(
            domain_id=self.domain_id, group_id=group_one.id
        )
        self.assertEqual(resp.status_code, 200)

        user_ids = [
            user[const.ID] for user in resp.json()[
                const.USERS]]

        self.assertIn(fed_user_id, user_ids)

        # check group two has fed user id
        resp = self.identity_admin_client.list_users_in_user_group_for_domain(
            domain_id=self.domain_id, group_id=group_two.id
        )
        self.assertEqual(resp.status_code, 200)

        user_ids = [
            user[const.ID] for user in resp.json()[
                const.USERS]]

        self.assertIn(fed_user_id, user_ids)

        # now, create saml with only one of the groups
        cert_with_groups = saml_helper.create_saml_assertion_v2(
            domain=self.domain_id, username=subject, issuer=self.issuer,
            email=self.test_email, private_key_path=key_path,
            public_key_path=cert_path, response_flavor='v2DomainOrigin',
            output_format='formEncode',
            seconds_to_expiration=1440,
            groups=[group_two.name])

        # auth with saml
        auth = self.identity_admin_client.auth_with_saml(
            saml=cert_with_groups, content_type=const.X_WWW_FORM_URLENCODED,
            base64_url_encode=True, new_url=True)
        self.assertEqual(auth.status_code, 200)
        self.assertSchema(auth, json_schema=self.updated_fed_auth_schema)

        # grab all the roles
        list_role_ids = [
            role[const.ID] for role in auth.json()[
                const.ACCESS][const.USER][const.ROLES]]

        # 4 roles - 1 default, 2 tenant access, 1 role from tenants
        self.assertEqual(len(list_role_ids), 4)
        # role is no longer assigned to user on a tenant via user group 1
        self.assertNotIn(role_1.id, list_role_ids)
        # role is no longer assigned to user on a tenant via user group 1
        self.assertNotIn(role_2.id, list_role_ids)
        # role is still assigned to user on a tenant via user group 2
        self.assertIn(role_3.id, list_role_ids)
        # role implicitly to user for all tenants within domain
        self.assertIn(self.tenant_access_role_id, list_role_ids)
        # identity:default role assigned
        self.assertIn(const.USER_DEFAULT_ROLE_ID, list_role_ids)

        # check users in group
        fed_user_id = auth.json()[const.ACCESS][const.USER][const.ID]

        self.identity_admin_client.list_users_in_user_group_for_domain(
            domain_id=self.domain_id, group_id=group_two.id
        )

        # check group one no longer has fed user id
        resp = self.identity_admin_client.list_users_in_user_group_for_domain(
            domain_id=self.domain_id, group_id=group_one.id
        )
        self.assertEqual(resp.status_code, 200)

        user_ids = [
            user[const.ID] for user in resp.json()[
                const.USERS]]

        self.assertNotIn(fed_user_id, user_ids)

        # check group two still has fed user id
        resp = self.identity_admin_client.list_users_in_user_group_for_domain(
            domain_id=self.domain_id, group_id=group_two.id
        )
        self.assertEqual(resp.status_code, 200)

        user_ids = [
            user[const.ID] for user in resp.json()[
                const.USERS]]

        self.assertIn(fed_user_id, user_ids)

        # now, create saml with no groups
        cert_without_groups = saml_helper.create_saml_assertion_v2(
            domain=self.domain_id, username=subject, issuer=self.issuer,
            email=self.test_email, private_key_path=key_path,
            public_key_path=cert_path, response_flavor='v2DomainOrigin',
            output_format='formEncode',
            seconds_to_expiration=1440)

        # auth with saml
        auth = self.identity_admin_client.auth_with_saml(
            saml=cert_without_groups, content_type=const.X_WWW_FORM_URLENCODED,
            base64_url_encode=True, new_url=True)
        self.assertEqual(auth.status_code, 200)
        self.assertSchema(auth, json_schema=self.updated_fed_auth_schema)

        # grab all the roles
        list_role_ids = [
            role[const.ID] for role in auth.json()[
                const.ACCESS][const.USER][const.ROLES]]

        # 3 roles - 1 default, 2 tenant access
        self.assertEqual(len(list_role_ids), 3)
        # role is no longer assigned to user on a tenant via user group 1
        self.assertNotIn(role_1.id, list_role_ids)
        # role is no longer assigned to user on a tenant via user group 1
        self.assertNotIn(role_2.id, list_role_ids)
        # role is still assigned to user on a tenant via user group 2
        self.assertNotIn(role_3.id, list_role_ids)
        # role implicitly to user for all tenants within domain
        self.assertIn(self.tenant_access_role_id, list_role_ids)
        # identity:default role assigned
        self.assertIn(const.USER_DEFAULT_ROLE_ID, list_role_ids)

        # check users in group
        fed_user_id = auth.json()[const.ACCESS][const.USER][const.ID]

        self.identity_admin_client.list_users_in_user_group_for_domain(
            domain_id=self.domain_id, group_id=group_two.id
        )

        # check group one does not have fed user id
        resp = self.identity_admin_client.list_users_in_user_group_for_domain(
            domain_id=self.domain_id, group_id=group_one.id
        )
        self.assertEqual(resp.status_code, 200)

        user_ids = [
            user[const.ID] for user in resp.json()[
                const.USERS]]

        self.assertNotIn(fed_user_id, user_ids)

        # check group two does not have fed user id
        resp = self.identity_admin_client.list_users_in_user_group_for_domain(
            domain_id=self.domain_id, group_id=group_two.id
        )
        self.assertEqual(resp.status_code, 200)

        user_ids = [
            user[const.ID] for user in resp.json()[
                const.USERS]]

        self.assertNotIn(fed_user_id, user_ids)

    def tearDown(self):
        for user_id in self.users:
            self.identity_admin_client.delete_user(user_id=user_id)
        self.delete_client(self.user_admin_client)
        for group_id, domain_id in self.group_ids:
            resp = self.identity_admin_client.delete_user_group_from_domain(
                group_id=group_id, domain_id=domain_id)
            self.assertEqual(
                resp.status_code, 204,
                msg='User group with ID {0} failed to delete'.format(
                    group_id))
        for role_id in self.role_ids:
            resp = self.identity_admin_client.delete_role(role_id=role_id)
            self.assertEqual(
                resp.status_code, 204,
                msg='Role with ID {0} failed to delete'.format(
                    role_id))
        for tenant_id in self.tenant_ids:
            resp = self.identity_admin_client.delete_tenant(
                tenant_id=tenant_id)
            # For some cases, tenant is getting deleted by delete_client()
            # call, prior. Hence checking for either 204 or 404.
            self.assertIn(
                resp.status_code, [204, 404],
                msg='Tenant with ID {0} failed to delete'.format(
                    tenant_id))
        super(TestFedUserGroups, self).tearDown()

    @classmethod
    def tearDownClass(cls):
        cls.identity_admin_client.delete_domain(cls.domain_id)
        super(TestFedUserGroups, cls).tearDownClass()
