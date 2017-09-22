import os
import re
import subprocess


def check_datetime_format(datetime_str):
    """
    Helper function used by some of saml generator methods
    """
    return re.match(r'^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}Z$',
                    datetime_str)


def get_params_and_command(public_key_path, private_key_path=None,
                           fed_api=None, for_logout=False,
                           for_metadata=False):
    """
    Common logic for 'create_saml_assertion' and 'create_saml_logout'
    """
    java_exec_path = subprocess.check_output(['which', 'java']).strip()
    cur_path = os.path.abspath('.')
    tests_path = re.search('(.*)tests', cur_path)
    if not tests_path:
        src_path = cur_path
    else:
        src_path = tests_path.group(1)
    jar_path = os.path.join(
        src_path, 'tests', 'resources',
        'identity-saml-generator-2.0-1498166922926-all.jar')
    key_path = os.path.join(src_path, 'src',
                            'commonTest', 'resources')
    public_key_path = public_key_path or os.path.join(
        key_path, 'saml-qe-idp.crt')
    if not for_metadata:
        private_key_path = private_key_path or os.path.join(
            key_path, 'saml-qe-idp.pkcs8')

    if fed_api == 'v1':
        command_list = [
            java_exec_path, '-jar', jar_path, fed_api,
            '-publicKey', public_key_path, '-privateKey', private_key_path
        ]
    elif fed_api == 'v2':
        command_list = [
            java_exec_path, '-jar', jar_path, fed_api,
        ]

    if for_metadata:
        command_list = [
            java_exec_path, '-jar', jar_path, '-metadata', 'true',
            '-publicKey', public_key_path,
        ]
        return public_key_path, command_list

    if for_logout:
        command_list.extend(['-logout', 'true'])
    return public_key_path, private_key_path, command_list


def create_saml_assertion(
        domain=None, issuer=None, email=None, subject=None,
        credential_type='password', days_to_expiration=None,
        seconds_to_expiration=86400, issue_date_assertion_to_set=None,
        issue_date_response_to_set=None, expiration_date_to_set=None,
        auth_instant_date_to_set=None, default_date_to_set=None,
        roles=None, exclude_type_on_role_attribute_values=None,
        convert_type_to_any_on_role_attribute_values=None,
        exclude_type_on_domain_attribute_values=None,
        convert_type_to_any_on_domain_attribute_values=None,
        exclude_type_on_email_attribute_values=None,
        convert_type_to_any_on_email_attribute_values=None,
        public_key_path=None, private_key_path=None,
        base64_url_encode=False, response_flavor=None, output_format=None):
    """
    Changes were made to saml-generator.
    In pom.xml, slf4j-api changed to slf4j-simple
    In Main.java, added:
        org.apache.log4j.BasicConfigurator.configure(
            new org.apache.log4j.varia.NullAppender());
    ----

    issuer: The URI of the issuer for the saml assertion.
    subject: The username of the federated user.
    domain: The domain ID for the federated user.
    roles: A list of role names for the federated user.
    email: The email address of the federated user.
    public_key_path: The path to the location of the public key to
                     decrypt assertions.
    private_key_path: The path to the location of the private key to
                      use to sign assertions.
    days_to_expiration: How long before the assertion is no longer valid.
    """

    public_key_path, private_key_path, command_list = get_params_and_command(
        public_key_path=public_key_path, private_key_path=private_key_path,
        fed_api='v1')

    command_list.extend(['-credentialType', credential_type])

    if issue_date_assertion_to_set is not None:
        if not check_datetime_format(issue_date_assertion_to_set):
            raise ValueError('Datetime should be in the format of '
                             'yyyy-MM-dd\'T\'HH:mm:ss.SSSZ')
        command_list.extend([
            '-issueDateAssertionToSet', issue_date_assertion_to_set])
    if issue_date_response_to_set is not None:
        if not check_datetime_format(issue_date_response_to_set):
            raise ValueError('Datetime should be in the format of '
                             'yyyy-MM-dd\'T\'HH:mm:ss.SSSZ')
        command_list.extend([
            '-issueDateResponseToSet', issue_date_response_to_set])
    if expiration_date_to_set is not None:
        if not check_datetime_format(expiration_date_to_set):
            raise ValueError('Datetime should be in the format of '
                             'yyyy-MM-dd\'T\'HH:mm:ss.SSSZ')
        command_list.extend([
            '-expirationDateToSet', expiration_date_to_set])
    if auth_instant_date_to_set is not None:
        if not check_datetime_format(auth_instant_date_to_set):
            raise ValueError('Datetime should be in the format of '
                             'yyyy-MM-dd\'T\'HH:mm:ss.SSSZ')
        command_list.extend([
            '-authInstantDateToSet', auth_instant_date_to_set])
    if default_date_to_set is not None:
        if not check_datetime_format(default_date_to_set):
            raise ValueError('Datetime should be in the format of '
                             'yyyy-MM-dd\'T\'HH:mm:ss.SSSZ')
        command_list.extend([
            '-defaultDateToSet', default_date_to_set])

    if exclude_type_on_role_attribute_values is not None:
        command_list.extend([
            '-rolesAttrTypesExclude', ','.join(
                exclude_type_on_role_attribute_values)
        ])
    if convert_type_to_any_on_role_attribute_values is not None:
        command_list.extend([
            '-rolesAttrTypeAny',
            ','.join(convert_type_to_any_on_role_attribute_values)
        ])

    if exclude_type_on_domain_attribute_values is not None:
        command_list.extend([
            '-domainAttrTypesExclude',
            str(exclude_type_on_domain_attribute_values)
        ])
    if convert_type_to_any_on_domain_attribute_values is not None:
        command_list.extend([
            '-domainAttrTypeAny',
            str(convert_type_to_any_on_domain_attribute_values)
        ])

    if exclude_type_on_email_attribute_values is not None:
        command_list.extend([
            '-emailAttrTypesExclude',
            str(exclude_type_on_email_attribute_values)
        ])
    if convert_type_to_any_on_email_attribute_values is not None:
        command_list.extend([
            '-emailAttrTypeAny',
            str(convert_type_to_any_on_email_attribute_values)
        ])
    if subject is not None:
        command_list.extend(['-subject', subject])
    command_list.extend([
        '-samlAssertionExpirationSeconds', str(seconds_to_expiration)])
    if base64_url_encode:
        command_list.extend([
            '-base64URLEncode', 'true'
        ])
    if output_format is not None:
        command_list.extend(['-outputFormat', output_format])

    if domain is not None:
        command_list.extend(['-domain', str(domain)])
    if issuer is not None:
        command_list.extend(['-issuer', issuer])
    if email is not None:
        command_list.extend(['-email', email])
    if roles is not None:
        command_list.extend(['-roles', ','.join(roles)])
    if days_to_expiration is not None:
        command_list.extend([
            '-samlAssertionExpirationDays', str(days_to_expiration)])

    cert = subprocess.check_output(command_list,
                                   stderr=subprocess.STDOUT).strip()

    return cert


def create_saml_assertion_v2(
        domain=None, issuer=None, email=None, username=None,
        seconds_to_expiration=86400, roles=None, public_key_path=None,
        private_key_path=None, response_flavor=None, output_format=None):
    """
    New saml generator created for v2 federation auth.
    issuer: The URI of the issuer for the saml assertion.
    username: The username of the federated user.
    domain: The domain ID for the federated user.
    roles: A list of role names for the federated user.
    email: The email address of the federated user.
    response_flavor: Which SAML response to generate. Valid
        options are 'v2Broker' and 'v1Origin'. Default: v2DomainBroker
    output_format: The format to output response. xml, base64, form, or
      formEncode. Default: xml
    public_key_path: The path to the location of the public key to
                     decrypt assertions.
    private_key_path: The path to the location of the private key to
                      use to sign assertions.
    """

    public_key_path, private_key_path, command_list = get_params_and_command(
        public_key_path=public_key_path, private_key_path=private_key_path,
        fed_api='v2')

    if output_format is not None:
            command_list.extend(['-outputFormat={}'.format(output_format)])

    if domain is not None:
        command_list.extend(['-domain={}'.format(str(domain))])
    if email is not None:
        command_list.extend(['-email={}'.format(email)])
    if roles is not None:
        command_list.extend(['-roles={}'.format(','.join(roles))])
    if username is not None:
        command_list.extend(['-username={}'.format(username)])
    command_list.extend([
        '-tokenExpirationSeconds={}'.format(str(seconds_to_expiration))])

    if response_flavor:
        command_list.extend([
            '-responseFlavor={}'.format(response_flavor)])

    if response_flavor == 'v2DomainOrigin':
        command_list.extend(['-originPublicKey={}'.format(public_key_path)])
        command_list.extend(['-originPrivateKey={}'.format(private_key_path)])
        command_list.extend(['-originIssuer={}'.format(issuer)])

    cert = subprocess.check_output(command_list,
                                   stderr=subprocess.STDOUT).strip()

    return cert


def create_saml_logout(issuer=None, days_to_issue_instant=None,
                       seconds_to_issue_instant=86400,
                       issue_date_to_set=None, destination=None,
                       name_id=None, public_key_path=None,
                       private_key_path=None, include_key_info=True,
                       base64_url_encode=False):

    """
    Changes were made to saml-generator.
    In pom.xml, slf4j-api changed to slf4j-simple
    In Main.java, added:
        org.apache.log4j.BasicConfigurator.configure(
            new org.apache.log4j.varia.NullAppender());
    ----

    issuer: The URI of the issuer for the saml logout request.
    name_id: The username of the federated user.
    days_to_issue_instant: Number of days saml logout will be valid
    seconds_to_issue_instant: Number of seconds saml logout will be valid
    issue_date_to_set: saml logout issue date
    destination: destination for the saml logout
    public_key_path: The path to the location of the public key to
                     decrypt assertions.
    private_key_path: The path to the location of the private key to
                      use to sign assertions.
    include_key_info:
    """

    public_key_path, private_key_path, command_list = get_params_and_command(
        public_key_path=public_key_path, private_key_path=private_key_path,
        for_logout=True, fed_api='v1')

    if issuer is not None:
        command_list.extend(['-issuer', issuer])
    if days_to_issue_instant is not None:
        command_list.extend([
            '-logoutIssueInstantDaysToAdd', str(days_to_issue_instant)])
    command_list.extend([
        '-logoutIssueInstantSecondsToAdd', str(
            seconds_to_issue_instant)])
    if issue_date_to_set is not None:
        if not check_datetime_format(issue_date_to_set):
            raise ValueError('Datetime should be in the format of '
                             'yyyy-MM-dd\'T\'HH:mm:ss.SSSZ')
        command_list.extend([
            '-logoutIssueInstantDateToSet', issue_date_to_set])

    if destination is not None:
        command_list.extend([
            '-destination', destination
        ])
    if name_id is not None:
        command_list.extend([
            '-nameId', name_id
        ])
    if include_key_info is not None:
        if include_key_info is True:
            command_list.extend([
                '-includeKeyInfo', 'true'
            ])
        elif include_key_info is False:
            command_list.extend([
                '-includeKeyInfo', 'false'
            ])

    if base64_url_encode:
        command_list.extend([
            '-base64URLEncode', 'true'
        ])

    cert = subprocess.check_output(command_list).strip()

    return cert


def create_metadata(issuer=None, org_name=None, auth_url=None,
                    public_key_path=None):
    """
    This is a client to help generate metadata.xml for create IdP with
    metadata.xml
    """
    public_key_path, command_list = get_params_and_command(
        public_key_path=public_key_path, for_metadata=True)

    if issuer is not None:
        command_list.extend(['-issuer', issuer])

    if auth_url is not None:
        command_list.extend(['-authenticationUrl', auth_url])

    if org_name is not None:
        command_list.extend(['-orgName', org_name])

    metadata = subprocess.check_output(command_list,
                                       stderr=subprocess.STDOUT).strip()

    return metadata
