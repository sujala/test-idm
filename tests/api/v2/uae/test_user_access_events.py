# -*- coding: utf-8 -*
import time
import xml.etree.ElementTree as ET
from tests.api.v2 import base
from tests.api.utils import log_search as log, func_helper
from tests.api import constants as const
from tests.api.v2.models import factory, requests
"""
This test run against local docker container, only verify User Access Events
    emit in repose log
    verify event log follow the format:
<?xml version="1.0"?>
<cadf:event xmlns:cadf="http://schemas.dmtf.org/cloud/audit/1.0/event"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:ua="http://feeds.api.rackspacecloud.com/cadf/user-access-event"
    xsi:schemaLocation="http://schemas.dmtf.org/cloud/audit/1.0/\
        event user-access-cadf.xsd"
    id="c766c42d-d557-475e-9311-bfe946f8ffd6"
    eventType="activity"
    typeURI="http://schemas.dmtf.org/cloud/audit/1.0/event"
    eventTime="2016-10-12T17:31:43.167+00:00"
    action="update/post" outcome="success">
  <cadf:initiator id="testacctapi-test-xi010u0yc7WD"
        typeURI="network/node" name="testacctapi-test-xi010u0yc7WD">
    <cadf:host address="172.17.0.1" agent="python-requests/2.11.0"/>
  </cadf:initiator>
  <cadf:target id="localhost" typeURI="service" name="repose">
    <cadf:host address="localhost"/>
  </cadf:target>
  <cadf:attachments>
    <cadf:attachment name="auditData" contentType="ua:auditData">
      <cadf:content>
        <ua:auditData version="1">
          <ua:region>USA</ua:region>
          <ua:dataCenter>DFW</ua:dataCenter>
          <ua:methodLabel/>
          <ua:requestURL>http://localhost:8082/idm/cloud/v2.0/tokens
          </ua:requestURL>
          <ua:queryString/>
          <ua:tenantId/>
          <ua:responseMessage>OK</ua:responseMessage>
          <ua:userName>testacctapi-test-xi010u0yc7WD</ua:userName>
          <ua:roles/>
        </ua:auditData>
      </cadf:content>
    </cadf:attachment>
  </cadf:attachments>
  <cadf:observer id="repose-identity-repose_node1" name="repose"
    typeURI="service/security"/>
  <cadf:reason reasonCode="200"
    reasonType="http://www.iana.org/assignments/http-status-codes/\
        http-status-codes.xml"/>
</cadf:event>

@TODO: how to verify event log in staging.
"""


class TestUserAccessEvents(base.TestBaseV2):
    @classmethod
    def setUpClass(cls):
        super(TestUserAccessEvents, cls).setUpClass()
        cls.repose_container = cls.test_config.repose_container_name
        cls.search_uae_log = 'org.openrepose.herp.post.filter - '
        cls.cadf_host_xpath = './cadf:initiator/cadf:host'
        cls.au_username_xpath = (
            ".//*[@name='auditData']/cadf:content/ua:auditData/ua:userName")
        cls.au_requesturl_xpath = (
            ".//*[@name='auditData']/cadf:content/ua:auditData/ua:requestURL")

    def setUp(self):
        if not self.test_config.run_local_and_jenkins_only:
            self.skipTest('Skipping tests from staging and production')
        log.clean_log(container_name=self.repose_container,
                      path_to_logfile=const.PATH_TO_REPOSE_LOG)
        self.user_ids = []
        self.device_ids = []

    def create_user(self):
        user_object = factory.get_add_user_request_object()
        resp = self.identity_admin_client.add_user(request_object=user_object)
        self.assertEqual(resp.status_code, 201)
        user_id = resp.json()[const.USER][const.ID]
        self.user_ids.append(user_id)
        return user_id, resp

    def add_otp_devide(self, user_id):
        opt_object = requests.OTPDeviceAdd(
            device_name=self.generate_random_string(
                pattern=const.OTP_NAME_PATTERN))
        resp = self.identity_admin_client.create_otp_device(
            user_id=user_id, request_object=opt_object)
        self.assertEqual(resp.status_code, 201)
        device_id = resp.json()[const.NS_OTP_DEVICE][const.ID]
        self.device_ids.append(device_id)
        return device_id, resp

    def auth_with_pwd_cred(self):
        user_id, resp = self.create_user()
        username = resp.json()[const.USER][const.USERNAME]
        password = resp.json()[const.USER][const.NS_PASSWORD]
        auth_resp = self.identity_admin_client.get_auth_token(
            user=username, password=password)
        self.assertEqual(auth_resp.status_code, 200)
        token_id = auth_resp.json()[const.ACCESS][const.TOKEN][const.ID]
        return token_id, user_id, username

    def auth_with_apikey_cred(self):
        user_id, resp = self.create_user()
        # get user apikey
        resp = self.identity_admin_client.get_api_key(user_id=user_id)
        username = resp.json()[const.NS_API_KEY_CREDENTIALS][const.USERNAME]
        apikey = resp.json()[const.NS_API_KEY_CREDENTIALS][const.API_KEY]
        auth_resp = self.identity_admin_client.get_auth_token(user=username,
                                                              api_key=apikey)
        self.assertEqual(auth_resp.status_code, 200)
        token_id = auth_resp.json()[const.ACCESS][const.TOKEN][const.ID]
        return token_id, user_id, username

    def verify_event(self, xml_str, username=None, reason_code=None,
                     request_url=None):
        '''
        Veriy event format and username and status log in event
        :param xml_str:
        :param username:
        :param reason_code:
        :param reason_url:
        :return:
        '''
        ns = {
            'cadf': const.CADF,
            'ua': const.UA
        }
        event_attribs = [const.ID, const.EVENT_TYPE, const.EVENT_TIME,
                         const.TYPE_URI, const.ACTION, const.OUTCOME]
        child_elements = [const.INITIATOR, const.TARGET, const.ATTACHMENTS,
                          const.OBSERVER, const.REASON]
        host_info = [const.ADDRESS, const.AGENT]
        audit_data = [const.REGION, const.DATA_CENTER, const.METHOD_LABEL,
                      const.REQUEST_URL, const.QUERY_STRING, const.TENANT_ID,
                      const.RESPONSE_MESSAGE, const.USER_NAME, const.ROLES]
        root_elem = ET.fromstring(xml_str)
        self.assertEqual(root_elem.tag, '{' + ns['cadf'] + '}event')

        # verifiy event attributes
        attributes = root_elem.attrib
        for attr in event_attribs:
            self.assertIn(attr, attributes)

        # get root child tags
        child_tags = []
        for child in root_elem:
            child_tags.append(child.tag)

        # verify even child elememt
        for el in child_elements:
            child = '{' + ns['cadf'] + '}' + el
            self.assertIn(child, child_tags)

        # verify event host info
        host_attrs = root_elem.find(self.cadf_host_xpath, ns).attrib
        for attr in host_info:
            self.assertIn(attr, host_attrs)

        # verify sub elements in attachments
        data_list = []
        attachments = root_elem.find(const.CADF_ATTACHMENTS, ns)
        # verify only auditData
        for attachment in attachments:
            if attachment.get(const.NAME) == const.AUDIT_DATA:
                content = attachment.find(const.CADF_CONTENT, ns)
                self.assertIsNotNone(content)
                data = content.find(const.UA_AUDIT_DATA, ns)
                self.assertIsNotNone(data)
                for child in data:
                    data_list.append(child.tag)
                for ea in audit_data:
                    item = '{' + ns['ua'] + '}' + ea
                    self.assertIn(item, data_list)

        # additional assertion for event data
        self.assertEqual(root_elem.get(const.EVENT_TYPE), 'activity')
        if reason_code:
            self.assertEqual(
                root_elem.find(const.CADF_REASON, ns).get(const.REASON_CODE),
                reason_code
            )
        if username:
            self.assertEqual(
                root_elem.find(const.CADF_INITIATOR, ns).get(const.NAME),
                username
            )
            self.assertEqual(
                root_elem.find(const.CADF_INITIATOR, ns).get(const.ID),
                username
            )
            nodes = root_elem.findall(self.au_username_xpath, ns)
            self.assertEqual(nodes[0].text, username)

        if request_url:
            nodes = root_elem.findall(self.au_requesturl_xpath, ns)
            self.assertEqual(nodes[0].text, request_url)

    def test_verify_uae_w_pwd_auth(self):
        token_id, user_id, username = self.auth_with_pwd_cred()
        req_url = self.url + const.TOKEN_URL
        time.sleep(1)

        # get events from log
        search_str = self.search_uae_log
        result = log.search_string(container_name=self.repose_container,
                                   search_pattern=search_str,
                                   path_to_logfile=const.PATH_TO_REPOSE_LOG)
        time.sleep(1)
        self.assertNotEqual(len(result), 0)
        # verify event
        for line in result.splitlines():
            event = line[line.index(search_str) + len(search_str):len(line)]
            self.verify_event(event, username=username, reason_code="200",
                              request_url=req_url)

    def test_verify_uae_w_apikey_auth(self):
        token_id, user_id, username = self.auth_with_apikey_cred()
        req_url = self.url + const.TOKEN_URL
        time.sleep(1)

        # get events from log
        search_str = self.search_uae_log
        result = log.search_string(container_name=self.repose_container,
                                   search_pattern=search_str,
                                   path_to_logfile=const.PATH_TO_REPOSE_LOG)
        time.sleep(1)
        self.assertNotEqual(len(result), 0)
        # verify event
        for line in result.splitlines():
            event = line[line.index(search_str) + len(search_str):len(line)]
            self.verify_event(event, username=username, reason_code="200",
                              request_url=req_url)

    def test_verify_uae_w_mfa_auth(self):
        """
        - create user
        - add otp device
        - verify otp device
        - update mfa enabled=true
        - authenticate get session id
        - verify log
        - authenticate with mfa
        - verify log
        :return:
        """
        req_url = self.url + const.TOKEN_URL
        user_id, resp = self.create_user()
        user_name = resp.json()[const.USER][const.USERNAME]
        password = resp.json()[const.USER][const.NS_PASSWORD]
        # add otp device
        device_id, resp = self.add_otp_devide(user_id=user_id)
        secret = func_helper.parse_secret_from_otp_device(otp_response=resp)
        code = func_helper.get_oath_from_secret(secret=secret)
        # verify otp device
        verify_obj = requests.OTPDeviceVerify(code=code)
        resp = self.identity_admin_client.verify_otp_device(
            user_id=user_id, otp_device_id=device_id,
            request_object=verify_obj
        )
        self.assertEqual(resp.status_code, 204)
        # update mfa enabled=true
        update_obj = requests.MFAUpdate(enabled=True)
        resp = self.identity_admin_client.update_mfa(user_id=user_id,
                                                     request_object=update_obj)
        self.assertEqual(resp.status_code, 204)
        # authenticate with pwd
        auth_resp = self.identity_admin_client.get_auth_token(
            user=user_name, password=password
        )
        # get events from log
        search_str = self.search_uae_log
        result = log.search_string(container_name=self.repose_container,
                                   search_pattern=search_str,
                                   path_to_logfile=const.PATH_TO_REPOSE_LOG)
        time.sleep(1)
        self.assertNotEqual(len(result), 0)
        # verify event
        for line in result.splitlines():
            event = line[line.index(search_str) + len(search_str):len(line)]
            self.verify_event(event, username=user_name,
                              request_url=req_url)

        log.clean_log(container_name=self.repose_container,
                      path_to_logfile=const.PATH_TO_REPOSE_LOG)
        auth_header = auth_resp.headers[const.WWW_AUTHENTICATE]
        session_id = auth_header.split('sessionId=\'')[1].split('\'')[0]
        mfa_resp = self.identity_admin_client.auth_with_mfa_cred(
            session_id=session_id, pass_code=code
        )
        self.assertEqual(mfa_resp.status_code, 200)
        # get events from log
        search_str = self.search_uae_log
        result = log.search_string(container_name=self.repose_container,
                                   search_pattern=search_str,
                                   path_to_logfile=const.PATH_TO_REPOSE_LOG)
        time.sleep(1)
        self.assertNotEqual(len(result), 0)
        # verify event
        for line in result.splitlines():
            event = line[line.index(search_str) + len(search_str):len(line)]
            self.verify_event(event, request_url=req_url)

    def tearDown(self):
        # Delete all users created in the tests
        for id in self.user_ids:
            resp = self.identity_admin_client.delete_user(user_id=id)
            self.assertEqual(resp.status_code, 204)
        super(TestUserAccessEvents, self).tearDown()
