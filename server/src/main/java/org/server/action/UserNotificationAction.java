package org.server.action;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.server.dao.ConversationDao;
import org.shared.Message;
import org.shared.ServerResponse;
import org.shared.ServerResponseMessage;
import org.shared.ServerResponseStatus;
import org.shared.entity.Conversation;
import org.shared.entity.User;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class UserNotificationAction {

    public void sendMessageToUser(ConcurrentHashMap<String, Socket> userSockets,
                                  ObjectMapper objectMapper, ServerResponse serverResponse,
                                  Conversation conversation) throws IOException {
        ConversationDao conversationDao = new ConversationDao();
        List<String> emailAddresses = conversationDao.getConversationParticipants(conversation)
                .stream().map(User::getEmailAddress).toList();
        for (var userSocket : userSockets.entrySet()) {
            if (!CollectionUtils.isEmpty(emailAddresses)) {
                String emailAddress = userSocket.getKey();
                Socket socket = userSocket.getValue();
                if (emailAddresses.contains(emailAddress)) {
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    serverResponse.setServerResponseStatus(ServerResponseStatus.SUCCESS);
                    serverResponse.setServerResponseMessage(ServerResponseMessage.NEW_MESSAGE_NOTIFIED);
                    serverResponse.setPayload(objectMapper.writeValueAsString(conversation));
                    out.println(objectMapper.writeValueAsString(serverResponse));
                }
            }

        }
    }
}
