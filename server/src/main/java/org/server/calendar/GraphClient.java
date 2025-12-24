package org.server.calendar;

import com.azure.identity.*;
import com.microsoft.graph.authentication.TokenCredentialAuthProvider;
import com.microsoft.graph.models.Calendar;
import com.microsoft.graph.requests.GraphServiceClient;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;

public class GraphClient {
    private GraphServiceClient userclient;

    //private DeviceCodeCredential deviceCodeCredential;

    private String clientId;

    private String tenantId;

    private String[] graphUserScopes;

    private Properties properties = new Properties();

    public GraphClient() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                System.out.println("Sorry, unable to find application.properties");
                return;
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
                            .build();

            TokenCredentialAuthProvider authProvider = new TokenCredentialAuthProvider(Arrays.stream(graphUserScopes).toList(), deviceCodeCredential);

            userclient = GraphServiceClient.builder()
                    .authenticationProvider(authProvider)
                    .buildClient();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public Calendar getUserCalendar() {
        try {
            var user = userclient.me().buildRequest().get();

            System.out.println("user = " + user.displayName);
            Calendar calendar = userclient
                    .me()
                    .calendar()
                    .buildRequest()
                    .get();
            return calendar;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
