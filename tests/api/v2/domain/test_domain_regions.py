import ast
import unittest

import ddt
from qe_coverage.opencafe_decorators import unless_coverage

from tests.api.v2 import base
from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


@ddt.ddt
class TestDomainRegions(base.TestBaseV2):

    ACCEPTABLE_ENDPOINT_REGIONS = {
        const.RACKSPACE_CLOUD_US: ["ORD", "IAD", "DFW", "HKG", "SYD"],
        const.RACKSPACE_CLOUD_UK: ["LON"],
    }
    FEATURE_FLAG = 'feature.enabled.use.domain.type.on.new.user.creation'
    PROPERTIES_FOR_REGION = {
        const.RACKSPACE_CLOUD_US: {
            'cloud': 'v1.default.cloud.endpoints.us',
            'files': 'v1.default.files.endpoints.us',
        },
        const.RACKSPACE_CLOUD_UK: {
            'cloud': 'v1.default.cloud.endpoints.uk',
            'files': 'v1.default.files.endpoints.uk',
        }
    }

    @classmethod
    @unless_coverage
    def setUpClass(cls):
        super().setUpClass()
        feature_enabled = cls.devops_client.get_feature_flag(cls.FEATURE_FLAG)
        if not feature_enabled:
            raise unittest.SkipTest("Skipping due to feature flag %s=%s"
                                    % (cls.FEATURE_FLAG, feature_enabled))

    @unless_coverage
    def setUp(self):
        super().setUp()
        self.domain_ids = []
        self.user_ids = []

    @base.base.log_tearDown_error
    @unless_coverage
    def tearDown(self):
        for user_id in self.user_ids:
            self.cleanupUser(user_id, self.identity_admin_client)
        for domain_id in self.domain_ids:
            self.cleanupDomain(domain_id, self.identity_admin_client)
        super().tearDown()

    @ddt.data(const.RACKSPACE_CLOUD_US, const.RACKSPACE_CLOUD_UK)
    def test_domain_type_based_endpoint_assignment(self, domain_type):
        # Create the domain
        domain_data = requests.Domain(
            domain_name=self.generate_random_string(
                const.NUMERIC_DOMAIN_ID_PATTERN
            ),
            description="A domain with type={}".format(domain_type),
            domain_type=domain_type,
        )
        resp = self.identity_admin_client.add_domain(domain_data)
        self.assertEqual(resp.status_code, 201)

        domain_id = resp.json()[const.RAX_AUTH_DOMAIN][const.ID]
        self.domain_ids.append(domain_id)

        # Create the user-admin
        #
        # Note: need roles to trigger "one call" logic, else no endpoints.
        #       And only works properly if domain_id parses to int?
        cloud_tenant_id = domain_id
        files_tenant_id = "MossoCloudFS_" + domain_id
        user_data = requests.UserAdd(
            user_name=self.generate_random_string(const.USER_ADMIN_PATTERN),
            domain_id=domain_id,
            roles=[
                {
                    "name": "object-store:default",
                    "tenantId": files_tenant_id,
                },
                {
                    "name": "compute:default",
                    "tenantId": cloud_tenant_id,
                },
            ],
        )
        resp = self.identity_admin_client.add_user(user_data)
        self.assertEqual(resp.status_code, 201)

        user_id = resp.json()[const.USER][const.ID]
        self.user_ids.append(user_id)

        # Get the user's service catalog
        auth_data = requests.AuthenticateWithPassword(
            user_name=resp.json()[const.USER][const.USERNAME],
            password=resp.json()[const.USER][const.OS_KSADM_PASSWORD],
        )
        resp = self.identity_admin_client.get_auth_token(auth_data)
        self.assertEqual(resp.status_code, 200)

        # CID-1864: Validate endpoints are in the domain's cloud region
        service_catalog = resp.json()[const.ACCESS][const.SERVICE_CATALOG]
        acceptable_regions = self.ACCEPTABLE_ENDPOINT_REGIONS[domain_type]
        for entry in service_catalog:
            for endpoint in entry[const.ENDPOINTS]:
                self.assertIn(
                    endpoint[const.REGION], acceptable_regions,
                    "User catalog has endpoint outside of region %s:\n%s" % (
                        domain_type, endpoint
                    )
                )

        # CID-1865: Check endpoint ids are assigned per server properties
        tenants = {'cloud': cloud_tenant_id, 'files': files_tenant_id}
        for tenant_type, tenant_id in tenants.items():
            # A server property specifies the default endpoint ids to assign
            server_prop = self.PROPERTIES_FOR_REGION[domain_type][tenant_type]
            raw_endpoint_ids = self.devops_client.get_prop_value(server_prop)

            # string "[123, 456]" -> list of ints
            endpoint_ids = ast.literal_eval(raw_endpoint_ids)

            resp = self.identity_admin_client.list_endpoints_for_tenant(
                tenant_id=tenant_id,
            )
            self.assertEqual(resp.status_code, 200)
            tenant_endpoints = resp.json()[const.ENDPOINTS]

            # Check that all v1 default endpoints were assigned to the tenant
            for endpoint_id in endpoint_ids:
                self.assertTrue(
                    any(e[const.ID] == endpoint_id for e in tenant_endpoints),
                    "Tenant %s in region %s is missing endpoint id=%s "
                    "(from %s=%s)" % (
                        tenant_id, domain_type, endpoint_id, server_prop,
                        raw_endpoint_ids
                    )
                )
