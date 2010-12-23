#! /bin/bash

#edir2.sat1.corp.rackspace.com
#edir3.sat1.corp.rackspace.com
#edir2.dfw1.corp.rackspace.com
#edir3.dfw1.corp.rackspace.com
#edir1.hkg1.corp.rackspace.com
#edir2.hkg1.corp.rackspace.com
#edir2.iad1.corp.rackspace.com
#edir3.iad1.corp.rackspace.com
#edir2.lon3.corp.rackspace.com
#edir3.lon3.corp.rackspace.com
#edir1.ord1.corp.rackspace.com
#edir2.ord1.corp.rackspace.com

# Must get the public key for all servers
SERVER="cert-edir.dfw1.corp.rackspace.com"
#SERVER="cert-edir2.dfw1.corp.rackspace.com"
echo "Retrieving public key for " $SERVER
openssl s_client -connect $SERVER:ldaps