# -*- coding: utf-8 -*
from tests.api.v2 import base
from tests.api.v2.models import factory
from tests.package.johny import constants as const


class TestGetDomain(base.TestBaseV2):
    """
    Tests GetDomain call
    """

    @classmethod
    def setUpClass(cls):
        super(TestGetDomain, cls).setUpClass()

    def setUp(self):
        super(TestGetDomain, self).setUp()
        self.domain_ids = []

    def create_domain(self):
        domain_template_data = {
            const.TENANT: [],
            const.DOMAIN: {
                const.DEFAULT: False,
                const.ENABLED: True,
                const.USERS: False}}
        req_obj = factory.get_domain_request_object(domain_template_data)
        resp = self.identity_admin_client.add_domain(req_obj)
        self.assertEqual(resp.status_code, 201)
        domain_id = resp.json()[const.RAX_AUTH_DOMAIN][const.ID]
        self.domain_ids.append(domain_id)
        return domain_id

    def test_get_domain(self):
        domain_id = self.create_domain()
        resp = self.identity_admin_client.get_domain(domain_id=domain_id)
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(
            domain_id, resp.json()[const.RAX_AUTH_DOMAIN][const.ID])

    def tearDown(self):
        super(TestGetDomain, self).tearDown()
        for domain_id in self.domain_ids:
            self.identity_admin_client.delete_domain(domain_id=domain_id)
