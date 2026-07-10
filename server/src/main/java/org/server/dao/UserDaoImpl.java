package org.server.dao;

import org.mindrot.jbcrypt.BCrypt;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.shared.entity.User;

import java.util.*;
import java.util.regex.Pattern;

public class UserDaoImpl implements UserDao {
    private SessionFactory sessionFactory;
    public static String errorMessage = "";

    public UserDaoImpl(SessionFactory sessionFactory) {
        // Initialize the driver once
        this.sessionFactory = sessionFactory;
    }

    @Override
    public User login(User user) {
        try {
            Session session = sessionFactory.openSession();
            User loggedUser = session.queryForObject(User.class, "MATCH (u:User {emailAddress:$emailAddress}) RETURN u",
                    Map.of("emailAddress", user.getEmailAddress()));
            if (loggedUser != null) {
                if (BCrypt.checkpw(user.getPassword(), loggedUser.getPassword())) {
                    sessionFactory.close();
                    return loggedUser;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (sessionFactory != null) {
                sessionFactory.close();
            }
        }
        errorMessage = "Invalid email address or password";
        return null;
    }

    @Override
    public boolean saveUser(User user) {
        try {
            //this.createConstraints();
            Session session = this.sessionFactory.openSession();
            session.save(user);
            this.sessionFactory.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            errorMessage = "This email address already exists in the database";
            if (sessionFactory != null) {
                this.sessionFactory.close();
            }
            return false;
        }
    }

    @Override
    public User findUserByEmailAddress(String emailAddress) {
        try {
            Session session = sessionFactory.openSession();
            return session.queryForObject(User.class, "MATCH (u:User {emailAddress: $emailAddress}) RETURN u",
                    Map.of("emailAddress", emailAddress));
        }
        catch (Exception e) {
            e.printStackTrace();
            if (sessionFactory != null) {
                this.sessionFactory.close();
            }
            return null;
        }
    }

    @Override
    public List<User> searchUsers(String searchField) {
        try {
            Session session = sessionFactory.openSession();
            String pattern = "(?i).*" + Pattern.quote(searchField) + ".*";
            Iterable<User> usersIterable = session.query(User.class, "MATCH (u:User) WHERE u.displayName =~ $displayName RETURN u LIMIT 20",
                    Map.of("displayName", pattern));
            List<User> usersFound = new ArrayList<>();
            usersIterable.forEach(usersFound::add);
            if (sessionFactory != null) {
                sessionFactory.close();
            }
            return usersFound;
        } catch (Exception e) {
            e.printStackTrace();
            if (sessionFactory != null) {
                sessionFactory.close();
            }
        }
        return new ArrayList<>();
    }
}