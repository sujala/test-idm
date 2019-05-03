# -*- coding: utf-8 -*
import pytest
from nose.plugins.skip import SkipTest

from qe_coverage.opencafe_decorators import tags, unless_coverage

from tests.api.utils import func_helper
from tests.api.utils import saml_helper
from tests.api.utils.create_cert import create_self_signed_cert

from tests.api.v2 import base
from tests.api.v2.models import factory
from tests.api.v2.models import responses

from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


class TestSearchUserAdminByTenantOrDomain(base.TestBaseV2):

    @classmethod
    @unless_coverage
    def setUpClass(cls):
        """Class level set up for the tests
        Create users needed for the tests and generate clients for those
        users.
        """
        super(TestSearchUserAdminByTenantOrDomain, cls).setUpClass()
        cls.unverified_user_ids = []
        cls.sub_user_ids = []
        cls.fed_user_ids = []
        cls.rcn = cls.test_config.unverified_user_rcn
        cls.non_existing_id = '!@#$%^&Z*9)'

        # Add Domain1 w/ RCN
        cls.domain_id_1 = func_helper.generate_randomized_domain_id(
            client=cls.identity_admin_client)
        dom_req = requests.Domain(
            domain_name=cls.domain_id_1,
            domain_id=cls.domain_id_1, rcn=cls.rcn)
        add_dom_resp = cls.identity_admin_client.add_domain(dom_req)
        assert add_dom_resp.status_code == 201, (
            'domain was not created successfully')

        # user admin1 email
        cls.email_user_admin_1 = cls.generate_random_string(
            pattern=const.EMAIL_PATTERN)

        cls.user_admin_client_1 = cls.generate_client(
            parent_client=cls.identity_admin_client,
            additional_input_data={'domain_id': cls.domain_id_1,
                                   'email': cls.email_user_admin_1})
        resp = cls.identity_admin_client.get_user(
            cls.user_admin_client_1.default_headers[const.X_USER_ID])
        # admin1 username
        cls.username_ua_1 = resp.json()[const.USER][const.USERNAME]

        # create unverified user in domain1
        cls.unverified_user_email = cls.generate_random_string(
            pattern=const.UNVERIFIED_EMAIL_PATTERN)
        create_unverified_user_req = requests.UnverifiedUser(
            email=cls.unverified_user_email, domain_id=cls.domain_id_1)
        create_unverified_resp = \
            cls.user_admin_client_1.create_unverified_user(
                request_object=create_unverified_user_req)
        cls.unverified_user_ids.append(
            create_unverified_resp.json()[const.USER][const.ID])

        # create a cert
        (cls.pem_encoded_cert, cls.cert_path, _, cls.key_path,
         cls.f_print) = create_self_signed_cert()

        # Add IDP with domain belonging to the user
        cls.idp_request_object = factory.get_add_idp_request_object(
            public_certificates=[cls.pem_encoded_cert],
            approved_domain_ids=[cls.domain_id_1])
        cls.identity_admin_client.create_idp(cls.idp_request_object)

        cls.subject = cls.generate_random_string(
            pattern=const.FED_USER_PATTERN)
        cls.fed_user_email = const.EMAIL_RANDOM
        cls.assertion = saml_helper.create_saml_assertion_v2(
            domain=cls.domain_id_1,
            username=cls.subject,
            issuer=cls.idp_request_object.issuer,
            email=cls.fed_user_email,
            private_key_path=cls.key_path,
            public_key_path=cls.cert_path, response_flavor='v2DomainOrigin')

        saml_resp = cls.identity_admin_client.auth_with_saml(
            saml=cls.assertion, content_type=const.XML,
            base64_url_encode=False,
            new_url=False)
        assert saml_resp.status_code == 200
        cls.fed_user_id = saml_resp.json()[const.ACCESS][const.USER][const.ID]
        cls.fed_user_ids.append(cls.fed_user_id)

        # Domain2 #
        cls.domain_id_2 = func_helper.generate_randomized_domain_id(
            client=cls.identity_admin_client)

        # user admin2 email
        cls.email_user_admin_2 = cls.generate_random_string(
            pattern=const.EMAIL_PATTERN)

        cls.user_admin_client_2 = cls.generate_client(
            parent_client=cls.identity_admin_client,
            additional_input_data={'domain_id': cls.domain_id_2,
                                   'email': cls.email_user_admin_2})
        cls.email_sub_user_2 = cls.generate_random_string(
            pattern=const.EMAIL_PATTERN)

        # Add a Tenant to Domain2
        tenant_object = factory.get_add_tenant_object(
                                domain_id=cls.domain_id_2)
        resp = cls.identity_admin_client.add_tenant(tenant=tenant_object)
        tenant = responses.Tenant(resp.json())
        cls.tenant_id = tenant.id

        # create sub user in Domain2
        cls.sub_user_name_2 = "sub_" + cls.generate_random_string()
        request_input = requests.UserAdd(user_name=cls.sub_user_name_2,
                                         email=cls.email_sub_user_2)
        resp = cls.user_admin_client_2.add_user(
                request_object=request_input)
        cls.sub_user_ids.append(resp.json()[const.USER][const.ID])

    @unless_coverage
    def setUp(self):
        super(TestSearchUserAdminByTenantOrDomain, self).setUp()

    @tags('positive', 'p0', 'smoke')
    @pytest.mark.smoke_alpha
    def test_list_user_admin_for_specified_domain(self):
        """List user admin by domain_id and
           admin_only param true
        """
        resp = self.identity_admin_client.list_users(
            option={'domain_id': self.domain_id_1,
                    'admin_only': True})
        self.assertEqual(resp.status_code, 200)

        self.assertEqual(len(resp.json()[const.USERS]), 1)
        for user in resp.json()[const.USERS]:
            self.assertEqual(user[const.RAX_AUTH_DOMAIN_ID], self.domain_id_1)
            self.assertEqual(user[const.USERNAME], self.username_ua_1)
            self.assertEqual(user[const.EMAIL], self.email_user_admin_1)

    @tags('negative', 'p0', 'regression')
    @pytest.mark.regression
    def test_list_users_by_user_admin_with_param_domainid(self):
        """List user admin by domain_id and
           admin_only param true
        """
        resp = self.user_admin_client_1.list_users(
            option={'domain_id': self.domain_id_1,
                    'admin_only': True})

        self.assertEqual(resp.status_code, 403)

    @tags('positive', 'p0', 'smoke')
    @pytest.mark.smoke_alpha
    def test_list_users_by_domain_id_and_name(self):
        """List verified users by query param domain_id,fed user name
            and admin_only=false
        """
        resp = self.identity_admin_client.list_users(
            option={'domain_id': self.domain_id_1,
                    'admin_only': False,
                    'name': self.subject})
        self.assertEqual(resp.status_code, 200)

        self.assertEqual(len(resp.json()[const.USERS]), 1)
        # fed user returned from domain1
        for user in resp.json()[const.USERS]:
            self.assertEqual(user[const.RAX_AUTH_DOMAIN_ID],
                             self.domain_id_1)
            self.assertEqual(user[const.USERNAME], self.subject)

    @tags('positive', 'p0', 'smoke')
    @pytest.mark.smoke_alpha
    def test_list_user_admin_of_specified_domain_by_name_email(self):
        # search by tenant_id, admin_only=true and useradmin email, name
        resp = self.identity_admin_client.list_users(
            option={'domain_id': self.domain_id_1,
                    'admin_only': True,
                    'email': self.email_user_admin_1,
                    'name': self.username_ua_1
                    })
        self.assertEqual(resp.status_code, 200)

        self.assertEqual(len(resp.json()[const.USERS]), 1)
        for user in resp.json()[const.USERS]:
            self.assertEqual(user[const.RAX_AUTH_DOMAIN_ID],
                             self.domain_id_1)
            self.assertEqual(user[const.USERNAME], self.username_ua_1)
            self.assertEqual(user[const.EMAIL], self.email_user_admin_1)

        # search by domain_id, admin_only=true and
        # other domain useradmin email
        resp = self.identity_admin_client.list_users(
            option={'domain_id': self.domain_id_1,
                    'admin_only': True,
                    'email': self.email_user_admin_2
                    })
        self.assertEqual(resp.status_code, 200)

        self.assertEqual(len(resp.json()[const.USERS]), 0)

    @tags('positive', 'p0', 'smoke')
    @pytest.mark.smoke_alpha
    def test_list_unverified_users_by_specific_domain_and_email(self):
        """List unverified users with query parameter domain_id, user_type
           and email by identity admin user
        """
        resp = self.identity_admin_client.list_users(
            option={'domain_id': self.domain_id_1,
                    'user_type': const.UNVERIFIED,
                    'email': self.unverified_user_email})
        self.assertEqual(resp.status_code, 200)

        self.assertEqual(len(resp.json()[const.USERS]), 1)
        for user in resp.json()[const.USERS]:
            self.assertEqual(user[const.RAX_AUTH_UNVERIFIED], True)
            self.assertEqual(user[const.RAX_AUTH_DOMAIN_ID], self.domain_id_1)
            self.assertIn(user[const.ID], self.unverified_user_ids)
            self.assertEqual(user[const.ENABLED], False)
            self.assertEqual(user[const.EMAIL], self.unverified_user_email)

    @tags('positive', 'p1', 'smoke')
    @pytest.mark.smoke_alpha
    def test_list_unverified_users_by_specific_domain_and_admin_only(self):
        """List unverified users with query parameter domain_id, user_type
           and admin_only
        """
        resp = self.identity_admin_client.list_users(
            option={'domain_id': self.domain_id_1,
                    'admin_only': True,
                    'user_type': const.UNVERIFIED})
        self.assertEqual(resp.status_code, 200)

        self.assertEqual(len(resp.json()[const.USERS]), 0)

    @tags('positive', 'p0', 'smoke')
    @pytest.mark.smoke_alpha
    def test_list_verified_users_by_domain_id_and_fed_user_email(self):
        """List all users by query param domain_id, email and user_type
        """
        resp = self.identity_admin_client.list_users(
            option={'domain_id': self.domain_id_1,
                    'user_type': const.VERIFIED,
                    'email': self.fed_user_email})
        self.assertEqual(resp.status_code, 200)

        self.assertEqual(len(resp.json()[const.USERS]), 1)
        # federated user returned from domain1
        for user in resp.json()[const.USERS]:
            self.assertEqual(user[const.RAX_AUTH_DOMAIN_ID],
                             self.domain_id_1)
            self.assertEqual(user[const.EMAIL], self.fed_user_email)

    @tags('positive', 'p0', 'smoke')
    @pytest.mark.smoke_alpha
    def test_list_all_users_by_domain_id(self):
        """List verified users by query param domain_id,user_type
        """
        resp = self.identity_admin_client.list_users(
            option={'domain_id': self.domain_id_1,
                    'user_type': const.ALL})
        self.assertEqual(resp.status_code, 200)

        self.assertEqual(len(resp.json()[const.USERS]), 3)
        # fed user returned from domain1
        user_ids = []
        for user in resp.json()[const.USERS]:
            user_ids.append(user[const.ID])
            self.assertEqual(user[const.RAX_AUTH_DOMAIN_ID],
                             self.domain_id_1)
        self.assertIn(self.fed_user_id, user_ids)

    @tags('positive', 'p0', 'smoke')
    @pytest.mark.smoke_alpha
    def test_list_users_in_specified_domain(self):
        """List users in specified domain by domain_id param
        """
        resp = self.identity_admin_client.list_users(
            option={'domain_id': self.domain_id_2})
        self.assertEqual(resp.status_code, 200)

        #  Returns both user-admin and sub user
        self.assertEqual(len(resp.json()[const.USERS]), 2)
        for user in resp.json()[const.USERS]:
            self.assertEqual(user[const.RAX_AUTH_DOMAIN_ID], self.domain_id_2)

        """List all users by domain_id param
           and admin_only param false
        """
        resp = self.identity_admin_client.list_users(
            option={'domain_id': self.domain_id_2,
                    'admin_only': False})
        self.assertEqual(resp.status_code, 200)

        # Returns both user-admin and sub user
        self.assertEqual(len(resp.json()[const.USERS]), 2)
        for user in resp.json()[const.USERS]:
            self.assertEqual(user[const.RAX_AUTH_DOMAIN_ID], self.domain_id_2)

    @tags('positive', 'p0', 'smoke')
    @pytest.mark.smoke_alpha
    def test_list_verified_users_by_domain_id_and_email(self):
        """List verified users by query param domain_id, email and user_type
        """
        resp = self.identity_admin_client.list_users(
            option={'domain_id': self.domain_id_2,
                    'user_type': const.VERIFIED,
                    'email': self.email_sub_user_2})
        self.assertEqual(resp.status_code, 200)

        self.assertEqual(len(resp.json()[const.USERS]), 1)
        # subuser returned from domain2
        for user in resp.json()[const.USERS]:
            self.assertEqual(user[const.RAX_AUTH_DOMAIN_ID],
                             self.domain_id_2)
            self.assertEqual(user[const.EMAIL], self.email_sub_user_2)

    @SkipTest
    @tags('negative', 'p1', 'regression')
    @pytest.mark.regression
    def test_list_users_for_nonexisting_domain_id(self):
        """Validate the error msg for nonexisting domain_id.
            skipping this test, as it is failing on repose
            side
        """
        resp = self.identity_admin_client.list_users(
            option={'domain_id': self.non_existing_id})

        self.assertEqual(resp.status_code, 404)
        self.assertEqual(
            resp.json()[const.ITEM_NOT_FOUND][const.MESSAGE],
            "Domain with ID {0} not found.".format(self.non_existing_id))

    @tags('positive', 'p0', 'smoke')
    @pytest.mark.smoke_alpha
    def test_list_users_for_specified_tenant(self):
        """List all users for a specified tenant's domain
        """
        resp = self.identity_admin_client.list_users(
            option={'tenant_id': self.tenant_id})
        self.assertEqual(resp.status_code, 200)

        self.assertEqual(len(resp.json()[const.USERS]), 2)
        for user in resp.json()[const.USERS]:
            self.assertEqual(user[const.RAX_AUTH_DOMAIN_ID],
                             self.domain_id_2)

        """List user admin by identity admin
           with admin_only param false
        """
        resp = self.identity_admin_client.list_users(
            option={'tenant_id': self.tenant_id,
                    'admin_only': False})
        self.assertEqual(resp.status_code, 200)

        self.assertEqual(len(resp.json()[const.USERS]), 2)
        for user in resp.json()[const.USERS]:
            self.assertEqual(user[const.RAX_AUTH_DOMAIN_ID],
                             self.domain_id_2)

    @tags('positive', 'p0', 'smoke')
    @pytest.mark.smoke_alpha
    def test_list_all_users_for_specified_tenant_by_name(self):
        """List users for a tenant_id, admin_only and name
        """
        resp = self.identity_admin_client.list_users(
            option={'tenant_id': self.tenant_id,
                    'admin_only': False,
                    'name': self.sub_user_name_2})

        self.assertEqual(resp.status_code, 200)
        # subuser returned from domain2
        for user in resp.json()[const.USERS]:
            self.assertEqual(user[const.RAX_AUTH_DOMAIN_ID],
                             self.domain_id_2)
            self.assertEqual(user[const.USERNAME], self.sub_user_name_2)

    @tags('positive', 'p0', 'smoke')
    @pytest.mark.smoke_alpha
    def test_list_user_admin_for_specified_tenant(self):
        """List user admin for specified tenant's domain
           with admin_only param true
        """
        resp = self.identity_admin_client.list_users(
            option={'tenant_id': self.tenant_id,
                    'admin_only': True})
        self.assertEqual(resp.status_code, 200)

        self.assertEqual(len(resp.json()[const.USERS]), 1)
        for user in resp.json()[const.USERS]:
            self.assertEqual(user[const.RAX_AUTH_DOMAIN_ID],
                             self.domain_id_2)
            self.assertEqual(user[const.EMAIL], self.email_user_admin_2)

    @tags('positive', 'p0', 'smoke')
    @pytest.mark.smoke_alpha
    def test_list_user_admin_for_specified_tenant_by_email(self):
        # search by tenant_id, admin_only=true and useradmin email
        resp = self.identity_admin_client.list_users(
            option={'tenant_id': self.tenant_id,
                    'admin_only': True,
                    'email': self.email_user_admin_2
                    })
        self.assertEqual(resp.status_code, 200)

        self.assertEqual(len(resp.json()[const.USERS]), 1)
        for user in resp.json()[const.USERS]:
            self.assertEqual(user[const.RAX_AUTH_DOMAIN_ID],
                             self.domain_id_2)
            self.assertEqual(user[const.EMAIL], self.email_user_admin_2)

        # search by tenant_id, admin_only=true and subuser email
        resp = self.identity_admin_client.list_users(
            option={'tenant_id': self.tenant_id,
                    'admin_only': True,
                    'email': self.email_sub_user_2
                    })
        self.assertEqual(resp.status_code, 200)

        self.assertEqual(len(resp.json()[const.USERS]), 0)

    @tags('positive', 'p1', 'smoke')
    @pytest.mark.smoke_alpha
    def test_list_user_admin_for_specified_tenant_by_email_name(self):

        """search by tenant_id, admin_only=true and useradmin email and
        username from other domain(tenant doesn't belong to)
        """
        resp = self.identity_admin_client.list_users(
            option={'tenant_id': self.tenant_id,
                    'admin_only': True,
                    'email': self.email_user_admin_2,
                    'name': self.username_ua_1
                    })
        self.assertEqual(resp.status_code, 200)

        self.assertEqual(len(resp.json()[const.USERS]), 0)

    @tags('positive', 'p0', 'smoke')
    @pytest.mark.smoke_alpha
    def test_list_verified_user_admin_for_specified_tenant(self):
        """List verified users with query params tenant_id, admin_only,
            user_type and email
        """
        resp = self.identity_admin_client.list_users(
            option={'tenant_id': self.tenant_id,
                    'admin_only': True,
                    'user_type': const.VERIFIED,
                    'email': self.email_user_admin_2})
        self.assertEqual(resp.status_code, 200)

        self.assertEqual(len(resp.json()[const.USERS]), 1)

        # user admin returned from domain2
        for user in resp.json()[const.USERS]:
            self.assertEqual(user[const.RAX_AUTH_DOMAIN_ID],
                             self.domain_id_2)
            self.assertEqual(user[const.EMAIL], self.email_user_admin_2)

    @tags('positive', 'p1', 'smoke')
    @pytest.mark.smoke_alpha
    def test_list_unverified_user_for_specified_tenant(self):
        """List unverified users with query params tenant_id,
            user_type
        """
        resp = self.identity_admin_client.list_users(
            option={'tenant_id': self.tenant_id,
                    'user_type': const.UNVERIFIED})
        self.assertEqual(resp.status_code, 200)

        self.assertEqual(len(resp.json()[const.USERS]), 0)

    @tags('negative', 'p1', 'regression')
    @pytest.mark.regression
    def test_list_all_users_for_specified_tenant_by_user_type_name(self):
        """verify 'user_type' parameter
           cannot be used with the 'name' parameter.
        """
        resp = self.identity_admin_client.list_users(
            option={'tenant_id': self.tenant_id,
                    'user_type': const.ALL,
                    'name': self.sub_user_name_2})
        self.assertEqual(resp.status_code, 400)
        self.assertEqual(
            resp.json()[const.BAD_REQUEST][const.MESSAGE],
            "Error code: 'GEN-000'; The 'user_type' parameter"
            " can not be used with the 'name' parameter.")

    @tags('negative', 'p0', 'regression')
    @pytest.mark.regression
    def test_list_users_by_user_admin_with_param_tenant_id(self):
        """tenant_id and admin_only cannot be accessed by user-admin
        """
        resp = self.user_admin_client_2.list_users(
            option={'tenant_id': self.tenant_id,
                    'admin_only': True})

        self.assertEqual(resp.status_code, 403)

    @tags('negative', 'p0', 'regression')
    @pytest.mark.regression
    def test_list_users_by_tenant_id_and_domain_id(self):
        """tenant_id and domain_id both cannot be specified
        """
        resp = self.identity_admin_client.list_users(
            option={'tenant_id': self.tenant_id,
                    'domain_id': self.domain_id_2})

        self.assertEqual(resp.status_code, 400)
        self.assertEqual(
            resp.json()[const.BAD_REQUEST][const.MESSAGE],
            "Error code: 'GEN-000'; The 'tenant_id' parameter"
            " can not be used with the 'domain_id' parameter.")

    @SkipTest
    @tags('negative', 'p1', 'regression')
    @pytest.mark.regression
    def test_list_users_for_nonexisting_tenant_id(self):
        """Validate error msg for non existing tenant_id
           skipping this test, as it is failing on repose side
        """
        resp = self.identity_admin_client.list_users(
            option={'tenant_id': self.non_existing_id})

        self.assertEqual(resp.status_code, 404)
        self.assertEqual(
            resp.json()[const.ITEM_NOT_FOUND][const.MESSAGE],
            "Tenant with id/name: '{0}' was not found.".format(
                self.non_existing_id))

    @unless_coverage
    def tearDown(self):
        super(TestSearchUserAdminByTenantOrDomain, self).tearDown()

    @classmethod
    @unless_coverage
    @base.base.log_tearDown_error
    def tearDownClass(cls):
        # Delete all users created in the setUpClass
        for id in cls.unverified_user_ids:
            cls.identity_admin_client.delete_user(user_id=id)
        for id in cls.sub_user_ids:
            cls.identity_admin_client.delete_user(user_id=id)
        for id in cls.fed_user_ids:
            cls.identity_admin_client.delete_user(user_id=id)
        cls.delete_client(client=cls.user_admin_client_1,
                          parent_client=cls.identity_admin_client)
        cls.delete_client(client=cls.user_admin_client_2,
                          parent_client=cls.identity_admin_client)

        super(TestSearchUserAdminByTenantOrDomain, cls).tearDownClass()
