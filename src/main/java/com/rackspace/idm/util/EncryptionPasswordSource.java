package com.rackspace.idm.util;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 4/19/13
 * Time: 10:35 AM
 * To change this template use File | Settings | File Templates.
 */
public interface EncryptionPasswordSource {
    String getPassword();
    String getPassword(String version);
}
