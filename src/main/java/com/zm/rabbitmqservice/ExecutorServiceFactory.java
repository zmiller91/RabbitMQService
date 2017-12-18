package com.zm.rabbitmqservice;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ExecutorServiceFactory {

    private final static AtomicInteger counter = new AtomicInteger();
    public static ExecutorService create(String poolName, int maxSize) {

        ThreadFactory factory = new ThreadFactory() {

            @Override
            public Thread newThread(Runnable r) {
                final String threadName = String.format("%s-%d", poolName, counter.incrementAndGet());
                return new Thread(r, threadName);
            }
        };

        return new ThreadPoolExecutor(1, maxSize, 500, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), factory);
    }
}
