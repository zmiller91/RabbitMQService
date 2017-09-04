/*
 * Copyright (C) 2017 zmiller
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.zm.rabbitmqservice;

/**
 *
 * @author zmiller
 */


import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

public class RMQClient {

    private Connection connection;
    private String requestQueueName;
    private final Gson gson;

    public RMQClient(String host, String queue, int executorPoolSize) 
            throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(host);
        requestQueueName = queue;
        connection = factory.newConnection(Executors.newFixedThreadPool(executorPoolSize));
        gson = new Gson();
    }

    public String call(String method, JsonArray params) throws IOException, InterruptedException, TimeoutException {
        
        final String corrId = UUID.randomUUID().toString();
        RPCRequest request = new RPCRequest();
        request.id = corrId;
        request.method = method;
        request.params = params;
        String message = gson.toJson(request);
        
        Channel channel = connection.createChannel();
        String replyQueueName = channel.queueDeclare().getQueue();
        AMQP.BasicProperties props = new AMQP.BasicProperties
                .Builder()
                .correlationId(corrId)
                .replyTo(replyQueueName)
                .build();

        channel.basicPublish("", requestQueueName, props, message.getBytes("UTF-8"));
        final BlockingQueue<String> response = new ArrayBlockingQueue<>(100);
        channel.basicConsume(replyQueueName, true, new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                if (properties.getCorrelationId().equals(corrId)) {
                    response.offer(new String(body, "UTF-8"));
                    try {
                        this.getChannel().close();
                    } catch (TimeoutException ex) {}
                }
            }
        });

        return response.take();
    }

    public void close() throws IOException {
        connection.close();
    }
}