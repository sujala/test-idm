from tests.api.base import TestBase

from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests


def get_add_user_request_object(
        username=None, password=None, input_data=None):
    if not username:
        username = "testacct" + TestBase.generate_random_string(
            pattern=const.USER_NAME_PATTERN)
    if not input_data:
        domain_id = TestBase.generate_random_string(
            pattern=const.DOMAIN_PATTERN)
        input_data = {'email': 'test@avex.jp', 'domain_id': domain_id}

    return requests.UserAdd(user_name=username, **input_data)


def get_add_user_one_call_request_object(domainid=None, username=None):
        user_name = username or TestBase.generate_random_string(
            pattern='Username[\w]{12}')
        secret_q = 'Who Me?'
        secret_a = 'Yes You!'
        secret_qa = {
            const.SECRET_QUESTION: secret_q,
            const.SECRET_ANSWER: secret_a
        }
        domain_id = domainid or TestBase.generate_random_string(
            pattern=const.MOSSO_TENANT_ID_PATTERN)

        return requests.UserAdd(
            user_name=user_name,
            enabled=True,
            domain_id=domain_id,
            secret_qa=secret_qa
        )


def get_add_user_request_object_pull(user_name=None, enabled=True,
                                     domain_id=None, groups=None, roles=None,
                                     secret_q=None, email=None, secret_a=None):
    # Gather all args into dictionary
    input_data = locals()
    if secret_q and secret_a:
        input_data["secret_qa"] = {const.QUESTION: secret_q,
                                   const.ANSWER: secret_a}
    del input_data['secret_q']
    del input_data['secret_a']

    # create UserAdd request object
    default_data = {}
    default_data["user_name"] = "testacct"+TestBase.generate_random_string(
                                             pattern=const.USER_NAME_PATTERN)
    default_data["enabled"] = True
    default_data["domain_id"] = TestBase.generate_random_string(
                                    pattern=const.DOMAIN_PATTERN)
    default_data["email"] = "test@avex.jp"
    default_data["groups"] = []
    default_data["roles"] = []
    default_data["secret_qa"] = {"question": "When isn't it?",
                                 "answer": "When it is!!"}
    for k in list(input_data.keys()):
        if input_data[k] is not None:
            default_data[k] = input_data[k]
    return requests.UserAdd(**default_data)


def get_add_service_object(service_name=None, service_id=None,
                           service_type=None,
                           service_desc=None):
    """
    Generate ServiceAdd request object with random data
    """
    if not service_name:
        service_name = TestBase.generate_random_string(
            pattern=const.SERVICE_NAME_PATTERN
        )

    if not service_id:
        service_id = TestBase.generate_random_string(
            pattern=const.ID_PATTERN
        )

    if not service_type:
        service_type = TestBase.generate_random_string(
            pattern=const.SERVICE_TYPE_PATTERN
        )

    if not service_desc:
        service_desc = "cid test service"

    return requests.ServiceAdd(service_name=service_name,
                               service_id=service_id,
                               service_type=service_type,
                               service_description=service_desc)


def get_add_endpoint_template_object(template_id=None, name=None,
                                     template_type=None,
                                     service_id=None,
                                     assignment_type=None,
                                     input_data=None):
    """
    Generate EndpointTemplateAdd object
    """
    if not template_id:
        template_id = TestBase.generate_random_string(
            pattern=const.ID_PATTERN
        )

    if (not name or not template_type) and not service_id:
        name = "cloudServers"
        template_type = "compute"

    if service_id and not assignment_type:
        assignment_type = 'MOSSO'

    if not input_data:
        input_data = {
            "public_url": "https://www.test_public_endpoint_template.com",
            "internal_url": "https://www.test_internal_endpoint_template.com",
            "admin_url": "https://www.test_admin_endpoint_template.com",
            "version_info": "test_version_info",
            "version_id": "1",
            "version_list": "test_version_list",
            "region": "ORD"
        }

    return requests.EndpointTemplateAdd(template_id=template_id, name=name,
                                        template_type=template_type,
                                        service_id=service_id,
                                        assignment_type=assignment_type,
                                        **input_data)


def get_add_tenant_object(tenant_name=None, tenant_id=None, enabled=True,
                          description='Describing API Test Tenant',
                          display_name='API Test Displayed Name',
                          domain_id=None, tenant_types=None):
    if not tenant_name:
        tenant_name = TestBase.generate_random_string(
            pattern=const.TENANT_NAME_PATTERN)

    if not tenant_id:
        tenant_id = tenant_name

    return requests.Tenant(
        tenant_name=tenant_name, tenant_id=tenant_id, description=description,
        enabled=enabled, display_name=display_name, domain_id=domain_id,
        tenant_types=tenant_types)


def get_add_group_request_object(group_name=None, group_desc=None):
    # Gather all args into dictionary

    default_data = {}
    default_data["group_name"] = "newgroup" + TestBase.generate_random_string(
        pattern=const.LOWER_CASE_LETTERS)
    default_data["group_desc"] = "newgrpdesc" + \
        TestBase.generate_random_string(pattern=const.LOWER_CASE_LETTERS)
    if group_name:
        default_data["group_name"] = group_name
    if group_desc:
        default_data["group_desc"] = group_desc

    return requests.GroupAdd(**default_data)


def get_domain_request_object(domain_req):
    enabled = True

    if "enabled" in domain_req:
        enabled = domain_req["enabled"]
    if "domain_id" in domain_req:
        domain_id = domain_req["domain_id"]
    else:
        domain_id = TestBase.generate_random_string(const.ID_PATTERN)

    dom = requests.Domain(
        domain_name=TestBase.generate_random_string(const.DOMAIN_PATTERN),
        domain_id=domain_id,
        description=TestBase.generate_random_string(const.DOMAIN_PATTERN),
        enabled=enabled)

    return dom


def get_add_role_request_object(role_name=None, role_id=None,
                                role_description=None,
                                role_type=None,
                                tenant_types=None,
                                administrator_role=None,
                                assignment=None,
                                service_id=None):
    """Generate Basic RoleAdd Object"""

    if not role_name:
        role_name = TestBase.generate_random_string(
            pattern=const.ROLE_NAME_PATTERN)
    if not role_id:
        role_id = TestBase.generate_random_string(const.ID_PATTERN)
    if not role_description:
        role_description = "CID Test Role"

    return requests.RoleAdd(role_name=role_name, role_id=role_id,
                            role_type=role_type, tenant_types=tenant_types,
                            role_description=role_description,
                            administrator_role=administrator_role,
                            assignment=assignment,
                            service_id=service_id)


def get_add_tenant_request_object(tenant_name=None, tenant_types=None,
                                  enabled=None, domain_id=None,
                                  input_data=None):
    """Generate Basic Tenant Object"""

    if not tenant_name:
        tenant_name = TestBase.generate_random_string(
            pattern=const.TENANT_NAME_PATTERN)

    if enabled is None:
        enabled = True

    if not input_data:
        input_data = {
            "tenant_id": TestBase.generate_random_string(
                pattern=const.NUMBERS_PATTERN),
            "description": "Api test tenant",
            "display_name": "Api tenant display name"
        }
    return requests.Tenant(tenant_name=tenant_name, tenant_types=tenant_types,
                           enabled=enabled, domain_id=domain_id, **input_data)


def get_add_idp_request_object(name=None, issuer=None, description=None,
                               federation_type=None, authentication_url=None,
                               public_certificates=None,
                               email_domains=None,
                               approved_domain_group=None,
                               approved_domain_ids=None):
    if not name:
        name = TestBase.generate_random_string(const.IDP_NAME_PATTERN)
    # Use api key pattern, since it's like a guid
    if not issuer:
        issuer = TestBase.generate_random_string(const.API_KEY_PATTERN)
    if not description:
        description = TestBase.generate_random_string(const.DESC_PATTERN)
    if not federation_type:
        if approved_domain_ids or approved_domain_group:
            federation_type = const.DOMAIN.upper()
        else:
            federation_type = const.RACKER.upper()
    if not authentication_url:
        authentication_url = TestBase.generate_random_string(const.URL_PATTERN)
    return requests.IDP(idp_name=name, issuer=issuer, description=description,
                        federation_type=federation_type,
                        authentication_url=authentication_url,
                        public_certificates=public_certificates,
                        approved_domain_group=approved_domain_group,
                        approved_domain_ids=approved_domain_ids,
                        email_domains=email_domains)


def get_add_user_group_request(domain_id, group_name=None, description=None):
    if not group_name:
        group_name = TestBase.generate_random_string(
            pattern=const.USER_GROUP_NAME_PATTERN)
    if not description:
        description = TestBase.generate_random_string(
            pattern=const.DESC_PATTERN)
    return requests.domainUserGroup(group_name=group_name, domain_id=domain_id,
                                    description=description)
