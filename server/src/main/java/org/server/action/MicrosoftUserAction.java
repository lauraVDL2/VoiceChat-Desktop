package org.server.action;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.graph.models.User;
import com.microsoft.graph.requests.GraphServiceClient;
import org.shared.*;
import org.shared.pojo.MicrosoftAccount;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class MicrosoftUserAction {

    public void getMicrosoftUser(ObjectMapper objectMapper, Message messageObj,
                                 ServerResponse serverResponse, Socket socket, User microsoftUser) throws IOException {
        serverResponse.setCorrelationId("msa-" + messageObj.getCorrelationId());
        serverResponse.setServerResponseMessage(ServerResponseMessage.MICROSOFT_AUTHENTICATED);
        serverResponse.setServerResponseStatus(ServerResponseStatus.SUCCESS);
        MicrosoftAccount account = new MicrosoftAccount();
        account.setDisplayName(microsoftUser.displayName);
        account.setEmailAddress(microsoftUser.mail);
        serverResponse.setBinaryPayload(objectMapper.writeValueAsBytes(account));
        byte[] bytes = objectMapper.writeValueAsBytes(serverResponse);
        DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
        outputStream.writeUTF("JSON_RESPONSE");
        outputStream.writeInt(bytes.length);
        outputStream.write(bytes);
        outputStream.flush();
    }
}
