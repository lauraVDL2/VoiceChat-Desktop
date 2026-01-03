package org.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.graph.requests.GraphServiceClient;
import org.server.action.*;
import org.server.microsoft_graph.pojo.MicrosoftUser;
import org.server.microsoft_graph.requester.CalendarRequester;
import org.server.microsoft_graph.GraphClient;
import org.server.microsoft_graph.requester.UserRequester;
import org.shared.entity.Conversation;
import org.shared.entity.User;
import org.shared.*;
import org.shared.pojo.Voice;
import org.shared.pojo.VoiceChatCalendar;
import org.shared.pojo.VoiceChatEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    public final static int SERVER_PORT = 8080;
    private final static ExecutorService executor = Executors.newCachedThreadPool();
    private final static Logger logger = LoggerFactory.getLogger(Server.class);
    private final static ConcurrentHashMap<String, UserSessionStatus> onlineUsers = new ConcurrentHashMap<>();
    public final static ConcurrentHashMap<String, Socket> userSockets = new ConcurrentHashMap<>();
    private final static ConcurrentHashMap<String, MicrosoftUser> microsoftUsers = new ConcurrentHashMap<>();
    public final static ConcurrentHashMap<String, Set<String>> meetingParticipants = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(SERVER_PORT)) {
            logger.info("Server started, waiting for clients...");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                var inputStream = clientSocket.getInputStream();
                handleClientAsync(clientSocket, inputStream);
            }
        }
    }

    public static void handleClientAsync(Socket socket, InputStream inputStream) {
        CompletableFuture.runAsync(() -> {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
                String message;
                while ((message = in.readLine()) != null) {
                    //logger.info(message);
                    ObjectMapper objectMapper = JsonMapper.getJsonMapper();
                    Message messageObj = objectMapper.readValue(message, Message.class);
                    ServerResponse serverResponse = new ServerResponse();
                    DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
                    UserAction userAction = null;
                    ConversationAction conversationAction = null;
                    MessageAction messageAction = null;
                    UserNotificationAction userNotificationAction = null;
                    UserRequester userRequester = null;
                    MicrosoftUserAction microsoftUserAction = null;
                    CalendarRequester calendarRequester = null;
                    User user = null;
                    GraphServiceClient graphServiceClient = null;
                    MicrosoftUser microsoftUser = null;
                    CalendarAction calendarAction = new CalendarAction();
                    VoiceChatEvent voiceChatEvent = null;
                    MeetingAction meetingAction = null;
                    switch (messageObj.getMessageType()) {
                        case USER_CREATE:
                            userAction = new UserAction();
                            User userCreated = userAction.userCreate(objectMapper, messageObj, serverResponse, socket);
                            if (userCreated != null) {
                                userAction.searchUserAvatar(messageObj, objectMapper, userCreated, dataOutputStream, serverResponse);
                            }
                            break;
                        case USER_LOG_IN:
                            userAction = new UserAction();
                            User userLogged = userAction.userLogIn(objectMapper, messageObj, serverResponse, socket);
                            if (userLogged != null) {
                                userAction.searchUserAvatar(messageObj, objectMapper, userLogged, dataOutputStream, serverResponse);
                            }
                            onlineUsers.computeIfAbsent(userLogged.getEmailAddress(), status -> UserSessionStatus.ONLINE);
                            userSockets.computeIfAbsent(userLogged.getEmailAddress(), mySocket -> socket);
                            break;
                        case MICROSOFT_AUTHENTICATE:
                            var serviceClient = GraphClient.getClient(objectMapper, socket, messageObj, serverResponse);
                            user = objectMapper.readValue(messageObj.getPayload(), User.class);
                            userRequester = new UserRequester();
                            var msUser = userRequester.getUser(serviceClient);
                            microsoftUsers.computeIfAbsent(user.getEmailAddress(), client ->
                                    new MicrosoftUser(serviceClient, msUser));
                            microsoftUserAction = new MicrosoftUserAction();
                            microsoftUserAction.getMicrosoftUser(objectMapper, messageObj, serverResponse, socket, msUser);
                            break;
                        case EVENTS_GET:
                            VoiceChatCalendar voiceChatCalendar = objectMapper.readValue(messageObj.getPayload(), VoiceChatCalendar.class);
                            microsoftUser = microsoftUsers.get(voiceChatCalendar.getOwnerEmailAddress());
                            graphServiceClient = microsoftUser.getGraphServiceClient();
                            calendarAction.getEvents(objectMapper, messageObj, serverResponse, socket, graphServiceClient, voiceChatCalendar);
                            break;
                        case EVENT_CREATE:
                            voiceChatEvent = objectMapper.readValue(messageObj.getPayload(), VoiceChatEvent.class);
                            microsoftUser = microsoftUsers.get(voiceChatEvent.getOrganizer());
                            graphServiceClient = microsoftUser.getGraphServiceClient();
                            calendarAction.createEvent(objectMapper, messageObj, serverResponse, socket, graphServiceClient, voiceChatEvent);
                            break;
                        case EVENT_DELETE:
                            voiceChatEvent = objectMapper.readValue(messageObj.getPayload(), VoiceChatEvent.class);
                            microsoftUser = microsoftUsers.get(voiceChatEvent.getOrganizer());
                            graphServiceClient = microsoftUser.getGraphServiceClient();
                            calendarAction.deleteEvent(objectMapper, messageObj, serverResponse, socket, graphServiceClient, voiceChatEvent);
                            break;
                        case USER_EXIT:
                            User userExit = objectMapper.readValue(messageObj.getPayload(), User.class);
                            String emailAddress = userExit.getEmailAddress();
                            onlineUsers.remove(emailAddress);
                            microsoftUsers.remove(emailAddress);
                            for (var meeting : meetingParticipants.entrySet()) {
                                var usersInMeeting = meeting.getValue();
                                if (usersInMeeting.contains(emailAddress)) {
                                    meeting.getValue().remove(emailAddress);
                                    System.out.println("USER REMOVED");
                                }
                            }
                            userSockets.get(emailAddress).close();
                            userSockets.remove(emailAddress);
                            break;
                        case USER_SEARCH:
                            userAction = new UserAction();
                            userAction.userSearch(objectMapper, messageObj, serverResponse, socket);
                            break;
                        case ONLINE_USERS_FETCH:
                            userAction = new UserAction();
                            userAction.getOnlineUsers(messageObj, objectMapper, serverResponse, socket, onlineUsers);
                            break;
                        case CONVERSATION_SEARCH:
                            conversationAction = new ConversationAction();
                            conversationAction.conversationSearchIfExists(objectMapper, messageObj, serverResponse, socket);
                            break;
                        case CONVERSATION_CREATE:
                            conversationAction = new ConversationAction();
                            conversationAction.createConversation(objectMapper, messageObj, serverResponse, socket);
                            break;
                        case CONVERSATION_DISPLAY:
                            conversationAction = new ConversationAction();
                            conversationAction.searchUserConversations(objectMapper, messageObj, serverResponse, out, socket);
                            break;
                        case CONVERSATION_SCROLL:
                            conversationAction = new ConversationAction();
                            conversationAction.scrollConversation(objectMapper, messageObj, serverResponse, socket);
                            break;
                        case READ_TARGET_AVATAR:
                            userAction = new UserAction();
                            userAction.searchTargetUser(objectMapper, messageObj, dataOutputStream, serverResponse);
                            break;
                        case CONVERSATION_GET:
                            conversationAction = new ConversationAction();
                            conversationAction.getConversation(objectMapper, messageObj, serverResponse, socket);
                            break;
                        case MESSAGE_SEND:
                            sendMessageToUser(objectMapper, messageObj, serverResponse, socket);
                            break;
                        case MESSAGE_CONVERSATION_SEARCH:
                            messageAction = new MessageAction();
                            messageAction.searchMessageInConversation(objectMapper, messageObj, serverResponse, socket);
                            break;
                        case MESSAGE_CONVERSATION_GO:
                            conversationAction = new ConversationAction();
                            conversationAction.goToMessage(objectMapper, messageObj, serverResponse, socket);
                            break;
                        case MEETING_CONNECT:
                            meetingAction = new MeetingAction();
                            VoiceChatEvent event = meetingAction.connect(objectMapper, messageObj, serverResponse, socket);
                            meetingParticipants.computeIfPresent(event.getId(), (key, participants) -> {
                                participants.add(event.getUserEmailAddress());
                                return participants;
                            });
                            // If you are starting the meeting
                            meetingParticipants.computeIfAbsent(event.getId(), (e) -> {
                                Set<String> list = new HashSet<>();
                                list.add(event.getUserEmailAddress());
                                return list;
                            });

                            break;
                        case IS_TALKING:
                            meetingAction = new MeetingAction();
                            meetingAction.captureAudio(objectMapper, messageObj, serverResponse, socket);
                            break;
                        case HAS_CAMERA:
                            meetingAction = new MeetingAction();
                            meetingAction.captureVideo(objectMapper, messageObj, serverResponse);
                            break;
                        case IS_SCREEN_SHARING:
                            meetingAction = new MeetingAction();
                            meetingAction.captureScreen(objectMapper, messageObj, serverResponse);
                            break;
                    }
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }, executor);
    }

    public static void sendMessageToUser(ObjectMapper objectMapper, Message messageObj,
                                         ServerResponse serverResponse, Socket socket) {
        try {
            MessageAction messageAction = new MessageAction();
            Conversation messageSentConversation = messageAction.sendMessage(objectMapper, messageObj, serverResponse, socket);
            UserNotificationAction userNotificationAction = new UserNotificationAction();
            userNotificationAction.sendMessageToUser(messageObj, userSockets, objectMapper, serverResponse,
                    messageSentConversation);
        } catch (Exception e) {
            logger.error("Error sending message: ", e);
        }
    }

}
