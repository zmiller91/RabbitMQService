package com.zm.rabbitmqservice;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import javafx.util.Pair;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeoutException;

public class RMQConnectionFactory {

    private static final ConcurrentHashMap<Pair<String, String>, ConnectionBag> connections = new ConcurrentHashMap<>();

    public static synchronized Channel create(String host, String queue) throws IOException, TimeoutException {
        Pair<String, String> id = new Pair<>(host, queue);
        if(!connections.containsKey(id)) {
            connections.putIfAbsent(id, new ConnectionBag(host));
        }

        return connections.get(id).getChannel();
    }

    public static synchronized void close(String host, String queue) {
        Pair<String, String> id = new Pair<>(host, queue);
        if(connections.containsKey(id)) {
            connections.computeIfPresent(id, (stringStringPair, connectionBag) -> {
                connectionBag.close();
                return null;
            });
        }
    }

    private static class ConnectionBag {
        private final ConnectionFactory factory;
        private Connection connection;
        private Channel channel;

        private ConnectionBag(String host) {
            this.factory = new ConnectionFactory();
            this.factory.setHost(host);
        }

        private Connection getConnection() throws IOException, TimeoutException {

            if(connection == null) {
                this.connection = factory.newConnection();
            }

            if(!connection.isOpen()) {
                connection.abort();
                this.connection = factory.newConnection();
            }

            return connection;
        }

        private Channel getChannel() throws IOException, TimeoutException {

            if(connection == null) {
                getConnection();
            }

            if(connection.isOpen() && (channel == null || !channel.isOpen())) {
                if(channel != null) {
                    channel.abort();
                }

                this.channel = connection.createChannel();
            }

            return channel;
        }

        private void close() {
            if(this.connection != null) {
                this.connection.abort();
            }
        }
    }
}
