package services;

import java.util.List;
import models.User;
import org.apache.http.HttpException;

public class DefaultUserService implements UserService {

    private IdmService idmService;

    public DefaultUserService(IdmService idmService) {
        this.idmService = idmService;
    }

    public boolean addUser(User user) {
        boolean success = idmService.addUser(user);
        return success;
    }

    public void deleteUser(String username) {
        idmService.deleteUser(username);
    }

    public void updateUser(User user) {
        idmService.updateUser(user);
    }

    public List<User> getAllUsers(String customerId) {
        return idmService.getUsersByCustomerId(customerId);
    }

    public User getUser(String username) throws HttpException {
        return idmService.getUser(username);
    }
}
