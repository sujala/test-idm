package com.rackspace.idm.validation.entity;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.Size;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 2/13/13
 * Time: 9:23 AM
 * To change this template use File | Settings | File Templates.
 */
@Data
public class CredentialTypeForValidation {
    @Size(max = 100)
    protected String username;

    @Size(max = 100)
    protected String password;

    @Size(max = 100)
    protected String tokenKey;

    @Size(max = 100)
    protected String key;

    @Size(max = 100)
    protected String signature;

    @Size(max = 100)
    protected String question;

    @Size(max = 100)
    protected String answer;

    @Size(max = 100)
    protected String apiKey;
}
