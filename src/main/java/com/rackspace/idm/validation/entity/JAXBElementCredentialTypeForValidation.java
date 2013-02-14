package com.rackspace.idm.validation.entity;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 2/13/13
 * Time: 12:16 PM
 * To change this template use File | Settings | File Templates.
 */
public class JAXBElementCredentialTypeForValidation extends JAXBElement<CredentialTypeForValidation>{

    public JAXBElementCredentialTypeForValidation(QName name, Class declaredType, Class scope, Object value) {
        super(name, declaredType, scope, (CredentialTypeForValidation) value);
    }

    public JAXBElementCredentialTypeForValidation(){
        super(new QName("http://www.rackspace.com"), CredentialTypeForValidation.class,null);
    }

    public JAXBElementCredentialTypeForValidation(CredentialTypeForValidation value){
        super(new QName("http://www.rackspace.com"), CredentialTypeForValidation.class, value);
    }
}
