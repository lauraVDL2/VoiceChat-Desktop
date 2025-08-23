package org.server.action;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections4.CollectionUtils;
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

public class ConversationAction {

    private static final Logger logger = LoggerFactory.getLogger(ConversationAction.class);

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
