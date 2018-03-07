# -*- coding: utf-8 -*
import ddt

from tests.api.utils.create_cert import create_self_signed_cert
from tests.api.v2.federation import federation
from tests.api.v2.models import factory
from tests.api.v2.schema import idp as idp_json

from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


@ddt.ddt
class TestUpdateIDP(federation.TestBaseFederation):
    """
    Test update IDP
    1. Allow users to update an IDP name
    2. The name must meet the same validations as setting the name on creation
    3. The specified name must be returned in the response as specified in the
    request, in the exact case in which it was originally provided
    """
    @classmethod
    def setUpClass(cls):
        """Class level set up for the tests

        Create users needed for the tests and generate clients for those users.
        """
        super(TestUpdateIDP, cls).setUpClass()

    def setUp(self):
        super(TestUpdateIDP, self).setUp()

        # user admin client
        self.domain_id = self.generate_random_string(pattern='[\d]{7}')

        self.user_clients = {}
        idp_user_admin_client = self.generate_client(
            parent_client=self.identity_admin_client,
            additional_input_data={'domain_id': self.domain_id})
        idp_user_admin_client.serialize_format = 'xml'
        idp_user_admin_client.default_headers[
            const.CONTENT_TYPE] = 'application/xml'
        idp_user_admin_client.default_headers[
            const.ACCEPT] = 'application/json'
        self.user_clients["user:admin"] = idp_user_admin_client

        # user manage client
        idp_user_manage_client = self.generate_client(
            parent_client=idp_user_admin_client,
            additional_input_data={'domain_id': self.domain_id,
                                   'is_user_manager': True})
        idp_user_manage_client.serialize_format = 'xml'
        idp_user_manage_client.default_headers[
            const.CONTENT_TYPE] = 'application/xml'
        idp_user_admin_client.default_headers[
            const.ACCEPT] = 'application/json'
        self.user_clients["user:manage"] = idp_user_manage_client

        self.provider_ids = []
        self.domains = []
        self.clients = []
        self.users = []

    def add_idp_user(self):
        request_object = factory.get_add_idp_request_object()
        resp = self.identity_admin_client.create_idp(request_object)
        self.assertEquals(resp.status_code, 201)
        provider_id = resp.json()[const.NS_IDENTITY_PROVIDER][const.ID]
        self.provider_ids.append(provider_id)
        provider_name = resp.json()[const.NS_IDENTITY_PROVIDER][const.NAME]
        return provider_id, provider_name

    def test_update_idp_valid_name(self):
        """Test update with valid name randomly generate name with
        alphanumeric, '.', and '-' characters in range from 1 to 255
        """
        provider_id, _ = self.add_idp_user()
        idp_name = self.generate_random_string(
            pattern='[a-zA-Z0-9.\-]{1:255}')
        idp_obj = requests.IDP(idp_name=idp_name)
        resp = self.identity_admin_client.update_idp(
            idp_id=provider_id, request_object=idp_obj)
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(resp.json()[const.NS_IDENTITY_PROVIDER][const.NAME],
                         idp_name)

    def test_update_idp_name_with_empty_string(self):
        """Update with empty string"""
        provider_id, _ = self.add_idp_user()
        error_msg = (
            "Error code: 'GEN-001'; 'name' is a required attribute")
        empty_str = " "
        idp_obj = requests.IDP(idp_name=empty_str)
        resp = self.identity_admin_client.update_idp(
            idp_id=provider_id, request_object=idp_obj)
        self.assertEqual(resp.status_code, 400)
        self.assertEqual(resp.json()[const.BAD_REQUEST][const.MESSAGE],
                         error_msg)

    def update_approved_domain_ids_for_idp(
            self, provider_id, new_list_of_domains):

        update_idp_obj = requests.IDP(approved_domain_ids=new_list_of_domains)
        resp = self.identity_admin_client.update_idp(
            idp_id=provider_id, request_object=update_idp_obj)
        self.assertEqual(resp.status_code, 200)

    @ddt.file_data('data_update_idp_fed_user.json')
    def test_update_idp_approved_domain_ids_with_spaces(self, test_data):

        domain_id = self.create_one_user_and_get_domain(users=self.users)
        self.domains.append(domain_id)

        issuer = self.generate_random_string(pattern='issuer[\-][\d\w]{12}')
        provider_id, cert_path, key_path = self.create_idp_with_certs(
            domain_id=domain_id, issuer=issuer)

        fed_auth = self.fed_user_call(
            test_data=test_data, domain_id=domain_id, private_key=key_path,
            public_key=cert_path, issuer=issuer)
        self.assertEqual(fed_auth.status_code, 200)
        fed_token, _, _ = self.parse_auth_response(fed_auth)

        # update the approved domains list
        new_list_of_domains = [domain_id + ' ']
        self.update_approved_domain_ids_for_idp(
            provider_id=provider_id, new_list_of_domains=new_list_of_domains)
        validate_post_update = self.identity_admin_client.validate_token(
            fed_token)
        self.assertEqual(validate_post_update.status_code, 200)

        # Now, try to auth after idp is updated
        fed_auth = self.fed_user_call(
            test_data=test_data, domain_id=domain_id, private_key=key_path,
            public_key=cert_path, issuer=issuer)
        self.assertEqual(fed_auth.status_code, 200)
        self.assertSchema(fed_auth, self.updated_fed_auth_schema)

    @ddt.file_data('data_update_idp_fed_user.json')
    def test_enable_disable_idp(self, test_data):

        domain_id = self.create_one_user_and_get_domain(users=self.users)
        self.domains.append(domain_id)

        issuer = self.generate_random_string(pattern='issuer[\-][\d\w]{12}')
        provider_id, cert_path, key_path = self.create_idp_with_certs(
            domain_id=domain_id, issuer=issuer)

        # Get fed auth token
        fed_auth = self.fed_user_call(
            test_data=test_data, domain_id=domain_id, private_key=key_path,
            public_key=cert_path, issuer=issuer)
        self.assertEqual(fed_auth.status_code, 200)
        self.assertSchema(fed_auth, self.updated_fed_auth_schema)
        fed_token, _, _ = self.parse_auth_response(fed_auth)

        # Disable IDP
        update_req = requests.IDP(enabled=False)
        update_resp = self.identity_admin_client.update_idp(
            idp_id=provider_id, request_object=update_req)
        self.assertEqual(update_resp.status_code, 200)
        self.assertEqual(
            update_resp.json()[const.NS_IDENTITY_PROVIDER][const.ENABLED],
            False)
        self.assertSchema(response=update_resp,
                          json_schema=idp_json.identity_provider)

        # Validate fed auth token
        resp = self.identity_admin_client.validate_token(token_id=fed_token)
        self.assertEqual(resp.status_code, 404)

        # Verify fed auth call fails when IdP is disabled
        fed_auth = self.fed_user_call(
            test_data=test_data, domain_id=domain_id, private_key=key_path,
            public_key=cert_path, issuer=issuer)
        self.assertEqual(fed_auth.status_code, 403)

        # Re-enable IDP
        update_req = requests.IDP(enabled=True)
        update_resp = self.identity_admin_client.update_idp(
            idp_id=provider_id, request_object=update_req)
        self.assertEqual(update_resp.status_code, 200)
        self.assertEqual(
            update_resp.json()[const.NS_IDENTITY_PROVIDER][const.ENABLED],
            True)
        self.assertSchema(response=update_resp,
                          json_schema=idp_json.identity_provider)

        fed_auth = self.fed_user_call(
            test_data=test_data, domain_id=domain_id, private_key=key_path,
            public_key=cert_path, issuer=issuer)
        self.assertEqual(fed_auth.status_code, 200)
        self.assertSchema(fed_auth, self.updated_fed_auth_schema)

    def verify_re_auth_after_idp_update(
            self, test_data, key_path, cert_path, issuer, domain_1, domain_2):

        # Now, try to auth with older domain, after idp is updated
        fed_auth = self.fed_user_call(
            test_data=test_data, domain_id=domain_1, private_key=key_path,
            public_key=cert_path, issuer=issuer)
        self.assertEqual(fed_auth.status_code, 403)

        # Now, try to auth with new domain, after idp is updated
        fed_auth = self.fed_user_call(
            test_data=test_data, domain_id=domain_2, private_key=key_path,
            public_key=cert_path, issuer=issuer)
        self.assertEqual(fed_auth.status_code, 200)
        self.assertSchema(fed_auth, self.updated_fed_auth_schema)

    @ddt.file_data('data_update_idp_fed_user.json')
    def test_update_idp_verify_delete_logs_and_re_auth(self, test_data):

        domain_id = self.create_one_user_and_get_domain(users=self.users)
        self.domains.append(domain_id)

        issuer = self.generate_random_string(pattern='issuer[\-][\d\w]{12}')
        provider_id, cert_path, key_path = self.create_idp_with_certs(
            domain_id=domain_id, issuer=issuer)

        fed_auth = self.fed_user_call(
            test_data=test_data, domain_id=domain_id, private_key=key_path,
            public_key=cert_path, issuer=issuer)
        self.assertEqual(fed_auth.status_code, 200)
        fed_token, fed_user_id, fed_username = self.parse_auth_response(
            fed_auth)

        # update the approved domains list with the new domain
        domain_id_2 = self.create_one_user_and_get_domain(users=self.users)
        self.domains.append(domain_id_2)
        self.update_approved_domain_ids_for_idp(
            provider_id=provider_id, new_list_of_domains=[domain_id_2])

        validate_post_update = self.identity_admin_client.validate_token(
            token_id=fed_token)
        self.assertEqual(validate_post_update.status_code, 404)

        get_fed_user = self.identity_admin_client.get_user(fed_user_id)
        self.assertEqual(get_fed_user.status_code, 404)
        self.verify_re_auth_after_idp_update(
            test_data=test_data, key_path=key_path, cert_path=cert_path,
            issuer=issuer, domain_1=domain_id, domain_2=domain_id_2)

    def test_update_idp_approved_domain_ids_with_duplicates(self):

        request_object = factory.get_domain_request_object({})
        dom_resp = self.identity_admin_client.add_domain(request_object)
        domain_id = dom_resp.json()[const.RAX_AUTH_DOMAIN][const.ID]
        self.domains.append(domain_id)
        request_object = factory.get_add_idp_request_object(
            federation_type='DOMAIN', approved_domain_ids=[domain_id])
        resp = self.identity_admin_client.create_idp(request_object)
        self.assertEquals(resp.status_code, 201)
        provider_id = resp.json()[const.NS_IDENTITY_PROVIDER][const.ID]
        self.provider_ids.append(provider_id)

        # update the approved domains list
        new_list_of_domains = [domain_id, domain_id]
        self.update_approved_domain_ids_for_idp(
            provider_id=provider_id, new_list_of_domains=new_list_of_domains)
        self.assertEqual(
            resp.json()[const.NS_IDENTITY_PROVIDER][const.APPROVED_DOMAIN_Ids],
            [domain_id])

    def test_update_idp_by_rcn_admin(self):

        request_object = factory.get_domain_request_object({})
        dom_resp = self.identity_admin_client.add_domain(request_object)
        domain_id = dom_resp.json()[const.RAX_AUTH_DOMAIN][const.ID]

        request_object = factory.get_add_idp_request_object(
            federation_type='DOMAIN', approved_domain_ids=[domain_id])
        resp = self.identity_admin_client.create_idp(request_object)
        self.assertEquals(resp.status_code, 201)
        provider_id = resp.json()[const.NS_IDENTITY_PROVIDER][const.ID]
        self.provider_ids.append(provider_id)

        option = {
            const.PARAM_ROLE_NAME: const.RCN_ADMIN_ROLE_NAME
        }
        list_resp = self.identity_admin_client.list_roles(option=option)
        rcn_admin_role_id = list_resp.json()[const.ROLES][0][const.ID]

        request_object = factory.get_add_user_one_call_request_object(
            domainid=domain_id)
        user_client = self.generate_client(
            parent_client=self.identity_admin_client,
            request_object=request_object)
        self.clients.append(user_client)
        user_id = user_client.default_headers[const.X_USER_ID]

        new_idp_name = self.generate_random_string(
            pattern=const.IDP_NAME_PATTERN)

        # update idp using user:admin
        update_idp_obj = requests.IDP(idp_name=new_idp_name)
        resp = user_client.update_idp(idp_id=provider_id,
                                      request_object=update_idp_obj)
        # Docker containers have the f-flag turned OFF. Hence, currently
        # checking for 403. But, when this is ready to be used for Staging
        # or Prod with the feature flag turned ON, we can update it to 200 or
        # as appropriate. We can keep it under if/else, but that will again
        # make a call to devops client to read the flag value. So,avoiding it.
        self.assertEqual(resp.status_code, 200)

        # add rcn:admin to the user created
        add_role_to_user_resp = self.identity_admin_client.add_role_to_user(
            user_id=user_id, role_id=rcn_admin_role_id)
        self.assertEqual(add_role_to_user_resp.status_code, 200)

        new_idp_name = self.generate_random_string(
            pattern=const.IDP_NAME_PATTERN)
        # update idp using user with role rcn:admin
        update_idp_obj = requests.IDP(idp_name=new_idp_name)
        resp = user_client.update_idp(idp_id=provider_id,
                                      request_object=update_idp_obj)
        # This may need to change once we test this in Staging/Prod w/ flag ON.
        # Because, currently, the user is user admin as well as rcn admin. It
        # should be changed to test w/ an existing RCN in Staging/Prod,
        self.assertEqual(resp.status_code, 200)

    def test_update_idp_by_user_clients(self):
        request_object = factory.get_domain_request_object({})
        dom_resp = self.identity_admin_client.add_domain(request_object)
        domain_id = dom_resp.json()[const.RAX_AUTH_DOMAIN][const.ID]

        request_object = factory.get_add_idp_request_object(
            federation_type='DOMAIN', approved_domain_ids=[domain_id])
        resp = self.identity_admin_client.create_idp(request_object)
        self.assertEquals(resp.status_code, 201)
        provider_id = resp.json()[const.NS_IDENTITY_PROVIDER][const.ID]
        self.provider_ids.append(provider_id)

        request_object = factory.get_add_user_one_call_request_object(
            domainid=domain_id)
        user_client = self.generate_client(
            parent_client=self.identity_admin_client,
            request_object=request_object)
        self.clients.append(user_client)
        self.assert_update_idp(provider_id, user_client)

    @ddt.data("user:admin", "user:manage")
    def test_update_idp_by_user_clients_metadata(self, client_key):
        (pem_encoded_cert, cert_path, _, key_path,
         f_print) = create_self_signed_cert()

        client_instance = self.user_clients[client_key]

        provider_id = self.add_idp_with_metadata_return_id(
            cert_path=cert_path, api_client=client_instance)
        self.provider_ids.append(provider_id)
        self.clients.append(client_instance)
        self.assert_update_idp(provider_id, client_instance)

    def assert_update_idp(self, provider_id, client_instance):
        new_idp_name = self.generate_random_string(
            pattern=const.IDP_NAME_PATTERN)

        # update idp with random name
        update_idp_obj = requests.IDP(idp_name=new_idp_name)
        client_instance.serialize_format = 'json'
        client_instance.default_headers[
            const.CONTENT_TYPE] = 'application/json'
        resp = client_instance.update_idp(
            idp_id=provider_id, request_object=update_idp_obj)
        self.assertEqual(resp.status_code, 200)
        get_name_resp = client_instance.get_idp(idp_id=resp.json()[
            const.NS_IDENTITY_PROVIDER][const.ID])
        get_name = get_name_resp.json()[const.NS_IDENTITY_PROVIDER][
            const.NAME]
        self.assertEquals(get_name, new_idp_name)

        client_instance.default_headers[
            const.CONTENT_TYPE] = 'application/xml'
        client_instance.serialize_format = 'xml'

    def tearDown(self):
        # Delete all providers created in the tests
        for id_ in self.provider_ids:
            self.identity_admin_client.delete_idp(idp_id=id_)
        for id_ in self.users:
            self.identity_admin_client.delete_user(user_id=id_)
        for id_ in self.domains:
            req_obj = requests.Domain(domain_name=id_, domain_id=id_,
                                      enabled=False)
            self.identity_admin_client.update_domain(
                domain_id=str(id_), request_object=req_obj)
            self.identity_admin_client.delete_domain(
                domain_id=id_)
        for client_ in self.clients:
            self.delete_client(client=client_)
        super(TestUpdateIDP, self).tearDown()

    @classmethod
    def tearDownClass(cls):
        super(TestUpdateIDP, cls).tearDownClass()
