package com.rackspace.idm.aspect

import org.springframework.stereotype.Component

@Component
public class TestAspectTargetImpl implements TestAspectTarget {

    def foo(int arg) {
        return arg
    }

}
