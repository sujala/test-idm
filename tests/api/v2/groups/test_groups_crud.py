from qe_coverage.opencafe_decorators import tags, unless_coverage
import pytest

from tests.api.v2 import base
from tests.api.v2.schema import groups as groups_json
from tests.api.v2.models import factory
from tests.api.utils import func_helper

from tests.package.johny import constants as const


class TestGroups(base.TestBaseV2):

    @classmethod
    @unless_coverage
    def setUpClass(cls):
        """Class level set up for the tests
        """
        super(TestGroups, cls).setUpClass()
        cls.user_ids = []
        cls.domain_id = func_helper.generate_randomized_domain_id(
            client=cls.identity_admin_client)
        cls.user_admin_client = cls.generate_client(
            parent_client=cls.identity_admin_client,
            additional_input_data={'domain_id': cls.domain_id})
        cls.user_id = cls.user_admin_client.default_headers[const.X_USER_ID]
        cls.user_ids.append(cls.user_id)

    @unless_coverage
    def setUp(self):
        super(TestGroups, self).setUp()
        self.group_ids = []

    def add_group(self):
        # create group
        request_object = factory.get_add_group_request_object()
        resp = self.identity_admin_client.add_group(
                   request_object=request_object)
        self.assertEqual(resp.status_code, 201)
        self.assertSchema(
                response=resp, json_schema=groups_json.add_group)
        self.group_ids.append(resp.json()[const.NS_GROUP][const.ID])
        return resp

    @tags('positive', 'p1', 'regression')
    @pytest.mark.regression
    def test_add_group(self):
        create_resp = self.add_group()
        self.assertEqual(create_resp.status_code, 201)

    @tags('positive', 'p1', 'regression')
    @pytest.mark.regression
    def test_get_group_by_id(self):
        create_resp = self.add_group()
        group_id = create_resp.json()[const.NS_GROUP][const.ID]
        get_resp = self.identity_admin_client.get_group(group_id=group_id)
        self.assertEqual(get_resp.status_code, 200)
        self.assertEqual(get_resp.json()[const.NS_GROUP][const.ID], group_id)

    @tags('positive', 'p1', 'regression')
    @pytest.mark.regression
    def test_update_group(self):
        create_resp = self.add_group()
        group_id = create_resp.json()[const.NS_GROUP][const.ID]
        request_object = factory.get_add_group_request_object(
                group_name='updated_group_name')
        resp = self.identity_admin_client.update_group(
                   group_id=group_id, request_object=request_object)
        self.assertEqual(resp.status_code, 200)
        self.assertEqual(resp.json()[const.NS_GROUP][const.NAME],
                         'updated_group_name')

    @tags('positive', 'p1', 'regression')
    @pytest.mark.regression
    def test_add_user_to_group(self):
        create_resp = self.add_group()
        group_id = create_resp.json()[const.NS_GROUP][const.ID]
        resp = self.identity_admin_client.add_user_to_group(
                   group_id=group_id, user_id=self.user_id)
        self.assertEqual(resp.status_code, 204)

        # List users for a group
        resp = self.identity_admin_client.get_users_in_group(
                   group_id=group_id)
        self.assertEqual(resp.status_code, 200)
        user_id_list = [user[const.ID] for user in resp.json()[
            const.USERS]]
        self.assertIn(self.user_id, user_id_list)

        # Remove user from group
        resp = self.identity_admin_client.remove_user_from_group(
                   group_id=group_id, user_id=self.user_id)
        self.assertEqual(resp.status_code, 204)

        # Verify user is removed
        resp = self.identity_admin_client.get_users_in_group(
                   group_id=group_id)
        self.assertEqual(resp.status_code, 200)
        user_id_list = [user[const.ID] for user in resp.json()[
            const.USERS]]
        self.assertEqual(user_id_list, [])

    @unless_coverage
    def tearDown(self):
        for group_id in self.group_ids:
            self.identity_admin_client.delete_group(group_id=group_id)
        super(TestGroups, self).tearDown()

    @classmethod
    @unless_coverage
    def tearDownClass(cls):
        for user_id in cls.user_ids:
            cls.identity_admin_client.delete_user(user_id=cls.user_id)
        super(TestGroups, cls).tearDownClass()
