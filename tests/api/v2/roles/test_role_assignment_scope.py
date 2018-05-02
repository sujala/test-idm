from nose.plugins.attrib import attr
import ddt

from tests.api.v2 import base
from tests.api.v2.schema import roles as roles_json
from tests.api.v2.models import factory
from tests.package.johny import constants as const
from tests.package.johny.v2 import client


@ddt.ddt
class TestRoleAssignmentFeature(base.TestBaseV2):
    """
    Verify RAX-AUTH:assignment behavior
    """
    @classmethod
    def setUpClass(cls):
        """Class level set up for the tests
        """
        super(TestRoleAssignmentFeature, cls).setUpClass()
        if cls.test_config.run_service_admin_tests:
            cls.service_admin_client = client.IdentityAPIClient(
                url=cls.url,
                serialize_format=cls.test_config.serialize_format,
                deserialize_format=cls.test_config.deserialize_format)
            cls.service_admin_client.default_headers[const.X_AUTH_TOKEN] = (
                cls.identity_config.service_admin_auth_token)

    def setUp(self):
        super(TestRoleAssignmentFeature, self).setUp()
        self.user_ids = []
        self.role_ids = []
        self.tenant_ids = []
        self.create_role_respdata = {}

    def is_role_for_user(self, role_assign_type, user_id):
        resp = self.service_admin_client.list_roles_for_user(user_id)
        create_role_resp = self.create_role_respdata[role_assign_type]
        list_role_ids = [k[const.ID] for k in resp.json()[const.ROLES]]
        return create_role_resp[const.ROLE][const.ID] in list_role_ids

    def is_role_for_user_on_tenant(self, role_assign_type, user_id, tenant_id):
        resp = self.service_admin_client.list_roles_for_user_on_tenant(
            tenant_id, user_id)
        create_role_resp = self.create_role_respdata[role_assign_type]
        list_role_ids = [k[const.ID] for k in resp.json()[const.ROLES]]
        return create_role_resp[const.ROLE][const.ID] in list_role_ids

    def create_role_with_assignment(self, assignment):
        """
        create role with RAX-AUTH:assignment
        """
        new_role_name = "New{0}Role{1}".format(
            assignment, self.generate_random_string(
                pattern=const.UPPER_CASE_LETTERS))
        request_object = factory.get_add_role_request_object(
            role_name=new_role_name,
            assignment=assignment,
            administrator_role=const.USER_MANAGE_ROLE_NAME)
        resp = self.identity_admin_client.add_role(
                   request_object=request_object)
        self.assertEqual(resp.status_code, 201)
        self.assertSchema(
                response=resp, json_schema=roles_json.add_role)
        self.role_ids.append(resp.json()[const.ROLE][const.ID])
        self.create_role_respdata[assignment or 'DEFAULT'] = resp.json()
        return resp.json()[const.ROLE][const.ID]

    def create_user(self):
        domain_id = tenant_id = self.generate_random_string(
            pattern=const.NUMERIC_DOMAIN_ID_PATTERN)
        req_obj = factory.get_add_user_request_object_pull(
            domain_id=domain_id)
        resp = self.identity_admin_client.add_user(request_object=req_obj)
        self.assertEqual(resp.status_code, 201)
        user_id = resp.json()[const.USER][const.ID]
        self.user_ids.append(user_id)
        self.tenant_ids.append(tenant_id)
        return user_id, tenant_id

    @attr('skip_at_gate')
    def test_fail_to_create_role_with_invalid_assingment_type(self):
        assignment = self.generate_random_string(
            pattern=const.UPPER_CASE_LETTERS)
        new_role_name = "New{0}Role{1}".format(
            assignment, self.generate_random_string(
                pattern=const.UPPER_CASE_LETTERS))
        request_object = factory.get_add_role_request_object(
            role_name=new_role_name,
            assignment=assignment,
            administrator_role=const.USER_MANAGE_ROLE_NAME)
        resp = self.identity_admin_client.add_role(
                   request_object=request_object)
        self.assertEqual(resp.status_code, 400)

    def test_fail_adding_role_with_assignment_type_tenant_to_user(self):
        user_id, tenant_id = self.create_user()
        assign_tenant_type_role_id = self.create_role_with_assignment(
            assignment=const.ROLE_ASSIGNMENT_TYPE_TENANT)
        resp = self.service_admin_client.add_role_to_user(
                  role_id=assign_tenant_type_role_id,
                  user_id=user_id)
        self.assertEqual(resp.status_code, 403)
        self.assertFalse(
            self.is_role_for_user_on_tenant(
                const.ROLE_ASSIGNMENT_TYPE_TENANT, user_id, tenant_id),
            msg=("role w/ TENANT assignment type attempted give to user should"
                 "not appear in response to list roles on specific tenant"))
        self.assertFalse(
            self.is_role_for_user(const.ROLE_ASSIGNMENT_TYPE_TENANT, user_id),
            msg=("role w/ TENANT assignment type attempted to be given to user"
                 "should not appear for listRoles on user"))

    def test_fail_adding_role_assignment_type_global_to_user_for_tenant(self):
        user_id, tenant_id = self.create_user()
        assign_global_type_role_id = self.create_role_with_assignment(
            assignment=const.ROLE_ASSIGNMENT_TYPE_GLOBAL)
        resp = self.service_admin_client.add_role_to_user_for_tenant(
                   tenant_id=tenant_id,
                   role_id=assign_global_type_role_id,
                   user_id=user_id)
        self.assertEqual(resp.status_code, 403)
        self.assertFalse(
            self.is_role_for_user_on_tenant(
                const.ROLE_ASSIGNMENT_TYPE_GLOBAL, user_id, tenant_id),
            msg=("role w/ GLOBAL assignment type attempted give to user for"
                 "tenant should not appear in list roles for specific tenant"))
        self.assertFalse(
            self.is_role_for_user(const.ROLE_ASSIGNMENT_TYPE_GLOBAL, user_id),
            msg=("role w/ GLOBAL assignment type attempted to be given to user"
                 "should not appear in listRoles on user"))

    def test_add_role_with_assignment_type_global_to_user(self):
        user_id, tenant_id = self.create_user()
        assign_global_type_role_id = self.create_role_with_assignment(
            assignment=const.ROLE_ASSIGNMENT_TYPE_GLOBAL)
        resp = self.service_admin_client.add_role_to_user(
                  role_id=assign_global_type_role_id,
                  user_id=user_id)
        self.assertEqual(resp.status_code, 200)
        self.assertFalse(
            self.is_role_for_user_on_tenant(
                const.ROLE_ASSIGNMENT_TYPE_GLOBAL, user_id, tenant_id),
            msg=("role w/ GLOBAL assignment type given to user should not be"
                 " specific to tenant"))
        self.assertTrue(
            self.is_role_for_user(const.ROLE_ASSIGNMENT_TYPE_GLOBAL, user_id),
            msg=("role w/ GLOBAL assignment type given to user should appear"
                 "for listRoles on user"))

    def test_add_role_with_assignment_type_tenant_to_user_for_tenant(self):
        user_id, tenant_id = self.create_user()
        assign_tenant_type_role_id = self.create_role_with_assignment(
            assignment=const.ROLE_ASSIGNMENT_TYPE_TENANT)
        resp = self.service_admin_client.add_role_to_user_for_tenant(
                   tenant_id=tenant_id,
                   role_id=assign_tenant_type_role_id,
                   user_id=user_id)
        self.assertEqual(resp.status_code, 200)
        self.assertTrue(
            self.is_role_for_user_on_tenant(
                const.ROLE_ASSIGNMENT_TYPE_TENANT, user_id, tenant_id),
            msg=("role w/ TENANT assignment type, given to user for tenant,"
                 " should be on specific tenant"))
        self.assertFalse(
            self.is_role_for_user(const.ROLE_ASSIGNMENT_TYPE_TENANT, user_id),
            msg=("role w/ TENANT assignment type, given to user for tenant,"
                 " should not be in listRoles on user"))

    def test_add_role_with_assignment_type_both_to_user(self):
        user_id, tenant_id = self.create_user()
        assign_both_type_role_id = self.create_role_with_assignment(
            assignment='BOTH')
        resp = self.service_admin_client.add_role_to_user(
                  role_id=assign_both_type_role_id,
                  user_id=user_id)
        self.assertEqual(resp.status_code, 200)
        self.assertFalse(
            self.is_role_for_user_on_tenant('BOTH', user_id, tenant_id),
            msg=("role w/ BOTH assignment type given to user should not be"
                 " specific to tenant"))
        self.assertTrue(
            self.is_role_for_user('BOTH', user_id),
            msg=("role w/ BOTH assignment type given to user should appear for"
                 " listRoles on user"))

    def test_add_role_with_assignment_type_both_to_user_for_tenant(self):
        user_id, tenant_id = self.create_user()
        assign_both_type_role_id = self.create_role_with_assignment(
            assignment='BOTH')
        resp = self.service_admin_client.add_role_to_user_for_tenant(
                   tenant_id=tenant_id,
                   role_id=assign_both_type_role_id,
                   user_id=user_id)
        self.assertEqual(resp.status_code, 200)
        self.assertTrue(
            self.is_role_for_user_on_tenant('BOTH', user_id, tenant_id),
            msg=("role w/ BOTH assignment type, given to user for tenant,"
                 " should be on specific tenant"))
        self.assertFalse(
            self.is_role_for_user('BOTH', user_id),
            msg=("role w/ BOTH assignment type, given to user for tenant,"
                 " should not be in listRoles on user"))

    def test_add_role_with_assignment_type_default_to_user(self):
        user_id, tenant_id = self.create_user()
        assign_default_type_role_id = self.create_role_with_assignment(
            assignment='')
        resp = self.service_admin_client.add_role_to_user(
                  role_id=assign_default_type_role_id,
                  user_id=user_id)
        self.assertEqual(resp.status_code, 200)
        self.assertFalse(
            self.is_role_for_user_on_tenant('DEFAULT', user_id, tenant_id),
            msg=("role w/ DEFAULT assignment type given to user should not be"
                 " specific to tenant"))
        self.assertTrue(
            self.is_role_for_user('DEFAULT', user_id),
            msg=("role w/ DEFAULT assignment type given to user should appear"
                 "for listRoles on user"))

    def test_add_role_with_assignment_type_default_to_user_for_tenant(self):
        user_id, tenant_id = self.create_user()
        assign_default_type_role_id = self.create_role_with_assignment(
            assignment='')
        resp = self.service_admin_client.add_role_to_user_for_tenant(
                   tenant_id=tenant_id,
                   role_id=assign_default_type_role_id,
                   user_id=user_id)
        self.assertEqual(resp.status_code, 200)
        self.assertTrue(
            self.is_role_for_user_on_tenant('DEFAULT', user_id, tenant_id),
            msg=("role w/ DEFAULT assignment type, given to user for tenant,"
                 " should be on specific tenant"))
        self.assertFalse(
            self.is_role_for_user('DEFAULT', user_id),
            msg=("role w/ DEFAULT assignment type, given to user for tenant,"
                 " should not be in listRoles on user"))

    def tearDown(self):
        for user_id in self.user_ids:
            self.identity_admin_client.delete_user(user_id=user_id)
        for role_id in self.role_ids:
            self.identity_admin_client.delete_role(role_id=role_id)
        for tenant_id in self.tenant_ids:
            self.identity_admin_client.delete_tenant(tenant_id=tenant_id)
        super(TestRoleAssignmentFeature, self).tearDown()

    @classmethod
    def tearDownClass(cls):
        super(TestRoleAssignmentFeature, cls).tearDownClass()
