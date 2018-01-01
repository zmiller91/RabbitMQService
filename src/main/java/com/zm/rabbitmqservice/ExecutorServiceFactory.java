package com.zm.rabbitmqservice;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ExecutorServiceFactory {

    private final static AtomicInteger counter = new AtomicInteger();

    public static ExecutorService createWithSize(String poolName, int corePoolSize, int maxSize) {
        return createCustom(poolName, corePoolSize, maxSize,
                60, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
    }

    public static ExecutorService createDefault(String poolName, int maxSize) {
        return createWithSize(poolName, 1, maxSize);
    }

    public static ExecutorService createCustom(String poolName, int corePoolSize, int maxSize, long threadKeepAliveTime,
                                               TimeUnit unit, BlockingQueue<Runnable> executor) {

        ThreadFactory factory = r -> {
            final String threadName = String.format("%s-%d", poolName, counter.incrementAndGet());
            return new Thread(r, threadName);
        };

        return new ThreadPoolExecutor(corePoolSize, maxSize, threadKeepAliveTime, unit, executor, factory);
    }
}
