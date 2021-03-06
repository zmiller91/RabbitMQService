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
package com.zm.rabbitmqservice.client;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.zm.rabbitmqservice.RMQConnectionFactory;
import com.zm.rabbitmqservice.RPCRequest;
import com.zm.rabbitmqservice.RPCResponse;
import com.zm.rabbitmqservice.service.ServiceUnavailableException;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.*;

import static com.zm.rabbitmqservice.service.ServiceUnavailableException.Status.*;

public class RMQClient {

    private final String host;
    private final String requestQueueName;
    private final Gson gson;

    private int timeout = 3000;
    private Integer expiry;

    protected RMQClient(String host, String queue) {
        System.out.println("Creating new RMQClient");
        this.host = host;
        requestQueueName = queue;
        this.gson = new Gson();
    }

    public void setMessageExpiry(Integer expiry) {
        this.expiry = expiry;
    }

    public void setClientTimeout(int timeout) {
        this.timeout = timeout;
    }

    //TODO: Provide a similar method that returns a future

    protected <T> T call(String method, JsonArray params, Class<T> retval) throws TimeoutException, IOException, Throwable {

        Channel channel = RMQConnectionFactory.create(host, requestQueueName);
        if(channel == null) {
            throw new ClientException("Failed to create client", null);
        }

        final String corrId = UUID.randomUUID().toString();
        final BlockingQueue<String> response = new ArrayBlockingQueue<>(1);

        String consumerTag = null;
        RPCRequest request = new RPCRequest();
        request.id = corrId;
        request.method = method;
        request.params = params;
        String message = gson.toJson(request);

        try {

            //TODO: Not everything requires a response from the server. Allow messages to be added to the queue without
            //TODO: waiting for a response

            String replyQueueName = channel.queueDeclare().getQueue();
            AMQP.BasicProperties props = new AMQP.BasicProperties
                    .Builder()
                    .expiration(expiry == null ? null : expiry.toString())
                    .correlationId(corrId)
                    .replyTo(replyQueueName)
                    .build();

            channel.basicPublish("", requestQueueName, props, message.getBytes("UTF-8"));
            consumerTag = channel.basicConsume(replyQueueName, true, new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                    if (properties.getCorrelationId().equals(corrId)) {
                        String r = new String(body, "UTF-8");
                        response.offer(r);
                    }
                }
            });

            String re = response.poll(timeout, TimeUnit.MILLISECONDS);
            if(re == null) {
                throw new ServiceUnavailableException(expiry == null ? IN_QUEUE : EXPIRED);
            }

            RPCResponse r = gson.fromJson(re, RPCResponse.class);
            if (r == null) {
                throw new ClientException("Invalid RPC Response: \"" + re + "\"", null);
            }

            if(r.error != null) {
                Throwable error;

                try {
                    Class<? extends Throwable> clazz = (Class<? extends Throwable>) Class.forName(r.error.clazz);
                    error = gson.fromJson(r.error.reason, clazz);
                } catch (ClassNotFoundException e) {
                    throw new ClientException("Could not find exception class.", e);
                } catch(Throwable t) {
                    throw new ClientException("Failed to throw error.", t);
                }

                if(error != null) {
                    throw error;
                }
            }

            return r.getResult(retval);
        }
        finally {
            if(consumerTag != null) {
                channel.basicCancel(consumerTag);
            }
        }
    }

    public void close() throws IOException {
        RMQConnectionFactory.close(host, requestQueueName);
    }
}