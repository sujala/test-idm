# -*- coding: utf-8 -*
import ddt
import copy

from tests.api.v2 import base
from tests.api.v2.schema import endpoint_templates
from tests.api.v2.models import factory
from tests.api import base as parent_base

from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


@ddt.ddt
class TestUpdateEndpointTemplate(base.TestBaseV2):

    """Update Endpoint Template Tests"""

    def setUp(self):
        super(TestUpdateEndpointTemplate, self).setUp()
        self.service_ids = []
        self.template_ids = []
        self.additional_schema_fields = [
            "publicURL", "adminURL", "internalURL", "versionId",
            "versionInfo", "versionList", "global", "enabled", "default"]

    def create_service(self):
        request_object = factory.get_add_service_object()
        resp = self.service_admin_client.add_service(
            request_object=request_object)
        self.assertEqual(resp.status_code, 201)
        service_id = resp.json()[const.NS_SERVICE][const.ID]
        self.service_ids.append(service_id)
        return service_id

    def create_endpoint_template(self):
        service_id = self.create_service()
        request_objest = factory.get_add_endpoint_template_object(
            service_id=service_id
        )
        resp = self.identity_admin_client.add_endpoint_template(
            request_object=request_objest
        )
        self.assertEqual(resp.status_code, 201)
        template_id = resp.json()[const.OS_KSCATALOG_ENDPOINT_TEMPLATE][
            const.ID]
        self.template_ids.append(template_id)
        return resp

    @ddt.file_data('data_update_endpoint_template_version.json')
    @parent_base.skip_if_no_service_admin_available
    def test_update_endpoint_template_version(self, test_data):
        """
        Add Endpoint Template and update endpoint version tests
        verify that version is mutable
        test_data comes from a json data file that can contain various possible
        input combinations. Each of these data combination is a separate test
        case.
        This method has tests for old way of creating endpoint templates
        using newly created service's name & type
        """

        # Create new endpoint template
        create_resp = self.create_endpoint_template()
        template_id = create_resp.json()[const.OS_KSCATALOG_ENDPOINT_TEMPLATE][
            const.ID]
        version_info = (
            create_resp.json()[const.OS_KSCATALOG_ENDPOINT_TEMPLATE][
                const.VERSION_INFO])
        version_list = (
            create_resp.json()[const.OS_KSCATALOG_ENDPOINT_TEMPLATE][
                const.VERSION_LIST])

        update_data_input = test_data['additional_input_for_update']
        expected_response_for_update = test_data[
            'expected_update_response']
        set_version_id = update_data_input['version_id']
        if 'version_info' in update_data_input:
            version_info = update_data_input['version_info']

        if 'version_list' in update_data_input:
            version_list = update_data_input['version_list']

        update_req_object = requests.EndpointTemplateUpdate(
            template_id=template_id, **update_data_input
        )
        resp = self.service_admin_client.update_endpoint_template(
            template_id=template_id, request_object=update_req_object
        )

        self.assertEqual(resp.status_code, expected_response_for_update)
        self.assertEqual(
            resp.json()[const.OS_KSCATALOG_ENDPOINT_TEMPLATE][
                const.VERSION_ID],
            set_version_id
        )
        self.assertEqual(
            resp.json()[const.OS_KSCATALOG_ENDPOINT_TEMPLATE][
                const.VERSION_INFO],
            version_info
        )
        self.assertEqual(
            resp.json()[const.OS_KSCATALOG_ENDPOINT_TEMPLATE][
                const.VERSION_LIST],
            version_list
        )

        updated_json_schema = copy.deepcopy(
            endpoint_templates.add_endpoint_template)
        updated_json_schema['properties'][
            const.OS_KSCATALOG_ENDPOINT_TEMPLATE]['required'] = (
            endpoint_templates.add_endpoint_template['properties'][
                const.OS_KSCATALOG_ENDPOINT_TEMPLATE]['required'] +
            self.additional_schema_fields)
        self.assertSchema(response=resp, json_schema=updated_json_schema)

    def tearDown(self):
        # Delete all templates created in the tests
        for id_ in self.template_ids:
            resp = self.identity_admin_client.delete_endpoint_template(
                template_id=id_)
            self.assertEqual(resp.status_code, 204)
        for id_ in self.service_ids:
            resp = self.service_admin_client.delete_service(service_id=id_)
            self.assertEqual(resp.status_code, 204)
        super(TestUpdateEndpointTemplate, self).tearDown()
