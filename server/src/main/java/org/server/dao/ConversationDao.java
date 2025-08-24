package org.server.dao;

import org.neo4j.ogm.model.Result;
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

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

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
                    MATCH (c:Conversation)-[:HAS]->(u1:User), (c)-[:HAS]->(u2:User)
                    WHERE u1.emailAddress = $currentUserEmailAddress AND u2.emailAddress = $targetUserEmailAddress
                    WITH c, COUNT(DISTINCT u2) AS participantCount
                    WHERE participantCount = 2
                    RETURN c
                    """, Map.of("currentUserEmailAddress", currentUser.getEmailAddress(),
                    "targetUserEmailAddress", targetUser.getEmailAddress()));
            if (sessionFactory != null) {
                sessionFactory.close();
            }
            return conversation;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Conversation> searchUserConversations(User user) {
        try {
            Session session = this.sessionFactory.openSession();
            String cypher = "MATCH (u:User {emailAddress: $emailAddress}) " +
                    "MATCH (conv:Conversation)-[:HAS]->(u) " +
                    "RETURN conv LIMIT 20 ";
            Result records = session.query(cypher,
                    Map.of("emailAddress", user.getEmailAddress()));
            List<Conversation> conversations = new ArrayList<>();
            for (var record : records) {
                Conversation conversation = (Conversation) record.get("conv");
                String cypher2 = "MATCH (c:Conversation)-[:HAS]->(u:User) WHERE id(c) = $conversationId\n" +
                        "RETURN u";
                Result recordsUser = session.query(cypher2, Map.of("conversationId", conversation.getId()));
                List<User> participants = new ArrayList<>();
                for (var recordUser : recordsUser) {
                    User participant = (User) recordUser.get("u");
                    participants.add(participant);
                }
                String cypher3 = "MATCH (c:Conversation)-[:CONTAINS]->(msg:Message) WHERE id(c) = $conversationId\n" +
                        "RETURN msg\n" +
                        "ORDER BY msg.time DESC\n" +
                        "LIMIT 20";
                Result recordMessages = session.query(cypher3, Map.of("conversationId", conversation.getId()));
                List<Message> messages = new ArrayList<>();
                for (var recordMessage : recordMessages) {
                    Message message = (Message) recordMessage.get("msg");
                    messages.add(message);
                }
                conversation.setParticipants(new HashSet<>(participants));
                conversation.setMessages(messages);
                conversations.add(conversation);
            }
            if (sessionFactory != null) {
                sessionFactory.close();
            }
            return conversations;
        }
        catch (Exception e) {
            e.printStackTrace();
            if (sessionFactory != null) {
                sessionFactory.close();
            }
        }
        return new ArrayList<>();
    }

    public Conversation createConversation(Set<User> users, Message message, User sender) {
        try {
            Session session = this.sessionFactory.openSession();
            // Start a transaction
            try (Transaction tx = session.beginTransaction()) {
                // Create message node
                String createMessageCypher =
                        "CREATE (m:Message {content: $content, time: $time}) " +
                                "WITH m " +
                                "MATCH (u:User {emailAddress: $emailAddress}) " +
                                "MERGE (m)<-[:SENT_BY]-(u) " +
                                " RETURN m";
                Message createdMessage = session.queryForObject(Message.class, createMessageCypher,
                        Map.of("content", message.getContent(), "time", message.getTime(), "emailAddress",
                                sender.getEmailAddress()));

                List<String> userEmailAddresses = users.stream().map(User::getEmailAddress).toList();

                String linkUsersCypher =
                        "CREATE (conv:Conversation)\n" +
                                "WITH conv\n" +
                                "UNWIND $usersEmailAddresses AS email\n" +
                                "MATCH (u:User {emailAddress: email})\n" +
                                "MERGE (conv)-[:HAS]->(u)\n" +
                                "WITH conv\n" +
                                "MATCH (m:Message)\n" +
                                "WHERE id(m) = $messageId\n" +
                                "MERGE (conv)-[:CONTAINS]->(m)\n" +
                                "RETURN conv";
                Conversation fullConversation = session.queryForObject(Conversation.class, linkUsersCypher, Map.of("usersEmailAddresses", userEmailAddresses,"messageId",
                        createdMessage.getId()));

                tx.commit();

                return fullConversation;
            }
        } catch (Exception e) {
            e.printStackTrace();
            errorMessage = e.getMessage();
            return null;
        }
    }

}
