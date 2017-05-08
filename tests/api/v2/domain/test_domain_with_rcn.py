# -*- coding: utf-8 -*
import copy

from tests.api.v2 import base
from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests
from tests.api.v2.schema import domains as domains_json


class TestRCNDomain(base.TestBaseV2):
    """
    Tests Create, Update domains with RCN.
    """

    @classmethod
    def setUpClass(cls):
        super(TestRCNDomain, cls).setUpClass()
        cls.domain_with_rcn_schema = copy.deepcopy(domains_json.domain)
        cls.domain_with_rcn_schema[
            'properties'][const.RAX_AUTH_DOMAIN]['required'].append(
                const.RCN_LONG)

    def setUp(self):
        super(TestRCNDomain, self).setUp()
        self.domain_ids = []

    def create_domain_with_rcn(self):
        req_obj = requests.Domain(
            domain_name=self.generate_random_string(const.DOMAIN_PATTERN),
            domain_id=self.generate_random_string(const.ID_PATTERN),
            description=self.generate_random_string(const.DOMAIN_PATTERN),
            enabled=True,
            rcn=self.generate_random_string(const.RCN_PATTERN))
        resp = self.identity_admin_client.add_domain(req_obj)
        return resp

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

    def test_update_domain_with_rcn(self):
        resp = self.create_domain_with_rcn()
        self.assertEqual(resp.status_code, 201)
        self.assertSchema(
            response=resp, json_schema=self.domain_with_rcn_schema)

        domain_id = resp.json()[const.RAX_AUTH_DOMAIN][const.ID]
        self.domain_ids.append(domain_id)

        updated_rcn = self.generate_random_string(const.RCN_PATTERN)
        resp = self.identity_admin_client.update_domain(
            domain_id=domain_id,
            request_object=requests.Domain(rcn=updated_rcn))
        self.assertEqual(resp.status_code, 200)
        self.assertSchema(
            response=resp, json_schema=self.domain_with_rcn_schema)
        self.assertEqual(
            resp.json()[const.RAX_AUTH_DOMAIN][const.RCN_LONG], updated_rcn)

    def tearDown(self):
        super(TestRCNDomain, self).tearDown()
        for domain_id in self.domain_ids:
            self.identity_admin_client.delete_domain(domain_id=domain_id)
