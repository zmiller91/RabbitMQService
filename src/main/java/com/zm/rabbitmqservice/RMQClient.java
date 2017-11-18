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
import java.util.concurrent.*;

public class RMQClient {

    private Connection connection;
    private String requestQueueName;
    private final Gson gson;
    private final ExecutorService pool;

    protected RMQClient(String host, String queue, int executorPoolSize) throws ClientException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(host);
        requestQueueName = queue;
        pool = Executors.newFixedThreadPool(executorPoolSize);
        try {
            connection = factory.newConnection(pool);
        } catch (IOException | TimeoutException e) {
            throw new ClientException("Could not connect to RabbitMQ client.", e);
        }
        gson = new Gson();
    }

    protected <T> T call(String method, JsonArray params, Class<T> retval) throws Throwable {
        
        final String corrId = UUID.randomUUID().toString();
        final BlockingQueue<String> response = new ArrayBlockingQueue<>(100);

        RPCRequest request = new RPCRequest();
        request.id = corrId;
        request.method = method;
        request.params = params;
        String message = gson.toJson(request);

        try {

            Channel channel = connection.createChannel();
            String replyQueueName = channel.queueDeclare().getQueue();
            AMQP.BasicProperties props = new AMQP.BasicProperties
                    .Builder()
                    .correlationId(corrId)
                    .replyTo(replyQueueName)
                    .build();

            channel.basicPublish("", requestQueueName, props, message.getBytes("UTF-8"));
            channel.basicConsume(replyQueueName, true, new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                    if (properties.getCorrelationId().equals(corrId)) {
                        String r = new String(body, "UTF-8");
                        response.offer(r);
                        try {
                            this.getChannel().close();
                        } catch (TimeoutException e) {
                            //TODO do something here
                        }
                    }
                }
            });

            RPCResponse<T> r = gson.fromJson(response.take(), RPCResponse.class);
            if(r.error != null) {
                try {
                    Class<? extends Throwable> clazz = (Class<? extends Throwable>) Class.forName(r.error.clazz);
                    throw gson.fromJson(r.error.reason, clazz);
                } catch (ClassNotFoundException e) {
                    throw new ClientException("Could not find exception class.", e);
                }
            }

            return r.result;
        }
        catch(IOException | InterruptedException e){
            throw new ClientException("Failed to call service.", e);
        }
    }

    public void close() throws IOException {
        pool.shutdown();
        connection.close();
    }
}