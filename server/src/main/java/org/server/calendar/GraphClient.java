package org.server.calendar;

import com.azure.identity.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.graph.authentication.TokenCredentialAuthProvider;
import com.microsoft.graph.models.Calendar;
import com.microsoft.graph.requests.GraphServiceClient;
import org.shared.*;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.Properties;

public class GraphClient {
    private static GraphServiceClient userclient;

    private static String clientId;

    private static String tenantId;

    private static String[] graphUserScopes;

    private static Properties properties = new Properties();

    public static GraphServiceClient getClient(ObjectMapper objectMapper, Socket socket,
                                               Message messageObj, ServerResponse serverResponse) {
        try (InputStream input = GraphClient.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                System.out.println("Sorry, unable to find application.properties");
                return null;
            }
            properties.load(input);
            clientId = properties.getProperty("app.clientId");
            tenantId = properties.getProperty("app.tenantId");
            String scopesStr = properties.getProperty("app.graphUserScopes");
            graphUserScopes = scopesStr.split(",");

           var deviceCodeCredential =
                    new DeviceCodeCredentialBuilder()
                            .clientId(clientId)
                            .tenantId("common")
                            .challengeConsumer(challenge -> {
                                System.out.println(challenge.getMessage());
                                try {
                                    buildServerResponse(serverResponse, socket, challenge, messageObj.getCorrelationId());
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            })
                            .build();

            TokenCredentialAuthProvider authProvider = new TokenCredentialAuthProvider(Arrays.stream(graphUserScopes).toList(), deviceCodeCredential);

            return userclient = GraphServiceClient.builder()
                    .authenticationProvider(authProvider)
                    .buildClient();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static void buildServerResponse(ServerResponse serverResponse, Socket socket, DeviceCodeInfo deviceCodeInfo, String correlationId) throws IOException {
        ObjectMapper objectMapper = JsonMapper.getJsonMapper();
        Authenticate authenticate = new Authenticate(deviceCodeInfo.getVerificationUrl(), deviceCodeInfo.getUserCode());
        serverResponse.setCorrelationId(correlationId);
        serverResponse.setServerResponseMessage(ServerResponseMessage.MICROSOFT_AUTHENTICATED);
        serverResponse.setServerResponseStatus(ServerResponseStatus.SUCCESS);
        serverResponse.setBinaryPayload(objectMapper.writeValueAsBytes(authenticate));
        byte[] bytes = objectMapper.writeValueAsBytes(serverResponse);
        DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
        outputStream.writeUTF("JSON_RESPONSE");
        outputStream.writeInt(bytes.length);
        outputStream.write(bytes);
        outputStream.flush();
    }

}
