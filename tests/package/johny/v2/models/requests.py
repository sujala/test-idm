import json
from lxml import etree
from cafe.engine.models import base
from ... import constants as const


class AuthenticateWithApiKey(base.AutoMarshallingModel):
    """Marshalling for Authentications requests with API Key.
        Tenant_id and tenant_name as optional (either or none but not both)
    """

    def __init__(self, user_name, api_key, tenant_id=None, tenant_name=None):
        self.user_name = user_name
        self.api_key = api_key
        self.tenant_id = tenant_id
        self.tenant_name = tenant_name

    def _obj_to_json(self):
        get_token_request = {const.AUTH: {const.NS_API_KEY_CREDENTIALS: {
            const.USERNAME: self.user_name, const.API_KEY: self.api_key}}}
        if self.tenant_id:
            get_token_request[const.AUTH][const.TENANT_ID] = self.tenant_id
        if self.tenant_name:
            get_token_request[const.AUTH][const.TENANT_NAME] = self.tenant_name
        return json.dumps(get_token_request)

    def _obj_to_xml(self):
        auth = etree.Element(const.AUTH)
        etree.SubElement(
            auth, const.API_KEY_CREDENTIALS, xmlns=const.XMLNS_RAX_KSKEY,
            username=self.user_name, apiKey=self.api_key)
        if self.tenant_id:
            etree.SubElement(auth, tenantId=self.tenant_id)
        if self.tenant_name:
            etree.SubElement(auth, tenantName=self.tenant_name)
        return etree.tostring(auth)


class AuthenticateWithPassword(base.AutoMarshallingModel):
    """Marshalling for Authentications requests with Password.
        Auth with with username/password
        Tenant_id and tenant_name as optional (either or none but not both)
    """

    def __init__(self, user_name, password, tenant_id=None, tenant_name=None):
        self.user_name = user_name
        self.password = password
        self.tenant_id = tenant_id
        self.tenant_name = tenant_name

    def _obj_to_json(self):
        get_token_request = {const.AUTH: {const.PASSWORD_CREDENTIALS: {
            const.USERNAME: self.user_name, const.PASSWORD: self.password}}}
        if self.tenant_id:
            get_token_request[const.AUTH][const.TENANT_ID] = self.tenant_id
        if self.tenant_name:
            get_token_request[const.AUTH][const.TENANT_NAME] = self.tenant_name
        return json.dumps(get_token_request)

    def _obj_to_xml(self):
        auth = etree.Element(const.AUTH)
        if self.tenant_id:
            etree.SubElement(auth, tenantId=self.tenant_id)
        if self.tenant_name:
            etree.SubElement(auth, tenantName=self.tenant_name)
        etree.SubElement(
            auth, const.PASSWORD_CREDENTIALS, xmlns=const.XMLNS_V20,
            username=self.user_name, password=self.password)
        return etree.tostring(auth)


class AuthenticateAsTenantWithToken(base.AutoMarshallingModel):
    """Marshalling for Authentications requests as tenant with token.
        auth token with either tenant_id of tenant_name or none but noth both
    """
    def __init__(self, token_id, tenant_id=None, tenant_name=None):
        self.token_id = token_id
        self.tenant_id = tenant_id
        self.tenant_name = tenant_name

    def _obj_to_json(self):
        tenant_with_token_auth = {const.AUTH: {
            const.TOKEN: {const.ID: self.token_id}}}

        if self.tenant_id:
            tenant_with_token_auth[const.AUTH][const.TENANT_ID] = (
                self.tenant_id
            )
        if self.tenant_name:
            tenant_with_token_auth[const.AUTH][const.TENANT_NAME] = (
                self.tenant_name
            )
        return json.dumps(tenant_with_token_auth)

    def _obj_to_xml(self):
        raise Exception("Not implemented yet")


class AuthenticateWithMFA(base.AutoMarshallingModel):
    """Marshalling for Authentication request with MFA cred"""
    def __init__(self, pass_code, tenant_id=None, tenant_name=None):
        self.pass_code = pass_code
        self.tenant_id = tenant_id
        self.tenant_name = tenant_name

    def _obj_to_json(self):
        mfa_auth_request = {
            const.AUTH: {
                const.NS_PASSCODE_CREDENTIALS: {
                    const.PASSCODE: self.pass_code
                }
            }
        }
        if self.tenant_id:
            mfa_auth_request[const.AUTH][const.TENANT_ID] = self.tenant_id
        if self.tenant_name:
            mfa_auth_request[const.AUTH][const.TENANT_NAME] = self.tenant_name
        return json.dumps(mfa_auth_request)

    # TODO: add xml obj


class AuthenticateAsRackerWithPassword(base.AutoMarshallingModel):
    """Marshalling for Authentications requests with Password, for Rackers.
        Auth with with username/password
    """

    def __init__(self, racker_name, racker_password):
        self.racker_name = racker_name
        self.racker_password = racker_password

    def _obj_to_json(self):
        get_token_request = {const.AUTH: {const.PASSWORD_CREDENTIALS: {
            const.USERNAME: self.racker_name,
            const.PASSWORD: self.racker_password},
            const.RAX_AUTH_DOMAIN: {
                const.NAME: const.RACKSPACE_DOMAIN}
        }}
        return json.dumps(get_token_request)

    def _obj_to_xml(self):
        auth = etree.Element(const.AUTH)
        etree.SubElement(
            auth, const.PASSWORD_CREDENTIALS, xmlns=const.XMLNS_V20,
            username=self.racker_name, password=self.racker_password)
        etree.SubElement(
            auth, const.RAX_AUTH_DOMAIN, xmlns=const.XMLNS_V20,
            name=const.RACKSPACE_DOMAIN)
        return etree.tostring(auth)


class AuthenticateWithDelegationAgreement(base.AutoMarshallingModel):
    """Marshalling for Authentication with DelegationAgreement."""

    def __init__(self, token, delegation_agreement_id):
        self.token = token
        self.delegation_agreement_id = delegation_agreement_id

    def _obj_to_json(self):
        get_token_request = {const.AUTH: {
            const.RAX_AUTH_DELEGATION_CREDENTIALS: {
                const.TOKEN: self.token,
                const.DELEGATION_AGREEMENT_ID: self.delegation_agreement_id}
        }}
        return json.dumps(get_token_request)

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
            add_user_request[const.USER][const.DOMAIN_ID] = self.domain_id
        if self.contact_id:
            add_user_request[const.USER][const.CONTACTID] = self.contact_id
        if self.default_region:
            add_user_request[const.USER][const.DEFAULT_REGION] = (
                self.default_region)
        if self.token_format:
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
            const.USER, xmlns=const.XMLNS_V20,
            username=self.user_name)
        if self.email:
            add_user_request.set(const.EMAIL, self.email)
        if self.domain_id:
            add_user_request.attrib[
                etree.QName(const.XMLNS_RAX_AUTH, const.DOMAIN_ID)] = (
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
        if self.enabled or self.enabled is None:
            add_user_request.set(const.ENABLED, 'true')
        else:
            add_user_request.set(const.ENABLED, 'false')

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
            update_user_request[const.USER][const.DOMAIN_ID] = self.domain_id
        if self.contact_id:
            update_user_request[const.USER][const.CONTACTID] = self.contact_id
        if self.default_region:
            update_user_request[const.USER][const.DEFAULT_REGION] = (
                self.default_region)
        if self.token_format:
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

    def __init__(self, role_name, role_id=None,
                 role_type=None, tenant_types=None, role_description=None,
                 administrator_role=None, service_id=None, assignment=None,
                 propagate=None):
        self.role_name = role_name
        self.role_id = role_id
        self.role_type = role_type
        self.tenant_types = tenant_types
        self.role_description = role_description
        self.administrator_role = administrator_role
        self.service_id = service_id
        self.assignment = assignment
        self.propagate = propagate

    def _obj_to_json(self):
        add_role_request = {
            const.ROLE: {const.NAME: self.role_name}
        }
        if self.role_id:
            add_role_request[const.ROLE][const.ID] = self.role_id
        if self.role_type:
            add_role_request[const.ROLE][const.RAX_AUTH_ROLE_TYPE] = (
                self.role_type)
        if self.tenant_types:
            add_role_request[const.ROLE][const.NS_TYPES] = (
                self.tenant_types)
        if self.role_description:
            add_role_request[const.ROLE][const.DESCRIPTION] = (
                self.role_description)
        if self.service_id:
            add_role_request[const.ROLE][const.SERVICE_ID] = self.service_id
        if self.assignment:
            add_role_request[const.ROLE][const.RAX_AUTH_ASSIGNMENT] = (
                self.assignment)
        if self.propagate is not None:
            add_role_request[const.ROLE][const.RAX_AUTH_PROPAGATE] = (
                self.propagate)
        if self.administrator_role:
            add_role_request[const.ROLE][const.ADMINISTRATOR_ROLE] = (
                self.administrator_role
            )
        return json.dumps(add_role_request)

    def _obj_to_xml(self):
        etree.register_namespace(
            const.RAX_AUTH, const.XMLNS_RAX_AUTH)
        add_role_request = etree.Element(
            const.ROLE, xmlns=const.XMLNS_V20, id=self.role_id,
            name=self.role_name, type=self.role_type,
            tenantTypes=self.tenant_types,
            description=self.role_description,
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
            const.ENDPOINT_TEMPLATE, xmlns=const.XMLNS_V20, id=str(self.id),
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
        if self.global_attr is not None:
            add_endpoint_template_request[
                const.OS_KSCATALOG_ENDPOINT_TEMPLATE][const.GLOBAL] = (
                self.global_attr)
        if self.default is not None:
            add_endpoint_template_request[
                const.OS_KSCATALOG_ENDPOINT_TEMPLATE][const.DEFAULT] = (
                self.default)
        if self.enabled is not None:
            add_endpoint_template_request[
                const.OS_KSCATALOG_ENDPOINT_TEMPLATE][const.ENABLED] = (
                self.enabled)
        return json.dumps(add_endpoint_template_request)

    def _obj_to_xml(self):
        etree.register_namespace(const.OS_KSADM_NAMESPACE,
                                 const.XMLNS_OS_KSADM)
        update_endpoint_template_request = etree.Element(
            const.ENDPOINT_TEMPLATE, xmlns=const.XMLNS_V20, id=str(self.id),
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
            const.SERVICE, xmlns=const.XMLNS_V20, id=str(self.service_id),
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
                 tenant_types=None, display_name=None, domain_id=None):
        self.tenant_name = tenant_name
        self.tenant_id = tenant_id
        self.domain_id = domain_id
        self.description = description
        self.enabled = enabled
        self.tenant_types = tenant_types
        self.display_name = display_name

    def _obj_to_json(self):
        add_tenant_request = {
            const.TENANT: {const.NAME: self.tenant_name}}
        if self.tenant_id:
            add_tenant_request[const.TENANT][const.ID] = self.tenant_id
        if self.description:
            add_tenant_request[const.TENANT][const.DESCRIPTION] = (
                self.description)
        if self.domain_id:
            add_tenant_request[const.TENANT][const.RAX_AUTH_DOMAIN_ID] = (
                self.domain_id)
        if self.enabled:
            add_tenant_request[const.TENANT][const.ENABLED] = self.enabled
        elif self.enabled is False:
            add_tenant_request[const.TENANT][const.ENABLED] = False
        if self.tenant_types is not None:  # Because [] needs to send
            add_tenant_request[const.TENANT][const.NS_TYPES] = (
                self.tenant_types)
        if self.display_name:
            add_tenant_request[const.TENANT][const.DISPLAY_NAME] = (
                self.display_name)
        return json.dumps(add_tenant_request)

    def _obj_to_xml(self):
        add_tenant_request = etree.Element(
            const.TENANT, xmlns=const.XMLNS_V20)
        if self.tenant_name:
            add_tenant_request.set(const.NAME, self.tenant_name)
        if self.tenant_id:
            add_tenant_request.set(const.ID, self.tenant_id)
        if self.description:
            desc = etree.SubElement(add_tenant_request, const.DESCRIPTION)
            desc.text = self.description
        if self.domain_id:
            add_tenant_request.set(const.RAX_AUTH_DOMAIN_ID, self.domain_id)
        if self.enabled:
            if self.enabled.lower() == 'true':
                add_tenant_request.set(const.ENABLED, 'true')
            else:
                add_tenant_request.set(const.ENABLED, 'false')

        # TODO update tenant types when have docment

        if self.display_name:
            add_tenant_request.set(const.DISPLAY_NAME, self.display_name)


class TenantType(base.AutoMarshallingModel):
    """Marshalling for Add/ Update Tenant Type Request."""
    # TODO: Add _obj_to_xml()

    def __init__(self, name, description):
        self.name = name
        self.description = description

    def _obj_to_json(self):
        add_tenant_type_request = {
            const.RAX_AUTH_TENANT_TYPE: {
                const.NAME: self.name,
                const.DESCRIPTION: self.description
            }
        }
        return json.dumps(add_tenant_type_request)


class Domain(base.AutoMarshallingModel):
    """Marshalling for Add/ Update Tenant Request."""
    # TODO: Add _obj_to_xml()

    def __init__(self, domain_name=None, domain_id=None,
                 description=None, rcn=None, enabled=True):
        self.domain_name = domain_name
        self.domain_id = domain_id or domain_name
        self.description = description
        self.enabled = enabled
        self.rcn = rcn

    def _obj_to_json(self):
        add_domain_request = {const.RAX_AUTH_DOMAIN: {
            const.NAME: self.domain_name}}
        if self.domain_id:
            add_domain_request[const.RAX_AUTH_DOMAIN][const.ID] = (
                self.domain_id)
        if self.description:
            add_domain_request[const.RAX_AUTH_DOMAIN][const.DESCRIPTION] = (
                self.description)
        if self.rcn:
            add_domain_request[const.RAX_AUTH_DOMAIN][const.RCN_LONG] = (
                self.rcn)
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


class OTPDeviceAdd(base.AutoMarshallingModel):
    """Marshalling for Add OTP Devive request"""
    def __init__(self, device_name):
        self.device_name = device_name

    def _obj_to_json(self):
        add_otp_request = {
            const.NS_OTP_DEVICE: {}
        }
        if self.device_name is not None:
            add_otp_request[const.NS_OTP_DEVICE][const.NAME] = self.device_name
        return json.dumps(add_otp_request)


class OTPDeviceVerify(base.AutoMarshallingModel):
    """Marshalling for Verify OTP Device request"""
    def __init__(self, code):
        self.code = code

    def _obj_to_json(self):
        add_otp_request = {const.NS_VERIFICATION_CODE: {
            const.CODE: self.code}}
        return json.dumps(add_otp_request)


class MFAUpdate(base.AutoMarshallingModel):
    """Marshalling for MFA Update request"""
    def __init__(self, enabled=None, mfa_enforce_level=None, unlock=None):
        self.enabled = enabled
        self.mfa_enforce_level = mfa_enforce_level
        self.unlock = unlock

    def _obj_to_json(self):
        update_mfa_request = {const.RAX_AUTH_MULTI_FACTOR: {}}
        if self.enabled is not None:
            update_mfa_request[const.RAX_AUTH_MULTI_FACTOR][const.ENABLED] = (
                self.enabled
            )
        if self.mfa_enforce_level:
            update_mfa_request[const.RAX_AUTH_MULTI_FACTOR][
                const.USER_MULTI_FACTOR_ENFORCEMENT_LEVEL] = (
                self.mfa_enforce_level
            )
        if self.unlock is not None:
            update_mfa_request[const.RAX_AUTH_MULTI_FACTOR][const.UNLOCK] = (
                self.unlock
            )
        return json.dumps(update_mfa_request)


class PublicCertificate(base.AutoMarshallingModel):
    """Marshalling for Add Public Certificate to IDP Request"""

    def __init__(self, public_certificate=None):
        self.public_certificate = public_certificate

    def _obj_to_json(self):
        associate_cert_request = {
            const.PUBLIC_CERTIFICATES: {
                const.PEM_ENCODED: self.public_certificate}
        }

        return json.dumps(associate_cert_request)


class IDP(base.AutoMarshallingModel):
    """Marshalling for Create IDP Request."""

    def __init__(self, idp_id=None, idp_name=None, issuer=None,
                 description=None, federation_type=None,
                 authentication_url=None, public_certificates=None,
                 approved_domain_group=None, approved_domain_ids=None,
                 enabled=None, email_domains=None):
        self.idp_id = idp_id
        self.idp_name = idp_name
        self.issuer = issuer
        self.description = description
        self.federation_type = federation_type
        self.authentication_url = authentication_url
        self.public_certificates = public_certificates
        self.approved_domain_group = approved_domain_group
        self.approved_domain_ids = approved_domain_ids
        self.enabled = enabled
        self.email_domains = email_domains

    def _obj_to_json(self):
        # So we don't have a bunch of 80 col issues.
        IDP = const.NS_IDENTITY_PROVIDER
        create_idp_request = {
            IDP: {}
        }
        if self.idp_name:
            create_idp_request[IDP][const.NAME] = self.idp_name
        if self.idp_id:
            create_idp_request[IDP][const.ID] = self.idp_id
        if self.issuer:
            create_idp_request[IDP][const.ISSUER] = self.issuer
        if self.description:
            create_idp_request[IDP][const.DESCRIPTION] = self.description
        if self.email_domains:
            create_idp_request[IDP][const.EMAIL_DOMAINS] = self.email_domains
        if self.federation_type:
            create_idp_request[IDP][const.FEDERATION_TYPE] = (
                self.federation_type)
        if self.authentication_url:
            create_idp_request[IDP][const.AUTHENTICATION_URL] = (
                self.authentication_url)
        if self.public_certificates:
            create_idp_request[IDP][const.PUBLIC_CERTIFICATES] = []
            # shorten to meet 80 col limit
            idpr = create_idp_request[IDP][const.PUBLIC_CERTIFICATES]

            for cert in self.public_certificates:
                idpr.append({
                    const.PEM_ENCODED: cert})
        if self.approved_domain_group:
            create_idp_request[IDP][const.APPROVED_DOMAIN_GROUP] = (
                self.approved_domain_group)
        if self.approved_domain_ids:
            create_idp_request[IDP][const.APPROVED_DOMAIN_Ids] = []
            # shorten name so we can be under 80 columns
            idpr = create_idp_request[IDP]
            for dom_id in self.approved_domain_ids:
                idpr[const.APPROVED_DOMAIN_Ids].append(dom_id)
        if self.enabled is not None:
            create_idp_request[IDP][const.ENABLED] = self.enabled

        return json.dumps(create_idp_request)


class IDPMetadata(base.AutoMarshallingModel):
    """Marshalling for Create IDP with Metadata Requests."""

    def __init__(self, metadata):
        self.metadata = metadata

    def _obj_to_xml(self):
        root = etree.XML(self.metadata)
        return etree.tostring(root)


class ChangePassword(base.AutoMarshallingModel):
    """
    Marshalling for change password request
    """
    def __init__(self, user_name, current_password, new_password):
        self.user_name = user_name
        self.current_password = current_password
        self.new_password = new_password

    def _obj_to_json(self):
        change_password_request = {
            const.RAX_AUTH_CHANGE_PASSWORD_CREDENTIALS: {
                const.USERNAME: self.user_name,
                const.PASSWORD: self.current_password,
                const.NEW_PASSWORD: self.new_password}
        }

        return json.dumps(change_password_request)


class PasswordPolicy(base.AutoMarshallingModel):
    """
    Marshalling for password policy requests
    """
    def __init__(self, duration=None, history_restriction=None):
        self.duration = duration
        self.history_restriction = history_restriction

    def _obj_to_json(self):
        password_policy_request = {
            const.PASSWORD_POLICY: {
                const.PASSWORD_DURATION: self.duration,
                const.PASSWORD_HISTORY_RESTRICTION: self.history_restriction}
        }
        return json.dumps(password_policy_request)


class ImpersonateUser(base.AutoMarshallingModel):
    """
    Marshalling for impersonation request
    """
    def __init__(self, user_name, idp=None, expire_in_seconds=None):
        self.user_name = user_name
        self.expire_in_seconds = expire_in_seconds
        self.idp = idp

    def _obj_to_json(self):

        IMPERSONATION = const.NS_IMPERSONATION
        impersonation_request = {
            IMPERSONATION: {const.USER: {const.USERNAME: self.user_name}}
        }
        if self.expire_in_seconds:
            impersonation_request[IMPERSONATION][
                const.EXPIRE_IN_SECONDS] = self.expire_in_seconds

        if self.idp:
            impersonation_request[
                IMPERSONATION][const.USER][const.NS_FEDERATED_IDP] = self.idp
        return json.dumps(impersonation_request)


class DevopsProp(base.AutoMarshallingModel):
    def __init__(self, prop_name=None, prop_value=None, prop_description=None,
                 prop_version=None, prop_reloadable=None, prop_searchable=None,
                 prop_value_type=None):
        kwargs = locals()
        del kwargs['self']
        for key in kwargs:
            setattr(self, key, kwargs[key])

    def _obj_to_json(self):
        return json.dumps({const.IDENTITY_PROPERTY: {
            const.NAME: self.prop_name,
            const.VALUE: self.prop_value,
            const.VALUE_TYPE: self.prop_value_type,
            const.DESCRIPTION: self.prop_description,
            const.IDM_VERSION: self.prop_version,
            const.RELOADABLE: self.prop_reloadable,
            const.SEARCHABLE: self.prop_searchable}})

    def _obj_to_xml(self):
        raise Exception("Not implemented yet")


class DomainAdministratorChange(base.AutoMarshallingModel):
    def __init__(self, promote_user_id, demote_user_id):
        self.promote_user_id = promote_user_id
        self.demote_user_id = demote_user_id

    def _obj_to_json(self):
        domain_admin_change_request = {
            const.DOMAIN_ADMIN_CHANGE: {
                const.PROMOTE_USER_ID: self.promote_user_id,
                const.DEMOTE_USER_ID: self.demote_user_id
            }
        }

        return json.dumps(domain_admin_change_request)

    def _obj_to_xml(self):
        raise Exception("Not implemented yet")


class domainUserGroup(base.AutoMarshallingModel):
    def __init__(self, group_name=None, domain_id=None, description=None):
        self.group_name = group_name
        self.description = description
        self.domain_id = domain_id

    def _obj_to_json(self):
        domain_user_group_request = {
            const.RAX_AUTH_USER_GROUP: {
                const.NAME: self.group_name,
                const.DESCRIPTION: self.description,
            }
        }
        if self.domain_id:
            domain_user_group_request[
                const.RAX_AUTH_USER_GROUP][const.DOMAIN_ID] = self.domain_id
        return json.dumps(domain_user_group_request)

    def _obj_to_xml(self):
        raise Exception("Not implemented yet")


class TenantRoleAssignments(base.AutoMarshallingModel):
    def __init__(self, *tenant_assignments):
        self.tenant_assignments = tenant_assignments

    def _obj_to_json(self):
        tenant_role_assignments_request = {
            const.RAX_AUTH_ROLE_ASSIGNMENTS: {
                const.TENANT_ASSIGNMENTS: list(self.tenant_assignments)
            }
        }
        return json.dumps(tenant_role_assignments_request)


class DelegationAgreements(base.AutoMarshallingModel):
    def __init__(self, da_name, principal_id=None,
                 principal_type=None, description=None,
                 allow_sub_agreements=None, sub_agreement_nest_level=None,
                 parent_da_id=None):
        self.da_name = da_name
        self.description = description
        self.principal_id = principal_id
        self.principal_type = principal_type
        self.allow_sub_agreements = allow_sub_agreements
        self.sub_agreement_nest_level = sub_agreement_nest_level
        self.parent_da_id = parent_da_id

    def _obj_to_json(self):
        delegation_agreement_request = {
            const.RAX_AUTH_DELEGATION_AGREEMENT: {
                const.NAME: self.da_name,
                const.DESCRIPTION: self.description
            }
        }
        if self.principal_id:
            delegation_agreement_request[const.RAX_AUTH_DELEGATION_AGREEMENT][
                const.PRINCIPAL_ID] = self.principal_id
        if self.principal_type:
            delegation_agreement_request[const.RAX_AUTH_DELEGATION_AGREEMENT][
                const.PRINCIPAL_TYPE] = self.principal_type
        if self.allow_sub_agreements is not None:
            delegation_agreement_request[const.RAX_AUTH_DELEGATION_AGREEMENT][
                const.ALLOW_SUB_AGREEMENTS] = self.allow_sub_agreements
        if self.sub_agreement_nest_level is not None:
            delegation_agreement_request[const.RAX_AUTH_DELEGATION_AGREEMENT][
                const.SUBAGREEMENT_NEST_LEVEL] = self.sub_agreement_nest_level
        if self.parent_da_id is not None:
            delegation_agreement_request[const.RAX_AUTH_DELEGATION_AGREEMENT][
                const.PARENT_DELEGATION_AGREEMENT_ID] = self.parent_da_id
        return json.dumps(delegation_agreement_request)
