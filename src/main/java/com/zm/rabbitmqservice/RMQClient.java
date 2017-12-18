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
import java.io.InvalidObjectException;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.zm.rabbitmqservice.ServiceUnavailableException.Status.*;

public class RMQClient {

    private String requestQueueName;
    private final Gson gson;
    private final ExecutorService pool;
    private final ConnectionFactory connectionFactory;
    private int timeout = 3000;
    private Integer expiry;
    private Connection connection;
    private Channel channel;

    protected RMQClient(String host, String queue, int executorPoolSize) {
        connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(host);
        requestQueueName = queue;
        ThreadFactory factory = new ThreadFactory() {

            private final AtomicInteger counter = new AtomicInteger();

            @Override
            public Thread newThread(Runnable r) {
                final String threadName = String.format("%s-%d", "rmq-client", counter.incrementAndGet());
                return new Thread(r, threadName);
            }
        };
        pool = new ThreadPoolExecutor(1, executorPoolSize, 500, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), factory);
        gson = new Gson();
    }

    public void setMessageExpiry(Integer expiry) {
        this.expiry = expiry;
    }

    public void setClientTimeout(int timeout) {
        this.timeout = timeout;
    }

    private synchronized void connectToRMQ() throws IOException, TimeoutException {

        if(connection == null) {
            connection = connectionFactory.newConnection(pool);
        }

        if(!connection.isOpen()) {
            connection.abort();
            connection = connectionFactory.newConnection(pool);
        }

        if(connection.isOpen() && (channel == null || !channel.isOpen())) {
            if(channel != null) {
                channel.abort();
            }

            channel = connection.createChannel();
        }
    }

    protected <T> T call(String method, JsonArray params, Class<T> retval) throws TimeoutException, IOException, Throwable {

        connectToRMQ();

        final String corrId = UUID.randomUUID().toString();
        final BlockingQueue<String> response = new ArrayBlockingQueue<>(1);

        RPCRequest request = new RPCRequest();
        request.id = corrId;
        request.method = method;
        request.params = params;
        String message = gson.toJson(request);

        try {
            String replyQueueName = channel.queueDeclare().getQueue();
            AMQP.BasicProperties props = new AMQP.BasicProperties
                    .Builder()
                    .expiration(expiry == null ? null : expiry.toString())
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
                try {
                    Class<? extends Throwable> clazz = (Class<? extends Throwable>) Class.forName(r.error.clazz);
                    throw gson.fromJson(r.error.reason, clazz);
                } catch (ClassNotFoundException e) {
                    throw new ClientException("Could not find exception class.", e);
                }
            }

            return r.getResult(retval);
        }
        catch(InterruptedException e){
            throw new ClientException("Failed to call service.", e);
        }
    }

    public void close() throws IOException {
        if(pool != null) {
            pool.shutdown();
        }

        if(connection != null && connection.isOpen()) {
            connection.close();
        }
    }
}