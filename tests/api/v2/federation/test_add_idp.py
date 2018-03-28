# -*- coding: utf-8 -*
from allpairspy import AllPairs

import ddt
from nose.plugins.attrib import attr

from tests.api.v2.federation import federation
from tests.api.v2.models import factory
from tests.api.v2.schema import idp as idp_json

from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


@ddt.ddt
class TestAddIDP(federation.TestBaseFederation):

    """Add IDP Tests."""

    @classmethod
    def setUpClass(cls):
        """Class level set up for the tests

        Create users needed for the tests and generate clients for those users.
        """
        super(TestAddIDP, cls).setUpClass()

    def setUp(self):
        super(TestAddIDP, self).setUp()

    @attr(type='regression')
    def test_add_idp_email_domain(self):
        email_domains = []
        email_domains.append(self.generate_random_string(const.EMAIL_PATTERN))

        domain_id = self.create_one_user_and_get_domain(
            auth_client=self.identity_admin_client)

        # Add IDP with email domain.
        request_object = factory.get_add_idp_request_object(
            federation_type='DOMAIN', approved_domain_ids=[domain_id],
            email_domains=email_domains)

        resp = self.identity_admin_client.create_idp(request_object)
        self.assertEquals(resp.status_code, 201)
        idp_id = resp.json()[const.NS_IDENTITY_PROVIDER][const.ID]
        self.provider_ids.append(idp_id)

        # Get IDPs by email domain.
        option = {const.EMAIL_DOMAIN: email_domains[0]}
        resp = self.identity_admin_client.list_idp(option=option)
        idp = resp.json()[const.NS_IDENTITY_PROVIDERS][0]
        self.assertEqual(idp[const.ID], idp_id)
        self.assertSchema(
            response=resp,
            json_schema=idp_json.list_idps)

        # Update IDP's Email Domain.
        updated_email_domains = []
        updated_email_domains.append(self.generate_random_string(
            const.EMAIL_PATTERN))

        idp_obj = requests.IDP(email_domains=updated_email_domains)
        resp = self.identity_admin_client.update_idp(
            idp_id=idp_id,
            request_object=idp_obj)
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(
            resp.json()[const.NS_IDENTITY_PROVIDER][const.EMAIL_DOMAINS],
            updated_email_domains)
        self.assertSchema(
            response=resp,
            json_schema=idp_json.identity_provider_w_email_domain)

        # Get IDP by ID & verify response.
        resp = self.identity_admin_client.get_idp(idp_id=idp_id)
        self.assertEqual(
            resp.json()[const.NS_IDENTITY_PROVIDER][const.EMAIL_DOMAINS],
            updated_email_domains)
        self.assertSchema(
            response=resp,
            json_schema=idp_json.identity_provider_w_email_domain)

    @attr(type='regression')
    def test_add_idp_with_name(self):
        '''Add with a name.'''

        request_object = factory.get_add_idp_request_object()
        resp = self.identity_admin_client.create_idp(request_object)
        self.assertEquals(resp.status_code, 201)
        self.provider_ids.append(resp.json()[
            const.NS_IDENTITY_PROVIDER][const.ID])
        self.assertEquals(resp.json()[const.NS_IDENTITY_PROVIDER][const.NAME],
                          request_object.idp_name)

    def test_add_idp_with_no_name(self):
        '''Add with empty  name.'''
        request_object = factory.get_add_idp_request_object()
        request_object.idp_name = None
        resp = self.identity_admin_client.create_idp(request_object)
        self.assertEquals(resp.status_code, 400)
        self.assertEquals(resp.json()[const.BAD_REQUEST][const.MESSAGE],
                          "Error code: 'GEN-001'; 'name' is a required"
                          " attribute")

    def test_add_idp_with_empty_name(self):
        '''Add with empty name.'''
        request_object = factory.get_add_idp_request_object()
        request_object.idp_name = ""
        resp = self.identity_admin_client.create_idp(request_object)
        self.assertEquals(resp.status_code, 400)
        self.assertEquals(resp.json()[const.BAD_REQUEST][const.MESSAGE],
                          "Error code: 'GEN-001'; 'name' is a required"
                          " attribute")

    def test_add_idp_with_dup_name(self):
        '''Add with dup name.'''
        request_object = factory.get_add_idp_request_object()
        resp = self.identity_admin_client.create_idp(request_object)
        self.assertEquals(resp.status_code, 201)
        self.provider_ids.append(resp.json()[
            const.NS_IDENTITY_PROVIDER][const.ID])
        resp = self.identity_admin_client.create_idp(request_object)
        self.assertEquals(resp.status_code, 409)
        self.assertEquals(resp.json()[const.BAD_REQUEST][const.MESSAGE],
                          "Error code: 'FED_IDP-005'; Identity provider with "
                          "name {0} already exist.".format(
                              request_object.idp_name))

    def test_add_idp_name_max_length(self):
        '''Add with bad characters in name.'''
        request_object = factory.get_add_idp_request_object()
        request_object.idp_name = self.generate_random_string(
            const.MAX_IDP_NAME_PATTERN)
        resp = self.identity_admin_client.create_idp(request_object)
        self.assertEquals(resp.status_code, 201)
        self.provider_ids.append(resp.json()[
            const.NS_IDENTITY_PROVIDER][const.ID])

        # verify name wasn't truncated
        get_name_resp = self.identity_admin_client.get_idp(idp_id=resp.json()[
            const.NS_IDENTITY_PROVIDER][const.ID])
        get_name = get_name_resp.json()[const.NS_IDENTITY_PROVIDER][const.NAME]
        self.assertEquals(get_name, request_object.idp_name)

        # Try with longer name
        request_object.idp_name += "B"
        resp = self.identity_admin_client.create_idp(request_object)
        self.assertEquals(resp.status_code, 400)
        self.assertEquals(resp.json()[const.BAD_REQUEST][const.MESSAGE],
                          "Error code: 'GEN-002'; name length cannot exceed "
                          "255 characters")

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

    def test_add_broker_idp(self):
        self.add_and_check_broker_idp()

    def test_adg_with_broker_idp(self):
        fed_type = const.BROKER
        dom_group = "BADVALUE"
        dom_ids = None
        request_object = factory.get_add_idp_request_object(
            approved_domain_ids=dom_ids, federation_type=fed_type,
            approved_domain_group=dom_group)
        resp = self.identity_admin_client.create_idp(request_object)
        self.assertEquals(resp.status_code, 400)

        self.assertEquals(
            resp.json()[const.BAD_REQUEST][const.MESSAGE],
            "Error code: 'FED_IDP-001'; When BROKER IDP is specified, the"
            " approvedDomainGroup must be set, and specified as GLOBAL")

    @attr(type='regression')
    def test_add_idp_with_name_get_idp(self):
        '''Verify get provider by id has name attribute.'''
        request_object = factory.get_add_idp_request_object()
        resp = self.identity_admin_client.create_idp(request_object)
        self.assertEquals(resp.status_code, 201)
        self.provider_ids.append(resp.json()[
            const.NS_IDENTITY_PROVIDER][const.ID])

        get_name_resp = self.identity_admin_client.get_idp(
            idp_id=resp.json()[const.NS_IDENTITY_PROVIDER][const.ID])
        get_name = get_name_resp.json()[const.NS_IDENTITY_PROVIDER][const.NAME]
        self.assertEquals(get_name, request_object.idp_name)

    @attr(type='regression')
    def test_add_idp_with_name_list_idp(self):
        '''Verify list providers has name attribute.'''
        request_object = factory.get_add_idp_request_object()
        resp = self.identity_admin_client.create_idp(request_object)
        self.assertEquals(resp.status_code, 201)
        self.provider_ids.append(resp.json()[
            const.NS_IDENTITY_PROVIDER][const.ID])

        idp_resp = self.identity_admin_client.list_idp()
        self.assertEquals(resp.status_code, 201)
        idps = idp_resp.json()[const.NS_IDENTITY_PROVIDERS]
        found = False
        for idp in idps:
            idp_name = idp[const.NAME]
            if idp_name == request_object.idp_name:
                found = True
        self.assertEquals(found, True)

    @attr(type='regression')
    @ddt.data(*AllPairs([["issuer", "name"],
                         ["test12345", "*"]]))
    def test_list_idp_query_param_name_missed_hit(self, data):
        '''Verify list providers can filter by name parameter.'''
        name = data[0]
        value = data[1]

        self.create_idp_helper()
        idp_list = self.identity_admin_client.list_idp(
            option={"name": None}).json()[
                const.NS_IDENTITY_PROVIDERS]

        self.assertTrue(len(idp_list) > 1)

        idp_resp = self.identity_admin_client.list_idp(option={name: value})
        self.assertEquals(idp_resp.status_code, 200)
        idp_list = idp_resp.json()[
            const.NS_IDENTITY_PROVIDERS]

        self.assertTrue(len(idp_list) == 0)

    def test_list_idp_query_param_issuer_case(self):
        '''Verify list providers issuer filter is case sensitive.'''
        idps = [self.create_idp_helper(), self.create_idp_helper()]
        idp_resp = self.identity_admin_client.list_idp(
            option={"issuer": idps[0].issuer.upper()})
        idp_list = idp_resp.json()[
            const.NS_IDENTITY_PROVIDERS]

        self.assertEquals(idp_resp.status_code, 200)
        self.assertTrue(len(idp_list) == 0)

    @ddt.data(*AllPairs([["issuer", "name"],
                         [None, ""]]))
    def test_list_idp_query_param_ignore_null_empty(self, data):
        '''Verify list providers can filter by name parameter.'''
        name = data[0]
        value = data[1]

        self.create_idp_helper()
        idp_resp = self.identity_admin_client.list_idp(
            option={name: value})

        self.assertEquals(idp_resp.status_code, 200)
        idp_list = idp_resp.json()[
            const.NS_IDENTITY_PROVIDERS]

        idp_resp2 = self.identity_admin_client.list_idp()
        self.assertEquals(idp_resp.status_code, 200)
        idp_list2 = idp_resp2.json()[
            const.NS_IDENTITY_PROVIDERS]

        self.assertEqual(len(idp_list), len(idp_list2))

    @ddt.data("name", "issuer")
    def test_list_idp_query_param_name(self, name):
        '''Verify list providers can filter by name parameter
           Also tests that additional values are ignored.
        '''
        idps = [self.create_idp_helper(), self.create_idp_helper()]
        found = True
        for idp in idps:
            if name == "name":
                value = [idp.idp_name.upper(), "blah"]
            else:
                # Note that issuer search is not case insensitive like name.
                value = [idp.issuer, "blah"]
            idp_resp = self.identity_admin_client.list_idp(
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

        # create  user
        user_name = self.generate_random_string(
            pattern=const.SUB_USER_PATTERN)
        dom_id = self.generate_random_string(const.NUMERIC_DOMAIN_ID_PATTERN)
        request_object = requests.UserAdd(
            user_name=user_name,
            domain_id=dom_id)
        self.domain_ids.append(dom_id)

        resp = self.identity_admin_client.add_user(request_object)
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
            idp_resp = self.identity_admin_client.list_idp(option=option)
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

    @attr(type='regression')
    @ddt.data('GLOBAL', 'RACKER', 'BROKER')
    def test_list_idps_for_global_and_racker_idps(self, idp_flavor):
        """
        Tests for verifying CID-1423, that is, if empty list is populated
        for approved domain ids if idp is a GLOBAL, RACKER, BROKER idp
        """

        issuer = self.generate_random_string(pattern=const.ISSUER_PATTERN)
        if idp_flavor == const.APPROVED_DOMAIN_GROUP_GLOBAL:
            request_object = factory.get_add_idp_request_object(
                approved_domain_group=const.APPROVED_DOMAIN_GROUP_GLOBAL,
                issuer=issuer
            )
        elif idp_flavor == const.BROKER:
            request_object = factory.get_add_idp_request_object(
                federation_type=const.BROKER, issuer=issuer,
                approved_domain_group=const.APPROVED_DOMAIN_GROUP_GLOBAL
            )
        else:
            request_object = factory.get_add_idp_request_object(
                federation_type=idp_flavor, issuer=issuer)
        resp = self.identity_admin_client.create_idp(request_object)
        self.assertEquals(resp.status_code, 201)
        self.provider_ids.append(resp.json()[
            const.NS_IDENTITY_PROVIDER][const.ID])
        option = {
            const.ISSUER: issuer
        }
        # list idps with query param
        list_resp = self.identity_admin_client.list_idp(option=option)
        self.assertEqual(
            list_resp.json()[
                const.NS_IDENTITY_PROVIDERS][0][const.APPROVED_DOMAIN_Ids], [])

        # list idps without query param
        list_resp = self.identity_admin_client.list_idp()
        for idp in list_resp.json()[const.NS_IDENTITY_PROVIDERS]:
            if (idp[const.FEDERATION_TYPE] in {const.BROKER, const.RACKER} or
                (const.APPROVED_DOMAIN_GROUP in idp and idp[
                    const.APPROVED_DOMAIN_GROUP] == (
                        const.APPROVED_DOMAIN_GROUP_GLOBAL))):
                self.assertEqual(idp[const.APPROVED_DOMAIN_Ids], [])
            else:
                self.assertNotEqual(idp[const.APPROVED_DOMAIN_Ids], [])

    def tearDown(self):
        super(TestAddIDP, self).tearDown()

    @classmethod
    def tearDownClass(cls):
        super(TestAddIDP, cls).tearDownClass()
