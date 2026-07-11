package org.server.dao;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.*;
import org.neo4j.harness.Neo4j;
import org.neo4j.harness.Neo4jBuilders;
import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;
import org.server.dao.ConversationDaoImpl;
import org.shared.entity.Conversation;
import org.shared.entity.Message;
import org.shared.entity.User;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

public class ConversationDaoImplTest {

    private static ConversationDaoImpl conversationDao;

    private static SessionFactory sessionFactory;

    private static Session session;

    private static Neo4j neo4j;

    @BeforeAll
    public static void setUp() {
        neo4j = Neo4jBuilders.newInProcessBuilder().build();

        sessionFactory = new SessionFactory(new org.neo4j.ogm.config.Configuration.Builder()
                .uri(neo4j.boltURI().toString())
                .build(),
                "org.shared.entity");

        session = sessionFactory.openSession();

        // Embedded session to not flood with real data
        conversationDao = new ConversationDaoImpl(sessionFactory);
    }

    @Test
    public void testCreateConversation() {
        User user1 = createDummyUser("test1.test@yahoo.fr", "Test 1");
        User user2 = createDummyUser("test2.test@yahoo.fr", "Test 2");

        Message message = fillDummyMessage();

        Conversation conversation = conversationDao.createConversation(Set.of(user1, user2), message, user1);

        Assertions.assertNotNull(conversation);
        Assertions.assertNotNull(conversation.getId());
    }

    @Test
    public void testGetConversation() {
        User user1 = createDummyUser("test1.test@yahoo.fr", "Test 1");
        User user2 = createDummyUser("test2.test@yahoo.fr", "Test 2");

        Message message = fillDummyMessage();

        // Firstly, save conversation in db
        Conversation conversationToSave = conversationDao.createConversation(Set.of(user1, user2), message, user1);

        // Then get it
        Conversation conversation = conversationDao.getConversation(conversationToSave);

        Assertions.assertNotNull(conversation);
        Assertions.assertNotNull(conversation.getId());
        Assertions.assertTrue(CollectionUtils.isNotEmpty(conversation.getMessages()));
        Assertions.assertNotNull(conversation.getMessages().getFirst());
        Assertions.assertEquals("Test message", conversation.getMessages().getFirst().getContent());
        Assertions.assertNotNull(conversation.getParticipants());
        Assertions.assertEquals(2, conversation.getParticipants().size());
    }

    @Test
    public void testSearchUserConversations() {
        User user1 = createDummyUser("test1.test@yahoo.fr", "Test 1");
        User user2 = createDummyUser("test2.test@yahoo.fr", "Test 2");
        User user3 = createDummyUser("test3.test@yahoo.fr", "Test 3");

        Message message = fillDummyMessage();

        //Save a first conversation in the db
        conversationDao.createConversation(Set.of(user1, user2), message, user1);
        //Same with a second one and different participants
        conversationDao.createConversation(Set.of(user1, user3), message, user1);

        List<Conversation> conversationList = conversationDao.searchUserConversations(user1);

        Assertions.assertTrue(CollectionUtils.isNotEmpty(conversationList));
        Assertions.assertEquals(2, conversationList.size());
        Assertions.assertTrue(CollectionUtils.isNotEmpty(conversationList.getFirst().getMessages()));
        Assertions.assertTrue(CollectionUtils.isNotEmpty(conversationList.getFirst().getParticipants()));
    }

    @Test
    public void testGetConversationParticipants() {
        User user1 = createDummyUser("test1.test@yahoo.fr", "Test 1");
        User user2 = createDummyUser("test2.test@yahoo.fr", "Test 2");

        Message message = fillDummyMessage();

        // Save a first conversation in the db
        Conversation conversation = conversationDao.createConversation(Set.of(user1, user2), message, user1);

        // Get participants of this conversation
        Set<User> participants = conversationDao.getConversationParticipants(conversation);

        Assertions.assertTrue(CollectionUtils.isNotEmpty(participants));
        Assertions.assertEquals(2, participants.size());
    }

    @Test
    public void testSearchExistingConversation() {
        User user1 = createDummyUser("test1.test@yahoo.fr", "Test 1");
        User user2 = createDummyUser("test2.test@yahoo.fr", "Test 2");

        Message message = fillDummyMessage();

        // Save a first conversation in the db
        conversationDao.createConversation(Set.of(user1, user2), message, user1);

        // Then check if exists
        Conversation conversation = conversationDao.searchConversationIfExists(user1, user2);

        Assertions.assertNotNull(conversation);
        Assertions.assertNotNull(conversation.getId());
        Assertions.assertTrue(CollectionUtils.isNotEmpty(conversation.getMessages()));
    }

    @Test
    public void testSearchNonExistingConversation() {
        User user1 = createDummyUser("test1.test@yahoo.fr", "Test 1");
        User user2 = createDummyUser("test2.test@yahoo.fr", "Test 2");

        Conversation conversation = conversationDao.searchConversationIfExists(user1, user2);

        Assertions.assertNull(conversation);
    }

    private User createDummyUser(String emailAddress, String name) {
        User user = new User();
        user.setEmailAddress(emailAddress);
        user.setDisplayName(name);
        user.setUserName(name);

        session.save(user);

        return user;
    }

    private Message fillDummyMessage() {
        Message message = new Message();
        message.setId(1L);
        String str = "2014-04-08 12:30";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        LocalDateTime dateTime = LocalDateTime.parse(str, formatter);
        message.setTime(dateTime);
        message.setContent("Test message");

        return message;
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