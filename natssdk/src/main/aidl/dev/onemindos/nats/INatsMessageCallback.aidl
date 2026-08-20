package dev.onemindos.nats;

oneway interface INatsMessageCallback {
    void onMessage(String subject, in byte[] payload);
}
