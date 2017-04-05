# -*- coding: utf-8 -*
import copy
import ddt

from tests.api.utils import header_validation
from tests.api.v2 import base
from tests.api.v2.models import factory
from tests.api.v2.models import responses
from tests.api.v2.schema import tenant_type_to_endpoint_mapping_rules as rules

from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


ENDPOINT_DOES_NOT_EXIST_ERROR = ("Error code: 'EP-000'; Endpoint template "
                                 "'{0}' does not exist")
ERROR_CODE = "Error code: 'EP-000'"
ENDPOINTS_LIMIT = 1000
ENDPOINT_LIMIT_ERROR = (
    'endpointTemplateMembers: size must be between 0 and 1000')
RULE_DOES_NOT_EXIST_ERROR = "The specified rule does not exist"
INVALID_RESPONSE_DETAIL = "responseDetail: Invalid value"

"""
 CID-362 : Create a new v2.0 API to associate a tenant type to a set of
 endpoints
 CID-370 : Provide a v2.0 Get Endpoint Mapping Rule service to retrieve a
 specific rule based on id
 CID-364 : Provide a service to delete a tenant type mapping rule
"""


@ddt.ddt
class TestAddEndpointMappingRule(base.TestBaseV2):

    """Add/Get/Delete Tenant Type to Endpoint Templates mapping rules Tests"""
    @classmethod
    def setUpClass(self):
        super(TestAddEndpointMappingRule, self).setUpClass()

        self.common_input = {
            "public_url": "https://www.test_public_endpoint_template.com",
            "internal_url": "https://www.test_internal_endpoint_template.com",
            "admin_url": "https://www.test_admin_endpoint_template.com",
            "version_info": "test_version_info",
            "version_id": "1",
            "version_list": "test_version_list",
            "region": "ORD"
        }
        self.additional_schema_fields = [
            "publicURL", "adminURL", "internalURL", "versionId",
            "versionInfo", "versionList", "global", "enabled", "default"]

        id_admin_id = self.identity_admin_client.default_headers[
            const.X_USER_ID]

        if self.test_config.run_service_admin_tests:
            option = {
                const.PARAM_ROLE_NAME: const.ENDPOINT_RULE_ADMIN_ROLE_NAME
            }
            list_resp = self.service_admin_client.list_roles(option=option)
            mapping_rules_role_id = list_resp.json()[const.ROLES][0][const.ID]
            self.service_admin_client.add_role_to_user(
                user_id=id_admin_id, role_id=mapping_rules_role_id)

        self.unexpected_headers_HTTP_201 = [
            header_validation.validate_transfer_encoding_header_not_present]
        self.unexpected_headers_HTTP_400 = [
            header_validation.validate_location_header_not_present]
        self.header_validation_functions_HTTP_201 = (
            self.default_header_validations +
            self.unexpected_headers_HTTP_201 + [
                header_validation.validate_header_location,
                header_validation.validate_header_content_length])
        self.header_validation_functions_HTTP_400 = (
            self.default_header_validations +
            self.unexpected_headers_HTTP_400)

    def setUp(self):
        super(TestAddEndpointMappingRule, self).setUp()
        self.description = self.generate_random_string(
            pattern=const.MAPPING_RULE_DESCRIPTION_PATTERN)
        self.service_ids = []
        self.template_ids = []
        self.rule_ids = []
        self.user_ids = []

    def create_service(self, service_object):

        resp = self.service_admin_client.add_service(
            request_object=service_object)
        self.assertEqual(resp.status_code, 201)
        service = responses.Service(resp.json())
        service_id = service.id
        self.service_ids.append(service_id)
        return service_id

    def create_endpoint_template_using_service_name_type(
            self, input_data):

        template_id = int(self.generate_random_string(pattern='[\d]{8}'))

        service_name = self.generate_random_string(
            pattern=const.SERVICE_NAME_PATTERN)
        service_type = self.generate_random_string(
            pattern=const.SERVICE_TYPE_PATTERN)
        service_id = int(self.generate_random_string(pattern='[\d]{8}'))
        service_object = requests.ServiceAdd(
            service_id=service_id, service_name=service_name,
            service_type=service_type, service_description='Test Service')
        self.create_service(service_object=service_object)

        endpoint_template = requests.EndpointTemplateAdd(
            template_id=template_id, name=service_name,
            template_type=service_type, **input_data)
        resp = self.identity_admin_client.add_endpoint_template(
            endpoint_template)

        self.assertEqual(resp.status_code, 201)
        template_id = resp.json()[const.OS_KSCATALOG_ENDPOINT_TEMPLATE][
            const.ID]
        return template_id

    def create_endpoint_template_using_service_id(self):

        service_object = factory.get_add_service_object()
        service_id = self.create_service(service_object=service_object)

        template_object = factory.get_add_endpoint_template_object(
            service_id=service_id)
        resp = self.identity_admin_client.add_endpoint_template(
            request_object=template_object)
        endpoint_template = responses.EndpointTemplate(resp.json())
        template_id = endpoint_template.id
        return template_id

    def get_rules_list_from_response(self, list_resp):
        """
        Obtain the list of mapping rules from the response
        """
        if const.RAX_AUTH_ENDPOINT_ASSIGNMENT_RULES in list_resp:
            return list_resp[const.RAX_AUTH_ENDPOINT_ASSIGNMENT_RULES][
                const.TENANT_TYPE_TO_ENDPOINT_MAPPING_RULES]
        else:
            return list_resp

    def check_if_rule_is_present_in_the_list(
            self, mapping_rule_id, list_resp):
        """
        Verify if a given mapping rule is in the provided list
        """

        rules_list = self.get_rules_list_from_response(list_resp)
        rule_ids = [rule['id'] for rule in rules_list]
        self.assertIn(mapping_rule_id, rule_ids)

    def verify_no_duplicates_in_list_rules(self, list_resp):
        """
        Assert that there are no duplicates in the list mapping rules response
        """

        rules_list = self.get_rules_list_from_response(list_resp)
        if rules_list:
            rule_ids = [rule['id'] for rule in rules_list]
            rule_ids_set = set(rule_ids)
            self.assertEqual(sorted(rule_ids), sorted(list(rule_ids_set)))

    def get_mapping_rule_id_from_response(self, response, teardown=True):
        """
        Return rule id from the response provided.
        Choice to add the rule for teardown cleanup
        """
        mapping_rule = responses.TenantTypeToEndpointMappingRule(
            response.json())
        if teardown:
            self.rule_ids.append(mapping_rule.id)
        return mapping_rule.id

    def validate_list_rules_response(self, mapping_rule_id=None):
        """
        Test list mapping rules response. Check for the schema, headers.
        Verify the provided rule is present in the list.
        Also, confirm if there are no duplicates in the list
        """
        ia_client = self.identity_admin_client
        # List tenant type to endpoints mapping rules
        list_resp = ia_client.list_tenant_type_to_endpoint_mapping_rules()
        self.assertEqual(list_resp.status_code, 200)
        if self.get_rules_list_from_response(list_resp.json()):
            self.assertSchema(
                list_resp, rules.list_tenant_type_to_endpoint_mapping_rules)
        self.assertHeaders(
            list_resp, *self.header_validation_functions_HTTP_200)
        if mapping_rule_id:
            self.check_if_rule_is_present_in_the_list(
                mapping_rule_id=mapping_rule_id, list_resp=list_resp.json())
        self.verify_no_duplicates_in_list_rules(list_resp=list_resp.json())
        return list_resp

    def test_create_tenant_type_to_endpoint_mapping_rules(self):
        """
        Tests to check creation of rule works with one and multiple
        of endpoints.
        Tests also verify that the tenant type need not be present on
        any existing tenant
        Lastly, tests check if creation of rule works even if tenant type is
        already used in other rule
        Test positive cases on delete, list mapping rules
        503 for if the search for list rules results in more than the
        threshold rules, is currently tested manually.

        """
        if not self.test_config.run_service_admin_tests:
            self.skipTest('Skipping Service Admin Tests per config value')

        input_data = copy.deepcopy(self.common_input)

        # first endpoint template
        template_id = (
            self.create_endpoint_template_using_service_name_type(
                input_data))
        self.template_ids.append(template_id)

        endpoint_ids = [template_id]
        tenant_type = self.generate_random_string(const.TENANT_TYPE_PATTERN)
        mapping_rule_object = requests.TenantTypeToEndpointMappingRule(
            tenant_type=tenant_type, endpoint_ids=endpoint_ids,
            description=self.description)
        ia_client = self.identity_admin_client
        resp = ia_client.add_tenant_type_to_endpoint_mapping_rule(
                request_object=mapping_rule_object)
        self.assertEqual(resp.status_code, 201)
        self.assertSchema(
            resp, rules.add_tenant_type_to_endpoint_mapping_rule)

        mapping_rule_id = self.get_mapping_rule_id_from_response(
            response=resp)

        # second endpoint template
        template_id = (
            self.create_endpoint_template_using_service_name_type(
                input_data))
        self.template_ids.append(template_id)

        endpoint_ids.append(template_id)

        # Test verifies that rule can be created even if tenant type
        # is already used in other rule. Also, it checks the functionality
        # for multiple endpoints
        mapping_rule_object = requests.TenantTypeToEndpointMappingRule(
            tenant_type=tenant_type, endpoint_ids=endpoint_ids,
            description=self.description)
        ia_client = self.identity_admin_client
        resp = ia_client.add_tenant_type_to_endpoint_mapping_rule(
            request_object=mapping_rule_object)
        self.assertEqual(resp.status_code, 201)
        self.assertSchema(
            resp, rules.add_tenant_type_to_endpoint_mapping_rule)
        self.assertHeaders(
            resp, *self.header_validation_functions_HTTP_201)

        # Tests for GET a rule call....CID-370
        # Obtaining the rule-id first
        mapping_rule_id = self.get_mapping_rule_id_from_response(
            response=resp, teardown=False)

        # GET mapping rule call, without the required role
        domain_id = self.generate_random_string(const.DOMAIN_PATTERN)
        uadm_client = self.generate_client(
            parent_client=self.identity_admin_client,
            additional_input_data={'domain_id': domain_id})
        user_admin_id = uadm_client.default_headers[const.X_USER_ID]
        self.user_ids.append(user_admin_id)
        get_resp = uadm_client.get_tenant_type_to_endpoint_mapping_rule(
            rule_id=mapping_rule_id)
        self.assertEqual(get_resp.status_code, 403)

        # DELETE mapping rule call, without the required role
        delete_resp = uadm_client.delete_tenant_type_to_endpoint_mapping_rule(
            rule_id=mapping_rule_id)
        self.assertEqual(delete_resp.status_code, 403)

        # GET with the required role
        get_resp = ia_client.get_tenant_type_to_endpoint_mapping_rule(
            rule_id=mapping_rule_id)
        self.assertEqual(get_resp.status_code, 200)
        self.assertSchema(
            get_resp, rules.add_tenant_type_to_endpoint_mapping_rule)
        get_mapping_rule = responses.TenantTypeToEndpointMappingRule(
            get_resp.json())
        endpoints_in_get_resp = get_mapping_rule.endpoint_templates
        self.verify_no_duplicate_endpoint_ids(
            endpoints_in_get_resp, endpoint_ids)
        self.assertEqual(get_mapping_rule.tenant_type, tenant_type)

        # # List tenant type to endpoints mapping rules
        self.validate_list_rules_response(mapping_rule_id=mapping_rule_id)

        # DELETE mapping rule call, with the required role...CID-364
        delete_resp = ia_client.delete_tenant_type_to_endpoint_mapping_rule(
            rule_id=mapping_rule_id)
        self.assertEqual(delete_resp.status_code, 204)

        # Check if it got deleted
        get_resp = ia_client.get_tenant_type_to_endpoint_mapping_rule(
            rule_id=mapping_rule_id)
        self.assertEqual(get_resp.status_code, 404)

    @ddt.file_data(
        'data_create_mapping_rules_with_different_tenant_types.json')
    def test_create_mapping_rules_with_different_tenant_types(
            self, test_data):
        """
        Test create mapping rules with different positive & negative
        cases for tenant types.
        Test list mapping rules in those cases
        """

        if not self.test_config.run_service_admin_tests:
            self.skipTest('Skipping Service Admin Tests per config value')

        template_id = self.create_endpoint_template_using_service_id()
        self.template_ids.append(template_id)
        endpoint_ids = [template_id]

        if 'tenant_type' in test_data:
            tenant_type = test_data['tenant_type']
        else:
            tenant_type = None
        expected_response = test_data['expected_response']
        mapping_rule_object = requests.TenantTypeToEndpointMappingRule(
            tenant_type=tenant_type, endpoint_ids=endpoint_ids,
            description=self.description)
        ia_client = self.identity_admin_client
        resp = ia_client.add_tenant_type_to_endpoint_mapping_rule(
            request_object=mapping_rule_object)
        self.assertEqual(resp.status_code, expected_response)
        if 'error_message' in test_data:
            self.assertEqual(resp.json()[const.BAD_REQUEST][const.MESSAGE],
                             test_data['error_message'])

        if expected_response == 201:
            self.assertSchema(
                resp, rules.add_tenant_type_to_endpoint_mapping_rule)
            self.assertHeaders(
                resp, *self.header_validation_functions_HTTP_201)

            mapping_rule_id = self.get_mapping_rule_id_from_response(
                response=resp)

            # Checking if this rule appears in List rules response & also
            # validate the list rules response
            self.validate_list_rules_response(mapping_rule_id=mapping_rule_id)

    @ddt.file_data(
        'data_create_mapping_rule_with_different_descriptions.json')
    def test_create_mapping_rules_with_different_descriptions(
            self, test_data):
        """
        Tests to validate different descriptions values on
        rules creation
        """
        if not self.test_config.run_service_admin_tests:
            self.skipTest('Skipping Service Admin Tests per config value')

        template_id = self.create_endpoint_template_using_service_id()
        self.template_ids.append(template_id)
        endpoint_ids = [template_id]

        description = None
        if 'description' in test_data:
            description = test_data['description']
        expected_response = test_data['expected_response']

        tenant_type = self.generate_random_string(const.TENANT_TYPE_PATTERN)
        mapping_rule_object = requests.TenantTypeToEndpointMappingRule(
            tenant_type=tenant_type, endpoint_ids=endpoint_ids,
            description=description)
        ia_client = self.identity_admin_client
        resp = ia_client.add_tenant_type_to_endpoint_mapping_rule(
            request_object=mapping_rule_object)
        self.assertEqual(resp.status_code, expected_response)
        if expected_response == 400:
            self.assertEqual(resp.json()[const.BAD_REQUEST][const.MESSAGE],
                             test_data['error_message'])
            self.assertHeaders(
                resp, *self.header_validation_functions_HTTP_400)
        elif expected_response == 201:
            self.assertSchema(
                resp, rules.add_tenant_type_to_endpoint_mapping_rule)
            self.assertHeaders(
                resp, *self.header_validation_functions_HTTP_201)
            if not description:
                self.assertNotIn(const.DESCRIPTION, resp.json()[
                    const.NS_TENANT_TYPE_TO_ENDPOINT_MAPPING_RULE])

            mapping_rule_id = self.get_mapping_rule_id_from_response(
                response=resp)

            # Checking if this rule appears in List rules response & also
            # validate the list rules response
            self.validate_list_rules_response(mapping_rule_id=mapping_rule_id)

    @ddt.data([], None, '')
    def test_create_mapping_rules_with_different_endpoint_ids(
            self, endpoint_ids):
        """
        Checking if endpoint templates are not the 'required' attributes
        in the request.
        """

        tenant_type = self.generate_random_string(const.TENANT_TYPE_PATTERN)
        mapping_rule_object = requests.TenantTypeToEndpointMappingRule(
            tenant_type=tenant_type, endpoint_ids=endpoint_ids)
        ia_client = self.identity_admin_client
        resp = ia_client.add_tenant_type_to_endpoint_mapping_rule(
            request_object=mapping_rule_object)
        self.assertEqual(resp.status_code, 201)
        self.assertSchema(
            resp, rules.add_tenant_type_to_endpoint_mapping_rule)
        self.assertHeaders(
            resp, *self.header_validation_functions_HTTP_201)

        mapping_rule_id = self.get_mapping_rule_id_from_response(
            response=resp)

        # Checking if this rule appears in List rules response & also
        # validate the list rules response
        self.validate_list_rules_response(mapping_rule_id=mapping_rule_id)

    def verify_no_duplicate_endpoint_ids(self, response_list, request_list):
        """
        Checking if the created rule does not have duplicate endpoint ids
        in the response, by first creating the set of the ids in request and
        then comparing the sorted list from response with sorted list
        obtained from the set
        """

        request_set = set(request_list)
        expected_list = list(request_set)
        expected_list_sorted = sorted(expected_list)

        response_id_list = [
            id_dict['id'] for id_dict in response_list if 'id' in id_dict]
        response_id_list_sorted = sorted(response_id_list)
        self.assertEqual(expected_list_sorted, response_id_list_sorted)

    def test_create_mapping_rules_checking_no_duplication_of_endpoints(self):
        """
        Tests to verify no duplication of endpoints on rule creation
        """

        template_id_1 = self.create_endpoint_template_using_service_id()
        self.template_ids.append(template_id_1)
        endpoint_ids = [template_id_1]

        tenant_type = self.generate_random_string(const.TENANT_TYPE_PATTERN)
        mapping_rule_object = requests.TenantTypeToEndpointMappingRule(
            tenant_type=tenant_type, endpoint_ids=endpoint_ids,
            description=self.description)
        ia_client = self.identity_admin_client
        resp = ia_client.add_tenant_type_to_endpoint_mapping_rule(
            request_object=mapping_rule_object)
        self.assertEqual(resp.status_code, 201)
        mapping_rule_id = resp.json()[
            const.NS_TENANT_TYPE_TO_ENDPOINT_MAPPING_RULE][const.ID]
        self.rule_ids.append(mapping_rule_id)

        # Creating another endpoint template
        template_id_2 = self.create_endpoint_template_using_service_id()
        self.template_ids.append(template_id_2)
        endpoint_ids.append(template_id_2)

        # Appending 1st template again in the list
        endpoint_ids.append(template_id_1)

        mapping_rule_object = requests.TenantTypeToEndpointMappingRule(
            tenant_type=tenant_type, endpoint_ids=endpoint_ids,
            description=self.description)
        ia_client = self.identity_admin_client
        resp = ia_client.add_tenant_type_to_endpoint_mapping_rule(
            request_object=mapping_rule_object)
        self.assertEqual(resp.status_code, 201)
        self.assertHeaders(
            resp, *self.header_validation_functions_HTTP_201)
        mapping_rule_id = resp.json()[
            const.NS_TENANT_TYPE_TO_ENDPOINT_MAPPING_RULE][const.ID]
        self.rule_ids.append(mapping_rule_id)

        endpoints_list_in_resp = resp.json()[
            const.NS_TENANT_TYPE_TO_ENDPOINT_MAPPING_RULE][
            const.OS_KSCATALOG_ENDPOINT_TEMPLATES]
        self.verify_no_duplicate_endpoint_ids(
            endpoints_list_in_resp, endpoint_ids)

    def test_to_verify_the_need_of_endpoint_admin_role(self):
        """
        Tests to check requirement of 'identity:endpoint-rule-admin' role
        to Create, Get, Delete mapping rule
        """

        domain_id = self.generate_random_string(const.DOMAIN_PATTERN)
        user_admin_client = self.generate_client(
            parent_client=self.identity_admin_client,
            additional_input_data={'domain_id': domain_id})
        user_admin_id = user_admin_client.default_headers[const.X_USER_ID]
        self.user_ids.append(user_admin_id)
        endpoint_ids = [100000]
        tenant_type = self.generate_random_string(const.TENANT_TYPE_PATTERN)
        mapping_rule_object = requests.TenantTypeToEndpointMappingRule(
            tenant_type=tenant_type, endpoint_ids=endpoint_ids,
            description=self.description)
        resp = user_admin_client.add_tenant_type_to_endpoint_mapping_rule(
            request_object=mapping_rule_object)
        self.assertEqual(resp.status_code, 403)

        # GET mapping rule call, without the required role
        mapping_rule_id = self.generate_random_string(const.NUMBERS_PATTERN)
        get_resp = user_admin_client.get_tenant_type_to_endpoint_mapping_rule(
            rule_id=mapping_rule_id)
        self.assertEqual(get_resp.status_code, 403)

        # LIST mapping rules call, without the required role
        list_resp = (
            user_admin_client.list_tenant_type_to_endpoint_mapping_rules())
        self.assertEqual(list_resp.status_code, 403)

        # DELETE mapping rule call, without the required role
        ua_client = user_admin_client
        get_resp = ua_client.delete_tenant_type_to_endpoint_mapping_rule(
            rule_id=mapping_rule_id)
        self.assertEqual(get_resp.status_code, 403)

    def test_to_verify_rules_creation_with_non_existing_endpoints(self):
        """
        Negative tests scenarios with endpoint templates
        """

        # With just one template which is non-existant
        template_id = self.generate_random_string(pattern='[1-9]{1}[\d]{8}')
        resp = self.identity_admin_client.get_endpoint_template(template_id)
        while resp.status_code == 200:
            template_id = self.generate_random_string(
                pattern='[1-9]{1}[\d]{8}')
            resp = self.identity_admin_client.get_endpoint_template(
                template_id)
        endpoint_ids = [template_id]
        tenant_type = self.generate_random_string(const.TENANT_TYPE_PATTERN)
        mapping_rule_object = requests.TenantTypeToEndpointMappingRule(
            tenant_type=tenant_type, endpoint_ids=endpoint_ids,
            description=self.description)
        ia_client = self.identity_admin_client
        resp = ia_client.add_tenant_type_to_endpoint_mapping_rule(
            request_object=mapping_rule_object)
        self.assertEqual(resp.status_code, 400)
        self.assertEqual(resp.json()[const.BAD_REQUEST][const.MESSAGE],
                         ENDPOINT_DOES_NOT_EXIST_ERROR.format(template_id))
        self.assertHeaders(
            resp, *self.header_validation_functions_HTTP_400)

        # With one existing and one non-existant endpoint
        template_id_2 = self.create_endpoint_template_using_service_id()
        self.template_ids.append(template_id_2)
        endpoint_ids.append(template_id_2)
        mapping_rule_object = requests.TenantTypeToEndpointMappingRule(
            tenant_type=tenant_type, endpoint_ids=endpoint_ids,
            description=self.description)
        resp = ia_client.add_tenant_type_to_endpoint_mapping_rule(
            request_object=mapping_rule_object)
        self.assertEqual(resp.status_code, 400)
        self.assertEqual(resp.json()[const.BAD_REQUEST][const.MESSAGE],
                         ENDPOINT_DOES_NOT_EXIST_ERROR.format(template_id))
        self.assertHeaders(
            resp, *self.header_validation_functions_HTTP_400)

        # With both templates which are non-existant
        endpoint_ids.remove(template_id_2)
        template_id_3 = self.generate_random_string(pattern='[\d]{9}')
        resp = self.identity_admin_client.get_endpoint_template(template_id_3)

        # Finding non existing endpoint template id
        while resp.status_code == 200:
            template_id_3 = self.generate_random_string(pattern='[\d]{9}')
            resp = self.identity_admin_client.get_endpoint_template(
                template_id_3)

        endpoint_ids.append(template_id_3)
        mapping_rule_object = requests.TenantTypeToEndpointMappingRule(
            tenant_type=tenant_type, endpoint_ids=endpoint_ids,
            description=self.description)
        resp = ia_client.add_tenant_type_to_endpoint_mapping_rule(
            request_object=mapping_rule_object)
        self.assertEqual(resp.status_code, 400)
        self.assertIn(
            ERROR_CODE, resp.json()[const.BAD_REQUEST][const.MESSAGE])
        self.assertHeaders(
            resp, *self.header_validation_functions_HTTP_400)

    def test_limit_on_number_of_endpoints_per_tenant_type(self):
        """
        Application does check the count before checking if the endpoints
        actually exist or not. So, any list of endpoint ids with more than
        the limit would be sufficient to test it.
        """
        endpoint_ids = range(1, ENDPOINTS_LIMIT + 2)
        tenant_type = self.generate_random_string(const.TENANT_TYPE_PATTERN)
        mapping_rule_object = requests.TenantTypeToEndpointMappingRule(
            tenant_type=tenant_type, endpoint_ids=endpoint_ids,
            description=self.description)
        ia_client = self.identity_admin_client
        resp = ia_client.add_tenant_type_to_endpoint_mapping_rule(
            request_object=mapping_rule_object)
        self.assertEqual(resp.status_code, 400)
        self.assertEqual(resp.json()[const.BAD_REQUEST][const.MESSAGE],
                         ENDPOINT_LIMIT_ERROR)
        self.assertHeaders(
            resp, *self.header_validation_functions_HTTP_400)

    def verify_get_and_delete_given_rule(self, rule_id):
        """
        Make a GET mapping rule call and then make a DELETE mapping rule call
        using rule id provided.
        This is mainly a helper for the negative scenarios being tested in
        test_get_tenant_type_to_endpoint_mapping_rules_for_different_rule_ids.
        """
        ia_client = self.identity_admin_client
        get_resp = ia_client.get_tenant_type_to_endpoint_mapping_rule(
            rule_id=rule_id)
        self.assertEqual(get_resp.status_code, 404)
        self.assertEqual(get_resp.json()['itemNotFound'][const.MESSAGE],
                         RULE_DOES_NOT_EXIST_ERROR)
        delete_resp = (
            ia_client.delete_tenant_type_to_endpoint_mapping_rule(
                rule_id=rule_id))
        self.assertEqual(delete_resp.status_code, 404)
        self.assertEqual(delete_resp.json()['itemNotFound'][const.MESSAGE],
                         RULE_DOES_NOT_EXIST_ERROR)

    def test_get_tenant_type_to_endpoint_mapping_rules_negative(
            self):
        """
        Testing various invalid cases for GET a rule using rule-id
        """
        # Non-existing rule id
        mapping_rule_id = self.generate_random_string()
        self.verify_get_and_delete_given_rule(mapping_rule_id)

        ia_client = self.identity_admin_client
        delete_resp = ia_client.delete_tenant_type_to_endpoint_mapping_rule(
            rule_id='')
        self.assertEqual(delete_resp.status_code, 405)

        # rule id set to None
        self.verify_get_and_delete_given_rule(None)

        # rule id close to an existing one
        template_id = self.create_endpoint_template_using_service_id()
        self.template_ids.append(template_id)
        endpoint_ids = [template_id]

        tenant_type = self.generate_random_string(const.TENANT_TYPE_PATTERN)
        mapping_rule_object = requests.TenantTypeToEndpointMappingRule(
            tenant_type=tenant_type, endpoint_ids=endpoint_ids,
            description=self.description)
        resp = ia_client.add_tenant_type_to_endpoint_mapping_rule(
            request_object=mapping_rule_object)
        mapping_rule_id = self.get_mapping_rule_id_from_response(
            response=resp)

        # existing rule id appended with a letter
        rule_id = mapping_rule_id + 'a'
        self.verify_get_and_delete_given_rule(rule_id)

        # existing rule id appended with a digit
        rule_id = mapping_rule_id + '8'
        self.verify_get_and_delete_given_rule(rule_id)

        # existing rule id missing last char
        rule_id = mapping_rule_id[:-1]
        self.verify_get_and_delete_given_rule(rule_id)

    @ddt.file_data('data_get_mapping_rule_with_query_param.json')
    def test_get_mapping_rule_with_query_param(self, test_data):
        """
        Tests for query param 'responseDetail' query param for
        GET mapping rule api call
        """

        endpoint_ids = []
        for count in range(3):
            template_id = self.create_endpoint_template_using_service_id(
                )
            self.template_ids.append(template_id)
            endpoint_ids.append(template_id)

        tenant_type = self.generate_random_string(const.TENANT_TYPE_PATTERN)
        mapping_rule_object = requests.TenantTypeToEndpointMappingRule(
            tenant_type=tenant_type, endpoint_ids=endpoint_ids,
            description=self.description)
        ia_client = self.identity_admin_client
        resp = ia_client.add_tenant_type_to_endpoint_mapping_rule(
            request_object=mapping_rule_object)
        mapping_rule_id = self.get_mapping_rule_id_from_response(
            response=resp)

        resp_detail = None
        if 'response_detail' in test_data:
            resp_detail = test_data['response_detail']

        schema_to_validate = test_data['schema_to_validate']
        get_resp = ia_client.get_tenant_type_to_endpoint_mapping_rule(
            rule_id=mapping_rule_id, response_detail=resp_detail)
        if schema_to_validate == 'basic':
            self.assertSchema(
                get_resp, rules.tenant_type_to_endpoint_mapping_rule_basic)
            self.assertEqual(get_resp.status_code, 200)
        elif schema_to_validate == 'minimum':
            self.assertSchema(
                get_resp, rules.add_tenant_type_to_endpoint_mapping_rule)
            self.assertEqual(get_resp.status_code, 200)
        else:
            self.assertEqual(get_resp.status_code, 400)
            self.assertEqual(get_resp.json()[const.BAD_REQUEST][const.MESSAGE],
                             INVALID_RESPONSE_DETAIL)

    def test_empty_list_of_mapping_rules(self):
        """
        Tests when there are no rules, list call returns empty list
        """

        # Running only locally & Jenkins, as this deletes all existing rules
        if not self.test_config.run_local_and_jenkins_only:
            self.skipTest('Skipping if not local and jenkins')
        ia_client = self.identity_admin_client

        # First remove all the rules to cause an empty list
        list_resp = self.validate_list_rules_response()
        rules_list = self.get_rules_list_from_response(list_resp.json())

        if rules_list:
            mapping_rule_ids = [rule['id'] for rule in rules_list]
            for id_ in mapping_rule_ids:
                resp = ia_client.delete_tenant_type_to_endpoint_mapping_rule(
                    rule_id=id_)
                self.assertEqual(resp.status_code, 204)
                if id_ in self.rule_ids:
                    self.rule_ids.remove(id_)

            # Now, call List and verify empty list
            list_resp = self.validate_list_rules_response()
            rules_list = self.get_rules_list_from_response(list_resp.json())
        # We may need to change this once defect CID-536 is fixed
        self.assertEqual(rules_list, None)

    def tearDown(self):
        for id_ in self.template_ids:
            self.identity_admin_client.delete_endpoint_template(
                template_id=id_)
        for id_ in self.rule_ids:
            ia_client = self.identity_admin_client
            ia_client.delete_tenant_type_to_endpoint_mapping_rule(rule_id=id_)
        for id_ in self.user_ids:
            self.identity_admin_client.delete_user(user_id=id_)
        if self.test_config.run_service_admin_tests:
            for id_ in self.service_ids:
                self.service_admin_client.delete_service(service_id=id_)
        super(TestAddEndpointMappingRule, self).tearDown()
