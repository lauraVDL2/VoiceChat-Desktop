package org.server.config;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.ogm.config.Configuration;

public class Neo4jConfig {

    public static Driver getConfiguration() {
        return GraphDatabase.driver(
                "neo4j://localhost:7687",
                AuthTokens.basic("neo4j", "password123")
        );
    }

}
