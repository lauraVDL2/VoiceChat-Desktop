package org.server.dao;

import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.shared.entity.Settings;
import org.shared.entity.ThemeMode;
import org.shared.entity.User;

import java.util.Map;

public class SettingsDaoImpl implements SettingsDao {

    private SessionFactory sessionFactory;
    public static String errorMessage = "";

    public SettingsDaoImpl(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public Settings setThemeMode(User user) {
        try {
            Session session = sessionFactory.openSession();
            Settings settings = getOrCreateSettings(user);
            ThemeMode themeMode = user.getSettings().getThemeMode();
            if (settings != null) {
                String cypher = """
                            MATCH (s:Settings)
                            WHERE id(s) = $settingsId
                            SET s.themeMode = $newThemeMode
                            RETURN s
                        """;
                Settings newSettings = session.queryForObject(Settings.class, cypher,
                        Map.of("settingsId", settings.getId(), "newThemeMode", themeMode));
                sessionFactory.close();
                return newSettings;
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (sessionFactory != null) {
                sessionFactory.close();
            }
        }
        return null;
    }


    @Override
    public Settings getOrCreateSettings(User user) {
        try {
            Session session = sessionFactory.openSession();
            if (user != null) {
                String cypher = """
                        MATCH (u:User {emailAddress: $emailAddress})-[:HAS]->(s:Settings)
                        RETURN s
                        """;
                Settings settings = session.queryForObject(Settings.class, cypher,
                        Map.of("emailAddress", user.getEmailAddress()));
                if (settings == null) {
                    cypher = """
                                MERGE (u:User {emailAddress: $emailAddress})
                                WITH u
                                OPTIONAL MATCH (u)-[:HAS]->(s:Settings)
                                WHERE s IS NULL
                                CREATE (newSettings:Settings { themeMode: 'LIGHT' })
                                CREATE (u)-[:HAS]->(newSettings)
                                RETURN newSettings
                            """;
                    settings = session.queryForObject(Settings.class, cypher,
                            Map.of("emailAddress", user.getEmailAddress()));
                }
                sessionFactory.close();
                return settings;
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (sessionFactory != null) {
                sessionFactory.close();
            }
        }
        return null;
    }
}
