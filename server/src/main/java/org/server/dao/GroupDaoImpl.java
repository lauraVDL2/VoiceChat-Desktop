package org.server.dao;

import org.apache.commons.collections4.CollectionUtils;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.transaction.Transaction;
import org.shared.entity.Conversation;
import org.shared.entity.Group;
import org.shared.entity.Message;
import org.shared.entity.User;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class GroupDaoImpl extends ConversationDaoImpl implements GroupDao {

    public GroupDaoImpl(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    @Override
    public Conversation createConversation(Set<User> users, Message message, User sender) {
        try {
            Conversation fullConversation = null;
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
                String cypherGroup = """
                        CREATE (conv:Conversation:Group {type: 'group'})
                        WITH conv
                        UNWIND $usersEmailAddresses AS email
                        MATCH (u:User {emailAddress: email})
                        MERGE (conv)-[:HAS]->(u)
                        WITH conv
                        MATCH (m:Message)
                        WHERE id(m) = $messageId
                        MERGE (conv)-[:CONTAINS]->(m)
                        RETURN conv
                        """;
                fullConversation = session.queryForObject(Group.class, cypherGroup, Map.of("usersEmailAddresses", userEmailAddresses, "messageId",
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
