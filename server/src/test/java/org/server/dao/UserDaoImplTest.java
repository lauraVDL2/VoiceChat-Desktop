package org.server.dao;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;
import org.neo4j.harness.Neo4j;
import org.neo4j.harness.Neo4jBuilders;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.shared.entity.Settings;
import org.shared.entity.ThemeMode;
import org.shared.entity.User;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class UserDaoImplTest {

    private static UserDaoImpl userDao;

    private static SessionFactory sessionFactory;

    private static Session session;

    private static Neo4j neo4j;

    @BeforeAll
    public static void setup() {
        neo4j = Neo4jBuilders.newInProcessBuilder().build();

        sessionFactory = new SessionFactory(new org.neo4j.ogm.config.Configuration.Builder()
                .uri(neo4j.boltURI().toString())
                .build(),
                "org.shared.entity");

        session = sessionFactory.openSession();

        userDao = new UserDaoImpl(sessionFactory);
    }

    @Test
    void testSaveUser() {
        User user = createDummyUser("test1.toto@yahoo.fr", "toto", "password123");

        boolean isSaved = userDao.saveUser(user);

        assertTrue(isSaved);
    }

    @Test
    void testLogin() {
        User user = createDummyUser("test2.toto@yahoo.fr", "toto", "password123");

        session.save(user);

        user.setPassword("password123");
        User userSaved = userDao.login(user);

        assertNotNull(userSaved);
        assertEquals(user.getEmailAddress(), userSaved.getEmailAddress());
        assertEquals(user.getUserName(), userSaved.getUserName());
    }

    @Test
    void testFindUserByEmailAddress() {
        User user = createDummyUser("test3.toto@yahoo.fr", "toto", "password123");

        session.save(user);

        User userFound = userDao.findUserByEmailAddress(user.getEmailAddress());

        assertNotNull(userFound);
        assertEquals(userFound.getEmailAddress(), user.getEmailAddress());
    }

    @Test
    void testSearchUsers() {
        User user = createDummyUser("test.test@yahoo.fr", "test user", "password123");

        session.save(user);

        //Only few letters are enough to find the user
        List<User> users = userDao.searchUsers("test");

        assertNotNull(users);
        assertFalse(users.isEmpty());
        assertEquals(users.getFirst().getEmailAddress(), user.getEmailAddress());
    }

    private User createDummyUser(String emailAddress, String username, String password) {
        User user = new User();
        user.setEmailAddress(emailAddress);
        user.setDisplayName(username);
        user.setUserName(username);
        user.setPassword(BCrypt.hashpw(password, BCrypt.gensalt()));
        return user;
    }

    @AfterAll
    public static void tearDown() {
        if (sessionFactory != null) {
            sessionFactory.close();
        }
        neo4j.close();
    }

    @AfterEach
    public void shutDown() {
        session.purgeDatabase();
        session.clear();
    }
}
