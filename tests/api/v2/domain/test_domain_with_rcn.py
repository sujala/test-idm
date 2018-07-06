# -*- coding: utf-8 -*
import copy
from nose.plugins.attrib import attr
from qe_coverage.opencafe_decorators import tags, unless_coverage

from tests.api.utils import func_helper
from tests.api.v2 import base
from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests
from tests.api.v2.schema import domains as domains_json


class TestRCNDomain(base.TestBaseV2):
    """
    Tests Create, Update domains with RCN.
    """

    @classmethod
    @unless_coverage
    def setUpClass(cls):
        super(TestRCNDomain, cls).setUpClass()
        cls.domain_with_rcn_schema = copy.deepcopy(domains_json.domain)
        cls.domain_with_rcn_schema[
            'properties'][const.RAX_AUTH_DOMAIN]['required'].append(
                const.RCN_LONG)

    @unless_coverage
    def setUp(self):
        super(TestRCNDomain, self).setUp()
        self.domain_ids = []
        self.user_ids = []

    @attr(type='smoke_alpha')
    def create_domain_with_rcn(self):
        domain_id = func_helper.generate_randomized_domain_id(
            client=self.identity_admin_client)
        req_obj = requests.Domain(
            domain_name=self.generate_random_string(const.DOMAIN_PATTERN),
            domain_id=domain_id,
            description=self.generate_random_string(const.DOMAIN_PATTERN),
            enabled=True,
            rcn=self.generate_random_string(const.RCN_PATTERN))
        resp = self.identity_admin_client.add_domain(req_obj)
        return resp

    @tags('positive', 'p0', 'regression')
    def test_create_domain_with_rcn(self):
        resp = self.create_domain_with_rcn()
        self.assertEqual(resp.status_code, 201)
        self.assertSchema(
            response=resp, json_schema=self.domain_with_rcn_schema)

        domain_id = resp.json()[const.RAX_AUTH_DOMAIN][const.ID]
        self.domain_ids.append(domain_id)

        rcn = resp.json()[const.RAX_AUTH_DOMAIN][const.RCN_LONG]

        resp = self.identity_admin_client.get_domain(domain_id=domain_id)
        self.assertEqual(resp.status_code, 200)
        self.assertSchema(
            response=resp, json_schema=self.domain_with_rcn_schema)
        self.assertEqual(
            rcn, resp.json()[const.RAX_AUTH_DOMAIN][const.RCN_LONG])

    @tags('positive', 'p1', 'smoke')
    @attr(type='smoke_alpha')
    def test_update_domain_with_rcn(self):
        resp = self.create_domain_with_rcn()
        self.assertEqual(resp.status_code, 201)
        self.assertSchema(
            response=resp, json_schema=self.domain_with_rcn_schema)

        domain_id = resp.json()[const.RAX_AUTH_DOMAIN][const.ID]
        orig_rcn = resp.json()[const.RAX_AUTH_DOMAIN][const.RCN_LONG]
        self.domain_ids.append(domain_id)

        updated_rcn = self.generate_random_string(const.RCN_PATTERN)
        resp = self.identity_admin_client.update_domain(
            domain_id=domain_id,
            request_object=requests.Domain(rcn=updated_rcn))
        self.assertEqual(resp.status_code, 200)
        self.assertSchema(
            response=resp, json_schema=self.domain_with_rcn_schema)
        self.assertEqual(
            resp.json()[const.RAX_AUTH_DOMAIN][const.RCN_LONG], orig_rcn)

    def create_user_admin_with_rcn_role(self, domain_id):

        role_dict = {
            'name': const.RCN_ADMIN_ROLE_NAME
        }
        create_user_req = requests.UserAdd(
            user_name=self.generate_random_string(const.USER_NAME_PATTERN),
            domain_id=domain_id, roles=[role_dict]
        )
        add_user_resp = self.identity_admin_client.add_user(create_user_req)
        user_id = add_user_resp.json()[const.USER][const.ID]
        self.user_ids.append(user_id)
        self.assertEqual(add_user_resp.status_code, 201)
        return user_id

    def check_users_rcn_role(self, user_id, role_expected=True):

        list_resp = self.identity_admin_client.list_roles_for_user(
            user_id=user_id, apply_rcn_roles=True)
        role_names = [role[const.NAME] for role in list_resp.json()[
            const.ROLES]]
        if role_expected:
            self.assertIn(const.RCN_ADMIN_ROLE_NAME, role_names)
        else:
            self.assertNotIn(const.RCN_ADMIN_ROLE_NAME, role_names)

    @tags('positive', 'p1', 'regression')
    def test_move_domain_to_rcn(self):
        """
        Test to validate the 'move domain to an RCN' call
        """
        resp = self.create_domain_with_rcn()
        self.assertEqual(resp.status_code, 201)
        self.assertSchema(
            response=resp, json_schema=self.domain_with_rcn_schema)

        domain_id = resp.json()[const.RAX_AUTH_DOMAIN][const.ID]
        self.domain_ids.append(domain_id)

        user_id = self.create_user_admin_with_rcn_role(domain_id=domain_id)
        self.check_users_rcn_role(user_id=user_id)

        new_rcn = self.generate_random_string(const.RCN_PATTERN)
        resp = self.identity_admin_client.move_domain_to_rcn(
            domain_id=domain_id, rcn=new_rcn)
        self.assertEqual(resp.status_code, 403)

        # Adding the required role to make the call
        if self.test_config.run_service_admin_tests:
            rcn_switch_role_id = None
            try:
                option = {
                    const.PARAM_ROLE_NAME: const.RCN_SWITCH_ROLE_NAME
                }
                list_resp = self.service_admin_client.list_roles(option=option)
                self.assertEqual(list_resp.status_code, 200)
                rcn_switch_role_id = list_resp.json()[const.ROLES][0][const.ID]
                self.service_admin_client.add_role_to_user(
                    user_id=self.identity_admin_client.default_headers[
                        const.X_USER_ID], role_id=rcn_switch_role_id)
                resp = self.identity_admin_client.move_domain_to_rcn(
                    domain_id=domain_id, rcn=new_rcn)
                self.assertEqual(resp.status_code, 204)
                resp = self.identity_admin_client.get_domain(
                    domain_id=domain_id)
                self.assertEqual(resp.status_code, 200)
                self.assertEqual(
                    new_rcn, resp.json()[
                        const.RAX_AUTH_DOMAIN][const.RCN_LONG])
                self.check_users_rcn_role(user_id=user_id, role_expected=False)
            finally:
                # cleanup
                self.service_admin_client.delete_role_from_user(
                    role_id=rcn_switch_role_id,
                    user_id=self.identity_admin_client.default_headers[
                        const.X_USER_ID])

    @base.base.log_tearDown_error
    @unless_coverage
    def tearDown(self):
        super(TestRCNDomain, self).tearDown()
        for user_id in self.user_ids:
            resp = self.identity_admin_client.delete_user(user_id=user_id)
            self.assertEqual(
                resp.status_code, 204,
                msg='User with ID {0} failed to delete'.format(user_id))
        for domain_id in self.domain_ids:
            disable_domain_req = requests.Domain(enabled=False)
            self.identity_admin_client.update_domain(
                domain_id=domain_id, request_object=disable_domain_req)
            resp = self.identity_admin_client.delete_domain(
                domain_id=domain_id)
            self.assertEqual(
                resp.status_code, 204,
                msg='Domain with ID {0} failed to delete'.format(
                    domain_id))

    @classmethod
    @unless_coverage
    def tearDownClass(cls):
        super(TestRCNDomain, cls).tearDownClass()
