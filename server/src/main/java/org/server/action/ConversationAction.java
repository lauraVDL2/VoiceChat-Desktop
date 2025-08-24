package org.server.action;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.neo4j.bolt.connection.GqlStatusError;
import org.server.dao.ConversationDao;
import org.shared.Message;
import org.shared.ServerResponse;
import org.shared.ServerResponseMessage;
import org.shared.ServerResponseStatus;
import org.shared.entity.Conversation;
import org.shared.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.util.List;
import java.util.Set;

public class ConversationAction {

    private static final Logger logger = LoggerFactory.getLogger(ConversationAction.class);

    public void searchUserConversations(ObjectMapper objectMapper, Message messageObj,
                                        ServerResponse serverResponse, PrintWriter out) throws JsonProcessingException {
        User user = objectMapper.readValue(messageObj.getPayload(), User.class);
        if (user != null) {
            ConversationDao conversationDao = new ConversationDao();
            List<Conversation> conversations = conversationDao.searchUserConversations(user);
            System.out.println("SIZE = " +conversations.size());
            if (!CollectionUtils.isEmpty(conversations)) {
                logger.info("Conversations found !");
                serverResponse.setServerResponseStatus(ServerResponseStatus.SUCCESS);
                serverResponse.setServerResponseMessage(ServerResponseMessage.CONVERSATION_DISPLAYED);
                serverResponse.setPayload(objectMapper.writeValueAsString(conversations));
                out.println(objectMapper.writeValueAsString(serverResponse));
            }
            else {
                logger.info("No conversation found !");
                serverResponse.setServerResponseStatus(ServerResponseStatus.INFO);
                serverResponse.setServerResponseMessage(ServerResponseMessage.CONVERSATION_DISPLAYED);
                serverResponse.setMessage("No conversations yet, start a new one !");
                out.println(objectMapper.writeValueAsString(serverResponse));
            }
        }
    }

    public void createConversation(ObjectMapper objectMapper, Message messageObj,
                                   ServerResponse serverResponse, PrintWriter out) throws JsonProcessingException {
        Conversation conversation = objectMapper.readValue(messageObj.getPayload(), Conversation.class);
        if (conversation != null) {
            Set<User> participants = conversation.getParticipants();
            org.shared.entity.Message message = conversation.getMessages().get(0);
            ConversationDao conversationDao = new ConversationDao();
            Conversation newConversation = conversationDao.createConversation(participants, message, message.getSender());
            if (newConversation != null) {
                logger.info("Conversation created !");
                serverResponse.setServerResponseStatus(ServerResponseStatus.SUCCESS);
                serverResponse.setServerResponseMessage(ServerResponseMessage.CONVERSATION_CREATED);
                serverResponse.setPayload(objectMapper.writeValueAsString(newConversation));
                out.println(objectMapper.writeValueAsString(serverResponse));
            }
            else {
                logger.info("Conversation could not be created !");
                serverResponse.setServerResponseStatus(ServerResponseStatus.FAILURE);
                serverResponse.setServerResponseMessage(ServerResponseMessage.CONVERSATION_CREATED);
                out.println(objectMapper.writeValueAsString(serverResponse));
            }
        }
    }

    public void conversationSearchIfExists(ObjectMapper objectMapper, Message messageObj,
                           ServerResponse serverResponse, PrintWriter out) throws JsonProcessingException {
        List<User> users = objectMapper.readValue(
                messageObj.getPayload(),
                new TypeReference<List<User>>() {}
        );
        if (!CollectionUtils.isEmpty(users)) {
            if (users.size() > 1) {
                User currentUser = users.get(0);
                User targetUser = users.get(1);
                ConversationDao conversationDao = new ConversationDao();
                Conversation conversation = conversationDao.searchConversationIfExists(currentUser, targetUser);
                if (conversation != null) {
                    logger.info("Conversation found !");
                    serverResponse.setServerResponseStatus(ServerResponseStatus.SUCCESS);
                    serverResponse.setServerResponseMessage(ServerResponseMessage.CONVERSATION_SEARCHED);
                    serverResponse.setPayload(objectMapper.writeValueAsString(conversation));
                    out.println(objectMapper.writeValueAsString(serverResponse));
                }
                else {
                    logger.info("No conversation found !");
                    serverResponse.setServerResponseStatus(ServerResponseStatus.INFO);
                    serverResponse.setServerResponseMessage(ServerResponseMessage.CONVERSATION_SEARCHED);
                    out.println(objectMapper.writeValueAsString(serverResponse));
                }
            }
        }
    }
}
