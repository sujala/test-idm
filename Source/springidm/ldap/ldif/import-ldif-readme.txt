Rebuild LDAP Repository (for CA Directory Server)
=================================================

To rebuild an LDAP repository for Customer IDM:

1.)  Setup a server instance with LDAP schema based on 99-user.ldif.
     (IDM Team TODO:  Look into automating this process)

2.)  Load all.ldif into the CA LDAP instance.
     (IDM Team TODO:  Determine the proper command for this step)

3.)  Restart the GlassFish appserver (to ensure it has a valid LDAP connection)

