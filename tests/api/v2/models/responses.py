import collections

from tests.api import constants as const


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
