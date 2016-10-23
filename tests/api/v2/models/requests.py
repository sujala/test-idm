import json
from lxml import etree
from cafe.engine.models import base
from tests.api import constants as const


class AuthenticateWithApiKey(base.AutoMarshallingModel):
    """Marshalling for Authentications requests with API Key."""

    def __init__(self, user_name, api_key):
        self.user_name = user_name
        self.api_key = api_key

    def _obj_to_json(self):
        get_token_request = {
            const.AUTH: {
                const.NS_API_KEY_CREDENTIALS: {
                    const.USERNAME: self.user_name,
                    const.API_KEY: self.api_key}
            }}
        return json.dumps(get_token_request)

    def _obj_to_xml(self):
        auth = etree.Element(const.AUTH)
        etree.SubElement(
            auth, const.API_KEY_CREDENTIALS, xmlns=const.XMLNS_RAX_KSKEY,
            username=self.user_name, apiKey=self.api_key)
        return etree.tostring(auth)


class AuthenticateWithPassword(base.AutoMarshallingModel):
    """Marshalling for Authentications requests with Password."""

    def __init__(self, user_name, password):
        self.user_name = user_name
        self.password = password

    def _obj_to_json(self):
        get_token_request = {
            const.AUTH: {
                const.PASSWORD_CREDENTIALS: {
                    const.USERNAME: self.user_name,
                    const.PASSWORD: self.password}
            }}
        return json.dumps(get_token_request)

    def _obj_to_xml(self):
        auth = etree.Element(const.AUTH)
        etree.SubElement(
            auth, const.PASSWORD_CREDENTIALS, xmlns=const.XMLNS,
            username=self.user_name, password=self.password)
        return etree.tostring(auth)


class TenantWithTokenAuth(base.AutoMarshallingModel):
    """Marshalling for Authentications requests as tenant with token."""

    def __init__(self, tenant_id, token_id):
        self.tenant_id = tenant_id
        self.token_id = token_id

    def _obj_to_json(self):
        tenant_with_token_auth = {
            const.AUTH: {
                const.TENANT_ID: self.tenant_id,
                const.TOKEN: {
                    const.ID: self.token_id}
            }}
        return json.dumps(tenant_with_token_auth)

    def _obj_to_xml(self):
        raise Exception("Not implemented yet")


class UserAdd(base.AutoMarshallingModel):
    """Marshalling for Add Identity Admin User Request."""

    def __init__(self, user_name, domain_id=None, contact_id=None,
                 default_region=None, token_format=None, password=None,
                 email=None, enabled=None, mf_enabled=None,
                 user_mf_enforcement_level=None, factor_type=None,
                 display_name=None, roles=None,
                 groups=None, secret_qa=None):
        self.user_name = user_name
        self.domain_id = domain_id
        self.contact_id = contact_id
        self.default_region = default_region
        self.token_format = token_format
        self.password = password
        self.email = email
        self.enabled = enabled
        self.mf_enabled = mf_enabled
        self.user_mf_enforcement_level = user_mf_enforcement_level
        self.factor_type = factor_type
        self.display_name = display_name
        self.roles = roles
        self.groups = groups
        self.secret_qa = secret_qa

    def _obj_to_json(self):
        add_user_request = {
            const.USER: {const.USERNAME: self.user_name}}
        if self.domain_id:
            add_user_request[const.USER][const.DOMAINID] = self.domain_id
        if self.contact_id:
            add_user_request[const.USER][const.CONTACTID] = self.contact_id
        if self.default_region:
            add_user_request[const.USER][const.DEFAULT_REGION] = (
                self.default_region)
        if self.contact_id:
            add_user_request[const.USER][const.TOKEN_FORMAT] = (
                self.token_format)
        if self.password:
            add_user_request[const.USER][const.PASSWORD] = self.password
        if self.email:
            add_user_request[const.USER][const.EMAIL] = self.email
        if self.enabled:
            add_user_request[const.USER][const.ENABLED] = self.enabled
        if self.mf_enabled:
            add_user_request[const.USER][
                const.RAX_AUTH_MULTI_FACTOR_ENABLED] = self.mf_enabled
        if self.user_mf_enforcement_level:
            add_user_request[const.USER][
                const.RAX_AUTH_USER_MULTI_FACTOR_ENFORCEMENT_LEVEL] = (
                self.user_mf_enforcement_level)
        if self.factor_type:
            add_user_request[const.USER][const.RAX_AUTH_FACTOR_TYPE] = (
                self.factor_type)
        if self.display_name:
            add_user_request[const.USER][const.DISPLAY_NAME] = (
                self.display_name)
        if self.roles:
            add_user_request[const.USER][const.ROLES] = self.roles
        if self.groups:
            add_user_request[const.USER][const.NS_GROUPS] = self.groups
        if self.secret_qa:
            add_user_request[const.USER][const.NS_SECRETQA] = self.secret_qa
        return json.dumps(add_user_request)

    def _obj_to_xml(self):
        # ET.register_namespace(
        #     'RAX-AUTH',
        #     'http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0')
        # ET.register_namespace('OS-KSADM', const.XMLNS_OS_KSADM)
        add_user_request = etree.Element(
            const.USER, xmlns=const.XMLNS,
            username=self.user_name)
        if self.email:
            add_user_request.set(const.EMAIL, self.email)
        if self.domain_id:
            add_user_request.attrib[
                etree.QName(const.XMLNS_RAX_AUTH, const.DOMAINID)] = (
                self.domain_id)
        if self.contact_id:
            add_user_request.set(const.CONTACTID, self.contact_id)
        if self.default_region:
            add_user_request.set(const.DEFAULT_REGION, self.default_region)
        if self.token_format:
            add_user_request.set(const.TOKEN_FORMAT, self.token_format)
        if self.password:
            add_user_request.attrib[etree.QName(
                const.XMLNS_OS_KSADM, const.PASSWORD)] = self.password
        if self.enabled:
            add_user_request.set(const.ENABLED, self.enabled)
        if self.mf_enabled:
            add_user_request.attrib[
                etree.QName(const.XMLNS_RAX_AUTH,
                            const.MULTI_FACTOR_ENABLED)] = self.mf_enabled
        if self.user_mf_enforcement_level:
            add_user_request.attrib[etree.QName(
                const.XMLNS_RAX_AUTH,
                const.USER_MULTI_FACTOR_ENFORCEMENT_LEVEL)] = (
                self.user_mf_enforcement_level)
        if self.factor_type:
            add_user_request.attrib[
                etree.QName(const.XMLNS_RAX_AUTH, const.FACTOR_TYPE)] = (
                self.factor_type)
        if self.display_name:
            add_user_request.set(const.DISPLAY_NAME, self.display_name)
        if self.roles:
            add_user_request.set(const.ROLES, self.roles)
        if self.groups:
            add_user_request.attrib[
                etree.QName(const.XMLNS_RAX_KSGRP,
                            const.GROUPS)] = self.groups
        if self.secret_qa:
            add_user_request.attrib[
                etree.QName(const.XMLNS_RAX_KSQA,
                            const.SECRET_QA)] = self.secret_qa
        return etree.tostring(add_user_request)


class UserUpdate(base.AutoMarshallingModel):
    """Marshalling for update user request.

        TODO: insert update user request xml part
    """

    def __init__(self, user_name=None, domain_id=None, contact_id=None,
                 default_region=None, token_format=None, password=None,
                 email=None, enabled=None, mfa_enabled=None,
                 mfa_state=None, mf_enforcement_level=None,
                 factor_type=None, display_name=None):

        self.user_name = user_name
        self.domain_id = domain_id
        self.contact_id = contact_id
        self.default_region = default_region
        self.token_format = token_format
        self.password = password
        self.email = email
        self.enabled = enabled
        self.mfa_enabled = mfa_enabled
        self.mfa_state = mfa_state
        self.mf_enforcement_level = mf_enforcement_level
        self.factor_type = factor_type
        self.display_name = display_name

    def _obj_to_json(self):
        update_user_request = {const.USER: {}}
        if self.user_name:
            update_user_request[const.USER][const.USERNAME] = self.user_name
        if self.domain_id:
            update_user_request[const.USER][const.DOMAINID] = self.domain_id
        if self.contact_id:
            update_user_request[const.USER][const.CONTACTID] = self.contact_id
        if self.default_region:
            update_user_request[const.USER][const.DEFAULT_REGION] = (
                self.default_region)
        if self.contact_id:
            update_user_request[const.USER][const.TOKEN_FORMAT] = (
                self.token_format)
        if self.password:
            update_user_request[const.USER][const.PASSWORD] = self.password
        if self.email:
            update_user_request[const.USER][const.EMAIL] = self.email
        if self.enabled is not None:
            update_user_request[const.USER][const.ENABLED] = self.enabled
        if self.mfa_enabled is not None:
            update_user_request[const.USER][
                const.RAX_AUTH_MULTI_FACTOR_ENABLED] = self.mfa_enabled
        if self.mfa_state:
            update_user_request[const.USER][
                const.RAX_AUTH_MULTI_FACTOR_STATE] = self.mfa_state
        if self.mf_enforcement_level:
            update_user_request[const.USER][
                const.RAX_AUTH_USER_MULTI_FACTOR_ENFORCEMENT_LEVEL] = (
                self.mf_enforcement_level
            )
        if self.factor_type:
            update_user_request[const.USER][const.RAX_AUTH_FACTOR_TYPE] = (
                self.factor_type
            )
        if self.display_name:
            update_user_request[const.USER][const.DISPLAY_NAME] = (
                self.display_name)
        return json.dumps(update_user_request)

        # TODO: insert update user request xml part


#  TODO: insert update user request xml part
class PasswordCredentialsAdd(base.AutoMarshallingModel):
    """Marshalling for Add Credentials Request"""
    def __init__(self, username, password):
        self.username = username
        self.password = password

    def _obj_to_json(self):
        postdata = {const.PASSWORD_CREDENTIALS: {
            const.USERNAME: self.username,
            const.PASSWORD: self.password}}
        return json.dumps(postdata)

    def _obj_to_xml(self):
        raise Exception("Not implemented yet")


class ApiKeyCredentialsUpdate(base.AutoMarshallingModel):
    """Marshalling for Update Api key Request"""
    def __init__(self, username, apikey):
        self.username = username
        self.apikey = apikey

    def _obj_to_json(self):
        postdata = {const.NS_API_KEY_CREDENTIALS: {
            const.USERNAME: self.username,
            const.API_KEY: self.apikey}}
        return json.dumps(postdata)

    def _obj_to_xml(self):
        raise Exception("Not implemented yet")


class GroupAdd(base.AutoMarshallingModel):
    """Marshalling for Adding a group"""
    def __init__(self, group_name, group_desc):
        self.group_name = group_name
        self.group_desc = group_desc

    def _obj_to_json(self):
        return json.dumps({const.NS_GROUP: {
            const.NAME: self.group_name,
            const.DESCRIPTION: self.group_desc}})

    def _obj_to_xml(self):
        raise Exception("Not implemented yet")


class UserUpgrade(base.AutoMarshallingModel):
    """Marshalling for Upgrade User Request."""
    def __init__(self, user_id, domain_id, secret_q, secret_a, groups, roles):
        self.user_id = user_id
        self.domain_id = domain_id
        self.secret_q = secret_q
        self.secret_a = secret_a
        self.groups = groups
        self.roles = roles

    def _obj_to_json(self):
        return json.dumps(
            {const.USER: {
                const.ID: self.user_id,
                const.RAX_AUTH_DOMAIN_ID: self.domain_id,
                const.SECRET_QA: {
                    const.SECRET_QUESTION: self.secret_q,
                    const.SECRET_ANSWER: self.secret_a},
                const.GROUPS: self.groups,
                const.ROLES: self.roles}})

    def _obj_to_xml(self):
        raise Exception("Not implemented yet")


class RoleAdd(base.AutoMarshallingModel):
    """Marshalling for Add Role Request."""

    def __init__(self, role_name, role_id=None, role_description=None,
                 administrator_role=None, service_id=None):
        self.role_name = role_name
        self.role_id = role_id
        self.role_description = role_description
        self.administrator_role = administrator_role
        self.service_id = service_id

    def _obj_to_json(self):
        add_role_request = {
            const.ROLE: {const.NAME: self.role_name}
        }
        if self.role_id:
            add_role_request[const.ROLE][const.ID] = self.role_id
        if self.role_description:
            add_role_request[const.ROLE][const.DESCRIPTION] = (
                self.role_description)
        if self.service_id:
            add_role_request[const.ROLE][const.SERVICE_ID] = self.service_id
        if self.administrator_role:
            add_role_request[const.ROLE][const.ADMINISTRATOR_ROLE] = (
                self.administrator_role
            )
        return json.dumps(add_role_request)

    def _obj_to_xml(self):
        etree.register_namespace(
            const.RAX_AUTH, const.XMLNS_RAX_AUTH)
        add_role_request = etree.Element(
            const.ROLE, xmlns=const.XMLNS, id=self.role_id,
            name=self.role_name, description=self.role_description,
            serviceId=self.service_id
        )
        return etree.tostring(add_role_request)


class EndpointTemplateAdd(base.AutoMarshallingModel):

    """Marshalling for Add Endpoint Template Request."""

    def __init__(self, template_id, region, template_type=None,
                 name=None, service_id=None, assignment_type=None,
                 public_url=None, internal_url=None, admin_url=None,
                 tenant_alias=None, version_id=None, version_info=None,
                 version_list=None, global_attr=None, enabled=None,
                 default=None):
        self.id = template_id
        self.region = region
        self.template_type = template_type
        self.name = name
        self.service_id = service_id
        self.assignment_type = assignment_type
        self.public_url = public_url
        self.internal_url = internal_url
        self.admin_url = admin_url
        self.tenant_alias = tenant_alias
        self.version_id = version_id
        self.version_info = version_info
        self.version_list = version_list
        self.global_attr = global_attr
        self.enabled = enabled
        self.default = default

    def _obj_to_json(self):
        add_endpoint_template_request = {
            const.OS_KSCATALOG_ENDPOINT_TEMPLATE: {
                const.ID: self.id
            }
        }
        if self.region:
            add_endpoint_template_request[
                const.OS_KSCATALOG_ENDPOINT_TEMPLATE][
                const.REGION] = self.region
        if self.name:
            add_endpoint_template_request[
                const.OS_KSCATALOG_ENDPOINT_TEMPLATE][
                const.SERVICE_NAME] = self.name
        if self.template_type:
            add_endpoint_template_request[
                const.OS_KSCATALOG_ENDPOINT_TEMPLATE][const.SERVICE_TYPE] = (
                self.template_type)
        if self.service_id:
            add_endpoint_template_request[
                const.OS_KSCATALOG_ENDPOINT_TEMPLATE][
                const.SERVICE_ID] = self.service_id
        if self.assignment_type:
            add_endpoint_template_request[
                const.OS_KSCATALOG_ENDPOINT_TEMPLATE][
                const.RAX_AUTH_ASSIGNMENT_TYPE] = self.assignment_type
        if self.public_url:
            add_endpoint_template_request[
                const.OS_KSCATALOG_ENDPOINT_TEMPLATE][
                const.PUBLIC_URL] = self.public_url
        if self.internal_url:
            add_endpoint_template_request[
                const.OS_KSCATALOG_ENDPOINT_TEMPLATE][
                const.INTERNAL_URL] = self.internal_url
        if self.admin_url:
            add_endpoint_template_request[
                const.OS_KSCATALOG_ENDPOINT_TEMPLATE][const.ADMIN_URL] = (
                self.admin_url)
        if self.version_id:
            add_endpoint_template_request[
                const.OS_KSCATALOG_ENDPOINT_TEMPLATE][const.VERSION_ID] = (
                self.version_id)
        if self.version_info:
            add_endpoint_template_request[
                const.OS_KSCATALOG_ENDPOINT_TEMPLATE][const.VERSION_INFO] = (
                self.version_info)
        if self.version_list:
            add_endpoint_template_request[
                const.OS_KSCATALOG_ENDPOINT_TEMPLATE][const.VERSION_LIST] = (
                self.version_list)
        if self.global_attr:
            add_endpoint_template_request[
                const.OS_KSCATALOG_ENDPOINT_TEMPLATE][const.GLOBAL] = (
                self.global_attr)
        if self.default:
            add_endpoint_template_request[
                const.OS_KSCATALOG_ENDPOINT_TEMPLATE][const.DEFAULT] = (
                self.default)
        if self.enabled:
            add_endpoint_template_request[
                const.OS_KSCATALOG_ENDPOINT_TEMPLATE][const.ENABLED] = (
                self.enabled)
        return json.dumps(add_endpoint_template_request)

    def _obj_to_xml(self):
        etree.register_namespace(const.OS_KSADM_NAMESPACE,
                                 const.XMLNS_OS_KSADM)
        add_endpoint_template_request = etree.Element(
            const.ENDPOINT_TEMPLATE, xmlns=const.XMLNS, id=str(self.id),
            name=self.name, publicURL=self.public_url, type=self.template_type,
            internalURL=self.internal_url, adminURL=self.admin_url,
            versionId=self.version_id, versionInfo=self.version_info,
            versionList=self.version_list, global_attr=self.global_attr,
            default=self.default, enabled=self.enabled, region=self.region,
            service_id=self.service_id, assignment_type=self.assignment_type)
        return etree.tostring(add_endpoint_template_request)


class EndpointTemplateUpdate(base.AutoMarshallingModel):

    """Marshalling for Update Endpoint Template Request."""

    def __init__(self, template_id, region=None, template_type=None,
                 name=None, service_id=None, assignment_type=None,
                 public_url=None, internal_url=None, admin_url=None,
                 tenant_alias=None, version_id=None, version_info=None,
                 version_list=None, global_attr=None, enabled=None,
                 default=None):
        self.id = template_id
        self.region = region
        self.template_type = template_type
        self.name = name
        self.service_id = service_id
        self.assignment_type = assignment_type
        self.public_url = public_url
        self.internal_url = internal_url
        self.admin_url = admin_url
        self.tenant_alias = tenant_alias
        self.version_id = version_id
        self.version_info = version_info
        self.version_list = version_list
        self.global_attr = global_attr
        self.enabled = enabled
        self.default = default

    def _obj_to_json(self):
        add_endpoint_template_request = {
            const.OS_KSCATALOG_ENDPOINT_TEMPLATE: {
                const.ID: self.id
            }
        }
        if self.region:
            add_endpoint_template_request[
                const.OS_KSCATALOG_ENDPOINT_TEMPLATE][
                const.REGION] = self.region
        if self.name:
            add_endpoint_template_request[
                const.OS_KSCATALOG_ENDPOINT_TEMPLATE][
                const.SERVICE_NAME] = self.name
        if self.template_type:
            add_endpoint_template_request[
                const.OS_KSCATALOG_ENDPOINT_TEMPLATE][const.SERVICE_TYPE] = (
                self.template_type)
        if self.service_id:
            add_endpoint_template_request[
                const.OS_KSCATALOG_ENDPOINT_TEMPLATE][
                const.SERVICE_ID] = self.service_id
        if self.assignment_type:
            add_endpoint_template_request[
                const.OS_KSCATALOG_ENDPOINT_TEMPLATE][
                const.RAX_AUTH_ASSIGNMENT_TYPE] = self.assignment_type
        if self.public_url:
            add_endpoint_template_request[
                const.OS_KSCATALOG_ENDPOINT_TEMPLATE][
                const.PUBLIC_URL] = self.public_url
        if self.internal_url:
            add_endpoint_template_request[
                const.OS_KSCATALOG_ENDPOINT_TEMPLATE][
                const.INTERNAL_URL] = self.internal_url
        if self.admin_url:
            add_endpoint_template_request[
                const.OS_KSCATALOG_ENDPOINT_TEMPLATE][const.ADMIN_URL] = (
                self.admin_url)
        if self.version_id:
            add_endpoint_template_request[
                const.OS_KSCATALOG_ENDPOINT_TEMPLATE][const.VERSION_ID] = (
                self.version_id)
        if self.version_info:
            add_endpoint_template_request[
                const.OS_KSCATALOG_ENDPOINT_TEMPLATE][const.VERSION_INFO] = (
                self.version_info)
        if self.version_list:
            add_endpoint_template_request[
                const.OS_KSCATALOG_ENDPOINT_TEMPLATE][const.VERSION_LIST] = (
                self.version_list)
        if self.global_attr:
            add_endpoint_template_request[
                const.OS_KSCATALOG_ENDPOINT_TEMPLATE][const.GLOBAL] = (
                self.global_attr)
        if self.default:
            add_endpoint_template_request[
                const.OS_KSCATALOG_ENDPOINT_TEMPLATE][const.DEFAULT] = (
                self.default)
        if self.enabled:
            add_endpoint_template_request[
                const.OS_KSCATALOG_ENDPOINT_TEMPLATE][const.ENABLED] = (
                self.enabled)
        return json.dumps(add_endpoint_template_request)

    def _obj_to_xml(self):
        etree.register_namespace(const.OS_KSADM_NAMESPACE,
                                 const.XMLNS_OS_KSADM)
        update_endpoint_template_request = etree.Element(
            const.ENDPOINT_TEMPLATE, xmlns=const.XMLNS, id=str(self.id),
            name=self.name, publicURL=self.public_url, type=self.template_type,
            internalURL=self.internal_url, adminURL=self.admin_url,
            versionId=self.version_id, versionInfo=self.version_info,
            versionList=self.version_list, global_attr=self.global_attr,
            default=self.default, enabled=self.enabled, region=self.region,
            service_id=self.service_id, assignment_type=self.assignment_type)
        return etree.tostring(update_endpoint_template_request)


class ServiceAdd(base.AutoMarshallingModel):
    """Marshalling for Add Service Request."""

    def __init__(self, service_name, service_id, service_type,
                 service_description=None):
        self.service_name = service_name
        self.service_id = service_id
        self.service_description = service_description
        self.service_type = service_type

    def _obj_to_json(self):
        add_service_request = {
            const.NS_SERVICE: {
                const.SERVICE_NAME: self.service_name,
                const.ID: self.service_id,
                const.SERVICE_TYPE: self.service_type}}
        if self.service_description:
            add_service_request[const.NS_SERVICE][const.DESCRIPTION] = (
                self.service_description)
        return json.dumps(add_service_request)

    def _obj_to_xml(self):
        etree.register_namespace(const.OS_KSADM_NAMESPACE,
                                 const.XMLNS_OS_KSADM)
        add_service_request = etree.Element(
            const.SERVICE, xmlns=const.XMLNS, id=str(self.service_id),
            name=self.service_name, type=self.service_type)
        if self.service_description:
            add_service_request.set(
                const.DESCRIPTION, self.service_description)
        return etree.tostring(add_service_request)


class AddEndpointToTenant(base.AutoMarshallingModel):

    """ Marshalling for add endpoint to tenant"""
    # TODO: Add _obj_to_xml()

    ROOT_TAG = const.OS_KSCATALOG_ENDPOINT_TEMPLATE

    def __init__(self, endpoint_template_id):
        self.endpoint_template_id = endpoint_template_id

    def _obj_to_json(self):
        add_endpoint_to_tenant_request = {
            self.ROOT_TAG: {}
        }
        if self.endpoint_template_id is not None:
            add_endpoint_to_tenant_request[self.ROOT_TAG][const.ID] = (
                self.endpoint_template_id
            )
        return json.dumps(add_endpoint_to_tenant_request)


class Tenant(base.AutoMarshallingModel):
    """Marshalling for Add/ Update Tenant Request."""
    def __init__(self, tenant_name, tenant_id, description=None, enabled=None,
                 display_name=None, domain_id=None):
        self.tenant_name = tenant_name
        self.tenant_id = tenant_id
        self.domain_id = domain_id
        self.description = description
        self.enabled = enabled
        self.display_name = display_name

    def _obj_to_json(self):
        add_tenant_request = {
            const.TENANT: {const.NAME: self.tenant_name}}
        if self.tenant_id:
            add_tenant_request[const.TENANT][const.ID] = self.tenant_id
        if self.description:
            add_tenant_request[const.TENANT][const.DESCRIPTION] = (
                self.description)
        if self.enabled:
            add_tenant_request[const.TENANT][const.ENABLED] = self.enabled
        elif self.enabled is False:
            add_tenant_request[const.TENANT][const.ENABLED] = False
        if self.display_name:
            add_tenant_request[const.TENANT][const.DISPLAY_NAME] = (
                self.display_name)
        return json.dumps(add_tenant_request)

    def _obj_to_xml(self):
        add_tenant_request = etree.Element(
            const.TENANT, xmlns=const.XMLNS)
        if self.tenant_name:
            add_tenant_request.set(const.NAME, self.tenant_name)
        if self.tenant_id:
            add_tenant_request.set(const.ID, self.tenant_id)
        if self.description:
            desc = etree.SubElement(add_tenant_request, const.DESCRIPTION)
            desc.text = self.description
        if self.enabled:
            if self.enabled.lower() == 'true':
                add_tenant_request.set(const.ENABLED, 'true')
            else:
                add_tenant_request.set(const.ENABLED, 'false')
        if self.display_name:
            add_tenant_request.set(const.DISPLAY_NAME, self.display_name)


class Domain(base.AutoMarshallingModel):
    """Marshalling for Add/ Update Tenant Request."""
    # TODO: Add _obj_to_xml()

    def __init__(self, domain_name, domain_id=None,
                 description=None, enabled=True):
        self.domain_name = domain_name
        self.domain_id = domain_id or domain_name
        self.description = description
        self.enabled = enabled

    def _obj_to_json(self):
        add_domain_request = {const.RAX_AUTH_DOMAIN: {
            const.NAME: self.domain_name}}
        if self.domain_id:
            add_domain_request[const.RAX_AUTH_DOMAIN][const.ID] = (
                self.domain_id)
        if self.description:
            add_domain_request[const.RAX_AUTH_DOMAIN][const.DESCRIPTION] = (
                self.description)
        if self.enabled:
            add_domain_request[const.RAX_AUTH_DOMAIN][const.ENABLED] = (
                self.enabled)
        elif self.enabled is False:
            add_domain_request[const.RAX_AUTH_DOMAIN][const.ENABLED] = False
        return json.dumps(add_domain_request)


class TenantTypeToEndpointMappingRule(base.AutoMarshallingModel):
    """Marshalling for Tenant Type to Endpoint Mapping Rule Request."""
    # TODO: Add _obj_to_xml()

    def __init__(self, tenant_type, endpoint_ids, description=None):
        self.tenant_type = tenant_type
        self.description = description
        self.endpoint_ids = endpoint_ids

    def _obj_to_json(self):
        tenant_type_to_endpoint_mapping_rule_request = {
            const.NS_TENANT_TYPE_TO_ENDPOINT_MAPPING_RULE: {
                const.TENANT_TYPE: self.tenant_type
            }
        }
        if self.description is not None:
            tenant_type_to_endpoint_mapping_rule_request[
                const.NS_TENANT_TYPE_TO_ENDPOINT_MAPPING_RULE][
                const.DESCRIPTION] = self.description
        if self.endpoint_ids is not None:
            tenant_type_to_endpoint_mapping_rule_request[
                const.NS_TENANT_TYPE_TO_ENDPOINT_MAPPING_RULE][
                const.OS_KSCATALOG_ENDPOINT_TEMPLATES] = []
            for endpoint_id in self.endpoint_ids:
                endpoint_dict = {
                    const.ID: endpoint_id
                }
                tenant_type_to_endpoint_mapping_rule_request[
                    const.NS_TENANT_TYPE_TO_ENDPOINT_MAPPING_RULE][
                    const.OS_KSCATALOG_ENDPOINT_TEMPLATES].append(
                        endpoint_dict)
        return json.dumps(tenant_type_to_endpoint_mapping_rule_request)
