import os

if 'CAFE_CONFIG_FILE_PATH' not in os.environ:
    os.environ['CAFE_CONFIG_FILE_PATH'] = os.path.expanduser(
        '~/.identity/api.conf')

if 'CAFE_ROOT_LOG_PATH' not in os.environ:
    os.environ['CAFE_ROOT_LOG_PATH'] = os.path.expanduser('~/.identity/logs')

if 'CAFE_TEST_LOG_PATH' not in os.environ:
    os.environ['CAFE_TEST_LOG_PATH'] = os.path.expanduser('~/.identity/logs')

from base import * # noqa
