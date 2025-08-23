package org.server.dao;

import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.server.config.Neo4jConfig;
import org.shared.entity.Conversation;
import org.shared.entity.User;

import java.util.Map;

public class ConversationDao {
    private SessionFactory sessionFactory;
    public static String errorMessage = "";

    public ConversationDao() {
        this.sessionFactory = Neo4jConfig.getSessionFactory();
    }

    public Conversation searchConversationIfExists(User currentUser, User targetUser) {
        try {
            Session session = this.sessionFactory.openSession();
            Conversation conversation = session.queryForObject(Conversation.class, """
                    MATCH (c:Conversation)-[:PARTICIPATES_IN]->(u1:User), (c)-[:PARTICIPATES_IN]->(u2:User)
                    WHERE u1.emailAddress = $currentUserEmailAddress AND u2.emailAddress = $targetUserEmailAddress
                    WITH c, COUNT(DISTINCT u2) AS participantCount
                    WHERE participantCount = 2
                    RETURN c
                    """, Map.of("currentUserEmailAddress", currentUser.getEmailAddress(),
                    "targetUserEmailAddress", targetUser.getEmailAddress()));
            return conversation;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
