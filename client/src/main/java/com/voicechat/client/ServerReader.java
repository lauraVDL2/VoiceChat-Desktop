package com.voicechat.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.shared.JsonMapper;
import org.shared.ServerResponse;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class ServerReader {
    // Map correlationId -> queue of avatars
    private final ConcurrentMap<String, BlockingQueue<ImageView>> avatarQueues = new ConcurrentHashMap<>();
    // Map correlationId -> queue of server responses
    private final ConcurrentMap<String, BlockingQueue<ServerResponse>> serverResponseQueues = new ConcurrentHashMap<>();
    // Map correlationId -> list of email notifications
    private final ConcurrentMap<String, List<String>> notifications = new ConcurrentHashMap<>();
    // Map correlationId -> avatar URL or ID (if needed)
    private final ConcurrentMap<String, String> avatars = new ConcurrentHashMap<>();
    // Map correlationId -> queue of server responses for processMessage
    private final ConcurrentMap<String, ConcurrentLinkedQueue<ServerResponse>> messageQueues = new ConcurrentHashMap<>();

    private final DataInputStream dataInputStream;
    private volatile boolean running = true;
    private Thread readerThread;

    public ServerReader(DataInputStream dataInputStream) {
        this.dataInputStream = dataInputStream;
    }

    private final ConcurrentHashMap<String, ThreadLocal<List<ServerResponse>>> correlationIdMap = new ConcurrentHashMap<>();

    public void processMessage(ServerResponse response) {
        String correlationId = response.getCorrelationId();
        ThreadLocal<List<ServerResponse>> queue = correlationIdMap.computeIfAbsent(correlationId, k -> new ThreadLocal<>());
        queue.get().add(response);
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


    public List<ServerResponse> getServerResponseByEmail(String emailAddress) {
        List<ServerResponse> responses = new ArrayList<>();
        synchronized (notifications) {
            List<String> correlationIds = notifications.get(emailAddress);
            if (correlationIds != null) {
                for (String correlationId : correlationIds) {
                    if (StringUtils.isNotBlank(correlationId)) {
                        BlockingQueue<ServerResponse> queue = serverResponseQueues.computeIfAbsent(correlationId, k -> new LinkedBlockingQueue<>());
                        // Retrieve all available responses without blocking
                        queue.drainTo(responses);
                    }
                }
            }
        }
        return responses;
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
        } else if (StringUtils.equals(responseType, "AUDIO_RESPONSE")) {
            // Read the size of the audio data
            int size = dataInputStream.readInt();
            if (size > 0) {
                byte[] audioBytes = new byte[size];
                dataInputStream.readFully(audioBytes);
                // Deserialize the audio bytes into ServerResponse
                ObjectMapper objectMapper = JsonMapper.getJsonMapper();
                ServerResponse response = objectMapper.readValue(audioBytes, ServerResponse.class);
                String correlationId = response.getCorrelationId();
                return new ResponseWrapper(correlationId, response);
            }
        }
        else {
            int size = dataInputStream.readInt();
            if (size > 0) {
                byte[] bytes = new byte[size];
                dataInputStream.readFully(bytes);
                ObjectMapper objectMapper = JsonMapper.getJsonMapper();
                ServerResponse response = objectMapper.readValue(bytes, ServerResponse.class);
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
                        return;
                    }
                    ResponseWrapper wrapper = readTargetResponseWrapper();
                    if (wrapper == null) {
                        System.err.println("Received null response wrapper.");
                        continue;
                    }
                    String correlationId = wrapper.correlationId;
                    Object payload = wrapper.payload;

                    if (payload instanceof ImageView) {
                        avatarQueues.computeIfAbsent(correlationId, k -> new LinkedBlockingQueue<>()).put((ImageView) payload);
                    } else if (payload instanceof ServerResponse) {
                        // Distribute responses to the proper queues
                        // For processMessage, responses are added to messageQueues
                        processResponse(correlationId, (ServerResponse) payload);
                        // Also, add to serverResponseQueues for retrieval
                        serverResponseQueues.computeIfAbsent(correlationId, k -> new LinkedBlockingQueue<>()).put((ServerResponse) payload);
                        // Optionally, update notifications if needed
                        synchronized (notifications) {
                            for (var entry : ((ServerResponse) payload).getUserMessageMap().entrySet()) {
                                if (entry.getValue().equals(correlationId)) {
                                    notifications.computeIfAbsent(entry.getKey(), k -> new ArrayList<>()).add(correlationId);
                                }
                            }
                        }
                    } else {
                        System.err.println("Unknown payload type received.");
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Reader thread interrupted: " + e.getMessage());
            } catch (IOException e) {
                System.err.println("Error reading avatar data: " + e.getMessage());
            }
        });
        this.readerThread.start();
    }

    private void processResponse(String correlationId, ServerResponse response) {
        // Add response to the message queue for concurrent processing
        messageQueues.computeIfAbsent(correlationId, k -> new ConcurrentLinkedQueue<>()).add(response);
    }

}
