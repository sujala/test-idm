#!/bin/bash
sed -i -e "s|172.17.0.1|10.200.10.1|g" repose/config/system-model.cfg.xml
sed -i -e "s|repose_node1|cloudidentity_repose_1|g" repose/config/system-model.cfg.xml
if test -f "repose/config/ip-user.cfg.xml"; then
	sed -i -e "s|172.17.0.1|10.200.10.1|g" repose/config/ip-user.cfg.xml
fi
if test -f "repose/config/saml-policy.cfg.xml"; then
	sed -i -e "s|172.17.0.1|10.200.10.1|g" repose/config/saml-policy.cfg.xml
fi

