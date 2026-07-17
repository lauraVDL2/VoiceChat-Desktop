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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class SettingsDaoImplTest {
    private static SettingsDaoImpl settingsDao;

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

        settingsDao = new SettingsDaoImpl(sessionFactory);
    }

    @Test
    void testGetOrCreateSettings() {
        User user = createDummyUser("toto1.toto@yahoo.fr", "toto1", "toto1");

        Settings settings = settingsDao.getOrCreateSettings(user);

        assertNotNull(settings);
    }

    @Test
    void testChangeTheme() {
        User user = createDummyUser("toto1.toto@yahoo.fr", "toto1", "toto1");

        Settings settings = settingsDao.getOrCreateSettings(user);

        settings.setThemeMode(ThemeMode.DARK);
        user.setSettings(settings);

        Settings newSettings = settingsDao.setThemeMode(user);

        assertNotNull(newSettings);
        assertEquals(ThemeMode.DARK, newSettings.getThemeMode());
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
