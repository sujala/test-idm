from qe_coverage.opencafe_decorators import tags, unless_coverage

from tests.api.v2 import base
from tests.api.v2.models import responses
from tests.api.v2.models import factory
from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


class AuthUserWithRoleMissingOpenstackType(base.TestBaseV2):
    """
    1.  A user assigned a role that exists under an application that does not
        have an openstack type:
    1.1 Must be allowed to authenticate
    1.2 Must not receive global endpoints based on having this role
        (user may get global endpoints based on other roles)
    """

    # NOTE: This service_id was found using ADS(clientId).
    # if anyone deleles it from LDAP then this test won't work.
    service_with_undefined_openstacktype = "02aec197e76448b6b3730725ccbc2c3c"

    def service_admin(self):
        return (self.test_config.run_local_and_jenkins_only and
                self.test_config.run_service_admin_tests)

    @unless_coverage
    def setUp(self):
        super(AuthUserWithRoleMissingOpenstackType, self).setUp()
        if not self.service_admin():
            self.skipTest("Environment is not set")
        self.user_ids = []
        self.role_ids = []
        self.user_ids = []
        self.service_ids = []
        self.template_ids = []
        self.tenant_ids = []
        self.domain_ids = []
        self.tenant_id_template_id_tuples = []
        self.endpoint_attributes = {
            'public_url': 'https://www.test_public_endpoint_special.com',
            'internal_url': 'https://www.test_internal_endpoint_special.com',
            'admin_url': 'https://www.test_admin_endpoint_special.com',
            'version_info': 'test_version_special_info',
            'version_id': '1',
            'version_list': 'test_version_special_list',
            'region': 'ORD'}

    def create_user_with_role_and_tenant(self, role_name):
        # Set up roles params
        tenant_id = self.generate_random_string(
            pattern=const.UPPER_CASE_LETTERS)
        roles = [{const.NAME: role_name, const.TENANT_ID: tenant_id}]

        # Create user with roles and tenantID
        uadm_username = self.generate_random_string(
            pattern=const.USER_ADMIN_PATTERN)
        uadm_domain_id = self.generate_random_string(
            pattern=const.UPPER_CASE_LETTERS)
        input_data = {'email': const.EMAIL_RANDOM,
                      'roles': roles,
                      'secret_qa':
                          {const.SECRET_QUESTION: const.SECRET_QUESTION,
                           const.SECRET_ANSWER: const.SECRET_ANSWER},
                      'domain_id': uadm_domain_id}
        req_obj = requests.UserAdd(user_name=uadm_username, **input_data)
        resp = self.identity_admin_client.add_user(request_object=req_obj)
        self.assertEqual(resp.status_code, 201)
        self.tenant_ids.append(tenant_id)

        new_user_id = resp.json()[const.USER][const.ID]
        self.user_ids.append(new_user_id)
        self.domain_ids.append(uadm_domain_id)
        uadm_password = resp.json()[const.USER][const.NS_PASSWORD]

        req_obj = requests.AuthenticateWithPassword(
            user_name=uadm_username,
            password=uadm_password)
        resp = self.identity_admin_client.get_auth_token(
            request_object=req_obj)
        self.assertEqual(resp.status_code, 200)

        return uadm_username, uadm_password, tenant_id, new_user_id

    def create_role(self, service_id):
        new_role_name = self.generate_random_string(
            pattern=const.ROLE_NAME_PATTERN)
        req_obj = factory.get_add_role_request_object(
            role_name=new_role_name,
            assignment='BOTH',
            service_id=service_id)
        resp = self.identity_admin_client.add_role(request_object=req_obj)
        self.assertEqual(resp.status_code, 201)
        new_role_id = resp.json()[const.ROLE][const.ID]
        self.role_ids.append(new_role_id)
        return new_role_name, new_role_id

    def create_endpoint_template(self, service_id):
        endpoint_template = requests.EndpointTemplateAdd(
            template_id=self.generate_random_string(
                const.NUMERIC_DOMAIN_ID_PATTERN),
            assignment_type='MOSSO',
            service_id=service_id,
            **self.endpoint_attributes)
        resp = self.identity_admin_client.add_endpoint_template(
            endpoint_template)
        self.assertEqual(resp.status_code, 201)
        template_id = resp.json()[const.OS_KSCATALOG_ENDPOINT_TEMPLATE][
                                  const.ID]
        self.template_ids.append(template_id)
        return template_id

    def update_endpoint_to_global_enabled(self, template_id):
        kwargs = {"global_attr": True, "enabled": True}
        req_obj = requests.EndpointTemplateUpdate(
            template_id=template_id, **kwargs)
        resp = self.service_admin_client.update_endpoint_template(
            template_id=template_id, request_object=req_obj)
        self.assertEqual(resp.status_code, 200)

    def create_service(self):
        service_name = self.generate_random_string(
            pattern=const.SERVICE_NAME_PATTERN)
        service_type = 'compute'
        service_id = self.generate_random_string(pattern='[\d]{8}')
        service_description = 'SERVICEMETEST'
        request_object = requests.ServiceAdd(
            service_id=service_id, service_name=service_name,
            service_type=service_type,
            service_description=service_description)
        resp = self.service_admin_client.add_service(
            request_object=request_object)
        self.assertEqual(resp.status_code, 201)
        service = responses.Service(resp.json())
        service_id = service.id
        service_type = service.type
        self.service_ids.append(service_id)
        return service_name, service_id

    def get_service_catalog(self, username, password):
        kwargs = {'user_name': username,
                  'password': password}
        request_object = requests.AuthenticateWithPassword(**kwargs)
        resp = self.identity_admin_client.get_auth_token(
            request_object=request_object)
        self.assertEqual(resp.status_code, 200)
        return resp.json()[const.ACCESS][const.SERVICE_CATALOG]

    @tags('positive', 'p1', 'regression')
    def test_can_auth_user_with_role_missing_openstacktype(self):
        # Create Role with service missing openstacktype
        new_role_name, new_role_id = self.create_role(
            self.service_with_undefined_openstacktype)
        # Create user with role
        user_name, user_password, tenant_id, user_id = (
            self.create_user_with_role_and_tenant(role_name=new_role_name))
        # Auth
        req_obj = requests.AuthenticateWithPassword(
            user_name=user_name,
            password=user_password)
        resp = self.identity_admin_client.get_auth_token(
            request_object=req_obj)
        self.assertEqual(resp.status_code, 200)

    @tags('positive', 'p1', 'regression')
    def test_no_endpoints_service_missing_openstacktype_but_still_allow_others(
      self):
        # Create Role with service missing openstacktype
        new_role_name, new_role_id = self.create_role(
            self.service_with_undefined_openstacktype)
        # Create Endpoint Template
        template_id = self.create_endpoint_template(
            self.service_with_undefined_openstacktype)
        # Update endpoint to be global & enabled
        self.update_endpoint_to_global_enabled(template_id)
        # Create user with role
        user_name, user_password, tenant_id, user_id = (
            self.create_user_with_role_and_tenant(role_name=new_role_name))
        # Get serviceCatalog
        service_catalog = self.get_service_catalog(user_name, user_password)
        # should be empty
        self.assertEqual(
            [],
            service_catalog,
            msg="Should not have found custom endpoints in catalog")
        """
        Now make sure a role on a service with a valid openstacktype
        has its custom endpoints show up in the serviceCatalog
        """
        # Create Service *WITH A VALID OPENSTACKTYPE*
        service_name, service_id = self.create_service()
        # Create Role
        new_role_name, new_role_id = self.create_role(service_id)
        # Create Endpoint Template
        template_id = self.create_endpoint_template(service_id)
        # Update endpoint to be global & enabled
        self.update_endpoint_to_global_enabled(template_id)
        # Add role to user for tenant
        resp = self.service_admin_client.add_role_to_user_for_tenant(
            user_id=user_id, tenant_id=tenant_id, role_id=new_role_id)
        self.assertEqual(resp.status_code, 200)
        # Add compute:default role to user for tenant
        resp = self.service_admin_client.add_role_to_user_for_tenant(
            user_id=user_id,
            tenant_id=tenant_id,
            role_id=const.COMPUTE_ROLE_ID)
        self.assertEqual(resp.status_code, 200)
        # get service catalog
        service_catalog = self.get_service_catalog(user_name, user_password)
        self.assertNotEqual(
            [],
            [service for service in service_catalog if service[
                const.NAME] == service_name],
            msg="service name not found: {0}".format(service_name))

    @unless_coverage
    def tearDown(self):
        if not self.test_config.run_service_admin_tests:
            return
        kwargs = {"global_attr": True, "enabled": False}
        for pair in self.tenant_id_template_id_tuples:
            self.service_admin_client.delete_endpoint_from_tenant(
                tenant_id=pair[0], endpoint_template_id=pair[1])
        for tenant_id in self.tenant_ids:
            self.service_admin_client.delete_tenant(tenant_id=tenant_id)
        for template_id in self.template_ids:
            req_obj = requests.EndpointTemplateUpdate(
                template_id=template_id, **kwargs)
            self.service_admin_client.update_endpoint_template(
                template_id=template_id, request_object=req_obj)
            self.service_admin_client.delete_endpoint_template(
                template_id=template_id)
        for user_id in self.user_ids:
            self.service_admin_client.delete_user(user_id=user_id)
        for role_id in self.role_ids:
            self.service_admin_client.delete_role(role_id=role_id)
        for domain_id in self.domain_ids:
            disable_domain_req = requests.Domain(enabled=False)
            self.identity_admin_client.update_domain(
                domain_id=domain_id, request_object=disable_domain_req)
            self.service_admin_client.delete_domain(domain_id=domain_id)
        for service_id in self.service_ids:
            self.service_admin_client.delete_service(service_id=service_id)
