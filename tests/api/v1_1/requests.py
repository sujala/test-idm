import json
from lxml import etree
from cafe.engine.models import base
from tests.package.johny import constants as const


class PasswordCredentials(base.AutoMarshallingModel):
    """Marshalling for Authentications requests with Password.
        Auth with username/password
    """

    def __init__(self, user_name, password):
        self.user_name = user_name
        self.password = password

    def _obj_to_json(self):
        get_token_request = {const.PASSWORD_CREDENTIALS: {
            const.USERNAME: self.user_name, const.PASSWORD: self.password}}
        return json.dumps(get_token_request)

    def _obj_to_xml(self):
        auth = etree.Element(const.PASSWORD_CREDENTIALS, xmlns=const.XMLNS_V11,
                             username=self.user_name, password=self.password)
        return etree.tostring(auth)


class Credentials(base.AutoMarshallingModel):
    """Marshalling for Authentications requests with API Key.
        Auth with username/apikey
    """
    def __init__(self, user_name, key):
        self.user_name = user_name
        self.key = key

    def _obj_to_json(self):
        get_token_request = {const.CREDENTIALS: {
            const.USERNAME: self.user_name, const.KEY: self.key}}
        return json.dumps(get_token_request)

    def _obj_to_xml(self):
        etree.register_namespace('v1', const.XMLNS_V11)
        auth = etree.Element('{' + const.XMLNS_V11 + '}' + const.CREDENTIALS,
                             username=self.user_name, key=self.key)
        return etree.tostring(auth)


class MossoCredentials(base.AutoMarshallingModel):
    def __init__(self, mosso_id, key):
        self.mosso_id = mosso_id
        self.key = key

    def _obj_to_json(self):
        get_token_request = {const.MOSSO_CREDENTIALS: {
            const.MOSSO_ID: self.mosso_id, const.KEY: self.key}}
        return json.dumps(get_token_request)

    def _obj_to_xml(self):
        etree.register_namespace('v1', const.XMLNS_V11)
        auth = etree.Element(const.MOSSO_CREDENTIALS)
        auth.set(const.XMLNS, const.XMLNS_V11)
        if self.mosso_id is not None:
            auth.set(const.MOSSO_ID, str(self.mosso_id))
        if self.key is not None:
            auth.set(const.KEY, str(self.key))
        return etree.tostring(auth)


class NastCredentials(base.AutoMarshallingModel):
    def __init__(self, nast_id, key):
        self.nast_id = nast_id
        self.key = key

    def _obj_to_json(self):
        get_token_request = {const.NAST_CREDENTIALS: {
            const.NAST_ID: self.nast_id, const.KEY: self.key}}
        return json.dumps(get_token_request)

    def _obj_to_xml(self):
        etree.register_namespace('v1', const.XMLNS_V11)
        auth = etree.Element(const.NAST_CREDENTIALS)
        auth.set(const.XMLNS, const.XMLNS_V11)
        if self.nast_id is not None:
            auth.set(const.NAST_ID, str(self.nast_id))
        if self.key is not None:
            auth.set(const.KEY, str(self.key))
        return etree.tostring(auth)


class User(base.AutoMarshallingModel):

    def __init__(self, id=None, key=None, mossoId=None, nastId=None,
                 enabled=None):
        self.id = id
        self.key = key
        self.mossoId = mossoId
        self.nastId = nastId
        self.enabled = enabled

    def _obj_to_json(self):
        ret = {const.ID: self.id, const.KEY: self.key,
               const.MOSSO_ID:  self.mossoId, const.NAST_ID: self.nastId,
               const.ENABLED: self.enabled}
        return json.dumps({const.USER: ret})

    def _obj_to_xml(self):
        element = etree.Element(const.USER)
        element.set(const.XMLNS, const.XMLNS_V11)
        if self.id is not None:
            element.set(const.ID, str(self.id))
        if self.key is not None:
            element.set(const.KEY, str(self.key))
        if self.mossoId is not None:
            element.set(const.MOSSO_ID, str(self.mossoId))
        if self.nastId is not None:
            element.set(const.NAST_ID, str(self.nastId))
        if self.enabled is not None:
            element.set(const.ENABLED, str(self.enabled).lower())
        return etree.tostring(element)
