package org.server.dao;

import org.neo4j.ogm.model.Result;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.transaction.Transaction;
import org.shared.entity.Conversation;
import org.shared.entity.Message;
import org.shared.entity.ReadStatus;
import org.shared.entity.User;
import org.shared.pojo.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class ConversationDaoImpl implements ConversationDao {
    private static final Logger log = LoggerFactory.getLogger(ConversationDaoImpl.class);
    private SessionFactory sessionFactory;
    public static String errorMessage = "";

    public ConversationDaoImpl(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public Conversation searchConversationIfExists(User currentUser, User targetUser) {
        try {
            Session session = this.sessionFactory.openSession();
            try (var tx = session.beginTransaction()) {
                // Get conversation
                Conversation conversation = session.queryForObject(Conversation.class, """
                        MATCH (u1:User {emailAddress: $currentUserEmailAddress})
                        MATCH (u2:User {emailAddress: $targetUserEmailAddress})
                        MATCH (c:Conversation)-[:HAS]->(u:User)
                        WHERE (c)-[:HAS]->(u1) AND (c)-[:HAS]->(u2)
                        WITH c, collect(DISTINCT u) AS users
                        WHERE size(users) = 2
                        RETURN c
                        """, Map.of("currentUserEmailAddress", currentUser.getEmailAddress(),
                        "targetUserEmailAddress", targetUser.getEmailAddress()));
                if (conversation != null) {
                    // Get messages of the conversation
                    String cypher = """
                            MATCH (c:Conversation)-[:CONTAINS]->(msg:Message)
                            WHERE id(c) = $conversationId
                            WITH msg
                            MATCH (msg:Message)<-[:SENT_BY]-(u:User)
                            RETURN msg, u ORDER BY msg.time DESC LIMIT 20
                            """;
                    Result records = session.query(cypher, Map.of("conversationId", conversation.getId()));
                    List<Message> messages = new ArrayList<>();
                    for (var record : records) {
                        Message message = (Message) record.get("msg");
                        User sender = (User) record.get("u");
                        message.setSender(sender);
                        messages.add(message);
                    }
                    conversation.setMessages(messages.stream()
                            .sorted(Comparator.comparing(Message::getTime))
                            .collect(Collectors.toList()));
                }
                // Commit transaction
                tx.commit();
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

    @Override
    public Set<User> getConversationParticipants(Conversation conversation) {
        try {
            Session session = this.sessionFactory.openSession();
            String cypherParticipants = """
                    MATCH (c:Conversation)-[:HAS]->(u:User) WHERE id(c) = $id RETURN u
                    """;
            Result usersRecord = session.query(cypherParticipants, Map.of("id", conversation.getId()));
            Set<User> participants = new LinkedHashSet<>();
            for (var userRecord : usersRecord) {
                User participant = (User) userRecord.get("u");
                participants.add(participant);
            }
            if (sessionFactory != null) {
                sessionFactory.close();
            }
            return participants;
        } catch (Exception e) {
            e.printStackTrace();
            if (sessionFactory != null) {
                sessionFactory.close();
            }
        }
        return new LinkedHashSet<>();
    }

    @Override
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
                String cypher3 = """
                        MATCH (c:Conversation)-[:CONTAINS]->(msg:Message)
                        WHERE id(c) = $conversationId
                        WITH msg
                        OPTIONAL MATCH (msg)<-[r:READ_BY]-(u:User {emailAddress: $emailAddress})
                        RETURN msg, r.isRead AS isRead ORDER BY msg.time DESC LIMIT 20
                        """;
                Result recordMessages = session.query(cypher3, Map.of("conversationId", conversation.getId(),
                        "emailAddress", user.getEmailAddress()));
                List<Message> messages = new ArrayList<>();
                for (var recordMessage : recordMessages) {
                    Message message = (Message) recordMessage.get("msg");
                    Boolean isRead = (Boolean) recordMessage.get("isRead");
                    if (isRead != null) {
                        ReadStatus readStatus = new ReadStatus();
                        readStatus.setRead(isRead);
                        readStatus.setUser(user);
                        readStatus.setMessage(message);
                        message.setReadStatuses(List.of(readStatus));
                    }
                    messages.add(message);
                }
                conversation.setParticipants(new HashSet<>(participants));
                conversation.setMessages(messages.stream()
                        .sorted(Comparator.comparing(Message::getTime))
                        .collect(Collectors.toList()));
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

    @Override
    public int getOffset(Conversation conversation) {
        try {
            Session session = this.sessionFactory.openSession();
            String cypher1 = """
                    MATCH (c:Conversation)-[:CONTAINS]->(m:Message)
                    WHERE id(c) = $conversationId AND m.time > $targetMessageTime
                    RETURN count(m)
                    """;
            int messageCount = session.queryForObject(Integer.class, cypher1, Map.of("conversationId", conversation.getId(),
                    "targetMessageTime", conversation.getMessages().getFirst().getTime()));
            int skip = messageCount;
            int pageSize = 20;
            int offset = skip/pageSize;
            return offset;
        } catch (Exception e) {
            e.printStackTrace();
            if (sessionFactory != null) {
                sessionFactory.close();
            }
        }
        return 0;
    }

    @Override
    public Conversation scrollConversationMessages(Conversation conversation, Page page) {
        try {
            Session session = this.sessionFactory.openSession();
            int skip = page.getOffset() * page.getLimit();
            String cypher = """
                    MATCH (c:Conversation)-[:CONTAINS]->(msg:Message) WHERE id(c) = $id
                    WITH msg MATCH (msg:Message)<-[:SENT_BY]-(u:User)
                    RETURN msg, u ORDER BY msg.time DESC SKIP $skip LIMIT $limit
                    """;
            Result records = session.query(cypher, Map.of("id", conversation.getId(), "skip", skip,
                    "limit", page.getLimit()));
            List<Message> messages = new ArrayList<>();
            for (var record : records) {
                Message message = (Message) record.get("msg");
                User sender = (User) record.get("u");
                message.setSender(sender);
                messages.add(message);
            }

            conversation.setMessages(messages.stream()
                    .sorted(Comparator.comparing(Message::getTime))
                    .collect(Collectors.toList()));
            String cypher2 = """
                    MATCH (c:Conversation)-[:HAS]->(u:User) WHERE id(c) = $id RETURN u
                    """;
            Result usersRecord = session.query(cypher2, Map.of("id", conversation.getId()));
            Set<User> participants = new LinkedHashSet<>();
            for (var userRecord : usersRecord) {
                User participant = (User) userRecord.get("u");
                participants.add(participant);
            }
            conversation.setParticipants(participants);
            if (sessionFactory != null) {
                sessionFactory.close();
            }
            return conversation;
        } catch (Exception e) {
            e.printStackTrace();
            if (sessionFactory != null) {
                sessionFactory.close();
            }
        }
        return null;
    }

    @Override
    public Conversation getConversation(Conversation conversation) {
        try {
            Session session = this.sessionFactory.openSession();
            String cypher = """
                    MATCH (c:Conversation)-[:CONTAINS]->(msg:Message) WHERE id(c) = $id
                    WITH msg MATCH (msg:Message)<-[:SENT_BY]-(u:User)
                    RETURN msg, u ORDER BY msg.time DESC LIMIT 20
                    """;
            Result records = session.query(cypher, Map.of("id", conversation.getId()));
            List<Message> messages = new ArrayList<>();
            for (var record : records) {
                Message message = (Message) record.get("msg");
                User sender = (User) record.get("u");
                message.setSender(sender);
                messages.add(message);
            }

            conversation.setMessages(messages.stream()
                    .sorted(Comparator.comparing(Message::getTime))
                    .collect(Collectors.toList()));
            String cypher2 = """
                    MATCH (c:Conversation)-[:HAS]->(u:User) WHERE id(c) = $id RETURN u
                    """;
            Result usersRecord = session.query(cypher2, Map.of("id", conversation.getId()));
            Set<User> participants = new LinkedHashSet<>();
            for (var userRecord : usersRecord) {
                User participant = (User) userRecord.get("u");
                participants.add(participant);
            }
            conversation.setParticipants(participants);
            if (sessionFactory != null) {
                sessionFactory.close();
            }
            return conversation;
        } catch (Exception e) {
            e.printStackTrace();
            if (sessionFactory != null) {
                sessionFactory.close();
            }
        }
        return null;
    }

    @Override
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

                if (sessionFactory != null) {
                    sessionFactory.close();
                }
                return fullConversation;
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (sessionFactory != null) {
                sessionFactory.close();
            }
            errorMessage = e.getMessage();
            return null;
        }
    }

}
