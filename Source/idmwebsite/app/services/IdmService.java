package services;

import java.util.List;
import models.User;
import org.apache.http.HttpException;

public interface IdmService {

    boolean addUser(User user);
    boolean deleteUser(String username);
    boolean updateUser(User user);
    String getTokenByUsername(String username, String password);
    List<User> getUsersByCustomerId(String customerId);
    User getUser(String username) throws HttpException;
}
