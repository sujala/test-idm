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


class RoleAdd(base.AutoMarshallingModel):
    """Marshalling for Add Role Request."""

    def __init__(self, role_name, role_id, role_description):
        self.role_name = role_name
        self.role_id = role_id
        self.role_description = role_description

    def _obj_to_json(self):
        add_role_request = {
            const.ROLE: {const.NAME: self.role_name,
                         const.ID: self.role_id,
                         const.DESCRIPTION: self.role_description}}
        return json.dumps(add_role_request)

    def _obj_to_xml(self):
        etree.register_namespace(
            const.RAX_AUTH, const.XMLNS_RAX_AUTH)
        add_role_request = etree.Element(
            const.ROLE, xmlns=const.XMLNS, id=self.role_id,
            name=self.role_name, description=self.role_description)
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


class AddService(base.AutoMarshallingModel):

    """Marshalling for Add Service Request."""

    def __init__(self, service_name, service_id, service_type,
                 service_description=None):
        self.service_name = service_name
        self.service_id = service_id
        self.service_description = service_description
        self.service_type = service_type

    def _obj_to_json(self):
        add_service_request = {
            const.SERVICE: {
                const.SERVICE_NAME: self.service_name,
                const.ID: self.service_id,
                const.SERVICE_TYPE: self.service_type}}
        if self.service_description:
            add_service_request[const.SERVICE][const.DESCRIPTION] = (
                self.service_description)
        return json.dumps(add_service_request)

    def _obj_to_xml(self):
        etree.register_namespace('OS-KSADM', const.XMLNS_OS_KSADM)
        add_service_request = etree.Element(
            'service', xmlns=const.XMLNS, id=str(self.service_id),
            name=self.service_name, type=self.service_type)
        if self.service_description:
            add_service_request.set('description', self.service_description)
        return etree.tostring(add_service_request)


class AddTenant(base.AutoMarshallingModel):
    """Marshalling for add tenant"""
    # TODO: Add _obj_to_xml()

    def __init__(self, tenant_name, tenant_id=None, enabled=None,
                 description=None):
        self.tenant_name = tenant_name
        self.tenant_id = tenant_id
        self.enabled = enabled
        self.description = description

    def _obj_to_json(self):

        add_tenant_request = {
            "tenant": {
                "name": self.tenant_name,
            }
        }
        if self.tenant_id is not None:
            add_tenant_request["tenant"]["id"] = self.tenant_id
        if self.enabled is not None:
            add_tenant_request["tenant"]["enabled"] = self.enabled
        if self.description is not None:
            add_tenant_request["tenant"]["description"] = self.description
        return json.dumps(add_tenant_request)


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
