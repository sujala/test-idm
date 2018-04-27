from tests.api.utils import func_helper
from tests.api.v2 import base

from tests.api.v2.models import factory, responses


from tests.package.johny import constants as const


class TestUserGroups(base.TestBaseV2):

    def setUp(self):
        super(TestUserGroups, self).setUp()
        if self.test_config.use_domain_for_user_groups:
            self.domain_id = self.test_config.domain_id
        else:
            self.domain_id = func_helper.generate_randomized_domain_id(
                client=self.identity_admin_client)
        self.password = self.generate_random_string(
            pattern=const.PASSWORD_PATTERN)
        self.user_name = self.generate_random_string(
            pattern=const.USER_NAME_PATTERN
        )
        self.role_ids = []
        self.tenant_ids = []

    def generate_tenants_assignment_dict(self, on_role, *for_tenants):

        tenant_assignment_request = {
            const.ON_ROLE: on_role,
            const.FOR_TENANTS: list(for_tenants)
        }
        return tenant_assignment_request

    def create_and_add_user_group_to_user(self, client, status_code=201):
        group_req = factory.get_add_user_group_request(self.domain_id)
        resp = client.add_user_group_to_domain(
            domain_id=self.domain_id, request_object=group_req)
        self.assertEqual(resp.status_code, status_code)

        if status_code is not 201:
            return None
        else:
            group = responses.UserGroup(resp.json())

            # adding user to the user group
            add_resp = client.add_user_to_user_group(
                user_id=client.default_headers[
                    const.X_USER_ID],
                group_id=group.id, domain_id=self.domain_id
            )
            self.assertEqual(add_resp.status_code, 204)
            return group

    def create_role(self):

        role_req = factory.get_add_role_request_object(
            administrator_role=const.USER_MANAGE_ROLE_NAME)
        add_role_resp = self.identity_admin_client.add_role(
            request_object=role_req)
        self.assertEqual(add_role_resp.status_code, 201)
        role = responses.Role(add_role_resp.json())
        self.role_ids.append(role.id)
        return role

    def create_tenant(self, domain=None):
        if domain:
            tenant_req = factory.get_add_tenant_object(domain_id=domain)
        else:
            tenant_req = factory.get_add_tenant_object(
                domain_id=self.domain_id)
        add_tenant_resp = self.identity_admin_client.add_tenant(
            tenant=tenant_req)
        self.assertEqual(add_tenant_resp.status_code, 201)
        tenant = responses.Tenant(add_tenant_resp.json())
        self.tenant_ids.append(tenant.id)
        return tenant

    def create_and_add_user_group_to_domain(self, client,
                                            domain_id=None,
                                            status_code=201):
        if domain_id is None:
            domain_id = self.domain_id
        group_req = factory.get_add_user_group_request(domain_id)
        # set the serialize format to json since that's what we support
        # for user groups
        client_default_serialize_format = client.serialize_format
        client.serialize_format = const.JSON
        resp = client.add_user_group_to_domain(
            domain_id=domain_id, request_object=group_req)
        self.assertEqual(resp.status_code, status_code)
        client.serialize_format = client_default_serialize_format

        if status_code != 201:
            return None
        else:
            return responses.UserGroup(resp.json())

    @base.base.log_tearDown_error
    def tearDown(self):
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
        super(TestUserGroups, self).tearDown()
