package org.server.dao;

import org.neo4j.driver.Driver;
import org.neo4j.ogm.config.Configuration;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.server.config.Neo4jConfig;
import org.shared.entity.User;

import java.util.Collections;
import java.util.UUID;

public class UserDao {
    private SessionFactory sessionFactory;

    public UserDao() {
        // Initialize the driver once
        this.sessionFactory = Neo4jConfig.getSessionFactory();
    }

    public void createConstraints() {
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
    }

    public boolean saveUser(User user) {
        try {
            this.createConstraints();
            Session session = this.sessionFactory.openSession();
            session.save(user);
            this.sessionFactory.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            if (sessionFactory != null) {
                this.sessionFactory.close();
            }
            return false;
        }
    }
}