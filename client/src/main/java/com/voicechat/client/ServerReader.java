package com.voicechat.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.apache.commons.lang3.StringUtils;
import org.shared.JsonMapper;
import org.shared.ServerResponse;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.concurrent.*;

public class ServerReader {
    // Map correlationId -> queue of avatars
    private final ConcurrentMap<String, BlockingQueue<ImageView>> avatarQueues = new ConcurrentHashMap<>();
    // Map correlationId -> queue of server responses
    private final ConcurrentMap<String, BlockingQueue<ServerResponse>> serverResponseQueues = new ConcurrentHashMap<>();

    private final DataInputStream dataInputStream;
    private volatile boolean running = true; // Flag to control thread execution
    private Thread readerThread; // Reference to the thread for stopping

    private final ConcurrentMap<String, String> notifications = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, String> avatars = new ConcurrentHashMap<>();

    public ServerReader(DataInputStream dataInputStream) {
        this.dataInputStream = dataInputStream;
    }

    /**
     * Blocking retrieval of avatar by correlationId.
     * @param correlationId correlation id to wait for
     * @return ImageView avatar
     * @throws InterruptedException if interrupted while waiting
     */
    public ImageView getAvatarByCorrelationId(String correlationId) throws InterruptedException {
        BlockingQueue<ImageView> queue = avatarQueues.computeIfAbsent(correlationId, k -> new LinkedBlockingQueue<>());
        return queue.take();
    }

    /**
     * Blocking retrieval of server response by correlationId.
     * @param correlationId correlation id to wait for
     * @return ServerResponse
     * @throws InterruptedException if interrupted while waiting
     */
    public ServerResponse getServerResponseByCorrelationId(String correlationId) throws InterruptedException {
        BlockingQueue<ServerResponse> queue = serverResponseQueues.computeIfAbsent(correlationId, k -> new LinkedBlockingQueue<>());
        return queue.take();
    }

    public ServerResponse getServerResponseByEmail(String emailAddress) throws InterruptedException {
        String correlationId = notifications.get(emailAddress);
        if (StringUtils.isNotBlank(correlationId)) {
            BlockingQueue<ServerResponse> queue = serverResponseQueues.computeIfAbsent(correlationId, k -> new LinkedBlockingQueue<>());
            return queue.take();
        }
        return null;
    }

    /**
     * Stops the reader thread.
     */
    public void stop() {
        running = false;
        if (readerThread != null) {
            readerThread.interrupt(); // Interrupt to unblock any blocking calls
        }
    }

    /**
     * Internal wrapper class to hold correlationId and payload.
     */
    private static class ResponseWrapper {
        final String correlationId;
        final Object payload; // ImageView or ServerResponse

        ResponseWrapper(String correlationId, Object payload) {
            this.correlationId = correlationId;
            this.payload = payload;
        }
    }

    /**
     * Reads the next response from the dataInputStream and returns a ResponseWrapper containing correlationId and payload.
     * @return ResponseWrapper containing correlationId and payload (ImageView or ServerResponse)
     * @throws IOException on IO error
     */
    private ResponseWrapper readTargetResponseWrapper() throws IOException {
        String responseType = dataInputStream.readUTF();
        if (StringUtils.equals(responseType, "IMAGE_RESPONSE")) {
            int size = dataInputStream.readInt();
            if (size > 0) {
                byte[] imageBytes = new byte[size];
                dataInputStream.readFully(imageBytes);
                ObjectMapper objectMapper = JsonMapper.getJsonMapper();
                ServerResponse response = objectMapper.readValue(imageBytes, ServerResponse.class);
                String correlationId = response.getCorrelationId();
                byte[] avatarBase64 = response.getBinaryPayload();
                try {
                    ImageView avatar = new ImageView();
                    Image image = new Image(new ByteArrayInputStream(avatarBase64));
                    avatar.setImage(image);
                    System.out.println("Avatar read successfully.");
                    return new ResponseWrapper(correlationId, avatar);
                } catch (Exception e) {
                    System.err.println("Error creating Image from bytes: " + e.getMessage());
                    return null;
                }
            } else {
                System.out.println("No avatar data received.");
                return null;
            }
        } else {
            int size = dataInputStream.readInt();
            if (size > 0) {
                byte[] bytes = new byte[size];
                dataInputStream.readFully(bytes);
                ObjectMapper objectMapper = JsonMapper.getJsonMapper();
                ServerResponse response = objectMapper.readValue(bytes, ServerResponse.class);
                System.out.println(response.getServerResponseMessage() + " correlation id " + response.getCorrelationId());
                return new ResponseWrapper(response.getCorrelationId(), response);
            }
        }
        return null;
    }

    /**
     * Modified startReading method to use readTargetResponseWrapper and distribute responses by correlationId.
     */
    public void startReadingWithCorrelationId() {
        readerThread = new Thread(() -> {
            try {
                while (running) {
                    if (dataInputStream == null) {
                        System.err.println("Data Input Stream is null. Exiting reader thread.");
                        return; // Exit the thread if the stream is null
                    }
                    ResponseWrapper wrapper = readTargetResponseWrapper();
                    if (wrapper == null) {
                        System.err.println("Received null response wrapper.");
                        continue;
                    }
                    String correlationId = wrapper.correlationId;
                    if (correlationId == null) {
                        System.err.println("Received response with null correlationId.");
                        continue;
                    }
                    Object payload = wrapper.payload;
                    if (payload instanceof ImageView) {
                        avatarQueues.computeIfAbsent(correlationId, k -> new LinkedBlockingQueue<>()).put((ImageView) payload);
                    } else if (payload instanceof ServerResponse) {
                        ServerResponse serverResponse = (ServerResponse) payload;
                        for (var entry : serverResponse.getUserMessageMap().entrySet()) {
                            if (entry.getValue().equals(correlationId)) {
                                notifications.computeIfAbsent(entry.getKey(), k -> correlationId);
                                System.out.
                                        println("va dans notifications");
                            }
                        }
                        serverResponseQueues.computeIfAbsent(correlationId, k -> new LinkedBlockingQueue<>()).put((ServerResponse) payload);
                    } else {
                        System.err.println("Unknown payload type received.");
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Preserve interrupt status
                System.err.println("Reader thread interrupted: " + e.getMessage());
            } catch (IOException e) {
                System.err.println("Error reading avatar data: " + e.getMessage());
            }
        });
        readerThread.start();
    }
}
