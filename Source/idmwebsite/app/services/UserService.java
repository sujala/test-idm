package services;

import java.util.List;
import models.User;
import org.apache.http.HttpException;

public interface UserService {

    boolean addUser(User user);
    void deleteUser(String username);
    void updateUser(User user);
    List<User> getAllUsers(String customerId);
    User getUser(String username) throws HttpException;
}
