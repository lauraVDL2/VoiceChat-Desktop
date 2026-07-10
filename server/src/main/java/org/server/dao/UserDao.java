package org.server.dao;

import org.shared.entity.User;

import java.util.List;

public interface UserDao {

    User login(User user);
    boolean saveUser(User user);
    User findUserByEmailAddress(String emailAddress);
    List<User> searchUsers(String searchField);
}
