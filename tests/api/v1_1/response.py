from lxml import etree
import json
from cafe.engine.models import base
from tests.api.base import TestBase as test_base
from tests.package.johny import constants as const


class Auth(base.AutoMarshallingModel):

    ROOT_TAG = const.AUTH

    def __init__(self, token=None, serviceCatalog=None):
        super(Auth, self).__init__()
        self.token = token
        self.serviceCatalog = serviceCatalog

    @classmethod
    def _json_to_obj(cls, serialized_str):
        ret = json.loads(serialized_str)
        return cls._dict_to_obj(ret.get(cls.ROOT_TAG))

    @classmethod
    def _dict_to_obj(cls, dic):
        if Token.ROOT_TAG in dic:
            dic[Token.ROOT_TAG] = Token(**dic.get(Token.ROOT_TAG))
        if ServiceCatalog.ROOT_TAG in dic:
            dic[ServiceCatalog.ROOT_TAG] = (
                ServiceCatalog._dict_to_obj(dic.get(ServiceCatalog.ROOT_TAG)))
        return Auth(**dic)

    @classmethod
    def _xml_to_obj(cls, serialized_str):
        element = etree.fromstring(serialized_str)
        test_base.remove_namespace(element, const.XMLNS_V11)
        if element.tag != cls.ROOT_TAG:
            return None
        return cls._xml_ele_to_obj(element)

    @classmethod
    def _xml_ele_to_obj(cls, xml_ele):
        kwargs = {}
        token = xml_ele.find(Token.ROOT_TAG)
        service_catalog = xml_ele.find(ServiceCatalog.ROOT_TAG)
        if token is not None:
            kwargs[const.TOKEN] = Token._xml_ele_to_obj(token)
        if service_catalog is not None:
            kwargs[const.SERVICE_CATALOG] = ServiceCatalog._xml_ele_to_obj(
                service_catalog)
        return Auth(**kwargs)


class Token(base.AutoMarshallingModel):

    ROOT_TAG = const.TOKEN

    def __init__(self, id=None, expires=None):
        super(Token, self).__init__()
        self.id = id
        self.expires = expires

    @classmethod
    def _xml_ele_to_obj(cls, xml_ele):
        kwargs = {const.ID: xml_ele.get(const.ID), const.EXPIRES: xml_ele.get(
            const.EXPIRES)}
        return Token(**kwargs)


class ServiceCatalog(base.AutoMarshallingModel):

    ROOT_TAG = const.SERVICE_CATALOG

    def __init__(self, serviceCatalog=None):
        super(ServiceCatalog, self).__init__()
        for service in serviceCatalog:
            self.append_service(service)

    def get(self, service_name):
        for service in self:
            if service.name == service_name:
                return service

    @classmethod
    def _json_to_obj(cls, serialized_str):
        ret = json.loads(serialized_str)
        ret = cls._dict_to_obj(ret.get(cls.ROOT_TAG))
        return ret

    @classmethod
    def _dict_to_obj(cls, dic):
        ret = {cls.ROOT_TAG: [Service._dict_to_obj(service_name=service,
                                                   endpoints=dic.get(service))
                              for service in dic]}
        return ServiceCatalog(**ret)

    @classmethod
    def _xml_to_obj(cls, serialized_str):
        element = etree.fromstring(serialized_str)
        test_base.remove_namespace(element, const.XMLNS_V11)
        if element.tag != cls.ROOT_TAG:
            return None
        return cls._xml_ele_to_obj(element)

    @classmethod
    def _xml_ele_to_obj(cls, xml_ele):
        kwargs = {}
        xml_services = xml_ele.findall(Service.ROOT_TAG)
        services = [Service._xml_ele_to_obj(xml_service)
                    for xml_service in xml_services]
        kwargs[cls.ROOT_TAG] = services
        return ServiceCatalog(**kwargs)

    @classmethod
    def append_service(cls, service=None):
        service_list = []
        service_list.append(service)
        return service_list


class Service(base.AutoMarshallingModel):

    ROOT_TAG = const.SERVICE

    def __init__(self, name=None, endpoints=None):
        super(Service, self).__init__()
        self.name = name
        self.endpoints = endpoints

    @classmethod
    def _dict_to_obj(cls, service_name, endpoints):
        endpoints = EndpointList._list_to_obj(endpoints)
        kwargs = {const.NAME: service_name, const.ENDPOINTS: endpoints}
        return Service(**kwargs)

    @classmethod
    def _xml_ele_to_obj(cls, xml_ele):
        kwargs = {const.NAME: xml_ele.get(const.NAME)}
        xml_endpoints = xml_ele.findall(Endpoint.ROOT_TAG)
        kwargs[const.ENDPOINTS] = EndpointList._xml_ele_to_obj(xml_endpoints)
        return Service(**kwargs)


class EndpointList(base.AutoMarshallingModel):

    ROOT_TAG = const.ENDPOINTS

    def __init__(self, endpoints=None):
        super(EndpointList, self).__init__()
        for ep in endpoints:
            self.append_endpoint(ep)

    @classmethod
    def _list_to_obj(cls, list_):
        kwargs = {cls.ROOT_TAG: [Endpoint(**ep) for ep in list_]}
        return EndpointList(**kwargs)

    @classmethod
    def _xml_ele_to_obj(cls, list_):
        endpoints = [Endpoint._xml_ele_to_obj(xml_endpoint)
                     for xml_endpoint in list_]
        kwargs = {cls.ROOT_TAG: endpoints}
        return EndpointList(**kwargs)

    @classmethod
    def append_endpoint(cls, endpoint=None):
        endpoint_list = []
        endpoint_list.append(endpoint)
        return endpoint_list


class Endpoint(base.AutoMarshallingModel):

    ROOT_TAG = const.ENDPOINT

    def __init__(self, region=None, v1Default=None, publicURL=None,
                 internalURL=None, **kwargs):
        super(Endpoint, self).__init__()
        self.region = region
        self.v1Default = v1Default
        self.publicURL = publicURL
        self.internalURL = internalURL

    @classmethod
    def _xml_ele_to_obj(cls, xml_ele):
        kwargs = {const.REGION: xml_ele.get(const.REGION),
                  const.PUBLIC_URL: xml_ele.get(const.PUBLIC_URL),
                  const.INTERNAL_URL: xml_ele.get(const.INTERNAL_URL)}
        if xml_ele.get(const.V1_DEFAULT) is not None:
            kwargs[const.V1_DEFAULT] = (
                json.loads(xml_ele.get(const.V1_DEFAULT).lower()))
        return Endpoint(**kwargs)


class User(base.AutoMarshallingModel):

    ROOT_TAG = const.USER

    def __init__(self, id=None, key=None, mossoId=None, nastId=None,
                 enabled=None, baseURLRefs=None, created=None, updated=None):
        super(User, self).__init__()
        self.id = id
        self.key = key
        self.mossoId = mossoId
        self.nastId = nastId
        self.enabled = enabled
        self.baseURLRefs = baseURLRefs
        self.created = created
        self.updated = updated

    @classmethod
    def _json_to_obj(cls, serialized_str):
        ret = json.loads(serialized_str)
        return cls._dict_to_obj(ret.get(cls.ROOT_TAG))

    @classmethod
    def _dict_to_obj(cls, dic):
        if BaseURLRefs.ROOT_TAG in dic:
            dic[BaseURLRefs.ROOT_TAG] = (
                BaseURLRefs._list_to_obj(dic.get(BaseURLRefs.ROOT_TAG)))
        return User(**dic)

    @classmethod
    def _xml_to_obj(cls, serialized_str):
        element = etree.fromstring(serialized_str)
        test_base.remove_namespace(element, const.XMLNS_V11)
        if element.tag != cls.ROOT_TAG:
            return None
        return cls._xml_ele_to_obj(element)

    @classmethod
    def _xml_ele_to_obj(cls, xml_ele):
        kwargs = {const.ID: xml_ele.get(const.ID),
                  const.NAST_ID: xml_ele.get(const.NAST_ID),
                  const.KEY: xml_ele.get(const.KEY),
                  const.CREATED: xml_ele.get(const.CREATED),
                  const.UPDATED: xml_ele.get(const.UPDATED)}
        try:
            kwargs[const.MOSSO_ID] = int(xml_ele.get(const.MOSSO_ID))
        except (ValueError, TypeError):
            kwargs[const.MOSSO_ID] = xml_ele.get(const.MOSSO_ID)
        if xml_ele.get(const.ENABLED) is not None:
            kwargs[const.ENABLED] = json.loads(xml_ele.get(
                const.ENABLED).lower())
        base_url_refs = xml_ele.find(BaseURLRefs.ROOT_TAG)
        if base_url_refs is not None:
            kwargs[BaseURLRefs.ROOT_TAG] = BaseURLRefs._xml_ele_to_obj(
                    base_url_refs)
        return User(**kwargs)


class BaseURLs(base.AutoMarshallingModel):

    ROOT_TAG = const.BASE_URLS

    def __init__(self, baseURLs=None):
        super(BaseURLs, self).__init__()
        self.extend(baseURLs)

    @classmethod
    def _json_to_obj(cls, serialized_str):
        ret = json.loads(serialized_str)
        return cls._list_to_obj(ret.get(cls.ROOT_TAG))

    @classmethod
    def _list_to_obj(cls, list_):
        kwargs = {cls.ROOT_TAG: [BaseURL(**group) for group in list_]}
        return BaseURLs(**kwargs)

    @classmethod
    def _xml_to_obj(cls, serialized_str):
        element = etree.fromstring(serialized_str)
        test_base.remove_namespace(element, const.XMLNS_V11)
        if element.tag != cls.ROOT_TAG:
            return None
        return cls._xml_ele_to_obj(element)

    @classmethod
    def _xml_ele_to_obj(cls, xml_ele):
        xml_base_urls = xml_ele.findall(BaseURL.ROOT_TAG)
        base_urls = [BaseURL._xml_ele_to_obj(xml_base_url)
                     for xml_base_url in xml_base_urls]
        kwargs = {cls.ROOT_TAG: base_urls}
        return BaseURLs(**kwargs)


class BaseURL(base.AutoMarshallingModel):

    ROOT_TAG = const.BASE_URL

    def __init__(self, id=None, userType=None, region=None, default=None,
                 serviceName=None, publicURL=None, internalURL=None,
                 enabled=None, adminURL=None):
        super(BaseURL, self).__init__()
        self.id = id
        self.userType = userType
        self.region = region
        self.default = default
        self.serviceName = serviceName
        self.publicURL = publicURL
        self.internalURL = internalURL
        self.adminURL = adminURL
        self.enabled = enabled

    @classmethod
    def _json_to_obj(cls, serialized_str):
        ret = json.loads(serialized_str)
        return BaseURL(**ret.get(cls.ROOT_TAG))

    @classmethod
    def _xml_to_obj(cls, serialized_str):
        element = etree.fromstring(serialized_str)
        test_base.remove_namespace(element, const.XMLNS_V11)
        if element.tag != cls.ROOT_TAG:
            return None
        return cls._xml_ele_to_obj(element)

    @classmethod
    def _xml_ele_to_obj(cls, xml_ele):
        kwargs = {const.USER_TYPE: xml_ele.get(const.USER_TYPE),
                  const.REGION: xml_ele.get(const.REGION),
                  const.SERVICE_NAME: xml_ele.get(const.SERVICE_NAME),
                  const.PUBLIC_URL: xml_ele.get(const.PUBLIC_URL),
                  const.INTERNAL_URL: xml_ele.get(const.INTERNAL_URL),
                  const.ADMIN_URL: xml_ele.get(const.ADMIN_URL)}
        try:
            kwargs[const.ID] = int(xml_ele.get(const.ID))
        except (ValueError, TypeError):
            kwargs[const.ID] = xml_ele.get(const.ID)
        if xml_ele.get(const.DEFAULT) is not None:
            kwargs[const.DEFAULT] = (
                json.loads(xml_ele.get(const.DEFAULT).lower()))
        if xml_ele.get(const.ENABLED) is not None:
            kwargs[const.ENABLED] = (
                json.loads(xml_ele.get(const.ENABLED).lower()))
        if xml_ele.get(const.ADMIN_URL) is not None:
            kwargs[const.ADMIN_URL] = json.loads(xml_ele.get(const.ADMIN_URL))
        return BaseURL(**kwargs)


class BaseURLRefs(base.AutoMarshallingModel):

    ROOT_TAG = const.BASE_URL_REFS

    def __init__(self, baseURLRefs=None):
        super(BaseURLRefs, self).__init__()
        self.extend_baseurlrefs(baseURLRefs)

    @classmethod
    def _json_to_obj(cls, serialized_str):
        ret = json.loads(serialized_str)
        return cls._list_to_obj(ret.get(cls.ROOT_TAG))

    @classmethod
    def _list_to_obj(cls, list_):
        # This is to account for when getting userId/enabled
        if BaseURLRef.ROOT_TAG in list_:
            list_ = list_.get(BaseURLRef.ROOT_TAG)
        kwargs = {cls.ROOT_TAG: [BaseURLRef(**ref) for ref in list_]}
        return BaseURLRefs(**kwargs)

    @classmethod
    def _xml_to_obj(cls, serialized_str):
        element = etree.fromstring(serialized_str)
        test_base.remove_namespace(element, const.XMLNS_V11)
        if element.tag != cls.ROOT_TAG:
            return None
        return cls._xml_ele_to_obj(element)

    @classmethod
    def _xml_ele_to_obj(cls, xml_ele):
        xml_base_url_refs = xml_ele.findall(BaseURLRef.ROOT_TAG)
        base_url_refs = [BaseURLRef._xml_ele_to_obj(xml_base_url_ref)
                         for xml_base_url_ref in xml_base_url_refs]
        kwargs = {cls.ROOT_TAG: base_url_refs}
        return BaseURLRefs(**kwargs)

    @classmethod
    def extend_baseurlrefs(cls, baseurlrefs):
        baseurlrefs_ = []
        return baseurlrefs_.extend(baseurlrefs)


class BaseURLRef(base.AutoMarshallingModel):

    ROOT_TAG = const.BASE_URL_REF

    def __init__(self, id=None, href=None, v1Default=None):
        super(BaseURLRef, self).__init__()
        self.id = id
        self.href = href
        self.v1Default = v1Default

    @classmethod
    def _json_to_obj(cls, serialized_str):
        ret = json.loads(serialized_str)
        return BaseURLRef(**ret.get(cls.ROOT_TAG))

    @classmethod
    def _xml_to_obj(cls, serialized_str):
        element = etree.fromstring(serialized_str)
        test_base.remove_namespace(element, const.XMLNS_V11)
        if element.tag != cls.ROOT_TAG:
            return None
        return cls._xml_ele_to_obj(element)

    @classmethod
    def _xml_ele_to_obj(cls, xml_ele):
        kwargs = {const.HREF: xml_ele.get(const.HREF)}
        if xml_ele.get(const.V1_DEFAULT) is not None:
            kwargs[const.V1_DEFAULT] = (
                json.loads(xml_ele.get(const.V1_DEFAULT).lower()))
        try:
            kwargs[const.ID] = int(xml_ele.get(const.ID))
        except (ValueError, TypeError):
            kwargs[const.ID] = xml_ele.get(const.ID)
        return BaseURLRef(**kwargs)
