package com.sun.xml.ws.test.container.cargo.gf;

import org.codehaus.cargo.container.internal.J2EEContainerCapability;
import org.codehaus.cargo.container.deployable.DeployableType;

/**
 * @author Kohsuke Kawaguchi
 */
final class GlassfishContainerCapability extends J2EEContainerCapability {
    public boolean supportsDeployableType(DeployableType type)
    {
        return (type == DeployableType.EJB) || super.supportsDeployableType(type);
    }
}
