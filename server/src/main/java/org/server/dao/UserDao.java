package org.server.dao;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.server.config.Neo4jConfig;
import org.shared.User;

import java.util.UUID;

public class UserDao {
    private Driver driver;

    public UserDao() {
        // Initialize the driver once
        this.driver = Neo4jConfig.getConfiguration();
    }

    // Create constraints method
    public void createConstraints() {
        try (Session session = driver.session()) {
            String cypher = "CREATE CONSTRAINT IF NOT EXISTS FOR (u:User) REQUIRE u.emailAddress IS UNIQUE";

            // Execute query
            session.executeWrite(tx -> {
                tx.run(cypher);
                return null;
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Save user method
    public boolean saveUser(User user) {
        createConstraints();
        try (Session session = driver.session()) {
            session.executeWrite(tx -> {
                String cypher = "MERGE (u:User {id: $id, emailAddress: $emailAddress," +
                        " displayName: $displayName, password: $password})";
                tx.run(cypher,
                        org.neo4j.driver.Values.parameters(
                                "id", UUID.randomUUID().toString(),
                                "emailAddress", user.getEmailAddress(),
                                "displayName", user.getDisplayName(),
                                "password", user.getPassword()
                        )
                );
                return null;
            });
            return true;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // Close driver on shutdown
    public void close() {
        if (driver != null) {
            driver.close();
        }
    }
}