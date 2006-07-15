package com.sun.xml.ws.test.exec;

import com.sun.xml.ws.test.container.DeploymentContext;

/**
 * {@link Executor} that cleans up the work directory (if necessary)
 * and create directories (if necessary).
 *
 * This is needed to avoid picking up old artifacts from the previous test run.
 *
 * @author Kohsuke Kawaguchi
 */
public class PrepareExecutor extends Executor {

    private final boolean clean;

    public PrepareExecutor(DeploymentContext context, boolean clean) {
        super(clean?"Clean ":"Prepare ", context);
        this.clean = clean;
    }

    public void runBare() throws Throwable {
        context.prepare(clean);
    }
}
