# -*- coding: utf-8 -*
"""
This test in intended to test user access events.
For an example of an Identity UAE see
https://github.com/rackerlabs/standard-usage-schemas/blob/master/message_samples/corexsd/xml/identity-user-access-event.xml

NOTE: This test has a dependency on being run with the ability
to connect to a local docker container. This is not ideal
and will be redesigned in a future iteration of UAE testing.
For now, it is highly discouraged to connect to docker
containers for UAE testing.
"""

import time
import xml.etree.ElementTree as ET

from tests.api.v2 import base
from tests.api.utils import log_search as log, func_helper
from tests.api.v2.models import factory

from tests.package.johny import constants as const
from tests.package.johny.v2.models import requests
from tests.api.utils import header_validation


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
        user_object = factory.get_add_user_one_call_request_object()
        resp = self.identity_admin_client.add_user(request_object=user_object)
        self.assertEqual(resp.status_code, 201)
        user_id = resp.json()[const.USER][const.ID]
        self.user_ids.append(user_id)
        return user_id, resp

    def add_otp_device(self, user_id):
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
        req_obj = requests.AuthenticateWithPassword(
            user_name=username, password=password
        )
        auth_resp = self.identity_admin_client.get_auth_token(
            request_object=req_obj)
        self.assertEqual(auth_resp.status_code, 200)
        token_id = auth_resp.json()[const.ACCESS][const.TOKEN][const.ID]
        return token_id, user_id, username

    def auth_with_apikey_cred(self):
        user_id, resp = self.create_user()
        # get user apikey
        resp = self.identity_admin_client.get_api_key(user_id=user_id)
        username = resp.json()[const.NS_API_KEY_CREDENTIALS][const.USERNAME]
        apikey = resp.json()[const.NS_API_KEY_CREDENTIALS][const.API_KEY]
        req_obj = requests.AuthenticateWithApiKey(user_name=username,
                                                  api_key=apikey)
        auth_resp = self.identity_admin_client.get_auth_token(
            request_object=req_obj)
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
        # create the user
        user_id, resp = self.create_user()
        user_name = resp.json()[const.USER][const.USERNAME]
        domain_id = resp.json()[const.USER][const.RAX_AUTH_DOMAIN_ID]
        password = resp.json()[const.USER][const.NS_PASSWORD]
        # add otp device
        device_id, resp = self.add_otp_device(user_id=user_id)
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

        # authenticate with pwd & mfa enabled (1st mfa auth step)
        auth_obj = requests.AuthenticateWithPassword(user_name=user_name,
                                                     password=password)
        auth_resp = self.identity_admin_client.get_auth_token(
            request_object=auth_obj
        )
        auth_header = auth_resp.headers[const.WWW_AUTHENTICATE]
        self.assertHeaders(auth_resp,
                           header_validation.
                           validate_username_header_not_present)
        session_id = auth_header.split('sessionId=\'')[1].split('\'')[0]
        # TODO: verify UAE
        # authenticate with passcode & session ID (2nd mfa auth step)
        mfa_resp = self.identity_admin_client.auth_with_mfa_cred(
            session_id=session_id, pass_code=code
        )
        self.assertEqual(mfa_resp.status_code, 200)
        self.assertEqual(mfa_resp.headers[const.X_TENANT_ID], domain_id)
        self.assertHeaders(mfa_resp,
                           header_validation.
                           validate_username_header_not_present)
        # TODO: verify UAE

    def tearDown(self):
        # Delete all users created in the tests
        for id in self.user_ids:
            resp = self.identity_admin_client.delete_user(user_id=id)
            self.assertEqual(resp.status_code, 204)
        super(TestUserAccessEvents, self).tearDown()
