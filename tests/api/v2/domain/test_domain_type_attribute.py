# -*- coding: utf-8 -*
import unittest

from qe_coverage.opencafe_decorators import tags, unless_coverage

from tests.api.v2 import base
from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


class TestDomainTypeAttribute(base.TestBaseV2):
    """
    Test Domain Type Attribute
    NOTE:Skipping all the tests in this file until we can enable the
    feature.enable.use.role.for.domain.management=true or have seperate
    johny config.
    """
    @classmethod
    @unless_coverage
    def setUpClass(cls):
        super(TestDomainTypeAttribute, cls).setUpClass()
        req_obj = requests.Domain(
            domain_name=cls.generate_random_string(const.DOMAIN_PATTERN),
            description=cls.generate_random_string(const.DOMAIN_PATTERN),
            enabled=True,
            rcn=cls.generate_random_string(const.RCN_PATTERN),
            domain_type='PUBLIC_CLOUD_US')
        cls.role_id = cls.get_role_id_by_name(
            role_name=const.IDENTITY_RS_DOMAIN_ADMIN_ROLE_NAME)
        user_id = cls.identity_admin_client.default_headers[const.X_USER_ID]
        cls.service_admin_client.add_role_to_user(
            user_id=user_id, role_id=cls.role_id)
        resp = cls.identity_admin_client.add_domain(req_obj)
        assert resp.status_code == 201
        assert resp.json()[const.RAX_AUTH_DOMAIN][const.TYPE] \
            == 'PUBLIC_CLOUD_US'
        cls.domain_id = resp.json()[const.RAX_AUTH_DOMAIN][const.ID]

    @unless_coverage
    def setUp(self):
        super(TestDomainTypeAttribute, self).setUp()
        self.user_ids = []

    @classmethod
    def get_role_id_by_name(cls, role_name):
        """return a role id"""
        role_id = None
        option = {'roleName': role_name}
        resp = cls.service_admin_client.list_roles(option=option)
        assert resp.status_code == 200
        if resp.json()[const.ROLES]:
            role_id = resp.json()[const.ROLES][0][const.ID]
        return role_id

    @unittest.skip("skipping")
    @tags('positive', 'p1', 'regression')
    def test_update_domain(self):
        domain_req = requests.Domain(domain_type='PUBLIC_CLOUD_US')
        resp = self.identity_admin_client.update_domain(
                    domain_id=self.domain_id, request_object=domain_req)
        self.assertEqual(
            resp.json()[const.RAX_AUTH_DOMAIN][const.TYPE], 'PUBLIC_CLOUD_US')

    @unittest.skip("skipping")
    @tags('positive', 'p1', 'regression')
    def test_existing_domain_type_cannot_be_updated(self):

        # cannot update existing domain type
        domain_req = requests.Domain(domain_type='PUBLIC_CLOUD_UK')
        resp = self.identity_admin_client.update_domain(
                    domain_id=self.domain_id, request_object=domain_req)
        self.assertEqual(
            resp.json()[const.BAD_REQUEST][const.MESSAGE],
            "Error code: 'GEN-000'; Domain '{0}' already has type"
            " 'PUBLIC_CLOUD_US' and cannot be updated.".format(self.domain_id))

    @unittest.skip("skipping")
    @tags('positive', 'p1', 'regression')
    def test_get_domain(self):
        resp = self.identity_admin_client.get_domain(domain_id=self.domain_id)
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(
            resp.json()[const.RAX_AUTH_DOMAIN][const.ID], self.domain_id)
        self.assertEqual(
            resp.json()[const.RAX_AUTH_DOMAIN][const.TYPE], 'PUBLIC_CLOUD_US')

    @unittest.skip("skipping")
    @tags('positive', 'p1', 'regression')
    def test_list_domains(self):
        resp = self.identity_admin_client.list_domains()
        self.assertEqual(resp.status_code, 200)

        for domain in resp.json()[const.RAX_AUTH_DOMAINS]:
            if domain[const.ID] == self.domain_id:
                self.assertEqual(domain[const.TYPE], 'PUBLIC_CLOUD_US')

    @unittest.skip("skipping")
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
                self.assertEqual(domain[const.TYPE], 'PUBLIC_CLOUD_US')

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
        resp = cls.service_admin_client.delete_role_from_user(
                    role_id=cls.role_id,
                    user_id=cls.identity_admin_client.default_headers[
                        const.X_USER_ID])
        assert resp.status_code == 204, (
                'Role with ID {0} failed to delete'.format(
                    cls.role_id))
        super(TestDomainTypeAttribute, cls).tearDownClass()
