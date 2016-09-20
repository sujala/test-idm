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
