import traceback

import sys
sys.executable = '.'

import site
import os.path
from com.rackspace.keystone import KeystoneSitePackages

jar_location = KeystoneSitePackages().getClass().getProtectionDomain().getCodeSource().getLocation().getPath()
site.addsitedir(os.path.join(jar_location, 'Lib/site-packages'))

from keystone_rax.common.wsgi import application

def call_application(app, environ):
    body = []
    status_headers = [None, None]
    def start_response(status, headers):
        status_headers[:] = [status, headers]
        return body.append(status_headers)
    app_iter = app(environ, start_response)
    try:
        for item in app_iter:
            body.append(item)
    finally:
        if hasattr(app_iter, 'close'):
            app_iter.close()
    return status_headers[0], status_headers[1], body[1]


def handler(environ, start_response):
    try:
        status, headers, body = call_application(application['/v3'], environ)

        start_response(status, headers)

        return [body]

    except:
        status = '500'
        body = traceback.format_exc()

        headers = [('Content-type', 'text/plain'),
                            ('Content-Length', str(len(body)))]

        start_response(status, headers)
        return [body]
