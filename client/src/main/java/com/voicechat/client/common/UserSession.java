package com.voicechat.client.common;

import org.shared.entity.User;
import org.shared.pojo.MicrosoftAccount;

public enum UserSession {
    INSTANCE;

    private User user;
    private MicrosoftAccount microsoftAccount;

    public void setMicrosoftAccount(MicrosoftAccount microsoftAccount) {
        this.microsoftAccount = microsoftAccount;
    }

    public MicrosoftAccount getMicrosoftAccount() {
        return microsoftAccount;
    }

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
        this.microsoftAccount = null;
    }
}
