package com.voicechat.client.login;

import org.shared.entity.User;

public enum UserSession {
    INSTANCE;

    private User user;

    // Set the current user
    public void setUser(User user) {
        this.user = user;
    }

    // Retrieve the current user
    public User getUser() {
        return user;
    }

    // Clear the session
    public void clear() {
        this.user = null;
    }
}
