import jsonschema
from lxml import etree
from StringIO import StringIO
from strgen import StringGenerator
import urlparse

from cafe.drivers.unittest import fixtures

from tests.api.utils import config
from tests.api.utils import header_validation


class TestBase(fixtures.BaseTestFixture):

    """Child class of fixtures.BaseTestFixture for testing CDN.

    Inherit from this and write your test methods. If the child class defines
    a prepare(self) method, this method will be called before executing each
    test method.
    """

    @classmethod
    def setUpClass(cls):

        super(TestBase, cls).setUpClass()

        import requests.packages.urllib3
        requests.packages.urllib3.disable_warnings()

        cls.identity_config = config.IdentityConfig()
        cls.test_config = config.TestConfig()
        cls.url = urlparse.urljoin(
            cls.identity_config.base_url, cls.identity_config.api_version)
        cls.default_header_validations = [
            header_validation.validate_header_vary,
            header_validation.validate_header_date,
            header_validation.validate_header_server,
            header_validation.validate_header_content_type,
            header_validation.validate_header_not_present]

    @staticmethod
    def generate_random_string(pattern="API[\-]Tests[\-][\d\w]{12}"):
        """Return a random string matching the given pattern

        ex patterns:
        '[\w\d]{12}' - output: 'wb5C0S524ckc'
        '[\w\p]{10}&[\p]{2}' - random string include num, char and special
        '[\w\d]{12}[\-][\w\d]{8}' - 'wb5C0S524ckc-5C0S524c'
        """
        return StringGenerator(pattern).render()

    def assertSchema(self, response, json_schema):
        """Validate that the response body matches the schema definition

        If the response object is in json format, use the json_schema to
        validate the schema. If the response object is in xml format, use the
        json_schema definition, to generate an equivalent RelaxNG Schema
        definition. Validate the reponse XML against the generated schema.
        """
        if self.test_config.deserialize_format == 'json':
            self.assertJSONSchema(
                response=response, expected_schema=json_schema)
        else:
            xml_schema = self.generate_relaxNG_schema(
                json_schema=json_schema)
            self.assertXMLSchema(response=response, expected_schema=xml_schema)

    def assertJSONSchema(self, response, expected_schema):
        """Compare the response json with the expected JSON schema."""
        try:
            jsonschema.validate(response.json(), expected_schema)
        except jsonschema.ValidationError as message:
            assert False, message

    def assertXMLSchema(self, response, expected_schema):
        """Compare the response XML with the expected XML schema."""
        relaxng_doc = etree.parse(StringIO(expected_schema))
        relaxng = etree.RelaxNG(relaxng_doc)

        response_string = str(response.text)
        response_doc = etree.parse(StringIO(response_string))
        # Validate the xml response against the defined schema
        relaxng.assert_(response_doc)

    def assertHeaders(self, response, *functions):
        for function in functions:
            function(response)

    @staticmethod
    def generate_relaxNG_schema(json_schema):
        """Generate a RelaxNG schema equivalent of the json_schema."""

        def _translate_attr_type(json_attribute_type):
            """
            Generate an xml equivalent attribute for the json attribute

            Currently translates string(with enum) & boolean values.
            @todo: Add other attribute types as needed
            """

            attr_type = json_attribute_type['type']
            xml_attribute_type = ''

            if attr_type == 'string':
                if 'enum' in json_attribute_type:
                    xml_attribute_type = '<choice>'
                    for item in json_attribute_type['enum']:
                        choice_string = '<value>{0}</value>'.format(item)
                        xml_attribute_type += choice_string
                    xml_attribute_type += '</choice>'
                else:
                    xml_attribute_type = '<data type="string"/>'
            elif attr_type == 'boolean':
                # This assumes identity APIs always use True/ False (NOT 1/0)
                # where boolean is expected. If the assumption is wrong, fix
                # the <value>s below.
                xml_attribute_type = (
                    '<data type="boolean">'
                    '<except><value>0</value><value>1</value></except>'
                    '</data>')
            return xml_attribute_type

        name_space = '''
        <grammar xmlns="http://relaxng.org/ns/structure/1.0"
            xmlns:atom="http://www.w3.org/2005/Atom"
            xmlns:RAX-AUTH="http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0"       # noqa
            xmlns:identity="http://docs.openstack.org/identity/api/v2.0"                    # noqa
            xmlns:ns4="http://docs.rackspace.com/identity/api/ext/RAX-KSGRP/v1.0"           # noqa
            xmlns:rax-ksqa="http://docs.rackspace.com/identetity/api/ext/RAX-KSQA/v1.0"     # noqa
            xmlns:OS-KSADM="http://docs.openstack.org/identity/api/ext/OS-KSADM/v1.0"       # noqa
            xmlns:os-ksec2="http://docs.openstack.org/identity/api/ext/OS-KSEC2/v1.0"       # noqa
            xmlns:rax-kskey="http://docs.rackspace.com/identity/api/ext/RAX-KSKEY/v1.0"     # noqa
            datatypeLibrary="http://www.w3.org/2001/XMLSchema-datatypes">
           <start>'''

        xml_schema = name_space
        end_attribute = '</attribute>'
        end_element = '</element>'
        end_start = '</start>'
        end_grammar = '</grammar>'

        for element in json_schema['properties'].keys():
            # Adds default identity namespace for elements with no namespace
            if ':' not in element:
                element_name = 'identity:' + element
            else:
                element_name = element
            xml_element = '<element name="{0}">'.format(element_name)
            xml_schema += xml_element
            xml_attribute = ''
            required_attributes = (
                json_schema['properties'][element]['required'])
            for attribute in required_attributes:
                xml_attribute = '<attribute name="{0}">'.format(attribute)
                json_attribute_type = (json_schema['properties'][element]
                                       ['properties'][attribute])
                xml_attribute_type = _translate_attr_type(
                    json_attribute_type=json_attribute_type)
                xml_schema += (
                    xml_attribute + xml_attribute_type + end_attribute)
            xml_schema += end_element

        xml_schema += end_start
        xml_schema += end_grammar

        return xml_schema

    @classmethod
    def tearDownClass(cls):
        """Deletes the added resources."""
        super(TestBase, cls).tearDownClass()
