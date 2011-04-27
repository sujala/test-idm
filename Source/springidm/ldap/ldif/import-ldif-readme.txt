Rebuild LDAP Repository (for CA Directory Server)
=================================================

To rebuild an LDAP repository for Customer IDM:

0.)  Become user "dsa" on the CA server.

1.)  Stop the CA server:
     {CA_DIRECTORY_ROOT}/bin/dxserver stop {DSA_NAME}

2.)  Place the new schema in place (all of the files in the "config" folder).

3.)  Clear out the LDAP DB:
     {CA_DIRECTORY_ROOT}/bin/dxemptydb {DSA_NAME}

4.)  Load base.ldif:
     {CA_DIRECTORY_ROOT}/bin/dxloaddb -v {DSA_NAME} base.ldif

5.)  Start the CA server:
     {CA_DIRECTORY_ROOT}/bin/dxserver start {DSA_NAME}

NOTE:  On some of the servers, the {CA_DIRECTORY_ROOT} is /opt/CA/Directory/dxserver