package com.rackspace.idm.util.predicate;

import com.rackspace.idm.domain.entity.User;
import org.apache.commons.collections4.Predicate;

public class UserEnabledPredicate implements Predicate<User> {

    @Override
    public boolean evaluate(User user) {
        return user.getEnabled();
    }

}
