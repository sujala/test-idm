package com.rackspace.idm.api.resource.cloud.atomHopper;

import com.sun.xml.bind.marshaller.NamespacePrefixMapper;

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 12/20/12
 * Time: 4:36 PM
 * To change this template use File | Settings | File Templates.
 */
public class AHNamespaceMapper extends NamespacePrefixMapper {

    private static final String ATOM_PREFIX = "atom";
    private static final String ATOM_URL = "http://www.w3.org/2005/Atom";

    private static final String EVENT_PREFIX = "";
    private static final String EVENT_URL = "http://docs.rackspace.com/core/event";

    private static final String USER_PREFIX = "id";
    private static final String USER_URL = "http://docs.rackspace.com/event/identity/user";

    @Override
    public String getPreferredPrefix(String namespaceUri, String suggestion, boolean requirePrefix) {
        if(ATOM_URL.equals(namespaceUri)){
            return ATOM_PREFIX;
        } else if(EVENT_URL.equals(namespaceUri)){
            return EVENT_PREFIX;
        } else if(USER_URL.equals(namespaceUri)){
            return USER_PREFIX;
        }
        return suggestion;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String[] getPreDeclaredNamespaceUris(){
        return new String[] {ATOM_URL, EVENT_URL, USER_URL};
    }
}
