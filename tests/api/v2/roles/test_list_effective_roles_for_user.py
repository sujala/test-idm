# -*- coding: utf-8 -*
from nose.plugins.attrib import attr
from qe_coverage.opencafe_decorators import tags, unless_coverage
from random import randrange

from tests.api.v2 import base
from tests.api.v2.models import factory, responses

from tests.package.johny import constants as const


class TestListEffectiveRolesForUser(base.TestBaseV2):

    """ List effective role for user
    """
    @unless_coverage
    def setUp(self):
        """Create users needed for the tests and generate clients for
        those users.
        """
        super(TestListEffectiveRolesForUser, self).setUp()

        contact_id = randrange(start=const.CONTACT_ID_MIN,
                               stop=const.CONTACT_ID_MAX)
        self.domain_id = self.generate_random_string(
            pattern='test[\-]spec[\-]user[\-]list[\-][\d]{5}')
        self.user_admin_client = self.generate_client(
            parent_client=self.identity_admin_client,
            additional_input_data={'domain_id': self.domain_id,
                                   'contact_id': contact_id},
            one_call=True)

        self.user_manager_client = self.generate_client(
            parent_client=self.user_admin_client,
            additional_input_data={'is_user_manager': True})

        sub_user_name = self.generate_random_string(
            pattern=const.SUB_USER_PATTERN)
        self.user_client = self.generate_client(
            parent_client=self.user_manager_client,
            additional_input_data={
                'domain_id': self.domain_id,
                'user_name': sub_user_name})

        self.role_ids = []
        self.tenant_ids = []

    @tags('positive', 'p0', 'smoke')
    @attr(type='smoke_alpha')
    def test_effective_roles_for_default_user(self):
        # create and add role to user
        role_name = self.create_role_and_add_to_user(
            self.user_client.default_headers[const.X_USER_ID]
        )

        # create tenant and add role on tenant for user
        tenant_one, tenant_role_name_one = \
            self.create_tenant_and_add_role_on_tenant_for_user(
                self.user_client.default_headers[const.X_USER_ID]
            )

        # create another tenant and add role on tenant for user
        tenant_two, tenant_role_name_two = \
            self.create_tenant_and_add_role_on_tenant_for_user(
                self.user_client.default_headers[const.X_USER_ID]
            )

        user_group_role_name = self.create_group_and_role_and_add_to_tenant(
            self.user_admin_client, self.user_client,
            self.domain_id, tenant_one.id)

        # validate effective roles as identity:default
        self.validate_effective_roles(
            self.user_client.default_headers[const.X_USER_ID],
            self.user_client, [tenant_role_name_one, tenant_role_name_two],
            role_name=role_name,
            user_group_role_name=user_group_role_name,
            identity_role_name=const.USER_DEFAULT_ROLE_NAME,
            number_of_tenant_assignments=6)

        # validate effective roles as identity:default with filter: tenant one
        self.validate_effective_roles(
            self.user_client.default_headers[const.X_USER_ID],
            self.user_client, [tenant_role_name_one],
            role_name=role_name, user_group_role_name=user_group_role_name,
            identity_role_name=const.USER_DEFAULT_ROLE_NAME,
            number_of_tenant_assignments=5, params={
                const.ON_TENANT_ID: tenant_one.id
            }
        )

        # validate effective roles as identity:default with filter: tenant two
        self.validate_effective_roles(
            self.user_client.default_headers[const.X_USER_ID],
            self.user_client, [tenant_role_name_two],
            role_name=role_name,
            identity_role_name=const.USER_DEFAULT_ROLE_NAME,
            number_of_tenant_assignments=4, params={
                const.ON_TENANT_ID: tenant_two.id
            }
        )

        # validate effective roles as identity:default with invalid filter
        self.validate_effective_roles(
            self.user_client.default_headers[const.X_USER_ID],
            self.user_client, [tenant_role_name_one, tenant_role_name_two],
            role_name=role_name, user_group_role_name=user_group_role_name,
            identity_role_name=const.USER_DEFAULT_ROLE_NAME,
            number_of_tenant_assignments=6, params={
                'tenantid': tenant_one.id
            }
        )

        # validate effective roles as identity:default with unknown tenant
        self.validate_effective_roles(
            self.user_client.default_headers[const.X_USER_ID],
            self.user_client, [tenant_role_name_one, tenant_role_name_two],
            role_name=role_name, user_group_role_name=user_group_role_name,
            identity_role_name=const.USER_DEFAULT_ROLE_NAME,
            number_of_tenant_assignments=0, params={
                const.ON_TENANT_ID: '{}unknown'.format(tenant_one.id)
            }
        )

        # validate effective roles as identity:user-manage
        self.validate_effective_roles(
            self.user_client.default_headers[const.X_USER_ID],
            self.user_manager_client,
            [tenant_role_name_one, tenant_role_name_two],
            role_name=role_name,
            user_group_role_name=user_group_role_name,
            identity_role_name=const.USER_DEFAULT_ROLE_NAME,
            number_of_tenant_assignments=6)

        # validate effective roles as identity:user-manage with filter:
        # tenant one
        self.validate_effective_roles(
            self.user_client.default_headers[const.X_USER_ID],
            self.user_manager_client, [tenant_role_name_one],
            role_name=role_name, user_group_role_name=user_group_role_name,
            identity_role_name=const.USER_DEFAULT_ROLE_NAME,
            number_of_tenant_assignments=5, params={
                const.ON_TENANT_ID: tenant_one.id
            }
        )

        # validate effective roles as identity:user-manage with filter:
        # tenant two
        self.validate_effective_roles(
            self.user_client.default_headers[const.X_USER_ID],
            self.user_manager_client, [tenant_role_name_two],
            role_name=role_name,
            identity_role_name=const.USER_DEFAULT_ROLE_NAME,
            number_of_tenant_assignments=4, params={
                const.ON_TENANT_ID: tenant_two.id
            }
        )

        # validate effective roles as identity:user-manage with invalid filter
        self.validate_effective_roles(
            self.user_client.default_headers[const.X_USER_ID],
            self.user_manager_client,
            [tenant_role_name_one, tenant_role_name_two],
            role_name=role_name, user_group_role_name=user_group_role_name,
            identity_role_name=const.USER_DEFAULT_ROLE_NAME,
            number_of_tenant_assignments=6, params={
                'tenantid': tenant_one.id
            }
        )

        # validate effective roles as identity:user-manage with unknown tenant
        self.validate_effective_roles(
            self.user_client.default_headers[const.X_USER_ID],
            self.user_manager_client,
            [tenant_role_name_one, tenant_role_name_two],
            role_name=role_name, user_group_role_name=user_group_role_name,
            identity_role_name=const.USER_DEFAULT_ROLE_NAME,
            number_of_tenant_assignments=0, params={
                const.ON_TENANT_ID: '{}unknown'.format(tenant_one.id)
            }
        )

        # validate effective roles as identity:user-admin
        self.validate_effective_roles(
            self.user_client.default_headers[const.X_USER_ID],
            self.user_admin_client,
            [tenant_role_name_one, tenant_role_name_two],
            role_name=role_name,
            user_group_role_name=user_group_role_name,
            identity_role_name=const.USER_DEFAULT_ROLE_NAME,
            number_of_tenant_assignments=6)

        # validate effective roles as identity:user-admin with filter:
        # tenant one
        self.validate_effective_roles(
            self.user_client.default_headers[const.X_USER_ID],
            self.user_admin_client, [tenant_role_name_one],
            role_name=role_name, user_group_role_name=user_group_role_name,
            identity_role_name=const.USER_DEFAULT_ROLE_NAME,
            number_of_tenant_assignments=5, params={
                const.ON_TENANT_ID: tenant_one.id
            }
        )

        # validate effective roles as identity:user-admin with filter:
        # tenant two
        self.validate_effective_roles(
            self.user_client.default_headers[const.X_USER_ID],
            self.user_admin_client, [tenant_role_name_two],
            role_name=role_name,
            identity_role_name=const.USER_DEFAULT_ROLE_NAME,
            number_of_tenant_assignments=4, params={
                const.ON_TENANT_ID: tenant_two.id
            }
        )

        # validate effective roles as identity:user-admin with invalid filter
        self.validate_effective_roles(
            self.user_client.default_headers[const.X_USER_ID],
            self.user_admin_client,
            [tenant_role_name_one, tenant_role_name_two],
            role_name=role_name, user_group_role_name=user_group_role_name,
            identity_role_name=const.USER_DEFAULT_ROLE_NAME,
            number_of_tenant_assignments=6, params={
                'tenantid': tenant_one.id
            }
        )

        # validate effective roles as identity:user-admin with unknown tenant
        self.validate_effective_roles(
            self.user_client.default_headers[const.X_USER_ID],
            self.user_admin_client,
            [tenant_role_name_one, tenant_role_name_two],
            role_name=role_name, user_group_role_name=user_group_role_name,
            identity_role_name=const.USER_DEFAULT_ROLE_NAME,
            number_of_tenant_assignments=0, params={
                const.ON_TENANT_ID: '{}unknown'.format(tenant_one.id)
            }
        )

        # validate effective roles as identity:admin
        self.validate_effective_roles(
            self.user_client.default_headers[const.X_USER_ID],
            self.identity_admin_client,
            [tenant_role_name_one, tenant_role_name_two],
            role_name=role_name,
            user_group_role_name=user_group_role_name,
            identity_role_name=const.USER_DEFAULT_ROLE_NAME,
            number_of_tenant_assignments=6)

        # validate effective roles as identity:admin with filter:
        # tenant one
        self.validate_effective_roles(
            self.user_client.default_headers[const.X_USER_ID],
            self.identity_admin_client, [tenant_role_name_one],
            role_name=role_name, user_group_role_name=user_group_role_name,
            identity_role_name=const.USER_DEFAULT_ROLE_NAME,
            number_of_tenant_assignments=5, params={
                const.ON_TENANT_ID: tenant_one.id
            }
        )

        # validate effective roles as identity:admin with filter: tenant two
        self.validate_effective_roles(
            self.user_client.default_headers[const.X_USER_ID],
            self.identity_admin_client, [tenant_role_name_two],
            role_name=role_name,
            identity_role_name=const.USER_DEFAULT_ROLE_NAME,
            number_of_tenant_assignments=4, params={
                const.ON_TENANT_ID: tenant_two.id
            }
        )

        # validate effective roles as identity:admin with invalid filter
        self.validate_effective_roles(
            self.user_client.default_headers[const.X_USER_ID],
            self.identity_admin_client,
            [tenant_role_name_one, tenant_role_name_two],
            role_name=role_name, user_group_role_name=user_group_role_name,
            identity_role_name=const.USER_DEFAULT_ROLE_NAME,
            number_of_tenant_assignments=6, params={
                'tenantid': tenant_one.id
            }
        )

        # validate effective roles as identity:admin with unknown tenant
        self.validate_effective_roles(
            self.user_client.default_headers[const.X_USER_ID],
            self.identity_admin_client,
            [tenant_role_name_one, tenant_role_name_two],
            role_name=role_name, user_group_role_name=user_group_role_name,
            identity_role_name=const.USER_DEFAULT_ROLE_NAME,
            number_of_tenant_assignments=0, params={
                const.ON_TENANT_ID: '{}unknown'.format(tenant_one.id)
            }
        )

    @tags('positive', 'p0', 'smoke')
    @attr(type='smoke_alpha')
    def test_effective_roles_for_manage_user(self):
        # create and add role to user
        role_name = self.create_role_and_add_to_user(
            self.user_manager_client.default_headers[const.X_USER_ID]
        )

        # create tenant and add role on tenant for user
        tenant_one, tenant_role_name_one = \
            self.create_tenant_and_add_role_on_tenant_for_user(
                self.user_manager_client.default_headers[const.X_USER_ID]
            )

        # create another tenant and add role on tenant for user
        tenant_two, tenant_role_name_two = \
            self.create_tenant_and_add_role_on_tenant_for_user(
                self.user_manager_client.default_headers[const.X_USER_ID]
            )

        user_group_role_name = self.create_group_and_role_and_add_to_tenant(
            self.user_admin_client, self.user_manager_client,
            self.domain_id, tenant_one.id)

        # validate effective roles as identity:user-manage
        self.validate_effective_roles(
            self.user_manager_client.default_headers[const.X_USER_ID],
            self.user_manager_client,
            [tenant_role_name_one, tenant_role_name_two],
            role_name=role_name,
            user_group_role_name=user_group_role_name,
            identity_role_name=const.USER_MANAGE_ROLE_NAME,
            number_of_tenant_assignments=7)

        # validate effective roles as identity:user-manage with filter:
        # tenant one
        self.validate_effective_roles(
            self.user_manager_client.default_headers[const.X_USER_ID],
            self.user_manager_client, [tenant_role_name_one],
            role_name=role_name, user_group_role_name=user_group_role_name,
            identity_role_name=const.USER_MANAGE_ROLE_NAME,
            number_of_tenant_assignments=6, params={
                const.ON_TENANT_ID: tenant_one.id
            }
        )

        # validate effective roles as identity:user-manage with filter:
        # tenant two
        self.validate_effective_roles(
            self.user_manager_client.default_headers[const.X_USER_ID],
            self.user_manager_client, [tenant_role_name_two],
            role_name=role_name,
            identity_role_name=const.USER_MANAGE_ROLE_NAME,
            number_of_tenant_assignments=5, params={
                const.ON_TENANT_ID: tenant_two.id
            }
        )

        # validate effective roles as identity:user-manage with invalid filter
        self.validate_effective_roles(
            self.user_manager_client.default_headers[const.X_USER_ID],
            self.user_manager_client,
            [tenant_role_name_one, tenant_role_name_two],
            role_name=role_name, user_group_role_name=user_group_role_name,
            identity_role_name=const.USER_MANAGE_ROLE_NAME,
            number_of_tenant_assignments=7, params={
                'tenantid': tenant_one.id
            }
        )

        # validate effective roles as identity:user-manage with unknown tenant
        self.validate_effective_roles(
            self.user_manager_client.default_headers[const.X_USER_ID],
            self.user_manager_client,
            [tenant_role_name_one, tenant_role_name_two],
            role_name=role_name, user_group_role_name=user_group_role_name,
            identity_role_name=const.USER_MANAGE_ROLE_NAME,
            number_of_tenant_assignments=0, params={
                const.ON_TENANT_ID: '{}unknown'.format(tenant_one.id)
            }
        )

        # validate effective roles as identity:user-admin
        self.validate_effective_roles(
            self.user_manager_client.default_headers[const.X_USER_ID],
            self.user_admin_client,
            [tenant_role_name_one, tenant_role_name_two],
            role_name=role_name,
            user_group_role_name=user_group_role_name,
            identity_role_name=const.USER_MANAGE_ROLE_NAME,
            number_of_tenant_assignments=7)

        # validate effective roles as identity:user-admin with filter:
        # tenant one
        self.validate_effective_roles(
            self.user_manager_client.default_headers[const.X_USER_ID],
            self.user_admin_client, [tenant_role_name_one],
            role_name=role_name, user_group_role_name=user_group_role_name,
            identity_role_name=const.USER_MANAGE_ROLE_NAME,
            number_of_tenant_assignments=6, params={
                const.ON_TENANT_ID: tenant_one.id
            }
        )

        # validate effective roles as identity:user-admin with filter:
        # tenant two
        self.validate_effective_roles(
            self.user_manager_client.default_headers[const.X_USER_ID],
            self.user_admin_client, [tenant_role_name_two],
            role_name=role_name,
            identity_role_name=const.USER_MANAGE_ROLE_NAME,
            number_of_tenant_assignments=5, params={
                const.ON_TENANT_ID: tenant_two.id
            }
        )

        # validate effective roles as identity:user-admin with invalid filter
        self.validate_effective_roles(
            self.user_manager_client.default_headers[const.X_USER_ID],
            self.user_admin_client,
            [tenant_role_name_one, tenant_role_name_two],
            role_name=role_name, user_group_role_name=user_group_role_name,
            identity_role_name=const.USER_MANAGE_ROLE_NAME,
            number_of_tenant_assignments=7, params={
                'tenantid': tenant_one.id
            }
        )

        # validate effective roles as identity:user-admin with unknown tenant
        self.validate_effective_roles(
            self.user_manager_client.default_headers[const.X_USER_ID],
            self.user_admin_client,
            [tenant_role_name_one, tenant_role_name_two],
            role_name=role_name, user_group_role_name=user_group_role_name,
            identity_role_name=const.USER_MANAGE_ROLE_NAME,
            number_of_tenant_assignments=0, params={
                const.ON_TENANT_ID: '{}unknown'.format(tenant_one.id)
            }
        )

        # validate effective roles as identity:admin
        self.validate_effective_roles(
            self.user_manager_client.default_headers[const.X_USER_ID],
            self.identity_admin_client,
            [tenant_role_name_one, tenant_role_name_two],
            role_name=role_name,
            user_group_role_name=user_group_role_name,
            identity_role_name=const.USER_MANAGE_ROLE_NAME,
            number_of_tenant_assignments=7)

        # validate effective roles as identity:admin with filter: tenant one
        self.validate_effective_roles(
            self.user_manager_client.default_headers[const.X_USER_ID],
            self.identity_admin_client, [tenant_role_name_one],
            role_name=role_name, user_group_role_name=user_group_role_name,
            identity_role_name=const.USER_MANAGE_ROLE_NAME,
            number_of_tenant_assignments=6, params={
                const.ON_TENANT_ID: tenant_one.id
            }
        )

        # validate effective roles as identity:admin with filter:
        # tenant two
        self.validate_effective_roles(
            self.user_manager_client.default_headers[const.X_USER_ID],
            self.identity_admin_client, [tenant_role_name_two],
            role_name=role_name,
            identity_role_name=const.USER_MANAGE_ROLE_NAME,
            number_of_tenant_assignments=5, params={
                const.ON_TENANT_ID: tenant_two.id
            }
        )

        # validate effective roles as identity:admin with invalid filter
        self.validate_effective_roles(
            self.user_manager_client.default_headers[const.X_USER_ID],
            self.identity_admin_client,
            [tenant_role_name_one, tenant_role_name_two],
            role_name=role_name, user_group_role_name=user_group_role_name,
            identity_role_name=const.USER_MANAGE_ROLE_NAME,
            number_of_tenant_assignments=7, params={
                'tenantid': tenant_one.id
            }
        )

        # validate effective roles as identity:admin with unknown tenant
        self.validate_effective_roles(
            self.user_manager_client.default_headers[const.X_USER_ID],
            self.identity_admin_client,
            [tenant_role_name_one, tenant_role_name_two],
            role_name=role_name, user_group_role_name=user_group_role_name,
            identity_role_name=const.USER_MANAGE_ROLE_NAME,
            number_of_tenant_assignments=0, params={
                const.ON_TENANT_ID: '{}unknown'.format(tenant_one.id)
            }
        )

    @tags('positive', 'p0', 'smoke')
    @attr(type='smoke_alpha')
    def test_effective_roles_for_admin_user(self):
        # create and add role to user
        role_name = self.create_role_and_add_to_user(
            self.user_admin_client.default_headers[const.X_USER_ID]
        )

        # create tenant and add role on tenant for user
        tenant_one, tenant_role_name_one = \
            self.create_tenant_and_add_role_on_tenant_for_user(
                self.user_admin_client.default_headers[const.X_USER_ID]
            )

        # create another tenant and add role on tenant for user
        tenant_two, tenant_role_name_two = \
            self.create_tenant_and_add_role_on_tenant_for_user(
                self.user_admin_client.default_headers[const.X_USER_ID]
            )

        user_group_role_name = self.create_group_and_role_and_add_to_tenant(
            self.identity_admin_client, self.user_admin_client,
            self.domain_id, tenant_one.id)

        # validate effective roles as identity:user-admin
        self.validate_effective_roles(
            self.user_admin_client.default_headers[const.X_USER_ID],
            self.user_admin_client,
            [tenant_role_name_one, tenant_role_name_two],
            role_name=role_name,
            user_group_role_name=user_group_role_name,
            identity_role_name=const.USER_ADMIN_ROLE_NAME,
            number_of_tenant_assignments=6)

        # validate effective roles as identity:user-admin with filter:
        # tenant one
        self.validate_effective_roles(
            self.user_admin_client.default_headers[const.X_USER_ID],
            self.user_admin_client, [tenant_role_name_one],
            role_name=role_name, user_group_role_name=user_group_role_name,
            identity_role_name=const.USER_ADMIN_ROLE_NAME,
            number_of_tenant_assignments=5, params={
                const.ON_TENANT_ID: tenant_one.id
            }
        )

        # validate effective roles as identity:user-admin with filter:
        # tenant two
        self.validate_effective_roles(
            self.user_admin_client.default_headers[const.X_USER_ID],
            self.user_admin_client, [tenant_role_name_two],
            role_name=role_name,
            identity_role_name=const.USER_ADMIN_ROLE_NAME,
            number_of_tenant_assignments=4, params={
                const.ON_TENANT_ID: tenant_two.id
            }
        )

        # validate effective roles as identity:user-admin with invalid filter
        self.validate_effective_roles(
            self.user_admin_client.default_headers[const.X_USER_ID],
            self.user_admin_client,
            [tenant_role_name_one, tenant_role_name_two],
            role_name=role_name, user_group_role_name=user_group_role_name,
            identity_role_name=const.USER_ADMIN_ROLE_NAME,
            number_of_tenant_assignments=6, params={
                'tenantid': tenant_one.id
            }
        )

        # validate effective roles as identity:user-admin with unknown tenant
        self.validate_effective_roles(
            self.user_admin_client.default_headers[const.X_USER_ID],
            self.user_admin_client,
            [tenant_role_name_one, tenant_role_name_two],
            role_name=role_name, user_group_role_name=user_group_role_name,
            identity_role_name=const.USER_ADMIN_ROLE_NAME,
            number_of_tenant_assignments=0, params={
                const.ON_TENANT_ID: '{}unknown'.format(tenant_one.id)
            }
        )

        # validate effective roles as identity:admin
        self.validate_effective_roles(
            self.user_admin_client.default_headers[const.X_USER_ID],
            self.identity_admin_client,
            [tenant_role_name_one, tenant_role_name_two],
            role_name=role_name,
            user_group_role_name=user_group_role_name,
            identity_role_name=const.USER_ADMIN_ROLE_NAME,
            number_of_tenant_assignments=6)

        # validate effective roles as identity:admin with filter: tenant one
        self.validate_effective_roles(
            self.user_admin_client.default_headers[const.X_USER_ID],
            self.identity_admin_client, [tenant_role_name_one],
            role_name=role_name, user_group_role_name=user_group_role_name,
            identity_role_name=const.USER_ADMIN_ROLE_NAME,
            number_of_tenant_assignments=5, params={
                const.ON_TENANT_ID: tenant_one.id
            }
        )

        # validate effective roles as identity:admin with filter: tenant two
        self.validate_effective_roles(
            self.user_admin_client.default_headers[const.X_USER_ID],
            self.identity_admin_client, [tenant_role_name_two],
            role_name=role_name,
            identity_role_name=const.USER_ADMIN_ROLE_NAME,
            number_of_tenant_assignments=4, params={
                const.ON_TENANT_ID: tenant_two.id
            }
        )

        # validate effective roles as identity:admin with invalid filter
        self.validate_effective_roles(
            self.user_admin_client.default_headers[const.X_USER_ID],
            self.identity_admin_client,
            [tenant_role_name_one, tenant_role_name_two],
            role_name=role_name, user_group_role_name=user_group_role_name,
            identity_role_name=const.USER_ADMIN_ROLE_NAME,
            number_of_tenant_assignments=6, params={
                'tenantid': tenant_one.id
            }
        )

        # validate effective roles as identity:admin with unknown tenant
        self.validate_effective_roles(
            self.user_admin_client.default_headers[const.X_USER_ID],
            self.identity_admin_client,
            [tenant_role_name_one, tenant_role_name_two],
            role_name=role_name, user_group_role_name=user_group_role_name,
            identity_role_name=const.USER_ADMIN_ROLE_NAME,
            number_of_tenant_assignments=0, params={
                const.ON_TENANT_ID: '{}unknown'.format(tenant_one.id)
            }
        )

    def create_group_and_role_and_add_to_tenant(self, parent_client,
                                                child_client, domain_id,
                                                tenant_id):
        # create user group role
        user_group_role_id, user_group_role_name = self.create_role(
            admin_role=const.USER_MANAGE_ROLE_NAME)
        # Create user group; add to user-default
        user_group = self.add_user_to_user_group(
            parent_client=parent_client,
            child_client=child_client
        )
        # add role to user group
        resp = self.user_manager_client.add_role_to_user_group_for_tenant(
            domain_id=domain_id,
            group_id=user_group.id,
            role_id=user_group_role_id,
            tenant_id=tenant_id)

        self.assertEqual(resp.status_code, 204)

        return user_group_role_name

    def create_role_and_add_to_user(self, user_id):
        # create global role
        role_id, role_name = self.create_role()
        # add global role to manage
        resp = self.identity_admin_client.add_role_to_user(
            role_id=role_id,
            user_id=user_id)
        self.assertEqual(resp.status_code, 200)

        return role_name

    def create_tenant_and_add_role_on_tenant_for_user(self, user_id):
        # create tenant
        tenant = self.create_tenant()
        # create tenant role
        tenant_role_id, tenant_role_name = self.create_role()
        # add tenant role to sub user for tenant
        resp = self.identity_admin_client.add_role_to_user_for_tenant(
            tenant_id=tenant.id,
            user_id=user_id,
            role_id=tenant_role_id)
        self.assertEqual(resp.status_code, 200)

        return tenant, tenant_role_name

    def validate_effective_roles(self, user_id, auth_client,
                                 tenant_role_name_list=[], role_name=None,
                                 user_group_role_name=None,
                                 identity_role_name=None,
                                 number_of_tenant_assignments=5, params=None):
        # get roles as user_id
        resp = auth_client.list_effective_roles_for_user(
            user_id=user_id,
            params=params
        )

        # validate return 200
        self.assertEqual(resp.status_code, 200)

        # tenant role, identity:default, global role, user group role
        # tenant access
        self.assertEqual(
            len(resp.json()[const.RAX_AUTH_ROLE_ASSIGNMENTS][
                    const.TENANT_ASSIGNMENTS]),
            number_of_tenant_assignments)

        if number_of_tenant_assignments > 0:
            self.validate_assignment_checks(
                resp, tenant_role_name_list, role_name, user_group_role_name,
                identity_role_name=identity_role_name
            )

    def validate_assignment_checks(
            self, resp, tenant_role_name_list, role_name,
            user_group_role_name,
            identity_role_name=const.USER_DEFAULT_ROLE_NAME):
        tenant_role_assignment_checked = False
        user_group_role_assignment_checked = False
        identity_role_assignment_checked = False
        global_role_assignment_checked = False
        tenant_access_role_assignment_checked = False

        for tenant_assignment in resp.json()[
                const.RAX_AUTH_ROLE_ASSIGNMENTS][const.TENANT_ASSIGNMENTS]:
            if tenant_assignment["onRoleName"] in tenant_role_name_list:
                tenant_role_assignment_checked = True
                self.assertEqual(
                    tenant_assignment["sources"][0]["sourceType"],
                    const.USER_SOURCE_TYPE
                )
                self.assertEqual(
                    tenant_assignment["sources"][0]["assignmentType"],
                    const.TENANT_ASSIGNMENT_TYPE
                )
            elif tenant_assignment[
                    "onRoleName"] == identity_role_name:
                identity_role_assignment_checked = True
                self.assertEqual(
                    tenant_assignment["sources"][0]["sourceType"],
                    const.USER_SOURCE_TYPE
                )
                self.assertEqual(
                    tenant_assignment["sources"][0]["assignmentType"],
                    const.DOMAIN_ASSIGNMENT_TYPE
                )
                self.assertEqual(
                    len(tenant_assignment["forTenants"]),
                    len(tenant_role_name_list)
                )
            elif tenant_assignment["onRoleName"] == role_name:
                global_role_assignment_checked = True
                self.assertEqual(
                    tenant_assignment["sources"][0]["sourceType"],
                    const.USER_SOURCE_TYPE
                )
                self.assertEqual(
                    tenant_assignment["sources"][0]["assignmentType"],
                    const.DOMAIN_ASSIGNMENT_TYPE
                )
                self.assertEqual(
                    len(tenant_assignment["forTenants"]),
                    len(tenant_role_name_list)
                )
            elif tenant_assignment[
                    "onRoleName"] == const.TENANT_ACCESS_ROLE_NAME:
                tenant_access_role_assignment_checked = True
                self.assertEqual(
                    tenant_assignment["sources"][0]["sourceType"],
                    const.SYSTEM_SOURCE_TYPE
                )
                self.assertEqual(
                    tenant_assignment["sources"][0]["assignmentType"],
                    const.TENANT_ASSIGNMENT_TYPE
                )
                self.assertEqual(
                    len(tenant_assignment["forTenants"]),
                    len(tenant_role_name_list)
                )
            elif tenant_assignment["onRoleName"] == user_group_role_name:
                user_group_role_assignment_checked = True
                self.assertEqual(
                    tenant_assignment["sources"][0]["sourceType"],
                    const.USERGROUP_SOURCE_TYPE
                )
                self.assertEqual(
                    tenant_assignment["sources"][0]["assignmentType"],
                    const.TENANT_ASSIGNMENT_TYPE
                )

        if len(tenant_role_name_list) > 0:
            self.assertEqual(tenant_role_assignment_checked, True)
        if identity_role_name is not None:
            self.assertEqual(identity_role_assignment_checked, True)
        if role_name is not None:
            self.assertEqual(global_role_assignment_checked, True)
        if user_group_role_name is not None:
            self.assertEqual(user_group_role_assignment_checked, True)

        # tenant-access is always there
        self.assertEqual(tenant_access_role_assignment_checked, True)

    def create_role(self, admin_role=None, role_name=None):
        if role_name is None:
            role_name = self.generate_random_string(
                pattern=const.ROLE_NAME_PATTERN)
        if admin_role:
            role_object = factory.get_add_role_request_object(
                role_name=role_name,
                administrator_role=admin_role)
        else:
            role_object = factory.get_add_role_request_object(
                role_name=role_name)
        resp = self.identity_admin_client.add_role(request_object=role_object)
        self.assertEqual(resp.status_code, 201)
        role_id = resp.json()[const.ROLE][const.ID]
        self.role_ids.append((role_id, role_name))
        return role_id, role_name

    def create_tenant(self):
        tenant_req = factory.get_add_tenant_object(domain_id=self.domain_id)
        add_tenant_resp = self.identity_admin_client.add_tenant(
            tenant=tenant_req)
        self.assertEqual(add_tenant_resp.status_code, 201)
        tenant = responses.Tenant(add_tenant_resp.json())
        self.tenant_ids.append(tenant.id)
        return tenant

    def add_user_to_user_group(self, parent_client, child_client):
        group_req = factory.get_add_user_group_request(self.domain_id)
        resp = parent_client.add_user_group_to_domain(
            domain_id=self.domain_id, request_object=group_req)
        self.assertEqual(resp.status_code, 201)
        group = responses.UserGroup(resp.json())

        # adding user to the user group
        add_resp = parent_client.add_user_to_user_group(
            user_id=child_client.default_headers[
                const.X_USER_ID],
            group_id=group.id, domain_id=self.domain_id
        )
        self.assertEqual(add_resp.status_code, 204)
        return group

    @base.base.log_tearDown_error
    @unless_coverage
    def tearDown(self):
        # Delete sub users created in the setUpClass
        resp = self.identity_admin_client.delete_user(
            self.user_client.default_headers[const.X_USER_ID])
        assert resp.status_code == 204, (
            'User with ID {0} failed to delete'.format(
                self.user_client.default_headers[const.X_USER_ID]))
        resp = self.identity_admin_client.delete_user(
            self.user_manager_client.default_headers[const.X_USER_ID])
        assert resp.status_code == 204, (
            'User with ID {0} failed to delete'.format(
                self.user_manager_client.default_headers[const.X_USER_ID]))
        # Delete client will delete user-admin, tenant & the domain
        self.delete_client(client=self.user_admin_client,
                           parent_client=self.identity_admin_client)
        for role_id, role_name in self.role_ids:
            resp = self.identity_admin_client.delete_role(role_id=role_id)
            assert resp.status_code == 204, (
                'Role with ID {0} failed to delete. Got {1} for {2}'.format(
                    role_id, resp.status_code, role_name))
        super(TestListEffectiveRolesForUser, self).tearDown()
