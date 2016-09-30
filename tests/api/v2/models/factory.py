from tests.api.v2.models import requests
from tests.api import constants as const
from tests.api.base import TestBase


def get_add_user_request_object(username=None, input_data=None):
    # Create UserAdd request object
    if not username:
        username = "testacct" + TestBase.generate_random_string(
                                             pattern=const.USER_NAME_PATTERN)
    if not input_data:
        domain_id = TestBase.generate_random_string(
                                    pattern=const.DOMAIN_PATTERN)
        input_data = {'email': 'test@avex.jp', 'domain_id': domain_id}
    return requests.UserAdd(user_name=username, **input_data)


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
            pattern=const.SERVICE_ID_PATTERN
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
                                     input_data=None):
    """
    Generate EndpointTemplateAdd object
    """
    if not template_id:
        template_id = TestBase.generate_random_string(
            pattern='[\d]{8}'
        )

    if not name and not service_id:
        name = "cloudServers"
        template_type = "compute"

    if not input_data:
        input_data = {
            "public_url": "https://www.test_public_endpoint_template.com",
            "internal_url": "https://www.test_internal_endpoint_template.com",
            "admin_url": "https://www.test_admin_endpoint_template.com",
            "version_info": "test_version_info",
            "version_id": "1",
            "version_list": "test_version_list",
            "region": "ORD",
            "assignment_type": "MOSSO"
        }

    return requests.EndpointTemplateAdd(template_id=template_id, name=name,
                                        template_type=template_type,
                                        service_id=service_id,
                                        **input_data)
