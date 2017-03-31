#!/usr/bin/env python

"""
This has validation methods defined for different response headers.
The initial focus is on v2.0 api calls.
TODO : More methods are to be added for v1.0 & v1.1 calls, when we add
tests for v1.0 & v1.1 calls
"""

from tests.package.johny import constants as const


def validate_header_vary(value):
    """
    :param value: as a list
    :return:
    """
    header = 'vary'

    def validation(response):
        basic_header_validations(response=response, header=header)
        assert value in response.headers[header]

    return validation


def validate_header_server(response):

    header = 'Server'
    basic_header_validations(response=response, header=header)

    # TODO : Assert the content of this header. e.g. its pattern


def validate_header_date(response):

    header = 'Date'
    basic_header_validations(response=response, header=header)

    # TODO : Assert the content of this header. e.g. its pattern


def validate_header_content_type(response):

    header = 'Content-Type'
    basic_header_validations(response=response, header=header)

    # TODO : Assert the content of this header. e.g. its pattern


def validate_header_content_length(response):

    header = 'Content-Length'
    basic_header_validations(response=response, header=header)

    # TODO : Assert the content of this header. e.g. its pattern


def validate_header_location(response):

    header = 'Location'
    basic_header_validations(response=response, header=header)

    # TODO : Assert the content of this header. e.g. its pattern
    # This depends on the research spike related to CID-170


def validate_header_transfer_encoding(response):

    header = 'Transfer-Encoding'
    basic_header_validations(response=response, header=header)

    # TODO : Assert the content of this header. e.g. its pattern


def validate_header_access_control_allow_origin(value):
    header = 'access-control-allow-origin'

    def validation(response):
        basic_header_validations(response=response, header=header)
        assert response.headers[header] == value

    return validation


def validate_header_access_control_allow_methods(value):
    header = 'access-control-allow-methods'

    def validation(response):
        basic_header_validations(response=response, header=header)
        assert value in response.headers[header]

    return validation


def validate_header_access_control_allow_credentials(value):
    header = 'access-control-allow-credentials'

    def validation(response):
        basic_header_validations(response=response, header=header)
        assert response.headers[header] == value

    return validation


def validate_header_access_control_allow_headers(values):
    header = 'access-control-allow-headers'

    def validation(response):
        basic_header_validations(response=response, header=header)
        assert values in response.headers[header]

    return validation


def validate_header_origin(value):
    header = 'origin'

    def validation(response):
        basic_header_validations(response=response, header=header)
        assert response.headers[header] == value

    return validation


def basic_header_validations(response, header):

    assert header in response.headers, (
        "header %s not present in response" % header)
    assert response.headers[header] is not None, (
        "header %s is None" % header)
    assert response.headers[header].strip() != '', (
        "header %s is blank" % header)


def validate_header_not_present(unexpected_headers):

    def validation(response):
        for header in unexpected_headers:
            assert header not in response.headers, (
                "header %s should not be present in response" % header)

    return validation


def validate_expected_headers(expected_headers):

    def validation(response):
        for header in expected_headers:
            assert header in response.headers, (
                "header %s not in response headers" % header
            )
    return validation


def validate_header_tenant_id(value):
    header = const.X_TENANT_ID

    def validation(response):
        basic_header_validations(response=response, header=header)
        msg = 'Expected Tenant ID: {0}, Tenant ID in Response: {1}'
        assert(response.headers[header] == value), msg.format(
            value, response.headers[header])

    return validation

validate_username_header_not_present = \
    validate_header_not_present(const.X_USER_NAME)
validate_location_header_not_present = validate_header_not_present('Location')
validate_content_length_header_not_present = validate_header_not_present(
    'Content-Length')
validate_transfer_encoding_header_not_present = validate_header_not_present(
    'Transfer-Encoding')
validate_tenant_id_header_not_present = (
    validate_header_not_present(const.X_TENANT_ID))
