# -*- coding: utf-8 -*
from allpairspy import AllPairs
import ddt

from tests.api.utils import saml_helper
from tests.api.utils.create_cert import create_self_signed_cert
from tests.api.v2 import base
from tests.api.v2.models import factory

from tests.package.johny import constants as const
from tests.package.johny.v2 import client
from tests.package.johny.v2.models import requests


@ddt.ddt
class TestAddIDP(base.TestBaseV2):

    """Add IDP Tests
    Currently only tests which involve setting the name."""

    @classmethod
    def setUpClass(cls):
        """Class level set up for the tests

        Create users needed for the tests and generate clients for those users.
        """
        super(TestAddIDP, cls).setUpClass()
        user_name = cls.generate_random_string()
        password = cls.generate_random_string(const.PASSWORD_PATTERN)
        request_object = requests.UserAdd(user_name=user_name,
                                          password=password)
        resp = cls.service_admin_client.add_user(request_object)

        req_obj = requests.AuthenticateWithPassword(
            user_name=user_name,
            password=password)

        cls.idp_ia_client = client.IdentityAPIClient(
            url=cls.url,
            serialize_format=cls.test_config.serialize_format,
            deserialize_format=cls.test_config.deserialize_format)

        resp = cls.idp_ia_client.get_auth_token(request_object=req_obj)
        identity_admin_auth_token = resp.json()[const.ACCESS][const.TOKEN][
            const.ID]
        identity_admin_id = resp.json()[const.ACCESS][const.USER][const.ID]
        cls.idp_ia_client.default_headers[const.X_AUTH_TOKEN] = (
            identity_admin_auth_token)
        cls.idp_ia_client.default_headers[const.X_USER_ID] = (
            identity_admin_id
        )

        if cls.test_config.run_service_admin_tests:
            option = {
                const.PARAM_ROLE_NAME: const.PROVIDER_MANAGEMENT_ROLE_NAME
            }
            list_resp = cls.service_admin_client.list_roles(option=option)
            mapping_rules_role_id = list_resp.json()[const.ROLES][0][const.ID]
            cls.service_admin_client.add_role_to_user(
                user_id=identity_admin_id, role_id=mapping_rules_role_id)

    def setUp(self):
        super(TestAddIDP, self).setUp()
        self.provider_ids = []
        self.user_ids = []
        self.domain_ids = []

    def create_idp_helper(self, dom_ids=None, fed_type=None, certs=None):
        dom_group = None
        if fed_type == const.BROKER:
            dom_group = const.GLOBAL.upper()
        request_object = factory.get_add_idp_request_object(
            approved_domain_ids=dom_ids, federation_type=fed_type,
            approved_domain_group=dom_group, public_certificates=certs)
        resp = self.idp_ia_client.create_idp(request_object)
        self.assertEquals(resp.status_code, 201)
        self.provider_ids.append(resp.json()[
            const.NS_IDENTITY_PROVIDER][const.ID])
        return request_object

    def test_add_idp_with_name(self):
        '''Add with a name
        '''
        if not self.test_config.run_service_admin_tests:
            self.skipTest('Skipping Service Admin Tests per config value')

        request_object = factory.get_add_idp_request_object()
        resp = self.idp_ia_client.create_idp(request_object)
        self.assertEquals(resp.status_code, 201)
        self.provider_ids.append(resp.json()[
            const.NS_IDENTITY_PROVIDER][const.ID])
        self.assertEquals(resp.json()[const.NS_IDENTITY_PROVIDER][const.NAME],
                          request_object.idp_name)

    def test_add_idp_with_no_name(self):
        '''Add with empty  name
        '''
        if not self.test_config.run_service_admin_tests:
            self.skipTest('Skipping Service Admin Tests per config value')
        request_object = factory.get_add_idp_request_object()
        request_object.idp_name = None
        resp = self.idp_ia_client.create_idp(request_object)
        self.assertEquals(resp.status_code, 400)
        self.assertEquals(resp.json()[const.BAD_REQUEST][const.MESSAGE],
                          "Error code: 'GEN-001'; 'name' is a required"
                          " attribute")

    def test_add_idp_with_empty_name(self):
        '''Add with empty  name
        '''
        if not self.test_config.run_service_admin_tests:
            self.skipTest('Skipping Service Admin Tests per config value')
        request_object = factory.get_add_idp_request_object()
        request_object.idp_name = ""
        resp = self.idp_ia_client.create_idp(request_object)
        self.assertEquals(resp.status_code, 400)
        self.assertEquals(resp.json()[const.BAD_REQUEST][const.MESSAGE],
                          "Error code: 'GEN-001'; 'name' is a required"
                          " attribute")

    def test_add_idp_with_dup_name(self):
        '''Add with dup name
        '''
        if not self.test_config.run_service_admin_tests:
            self.skipTest('Skipping Service Admin Tests per config value')

        request_object = factory.get_add_idp_request_object()
        resp = self.idp_ia_client.create_idp(request_object)
        self.assertEquals(resp.status_code, 201)
        self.provider_ids.append(resp.json()[
            const.NS_IDENTITY_PROVIDER][const.ID])
        resp = self.idp_ia_client.create_idp(request_object)
        self.assertEquals(resp.status_code, 409)
        self.assertEquals(resp.json()[const.BAD_REQUEST][const.MESSAGE],
                          "Error code: 'FED_IDP-005'; Identity provider with "
                          "name {0} already exist.".format(
                              request_object.idp_name))

    def test_add_idp_name_max_length(self):
        '''Add with bad characters in name
        '''
        if not self.test_config.run_service_admin_tests:
            self.skipTest('Skipping Service Admin Tests per config value')
        request_object = factory.get_add_idp_request_object()
        request_object.idp_name = self.generate_random_string(
            const.MAX_IDP_NAME_PATTERN)
        resp = self.idp_ia_client.create_idp(request_object)
        self.assertEquals(resp.status_code, 201)
        self.provider_ids.append(resp.json()[
            const.NS_IDENTITY_PROVIDER][const.ID])

        # verify name wasn't truncated
        get_name_resp = self.idp_ia_client.get_idp(idp_id=resp.json()[
            const.NS_IDENTITY_PROVIDER][const.ID])
        get_name = get_name_resp.json()[const.NS_IDENTITY_PROVIDER][const.NAME]
        self.assertEquals(get_name, request_object.idp_name)

        # Try with longer name
        request_object.idp_name += "B"
        resp = self.idp_ia_client.create_idp(request_object)
        self.assertEquals(resp.status_code, 400)
        self.assertEquals(resp.json()[const.BAD_REQUEST][const.MESSAGE],
                          "Error code: 'GEN-002'; name length cannot exceed "
                          "255 characters")

    def check_bad_name(self, name):
        """ Helper method to isolate repeating bad name code. """
        if not self.test_config.run_service_admin_tests:
            self.skipTest('Skipping Service Admin Tests per config value')
        request_object = factory.get_add_idp_request_object()
        request_object.idp_name = name
        resp = self.idp_ia_client.create_idp(request_object)
        self.assertEquals(resp.status_code, 400)
        self.assertEquals(resp.json()[const.BAD_REQUEST][const.MESSAGE],
                          "Error code: 'GEN-005'; Identity provider name must"
                          " consist of only alphanumeric, '.', and '-'"
                          " characters.")

    def test_add_idp_with_bad_char(self):
        '''Add with bad characters in name
        '''
        self.check_bad_name(name="DSAFDSFA#@$@$#@$AFAS")

    def test_add_idp_with_spaces_at_end(self):
        '''Add with spaces at the end of the name.
        '''
        self.check_bad_name(
            name=self.generate_random_string(const.IDP_NAME_PATTERN) + "  ")

    def test_add_idp_with_spaces_at_the_beginning(self):
        '''Add with spaces at the beginning of the name.
        '''
        self.check_bad_name(
            name="  " + self.generate_random_string(const.IDP_NAME_PATTERN))

    def add_and_check_broker_idp(self, certs=None):
        if not self.test_config.run_service_admin_tests:
            self.skipTest('Skipping Service Admin Tests per config value')

        idp = self.create_idp_helper(fed_type=const.BROKER, certs=certs)
        idp_list = self.idp_ia_client.list_idp(
            option={"name": idp.idp_name}).json()[const.NS_IDENTITY_PROVIDERS]
        self.assertEquals(len(idp_list), 1)
        self.assertEquals(idp_list[0][const.FEDERATION_TYPE], "BROKER")
        get_resp = self.idp_ia_client.get_idp(idp_id=idp_list[0][const.ID])
        get_name = get_resp.json()[const.NS_IDENTITY_PROVIDER][const.NAME]
        self.assertEquals(get_name, idp.idp_name)

        self.assertEquals(get_resp.json()[
            const.NS_IDENTITY_PROVIDER][const.FEDERATION_TYPE], "BROKER")

        return idp

    def test_add_broker_idp(self):
        self.add_and_check_broker_idp()

    def test_adg_with_broker_idp(self):
        fed_type = const.BROKER
        dom_group = "BADVALUE"
        dom_ids = None
        request_object = factory.get_add_idp_request_object(
            approved_domain_ids=dom_ids, federation_type=fed_type,
            approved_domain_group=dom_group)
        resp = self.idp_ia_client.create_idp(request_object)
        self.assertEquals(resp.status_code, 400)

        self.assertEquals(
            resp.json()[const.BAD_REQUEST][const.MESSAGE],
            "Error code: 'FED_IDP-001'; When BROKER IDP is specified, the"
            " approvedDomainGroup must be set, and specified as GLOBAL")

    def test_cant_auth_with_broker_idp(self):
        """ Note: will fail once broker auth is enabled. """
        test_data = {"fed_input": {
                     "base64_url_encode": True,
                     "new_url": True,
                     "content_type": "x-www-form-urlencoded"}}
        (pem_encoded_cert, cert_path, _, key_path,
         f_print) = create_self_signed_cert()

        idp = self.add_and_check_broker_idp(certs=[pem_encoded_cert])
        subject = self.generate_random_string(
            pattern='fed[\-]user[\-][\d\w]{12}')
        fed_input_data = test_data['fed_input']
        base64_url_encode = fed_input_data['base64_url_encode']
        new_url = fed_input_data['new_url']
        content_type = fed_input_data['content_type']

        dom_id = self.generate_random_string(const.NUMERIC_DOMAIN_ID_PATTERN)
        test_email = "random@rackspace.com"
        issuer = idp.issuer
        cert = saml_helper.create_saml_assertion(
            domain=dom_id, subject=subject, issuer=issuer,
            email=test_email, base64_url_encode=base64_url_encode,
            private_key_path=key_path,
            public_key_path=cert_path)
        auth = self.identity_admin_client.auth_with_saml(
            saml=cert, content_type=content_type,
            base64_url_encode=base64_url_encode, new_url=new_url)
        self.assertEquals(
            auth.json()[const.FORBIDDEN][const.MESSAGE],
            "Error code: 'FED-000'; v1 Authentication "
            "is not supported for this IDP")
        self.assertEquals(403, auth.status_code)

    def test_add_idp_with_name_get_idp(self):
        '''Verify get provider by id has name attribute
        '''
        if not self.test_config.run_service_admin_tests:
            self.skipTest('Skipping Service Admin Tests per config value')

        request_object = factory.get_add_idp_request_object()
        resp = self.idp_ia_client.create_idp(request_object)
        self.assertEquals(resp.status_code, 201)
        self.provider_ids.append(resp.json()[
            const.NS_IDENTITY_PROVIDER][const.ID])

        get_name_resp = self.idp_ia_client.get_idp(idp_id=resp.json()[
            const.NS_IDENTITY_PROVIDER][const.ID])
        get_name = get_name_resp.json()[const.NS_IDENTITY_PROVIDER][const.NAME]
        self.assertEquals(get_name, request_object.idp_name)

    def test_add_idp_with_name_list_idp(self):
        '''Verify list providers has name attribute
        '''
        if not self.test_config.run_service_admin_tests:
            self.skipTest('Skipping Service Admin Tests per config value')

        request_object = factory.get_add_idp_request_object()
        resp = self.idp_ia_client.create_idp(request_object)
        self.assertEquals(resp.status_code, 201)
        self.provider_ids.append(resp.json()[
            const.NS_IDENTITY_PROVIDER][const.ID])

        idp_resp = self.idp_ia_client.list_idp()
        self.assertEquals(resp.status_code, 201)
        idps = idp_resp.json()[
            const.NS_IDENTITY_PROVIDERS]
        found = False
        for idp in idps:
            idp_name = idp[const.NAME]
            if idp_name == request_object.idp_name:
                found = True
        self.assertEquals(found, True)

    @ddt.data(*AllPairs([["issuer", "name"],
                         ["test12345", "*"]]))
    def test_list_idp_query_param_name_missed_hit(self, data):
        '''Verify list providers can filter by name parameter
        '''
        name = data[0]
        value = data[1]

        self.create_idp_helper()
        if not self.test_config.run_service_admin_tests:
            self.skipTest('Skipping Service Admin Tests per config value')
        idp_list = self.idp_ia_client.list_idp(
            option={"name": None}).json()[
                const.NS_IDENTITY_PROVIDERS]

        self.assertTrue(len(idp_list) > 1)

        idp_resp = self.idp_ia_client.list_idp(option={name: value})
        self.assertEquals(idp_resp.status_code, 200)
        idp_list = idp_resp.json()[
            const.NS_IDENTITY_PROVIDERS]

        self.assertTrue(len(idp_list) == 0)

    def test_list_idp_query_param_issuer_case(self):
        '''Verify list providers issuer filter is case sensitive
        '''
        if not self.test_config.run_service_admin_tests:
            self.skipTest('Skipping Service Admin Tests per config value')

        idps = [self.create_idp_helper(), self.create_idp_helper()]
        idp_resp = self.idp_ia_client.list_idp(
            option={"issuer": idps[0].issuer.upper()})
        idp_list = idp_resp.json()[
            const.NS_IDENTITY_PROVIDERS]

        self.assertEquals(idp_resp.status_code, 200)
        self.assertTrue(len(idp_list) == 0)

    @ddt.data(*AllPairs([["issuer", "name"],
                         [None, ""]]))
    def test_list_idp_query_param_ignore_null_empty(self, data):
        '''Verify list providers can filter by name parameter
        '''
        name = data[0]
        value = data[1]

        self.create_idp_helper()
        if not self.test_config.run_service_admin_tests:
            self.skipTest('Skipping Service Admin Tests per config value')
        idp_resp = self.idp_ia_client.list_idp(
            option={name: value})

        self.assertEquals(idp_resp.status_code, 200)
        idp_list = idp_resp.json()[
            const.NS_IDENTITY_PROVIDERS]

        idp_resp2 = self.idp_ia_client.list_idp()
        self.assertEquals(idp_resp.status_code, 200)
        idp_list2 = idp_resp2.json()[
            const.NS_IDENTITY_PROVIDERS]

        self.assertEqual(len(idp_list), len(idp_list2))

    @ddt.data("name", "issuer")
    def test_list_idp_query_param_name(self, name):
        '''Verify list providers can filter by name parameter
           Also tests that additional values are ignored.
        '''
        if not self.test_config.run_service_admin_tests:
            self.skipTest('Skipping Service Admin Tests per config value')
        idps = [self.create_idp_helper(), self.create_idp_helper()]
        found = True
        for idp in idps:
            if name == "name":
                value = [idp.idp_name.upper(), "blah"]
            else:
                # Note that issuer search is not case insensitive like name.
                value = [idp.issuer, "blah"]
            idp_resp = self.idp_ia_client.list_idp(
                option={name: value})
            self.assertEquals(idp_resp.status_code, 200)
            idp_list = idp_resp.json()[
                const.NS_IDENTITY_PROVIDERS]
            if len(idp_list) < 1 or idp_list[0][const.NAME] != idp.idp_name:
                found = False
        self.assertEquals(found, True)

    # use_property means to get that property from the generated idp object.
    @ddt.data(*AllPairs([["issuer", "name"],
                         [None, "", "use_property"]]))
    def test_list_idp_mixed_query_param(self, data):
        '''Verify list providers can filter by name parameter
        '''
        name = data[0]
        value = data[1]
        if not self.test_config.run_service_admin_tests:
            self.skipTest('Skipping Service Admin Tests per config value')
        user_name = self.generate_random_string(
            pattern=const.SUB_USER_PATTERN)
        # create admin user
        dom_id = self.generate_random_string(const.NUMERIC_DOMAIN_ID_PATTERN)
        request_object = requests.UserAdd(
            user_name=user_name,
            domain_id=dom_id)
        self.domain_ids.append(dom_id)

        resp = self.idp_ia_client.add_user(request_object)
        self.user_ids.append(resp.json()[const.USER][const.ID])
        dom_id = resp.json()[const.USER][const.RAX_AUTH_DOMAIN_ID]
        idps = [self.create_idp_helper(dom_ids=[dom_id]),
                self.create_idp_helper(dom_ids=[dom_id])]
        found = True
        for idp in idps:
            if value == "use_property":
                if name == "name":
                    value = [idp.idp_name.upper(), "blah"]
                else:
                    value = [idp.issuer]
            option = {name: value,
                      "approved_DomainId": dom_id}
            idp_resp = self.idp_ia_client.list_idp(option=option)
            self.assertEquals(idp_resp.status_code, 200)
            idp_list = idp_resp.json()[
                const.NS_IDENTITY_PROVIDERS]

            if (value == "use_property" and
                (len(idp_list) > 1 or
                 idp_list[0][const.NAME] != idp.idp_name)):
                found = False
            elif len(idp_list) < 1:
                found = False

        self.assertEquals(found, True)

    def tearDown(self):
        # Delete all providers created in the tests

        for id_ in self.provider_ids:
            self.idp_ia_client.delete_idp(idp_id=id_)
        # Delete all users created in the tests
        for id in self.user_ids:
            self.idp_ia_client.delete_user(user_id=id)
        for dom in self.domain_ids:
            self.idp_ia_client.delete_domain(domain_id=dom)
        super(TestAddIDP, self).tearDown()

    @classmethod
    def tearDownClass(cls):
        # Delete all users created in the setUpClass
        cls.delete_client(client=cls.idp_ia_client,
                          parent_client=cls.service_admin_client)
        super(TestAddIDP, cls).tearDownClass()
