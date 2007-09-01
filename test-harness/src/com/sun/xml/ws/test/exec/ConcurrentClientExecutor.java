package com.sun.xml.ws.test.exec;

import bsh.Interpreter;
import com.sun.xml.ws.test.container.DeploymentContext;
import com.sun.xml.ws.test.model.TestClient;

import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Executes {@link TestClient} in concurrent fashion via
 * {@link Executor}.
 *
 * @author Kohsuke Kawaguchi
 */
public abstract class ConcurrentClientExecutor extends ClientExecutor {
    /**
     * Degree of concurrency.
     */
    public static final int THREAD_COUNT = 20;

    public static final int REQUESTS = 50000;

    public ConcurrentClientExecutor(DeploymentContext context, TestClient client) {
        super(context, client);
    }

    /**
     * Runs the actual test in highly concurrent fashion.
     */
    protected void invoke(final Interpreter engine) throws Throwable {
        ExecutorService service = createExecutorService();
        // record all errors
        final Vector<Throwable> errors = new Vector<Throwable>();

        for(int i=0; i<REQUESTS && errors.isEmpty(); i++) {
            service.execute(new Runnable() {
                public void run() {
                    try {
                        ConcurrentClientExecutor.super.invoke(engine);
                    } catch(Throwable e) {
                        errors.add(e);
                    }
                }
            });
        }
        service.shutdown();

        while(!service.awaitTermination(1L, TimeUnit.SECONDS))
            ;

        // if any error, print first 20 of them
        if(!errors.isEmpty())
            System.out.printf("Found %d errors\n",errors.size());
        for (Throwable error : errors.subList(0,Math.max(errors.size(),20)))
            error.printStackTrace();
        if(!errors.isEmpty())
            throw errors.get(0);    // and throw the first failure to make the test fail
    }

    /**
     * Creates the {@link ExecutorService} used for testing.
     */
    protected abstract ExecutorService createExecutorService();

    /**
     * Fixed thread pool.
     */
    public static final class Fixed extends ConcurrentClientExecutor {
        public Fixed(DeploymentContext context, TestClient client) {
            super(context, client);
        }

        protected ExecutorService createExecutorService() {
            return Executors.newFixedThreadPool(THREAD_COUNT);
        }
    }

    /**
     * Fixed thread pool.
     */
    public static final class Cached extends ConcurrentClientExecutor {
        public Cached(DeploymentContext context, TestClient client) {
            super(context, client);
        }

        protected ExecutorService createExecutorService() {
            return Executors.newCachedThreadPool();
        }
    }
}
