package org.server.dao;

import org.shared.entity.Conversation;
import org.shared.entity.Message;

import java.util.List;

public interface MessageDao {
    Message sendMessage(Conversation conversation);

    List<Message> searchMessageInConversation(Conversation conversation);
}

