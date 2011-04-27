#! /usr/bin/env python

from restkit import Resource
from restkit.errors import ResourceError, ResourceNotFound
import json

########################################
data_ldif = 'combined.ldif'
########################################
url = 'http://10.127.7.166:8080/v1.0'
#url = 'http://10.127.7.164:8080/v1.0'
########################################
def_hdrs = {'Content-Type':'application/json'}
root = Resource(url)
########################################

userDict = {}

client_cred = {'client_secret':'password',
    'client_id':'18e7a7032733486cd32f472d7bd58f709ac0d221', 'grant_type':'CLIENT_CREDENTIALS'}


def show_response(resp):
    print(resp.status)
    print
    body = resp.body_string()
    #print(body)
    #print
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


def addBaseUrl(thisRecord, hdrs):
    res = '/baseurls/addBaseUrl/'

    body = {'id' : thisRecord['baseUrlId'][0],
            'userType' : thisRecord['baseUrlType'][0],
            'serviceName' : thisRecord['service'][0],
            'publicURL' : thisRecord['publicUrl'][0],
            'default' : thisRecord['def'][0]}

    if 'internalUrl' in thisRecord:
       body['internalURL'] = thisRecord['internalUrl'][0]
    if 'adminUrl' in thisRecord:
       body['adminURL'] = thisRecord['adminUrl'][0]
    if 'rsRegion' in thisRecord:
       body['region'] = thisRecord['rsRegion'][0]

    do_post(res, json.dumps(body), hdrs)


def addCustomer(thisRecord, hdrs):
    res = '/customers/'

    body = {'customerId' : thisRecord['RCN'][0]}
    if 'softDeleted' in thisRecord:
       body['softDeleted'] = thisRecord['softDeleted'][0].lower()
    if 'locked' in thisRecord:
       body['locked'] = thisRecord['locked'][0].lower()

    do_post(res, json.dumps(body), hdrs)

def addUser(thisRecord, hdrs):
    customerId = thisRecord['RCN'][0]
    res = '/customers/' + customerId + '/users/'

    body = {'region' : thisRecord['c'][0],
            'prefLanguage' : thisRecord['preferredLanguage'][0],
            'timeZone' : thisRecord['timeZone'][0],
            'username' : thisRecord['uid'][0],
            'displayName' : thisRecord['displayName'][0],
            'lastName' : thisRecord['sn'][0],
            'firstName' : thisRecord['givenName'][0],
            'personId' : thisRecord['RPN'][0],
            'email' : thisRecord['mail'][0],
            'customerId' : thisRecord['RCN'][0],
            'secret' : {'secretAnswer' : thisRecord['secretAnswer'][0],
                        'secretQuestion' : thisRecord['secretQuestion'][0]},
            'password' : {'password' : thisRecord['userPassword'][0]}}

    if 'rsMossoId' in thisRecord:
       body['mossoId'] = thisRecord['rsMossoId'][0]
    if 'rsNastId' in thisRecord:
       body['nastId'] = thisRecord['rsNastId'][0]
    if 'middleName' in thisRecord:
       body['middleName'] = thisRecord['middleName'][0]
    if 'softDeleted' in thisRecord:
       body['softDeleted'] = thisRecord['softDeleted'][0].lower()
    if 'status' in thisRecord:
       body['status'] = thisRecord['status'][0].upper()
    if 'locked' in thisRecord:
       body['locked'] = thisRecord['locked'][0].lower()
            
    do_post(res, json.dumps(body), hdrs)

    ldifInum = thisRecord['inum'][0]
    userDict[ldifInum] = thisRecord['uid'][0]


    # Setting the API key must be a separate call from addUser
    res = '/customers/' + customerId + '/users/' + thisRecord['uid'][0] + '/key/'
    body = { 'apiKey' : thisRecord['rsApiKey'][0] }
    do_put(res, json.dumps(body), hdrs)
 

def addClient(thisRecord, hdrs):
    customerId = thisRecord['RCN'][0]
    res = '/customers/' + customerId + '/clients/'

    body = {'customerId' : thisRecord['RCN'][0],
            'name' : thisRecord['cn'][0],
            'clientId' : thisRecord['clientId'][0],
            'permissions' : { 'permission' : [{'permissionId' : 'AuthWithUsernameAndApiKey',
                             'customerId' : 'RACKSPACE',
                             'clientId' : '18e7a7032733486cd32f472d7bd58f709ac0d221',
                             'type' : ''}]}}
    if 'softDeleted' in thisRecord:
       body['softDeleted'] = thisRecord['softDeleted'][0].lower()
    if 'status' in thisRecord:
       body['status'] = thisRecord['status'][0].upper()
    if 'locked' in thisRecord:
       body['locked'] = thisRecord['locked'][0].lower()

    do_post(res, json.dumps(body), hdrs)


def addClientGroup(thisRecord, hdrs):
    clientId = thisRecord['clientId'][0]
    customerId = thisRecord['RCN'][0]
    groupName = thisRecord['cn'][0]
    res = '/customers/' + customerId + '/clients/' + clientId + '/groups/'

    body = {'customerId' : customerId,
            'name' : groupName,
            'clientId' : clientId}

    do_post(res, json.dumps(body), hdrs)

    # Add any members to the Idm Group
    for member in thisRecord['member']:
       thisInum, thisSep, theRest = member.partition(",")
       thisInum = thisInum[5:]
       thisUid = userDict[thisInum]
       res = '/customers/' + customerId + '/clients/' + clientId + '/groups/' + groupName + '/members/' + thisUid

       body['groupname'] = groupName
       body['username'] = thisUid
       do_put(res, json.dumps(body), hdrs)


       

def main():
   # Get an access token
   hdrs, token = auth_header(client_cred)
   hdrs.update(def_hdrs)

   f = open(data_ldif, 'r')
   thisRecord = {}

   # Iterate through all lines in the LDIF script
   for line in f:

      # Skip over comments
      if line.startswith('#'):
         continue
  
      # With whitespace, add the entity to IdM and start a new record
      if line.isspace():

         if not thisRecord:
            continue

         if 'baseUrl' in thisRecord['objectClass']:
            try:
               addBaseUrl(thisRecord, hdrs)
            except ResourceError as e:
                print("Request failed: %s" % e)


         elif 'rsApplication' in thisRecord['objectClass']:
            try:
                addClient(thisRecord, hdrs)
            except ResourceError as e:
                print("Request failed: %s" % e)


         elif 'rsPerson' in thisRecord['objectClass']:
            try:
                addUser(thisRecord, hdrs)
            except ResourceError as e:
                print("Request failed: %s" % e)


         elif 'rsOrganization' in thisRecord['objectClass']:
            try:
                addCustomer(thisRecord, hdrs)
            except ResourceError as e:
                print("Request failed: %s" % e)

         elif 'clientGroup' in thisRecord['objectClass']:
            try:
                addClientGroup(thisRecord, hdrs)
            except ResourceError as e:
                print("Request failed: %s" % e)

         thisRecord = {}

      else:

         # Grab the key/value pair for the current line
         thisKey, thisSep, thisVal = line.partition(":")
         thisKey = thisKey.strip()
         thisVal = thisVal.strip()

         # Add the key/value pair to the record,
         #     appending if the key already exists
         if thisKey in thisRecord:
            thisRecord[thisKey].append(thisVal)
         else:
            thisRecord[thisKey] = [thisVal]

if __name__ == "__main__":
    main()
