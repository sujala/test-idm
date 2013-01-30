package com.rackspace.idm.api.resource.cloud

import spock.lang.Specification
import spock.lang.Shared
import com.rackspace.idm.domain.dao.impl.LdapPatternRepository
import com.rackspace.idm.domain.entity.Pattern
import com.rackspace.idm.exception.BadRequestException
import com.rackspacecloud.docs.auth.api.v1.User;

/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: 11/30/12
 * Time: 1:16 PM
 * To change this template use File | Settings | File Templates.
 */
class ValidatorTest extends Specification {

    @Shared Validator validator;
    @Shared LdapPatternRepository ldapPatternRepository

    def setupSpec(){
        validator = new Validator();
    }

    def "Validate username"(){
        given:
        setupMock()
        Pattern patterns = pattern("username", "[a-zA-Z0-9-_.@]*","Username has invalid characters.","pattern for invalid characters");
        ldapPatternRepository.getPattern(_) >> patterns

        when:
        boolean result = validator.isUsernameValid("someUsername123-@._")

        then:
        result
    }

    def "Invalidate username"(){
        given:
        setupMock()
        Pattern patterns = pattern("username", "[a-zA-Z0-9-_.@]*","Username has invalid characters.","pattern for invalid characters")
        ldapPatternRepository.getPattern(_) >> patterns

        when:
        validator.isUsernameValid("someUsername*")

        then:
        thrown(BadRequestException)
    }

    def "Validate email"(){
        given:
        setupMock()
        Pattern patterns = pattern("email", "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[A-Za-z]+","validate email","pattern for invalid characters")
        ldapPatternRepository.getPattern(_) >> patterns

        when:
        boolean result = validator.isEmailValid("joe.racker@rackspace.com")

        then:
        result
    }

    def "Invalid email"(){
        given:
        setupMock()
        Pattern patterns = pattern("email", "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[A-Za-z]+","validate email","pattern for invalid characters")
        ldapPatternRepository.getPattern(_) >> patterns

        when:
        validator.isEmailValid("joe racker@rackspace.com")

        then:
        thrown(BadRequestException)
    }

    def "Validate phone"(){
        given:
        setupMock()
        Pattern patterns = pattern("phone", "[0-9]*","Phone has invalid characters.","pattern for invalid characters")
        ldapPatternRepository.getPattern(_) >> patterns

        when:
        boolean result = validator.isPhoneValid("8001234568")

        then:
        result
    }

    def "Invalid phone"(){
        given:
        setupMock()
        Pattern patterns = pattern("phone", "[0-9]*","phone has invalid characters.","pattern for invalid characters")
        ldapPatternRepository.getPattern(_) >> patterns

        when:
        validator.isPhoneValid("1235a123544")

        then:
        thrown(BadRequestException)
    }

    def "Validate alphaNumeric"(){
        given:
        setupMock()
        Pattern patterns = pattern("username", "[a-zA-Z0-9]*","Username has invalid characters.","pattern for invalid characters")
        ldapPatternRepository.getPattern(_) >> patterns

        when:
        boolean result = validator.isAlphaNumeric("someName123")

        then:
        result
    }

    def "Invalidate alphaNumeric"(){
        given:
        setupMock()
        Pattern patterns = pattern("username", "[a-zA-Z0-9]*","Username has invalid characters.","pattern for invalid characters")
        ldapPatternRepository.getPattern(_) >> patterns

        when:
        validator.isAlphaNumeric("someUsername*")

        then:
        thrown(BadRequestException)
    }

    def "validate v11 user"(){
        given:
        setupMock()
        User user = new User();
        user.id = "somename"

        when:
        validator.validate11User(user)

        then:
        true
    }

    def "null username - v11 user"(){
        given:
        setupMock()
        User user = new User();

        when:
        validator.validate11User(user)

        then:
        thrown(BadRequestException)
    }

    def "Empty username - v11 user"(){
        given:
        setupMock()
        User user = new User();
        user.id = ""

        when:
        validator.validate11User(user)

        then:
        thrown(BadRequestException)
    }

    def "Null username - v11 user"(){
        given:
        setupMock()

        when:
        validator.validate11User(null)

        then:
        thrown(BadRequestException)
    }

    def "Validate username: bad pattern"(){
        given:
        setupMock()
        Pattern patterns = pattern("username", "[a-zA-Z0-9-_.@asdfsadf24232%^&*","Username has invalid characters.","pattern for invalid characters");
        ldapPatternRepository.getPattern(_) >> patterns

        when:
        validator.isUsernameValid("someUsername123")

        then:
        thrown(IllegalStateException)
    }

    def "Validate username: null username"(){
        given:
        setupMock()
        Pattern patterns = pattern("username", "^[A-Za-z0-9][a-zA-Z0-9-_.@]*","Username has invalid characters.","pattern for invalid characters");
        ldapPatternRepository.getPattern(_) >> patterns

        when:
        validator.isUsernameValid(null)

        then:
        thrown(BadRequestException)
    }

    def "Validate username: empty username"(){
        given:
        setupMock()
        Pattern patterns = pattern("username", "^[A-Za-z0-9][a-zA-Z0-9-_.@]*","Username has invalid characters.","pattern for invalid characters");
        ldapPatternRepository.getPattern(_) >> patterns

        when:
        validator.isUsernameValid("")

        then:
        thrown(BadRequestException)
    }


    def setupMock(){
        ldapPatternRepository = Mock();
        validator.ldapPatternRepository = ldapPatternRepository;
    }

    def pattern (String name, String regex, String errMsg, String description){
        new Pattern().with {
            it.name = name
            it.regex = regex
            it.errMsg = errMsg
            it.description = description
            return it
        }
    }
}
