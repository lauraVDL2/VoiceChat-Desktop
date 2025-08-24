package org.server.dao;

import org.mindrot.jbcrypt.BCrypt;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.server.config.Neo4jConfig;
import org.shared.entity.User;

import java.util.*;
import java.util.regex.Pattern;

public class UserDao {
    private SessionFactory sessionFactory;
    public static String errorMessage = "";

    public UserDao() {
        // Initialize the driver once
        this.sessionFactory = Neo4jConfig.getSessionFactory();
    }

    /*public void createConstraints() {
        try {
            Session session = this.sessionFactory.openSession();
            session.query("CREATE CONSTRAINT IF NOT EXISTS FOR (u:User) REQUIRE u.emailAddress IS UNIQUE", Collections.emptyMap());
            this.sessionFactory.close();
        } catch (Exception e) {
            e.printStackTrace();
            if (sessionFactory != null) {
                this.sessionFactory.close();
            }
        }
    }*/

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

    public List<User> searchUsers(String searchField) {
        try {
            Session session = sessionFactory.openSession();
            String pattern = "(?i).*" + Pattern.quote(searchField) + ".*";
            Iterable<User> usersIterable = session.query(User.class, "MATCH (u:User) WHERE u.displayName =~ $displayName RETURN u LIMIT 20",
                    Map.of("displayName", pattern));
            List<User> usersFound = new ArrayList<>();
            usersIterable.forEach(usersFound::add);
            return usersFound;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }
}