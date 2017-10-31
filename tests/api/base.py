import jsonschema
import logging
from lxml import etree
import os
from StringIO import StringIO
from strgen import StringGenerator
from itertools import combinations
import urlparse

from cafe.drivers.unittest import fixtures

from tests.api.utils import config
from tests.api.utils import header_validation

from tests.package.johny import constants as const


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
            cls.identity_config.base_url, cls.identity_config.cloud_url)
        cls.url = urlparse.urljoin(cls.url, cls.identity_config.api_version)
        cls.internal_url = urlparse.urljoin(
            cls.identity_config.internal_url, cls.identity_config.cloud_url)
        cls.internal_url = urlparse.urljoin(cls.internal_url,
                                            cls.identity_config.api_version)

        cls.devops_url = urlparse.urljoin(cls.identity_config.base_url,
                                          const.DEVOPS_URL)
        cls.default_header_validations = [
            header_validation.validate_header_vary,
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

    def assertBoolean(self, expected, actual):
        if expected.lower() == 'true':
            expected = True
        else:
            expected = False
        self.assertEqual(actual, expected)

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
            elif attr_type == 'integer':
                xml_attribute_type = '<data type="string"/>'
            return xml_attribute_type

        name_space = '''
        <grammar xmlns="http://relaxng.org/ns/structure/1.0"
            xmlns:atom="http://www.w3.org/2005/Atom"
            xmlns:RAX-AUTH="http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0"
            xmlns:OS-KSCATALOG="http://docs.openstack.org/identity/api/ext/OS-KSCATALOG/v1.0"
            xmlns:identity="http://docs.openstack.org/identity/api/v2.0"
            xmlns:ns4="http://docs.rackspace.com/identity/api/ext/RAX-KSGRP/v1.0"
            xmlns:rax-ksqa="http://docs.rackspace.com/identetity/api/ext/RAX-KSQA/v1.0"
            xmlns:OS-KSADM="http://docs.openstack.org/identity/api/ext/OS-KSADM/v1.0"
            xmlns:os-ksec2="http://docs.openstack.org/identity/api/ext/OS-KSEC2/v1.0"
            xmlns:rax-kskey="http://docs.rackspace.com/identity/api/ext/RAX-KSKEY/v1.0"
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
    def generate_data_combinations(self, data_dict, start=None):
        """
        This method is intended to support a common testing pattern where a
        data-driven test is given a data set and tests using combinations of
        that data to provide better coverage the input domain. This is done
        by creating combinations of the input data dictionary with length
        starting with `start` and ending with the length of the input data
        dictionary.
        For example, you are trying to create/update a user's MFA attributes
        with the following data set:
        {
                "mf_enabled": true,
                "user_mf_enforcement_level": "OPTIONAL",
                "factor_type": "SMS"
        }
        this test would allow for you to test with the following data sets
        without needing to enumerate all of them in the data file
        [{u'mf_enabled': True}, {u'factor_type': u'SMS'},
         {u'user_mf_enforcement_level': u'OPTIONAL'},
         {u'mf_enabled': True, u'factor_type': u'SMS'},
         {u'mf_enabled': True, u'user_mf_enforcement_level': u'OPTIONAL'},
         {u'factor_type': u'SMS', u'user_mf_enforcement_level': u'OPTIONAL'},
         {u'mf_enabled': True, u'factor_type': u'SMS',
         u'user_mf_enforcement_level': u'OPTIONAL'}]
        :param start: value from 1 to len of dict + 1, set to 1 if out of range
        :param data_dict: take a dictionary
        :return: List of data sets
        """
        result_list = []
        if start is None:
            start = 1
        elif start < 1 | start > (len(data_dict) + 1):
            start = 1
        if data_dict:
            for i in range(start, len(data_dict) + 1):
                # Loop through list of combinations
                for ec in combinations(data_dict, i):
                    # create dict with key:value items for each combination
                    dic = {item: data_dict[item] for item in ec}
                    result_list.append(dic)
        return result_list

    @classmethod
    def remove_namespace(cls, doc, namespace):
        """Remove namespace in the passed document in place."""
        ns = u'{%s}' % namespace
        nsl = len(ns)
        for elem in doc.getiterator():
            for key in elem.attrib:
                if key.startswith(ns):
                    new_key = key[nsl:]
                    elem.attrib[new_key] = elem.attrib[key]
                    del elem.attrib[key]
            if elem.tag.startswith(ns):
                elem.tag = elem.tag[nsl:]

    @classmethod
    def tearDownClass(cls):
        """Deletes the added resources."""
        super(TestBase, cls).tearDownClass()


def skip_if_no_service_admin_available(func):
    """
    Function used to raise SkipTest if the service admin-level tests are not
    runnable.
    :param func: test method
    :return: wrapper
    """

    def wrapper(*args, **kwargs):
        test_config = config.TestConfig()
        if not test_config.run_service_admin_tests:
            import unittest
            raise unittest.SkipTest('Service Admin user is not available')
        return func(*args, **kwargs)
    return wrapper


def create_logger():
    teardown_log_file = os.environ['CAFE_TEST_LOG_PATH'] + '/tearDown.log'
    handler = logging.FileHandler(teardown_log_file)
    tearDown_logger = logging.getLogger('tearDown')
    tearDown_logger.setLevel(logging.WARNING)
    tearDown_logger.addHandler(handler)
    return tearDown_logger

logger = create_logger()


def log_tearDown_error(tearDown_method):

    def wrapper(*args, **kwargs):
        try:
            tearDown_method(*args, **kwargs)
        except AssertionError as e:
            # log the error
            msg = '{0}, {1}'.format(args, e)
            logger.warning(msg)
    return wrapper
