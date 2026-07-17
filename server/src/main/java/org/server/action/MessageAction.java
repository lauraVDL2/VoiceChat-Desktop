package org.server.action;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.neo4j.ogm.session.SessionFactory;
import org.server.config.Neo4jConfig;
import org.server.dao.MessageDao;
import org.server.dao.MessageDaoImpl;
import org.shared.ServerResponse;
import org.shared.ServerResponseMessage;
import org.shared.ServerResponseStatus;
import org.shared.entity.Conversation;
import org.shared.entity.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.List;

public class MessageAction extends AbstractAction {
    private static final Logger logger = LoggerFactory.getLogger(MessageAction.class);

    private final SessionFactory sessionFactory = Neo4jConfig.getSessionFactory();

    private final MessageDao messageDao;

    public MessageAction(MessageDao messageDao) {
        this.messageDao = messageDao;
    }

    public Conversation sendMessage(ObjectMapper objectMapper, org.shared.Message messageObj,
                                    ServerResponse serverResponse, Socket socket) throws IOException {
        Conversation conversation = objectMapper.readValue(messageObj.getPayload(), Conversation.class);
        Message message = messageDao.sendMessage(conversation);
        byte[] bytes = null;
        if (message != null) {
            logger.info("Message sent !");
            bytes = buildSuccessResponseWithPayload(serverResponse, ServerResponseMessage.MESSAGE_SENT,
                    messageObj.getCorrelationId(), message, objectMapper);
            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
            outputStream.writeUTF("JSON_RESPONSE");
            outputStream.writeInt(bytes.length);
            outputStream.write(bytes);
            outputStream.flush();
            conversation.setMessages(List.of(message));
            return conversation;
        }
        else {
            logger.error("Failed to send message !");
            bytes = buildFailureResponse(serverResponse, ServerResponseMessage.MESSAGE_SENT,
                    messageObj.getCorrelationId(), "Failed to send message", objectMapper);
            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
            outputStream.writeUTF("JSON_RESPONSE");
            outputStream.writeInt(bytes.length);
            outputStream.write(bytes);
            outputStream.flush();
            return null;
        }
    }

    public void searchMessageInConversation(ObjectMapper objectMapper, org.shared.Message messageObj,
                                            ServerResponse serverResponse, Socket socket) throws IOException {
        Conversation conversation = objectMapper.readValue(messageObj.getPayload(), Conversation.class);
        List<Message> messages = messageDao.searchMessageInConversation(conversation);
        byte[] bytes = null;
        if (CollectionUtils.isNotEmpty(messages)) {
            bytes = buildSuccessResponseWithPayload(serverResponse, ServerResponseMessage.MESSAGE_CONVERSATION_SEARCHED,
                    messageObj.getCorrelationId(), messages, objectMapper);
            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
            outputStream.writeUTF("JSON_RESPONSE");
            outputStream.writeInt(bytes.length);
            outputStream.write(bytes);
            outputStream.flush();
        }
        else {
            bytes = buildFailureResponse(serverResponse, ServerResponseMessage.MESSAGE_CONVERSATION_SEARCHED, messageObj.getCorrelationId(),
                    "", objectMapper);
            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
            outputStream.writeUTF("JSON_RESPONSE");
            outputStream.writeInt(bytes.length);
            outputStream.write(bytes);
            outputStream.flush();
        }
    }
}
