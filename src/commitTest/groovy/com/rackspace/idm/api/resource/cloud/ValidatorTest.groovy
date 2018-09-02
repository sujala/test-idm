package com.rackspace.idm.api.resource.cloud

import com.rackspace.idm.api.resource.cloud.v20.DefaultRegionService
import com.rackspace.idm.domain.dao.impl.LdapPatternRepository
import com.rackspace.idm.domain.entity.ClientRole
import com.rackspace.idm.domain.entity.Group
import com.rackspace.idm.domain.entity.Pattern
import com.rackspace.idm.domain.entity.TenantRole
import com.rackspace.idm.domain.service.GroupService
import com.rackspace.idm.domain.service.RoleService
import com.rackspace.idm.domain.service.UserService
import com.rackspace.idm.exception.BadRequestException
import com.rackspace.idm.exception.DuplicateUsernameException
import com.rackspace.idm.validation.Validator
import com.rackspacecloud.docs.auth.api.v1.User
import org.apache.commons.configuration.Configuration
import spock.lang.Shared
import spock.lang.Specification

/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: 11/30/12
 * Time: 1:16 PM
 * To change this template use File | Settings | File Templates.
 */
class ValidatorTest extends Specification {

    @Shared Validator validator
    @Shared LdapPatternRepository ldapPatternRepository
    @Shared UserService userService
    @Shared DefaultRegionService defaultRegionService
    @Shared RoleService roleService
    @Shared GroupService groupService
    @Shared Pattern passwordPattern
    @Shared Pattern usernamePattern
    @Shared Pattern emailPattern
    @Shared Configuration config

    def setupSpec(){
        validator = new Validator();
        passwordPattern = pattern("password", "[a-zA-Z0-9-_.@]*","Password has invalid characters.","pattern for invalid characters")
        usernamePattern = pattern("username", "[a-zA-Z0-9-_.@]*","Username has invalid characters.","pattern for invalid characters")
        emailPattern = pattern("email", "^['a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[A-Za-z]+","validate email","pattern for invalid characters")
    }

    def setup() {
        setupMock()
    }

    def "Validate username"(){
        given:
        ldapPatternRepository.getPattern(_) >> usernamePattern

        when:
        boolean result = validator.isUsernameValid("someUsername123-@._")

        then:
        result
    }

    def "Invalidate username"(){
        given:
        ldapPatternRepository.getPattern(_) >> usernamePattern

        when:
        validator.isUsernameValid("someUsername*")

        then:
        thrown(BadRequestException)
    }

    def "Validate valid email"() {
        given:
        ldapPatternRepository.getPattern(_) >> emailPattern

        when:
        boolean result = validator.isEmailValid("joe.racker@rackspace.com")

        then:
        result
    }

    def "Validate valid email with apostrophes"() {
        given:
        ldapPatternRepository.getPattern(_) >> emailPattern

        expect:
        validator.isEmailValid("'joe'.racker@rackspace.com")
        validator.isEmailValid("j'oe.racker@rackspace.com")
        validator.isEmailValid("joe.rac'ker@rackspace.com")
        validator.isEmailValid("j'oe.rac'ker@rackspace.com")
        validator.isEmailValid("joe.rac''ker@rackspace.com")
        validator.isEmailValid("'joe.racker@rackspace.com")
        validator.isEmailValid("joe.racker'@rackspace.com")
        validator.isEmailValid("'''@rackspace.com")
    }

    def "Validate invalid email"() {
        given:
        ldapPatternRepository.getPattern(_) >> emailPattern

        when:
        boolean result = validator.isEmailValid("joe.racker")

        then:
        !result
    }

    def "assert valid email"() {
        given:
        ldapPatternRepository.getPattern(_) >> emailPattern

        when:
        validator.assertEmailValid("joe.racker@rackspace.com")

        then:
        notThrown BadRequestException
    }

    def "assert valid email with apostrophes"() {
        given:
        ldapPatternRepository.getPattern(_) >> emailPattern

        when:
        validator.assertEmailValid("'joe'.racker@rackspace.com")
        validator.assertEmailValid("j'oe.racker@rackspace.com")
        validator.assertEmailValid("joe.rac'ker@rackspace.com")
        validator.assertEmailValid("j'oe.rac'ker@rackspace.com")
        validator.assertEmailValid("joe.rac''ker@rackspace.com")
        validator.assertEmailValid("'joe.racker@rackspace.com")
        validator.assertEmailValid("joe.racker'@rackspace.com")
        validator.assertEmailValid("'''@rackspace.com")

        then:
        notThrown BadRequestException
    }

    def "assert invalid email"() {
        given:
        ldapPatternRepository.getPattern(_) >> emailPattern

        when:
        validator.assertEmailValid("joe.racker")

        then:
        thrown BadRequestException
    }

    def "Invalid email"(){
        given:
        ldapPatternRepository.getPattern(_) >> emailPattern

        when:
        validator.assertEmailValid("joe racker@rackspace.com")

        then:
        thrown(BadRequestException)
    }

    def "Invalid email with apostrophe in domain"(){
        given:
        ldapPatternRepository.getPattern(_) >> emailPattern

        when:
        validator.assertEmailValid("joe.racker@rack'space.com")

        then:
        thrown(BadRequestException)
    }

    def "Validate alphaNumeric"(){
        given:
        ldapPatternRepository.getPattern(_) >> usernamePattern

        when:
        boolean result = validator.isAlphaNumeric("someName123")

        then:
        result
    }

    def "Invalidate alphaNumeric"(){
        given:
        ldapPatternRepository.getPattern(_) >> usernamePattern

        when:
        validator.isAlphaNumeric("someUsername*")

        then:
        thrown(BadRequestException)
    }

    def "validate v11 user"(){
        given:
        User user = new User();
        user.mossoId = 98765
        userService.getUserByTenantId(String.valueOf(user.mossoId)) >> null

        when:
        validator.validate11User(user)

        then:
        true
    }

    def "validate v11 user when mossoId not specified"(){
        given:
        User user = new User();
        user.mossoId = null

        when:
        validator.validate11User(user)

        then:
        thrown(BadRequestException)
    }

    def "validate v11 user when another user with same mossoId already exists"(){
        given:
        User user = new User();
        user.mossoId = 98765
        userService.getUserByTenantId(String.valueOf(user.mossoId)) >>  new com.rackspace.idm.domain.entity.User()

        when:
        validator.validate11User(user)

        then:
        thrown(BadRequestException)
    }

    def "Validate username: bad pattern"(){
        given:
        Pattern patterns = pattern("username", "[a-zA-Z0-9-_.@asdfsadf24232%^&*","Username has invalid characters.","pattern for invalid characters");
        ldapPatternRepository.getPattern(_) >> patterns

        when:
        validator.isUsernameValid("someUsername123")

        then:
        thrown(IllegalStateException)
    }

    def "Validate username: null username"(){
        given:
        ldapPatternRepository.getPattern(_) >> usernamePattern

        when:
        validator.isUsernameValid(null)

        then:
        thrown(BadRequestException)
    }

    def "Validate username: empty username"(){
        given:
        Pattern patterns = pattern("username", "^[A-Za-z0-9][a-zA-Z0-9-_.@]*","Username has invalid characters.","pattern for invalid characters");
        ldapPatternRepository.getPattern(_) >> patterns

        when:
        validator.isUsernameValid("")

        then:
        thrown(BadRequestException)
    }

    def "Validate username: spaces username"(){
        given:
        ldapPatternRepository.getPattern(_) >> usernamePattern

        when:
        validator.isUsernameValid("           ")

        then:
        thrown(BadRequestException)
    }

    def "Validate user"(){
        given:

        def userEntity = createUser()

        ldapPatternRepository.getPattern(_) >> usernamePattern
        userService.isUsernameUnique(userEntity.username) >> true
        roleService.getRoleByName(_) >> new ClientRole()
        groupService.getGroupById(_) >> new Group()

        when:
        validator.validateUser(userEntity)

        then:
        1 * defaultRegionService.validateDefaultRegion(userEntity.region)
    }

    def "Validate user when username is invalid"(){
        given:

        def userEntity = createUser().with {
            it.username = "some#Username123@"
            return it
        }

        ldapPatternRepository.getPattern(_) >> usernamePattern
        userService.isUsernameUnique(userEntity.username) >> true
        roleService.getRoleByName(_) >> new ClientRole()
        groupService.getGroupById(_) >> new Group()

        when:
        validator.validateUser(userEntity)

        then:
        thrown(BadRequestException)
    }

    def "Validate user when username is empty string"(){
        given:

        def userEntity = createUser().with {
            it.username = "   "
            return it
        }

        ldapPatternRepository.getPattern(_) >> usernamePattern
        userService.isUsernameUnique(userEntity.username) >> true
        roleService.getRoleByName(_) >> new ClientRole()
        groupService.getGroupById(_) >> new Group()

        when:
        validator.validateUser(userEntity)

        then:
        thrown(BadRequestException)
    }

    def "Validate user when username is null"(){
        given:

        def userEntity = createUser().with {
            it.username = null
            return it
        }

        ldapPatternRepository.getPattern(_) >> usernamePattern
        userService.isUsernameUnique(userEntity.username) >> true
        roleService.getRoleByName(_) >> new ClientRole()
        groupService.getGroupById(_) >> new Group()

        when:
        validator.validateUser(userEntity)

        then:
        thrown(BadRequestException)
    }

    def "Validate user when username is not unique"(){
        given:

        def userEntity = createUser()
        ldapPatternRepository.getPattern(_) >> usernamePattern
        userService.isUsernameUnique(userEntity.username) >> false

        when:
        validator.validateUser(userEntity)

        then:
        thrown(DuplicateUsernameException)
    }

    def "Validate user when password is invalid"(){
        given:

        def userEntity = createUser().with {
            it.password = "pass#`;"
            return it
        }
        ldapPatternRepository.getPattern(_) >> passwordPattern
        userService.isUsernameUnique(userEntity.username) >> true

        when:
        validator.validateUser(userEntity)

        then:
        thrown(BadRequestException)
    }

    def "Validate user when email is invalid"(){
        given:

        def userEntity = createUser().with {
            it.email = "joe joe@email.com"
            return it
        }
        ldapPatternRepository.getPattern(_) >> emailPattern
        userService.isUsernameUnique(userEntity.username) >> true

        when:
        validator.validateUser(userEntity)

        then:
        thrown(BadRequestException)
    }

    def "Validate user validates region for non-subusers when subuser region validation enabled"(){
        given:
        config.getBoolean(Validator.FEATURE_VALIDATE_SUBUSER_DEFAULTREGION_ENABLED_PROP_NAME, _) >> true

        def userEntity = createUser()
        ldapPatternRepository.getPattern(_) >> usernamePattern
        userService.isUsernameUnique(userEntity.username) >> true
        defaultRegionService.validateDefaultRegion(userEntity.region) >> { throw new BadRequestException("invalid") }

        when:
        validator.validateUser(userEntity)

        then:
        thrown(BadRequestException)
    }

    def "Validate user validates region for non-subusers when subuser region validation disabled"(){
        given:
        config.getBoolean(Validator.FEATURE_VALIDATE_SUBUSER_DEFAULTREGION_ENABLED_PROP_NAME, _) >> false

        def userEntity = createUser()
        ldapPatternRepository.getPattern(_) >> usernamePattern
        userService.isUsernameUnique(userEntity.username) >> true
        defaultRegionService.validateDefaultRegion(userEntity.region) >> { throw new BadRequestException("invalid") }

        when:
        validator.validateUser(userEntity)

        then:
        thrown(BadRequestException)
    }

    def "Validate user does not validate region for subuser when flag disabled"(){
        given:
        def subUserRoleName = "identity:default"
        config.getBoolean(Validator.FEATURE_VALIDATE_SUBUSER_DEFAULTREGION_ENABLED_PROP_NAME, _) >> false
        config.getString("cloudAuth.userRole") >> subUserRoleName

        def userEntity = createUser()
        TenantRole subUserRole = new TenantRole().with {
            it.name = subUserRoleName
            return it
        }
        userEntity.getRoles().add(subUserRole)
        ldapPatternRepository.getPattern(_) >> usernamePattern
        userService.isUsernameUnique(userEntity.username) >> true
        roleService.getRoleByName(_) >> new ClientRole()
        groupService.getGroupById(_) >> new Group()
        defaultRegionService.validateDefaultRegion(userEntity.region) >> { throw new BadRequestException("invalid") }

        when:
        validator.validateUser(userEntity)

        then:
        notThrown(BadRequestException)
    }

    def "Validate user does validate region for subuser when flag enabled"(){
        given:
        def subUserRoleName = "identity:default"
        config.getBoolean(Validator.FEATURE_VALIDATE_SUBUSER_DEFAULTREGION_ENABLED_PROP_NAME, _) >> true
        config.getString("cloudAuth.userRole") >> subUserRoleName

        def userEntity = createUser()
        TenantRole subUserRole = new TenantRole().with {
            it.name = subUserRoleName
            return it
        }
        userEntity.getRoles().add(subUserRole)
        ldapPatternRepository.getPattern(_) >> usernamePattern
        userService.isUsernameUnique(userEntity.username) >> true
        roleService.getRoleByName(_) >> new ClientRole()
        groupService.getGroupById(_) >> new Group()
        defaultRegionService.validateDefaultRegion(userEntity.region) >> { throw new BadRequestException("invalid") }

        when:
        validator.validateUser(userEntity)

        then:
        thrown(BadRequestException)
    }

    def "Validate user when role is invalid"(){
        given:

        def userEntity = createUser()
        ldapPatternRepository.getPattern(_) >> usernamePattern
        userService.isUsernameUnique(userEntity.username) >> true
        roleService.getRoleByName(_) >> null

        when:
        validator.validateUser(userEntity)

        then:
        thrown(BadRequestException)
    }

    def "Validate user when group is invalid"(){
        given:

        def userEntity = createUser()
        ldapPatternRepository.getPattern(_) >> usernamePattern
        userService.isUsernameUnique(userEntity.username) >> true
        roleService.getRoleByName(_) >> new ClientRole()
        groupService.getGroupById(_) >> null

        when:
        validator.validateUser(userEntity)

        then:
        thrown(BadRequestException)
    }

    def setupMock(){
        ldapPatternRepository = Mock()
        userService = Mock()
        defaultRegionService = Mock()
        roleService = Mock()
        groupService = Mock()
        config = Mock()


        validator.ldapPatternRepository = ldapPatternRepository
        validator.userService = userService
        validator.roleService = roleService
        validator.groupService = groupService
        validator.defaultRegionService = defaultRegionService
        validator.config = config
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

    def createUser() {
        com.rackspace.idm.domain.entity.User userEntity = new com.rackspace.idm.domain.entity.User().with {
            it.username = "joe.racker"
            it.password = "myPassword"
            it.email = "joe@email.com"
            it.region = "DFW"
            it.roles = [ new TenantRole().with{it.name = "observer"; return it} ].asList()
            it.rsGroupId = ["groupId"].asList()
            return it
        }
    }
}
