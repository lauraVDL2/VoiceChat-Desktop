package org.server.microsoft_graph.pojo;

import com.microsoft.graph.models.User;
import com.microsoft.graph.requests.GraphServiceClient;

public class MicrosoftUser {

    private GraphServiceClient graphServiceClient;
    private User user;

    public MicrosoftUser() {

    }

    public MicrosoftUser(GraphServiceClient graphServiceClient, User user) {
        this.graphServiceClient = graphServiceClient;
        this.user = user;
    }

    public GraphServiceClient getGraphServiceClient() {
        return graphServiceClient;
    }

    public void setGraphServiceClient(GraphServiceClient graphServiceClient) {
        this.graphServiceClient = graphServiceClient;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
