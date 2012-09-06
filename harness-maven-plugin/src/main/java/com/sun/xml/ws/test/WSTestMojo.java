/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * http://glassfish.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
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
package com.sun.xml.ws.test;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.Os;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;
import org.codehaus.plexus.util.cli.DefaultConsumer;
import org.codehaus.plexus.util.cli.StreamConsumer;

/**
 * @goal ws-test
 * @phase test
 * @author lukas
 */
public class WSTestMojo extends AbstractMojo {

    private static final String HARNESS_GID = "com.sun.xml.ws.test";
    private static final String HARNESS_AID = "harness-lib";
    /**
     * @parameter default-value="0.1-SNAPSHOT"
     */
    private String harnessVersion;
    /**
     * Specify the target JAX-WS version being tested. This determines test
     * exclusions.
     *
     * @parameter default-value="2.2.8"
     */
    private String version;
    /**
     * Target folder for JUnit test report XMLs.
     *
     * @parameter default-value="${project.build.directory}/surefire-reports"
     */
    private File resultsDirectory;
    /**
     * @parameter
     */
    private File endorsedDir;
    /**
     * Find test directories recursively.
     *
     * @parameter default-value="true"
     */
    private boolean recursive;
    /**
     * Enable all transport dumps.
     *
     * @parameter default-value="false"
     */
    private boolean dump;
    /**
     * Generate output for debugging harness.
     *
     * @parameter default-value="false"
     */
    private boolean debug;
    /**
     * @parameter default-value="${project.basedir}/src/test/testcases"
     * @required
     */
    private File tests;
    /**
     * @parameter
     */
    private List<String> args;
    /**
     * @parameter
     */
    private List<String> vmArgs;
    /**
     * @parameter default-value=${project.basedir}/lib/ext
     */
    private File extDir;
    /**
     * @component
     */
    private ArtifactFactory artifactFactory;
    /**
     * @component
     */
    private ArtifactResolver resolver;
    /**
     * @parameter default-value="${localRepository}"
     */
    private ArtifactRepository localRepo;
    /**
     * @parameter default-value="${project.remoteArtifactRepositories}"
     */
    private List remoteRepos;
    /**
     * @component
     */
    private ArtifactMetadataSource mdataSource;

    public void execute() throws MojoExecutionException, MojoFailureException {
        Commandline cmd = new Commandline();
        cmd.setExecutable(new File(new File(System.getProperty("java.home"), "bin"), getJavaExec()).getAbsolutePath());
        if (endorsedDir != null && endorsedDir.isDirectory()) {
            cmd.createArg().setValue("-Djava.endorsed.dirs=" + endorsedDir.getAbsolutePath());
        } else {
            //set up
            //cmd.createArg().setLine("-Xbootclasspath/p:");
        }
        if (extDir != null && extDir.exists() && extDir.isDirectory()) {
            cmd.createArg().setValue("-DHARNESS_EXT=" + extDir.getAbsolutePath());
        } else {
            getLog().info("'ext' directory not found");
        }
        if (vmArgs != null) {
            for (String arg : vmArgs) {
                cmd.createArg().setLine(arg);
            }
        }
        cmd.createArg().setLine("-cp " + getHarnessClassPath());
        cmd.createArg().setValue("com.sun.xml.ws.test.Main");
        cmd.createArg().setLine("-report " + resultsDirectory.getAbsolutePath());
        if (recursive) {
            cmd.createArg().setValue("-r");
        }
        if (debug) {
            cmd.createArg().setValue("-debug");
        }
        if (dump) {
            cmd.createArg().setValue("-dump");
        }
        if (version != null && version.trim().length() > 0) {
            cmd.createArg().setLine("-version " + version);
        }
        if (args != null) {
            for (String arg : args) {
                cmd.createArg().setLine(arg);
            }
        }
        cmd.createArg().setValue(tests.getAbsolutePath());

        StreamConsumer sc = new DefaultConsumer();
        getLog().info(cmd.toString());
        try {
            CommandLineUtils.executeCommandLine(cmd, sc, sc);
        } catch (CommandLineException ex) {
            throw new MojoExecutionException(ex.getMessage(), ex);
        }
    }

    private String getJavaExec() {
        return Os.isFamily(Os.FAMILY_WINDOWS) ? "java.exe" : "java";
    }

    private String getHarnessClassPath() throws MojoExecutionException {
        StringBuilder sb = new StringBuilder();
        for (Artifact a : getHarnessLib()) {
            sb.append(a.getFile().getAbsolutePath());
            sb.append(File.pathSeparator);
        }
        return sb.substring(0, sb.length() - 1);
    }

    private Set<Artifact> getHarnessLib() throws MojoExecutionException {
        Artifact harnessLib = artifactFactory.createBuildArtifact(HARNESS_GID, HARNESS_AID, harnessVersion, "jar");
        Artifact dummyArtifact =
                artifactFactory.createBuildArtifact("javax.xml.ws", "jaxws-api", "2.2.8", "jar");
        ArtifactRepositoryPolicy always =
                new ArtifactRepositoryPolicy(true, ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS,
                ArtifactRepositoryPolicy.CHECKSUM_POLICY_WARN);
        ArtifactResolutionResult arr = null;
        try {
            arr = resolver.resolveTransitively(Collections.singleton(harnessLib), dummyArtifact,
                    remoteRepos, localRepo, mdataSource);
        } catch (Exception e) {
            throw new MojoExecutionException("Couldn't download artifact: " + e.getMessage(), e);
        }
        return arr.getArtifacts();
    }
}
