# -*- coding: utf-8 -*
from hypothesis import given, strategies
import pytest
from qe_coverage.opencafe_decorators import tags, unless_coverage
import ast
import copy
import ddt
import urllib.parse

from tests.api.v2 import base
from tests.api.v2.schema import endpoint_templates
from tests.api import base as parent_base

from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


@ddt.ddt
class TestAddEndpointTemplate(base.TestBaseV2):

    """Add Endpoint Template Tests"""
    @classmethod
    @unless_coverage
    def setUpClass(self):
        super(TestAddEndpointTemplate, self).setUpClass()

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

        self.updated_json_schema = copy.deepcopy(
            endpoint_templates.add_endpoint_template)
        self.updated_json_schema['properties'][
            const.OS_KSCATALOG_ENDPOINT_TEMPLATE]['required'] = (
            endpoint_templates.add_endpoint_template['properties'][
                const.OS_KSCATALOG_ENDPOINT_TEMPLATE]['required'] +
            self.additional_schema_fields)

    @unless_coverage
    def setUp(self):
        super(TestAddEndpointTemplate, self).setUp()
        self.service_ids = []
        self.template_ids = []
        self.user_ids = []

    @unless_coverage
    @ddt.file_data('data_add_endpoint_template_using_new_service_name_and_'
                   'type.json')
    @parent_base.skip_if_no_service_admin_available
    def test_add_endpoint_template_using_new_service_name_and_type(
            self, test_data):
        """
        Add Endpoint Template tests
        test_data comes from a json data file that can contain various possible
        input combinations. Each of these data combination is a separate test
        case.
        This method has tests for old way of creating endpoint templates
        using newly created service's name & type
        """
        input_data = copy.deepcopy(self.common_input)

        service_name = self.generate_random_string(
            pattern=const.SERVICE_NAME_PATTERN)
        service_type = self.generate_random_string(
            pattern=const.SERVICE_TYPE_PATTERN)
        service_id = int(self.generate_random_string(pattern='[\d]{8}'))

        req_object = requests.ServiceAdd(service_id=service_id,
                                         service_name=service_name,
                                         service_type=service_type,
                                         service_description='Test Service')
        resp = self.service_admin_client.add_service(
            request_object=req_object)
        self.assertEqual(resp.status_code, 201)
        service_id = resp.json()[const.NS_SERVICE][const.ID]
        self.service_ids.append(service_id)

        template_id = int(self.generate_random_string(pattern='[\d]{8}'))
        endpoint_template = requests.EndpointTemplateAdd(
            template_id=template_id, name=service_name,
            template_type=service_type, **input_data)
        resp = self.identity_admin_client.add_endpoint_template(
            endpoint_template)

        self.assertEqual(resp.status_code, 201)
        template_id = resp.json()[
            const.OS_KSCATALOG_ENDPOINT_TEMPLATE][const.ID]
        assignment_type = resp.json()[
            const.OS_KSCATALOG_ENDPOINT_TEMPLATE][
            const.RAX_AUTH_ASSIGNMENT_TYPE]

        self.template_ids.append(template_id)

        self.assertSchema(
            response=resp,
            json_schema=self.updated_json_schema)

        # GET endpoint template
        resp = self.identity_admin_client.get_endpoint_template(
            template_id=template_id)
        self.assertSchema(
            response=resp,
            json_schema=self.updated_json_schema)

        # List endpoint templates
        resp = self.identity_admin_client.list_endpoint_templates()
        self.assertSchema(
            response=resp,
            json_schema=endpoint_templates.list_endpoint_templates)

        input_update_data = test_data['additional_input_for_update']
        region = input_data[const.REGION]
        expected_response_for_update = (
            test_data['expected_update_response'])

        endpoint_template = requests.EndpointTemplateUpdate(
            template_id=template_id, region=region, **input_update_data)
        resp = self.service_admin_client.update_endpoint_template(
            template_id=template_id, request_object=endpoint_template)

        self.assertEqual(resp.status_code, expected_response_for_update)

        if expected_response_for_update == 200:
            expected_endpoint_in_catalog = test_data[
                'expected_endpoint_in_catalog']
            public_url = input_data['public_url']
            # validate endpoint in the service catalog
            self._validate_endpoint_in_service_catalog(
                service_name=service_name, template_id=template_id,
                public_url=public_url, assignment_type=assignment_type,
                expected_endpoint_in_catalog=expected_endpoint_in_catalog)

    @unless_coverage
    @ddt.file_data('data_add_endpoint_template_using_existing_service_name_'
                   'and_type.json')
    def test_add_endpoint_template_using_existing_service_name_and_type(
            self, test_data):
        """
        Add Endpoint Template tests
        test_data comes from a json data file that can contain various possible
        input combinations. Each of these data combination is a separate test
        case.
        This method has tests for old way of creating endpoint templates
        using existing service's name & type
        """
        input_data = copy.deepcopy(self.common_input)
        input_data.update(test_data['additional_input_for_create'])

        service_name = input_data[const.SERVICE_NAME]
        template_id = int(self.generate_random_string(pattern='[\d]{8}'))

        endpoint_template = requests.EndpointTemplateAdd(
            template_id=template_id, **input_data)
        resp = self.identity_admin_client.add_endpoint_template(
            endpoint_template)

        self.assertEqual(resp.status_code, 201)
        template_id = resp.json()[const.OS_KSCATALOG_ENDPOINT_TEMPLATE][
            const.ID]
        assignment_type = resp.json()[const.OS_KSCATALOG_ENDPOINT_TEMPLATE][
            const.RAX_AUTH_ASSIGNMENT_TYPE]

        self.template_ids.append(template_id)

        self.assertSchema(
            response=resp,
            json_schema=self.updated_json_schema)

        # GET endpoint template
        resp = self.identity_admin_client.get_endpoint_template(
            template_id=template_id)
        self.assertSchema(
            response=resp,
            json_schema=self.updated_json_schema)

        # List endpoint templates
        resp = self.identity_admin_client.list_endpoint_templates()
        self.assertSchema(
            response=resp,
            json_schema=endpoint_templates.list_endpoint_templates)

        if self.test_config.run_service_admin_tests:
            input_update_data = test_data['additional_input_for_update']
            region = input_data[const.REGION]
            expected_response_for_update = test_data[
                'expected_update_response']

            endpoint_template = requests.EndpointTemplateUpdate(
                template_id=template_id, region=region, **input_update_data)
            resp = self.service_admin_client.update_endpoint_template(
                template_id=template_id, request_object=endpoint_template)

            self.assertEqual(resp.status_code, expected_response_for_update)

            if expected_response_for_update == 200:
                expected_endpoint_in_catalog = test_data[
                    'expected_endpoint_in_catalog']
                public_url = input_data['public_url']
                # validate endpoint in the service catalog
                self._validate_endpoint_in_service_catalog(
                    service_name=service_name, template_id=template_id,
                    public_url=public_url, assignment_type=assignment_type,
                    expected_endpoint_in_catalog=expected_endpoint_in_catalog)

    @unless_coverage
    @ddt.file_data('data_add_endpoint_template_using_new_service_id.json')
    @parent_base.skip_if_no_service_admin_available
    def test_add_endpoint_template_using_new_service_id(self, test_data):
        """
        Add Endpoint Template tests
        test_data comes from a json data file that can contain various possible
        input combinations. Each of these data combination is a separate test
        case.
        This method has tests for new way of creating endpoint templates
        using newly created service's id
        """
        input_data = copy.deepcopy(self.common_input)
        input_data.update(test_data['additional_input_for_create'])

        assignment_type = input_data['assignment_type']
        template_id = int(self.generate_random_string(pattern='[\d]{8}'))

        service_name = self.generate_random_string(
            pattern=const.SERVICE_NAME_PATTERN)
        service_type = self.generate_random_string(
            pattern=const.SERVICE_TYPE_PATTERN)
        service_id = int(self.generate_random_string(pattern='[\d]{8}'))
        req_object = requests.ServiceAdd(service_id=service_id,
                                         service_name=service_name,
                                         service_type=service_type,
                                         service_description='Test Service')
        resp = self.service_admin_client.add_service(
            request_object=req_object)
        self.assertEqual(resp.status_code, 201)
        service_id = resp.json()[const.NS_SERVICE][const.ID]
        self.service_ids.append(service_id)

        endpoint_template = requests.EndpointTemplateAdd(
            template_id=template_id, service_id=service_id, **input_data)
        resp = self.identity_admin_client.add_endpoint_template(
            endpoint_template)

        self.assertEqual(resp.status_code, 201)
        template_id = resp.json()[const.OS_KSCATALOG_ENDPOINT_TEMPLATE][
            const.ID]
        self.template_ids.append(template_id)

        self.assertSchema(
            response=resp,
            json_schema=self.updated_json_schema)

        # GET endpoint template
        resp = self.identity_admin_client.get_endpoint_template(
            template_id=template_id)
        self.assertSchema(
            response=resp,
            json_schema=self.updated_json_schema)

        # List endpoint templates
        resp = self.identity_admin_client.list_endpoint_templates()
        self.assertSchema(
            response=resp,
            json_schema=endpoint_templates.list_endpoint_templates)

        region = input_data[const.REGION]
        input_update_data = test_data['additional_input_for_update']
        expected_response_for_update = test_data[
            'expected_update_response']

        endpoint_template = requests.EndpointTemplateUpdate(
            template_id=template_id, region=region, **input_update_data)
        resp = self.service_admin_client.update_endpoint_template(
            template_id=template_id, request_object=endpoint_template)

        self.assertEqual(resp.status_code, expected_response_for_update)

        if expected_response_for_update == 200:
            expected_endpoint_in_catalog = test_data[
                'expected_endpoint_in_catalog']
            public_url = input_data['public_url']
            # validate in service catalog
            self._validate_endpoint_in_service_catalog(
                service_name=service_name, service_id=service_id,
                assignment_type=assignment_type, template_id=template_id,
                public_url=public_url,
                expected_endpoint_in_catalog=expected_endpoint_in_catalog)

    @unless_coverage
    @ddt.file_data('data_add_endpoint_template_using_existing_service_id.json')
    def test_add_endpoint_template_using_existing_service_id(self, test_data):
        """
        Add Endpoint Template tests
        test_data comes from a json data file that can contain various possible
        input combinations. Each of these data combination is a separate test
        case.
        This method has tests for new way of creating endpoint templates
        using existing service's id
        """
        input_data = copy.deepcopy(self.common_input)
        input_data.update(test_data['additional_input_for_create'])

        template_id = int(self.generate_random_string(pattern='[\d]{8}'))
        assignment_type = input_data['assignment_type']

        service_name = test_data['service'][const.SERVICE_NAME]

        endpoint_template = requests.EndpointTemplateAdd(
            template_id=template_id, **input_data)
        resp = self.identity_admin_client.add_endpoint_template(
            endpoint_template)

        self.assertEqual(resp.status_code, 201)
        template_id = resp.json()[const.OS_KSCATALOG_ENDPOINT_TEMPLATE][
            const.ID]
        self.template_ids.append(template_id)

        self.assertSchema(
            response=resp,
            json_schema=self.updated_json_schema)

        # GET endpoint template
        resp = self.identity_admin_client.get_endpoint_template(
            template_id=template_id)

        self.assertSchema(
            response=resp,
            json_schema=self.updated_json_schema)

        # List endpoint templates
        resp = self.identity_admin_client.list_endpoint_templates()
        self.assertSchema(
            response=resp,
            json_schema=endpoint_templates.list_endpoint_templates)

        if self.test_config.run_service_admin_tests:
            region = input_data[const.REGION]
            input_update_data = test_data['additional_input_for_update']
            expected_response_for_update = test_data[
                'expected_update_response']

            endpoint_template = requests.EndpointTemplateUpdate(
                template_id=template_id, region=region, **input_update_data)
            resp = self.service_admin_client.update_endpoint_template(
                template_id=template_id, request_object=endpoint_template)

            self.assertEqual(resp.status_code, expected_response_for_update)

            if expected_response_for_update == 200:
                expected_endpoint_in_catalog = test_data[
                    'expected_endpoint_in_catalog']
                public_url = input_data['public_url']
                # validate in service catalog
                self._validate_endpoint_in_service_catalog(
                    service_name=service_name, assignment_type=assignment_type,
                    template_id=template_id, public_url=public_url,
                    expected_endpoint_in_catalog=expected_endpoint_in_catalog)

    @unless_coverage
    @pytest.mark.skip_at_gate
    @ddt.file_data('data_add_endpoint_template_negative_cases.json')
    def test_add_endpoint_template_negative(self, test_data):
        """
        Add Endpoint Template tests
        test_data comes from a json data file that can contain various possible
        input combinations. Each of these data combination is a separate test
        case.
        This method has negative tests for endpoint template creation
        e.g. empty service id
        This mainly checks for respective response code & error message
        """
        input_data = copy.deepcopy(self.common_input)
        input_data.update(test_data['additional_input_for_create'])

        expected_response = test_data['expected_response']
        template_id = int(self.generate_random_string(pattern='[\d]{8}'))

        endpoint_template = requests.EndpointTemplateAdd(
            template_id=template_id, **input_data)
        resp = self.identity_admin_client.add_endpoint_template(
            endpoint_template)

        self.assertEqual(resp.status_code, expected_response)

        if 'expected_error_message' in test_data:
            expected_error_message = test_data['expected_error_message']

            # Checking config property value to determine the expected
            # error message. Using devops client to get config properties
            disable_service_name_type_prop_value = None
            if self.test_config.run_service_admin_tests:
                disable_service_name_type_prop_resp = (
                    self.devops_client.get_devops_properties(
                        const.FEATURE_FLAG_FOR_DISABLING_SERVICE_NAME_TYPE))
                disable_service_name_type_prop_dict = (
                    disable_service_name_type_prop_resp.json())
                disable_service_name_type_prop_value = (
                    disable_service_name_type_prop_dict[
                        const.PROPERTIES][0][
                        const.VALUE])

            # This is the test for CID-353
            if (disable_service_name_type_prop_value and
                    not test_data['additional_input_for_create']):
                expected_error_message = (
                    "'{0}' and '{1}' are "
                    "required attributes.".format(
                        const.SERVICE_ID, const.RAX_AUTH_ASSIGNMENT_TYPE))
            self.assertEqual(resp.json()['badRequest']['message'],
                             expected_error_message)

    @tags('positive', 'p1', 'regression')
    @given(strategies.text(), strategies.booleans())
    def test_add_endpoint_template_hypothesis(self, template_id, region):
        """Property Based Testing
        Generate possible test inputs based on the definition of the input
        fields.
        """
        if not self.test_config.run_hypothesis_tests:
            self.skipTest('Skipping Hypothesis tests per config value')

        service_id = "a45b14e394a57e3fd4e45d59ff3693ead204998b"
        assignment_type = "MOSSO"
        name = "cloudServers"
        template_type = "compute"

        endpoint_template = requests.EndpointTemplateAdd(
            template_id=template_id, region=region, service_id=service_id,
            assignment_type=assignment_type)
        resp = self.identity_admin_client.add_endpoint_template(
            endpoint_template)
        self.assertLess(resp.status_code, 500)

        endpoint_template = requests.EndpointTemplateAdd(
            template_id=template_id, region=region, service_id=service_id,
            name=name, template_type=template_type)
        resp = self.identity_admin_client.add_endpoint_template(
            endpoint_template)
        self.assertLess(resp.status_code, 500)

    def _validate_endpoint_in_service_catalog(
            self, service_name=None, service_id=None, assignment_type=None,
            template_id=None, public_url=None,
            expected_endpoint_in_catalog=True):
        """
        Create a user with one call logic, which creates Mosso &
        Nast tenants on the fly. Authenticate the user and verify
        the presence of endpoint in the service catalog, depending on the
        test scenario. Remove endpoint from the tenant, at the end of
        verification
        """

        user_name = self.generate_random_string(pattern='Username[\w]{12}')
        password = self.generate_random_string(pattern='Password1[\d\w]{10}')
        secret_q = self.generate_random_string(pattern='SecretQ[\w]{15}')
        secret_a = self.generate_random_string(pattern='SecretA[\w]{15}')
        secret_qa = {
            const.SECRET_QUESTION: secret_q,
            const.SECRET_ANSWER: secret_a
        }
        domain_id = self.generate_random_string(pattern='[\d]{6}')

        user_object = requests.UserAdd(
            user_name=user_name,
            password=password,
            enabled=True,
            domain_id=domain_id,
            secret_qa=secret_qa
        )
        resp = self.identity_admin_client.add_user(user_object)

        self.assertEqual(resp.status_code, 201)
        user_id = resp.json()[const.USER][const.ID]
        self.user_ids.append(user_id)

        tenant_id = domain_id
        if assignment_type == const.ASSIGNMENT_TYPE_MOSSO:
            tenant_id = domain_id
        elif assignment_type == const.ASSIGNMENT_TYPE_NAST:
            tenant_id = const.NAST_PREFIX + domain_id
        elif assignment_type == const.ASSIGNMENT_TYPE_MANUAL:
            tenant_id = domain_id
            self.identity_admin_client.add_endpoint_to_tenant(
                tenant_id=tenant_id, endpoint_template_id=template_id)

        expected_url = urllib.parse.urljoin(public_url, tenant_id)
        req_obj = requests.AuthenticateWithPassword(
            user_name=user_name, password=password
        )
        resp = self.identity_admin_client.get_auth_token(
            request_object=req_obj)
        catalog = resp.json()[const.ACCESS][const.SERVICE_CATALOG]
        assert_endpoint_present = False
        for service_ in catalog:
            if service_[const.SERVICE_NAME] == service_name:
                for endpoint_ in service_[const.SERVICE_ENDPOINTS]:
                    if const.PUBLIC_URL in endpoint_:
                        if ast.literal_eval(
                             "'%s'" % endpoint_[const.PUBLIC_URL]) == (
                                expected_url):
                            assert_endpoint_present = True
                            break
                break
        self.assertEqual(assert_endpoint_present,
                         expected_endpoint_in_catalog)

        auth_token = resp.json()[const.ACCESS][const.TOKEN][const.ID]
        resp = self.identity_admin_client.list_endpoints_for_token(
            token=auth_token)
        self.assertEqual(resp.status_code, 200)

        assert_endpoint_present = False
        endpoints_ = resp.json()[const.ENDPOINTS]
        for endpoint_ in endpoints_:
            if endpoint_[const.ID] == int(template_id):
                if const.PUBLIC_URL in endpoint_:
                    if ast.literal_eval("'%s'" % endpoint_[
                          const.PUBLIC_URL]) == expected_url:
                        assert_endpoint_present = True
                        break
        self.assertEqual(assert_endpoint_present,
                         expected_endpoint_in_catalog)

        # For cleanup
        self.service_admin_client.delete_endpoint_from_tenant(
            tenant_id=tenant_id, endpoint_template_id=template_id)

    @unless_coverage
    def tearDown(self):
        # Disable & delete all templates created in the tests
        if self.test_config.run_service_admin_tests:
            for id_ in self.template_ids:
                # Disable the endpoint template, so that it can be deleted
                endpoint_template = requests.EndpointTemplateUpdate(
                    template_id=id_, enabled='false')
                resp = self.service_admin_client.update_endpoint_template(
                    template_id=id_, request_object=endpoint_template)
                self.assertEqual(resp.status_code, 200)

                resp = self.identity_admin_client.delete_endpoint_template(
                    template_id=id_)
                self.assertEqual(resp.status_code, 204)
            for id_ in self.service_ids:
                resp = self.service_admin_client.delete_service(service_id=id_)
                self.assertEqual(resp.status_code, 204)
        # Delete users created during the tests
        for id_ in self.user_ids:
            resp = self.identity_admin_client.delete_user(user_id=id_)
            self.assertEqual(resp.status_code, 204)
        super(TestAddEndpointTemplate, self).tearDown()

    @classmethod
    @unless_coverage
    def tearDownClass(cls):
        super(TestAddEndpointTemplate, cls).tearDownClass()
