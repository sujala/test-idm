#!/usr/bin/env python

"""
This has validation methods defined for different response headers.
The initial focus is on v2.0 api calls.
TODO : More methods are to be added for v1.0 & v1.1 calls, when we add
tests for v1.0 & v1.1 calls
"""


def validate_header_vary(response):

    header = 'Vary'
    basic_header_validations(response=response, header=header)

    # TODO : Assert the content of this header. e.g. its pattern


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


validate_location_header_not_present = validate_header_not_present('Location')
validate_content_length_header_not_present = validate_header_not_present(
    'Content-Length')
validate_transfer_encoding_header_not_present = validate_header_not_present(
    'Transfer-Encoding')
