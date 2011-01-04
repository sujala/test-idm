#! /bin/bash

# Use this to add an LDAP server's public key to an Java's allowed list of TLS certificates. Make sure to change the param values below accrodingly.
# keytool is part of JDK.

# Mac OS's path for cacerts. Don't use this for the IDM APP
# CACERTS_PATH="/System/Library/Frameworks/JavaVM.framework/Resources/Deploy.bundle/Contents/Home/lib/security/cacerts"

CACERTS_MAIN_PATH="../src/main/resources/prod_cacerts"
CACERTS_TEST_PATH="../src/test/resources/prod_cacerts"

# Must add all LDAP servers to the trusted list
CERT_ALIAS="edir2.sat1.corp.rackspace.com"
PUBKEY_PATH="sat1-edir2.pem"

# CERT_ALIAS="cert-edir2.dfw1.corp.rackspace.com"
# PUBKEY_PATH="cert-edir2.pem"

echo $CERT_ALIAS
echo $CACERTS_MAIN_PATH
echo $PUBKEY_PATH
sudo keytool -import -alias $CERT_ALIAS -keystore $CACERTS_MAIN_PATH -file $PUBKEY_PATH
sudo keytool -import -alias $CERT_ALIAS -keystore $CACERTS_TEST_PATH -file $PUBKEY_PATH