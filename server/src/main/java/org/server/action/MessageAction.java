package org.server.action;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.server.dao.MessageDao;
import org.shared.ServerResponse;
import org.shared.ServerResponseMessage;
import org.shared.ServerResponseStatus;
import org.shared.entity.Conversation;
import org.shared.entity.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class MessageAction {
    private static final Logger logger = LoggerFactory.getLogger(MessageAction.class);

    public Conversation sendMessage(ObjectMapper objectMapper, org.shared.Message messageObj,
                               ServerResponse serverResponse, PrintWriter out) throws JsonProcessingException {
        Conversation conversation = objectMapper.readValue(messageObj.getPayload(), Conversation.class);
        MessageDao messageDao = new MessageDao();
        Message message = messageDao.sendMessage(conversation);
        if (message != null) {
            logger.info("Message sent !");
            serverResponse.setServerResponseStatus(ServerResponseStatus.SUCCESS);
            serverResponse.setServerResponseMessage(ServerResponseMessage.MESSAGE_SENT);
            serverResponse.setPayload(objectMapper.writeValueAsString(message));
            out.println(objectMapper.writeValueAsString(serverResponse));
            conversation.setMessages(List.of(message));
            return conversation;
        }
        else {
            logger.error("Failed to send message !");
            serverResponse.setServerResponseStatus(ServerResponseStatus.FAILURE);
            serverResponse.setServerResponseMessage(ServerResponseMessage.MESSAGE_SENT);
            out.println(objectMapper.writeValueAsString(serverResponse));
            return null;
        }
    }
}
