from qe_coverage.opencafe_decorators import tags, unless_coverage

from tests.api.v2 import base
from tests.api.v2.schema import unboundid as unboundid_json
from tests.api.v2.schema import groups as groups_json
from tests.api.v2.schema import roles as roles_json
from tests.api.v2.models import factory

from tests.package.johny import constants as const
from tests.package.johny.v2 import client


class TestUserPrefixes(base.TestBaseV2):
    """
    Verifying user.attr.prefixes
    """
    @classmethod
    @unless_coverage
    def setUpClass(cls):
        """Class level set up for the tests
        """
        super(TestUserPrefixes, cls).setUpClass()
        if cls.test_config.run_service_admin_tests:
            cls.service_admin_client = client.IdentityAPIClient(
                url=cls.url,
                serialize_format=cls.test_config.serialize_format,
                deserialize_format=cls.test_config.deserialize_format)
            cls.service_admin_client.default_headers[const.X_AUTH_TOKEN] = (
                cls.identity_config.service_admin_auth_token)

    @unless_coverage
    def setUp(self):
        super(TestUserPrefixes, self).setUp()
        self.user_ids = []
        self.group_ids = []
        self.role_ids = []

    @tags('positive', 'p1', 'regression')
    def test_verify_absence_of_include_user_attr_prefixes(self):
        """
        Verify feature.include.user.attr.prefixes is not in unboundid config
        """
        if not self.test_config.run_service_admin_tests:
            self.skipTest('Skipping Service Admin Tests per config value')

        resp = self.devops_client.get_devops_properties()
        self.assertEqual(resp.status_code, 200)
        self.assertSchema(response=resp,
                          json_schema=unboundid_json.config_list)
        self.assertFalse('feature.include.user.attr.prefixes' in resp.text,
                         msg=('feature.include.user.attr.prefixes is suppose'
                              ' to have been removed'))

    @tags('positive', 'p1', 'regression')
    def test_create_user_has_correct_prefixes(self):
        """
        Make sure createUser API call returns a JSON structure
        that has the RAX-KSGRP prefix on groups and has
        the RAX-KSQA on secreteQA
        """
        # create group
        request_object = factory.get_add_group_request_object()
        resp = self.identity_admin_client.add_group(
                   request_object=request_object)
        self.assertEqual(resp.status_code, 201)
        self.assertSchema(
                response=resp, json_schema=groups_json.add_group)
        new_group_name = resp.json()[const.NS_GROUP][const.NAME]
        self.group_ids.append(resp.json()[const.NS_GROUP][const.ID])

        # create role
        new_role_name = "NewUpgradeRole"+self.generate_random_string(
                pattern=const.UPPER_CASE_LETTERS)
        request_object = factory.get_add_role_request_object(
            role_name=new_role_name,
            administrator_role="identity:user-manage")
        resp = self.identity_admin_client.add_role(
                   request_object=request_object)
        self.assertEqual(resp.status_code, 201)
        self.assertSchema(
                response=resp, json_schema=roles_json.add_role)
        self.role_ids.append(resp.json()[const.ROLE][const.ID])

        # create User
        request_object = factory.get_add_user_request_object_pull(
            groups=[{const.NAME: new_group_name}],
            roles=[{const.NAME: new_role_name}])
        resp = self.identity_admin_client.add_user(
                   request_object=request_object)
        self.assertEqual(resp.status_code, 201)
        self.user_ids.append(resp.json()[const.USER][const.ID])

        self.assertIn(const.NS_GROUPS, resp.json()[const.USER],
                      msg="{0} not in response.".format(const.NS_GROUPS))
        self.assertIn(const.NS_SECRETQA, resp.json()[const.USER],
                      msg="{0} not in response.".format(const.NS_SECRETQA))

    @unless_coverage
    def tearDown(self):
        for user_id in self.user_ids:
            self.identity_admin_client.delete_user(user_id=user_id)
        for role_id in self.role_ids:
            self.identity_admin_client.delete_role(role_id=role_id)
        for group_id in self.group_ids:
            self.identity_admin_client.delete_group(group_id=group_id)
        super(TestUserPrefixes, self).tearDown()

    @classmethod
    @unless_coverage
    def tearDownClass(cls):
        # @todo: Delete all users created in the setUpClass
        super(TestUserPrefixes, cls).tearDownClass()
