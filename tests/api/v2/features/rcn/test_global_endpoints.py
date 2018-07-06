# -*- coding: utf-8 -*
from nose.plugins.attrib import attr
from urlparse import urljoin
from qe_coverage.opencafe_decorators import tags, unless_coverage

from tests.api.v2 import base
from tests.api.v2.models import factory

from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests

"""
Verify Assign global endpoints for compute:default and object-store:default
 roles when feature flag is enabled
1. A user must receive the global endpoints associated will compute:default
  role (endpoint baseUrlType = MOSSO) and object-store:default role (endpoint
  baseUrlType = NAST) on any given tenant for which they have the role
2. This also verifies that a user can receive global endpoints for both NAST
  and MOSSO on the same tenant.
"""


class TestGlobalEndpoints(base.TestBaseV2):
    """ Test Assign global endpoint for compute:default and
    object-store:default roles on tenant
    """

    @classmethod
    @unless_coverage
    def setUpClass(cls):
        super(TestGlobalEndpoints, cls).setUpClass()
        # These are preset role names to ldif to follow alphabetical order
        cls.ROLE_NAME_COMPUTE = 'compute:default'
        cls.ROLE_NAME_FILES = 'object-store:default'

    def create_tenant_type(self, name):
        request_object = requests.TenantType(name, 'description')
        self.service_admin_client.add_tenant_type(tenant_type=request_object)
        self.tenant_type_ids.append(name.lower())

    @unless_coverage
    def setUp(self):
        super(TestGlobalEndpoints, self).setUp()
        if not self.test_config.run_local_and_jenkins_only:
            self.skipTest('Skipping local and jenkins run tests')
        # hard code to get specific service for compute
        self.COMPUTE_SERVICE_NAME = 'cloudServers'
        self.FILES_SERVICE_NAME = 'cloudFiles'
        self.compute_service_id = self.get_service_id_by_name(
            service_name=self.COMPUTE_SERVICE_NAME)
        self.files_service_id = self.get_service_id_by_name(
            service_name=self.FILES_SERVICE_NAME)
        self.user_ids = []
        self.tenant_ids = []
        self.domain_ids = []
        self.role_ids = []
        self.service_ids = []
        self.template_ids = []
        self.tenant_type_ids = []
        self.create_tenant_type('type1')

    def create_admin_user(self):
        """regular"""
        request_object = factory.get_add_user_request_object()
        resp = self.identity_admin_client.add_user(
            request_object=request_object)
        self.assertEqual(resp.status_code, 201)
        user_id = resp.json()[const.USER][const.ID]
        domain_id = resp.json()[const.USER][const.RAX_AUTH_DOMAIN_ID]
        self.user_ids.append(user_id)
        self.domain_ids.append(domain_id)
        return user_id, resp

    def create_tenant(self, domain_id):
        tenant_types = ['type1']
        tenant_name = self.generate_random_string(
            pattern=const.NUMBERS_PATTERN)
        tenant_req = factory.get_add_tenant_request_object(
            tenant_name=tenant_name, tenant_types=tenant_types,
            domain_id=domain_id)
        resp = self.identity_admin_client.add_tenant(tenant=tenant_req)
        self.assertEqual(resp.status_code, 201)
        tenant_id = resp.json()[const.TENANT][const.ID]
        self.tenant_ids.append(tenant_id)
        return tenant_id, resp

    def get_service_id_by_name(self, service_name):
        option = {'name': service_name}
        resp = self.service_admin_client.list_services(option=option)
        self.assertEqual(resp.status_code, 200)
        service_id = resp.json()[const.NS_SERVICES][0][const.ID]
        return service_id

    def create_endpoint_template(self, service_id, assignment_type,
                                 public_url):
        input_data = {
            "public_url": public_url,
            "internal_url": "https://compute.test_ep_template.com",
            "admin_url": "https://compute.test_ep_template.com",
            "version_info": "test_version_info",
            "version_id": "1",
            "version_list": "test_version_list",
            "region": "ORD"
        }

        request_objest = factory.get_add_endpoint_template_object(
            service_id=service_id, assignment_type=assignment_type,
            input_data=input_data
        )
        resp = self.identity_admin_client.add_endpoint_template(
            request_object=request_objest
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

    @tags('positive', 'p1', 'regression')
    @attr('skip_at_gate')
    def test_assign_global_endpoints_on_tenant(self):
        """Tests that a user receives MOSSO global endpoints when granted
        the compute:default role and NAST global endpoints when granted
        the object-store:default role
        """
        compute_role = self.get_role_by_name(role_name=self.ROLE_NAME_COMPUTE)
        files_role = self.get_role_by_name(role_name=self.ROLE_NAME_FILES)

        # create user
        user_id, user_resp = self.create_admin_user()
        user_name = user_resp.json()[const.USER][const.USERNAME]
        password = user_resp.json()[const.USER][const.NS_PASSWORD]
        domain_id = user_resp.json()[const.USER][const.RAX_AUTH_DOMAIN_ID]

        # create tenant in user domain
        tenant_id, tenant_resp = self.create_tenant(domain_id)

        # add global MOSSO endpoint template
        compute_template_id, compute_temp_resp = self.create_endpoint_template(
            service_id=self.compute_service_id, assignment_type="MOSSO",
            public_url="https://compute.test_public_endpoint_template.com")
        compute_template_name = compute_temp_resp.json()[
            const.OS_KSCATALOG_ENDPOINT_TEMPLATE][
            const.NAME]
        compute_public_url = compute_temp_resp.json()[
            const.OS_KSCATALOG_ENDPOINT_TEMPLATE][
            const.PUBLIC_URL]

        # update mosso endpoint template to active
        self.update_endpoint_template(template_id=compute_template_id)

        # add global NAST endpoint template
        files_template_id, files_temp_resp = self.create_endpoint_template(
            service_id=self.files_service_id, assignment_type="NAST",
            public_url="https://nast.test_public_endpoint_template.com")
        files_template_name = files_temp_resp.json()[
            const.OS_KSCATALOG_ENDPOINT_TEMPLATE][
            const.NAME]
        files_public_url = files_temp_resp.json()[
            const.OS_KSCATALOG_ENDPOINT_TEMPLATE][
            const.PUBLIC_URL]

        # update nast endpoint template to active
        self.update_endpoint_template(template_id=files_template_id)

        # authenticate with user info
        auth_obj = requests.AuthenticateWithPassword(
            user_name=user_name, password=password
        )
        resp1 = self.identity_admin_client.get_auth_token(
            request_object=auth_obj)
        self.assertEqual(resp1.status_code, 200)

        # endpoints not in catalog
        endpoints_list = resp1.json()[const.ACCESS][
            const.SERVICE_CATALOG]
        self.assertNotIn(compute_public_url, str(endpoints_list))
        self.assertNotIn(files_public_url, str(endpoints_list))

        # add compute role to user on tenant
        add_resp1 = self.identity_admin_client.add_role_to_user_for_tenant(
            tenant_id=tenant_id, user_id=user_id, role_id=compute_role
        )
        self.assertEqual(add_resp1.status_code, 200)

        # authenticate with user info
        auth_resp = self.identity_admin_client.get_auth_token(
            request_object=auth_obj)
        self.assertEqual(auth_resp.status_code, 200)

        endpoints_list = auth_resp.json()[const.ACCESS][
            const.SERVICE_CATALOG]

        # verify compute endpoint in the endpoints list
        self.assertIn(compute_template_name, str(endpoints_list))
        endpoints = [item for item in endpoints_list
                     if item[const.NAME] == compute_template_name]
        self.assertEqual(endpoints[0][const.ENDPOINTS][0][const.TENANT_ID],
                         tenant_id)
        self.assertEqual(endpoints[0][const.ENDPOINTS][0][
                             const.PUBLIC_URL], urljoin(compute_public_url,
                                                        tenant_id))
        self.assertNotIn(files_public_url, str(endpoints_list))

        # add nast role to user on tenant
        add_resp2 = self.identity_admin_client.add_role_to_user_for_tenant(
            tenant_id=tenant_id, user_id=user_id, role_id=files_role
        )
        self.assertEqual(add_resp2.status_code, 200)

        # authenticate with user info again
        auth_resp2 = self.identity_admin_client.get_auth_token(
            request_object=auth_obj)
        self.assertEqual(auth_resp2.status_code, 200)

        endpoints_list = auth_resp2.json()[const.ACCESS][
            const.SERVICE_CATALOG]

        # verify both endpoints in the endpoints list
        self.assertIn(files_template_name, str(endpoints_list))
        for item in endpoints_list:
            self.assertEqual(endpoints[0][const.ENDPOINTS][0][
                                 const.TENANT_ID], tenant_id)
            if item[const.NAME] == files_template_name:
                self.assertEqual(item[const.ENDPOINTS][0][
                                     const.PUBLIC_URL],
                                 urljoin(files_public_url,
                                         tenant_id))
            if item[const.NAME] == compute_template_name:
                self.assertEqual(item[const.ENDPOINTS][0][
                                     const.PUBLIC_URL],
                                 urljoin(compute_public_url,
                                         tenant_id))

    @unless_coverage
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
        for name in self.tenant_type_ids:
            self.service_admin_client.delete_tenant_type(name=name)
        super(TestGlobalEndpoints, self).tearDown()

    @classmethod
    @unless_coverage
    def tearDownClass(cls):
        super(TestGlobalEndpoints, cls).tearDownClass()
