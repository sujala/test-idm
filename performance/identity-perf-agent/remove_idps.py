import os
import requests

TOKEN = os.getenv("TOKEN")

headers = {
    'x-auth-token': TOKEN,
    'accept': 'application/json'
}

# get idps
resp = requests.get('https://staging.identity.api.rackspacecloud.com/v2.0/RAX-AUTH/federation/identity-providers', headers=headers)

if resp.status_code == 200:
    # get only the idp that's for perf
    for idp in resp.json()["RAX-AUTH:identityProviders"]:
        print("INFO::::start deleting {}".format(idp))
        if idp['description'] == 'Performance Test IDP, Domain is in issuer and auth url':
            resp2 = requests.delete('https://staging.identity.api.rackspacecloud.com/v2.0/RAX-AUTH/federation/identity-providers/{}'.format(idp['id']), headers=headers)
            if resp2.status_code == 204:
                print("DELETED {}".format(idp))
                continue
            else:
                print("ERROR:: {} is not delete".format(idp))
        else:
            print("did not delete because not a performance idp: {}".format(idp))
else:
    print("ERROR:: did not return idps")
    print(resp.status_code)
