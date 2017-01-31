import ddt
import copy
from tests.api.v2 import base
from tests.api.v2.schema import tenants
from tests.api.v2.models import factory
from tests.api.v2.models import responses

from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


@ddt.ddt
class EndpointMappingsInAuthAndListEndpoints(base.TestBaseV2):
    """
    EndpointMappingsInAuthAndListEndpoints test class
    """
    def setUp(self):
        super(EndpointMappingsInAuthAndListEndpoints, self).setUp()
        if (not self.test_config.run_service_admin_tests or not
                self.is_endpoints_based_on_rules_enabled()):
            self.skipTest('Skipping test because no serviceAdminClient or'
                          ' feature.include.endpoints.based.on.rules is off')
        self.common_input = {
            'public_url': 'https://www.test_public_endpoint_special.com',
            'internal_url': 'https://www.test_internal_endpoint_special.com',
            'admin_url': 'https://www.test_admin_endpoint_special.com',
            'version_info': 'test_version_special_info',
            'version_id': '1',
            'version_list': 'test_version_special_list',
            'region': 'ORD'}
        self.tenant_ids = []
        self.role_ids = []
        self.user_ids = []
        self.service_ids = []
        self.template_ids = []
        self.map_rule_ids = []
        self.tenant_description = 'A tenant described'
        self.tenant_display_name = 'A name displayed'
        self.add_tenant_schema = tenants.add_tenant
        self.add_tenant_with_types_schema = copy.deepcopy(tenants.add_tenant)
        (self.add_tenant_with_types_schema['properties'][const.TENANT]
            ['properties'].update(
                {const.NS_TYPES: {'type': 'array'}}))
        (self.add_tenant_with_types_schema['properties'][const.TENANT]
            ['required'].append(const.NS_TYPES))

    def is_endpoints_based_on_rules_enabled(self):
        """
        Returns the value of feature.include.endpoints.based.on.rules
        which should be boolean indicating if it's on or off
        """
        resp = self.devops_client.get_devops_properties(
            prop_name=const.FEATURE_FLAG_FOR_ENDPOINTS_BASED_ON_RULES)
        self.assertEqual(resp.status_code, 200)
        return resp.json()[const.PROPERTIES][0][const.VALUE]

    def create_endpoint_template(self, endpoint_attributes):
        """
        Creates a new endpoint template
        returns the template id and service name used in creation
        """
        template_id = self.generate_random_string(
            pattern=const.NUMERIC_DOMAIN_ID_PATTERN)
        service_name = self.generate_random_string(
            pattern=const.SERVICE_NAME_PATTERN)
        service_type = self.generate_random_string(
            pattern=const.SERVICE_TYPE_PATTERN)
        service_id = self.generate_random_string(pattern='[\d]{8}')
        service_description = 'SERVICEMETEST'

        request_object = requests.ServiceAdd(
            service_id=service_id, service_name=service_name,
            service_type=service_type,
            service_description=service_description)

        resp = self.service_admin_client.add_service(
            request_object=request_object)
        self.assertEqual(resp.status_code, 201)
        service = responses.Service(resp.json())
        service_id = service.id
        self.service_ids.append(service_id)

        endpoint_template = requests.EndpointTemplateAdd(
            template_id=template_id, name=service_name,
            template_type=service_type, **endpoint_attributes)
        resp = self.identity_admin_client.add_endpoint_template(
            endpoint_template)
        self.assertEqual(resp.status_code, 201)
        template_id = resp.json()[const.OS_KSCATALOG_ENDPOINT_TEMPLATE][
                                  const.ID]
        self.template_ids.append(template_id)

        return template_id, service_name

    def add_role_to_user_on_tenant_and_create_mapping_rule(self, user_id):
        """
        Add a new mapping rule with new templates_ids, endpoint attributes
        and role
        """
        # Create new endpoint templates
        test_data = self.create_random_endpoint_templates(5)
        # Create a role
        new_role_id = self.create_role()
        # Create Tenant with types
        resp = self.create_tenant_with_types()
        tenant_id = resp.json()[const.TENANT][const.ID]
        tenant_name = resp.json()[const.TENANT][const.NAME]
        tenant_types = resp.json()[const.TENANT][const.NS_TYPES]
        # Create Mapping rule for tenantType to Endpoint template
        self.create_mapping_rule_for_tenant_type_and_template_ids(
            tenant_type=tenant_types[0],
            template_ids=[tupleset[0] for tupleset in test_data])
        # Add role to user for tenant
        resp = self.service_admin_client.add_role_to_user_for_tenant(
                   tenant_id=tenant_id, role_id=new_role_id,
                   user_id=user_id)
        self.assertEqual(resp.status_code, 200)
        return {'tenant_id': tenant_id, 'tenant_name': tenant_name,
                'role_id': new_role_id, 'test_data': test_data}

    def create_random_endpoint_templates(self, count=1):
        """
        This will create <count> tuples of template_ids and service name
        The endpoint attributes will be somewhat randomized based on the
        original self.common_input
        """
        test_data = []
        for q in range(count):
            new_data = copy.deepcopy(self.common_input)
            rand_string = self.generate_random_string(
                pattern=const.LOWER_CASE_LETTERS)
            for key in new_data.keys():
                new_data[key] = new_data[key].replace(
                    "special", rand_string)
            new_data[const.REGION] = rand_string[:3].upper()
            new_data['version_id'] = str(q+2)
            template_id, service_name = self.create_endpoint_template(new_data)
            test_data.append((template_id, service_name, new_data))

        return test_data

    def create_tenant_with_types(self):
        """
        Create a tenant object with 3 random types assigned to it
        returns the tenant_id
        """
        tenant_types = []
        for q in range(3):
            tenant_types.append(self.generate_random_string(
                                pattern=const.TENANT_TYPE_PATTERN).upper())
        tenant_id = self.generate_random_string(
            pattern=const.TENANT_ID_PATTERN)
        tenant_name = self.generate_random_string(
            pattern=const.TENANT_NAME_PATTERN)
        request_object = requests.Tenant(
            tenant_name=tenant_name,
            description=self.tenant_description,
            tenant_id=tenant_id,
            enabled=True,
            tenant_types=tenant_types,
            display_name=self.tenant_display_name)
        resp = self.identity_admin_client.add_tenant(tenant=request_object)
        self.assertEqual(resp.status_code, 201)
        self.tenant_ids.append(tenant_id)
        self.assertSchema(response=resp,
                          json_schema=self.add_tenant_with_types_schema)
        self.assertEqual(len(
            resp.json()[const.TENANT][const.NS_TYPES]), 3)
        return resp

    def verify_endpoint_attributes(self, service_catalog, service_name,
                                   template_data, tenant_id):
        """
        Given a service_catalog structure, it will verify that the
        expected endpoints are found with the correct attribute values.
        """
        msg = "for {key}, {value1} not equal to {value2}"
        endpoints = ([service[const.ENDPOINTS] for service in service_catalog
                     if service[const.NAME] == service_name])
        if not len(endpoints):
            raise Exception("Endpoint with name {service_name} not found".
                            format(service_name=service_name))
        endpoint = endpoints[0][0]

        template_data = copy.deepcopy(template_data)

        template_data['public_url'] += '/{tenant_id}'.format(
            tenant_id=tenant_id)
        template_data['internal_url'] += '/{tenant_id}'.format(
            tenant_id=tenant_id)

        for key_value_set in ([(const.TENANT_ID,
                                endpoint[const.TENANT_ID],
                                tenant_id),
                               (const.PUBLIC_URL,
                                endpoint[const.PUBLIC_URL],
                                template_data['public_url']),
                               (const.INTERNAL_URL,
                                endpoint[const.INTERNAL_URL],
                                template_data['internal_url']),
                               (const.VERSION_INFO,
                                endpoint[const.VERSION_INFO],
                                template_data['version_info']),
                               (const.VERSION_ID,
                                endpoint[const.VERSION_ID],
                                template_data['version_id']),
                               (const.VERSION_LIST,
                                endpoint[const.VERSION_LIST],
                                template_data['version_list']),
                               (const.REGION,
                                endpoint[const.REGION],
                                template_data['region'])]):
            self.assertEqual(key_value_set[1], key_value_set[2],
                             msg=msg
                             .format(key=key_value_set[0],
                                     value1=key_value_set[1],
                                     value2=key_value_set[2]))

    def create_role(self):
        """
        Create a role, returns new role id
        """
        new_role_name = self.generate_random_string(
            pattern=const.ROLE_NAME_PATTERN)
        req_obj = factory.get_add_role_request_object(
            role_name=new_role_name,
            administrator_role="identity:user-manage")
        resp = self.identity_admin_client.add_role(request_object=req_obj)
        self.assertEqual(resp.status_code, 201)
        new_role_id = resp.json()[const.ROLE][const.ID]
        self.role_ids.append(new_role_id)
        return new_role_id

    def create_mapping_rule_for_tenant_type_and_template_ids(self,
                                                             tenant_type,
                                                             template_ids):
        """
        Create Mapping rule for tenantType to Endpoint template
        returns new map rule id
        """
        request_object = requests.TenantTypeToEndpointMappingRule(
            tenant_type=tenant_type,
            description='MapRule for ' + self.tenant_description,
            endpoint_ids=template_ids)
        resp = (self.identity_admin_client
                .add_tenant_type_to_endpoint_mapping_rule(
                    request_object=request_object))
        self.assertEqual(resp.status_code, 201)
        map_rule_id = responses.TenantTypeToEndpointMappingRule(resp.json()).id
        self.map_rule_ids.append(map_rule_id)
        return map_rule_id

    def create_user_with_endpoint_from_mapping_rules(self):
        """
        This will create the base case for these tests, a user
        that has a service with endpoint attributes in its serviceCatalog
        because of a mapping rule created on one of the tenantTypes of
        the tenantID this user has
        Returns a dictionary of useful information about the account
        and the objects created for the acct to use.
        """
        # Create user with tenantID
        uadm_username = self.generate_random_string(
            pattern=const.USER_ADMIN_PATTERN)
        uadm_domain_id = self.generate_random_string(
            pattern=const.NUMERIC_DOMAIN_ID_PATTERN)
        input_data = {'email': const.EMAIL_RANDOM,
                      'secret_qa':
                          {const.SECRET_QUESTION: const.SECRET_QUESTION,
                           const.SECRET_ANSWER: const.SECRET_ANSWER},
                      'domain_id': uadm_domain_id}
        req_obj = requests.UserAdd(user_name=uadm_username, **input_data)
        resp = self.identity_admin_client.add_user(request_object=req_obj)
        self.assertEqual(resp.status_code, 201)
        new_user_id = resp.json()[const.USER][const.ID]
        self.user_ids.append(new_user_id)
        uadm_password = resp.json()[const.USER][const.NS_PASSWORD]

        # Create endpoint template
        template_id, service_name = self.create_endpoint_template(
            self.common_input)
        # Create role
        new_role_id = self.create_role()
        # Create Tenant with types
        resp = self.create_tenant_with_types()
        tenant_id = resp.json()[const.TENANT][const.ID]
        tenant_name = resp.json()[const.TENANT][const.NAME]
        tenant_types = resp.json()[const.TENANT][const.NS_TYPES]
        # Create Mapping rule for tenantType to Endpoint template
        self.create_mapping_rule_for_tenant_type_and_template_ids(
            tenant_type=tenant_types[0], template_ids=[template_id])
        # Add role to user for tenant
        resp = self.service_admin_client.add_role_to_user_for_tenant(
                   tenant_id=tenant_id, role_id=new_role_id,
                   user_id=new_user_id)
        self.assertEqual(resp.status_code, 200)

        return {'user_name': uadm_username, 'password': uadm_password,
                'tenant_name': tenant_name, 'user_id': new_user_id,
                'service_name': service_name, 'template_id': template_id,
                'tenant_types': tenant_types, 'role_id': new_role_id,
                'tenant_id': tenant_id}

    def auth_with_userpass_response(self, acct_info):
        """
        Login with user and pass
        returns response object
        """
        req_obj = requests.AuthenticateWithPassword(
            user_name=acct_info['user_name'],
            password=acct_info['password']
        )
        resp = self.identity_admin_client.get_auth_token(
            request_object=req_obj)
        self.assertEqual(resp.status_code, 200)
        return resp

    def auth_with_api_key(self, acct_info):
        """
        Login with  user & apikey
        returns response object
        """
        resp = self.identity_admin_client.get_api_key(acct_info['user_id'])
        self.assertEqual(resp.status_code, 200)
        api_key = (resp.json()[const.NS_API_KEY_CREDENTIALS]
                   [const.API_KEY])
        req_obj = requests.AuthenticateWithApiKey(
            user_name=acct_info['user_name'],
            api_key=api_key
        )
        resp = self.identity_admin_client.get_auth_token(
            request_object=req_obj)
        self.assertEqual(resp.status_code, 200)
        return resp

    def auth_with_tenant_user_pass(self, acct_info, use_tenant_name=False):
        """
        get response for tenant user pass
        """
        key = use_tenant_name and 'tenant_name' or 'tenant_id'

        kwargs = {'user_name': acct_info['user_name'],
                  'password': acct_info['password'],
                  key: acct_info[key]}
        request_object = requests.AuthenticateWithPassword(**kwargs)
        resp = self.identity_admin_client.get_auth_token(
            request_object=request_object)
        self.assertEqual(resp.status_code, 200)
        return resp

    def auth_with_tenant_user_api_key(self, acct_info, use_tenant_name=False):
        """
        get response for tenant user api key
        """
        key = use_tenant_name and 'tenant_name' or 'tenant_id'

        resp = self.identity_admin_client.get_api_key(acct_info['user_id'])
        self.assertEqual(resp.status_code, 200)
        api_key = (resp.json()[const.NS_API_KEY_CREDENTIALS]
                   [const.API_KEY])
        kwargs = {'user_name': acct_info['user_name'],
                  'api_key': api_key,
                  key: acct_info[key]}
        request_object = requests.AuthenticateWithApiKey(**kwargs)
        resp = self.identity_admin_client.get_auth_token(
            request_object=request_object)
        self.assertEqual(resp.status_code, 200)
        return resp

    def auth_with_tenant_token(self, acct_info, use_tenant_name=False):
        """
        get response for auth with tenant token
        """
        key = use_tenant_name and 'tenant_name' or 'tenant_id'
        req_obj = requests.AuthenticateWithPassword(
            user_name=acct_info['user_name'],
            password=acct_info['password']
        )
        resp = self.identity_admin_client.get_auth_token(
            request_object=req_obj)
        self.assertEqual(resp.status_code, 200)

        token = resp.json()[const.ACCESS][const.TOKEN][const.ID]
        kwargs = {'token_id': token, key: acct_info[key]}

        request_object = requests.AuthenticateAsTenantWithToken(**kwargs)
        resp = self.identity_admin_client.get_auth_token(
            request_object=request_object)
        self.assertEqual(resp.status_code, 200)
        return resp

    def verify_endpoint_attributes_present_in_all_auths(self, acct_info,
                                                        service_name,
                                                        template_data,
                                                        tenant_id):

                            # Auth user pass
        for action_tuple in [(self.auth_with_userpass_response, []),
                             # Auth user, apikey
                             (self.auth_with_api_key, []),
                             # Auth tenantId, user, pass
                             (self.auth_with_tenant_user_pass, []),
                             # Auth tenantId, user, apikey
                             (self.auth_with_tenant_user_api_key, []),
                             # Auth tenantId, token
                             (self.auth_with_tenant_token, []),
                             # Auth tenantName, user, pass
                             (self.auth_with_tenant_user_pass, [True]),
                             # Auth tenantName, user, apikey
                             (self.auth_with_tenant_user_api_key, [True]),
                             # Auth tenantName, token
                             (self.auth_with_tenant_token, [True])]:
            resp = action_tuple[0](acct_info, *action_tuple[1])
            service_catalog = resp.json()[const.ACCESS][const.SERVICE_CATALOG]
            self.verify_endpoint_attributes(service_catalog=service_catalog,
                                            service_name=service_name,
                                            template_data=template_data,
                                            tenant_id=tenant_id)

    def verify_endpoint_attributes_present_in_list_endpoints_for_token(
            self, token, template_data, service_name, tenant_id):
        """
        Verify list_endpoints_for_token has the expected endpoint attributes
        """
        resp = self.identity_admin_client.list_endpoints_for_token(token)
        self.assertEqual(resp.status_code, 200)
        target_endpoint = None
        for endpoint in resp.json()[const.ENDPOINTS]:
            if endpoint[const.NAME] == service_name:
                target_endpoint = endpoint
                break
        self.assertIsNotNone(target_endpoint,
                             msg="{service_name} not found"
                             .format(service_name=service_name))
        # Making a structure that fits serviceCatalog
        service_catalog_structure = (
            [{const.NAME: service_name,
             const.ENDPOINTS: [target_endpoint]}])
        # Verify endpoint attributes found in the endpoints listing
        self.verify_endpoint_attributes(
            service_catalog=service_catalog_structure,
            service_name=service_name, template_data=template_data,
            tenant_id=tenant_id)
        # check admin_url seperately
        msg = "{key} don't match enough, {value1} != {value2}"
        self.assertTrue(endpoint[const.ADMIN_URL].startswith(
            template_data['admin_url']),
            msg=msg.format(key=const.ADMIN_URL,
                           value1=endpoint[const.ADMIN_URL],
                           value2=template_data['admin_url']))

    def test_verify_endpoint_attributes_present_in_all_auths(self):
        """
        Verify all auths return end points mapped by rules
        """
        acct_info = self.create_user_with_endpoint_from_mapping_rules()

        self.verify_endpoint_attributes_present_in_all_auths(
            acct_info=acct_info, service_name=acct_info['service_name'],
            template_data=self.common_input,
            tenant_id=acct_info['tenant_name'])

    def test_verify_adding_endpoint_attributes_to_existing_tenant_type(self):
        """
        Add another mapping rule to a tenant_type that already exists for
        tenantId of a role already on the user.
        """
        acct_info = self.create_user_with_endpoint_from_mapping_rules()

        # Copy common_input data and change stuff in it
        new_data = copy.deepcopy(self.common_input)
        rand_string = self.generate_random_string(
            pattern=const.LOWER_CASE_LETTERS)
        for key in new_data.keys():
            new_data[key] = new_data[key].replace("special", rand_string)
        new_data[const.REGION] = rand_string[:3].upper()
        new_data['version_id'] = '2'
        # create a new endpoint template with this different test_data
        template_id, service_name = self.create_endpoint_template(new_data)
        # Create mapping rule for one of the existing tenant_types
        # already on the user
        self.create_mapping_rule_for_tenant_type_and_template_ids(
            tenant_type=acct_info['tenant_types'][1],
            template_ids=[template_id])
        # verify the new endpoint attributes are present
        self.verify_endpoint_attributes_present_in_all_auths(
            acct_info=acct_info, service_name=service_name,
            template_data=new_data, tenant_id=acct_info['tenant_name'])
        # verify the original endpoint attributes are present as well
        self.verify_endpoint_attributes_present_in_all_auths(
            acct_info=acct_info, service_name=acct_info['service_name'],
            template_data=self.common_input,
            tenant_id=acct_info['tenant_name'])

    def test_verify_remove_endpoint_attributes_from_existing_tenant_type(self):
        """
        remove mapping rule from a tenant_type that already exists for
        tenantId of a role already on the user.
        """
        acct_info = self.create_user_with_endpoint_from_mapping_rules()

        # Copy common_input data and change stuff in it
        new_data = copy.deepcopy(self.common_input)
        rand_string = self.generate_random_string(
            pattern=const.LOWER_CASE_LETTERS)
        for key in new_data.keys():
            new_data[key] = new_data[key].replace("special", rand_string)
        new_data[const.REGION] = rand_string[:3].upper()
        new_data['version_id'] = '2'
        # create a new mapping template with this different test_data
        template_id, service_name = self.create_endpoint_template(new_data)

        # Create mapping rule for one of the existing tenant_types
        # already on the user
        rule_id = self.create_mapping_rule_for_tenant_type_and_template_ids(
            tenant_type=acct_info['tenant_types'][1],
            template_ids=[template_id])

        # verify the new endnpoint attributes are present
        self.verify_endpoint_attributes_present_in_all_auths(
            acct_info=acct_info, service_name=service_name,
            template_data=new_data, tenant_id=acct_info['tenant_name'])

        # verify the original endpoint attributes are present as well
        self.verify_endpoint_attributes_present_in_all_auths(
            acct_info=acct_info, service_name=acct_info['service_name'],
            template_data=self.common_input,
            tenant_id=acct_info['tenant_name'])

        # Delete mapping rule id
        (self.identity_admin_client
         .delete_tenant_type_to_endpoint_mapping_rule(rule_id=rule_id))

        # Verify endpoint attributes are now ***GONE***!
        verification_error = None
        try:
            self.verify_endpoint_attributes_present_in_all_auths(
               acct_info=acct_info, service_name=service_name,
               template_data=new_data,
               tenant_id=acct_info['tenant_name'])
        except Exception, e:
            verification_error = str(e)
        self.assertEqual(verification_error,
                         "Endpoint with name {service_name} not found".
                         format(service_name=service_name),
                         msg="The endpoints were not deleted!")
        # verify the original endpoint attributes are still there
        self.verify_endpoint_attributes_present_in_all_auths(
            acct_info=acct_info, service_name=acct_info['service_name'],
            template_data=self.common_input,
            tenant_id=acct_info['tenant_name'])

    def test_list_endpoints_for_token(self):
        """
        Verify that listing endpoints for a token contains the endpoints
        that were created and added to this user's tenant
        """
        acct_info = self.create_user_with_endpoint_from_mapping_rules()
        req_obj = requests.AuthenticateWithPassword(
            user_name=acct_info['user_name'],
            password=acct_info['password']
        )
        resp = self.identity_admin_client.get_auth_token(
            request_object=req_obj)
        self.assertEqual(resp.status_code, 200)

        token = resp.json()[const.ACCESS][const.TOKEN][const.ID]
        # Verify the endpoint attributes are there
        self.verify_endpoint_attributes_present_in_list_endpoints_for_token(
            token=token, template_data=self.common_input,
            service_name=acct_info['service_name'],
            tenant_id=acct_info['tenant_name'])

    def test_list_endpoints_for_token_after_adding_mapping_rule(self):
        """
        Verify that listing endpoints for a token contains the endpoints
        that were created and added to this user's tenant via mapping rule
        """
        acct_info = self.create_user_with_endpoint_from_mapping_rules()
        req_obj = requests.AuthenticateWithPassword(
            user_name=acct_info['user_name'],
            password=acct_info['password']
        )
        resp = self.identity_admin_client.get_auth_token(
            request_object=req_obj)
        self.assertEqual(resp.status_code, 200)

        token = resp.json()[const.ACCESS][const.TOKEN][const.ID]

        # Verify the endpoint attributes there
        self.verify_endpoint_attributes_present_in_list_endpoints_for_token(
            token=token, template_data=self.common_input,
            service_name=acct_info['service_name'],
            tenant_id=acct_info['tenant_name'])
        # Copy common_input data and change stuff in it
        new_data = copy.deepcopy(self.common_input)
        rand_string = self.generate_random_string(
            pattern=const.LOWER_CASE_LETTERS)
        for key in new_data.keys():
            new_data[key] = new_data[key].replace("special", rand_string)
        new_data[const.REGION] = rand_string[:3].upper()
        new_data['version_id'] = '2'
        # create a new mapping template with this different test_data
        template_id, service_name = self.create_endpoint_template(new_data)
        # Create mapping rule for one of the existing tenant_types
        # already on the user
        self.create_mapping_rule_for_tenant_type_and_template_ids(
            tenant_type=acct_info['tenant_types'][1],
            template_ids=[template_id])
        # Verify the new endpoint attributes are in list_endpoints_for_token
        self.verify_endpoint_attributes_present_in_list_endpoints_for_token(
            token=token, template_data=new_data,
            service_name=service_name,
            tenant_id=acct_info['tenant_name'])
        # Verify the original endpoint attributes are still there
        self.verify_endpoint_attributes_present_in_list_endpoints_for_token(
            token=token, template_data=self.common_input,
            service_name=acct_info['service_name'],
            tenant_id=acct_info['tenant_name'])

    def test_list_endpoints_for_token_after_add_multi_template_mapping_rule(
            self):
        """
        Verify that listing endpoints for a token contains the endpoints
        that were created and added to this user's tenant by a rule
        that contains multiple template ids
        """
        acct_info = self.create_user_with_endpoint_from_mapping_rules()
        req_obj = requests.AuthenticateWithPassword(
            user_name=acct_info['user_name'],
            password=acct_info['password']
        )
        resp = self.identity_admin_client.get_auth_token(
            request_object=req_obj)
        self.assertEqual(resp.status_code, 200)

        token = resp.json()[const.ACCESS][const.TOKEN][const.ID]

        # Verify the endpoint attributes are there
        self.verify_endpoint_attributes_present_in_list_endpoints_for_token(
            token=token, template_data=self.common_input,
            service_name=acct_info['service_name'],
            tenant_id=acct_info['tenant_name'])
        # Create multiple templates & services
        test_data = self.create_random_endpoint_templates(5)
        # Create mapping rule for one of the existing tenant_types
        # already on the user, using all the template ids created above
        self.create_mapping_rule_for_tenant_type_and_template_ids(
            tenant_type=acct_info['tenant_types'][1],
            template_ids=[tupleset[0] for tupleset in test_data])
        # verify the new endpoint attributes are present for all the
        # test_data tuples
        for tupleset in test_data:
            (self.
             verify_endpoint_attributes_present_in_list_endpoints_for_token(
                token=token,
                service_name=tupleset[1],
                template_data=tupleset[2],
                tenant_id=acct_info['tenant_name']))
        # Verify the original endpoint attributes are still there
        self.verify_endpoint_attributes_present_in_list_endpoints_for_token(
            token=token, template_data=self.common_input,
            service_name=acct_info['service_name'],
            tenant_id=acct_info['tenant_name'])

    def test_endpoints_in_list_endpoints_for_token_after_adding_deleting_roles(
            self):
        """
        Verify endpoint attributes disappear from response after
        a 2nd role that added them is deleted.
        """
        acct_info = self.create_user_with_endpoint_from_mapping_rules()
        req_obj = requests.AuthenticateWithPassword(
            user_name=acct_info['user_name'],
            password=acct_info['password']
        )
        resp = self.identity_admin_client.get_auth_token(
            request_object=req_obj)
        self.assertEqual(resp.status_code, 200)

        token = resp.json()[const.ACCESS][const.TOKEN][const.ID]

        meta_data = self.add_role_to_user_on_tenant_and_create_mapping_rule(
            acct_info['user_id'])

        verify_endpoint_attrs_in_list_endpts4token = (
            self.verify_endpoint_attributes_present_in_list_endpoints_for_token
            )

        # Verify new endpoint attributes are found
        for tupleset in meta_data['test_data']:
            verify_endpoint_attrs_in_list_endpts4token(
                token=token, template_data=tupleset[2],
                service_name=tupleset[1], tenant_id=meta_data['tenant_id'])
        # Verify the original endpoint attributes are still there
        self.verify_endpoint_attributes_present_in_list_endpoints_for_token(
            token=token, template_data=self.common_input,
            service_name=acct_info['service_name'],
            tenant_id=acct_info['tenant_name'])
        # Delete role
        resp = self.identity_admin_client.delete_role(
            role_id=meta_data['role_id'])
        self.assertEqual(resp.status_code, 204)
        # Verify endpoint attributes are ***GONE***
        for tupleset in meta_data['test_data']:
            verification_error = None
            try:
                verify_endpoint_attrs_in_list_endpts4token(
                    token=token, template_data=tupleset[2],
                    service_name=tupleset[1],
                    tenant_id=meta_data['tenant_id'])
            except Exception, e:
                verification_error = str(e)
            self.assertEqual(verification_error,
                             "{service_name} not found".
                             format(service_name=tupleset[1]),
                             msg="The endpoints were not deleted!")
        # Verify the original endpoint attributes are still there
        self.verify_endpoint_attributes_present_in_list_endpoints_for_token(
            token=token, template_data=self.common_input,
            service_name=acct_info['service_name'],
            tenant_id=acct_info['tenant_name'])

    def test_endpoints_in_list_endpoints_for_token_after_deleting_rule(self):
        """
        Verify endpoint attributes disappear from response after
        the mapping rule that made them appear, is deleted
        """
        acct_info = self.create_user_with_endpoint_from_mapping_rules()
        req_obj = requests.AuthenticateWithPassword(
            user_name=acct_info['user_name'],
            password=acct_info['password']
        )
        resp = self.identity_admin_client.get_auth_token(
            request_object=req_obj)
        self.assertEqual(resp.status_code, 200)

        token = resp.json()[const.ACCESS][const.TOKEN][const.ID]

        # Copy common_input data and change stuff in it
        new_data = copy.deepcopy(self.common_input)
        rand_string = self.generate_random_string(
            pattern=const.LOWER_CASE_LETTERS)
        for key in new_data.keys():
            new_data[key] = new_data[key].replace("special", rand_string)
        new_data[const.REGION] = rand_string[:3].upper()
        new_data['version_id'] = '2'
        # create a new mapping template with this different test_data
        template_id, service_name = self.create_endpoint_template(new_data)
        # Create mapping rule for one of the existing tenant_types
        # already on the user
        rule_id = self.create_mapping_rule_for_tenant_type_and_template_ids(
            tenant_type=acct_info['tenant_types'][1],
            template_ids=[template_id])
        # Verify the new endpoint attributes are in list_endpoints_for_token
        self.verify_endpoint_attributes_present_in_list_endpoints_for_token(
            token=token, template_data=new_data,
            service_name=service_name,
            tenant_id=acct_info['tenant_name'])
        # Verify the original endpoint attributes are still there
        self.verify_endpoint_attributes_present_in_list_endpoints_for_token(
            token=token, template_data=self.common_input,
            service_name=acct_info['service_name'],
            tenant_id=acct_info['tenant_name'])
        # Delete mapping rule id
        (self.identity_admin_client
         .delete_tenant_type_to_endpoint_mapping_rule(rule_id=rule_id))
        # Verify the new endpoint attributes are ***GONE***!
        verification_error = None
        try:
            (self.
                verify_endpoint_attributes_present_in_list_endpoints_for_token(
                    token=token, template_data=new_data,
                    service_name=service_name,
                    tenant_id=acct_info['tenant_name']))
        except Exception, e:
            verification_error = str(e)
        self.assertEqual(verification_error,
                         "{service_name} not found".
                         format(service_name=service_name),
                         msg="The endpoints were not deleted!")
        # Verify the original endpoint attributes are still there
        self.verify_endpoint_attributes_present_in_list_endpoints_for_token(
            token=token, template_data=self.common_input,
            service_name=acct_info['service_name'],
            tenant_id=acct_info['tenant_name'])

    def test_verify_auths_after_adding_deleting_role(self):
        """
        Add another role that has a new tenant_id with new types and a new
        mapping rule to one of those types with new templates
        """
        acct_info = self.create_user_with_endpoint_from_mapping_rules()
        original_tenant_id = acct_info['tenant_id']

        meta_data = self.add_role_to_user_on_tenant_and_create_mapping_rule(
            acct_info['user_id'])

        # Verify the endpoint attributes exist for mapping rule, for this
        # newer tenant_id connected to this newer role.
        acct_info['tenant_id'] = meta_data['tenant_name']
        acct_info['tenant_name'] = meta_data['tenant_name']
        for tupleset in meta_data['test_data']:
            self.verify_endpoint_attributes_present_in_all_auths(
                acct_info=acct_info,
                service_name=tupleset[1],
                template_data=tupleset[2],
                tenant_id=meta_data['tenant_name'])
        # verify the original endpoint attributes are present as well
        acct_info['tenant_id'] = acct_info['tenant_name'] = original_tenant_id
        self.verify_endpoint_attributes_present_in_all_auths(
            acct_info=acct_info, service_name=acct_info['service_name'],
            template_data=self.common_input,
            tenant_id=acct_info['tenant_name'])
        # Delete role
        resp = self.identity_admin_client.delete_role(
            role_id=meta_data['role_id'])
        self.assertEqual(resp.status_code, 204)
        # Verify the endpoint attributes are ***GONE!****
        acct_info['tenant_id'] = meta_data['tenant_name']
        acct_info['tenant_name'] = meta_data['tenant_name']
        for tupleset in meta_data['test_data']:
            verification_error = None
            try:
                self.verify_endpoint_attributes_present_in_all_auths(
                    acct_info=acct_info,
                    service_name=tupleset[1],
                    template_data=tupleset[2],
                    tenant_id=meta_data['tenant_name'])
            except Exception, e:
                verification_error = str(e)
            self.assertEqual(verification_error,
                             "Endpoint with name {service_name} not found".
                             format(service_name=tupleset[1]),
                             msg="The endpoints were not deleted for {t}".
                             format(t=str(tupleset)))
        # verify the original endpoint attributes are still there
        acct_info['tenant_id'] = acct_info['tenant_name'] = original_tenant_id
        self.verify_endpoint_attributes_present_in_all_auths(
            acct_info=acct_info, service_name=acct_info['service_name'],
            template_data=self.common_input,
            tenant_id=acct_info['tenant_name'])

    def test_verify_auths_after_multiple_templates_on_single_rule(self):
        """
        Add another mapping rule to a tenant_type that already exists for
        tenantId of a role already on the user.
        """
        acct_info = self.create_user_with_endpoint_from_mapping_rules()

        # Copy common_input data and change stuff in it
        test_data = self.create_random_endpoint_templates(5)
        # Create mapping rule for one of the existing tenant_types
        # already on the user, using all the template ids created above
        self.create_mapping_rule_for_tenant_type_and_template_ids(
            tenant_type=acct_info['tenant_types'][1],
            template_ids=[tupleset[0] for tupleset in test_data])
        # verify the new endpoint attributes are present
        for tupleset in test_data:
            self.verify_endpoint_attributes_present_in_all_auths(
                acct_info=acct_info,
                service_name=tupleset[1],
                template_data=tupleset[2],
                tenant_id=acct_info['tenant_name'])
        # verify the original endpoint attributes are present as well
        self.verify_endpoint_attributes_present_in_all_auths(
            acct_info=acct_info, service_name=acct_info['service_name'],
            template_data=self.common_input,
            tenant_id=acct_info['tenant_name'])

    def tearDown(self):
        for map_rule_id in self.map_rule_ids:
            (self.identity_admin_client
             .delete_tenant_type_to_endpoint_mapping_rule(rule_id=map_rule_id))
        for template_id in self.template_ids:
            self.identity_admin_client.delete_endpoint_template(template_id)
        for tenant_id in self.tenant_ids:
            self.identity_admin_client.delete_tenant(tenant_id=tenant_id)
        for user_id in self.user_ids:
            self.identity_admin_client.delete_user(user_id=user_id)
        for role_id in self.role_ids:
            self.identity_admin_client.delete_role(role_id=role_id)
        if self.test_config.run_service_admin_tests:
            for service_id in self.service_ids:
                self.service_admin_client.delete_service(
                    service_id=service_id)
        super(EndpointMappingsInAuthAndListEndpoints, self).tearDown()
