package org.server.dao;

import org.shared.entity.Settings;
import org.shared.entity.User;

public interface SettingsDao {
    Settings setThemeMode(User user);
    Settings getOrCreateSettings(User user);
}
