package com.rackspace.idm.util

import org.apache.commons.configuration.Configuration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import spock.lang.Shared
import spock.lang.Specification

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 4/19/13
 * Time: 10:39 AM
 * To change this template use File | Settings | File Templates.
 */
@ContextConfiguration(locations = "classpath:app-config.xml")
class DefaultEncryptionPasswordSourceTest extends Specification{
    @Autowired
    DefaultEncryptionPasswordSource encryptionPasswordSource

    @Shared String filename
    @Shared String password

    def setupSpec(){
        filename = "DPST_filename"
        password = "this is a super secret key!"
    }

    def setup(){
        def writer = new FileWriter(new File(filename))
        writer.write("1|newPassword")
        writer.flush()
    }

    def cleanup(){
        def file = new File(filename)
        if(file.exists()){
            file.delete()
        }
    }

    def "getPassword gets latest password"(){
        given:
        encryptionPasswordSource.init()
        encryptionPasswordSource.setPassword("don't use me")
        encryptionPasswordSource.setPassword("use me")

        when:
        def password = encryptionPasswordSource.getPassword()

        then:
        password == "use me"
    }

    def "getPassword with not defined password file returns config password"(){
        given:
        def file = new File(filename)
        if(file.exists()){
            file.delete()
        }
        encryptionPasswordSource.init()

        when:
        def password = encryptionPasswordSource.getPassword()

        then:
        password == password
    }

    def "getPassword with Encryption File defined returns latest password" (){
        given:
        encryptionPasswordSource.init()

        when:
        String password = encryptionPasswordSource.getPassword()

        then:
        password == "newPassword"
    }

    def "getPassword with version returns password" (){
        given:
        encryptionPasswordSource.init()

        when:
        String password = encryptionPasswordSource.getPassword(0)

        then:
        password == password
    }

    def "Set password sets to current password" () {
        given:
        encryptionPasswordSource.init()

        when:
        encryptionPasswordSource.setPassword("otherPassword")
        String password = encryptionPasswordSource.getPassword()

        then:
        password == "otherPassword"
    }

    def "Setting password writes to file" () {
        given:
        encryptionPasswordSource.init()
        def pwd = ""

        when:
        encryptionPasswordSource.setPassword("otherPassword")
        File file = new File(filename);
        if(file.exists()){
            FileReader fileReader = new FileReader(file);
            BufferedReader br = new BufferedReader(fileReader);
            String password;
            while((password = br.readLine()) != null){
                if(password.trim().length() > 0){
                    pwd = password
                }
            }
        }

        then:
        pwd == "otherPassword"
    }
}
