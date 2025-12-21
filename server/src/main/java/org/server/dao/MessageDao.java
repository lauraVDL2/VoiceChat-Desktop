package org.server.dao;

import org.apache.commons.lang3.StringUtils;
import org.neo4j.ogm.model.Result;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.transaction.Transaction;
import org.server.config.Neo4jConfig;
import org.shared.entity.Conversation;
import org.shared.entity.Message;
import org.shared.entity.ReadStatus;
import org.shared.entity.User;

import java.util.*;

public class MessageDao {

    private SessionFactory sessionFactory;
    public static String errorMessage = "";

    public MessageDao(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public Message sendMessage(Conversation conversation) {
        try {
            Session session = this.sessionFactory.openSession();
            // Normally, we should insert only one message per request
            Message lastMessage = conversation.getMessages().stream().findFirst().orElse(null);
            if (lastMessage != null) {
                try (Transaction tx = session.beginTransaction()) {
                    String cypher = """
                            MATCH (c:Conversation) WHERE id(c) = $conversationId
                            MATCH (u:User {emailAddress: $emailAddress})
                            CREATE (msg:Message {content: $content, time: $time})
                            CREATE (c)-[:CONTAINS]->(msg)
                            CREATE (msg)<-[:SENT_BY]-(u)
                            RETURN msg,u
                            """;
                    var result = session.query(cypher, Map.of("conversationId", conversation.getId(),
                            "emailAddress", lastMessage.getSender().getEmailAddress(), "content", lastMessage.getContent(),
                            "time", lastMessage.getTime()));
                    Message message = null;
                    User user = null;
                    for (var record : result) {
                        message = (Message) record.get("msg");
                        user = (User) record.get("u");
                    }

                    String cypherParticipants = """
                    MATCH (c:Conversation)-[:HAS]->(u:User) WHERE id(c) = $id RETURN u
                    """;
                    Result usersRecord = session.query(cypherParticipants, Map.of("id", conversation.getId()));
                    Set<User> participants = new LinkedHashSet<>();
                    for (var userRecord : usersRecord) {
                        User participant = (User) userRecord.get("u");
                        participants.add(participant);
                    }

                    // Read status
                    List<ReadStatus> readStatuses = new ArrayList<>();
                    for (User participant : participants) {
                        boolean isRead;
                        if (!StringUtils.equals(participant.getEmailAddress(), user.getEmailAddress())) {
                            isRead = false;
                        } else {
                            isRead = true;
                        }
                        String cypher2 = """
                                MATCH (u:User {emailAddress: $emailAddress})
                                MATCH (m:Message) WHERE id(m) = $messageId
                                MERGE (m)<-[r:READ_BY {isRead: $isRead}]-(u)
                                RETURN r
                                """;
                        ReadStatus readStatus = session.queryForObject(ReadStatus.class, cypher2, Map.of("isRead", isRead, "emailAddress",
                                participant.getEmailAddress(), "messageId", message.getId()));
                        readStatuses.add(readStatus);

                    }
                    tx.commit();
                    message.setReadStatuses(readStatuses);
                    message.setSender(user);
                    if (sessionFactory != null) {
                        sessionFactory.close();
                    }
                    return message;
                }
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            if (sessionFactory != null) {
                sessionFactory.close();
            }
        }
        return null;
    }

    public List<Message> searchMessageInConversation(Conversation conversation) {
        try {
            Session session = this.sessionFactory.openSession();
            String cypher = """
                    MATCH (c:Conversation)-[:CONTAINS]->(msg:Message) WHERE id(c) = $id
                    AND msg.content CONTAINS $messageContent
                    RETURN msg AS messages LIMIT 20
                    """;
            Result records = session.query(cypher, Map.of("id", conversation.getId(),
                    "messageContent", conversation.getMessages().getFirst().getContent()));
            List<Message> messages = new ArrayList<>();
            for (var record : records) {
                Message message = (Message) record.get("messages");
                messages.add(message);
            }
            return messages;
        } catch (Exception e) {
            e.printStackTrace();
            if (sessionFactory != null) {
                sessionFactory.close();
            }
        }
        return null;
    }

}
