import collections

from tests.package.johny import constants as const


class Tenant(object):
    """Response object for Tenant."""

    def __init__(self, resp_json):

        tenant = resp_json[const.TENANT]
        self.domain_id = tenant.get(const.RAX_AUTH_DOMAIN_ID, None)
        self.enabled = tenant.get(const.ENABLED, None)
        self.id = tenant.get(const.ID, None)
        self.name = tenant.get(const.NAME, None)
        self.description = tenant.get(const.DESCRIPTION, None)


class Role(object):
    """Response object for Role."""

    def __init__(self, resp_json):

        role = resp_json[const.ROLE]
        self.id = role.get(const.ID, None)
        self.name = role.get(const.NAME, None)
        self.description = role.get(const.DESCRIPTION, None)


class User(object):
    """Response object for User."""

    def __init__(self, resp_json):

        user = resp_json[const.USER]
        self.id = user.get(const.ID, None)
        self.name = user.get(const.USERNAME, None)
        self.domain_id = user.get(const.RAX_AUTH_DOMAIN_ID, None)
        self.enabled = user.get(const.ENABLED, None)
        self.user_name = user.get(const.USERNAME, None)
        self.password = user.get(const.OS_KSADM_PASSWORD, None)
        self.email = user.get(const.EMAIL, None)

        roles = user.get(const.ROLES, [])
        role_tuple = collections.namedtuple(
            'Role', 'name tenant_id')
        self.roles = [
            role_tuple(name=item.get(const.NAME, None),
                       tenant_id=item.get(const.TENANT_ID, None))
            for item in roles]
        self.default_region = user.get(const.RAX_AUTH_DEFAULT_REGION, None)

        secret_qa = user.get(const.NS_SECRETQA, {})
        self.secret_qa = {}
        self.secret_qa['question'] = secret_qa.get(const.SECRET_QUESTION, None)
        self.secret_qa['answer'] = secret_qa.get(const.SECRET_ANSWER, None)
        self.default_region = user.get(const.RAX_AUTH_DEFAULT_REGION, None)


class Access(object):
    """Response object for Access."""

    def __init__(self, resp_json):

        access = resp_json[const.ACCESS]
        access_tuple = collections.namedtuple(
            'Access', 'token user service_catalog')

        token = access.get(const.TOKEN)
        token_id = token.get(const.ID, None)
        token_expires = token.get(const.EXPIRES, None)

        tenant = token.get(const.TENANT, {})
        tenant_id = tenant.get(const.ID, None)
        tenant_name = tenant.get(const.NAME, None)

        tenant_tuple = collections.namedtuple('Tenant', 'id name')
        tenant_tuple_obj = tenant_tuple(id=tenant_id, name=tenant_name)

        token_tuple = collections.namedtuple('Token', 'id expires tenant')
        token_tuple_obj = token_tuple(id=token_id, expires=token_expires,
                                      tenant=tenant_tuple_obj)

        user = access.get(const.USER)
        roles = user.get(const.ROLES, {})
        role_tuple = collections.namedtuple(
            'Role', 'description name id tenant_id')
        role_obj_array = [role_tuple(
            description=item.get(const.DESCRIPTION, None),
            name=item.get(const.NAME, None),
            id=item.get(const.ID, None),
            tenant_id=item.get(const.TENANT_ID, None)) for item in roles]

        user_tuple = collections.namedtuple(
            'User', 'default_region id name roles')
        user_default_region = user.get(const.DEFAULT_REGION, None)
        user_id = user.get(const.ID, None)
        user_name = user.get(const.NAME, None)

        user_obj = user_tuple(
            default_region=user_default_region, id=user_id, name=user_name,
            roles=role_obj_array)

        self.access = access_tuple(
            token=token_tuple_obj, user=user_obj, service_catalog={})


class Service(object):

    def __init__(self, resp_json):

        service = resp_json[const.NS_SERVICE]
        self.type = service.get(const.SERVICE_TYPE, None)
        self.id = service.get(const.ID, None)
        self.name = service.get(const.SERVICE_NAME, None)
        self.description = service.get(const.DESCRIPTION, None)


class EndpointTemplate(object):

    def __init__(self, resp_json):

        endpoint_template = resp_json[const.OS_KSCATALOG_ENDPOINT_TEMPLATE]
        self.template_type = endpoint_template.get(const.TYPE, None)
        self.name = endpoint_template.get(const.NAME, None)
        self.id = endpoint_template.get(const.ID, None)
        self.service_id = endpoint_template.get(const.SERVICE_ID, None)
        self.assignment_type = endpoint_template.get(const.ID, None)
        self.region = endpoint_template.get(const.REGION, None)
        self.public_url = endpoint_template.get(const.PUBLIC_URL, None)
        self.internal_url = endpoint_template.get(const.INTERNAL_URL, None)
        self.admin_url = endpoint_template.get(const.ADMIN_URL, None)
        self.tenant_alias = endpoint_template.get(const.TENANT_ALIAS, None)
        self.version_id = endpoint_template.get(const.VERSION_ID, None)
        self.version_info = endpoint_template.get(const.VERSION_INFO, None)
        self.version_list = endpoint_template.get(const.VERSION_LIST, None)
        self.global_attr = endpoint_template.get(const.GLOBAL, None)
        self.enabled = endpoint_template.get(const.ENABLED, None)
        self.default = endpoint_template.get(const.DEFAULT, None)


class TenantTypeToEndpointMappingRule(object):

    def __init__(self, resp_json):

        mapping_rule = resp_json[
            const.NS_TENANT_TYPE_TO_ENDPOINT_MAPPING_RULE]
        self.id = mapping_rule.get(const.ID, None)
        self.tenant_type = mapping_rule.get(const.TENANT_TYPE)
        self.endpoint_templates = mapping_rule.get(
            const.OS_KSCATALOG_ENDPOINT_TEMPLATES)
        self.description = mapping_rule.get(const.DESCRIPTION, None)
