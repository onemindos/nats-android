package dev.onemindos.nats;

import dev.onemindos.nats.INatsMessageCallback;

interface INatsLeafService {
    boolean isConnected();
    void publish(String subject, in byte[] payload);
    void subscribe(String subject, INatsMessageCallback callback);
    void unsubscribe(String subject);
}
