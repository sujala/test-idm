package testHelpers;

import com.rackspace.idm.domain.entity.User;
import lombok.Builder;
import lombok.Data;
import lombok.FluentSetter;

import java.lang.String;

@Data
@FluentSetter
public class UserBuilder extends ObjectMapper<User> {
    private String username;
    private String displayName;
    private String id;
    private String domainId;
    private String email;
    private String password;
    private boolean enabled;
    private String region;
    private String uniqueId;
}
