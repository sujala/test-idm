package com.rackspace.idm.util;

import com.rackspace.idm.domain.entity.User.UserType;

/**
 * This class is responsible to convert string query params.
 */
public class QueryParamConverter {

    public static UserType convertUserTypeParamToEnum(String userType){

        if(UserType.ALL.toString().equalsIgnoreCase(userType)){
            return UserType.ALL;
        }
        if(UserType.UNVERIFIED.toString().equalsIgnoreCase(userType)){
            return UserType.UNVERIFIED;
        }

        // By Default return user type as verified if no query param is passed or is not valid input
        return UserType.VERIFIED;
    }

}
