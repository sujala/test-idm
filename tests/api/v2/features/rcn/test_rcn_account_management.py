# -*- coding: utf-8 -*
from urlparse import urljoin

from tests.api.v2 import base
from tests.api.v2.models import factory

from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


"""
Verify Assign global endpoints for all roles on tenant
1. A user must receive the global endpoints associated will all roles the user
   has on any given tenant
2. This functionality must be feature flagged such that when disabled,
   the existing behavior is preserved.
   2.1 Both behaviors should have sufficient tests demonstrating the defect
   (if flag is disabled), and showing the defect resolved (if flag is enabled)
   --> this case have to manually flip flag true/false and verify
   (this test need to add specific roles to ldif in order to verify it
   correctly, only run against local and jenkins)

This test class is unneccessary if the performant service catalog feature flag
is enabled. That flag will disable the generic global endpoint functionality
which is tested, in part, by this test class. The test_global_endpoints
tests perform the appropriate testing when this flag is enabled.
"""


class TestRCNAccountManagement(base.TestBaseV2):
    """ Test Assign global endpoint for all roll on tenant"""

    @classmethod
    def setUpClass(cls):
        super(TestRCNAccountManagement, cls).setUpClass()
        # These are preset role names to ldif to follow alphabetical order
        cls.ROLE_NAME1 = 'testRole1'
        cls.ROLE_NAME2 = 'testRole2'
        cls.ROLE_NAME3 = 'testRole3'

    def setUp(self):
        super(TestRCNAccountManagement, self).setUp()
        if not self.test_config.run_service_admin_tests:
            self.skipTest('Skipping Service Admin Tests per config value')
        if not self.test_config.run_local_and_jenkins_only:
            self.skipTest('Skipping local and jenkins run tests')
        # hard code to get specific service for compute
        self.SERVICE_NAME = 'cloudServers'
        self.service_id = self.get_service_id_by_name(
            service_name=self.SERVICE_NAME)
        self.user_ids = []
        self.tenant_ids = []
        self.domain_ids = []
        self.role_ids = []
        self.service_ids = []
        self.template_ids = []

    def create_admin_user(self):
        """regular"""
        request_object = factory.get_add_user_request_object()
        resp = self.identity_admin_client.add_user(
            request_object=request_object)
        self.assertEqual(resp.status_code, 201)
        user_id = resp.json()[const.USER][const.ID]
        self.user_ids.append(user_id)
        return user_id, resp

    def create_admin_user_one_call(self):
        """Using 1 call logic"""
        domain_id = self.generate_random_string(pattern=const.NUMBERS_PATTERN)
        request_object = factory.get_add_user_request_object_pull(
            domain_id=domain_id)
        resp = self.identity_admin_client.add_user(
            request_object=request_object)
        self.assertEqual(resp.status_code, 201)
        user_id = resp.json()[const.USER][const.ID]
        self.user_ids.append(user_id)
        return user_id, resp

    def create_tenant(self):
        tenant_types = ['cloud']
        tenant_name = self.generate_random_string(
            pattern=const.NUMBERS_PATTERN)
        tenant_req = factory.get_add_tenant_request_object(
            tenant_name=tenant_name, tenant_types=tenant_types)
        resp = self.identity_admin_client.add_tenant(tenant=tenant_req)
        self.assertEqual(resp.status_code, 201)
        tenant_id = resp.json()[const.TENANT][const.ID]
        self.tenant_ids.append(tenant_id)
        return tenant_id, resp

    def create_service(self):
        request_object = factory.get_add_service_object()
        resp = self.service_admin_client.add_service(
            request_object=request_object)
        self.assertEqual(resp.status_code, 201)
        service_id = resp.json()[const.NS_SERVICE][const.ID]
        self.service_ids.append(service_id)
        service_name = resp.json()[const.NS_SERVICE][const.NAME]
        return service_id, service_name

    def get_service_id_by_name(self, service_name):
        option = {'name': service_name}
        resp = self.service_admin_client.list_services(option=option)
        self.assertEqual(resp.status_code, 200)
        service_id = resp.json()[const.NS_SERVICES][0][const.ID]
        return service_id

    def create_role(self, service_id):
        role_obj = factory.get_add_role_request_object(service_id=service_id)
        resp = self.identity_admin_client.add_role(request_object=role_obj)
        self.assertEqual(resp.status_code, 201)
        role_id = resp.json()[const.ROLE][const.ID]
        self.role_ids.append(role_id)
        return role_id, resp

    def create_endpoint_template(self, service_id):
        request_object = factory.get_add_endpoint_template_object(
            service_id=service_id
        )
        resp = self.identity_admin_client.add_endpoint_template(
            request_object=request_object
        )
        self.assertEqual(resp.status_code, 201)
        template_id = resp.json()[const.OS_KSCATALOG_ENDPOINT_TEMPLATE][
            const.ID]
        self.template_ids.append(template_id)
        return template_id, resp

    def update_endpoint_template(self, template_id, update_input=None):
        if not update_input:
            update_input = {
                "default": False,
                "global_attr": True,
                "enabled": True
            }
        update_obj = requests.EndpointTemplateUpdate(template_id=template_id,
                                                     **update_input)
        resp = self.service_admin_client.update_endpoint_template(
            template_id=template_id, request_object=update_obj)
        self.assertEqual(resp.status_code, 200)
        return resp

    def get_role_by_name(self, role_name):
        """return a role id"""
        option = {'roleName': role_name}
        resp = self.identity_admin_client.list_roles(option=option)
        self.assertEqual(resp.status_code, 200)
        return resp.json()[const.ROLES][0][const.ID]

    def test_assign_global_endpoints_for_all_roles_on_tenant_case1(self):
        """3.7.0 features assign global endpoints for all roles on tenant
            this test need specific roles setting in order to verify correctly
            roles are previously added to ldif
            This case verify feature flag default (False)
            Compute Role (ROLE_ID2) comes after identity role (ROLE_ID1)
            no new global endpoint in service catalog
        """
        # get identity role
        role_id1 = self.get_role_by_name(role_name=self.ROLE_NAME1)

        # create user
        user_id, user_resp = self.create_admin_user()
        user_name = user_resp.json()[const.USER][const.USERNAME]
        password = user_resp.json()[const.USER][const.NS_PASSWORD]

        # authenticate with user info
        auth_obj = requests.AuthenticateWithPassword(
            user_name=user_name, password=password
        )
        resp1 = self.identity_admin_client.get_auth_token(
            request_object=auth_obj)
        self.assertEqual(resp1.status_code, 200)

        # create tenant
        tenant_id, tenant_resp = self.create_tenant()

        # add role to user on tenant
        add_resp1 = self.identity_admin_client.add_role_to_user_for_tenant(
            tenant_id=tenant_id, user_id=user_id, role_id=role_id1
        )
        self.assertEqual(add_resp1.status_code, 200)

        # add compute role to user on tenant
        add_resp2 = self.identity_admin_client.add_role_to_user_for_tenant(
            tenant_id=tenant_id, user_id=user_id, role_id=const.COMPUTE_ROLE_ID
        )
        self.assertEqual(add_resp2.status_code, 200)

        # add endpoint template
        template_id, temp_resp = self.create_endpoint_template(
            service_id=self.service_id)
        template_name = temp_resp.json()[const.OS_KSCATALOG_ENDPOINT_TEMPLATE][
            const.NAME]
        public_url = temp_resp.json()[const.OS_KSCATALOG_ENDPOINT_TEMPLATE][
            const.PUBLIC_URL]

        # update endpoint template
        self.update_endpoint_template(template_id=template_id)

        # authenticate with user info
        auth_resp = self.identity_admin_client.get_auth_token(
            request_object=auth_obj)
        self.assertEqual(auth_resp.status_code, 200)

        endpoints_list = auth_resp.json()[const.ACCESS][
            const.SERVICE_CATALOG]
        # verify endpoint with new service name in the endpionts list
        self.assertIn(template_name, str(endpoints_list))
        endpoints = [item for item in endpoints_list
                     if item[const.NAME] == template_name]
        self.assertEqual(endpoints[0][const.ENDPOINTS][0][const.TENANT_ID],
                         tenant_id)
        self.assertEqual(endpoints[0][const.ENDPOINTS][0][
                             const.PUBLIC_URL], urljoin(public_url,
                                                        tenant_id))

    def test_assign_global_endpoints_for_all_roles_on_tenant_case2(self):
        """3.7.0 features assign global endpoints for all roles on tenant
            this test need specific role setting in order to verify correctly
            roles are previously added to ldif
            This case verify feature flag default (False)
            Compute Role (ROLE_ID1) comes before identity role (ROLE_ID2)
            new global endpoint in service catalog
        """
        # get identity role
        role_id2 = self.get_role_by_name(role_name=self.ROLE_NAME3)

        # create user
        user_id, user_resp = self.create_admin_user()
        user_name = user_resp.json()[const.USER][const.USERNAME]
        password = user_resp.json()[const.USER][const.NS_PASSWORD]

        # authenticate with user info
        auth_obj = requests.AuthenticateWithPassword(
            user_name=user_name, password=password
        )
        resp1 = self.identity_admin_client.get_auth_token(
            request_object=auth_obj)
        self.assertEqual(resp1.status_code, 200)

        # create tenant
        tenant_id, tenant_resp = self.create_tenant()

        # add role to user on tenant
        add_resp1 = self.identity_admin_client.add_role_to_user_for_tenant(
            tenant_id=tenant_id, user_id=user_id, role_id=const.COMPUTE_ROLE_ID
        )
        self.assertEqual(add_resp1.status_code, 200)

        # add identity role to user on tenant
        add_resp2 = self.identity_admin_client.add_role_to_user_for_tenant(
            tenant_id=tenant_id, user_id=user_id, role_id=role_id2
        )
        self.assertEqual(add_resp2.status_code, 200)

        # add endpoint template
        template_id, temp_resp = self.create_endpoint_template(
            service_id=self.service_id)
        template_name = temp_resp.json()[const.OS_KSCATALOG_ENDPOINT_TEMPLATE][
            const.NAME]
        template_type = temp_resp.json()[const.OS_KSCATALOG_ENDPOINT_TEMPLATE][
            const.TYPE]
        public_url = temp_resp.json()[const.OS_KSCATALOG_ENDPOINT_TEMPLATE][
            const.PUBLIC_URL]

        # update endpoint template
        self.update_endpoint_template(template_id=template_id)

        # authenticate with user info
        auth_resp = self.identity_admin_client.get_auth_token(
            request_object=auth_obj)
        self.assertEqual(auth_resp.status_code, 200)

        endpoints_list = auth_resp.json()[const.ACCESS][
            const.SERVICE_CATALOG]
        # since compute role comes before identity role in alphabetical order
        # the endpoint in service catalog no matter feature flag true or false
        self.assertIn(template_name, str(endpoints_list))
        self.assertIn(template_type, str(endpoints_list))
        self.assertIn(public_url, str(endpoints_list))

        # get endpoints for tenant
        endpoints = [item for item in endpoints_list
                     if item[const.ENDPOINTS][0][const.TENANT_ID] == tenant_id]
        self.assertEqual(len(endpoints), 1)
        self.assertEqual(endpoints[0][const.ENDPOINTS][0][
                             const.PUBLIC_URL], urljoin(public_url,
                                                        tenant_id))

    def test_assign_global_endpoints_for_all_roles_on_mosso_tenant(self):
        """3.7.0 features assign global endpoints for all roles on tenant
            This case role randomly create so added global endpoint may or
            may not in the service catalog so just verify when the feature
            flag is True.
        """

        # create user
        user_id, user_resp = self.create_admin_user_one_call()
        user_name = user_resp.json()[const.USER][const.USERNAME]
        password = user_resp.json()[const.USER][const.NS_PASSWORD]
        domain_id = user_resp.json()[const.USER][const.RAX_AUTH_DOMAIN_ID]
        self.domain_ids.append(domain_id)

        # authenticate with user info
        auth_obj = requests.AuthenticateWithPassword(
            user_name=user_name, password=password
        )
        resp1 = self.identity_admin_client.get_auth_token(
            request_object=auth_obj)
        self.assertEqual(resp1.status_code, 200)

        # get mosso tenant
        tenant_id = resp1.json()[const.ACCESS][const.TOKEN][const.TENANT][
            const.ID]
        self.tenant_ids.append(tenant_id)

        # create service
        service_id, service_name = self.create_service()

        # create role
        role_id, _ = self.create_role(service_id=service_id)

        # add role to user on (mosso) tenant
        add_resp = self.identity_admin_client.add_role_to_user_for_tenant(
            tenant_id=tenant_id, user_id=user_id, role_id=role_id
        )
        self.assertEqual(add_resp.status_code, 200)

        # add endpoint template
        template_id, temp_resp = self.create_endpoint_template(
            service_id=service_id)
        public_url = temp_resp.json()[const.OS_KSCATALOG_ENDPOINT_TEMPLATE][
            const.PUBLIC_URL]

        # update endpoint template
        self.update_endpoint_template(template_id=template_id)

        # authenticate with user info
        auth_resp = self.identity_admin_client.get_auth_token(
            request_object=auth_obj)
        self.assertEqual(auth_resp.status_code, 200)

        endpoints_list = auth_resp.json()[const.ACCESS][
            const.SERVICE_CATALOG]
        # verify endpoint with new service name is in the endpoints list
        self.assertIn(service_name, str(endpoints_list))
        endpoints = [item for item in endpoints_list
                     if item[const.NAME] == service_name]
        self.assertEqual(endpoints[0][const.ENDPOINTS][0][const.TENANT_ID],
                         tenant_id)
        self.assertEqual(endpoints[0][const.ENDPOINTS][0][
                             const.PUBLIC_URL], urljoin(public_url,
                                                        tenant_id))

        # verify endpoint for the compute role exists in the endpoints list
        self.assertIn('cloudServers', str(endpoints_list))
        endpoints = [item for item in endpoints_list
                     if item[const.NAME] == 'cloudServers']
        self.assertEqual(endpoints[0][const.ENDPOINTS][0][const.TENANT_ID],
                         tenant_id)

    def tearDown(self):
        # Delete all resources created in the tests
        for id_ in self.user_ids:
            self.identity_admin_client.delete_user(user_id=id_)
        for id_ in self.tenant_ids:
            self.identity_admin_client.delete_tenant(tenant_id=id_)
        for id_ in self.domain_ids:
            disable_domain_req = requests.Domain(enabled=False)
            self.identity_admin_client.update_domain(
                domain_id=id_, request_object=disable_domain_req)
            self.identity_admin_client.delete_domain(domain_id=id_)
        for id_ in self.role_ids:
            self.identity_admin_client.delete_role(role_id=id_)
        for id_ in self.template_ids:
            # update endpoint template to disabled before able to delete
            update_input = {
                "default": False,
                "global_attr": False,
                "enabled": False
            }
            self.update_endpoint_template(template_id=id_,
                                          update_input=update_input)
            self.identity_admin_client.delete_endpoint_template(
                template_id=id_)
        for id_ in self.service_ids:
            self.service_admin_client.delete_service(service_id=id_)
        super(TestRCNAccountManagement, self).tearDown()
