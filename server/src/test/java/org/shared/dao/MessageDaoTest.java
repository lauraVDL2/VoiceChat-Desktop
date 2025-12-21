package org.shared.dao;

import org.junit.jupiter.api.*;
import org.neo4j.harness.Neo4j;
import org.neo4j.harness.Neo4jBuilders;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.server.dao.ConversationDao;
import org.server.dao.MessageDao;
import org.shared.entity.Conversation;
import org.shared.entity.Message;
import org.shared.entity.User;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

public class MessageDaoTest {

    private static MessageDao messageDao;

    private static SessionFactory sessionFactory;

    private static Session session;

    private static Neo4j neo4j;

    private static ConversationDao conversationDao;

    @BeforeAll
    public static void setUp() {
        neo4j = Neo4jBuilders.newInProcessBuilder().build();

        sessionFactory = new SessionFactory(new org.neo4j.ogm.config.Configuration.Builder()
                .uri(neo4j.boltURI().toString())
                .build(),
                "org.shared.entity");

        session = sessionFactory.openSession();

        // Embedded session to not flood with real data
        messageDao = new MessageDao(sessionFactory);
        conversationDao = new ConversationDao(sessionFactory);
    }

    @Test
    public void testSendMessage() {
        User user1 = createDummyUser("test1.test@yahoo.fr", "Test 1");
        User user2 = createDummyUser("test2.test@yahoo.fr", "Test 2");

        Message message = new Message();
        message.setId(1L);
        String str = "2014-04-08 12:30";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        LocalDateTime dateTime = LocalDateTime.parse(str, formatter);
        message.setTime(dateTime);
        message.setContent("Test message");

        // First, create a conversation to send a message
        Conversation conversation = conversationDao.createConversation(Set.of(user1, user2), message, user1);

        // Send new message
        Message message1 = new Message();
        message1.setSender(user2);
        message1.setContent("Test message 2");
        message1.setTime(dateTime);
        conversation.getMessages().add(message1);

        Message newMessage = messageDao.sendMessage(conversation);

        Assertions.assertNotNull(newMessage);
        Assertions.assertNotNull(newMessage.getId());
        Assertions.assertEquals("Test message 2", newMessage.getContent());
    }

    @Test
    public void testSearchMessageInConversation() {
        User user1 = createDummyUser("test1.test@yahoo.fr", "Test 1");
        User user2 = createDummyUser("test2.test@yahoo.fr", "Test 2");

        Message message = new Message();
        message.setId(1L);
        String str = "2014-04-08 12:30";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        LocalDateTime dateTime = LocalDateTime.parse(str, formatter);
        message.setTime(dateTime);
        message.setContent("Test message");

        // First, create a conversation to send a message
        Conversation conversation = conversationDao.createConversation(Set.of(user1, user2), message, user1);

        // Send new message
        Message message1 = new Message();
        message1.setSender(user2);
        message1.setContent("Test message 2");
        message1.setTime(dateTime);
        conversation.getMessages().add(message1);

        messageDao.sendMessage(conversation);

        Message messageToSearch = message;
        Conversation conversationToSearch = conversation;
        conversationToSearch.setMessages(List.of(messageToSearch));

        List<Message> messages = messageDao.searchMessageInConversation(conversationToSearch);

        Assertions.assertNotNull(messages);
        Assertions.assertEquals(2, messages.size());
    }

    private User createDummyUser(String emailAddress, String name) {
        User user = new User();
        user.setEmailAddress(emailAddress);
        user.setDisplayName(name);
        user.setUserName(name);

        session.save(user);

        return user;
    }

    @AfterAll
    public static void tearDown() {
        if (sessionFactory != null) {
            sessionFactory.close();
        }
        neo4j.close();
    }

    @AfterEach
    public void shutDown() {
        session.purgeDatabase();
        session.clear();
    }

}
