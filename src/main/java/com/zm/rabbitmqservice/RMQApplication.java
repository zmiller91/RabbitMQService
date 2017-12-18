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

import com.rabbitmq.client.*;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author zmiller
 */
public class RMQApplication<U> extends TimerTask {


    private final ExecutorService pool;
    private final ConnectionFactory connectionFactory;
    private U app;
    private String queue;
    private Connection connection;
    private Channel channel;

    private RMQApplication(U app, String queue, String host, int poolSize) {
        this.queue = queue;
        connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(host);
        ThreadFactory factory = new ThreadFactory() {

            private final AtomicInteger counter = new AtomicInteger();

            @Override
            public Thread newThread(Runnable r) {
                final String threadName = String.format("%s-%d", "app", counter.incrementAndGet());
                return new Thread(r, threadName);
            }
        };
        pool = new ThreadPoolExecutor(1, poolSize, 500, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), factory);
        this.app = app;
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
            channel.queueDeclare(queue, false, false, false, null);
            channel.basicQos(1);
            AppConsumer<U> appConsumer = new AppConsumer<>(channel, app);
            channel.basicConsume(queue, false, appConsumer);
        }
    }

    @Override
    public void run() {
        try {
            connectToRMQ();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    /**
     * Start the RabbitMQ application. This is the main method that gets executed when creating a RabbitMQ application.
     *
     * @param host - RabbitMQ host
     * @param queueName - RabbitMQ channel
     * @param app - Class that implements an API which will be called when messages are retrieved
     * @param api - API that defines the valid application operations
     * @param executorPoolSize - size of the RabbitMQ thread pool
     */
    public static <T extends U, U> void start(String host, String queueName, T app, Class<U> api, int executorPoolSize) {
        
        // No duplicate method names allowed
        Set<String> names = new HashSet<>();
        for(Method m : api.getMethods()) {
            if(names.contains(m.getName())) {
                throw new RuntimeException("Duplicate API method names are not allowed");
            }

            names.add(m.getName());
        }
        
        // Start consuming the queue
        RMQApplication<U> tr = new RMQApplication<>(app, queueName, host, executorPoolSize);
        new Timer().schedule(tr, 0, 100);
    }
}
