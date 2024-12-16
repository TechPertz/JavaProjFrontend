package com.example.swinggradleapp.client;

public interface Client {
    boolean connect();
    void sendMessage(String message);

    void close();
}
