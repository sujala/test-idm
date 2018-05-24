from tests.api import base
from tests.api import devops_client
from tests.api.v2.models import factory
from tests.api.utils import header_validation

from tests.package.johny import constants as const
from tests.package.johny.v2 import client
from tests.package.johny.v2.models import requests


class TestBaseV2(base.TestBase):

    """Child class of fixtures.BaseTestFixture for testing CDN.

    Inherit from this and write your test methods. If the child class defines
    a prepare(self) method, this method will be called before executing each
    test method.
    """

    @classmethod
    def setUpClass(cls):

        super(TestBaseV2, cls).setUpClass()

        cls.identity_admin_client = client.IdentityAPIClient(
            url=cls.url,
            serialize_format=cls.test_config.serialize_format,
            deserialize_format=cls.test_config.deserialize_format)

        req_obj = requests.AuthenticateWithPassword(
            user_name=cls.identity_config.identity_admin_user_name,
            password=cls.identity_config.identity_admin_password)
        resp = cls.identity_admin_client.get_auth_token(request_object=req_obj)
        identity_admin_auth_token = resp.json()[const.ACCESS][const.TOKEN][
            const.ID]
        identity_admin_id = resp.json()[const.ACCESS][const.USER][const.ID]
        cls.identity_admin_client.default_headers[const.X_AUTH_TOKEN] = (
            identity_admin_auth_token)
        cls.identity_admin_client.default_headers[const.X_USER_ID] = (
            identity_admin_id
        )

        if cls.test_config.run_service_admin_tests:
            cls.service_admin_client = client.IdentityAPIClient(
                url=cls.url,
                serialize_format=cls.test_config.serialize_format,
                deserialize_format=cls.test_config.deserialize_format)
            cls.service_admin_client.default_headers[const.X_AUTH_TOKEN] = (
                cls.identity_config.service_admin_auth_token)
            cls.devops_client = devops_client.IdentityDevopsClient(
                cls.devops_url,
                serialize_format=cls.test_config.serialize_format,
                deserialize_format=cls.test_config.deserialize_format)
            cls.devops_client.default_headers[const.X_AUTH_TOKEN] = (
                cls.identity_config.service_admin_auth_token)
        else:
            # use identity:admin instead
            cls.devops_client = devops_client.IdentityDevopsClient(
                cls.devops_url,
                serialize_format=cls.test_config.serialize_format,
                deserialize_format=cls.test_config.deserialize_format)
            cls.devops_client.default_headers[const.X_AUTH_TOKEN] = (
                identity_admin_auth_token)

        cls.unexpected_headers_HTTP_201 = [
            header_validation.validate_transfer_encoding_header_not_present]
        cls.unexpected_headers_HTTP_400 = [
            header_validation.validate_location_header_not_present]
        cls.unexpected_headers_HTTP_200 = [
            header_validation.validate_location_header_not_present]
        cls.header_validation_functions_HTTP_200 = (
            cls.default_header_validations +
            cls.unexpected_headers_HTTP_200)
        cls.header_validation_functions_HTTP_201 = (
            cls.default_header_validations +
            cls.unexpected_headers_HTTP_201 + [
                header_validation.validate_header_location,
                header_validation.validate_header_content_length])
        cls.header_validation_functions_HTTP_400 = (
            cls.default_header_validations +
            cls.unexpected_headers_HTTP_400)

    @classmethod
    def generate_client(cls, parent_client=None, request_object=None,
                        additional_input_data=None, token=None,
                        one_call=False):
        """Return a client object
        the object will be added few default headers for later use such as
        x-auth-token, x-user-id, x-domain-id, x-tenant-id
        :param parent_client: client object of the parent user. The parent user
        type can be identity_admin or user_admin. If no parent_client is given,
        uses the service_admin (or) identity_admin level user's client
        depending on whether run_service_admin_tests in the config file is
        True or False respectively.
        :param request_object: is a create user request object which give users
        flexibility to build object outside to create user as they desire
        :param additional_input_data: any additional input need to add to user
        eg. {'domain_id': '123456', 'is_user_manager': True}
        :param token: a token user passing in to generate client in this case
        regardless additional_input_data
        """
        id_client = client.IdentityAPIClient(
            url=cls.url,
            serialize_format=cls.test_config.serialize_format,
            deserialize_format=cls.test_config.deserialize_format)

        if token:
            # generate client from existing user token
            id_client.default_headers[const.X_AUTH_TOKEN] = token
        else:
            password = None
            # create user as client
            if not parent_client:
                if cls.test_config.run_service_admin_tests:
                    parent_client = cls.service_admin_client
                else:
                    parent_client = cls.identity_admin_client
            if not request_object:
                user_name = (additional_input_data.get(
                    'user_name',
                    cls.generate_random_string(
                        pattern=const.USER_NAME_PATTERN)))
                password = additional_input_data.get('password', None)
                input_data = {
                    'domain_id': additional_input_data.get('domain_id', None),
                    'email': additional_input_data.get('email', None),
                    'contact_id': additional_input_data.get(
                        'contact_id', None),
                    'password': password,
                    'roles': additional_input_data.get('roles', None)
                }

                if one_call:
                    # must be provided with domain_id
                    request_object = \
                        factory.get_add_user_one_call_request_object(
                            username=user_name,
                            domainid=input_data['domain_id'])
                else:
                    request_object = factory.get_add_user_request_object(
                        username=user_name, input_data=input_data)

            user_resp = parent_client.add_user(request_object=request_object)
            assert user_resp.status_code == 201, (
                'User with Name {0} failed to create'.format(
                    user_name))
            user_id = user_resp.json()[const.USER][const.ID]

            if additional_input_data:
                if ('is_user_manager' in additional_input_data and
                        additional_input_data['is_user_manager'] is True):
                    parent_client.add_role_to_user(
                        user_id=user_id, role_id=const.USER_MANAGER_ROLE_ID)

            username = user_resp.json()[const.USER][const.USERNAME]
            if not password:
                password = user_resp.json()[const.USER][const.NS_PASSWORD]
            if const.DOMAIN_ID in str(user_resp.json()[const.USER]):
                id_client.default_headers[const.X_DOMAIN_ID] = (
                    user_resp.json()[const.USER][const.RAX_AUTH_DOMAIN_ID])

            req_obj = requests.AuthenticateWithPassword(
                user_name=username, password=password
            )
            resp = id_client.get_auth_token(request_object=req_obj)
            auth_token = resp.json()[const.ACCESS][const.TOKEN][const.ID]
            user_id = resp.json()[const.ACCESS][const.USER][const.ID]
            id_client.default_headers[const.X_USER_ID] = user_id
            id_client.default_headers[const.X_AUTH_TOKEN] = auth_token

        return id_client

    @classmethod
    def generate_racker_client(cls, request_object=None, token=None):

        racker_client = client.IdentityAPIClient(
            url=cls.internal_url,
            serialize_format=cls.test_config.serialize_format,
            deserialize_format=cls.test_config.deserialize_format)

        if token:
            # generate client from existing user token
            racker_client.default_headers[const.X_AUTH_TOKEN] = token
        else:
            if not request_object:
                request_object = requests.AuthenticateAsRackerWithPassword(
                    racker_name=cls.identity_config.racker_username,
                    racker_password=cls.identity_config.racker_password)

            resp = racker_client.get_auth_token(request_object=request_object)
            auth_token = resp.json()[const.ACCESS][const.TOKEN][const.ID]
            user_id = resp.json()[const.ACCESS][const.USER][const.ID]
            racker_client.default_headers[const.X_USER_ID] = user_id
            racker_client.default_headers[const.X_AUTH_TOKEN] = auth_token

        return racker_client

    def generate_client_with_x_auth_token(cls, x_auth_token=None):
        """
        Return a client object using x-auth-token. So the call being made
        with the client generated by this method will be a call made by the
        user to whom the token belongs
        """
        # TODO: deprecate
        id_client = client.IdentityAPIClient(
            url=cls.url,
            serialize_format=cls.test_config.serialize_format,
            deserialize_format=cls.test_config.deserialize_format)

        id_client.default_headers['X-Auth-Token'] = x_auth_token
        return id_client

    @classmethod
    @base.log_tearDown_error
    def delete_client(cls, client, parent_client=None):
        """Return delete client object
        Clean up all resources create if generated client added user.
        :param parent_client: client object of the parent user. The parent user
        type can be identity_admin or user_admin. If no parent_client is given,
        uses the service_admin (or) identity_admin level user's client
        depending on whether run_service_admin_tests in the config file is
        True or False respectively.
        :param client: client whose resources are to be deleted
        """
        if not parent_client:
            if cls.test_config.run_service_admin_tests:
                parent_client = cls.service_admin_client
            else:
                parent_client = cls.identity_admin_client
        if const.X_USER_ID in client.default_headers:
            user_id = client.default_headers[const.X_USER_ID]
            resp = parent_client.delete_user(user_id=user_id)
            assert resp.status_code == 204, (
                'User with ID {0} failed to delete'.format(
                    user_id))
        if const.X_DOMAIN_ID in client.default_headers:
            domain_id = client.default_headers[const.X_DOMAIN_ID]

            # Deleting the tenants
            list_resp = cls.identity_admin_client.list_tenants_in_domain(
                domain_id=domain_id)

            if list_resp.status_code == 200:
                tenants = list_resp.json()[const.TENANTS]

                tenant_ids = [item[const.ID] for item in tenants]
                for tenant_id in tenant_ids:
                    resp = cls.identity_admin_client.delete_tenant(
                        tenant_id=tenant_id)
                    assert resp.status_code == 204, (
                        'Tenant with ID {0} failed to delete'.format(
                            tenant_id))

            # Disable domain before delete
            disable_domain_req = requests.Domain(enabled=False)
            parent_client.update_domain(
                domain_id=domain_id, request_object=disable_domain_req)
            resp = parent_client.delete_domain(
                domain_id=domain_id)
            assert resp.status_code == 204, (
                'Domain with ID {0} failed to delete'.format(
                    domain_id))

    @classmethod
    def tearDownClass(cls):
        """Deletes the added resources."""
        super(TestBaseV2, cls).tearDownClass()
