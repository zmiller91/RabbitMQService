package com.zm.rabbitmqservice;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import javafx.util.Pair;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeoutException;

class RMQConnectionFactory {

    private static final ConcurrentHashMap<Pair<String, String>, ConnectionBag> connections = new ConcurrentHashMap<>();

    static synchronized Channel create(String host, String queue, ExecutorService pool) throws IOException, TimeoutException {
        Pair<String, String> id = new Pair<>(host, queue);
        if(!connections.containsKey(id)) {
            connections.putIfAbsent(id, new ConnectionBag(host, pool));
        }

        return connections.get(id).getChannel();
    }

    static synchronized void close(String host, String queue) {
        Pair<String, String> id = new Pair<>(host, queue);
        if(connections.containsKey(id)) {
            connections.computeIfPresent(id, (stringStringPair, connectionBag) -> {
                connectionBag.close();
                return null;
            });
        }
    }

    private static class ConnectionBag {
        private final ExecutorService pool;
        private final ConnectionFactory factory;
        private Connection connection;
        private Channel channel;

        private ConnectionBag(String host, ExecutorService pool) {
            this.pool = pool;
            this.factory = new ConnectionFactory();
            this.factory.setHost(host);
        }

        private Connection getConnection() throws IOException, TimeoutException {

            if(connection == null) {
                this.connection = factory.newConnection(pool);
            }

            if(!connection.isOpen()) {
                connection.abort();
                this.connection = factory.newConnection(pool);
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

            if(this.pool != null) {
                this.pool.shutdown();
            }
        }
    }
}
