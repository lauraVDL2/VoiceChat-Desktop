package org.server.action;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.shared.ServerResponse;
import org.shared.ServerResponseMessage;
import org.shared.ServerResponseStatus;

import java.io.IOException;

public abstract class AbstractAction {

    public byte[] buildSuccessResponseWithPayload(ServerResponse serverResponse, ServerResponseMessage serverResponseMessage,
                                                  String correlationId, Object binaryPayloadContent, ObjectMapper objectMapper) throws IOException {
        serverResponse.setCorrelationId(correlationId);
        serverResponse.setServerResponseStatus(ServerResponseStatus.SUCCESS);
        serverResponse.setServerResponseMessage(serverResponseMessage);
        serverResponse.setBinaryPayload(objectMapper.writeValueAsBytes(binaryPayloadContent));
        return objectMapper.writeValueAsBytes(serverResponse);
    }

    public byte[] buildSuccessResponse(ServerResponse serverResponse, ServerResponseMessage serverResponseMessage,
    String correlationId, ObjectMapper objectMapper) throws IOException {
        serverResponse.setCorrelationId(correlationId);
        serverResponse.setServerResponseStatus(ServerResponseStatus.SUCCESS);
        serverResponse.setServerResponseMessage(serverResponseMessage);
        return objectMapper.writeValueAsBytes(serverResponse);
    }

    public byte[] buildFailureResponse(ServerResponse serverResponse, ServerResponseMessage serverResponseMessage,
                                       String correlationId, String message, ObjectMapper objectMapper) throws IOException {
        serverResponse.setCorrelationId(correlationId);
        serverResponse.setServerResponseStatus(ServerResponseStatus.FAILURE);
        serverResponse.setServerResponseMessage(serverResponseMessage);
        serverResponse.setMessage(message);
        return objectMapper.writeValueAsBytes(serverResponse);
    }
}
