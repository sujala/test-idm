Rebuild LDAP Repository (for CA Directory Server)
=================================================

To rebuild an LDAP repository for Customer IDM:

0.)  Become user "dsa" on the CA server.

1.)  Stop the CA server:
     {CA_DIRECTORY_ROOT}/bin/dxserver stop {DSA_NAME}

2.)  Place rack3a_schema.dxc in {CA_DIRECTORY_ROOT}/config/schema.

3.)  Clear out the LDAP DB:
     {CA_DIRECTORY_ROOT}/bin/dxemptydb {DSA_NAME}

4.)  Load base.ldif:
     {CA_DIRECTORY_ROOT}/bin/dxloaddb -v {DSA_NAME} base.ldif

5.)  Start the CA server:
     {CA_DIRECTORY_ROOT}/bin/dxserver start {DSA_NAME}

6.)  Restart the GlassFish appserver (to ensure it has a valid LDAP connection)

7.)  Edit populateLdap.py to point to the correct API url.  
     Then, with a Python installation that has restkit installed, execute the script.

NOTE:  On some of the servers, the {CA_DIRECTORY_ROOT} is /opt/CA/Directory/dxserver