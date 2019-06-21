# -*- coding: utf-8 -*
from qe_coverage.opencafe_decorators import tags, unless_coverage
import unittest

from tests.api.v2 import base
from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


class TestDomainTypeAttribute(base.TestBaseV2):
    """
    Test Domain Type Attribute

    Note: Previously, these tests required
    `feature.enable.use.role.for.domain.management = true`. This property was
    set to `true` in 3.30.0 and then removed in 3.31.0.
    """
    FEATURE_FLAG = 'feature.enabled.use.domain.type.on.new.user.creation'

    @classmethod
    @unless_coverage
    def setUpClass(cls):
        super(TestDomainTypeAttribute, cls).setUpClass()
        cls.feature_enabled = cls.devops_client.get_feature_flag(
                                                 cls.FEATURE_FLAG)
        req_obj = requests.Domain(
            domain_name=cls.generate_random_string(const.DOMAIN_PATTERN),
            description=cls.generate_random_string(const.DOMAIN_PATTERN),
            enabled=True,
            rcn=cls.generate_random_string(const.RCN_PATTERN),
            domain_type=const.RACKSPACE_CLOUD_US,
        )

        resp = cls.identity_admin_client.add_domain(req_obj)
        assert resp.status_code == 201
        assert resp.json()[const.RAX_AUTH_DOMAIN][const.TYPE] \
            == const.RACKSPACE_CLOUD_US
        cls.domain_id = resp.json()[const.RAX_AUTH_DOMAIN][const.ID]

        # Create Domain with UK domain type
        req_obj = requests.Domain(
            domain_name=cls.generate_random_string(const.DOMAIN_PATTERN),
            description=cls.generate_random_string(const.DOMAIN_PATTERN),
            enabled=True,
            rcn=cls.generate_random_string(const.RCN_PATTERN),
            domain_type=const.RACKSPACE_CLOUD_UK,
        )
        resp = cls.identity_admin_client.add_domain(req_obj)
        assert resp.status_code == 201
        assert resp.json()[const.RAX_AUTH_DOMAIN][const.TYPE] \
            == const.RACKSPACE_CLOUD_UK
        cls.domain_id_UK = resp.json()[const.RAX_AUTH_DOMAIN][const.ID]

    @unless_coverage
    def setUp(self):
        super(TestDomainTypeAttribute, self).setUp()
        self.user_ids = []

    @classmethod
    def get_role_id_by_name(cls, role_name):
        return super().get_role_id_by_name(cls.service_admin_client, role_name)

    @tags('positive', 'p0', 'regression')
    def test_add_user_for_region_validation(self):
        if not self.feature_enabled:
            raise unittest.SkipTest("Skipping due to feature flag %s=%s"
                                    % (self.FEATURE_FLAG,
                                       self.feature_enabled))
        """
        CID-1876 : Use domain type for region validation in v2.0 add user.
        """
        # when domain_type is RACKSPACE_CLOUD_US
        user_name = self.generate_random_string()
        password = self.generate_random_string(pattern=const.PASSWORD_PATTERN)
        input_data = {
            'domain_id': self.domain_id}
        request_object = requests.UserAdd(user_name=user_name,
                                          password=password, **input_data)
        resp = self.identity_admin_client.add_user(request_object)
        self.assertEqual(resp.status_code, 201)

        # Verify region is ORD
        self.assertEqual(
            resp.json()[const.USER][const.RAX_AUTH_DEFAULT_REGION],
            'ORD')
        user_id = resp.json()[const.USER][const.ID]
        self.user_ids.append(user_id)

        # when domain_type is RACKSPACE_CLOUD_UK
        user_name = self.generate_random_string()
        password = self.generate_random_string(pattern=const.PASSWORD_PATTERN)
        input_data = {
            'domain_id': self.domain_id_UK}
        request_object = requests.UserAdd(user_name=user_name,
                                          password=password, **input_data)
        resp = self.identity_admin_client.add_user(request_object)
        self.assertEqual(resp.status_code, 201)

        # Verify region is LON
        self.assertEqual(
            resp.json()[const.USER][const.RAX_AUTH_DEFAULT_REGION],
            'LON')
        user_id = resp.json()[const.USER][const.ID]
        self.user_ids.append(user_id)

    @tags('positive', 'p0', 'regression')
    def test_default_region_is_appropriate_for_domain_type(self):
        """
        CID-1876 : Create a user with a default region of LON when the user's
        domain type is RACKSPACE_CLOUD_US
        """
        if not self.feature_enabled:
            raise unittest.SkipTest("Skipping due to feature flag %s=%s"
                                    % (self.FEATURE_FLAG,
                                       self.feature_enabled))
        user_name = self.generate_random_string()
        password = self.generate_random_string(pattern=const.PASSWORD_PATTERN)
        input_data = {
            'domain_id': self.domain_id}
        request_object = requests.UserAdd(user_name=user_name,
                                          default_region="LON",
                                          password=password, **input_data)
        resp = self.identity_admin_client.add_user(request_object)
        self.assertEqual(resp.status_code, 400)
        self.assertEqual(
            resp.json()[const.BAD_REQUEST][const.MESSAGE],
            "Invalid defaultRegion value, accepted values are: ORD, DFW.")
        """
        Create a user with a default region of ORD when the user's
        domain type is RACKSPACE_CLOUD_UK
        """
        user_name = self.generate_random_string()
        password = self.generate_random_string(pattern=const.PASSWORD_PATTERN)
        input_data = {
            'domain_id': self.domain_id_UK}
        request_object = requests.UserAdd(user_name=user_name,
                                          default_region="ORD",
                                          password=password, **input_data)
        resp = self.identity_admin_client.add_user(request_object)
        self.assertEqual(resp.status_code, 400)
        self.assertEqual(
            resp.json()[const.BAD_REQUEST][const.MESSAGE],
            "Invalid defaultRegion value, accepted values are: LON.")

    @tags('positive', 'p1', 'regression')
    def test_update_domain(self):
        domain_req = requests.Domain(domain_type=const.RACKSPACE_CLOUD_US)
        resp = self.identity_admin_client.update_domain(
                    domain_id=self.domain_id, request_object=domain_req)
        self.assertEqual(
            resp.json()[const.RAX_AUTH_DOMAIN][const.TYPE],
            const.RACKSPACE_CLOUD_US)

    @tags('positive', 'p1', 'regression')
    def test_existing_domain_type_cannot_be_updated(self):

        # cannot update existing domain type
        domain_req = requests.Domain(domain_type=const.RACKSPACE_CLOUD_UK)
        resp = self.identity_admin_client.update_domain(
                    domain_id=self.domain_id, request_object=domain_req)
        self.assertEqual(
            resp.json()[const.BAD_REQUEST][const.MESSAGE],
            "Error code: 'GEN-000'; Domain '{0}' already has type"
            " '{1}' and cannot be updated."
            .format(self.domain_id, const.RACKSPACE_CLOUD_US))

    @tags('positive', 'p1', 'regression')
    def test_get_domain(self):
        resp = self.identity_admin_client.get_domain(domain_id=self.domain_id)
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(
            resp.json()[const.RAX_AUTH_DOMAIN][const.ID], self.domain_id)
        self.assertEqual(
            resp.json()[const.RAX_AUTH_DOMAIN][const.TYPE],
            const.RACKSPACE_CLOUD_US)

    @tags('positive', 'p1', 'regression')
    def test_list_domains(self):
        resp = self.identity_admin_client.list_domains()
        self.assertEqual(resp.status_code, 200)

        for domain in resp.json()[const.RAX_AUTH_DOMAINS]:
            if domain[const.ID] == self.domain_id:
                self.assertEqual(domain[const.TYPE], const.RACKSPACE_CLOUD_US)

    @tags('positive', 'p1', 'regression')
    def test_accessible_domains_for_user(self):
        user_name = self.generate_random_string()
        # Add a user
        request_object = requests.UserAdd(
            user_name=user_name, domain_id=self.domain_id)
        resp = self.identity_admin_client.add_user(request_object)
        self.assertEqual(resp.status_code, 201)
        user_id = resp.json()[const.USER][const.ID]
        self.user_ids.append(user_id)
        # List Domains for a user
        resp = self.identity_admin_client.list_domains_for_user(
            user_id=user_id)
        self.assertEqual(resp.status_code, 200)
        # Verify if domain type attribute is present
        for domain in resp.json()[const.RAX_AUTH_DOMAINS]:
            if domain[const.ID] == self.domain_id:
                self.assertEqual(domain[const.TYPE], const.RACKSPACE_CLOUD_US)

    @base.base.log_tearDown_error
    @unless_coverage
    def tearDown(self):
        super(TestDomainTypeAttribute, self).tearDown()
        for user_id in self.user_ids:
            resp = self.identity_admin_client.delete_user(user_id=user_id)
            assert resp.status_code in [204, 404], (
                'User with ID {0} failed to delete'.format(user_id))

    @classmethod
    @unless_coverage
    @base.base.log_tearDown_error
    def tearDownClass(cls):
        disable_domain_req = requests.Domain(enabled=False)
        cls.identity_admin_client.update_domain(
            domain_id=cls.domain_id, request_object=disable_domain_req)
        resp = cls.identity_admin_client.delete_domain(
            domain_id=cls.domain_id)
        assert resp.status_code == 204, (
            'Domain with ID {0} failed to delete'.format(
             cls.domain_id))
        cls.identity_admin_client.update_domain(
            domain_id=cls.domain_id_UK, request_object=disable_domain_req)
        resp = cls.identity_admin_client.delete_domain(
            domain_id=cls.domain_id_UK)
        assert resp.status_code == 204, (
            'Domain with ID {0} failed to delete'.format(
                cls.domain_id_UK))
        super(TestDomainTypeAttribute, cls).tearDownClass()
