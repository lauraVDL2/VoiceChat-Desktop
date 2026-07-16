package org.server.action;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.server.dao.SettingsDao;
import org.server.dao.UserDaoImpl;
import org.shared.Message;
import org.shared.ServerResponse;
import org.shared.ServerResponseMessage;
import org.shared.ServerResponseStatus;
import org.shared.entity.Settings;
import org.shared.entity.User;

import java.io.DataOutputStream;
import java.net.Socket;

public class SettingsAction {
    private SettingsDao settingsDao;

    public SettingsAction(SettingsDao settingsDao) {
        this.settingsDao = settingsDao;
    }

    public void setThemeMode(ObjectMapper objectMapper, Message messageObj,
                      ServerResponse serverResponse, Socket socket) throws Exception {
        User user = objectMapper.readValue(messageObj.getPayload(), User.class);
        byte[] bytes = null;
        if (user.getSettings() != null) {
            Settings settings = settingsDao.setThemeMode(user);
            if (settings != null) {
                serverResponse.setCorrelationId(messageObj.getCorrelationId());
                serverResponse.setBinaryPayload(objectMapper.writeValueAsBytes(settings));
                serverResponse.setServerResponseStatus(ServerResponseStatus.SUCCESS);
                serverResponse.setServerResponseMessage(ServerResponseMessage.THEME_MODE_SET);
                bytes = objectMapper.writeValueAsBytes(serverResponse);
                DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
                outputStream.writeUTF("JSON_RESPONSE");
                outputStream.writeInt(bytes.length);
                outputStream.write(bytes);
                outputStream.flush();
            }
            else {
                serverResponse.setCorrelationId(messageObj.getCorrelationId());
                serverResponse.setServerResponseStatus(ServerResponseStatus.FAILURE);
                serverResponse.setServerResponseMessage(ServerResponseMessage.THEME_MODE_SET);
                serverResponse.setMessage(UserDaoImpl.errorMessage);
                bytes = objectMapper.writeValueAsBytes(serverResponse);
                DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
                outputStream.writeUTF("JSON_RESPONSE");
                outputStream.writeInt(bytes.length);
                outputStream.write(bytes);
                outputStream.flush();
            }
        }
        else {
            serverResponse.setCorrelationId(messageObj.getCorrelationId());
            serverResponse.setServerResponseStatus(ServerResponseStatus.FAILURE);
            serverResponse.setServerResponseMessage(ServerResponseMessage.THEME_MODE_SET);
            serverResponse.setMessage(UserDaoImpl.errorMessage);
            bytes = objectMapper.writeValueAsBytes(serverResponse);
            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
            outputStream.writeUTF("JSON_RESPONSE");
            outputStream.writeInt(bytes.length);
            outputStream.write(bytes);
            outputStream.flush();
        }
    }
}
