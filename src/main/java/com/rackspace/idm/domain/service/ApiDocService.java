package com.rackspace.idm.domain.service;

public interface ApiDocService {
    String getXsd(String fileName);

    String getXslt();

    String getWadl();
}
