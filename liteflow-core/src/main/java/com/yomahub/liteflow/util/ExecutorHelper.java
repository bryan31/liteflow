/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2020/4/1
 */
package com.yomahub.liteflow.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;


/**
 * 线程池工具类
 * @author Bryan.Zhang
 */
public class ExecutorHelper {
    
    private static final Logger LOG = LoggerFactory.getLogger(ExecutorHelper.class);
    
    private ExecutorHelper() {
    }

    /**
     * 使用默认的等待时间1分钟，来关闭目标线程组。
     * <p>
     *
     * @param pool 需要关闭的线程组.
     */
    public static void shutdownAwaitTermination(ExecutorService pool) {
        shutdownAwaitTermination(pool, 60L);
    }

    /**
     * 关闭ExecutorService的线程管理者
     * <p>
     *
     * @param pool    需要关闭的管理者
     * @param timeout 等待时间
     */
    public static void shutdownAwaitTermination(ExecutorService pool,
                                                long timeout) {
        pool.shutdown();
        try {
            if (!pool.awaitTermination(timeout, TimeUnit.SECONDS)) {
                pool.shutdownNow();
                if (!pool.awaitTermination(timeout, TimeUnit.SECONDS)) {
                    LOG.error("Pool did not terminate.");
                }
            }
        } catch (InterruptedException ie) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 返回一个线程工厂,这是一个可以定义线程名字的线程工厂,返回的线程都将被命名.
     * 创建的线程都是非后台线程.
     *
     * @param name 名称.
     * @return 线程工厂实例.
     */
    public static ThreadFactory buildExecutorFactory(final String name) {
        return buildExecutorFactory(name, false);
    }

    /**
     * 返回一个线程工厂,这是一个可以定义线程名字的线程工厂,返回的线程都将被命名.
     *
     * @param name   名称.
     * @param daemon 是否为后台线程.
     * @return 线程工厂实例.
     */
    public static ThreadFactory buildExecutorFactory(final String name, final boolean daemon) {
        return new ThreadFactory() {

            private final AtomicLong number = new AtomicLong();

            @Override
            public Thread newThread(Runnable r) {
                Thread newThread = Executors.defaultThreadFactory().newThread(r);
                newThread.setName(name + "-" + number.getAndIncrement());
                newThread.setDaemon(daemon);
                return newThread;
            }

        };
    }

    public static ExecutorService buildExecutor(int worker, int queue, String namePrefix, boolean daemon) {
        return new ThreadPoolExecutor(worker, worker,
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queue),
                buildExecutorFactory(namePrefix, daemon),
                new ThreadPoolExecutor.AbortPolicy()
        );
    }
}
