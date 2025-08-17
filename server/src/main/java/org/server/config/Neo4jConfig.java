package org.server.config;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.ogm.config.Configuration;
import org.neo4j.ogm.session.SessionFactory;

public class Neo4jConfig {

    public static Configuration getConfiguration() {
        return new Configuration.Builder()
                .uri("neo4j://localhost:7687")
                .credentials("neo4j", "password123")
                .build();
    }

    public static SessionFactory getSessionFactory() {
        return new SessionFactory(getConfiguration(), "org.shared.entity");
    }

}
