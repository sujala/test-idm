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
                 display_name=None):
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
        return json.dumps(add_user_request)

    def _obj_to_xml(self):
        # ET.register_namespace(
        #     'RAX-AUTH',
        #     'http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0')
        # ET.register_namespace('OS-KSADM', XMLNS_OS_KSADM)
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
        update_user_request = {'user': {}}
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
