from tests.api.v2 import base


class TestUserGroups(base.TestBaseV2):

    def setUp(self):
        super(TestUserGroups, self).setUp()
        if self.test_config.use_domain_for_user_groups:
            self.domain_id = self.test_config.domain_id
        else:
            self.domain_id = self.generate_random_string(pattern='[\d]{7}')
