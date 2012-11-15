/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2008 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 * or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 *
 * Contributor(s):
 *
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

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
        for (Throwable error : errors.subList(0,Math.min(errors.size(),20)))
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
