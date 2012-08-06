Rebuild LDAP Repository (for CA Directory Server)
=================================================

CA Servers:
DEVCIDM1 (10.127.39.220)  {DSA_NAME} = DEVCIDM1.DEV
QACIDM1 (10.127.39.221)   {DSA_NAME} = QACIDM1.QA
QACIDM2 (10.127.39.222)   {DSA_NAME} = QACIDM2.QA

NOTE:  QACIDM1 and QACIDM2 are intended to be setup in a "router" cluster configuration, but this has not yet been done.  For now, the QA environment is only running off of the QACIDM1 LDAP server.

To rebuild an LDAP repository for Customer IDM:

0.)  Login to the CA server as "dsa" / "qwerty"

1.)  Stop the CA server:
     {CA_DIRECTORY_ROOT}/bin/dxserver stop {DSA_NAME}

2.)  Place the new files in the right folder under {CA_DIRECTORY_ROOT}/config.  

If only RackSchema.dxc is being deployed, no changes need to be made.  If redeploying the entire "config" folder, be sure to adjust the file {CA_DIRECTORY_ROOT}/config/knowledge/RackKnowledge.dxg which has environment-specific information.

3.)  Clear out the LDAP DB:
     {CA_DIRECTORY_ROOT}/bin/dxemptydb {DSA_NAME}

4.)  Load base.qa.ldif:
     {CA_DIRECTORY_ROOT}/bin/dxloaddb -v {DSA_NAME} /path/to/base.qa.ldif

5.)  Start the CA server:
     {CA_DIRECTORY_ROOT}/bin/dxserver start {DSA_NAME}

NOTE:  On most of the servers, the {CA_DIRECTORY_ROOT} is /opt/CA/Directory/dxserver