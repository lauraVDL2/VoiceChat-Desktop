package org.server.action;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.neo4j.ogm.session.SessionFactory;
import org.server.config.Neo4jConfig;
import org.server.dao.MessageDao;
import org.shared.ServerResponse;
import org.shared.ServerResponseMessage;
import org.shared.ServerResponseStatus;
import org.shared.entity.Conversation;
import org.shared.entity.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;

public class MessageAction {
    private static final Logger logger = LoggerFactory.getLogger(MessageAction.class);

    private final SessionFactory sessionFactory = Neo4jConfig.getSessionFactory();

    public Conversation sendMessage(ObjectMapper objectMapper, org.shared.Message messageObj,
                                    ServerResponse serverResponse, Socket socket) throws IOException {
        Conversation conversation = objectMapper.readValue(messageObj.getPayload(), Conversation.class);
        MessageDao messageDao = new MessageDao(sessionFactory);
        Message message = messageDao.sendMessage(conversation);
        byte[] bytes = null;
        if (message != null) {
            logger.info("Message sent !");
            serverResponse.setCorrelationId(messageObj.getCorrelationId());
            serverResponse.setServerResponseStatus(ServerResponseStatus.SUCCESS);
            serverResponse.setServerResponseMessage(ServerResponseMessage.MESSAGE_SENT);
            serverResponse.setBinaryPayload(objectMapper.writeValueAsBytes(message));
            bytes = objectMapper.writeValueAsBytes(serverResponse);
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
            serverResponse.setCorrelationId(messageObj.getCorrelationId());
            serverResponse.setServerResponseStatus(ServerResponseStatus.FAILURE);
            serverResponse.setServerResponseMessage(ServerResponseMessage.MESSAGE_SENT);
            bytes = objectMapper.writeValueAsBytes(serverResponse);
            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
            outputStream.writeUTF("JSON_RESPONSE");
            outputStream.writeInt(bytes.length);
            outputStream.write(bytes);
            outputStream.flush();
            return null;
        }
    }
}
