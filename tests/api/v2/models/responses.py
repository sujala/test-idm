from tests.api import constants as const


class Tenant(object):

    def __init__(self, resp_json):

        tenant = resp_json[const.TENANT]
        self.domain_id = tenant.get(const.RAX_AUTH_DOMAIN_ID, None)
        self.enabled = tenant.get(const.ENABLED, None)
        self.id = tenant.get(const.ID, None)
        self.name = tenant.get(const.NAME, None)
        self.description = tenant.get(const.DESCRIPTION, None)
