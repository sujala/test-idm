import json
from lxml import etree


from cafe.engine.models import base


XMLNS = "http://docs.openstack.org/identity/api/v2.0"
XMLNS_OS_KSADM = "http://docs.openstack.org/identity/api/ext/OS-KSADM/v1.0"
XMLNS_RAX_AUTH = "http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0"
XMLNS_RAX_KSGRP = "http://docs.rackspace.com/identity/api/ext/RAX-KSGRP/v1.0"
XMLNS_RAX_KSQA = "http://docs.rackspace.com/identity/api/ext/RAX-KSQA/v1.0"
XMLNS_RAX_KSKEY = "http://docs.rackspace.com/identity/api/ext/RAX-KSKEY/v1.0"


class AuthenticateWithApiKey(base.AutoMarshallingModel):
    """Marshalling for Authentications requests with API Key."""

    def __init__(self, user_name, api_key):
        self.user_name = user_name
        self.api_key = api_key

    def _obj_to_json(self):
        get_token_request = {
            "auth": {
                "RAX-KSKEY:apiKeyCredentials": {
                    "username": self.user_name,
                    "apiKey": self.api_key}
            }}
        return json.dumps(get_token_request)

    def _obj_to_xml(self):
        auth = etree.Element('auth')
        etree.SubElement(
            auth, 'apiKeyCredentials', xmlns=XMLNS_RAX_KSKEY,
            username=self.user_name, apiKey=self.api_key)
        return etree.tostring(auth)


class AuthenticateWithPassword(base.AutoMarshallingModel):
    """Marshalling for Authentications requests with Password."""
    def __init__(self, user_name, password):
        self.user_name = user_name
        self.password = password

    def _obj_to_json(self):
        get_token_request = {
            "auth": {
                "passwordCredentials": {
                    "username": self.user_name,
                    "password": self.password}
            }}
        return json.dumps(get_token_request)

    def _obj_to_xml(self):
        auth = etree.Element('auth')
        etree.SubElement(
            auth, 'passwordCredentials', xmlns=XMLNS,
            username=self.user_name, password=self.password)
        return etree.tostring(auth)


class AddUser(base.AutoMarshallingModel):
    """Marshalling for Add Identity Admin User Request."""

    def __init__(self, user_name, domain_id=None, contact_id=None,
                 default_region=None, token_format=None, password=None,
                 email=None, enabled=None, display_name=None):
        self.user_name = user_name
        self.domain_id = domain_id
        self.contact_id = contact_id
        self.default_region = default_region
        self.token_format = token_format
        self.password = password
        self.email = email
        self.enabled = enabled
        self.display_name = display_name

    def _obj_to_json(self):
        add_user_request = {
            "user": {"username": self.user_name}}
        if self.domain_id:
            add_user_request['user']['domainId'] = self.domain_id
        if self.contact_id:
            add_user_request['user']['contactId'] = self.contact_id
        if self.default_region:
            add_user_request['user']['defaultRegion'] = self.default_region
        if self.contact_id:
            add_user_request['user']['tokenFormat'] = self.token_format
        if self.password:
            add_user_request['user']['password'] = self.password
        if self.email:
            add_user_request['user']['email'] = self.email
        if self.enabled:
            add_user_request['user']['enabled'] = self.enabled
        if self.display_name:
            add_user_request['user']['display_name'] = self.display_name
        return json.dumps(add_user_request)

    def _obj_to_xml(self):
        # ET.register_namespace(
        #     'RAX-AUTH',
        #     'http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0')
        # ET.register_namespace('OS-KSADM', XMLNS_OS_KSADM)
        add_user_request = etree.Element(
            'user', xmlns=XMLNS,
            username=self.user_name)
        if self.email:
            add_user_request.set('email', self.email)
        if self.domain_id:
            add_user_request.attrib[
                etree.QName(XMLNS_RAX_AUTH, 'domainId')] = self.domain_id
        if self.contact_id:
            add_user_request.set('contactId', self.contact_id)
        if self.default_region:
            add_user_request.set('defaultRegion', self.default_region)
        if self.token_format:
            add_user_request.set('tokenFormat', self.token_format)
        if self.password:
            add_user_request.attrib[etree.QName(
                XMLNS_OS_KSADM, 'password')] = self.password
        if self.enabled:
            add_user_request.set('enabled', self.enabled)
        if self.display_name:
            add_user_request.set('display_name', self.display_name)
        return etree.tostring(add_user_request)


class AddRole(base.AutoMarshallingModel):
    """Marshalling for Add Role Request."""
    def __init__(self, role_name, role_id, role_description):
        self.role_name = role_name
        self.role_id = role_id
        self.role_description = role_description

    def _obj_to_json(self):
        add_role_request = {
            "role": {"name": self.role_name,
                     "id": self.role_id,
                     "description": self.role_description}}
        return json.dumps(add_role_request)

    def _obj_to_xml(self):
        etree.register_namespace(
            'RAX-AUTH',
            'http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0')
        add_role_request = etree.Element(
            'role', xmlns=XMLNS, id=self.role_id,
            name=self.role_name, description=self.role_description)
        return etree.tostring(add_role_request)
