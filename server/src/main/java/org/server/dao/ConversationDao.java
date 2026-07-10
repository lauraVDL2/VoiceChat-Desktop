package org.server.dao;

import org.shared.entity.Conversation;
import org.shared.entity.Message;
import org.shared.entity.User;

import java.util.List;
import java.util.Set;

public interface ConversationDao {

    Conversation searchConversationIfExists(User currentUser, User targetUser);
    Set<User> getConversationParticipants(Conversation conversation);
    List<Conversation> searchUserConversations(User user);
    int getOffset(Conversation conversation);
    Conversation scrollConversationMessages(Conversation conversation, int offset);
    Conversation getConversation(Conversation conversation);
    Conversation createConversation(Set<User> users, Message message, User sender);
}
