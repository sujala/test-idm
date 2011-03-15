Rebuild LDAP Repository (for OpenDS)
====================================

To rebuild an LDAP repository, follow these steps:

1.)  Stop the OpenDS server:
/opt/OpenDS-2.2.1/bin/stop-ds

2.)  Put the LDAP schema in the config directory:
cp 99-user.ldif /opt/OpenDS-2.2.1/config/schema

3.)  Load the base.ldif data:
/opt/OpenDS-2.2.1/bin/import-ldif --ldifFile ./base.ldif -n userRoot --clearBackend -R barf.out

4.)  Start the OpenDS server:
/opt/OpenDS-2.2.1/bin/start-ds

5.)  Restart the GlassFish appserver (to ensure it has a valid LDAP connection)

6.)  Edit the populateLdap.py script (to point to the correct input file and correct IdM URL) and execute it.

