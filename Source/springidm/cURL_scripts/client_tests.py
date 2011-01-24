#! /usr/bin/env python

from restkit import Resource
from restkit.errors import ResourceError, ResourceNotFound
import json

url = 'http://localhost:8080/idm'
def_hdrs = {'Content-Type':'application/json'}
root = Resource(url)

admin_cred = {'password':'password', 'username':'mkovacs', 'client_secret':'password',
    'client_id':'ABCDEF', 'grant_type':'PASSWORD'}
client_cred = {'client_secret':'password',
    'client_id':'18e7a7032733486cd32f472d7bd58f709ac0d221', 'grant_type':'NONE'}


def show_response(resp):
    print(resp.status)
    body = resp.body_string()
    print(body)
    print
    return body


def do_get(res, hdrs=def_hdrs):
    print("GET %s" % res)
    resp = root.get(res, hdrs)
    return show_response(resp)


def do_post(res, body, hdrs=def_hdrs):
    print("POST: %s" % res)
    print("body -> %s" % body)
    resp = root.post(res, body, hdrs)
    return show_response(resp)


def do_put(res, body, hdrs=def_hdrs):
    print("PUT: %s" % res)
    print("body -> %s" % body)
    resp = root.put(res, body, hdrs)
    return show_response(resp)


def do_delete(res, hdrs=def_hdrs):
    print("DELETE %s" % res)
    resp = root.delete(res, hdrs)
    return show_response(resp)


def get_auth(body):
    body = do_post('/token', body)
    return json.loads(body)


def auth_header(cred, headers={}):
    auth = get_auth(json.dumps(cred))
    atoken = auth[u'access_token'][u'id']
    headers['Authorization'] = 'OAuth %s' % atoken
    print("Header: %s" % headers)
    return headers, atoken


def main():
    try:
        # Get root resource, versions.
        do_get('/')
        
        # Get an access token
        hdrs, token = auth_header(admin_cred)
        hdrs.update(def_hdrs)
        
        # Test user
        user_res = '/customers/RACKSPACE/users/john.eo'
        
        #Get a user
        userResp = None
        try:
            userResp = do_get(user_res, hdrs)
        except ResourceNotFound:
            print("%s not found" % user_res)
        
        # Delete the test user if exists
        if userResp:
            user = json.loads(userResp)
            if user['username'] == 'john.eo':
                hdrs, token = auth_header(client_cred, hdrs)
                do_delete(user_res, hdrs)
        
        # Add a user
        user = {'customerId':'RACKSPACE', 'personId':'RPN-000-123-4567', 'username':'john.eo',
            'password':{'password':'D0ntL00k@me'}, 'firstName':'John', 'lastName':'Eo',
            'email':'john.eo@rackspace.com', 'middleName':'helloworld',
            'secret':{'secretQuestion':'What is your favourite colour?',
                'secretAnswer':'Yellow. No, blue!'},
            'prefLanguage':'en_US', 'timeZone':'America/Chicago'}
        hdrs, token = auth_header(admin_cred, hdrs)
        do_post('/customers/RACKSPACE/users/', json.dumps(user), hdrs)
        
        # Authenticate the test user and validate his token
        hdrs, token = auth_header(client_cred, hdrs)
        do_get('/token/%s' % token, hdrs)
        
        # Clean up by deleting the test user
        do_delete(client_cred, hdrs)
    except ResourceError as e:
        print("Request failed: %s" % e)

if __name__ == "__main__":
    main()