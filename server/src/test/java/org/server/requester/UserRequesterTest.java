package org.server.requester;

import com.microsoft.graph.models.User;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.UserRequest;
import com.microsoft.graph.requests.UserRequestBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.server.microsoft_graph.requester.UserRequester;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UserRequesterTest {

    GraphServiceClient mockGraphClient;
    UserRequester userRequester;

    @BeforeEach
    public void setUp() {
        mockGraphClient = mock(GraphServiceClient.class);
        userRequester = new UserRequester();
    }

    @Test
    public void testGetUser_Success() {
        // Prepare a real model user to be returned by the mocked request
        User mockUser = new User();
        mockUser.id = "user-1";
        mockUser.displayName = "Test User";
        mockUser.mail = "test@example.com";

        // Mock the builder and request objects from the SDK
        UserRequestBuilder mockUserRequestBuilder = mock(UserRequestBuilder.class);
        UserRequest mockUserRequest = mock(UserRequest.class);

        when(mockGraphClient.me()).thenReturn(mockUserRequestBuilder);
        when(mockUserRequestBuilder.buildRequest()).thenReturn(mockUserRequest);
        when(mockUserRequest.get()).thenReturn(mockUser);

        // Call the method under test
        User result = userRequester.getUser(mockGraphClient);

        // Verify the result
        assertNotNull(result, "Expected non-null User from getUser()");
        assertEquals("user-1", result.id);
        assertEquals("Test User", result.displayName);
        assertEquals("test@example.com", result.mail);
    }
}
