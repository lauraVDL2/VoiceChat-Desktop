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

public class SettingsAction extends AbstractAction {
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
                bytes = buildSuccessResponseWithPayload(serverResponse, ServerResponseMessage.THEME_MODE_SET,
                        messageObj.getCorrelationId(), settings, objectMapper);
                DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
                outputStream.writeUTF("JSON_RESPONSE");
                outputStream.writeInt(bytes.length);
                outputStream.write(bytes);
                outputStream.flush();
            }
            else {
                bytes = buildFailureResponse(serverResponse, ServerResponseMessage.THEME_MODE_SET, messageObj.getCorrelationId(),
                        "", objectMapper);
                DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
                outputStream.writeUTF("JSON_RESPONSE");
                outputStream.writeInt(bytes.length);
                outputStream.write(bytes);
                outputStream.flush();
            }
        }
        else {
            bytes = buildFailureResponse(serverResponse, ServerResponseMessage.THEME_MODE_SET, messageObj.getCorrelationId(),
                    "", objectMapper);
            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
            outputStream.writeUTF("JSON_RESPONSE");
            outputStream.writeInt(bytes.length);
            outputStream.write(bytes);
            outputStream.flush();
        }
    }
}
