package org.server.calendar.requester;

import com.microsoft.graph.models.User;
import com.microsoft.graph.requests.GraphServiceClient;

public class UserRequester {

    public User getUser(GraphServiceClient graphServiceClient) {
        try {
            return graphServiceClient.me().buildRequest().get();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
