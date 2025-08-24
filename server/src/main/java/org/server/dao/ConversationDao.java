package org.server.dao;

import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.transaction.Transaction;
import org.server.config.Neo4jConfig;
import org.shared.entity.Conversation;
import org.shared.entity.Message;
import org.shared.entity.ReadStatus;
import org.shared.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ConversationDao {
    private static final Logger log = LoggerFactory.getLogger(ConversationDao.class);
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

    public Conversation createConversation(Set<User> users, Message message) {
        try {
            Session session = this.sessionFactory.openSession();

            Conversation conversation = new Conversation();
            String mergeUsersCypher =
                    "UNWIND $users AS user\n" +
                            "MATCH (u:User {emailAddress: user.emailAddress})\n" +
                            "SET u.displayName = user.displayName";
            try (Transaction tx = session.beginTransaction()) {

                session.query(mergeUsersCypher, Map.of("users", users));

                String createMessageCypher =
                        "CREATE (m:Message {content: $content, timestamp: datetime()})";
                session.query(createMessageCypher, Map.of("content", message.getContent()));

                String createConversationCypher =
                        "MERGE (conv:Conversation)";
                session.query(createConversationCypher, Map.of());

                String linkUsersToConvCypher =
                        "UNWIND $users AS user\n" +
                                "MATCH (u:User {emailAddress: user.emailAddress})\n" +
                                "MERGE (u)-[:PARTICIPATES_IN]->(conv)";
                session.query(linkUsersToConvCypher, Map.of("users", users));

                String linkConvToMessageCypher =
                        "MATCH (conv:Conversation), (m:Message)\n" +
                                "WHERE NOT exists((conv)-[:CONTAINS]->(m))\n" +
                                "CREATE (conv)-[:CONTAINS]->(m)";
                session.query(linkConvToMessageCypher, Map.of());

                tx.commit();

                if (sessionFactory != null) {
                    sessionFactory.close();
                }

                return conversation;
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
