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
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
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
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;
import org.codehaus.plexus.util.IOUtil;
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

    private static final String HARNESS_GID = "org.glassfish.metro";
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
     * @parameter default-value="${project.basedir}/lib/ext"
     */
    private File extDir;
    /**
     * @parameter expression="${ws.image}"
     */
    private URL image;
    /**
     * @parameter expression="${ws.transport}"
     */
    private URL transport;
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
     * @parameter property="project"
     * @readonly
     */
    private MavenProject project;
    /**
     * To look up Archiver/UnArchiver implementations
     *
     * @component
     */
    protected ArchiverManager archiverManager;
    /**
     * @component
     */
    private ArtifactMetadataSource mdataSource;
    private File imageFolder = null;
    private File transportFile = null;

    public void execute() throws MojoExecutionException, MojoFailureException {
        if (image != null) {
            try {
                getLog().info("Downloading: " + image);
                imageFolder = new File(project.getBuild().getDirectory(), "tested-image");
                if (imageFolder.mkdirs()) {
                    File imageFile = new File(imageFolder, new File(image.getFile()).getName());
                    if (imageFile.createNewFile()) {
                        getLog().info("to: " + imageFile.getAbsolutePath());
                        IOUtil.copy(image.openStream(), new FileOutputStream(imageFile));
                        getLog().info("unpacking " + imageFile.getName() + "...");
                        UnArchiver unArchiver = archiverManager.getUnArchiver(imageFile);
                        unArchiver.setSourceFile(imageFile);
                        unArchiver.setDestDirectory(imageFolder);
                        unArchiver.extract();
                    }
                }
            } catch (NoSuchArchiverException ex) {
                Logger.getLogger(WSTestMojo.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(WSTestMojo.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if (transport != null) {
            try {
                getLog().info("Downloading: " + transport);
                imageFolder = new File(project.getBuild().getDirectory(), "tested-image");
                if (imageFolder.exists() || imageFolder.mkdirs()) {
                    transportFile = new File(imageFolder, new File(transport.getFile()).getName());
                    if (transportFile.createNewFile()) {
                        getLog().info("to: " + transportFile.getAbsolutePath());
                        IOUtil.copy(transport.openStream(), new FileOutputStream(transportFile));
                    }
                }
            } catch (IOException ex) {
                Logger.getLogger(WSTestMojo.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        Commandline cmd = new Commandline();
        cmd.setExecutable(new File(new File(System.getProperty("java.home"), "bin"), getJavaExec()).getAbsolutePath());
        //APIs have to be first
        if (endorsedDir != null && endorsedDir.isDirectory()) {
            cmd.createArg().setValue("-Djava.endorsed.dirs=" + endorsedDir.getAbsolutePath());
        } else {
            //is it JAXWS-RI?
            if (imageFolder != null) {
            File img = new File(imageFolder, "jaxws-ri");
            if (img.exists() && img.isDirectory()) {
                StringBuilder sb = new StringBuilder();
                sb.append(new File(img, "lib/saaj-api.jar"));
                sb.append(File.pathSeparatorChar);
                sb.append(new File(img, "lib/jaxb-api.jar"));
                sb.append(File.pathSeparatorChar);
                sb.append(new File(img, "lib/jaxws-api.jar"));
                cmd.createArg().setLine("-Xbootclasspath/p:" + sb.toString());
            } else {
                //or Metro?
                img = new File(imageFolder, "metro");
                if (img.exists() && img.isDirectory()) {
                    File wsApis = new File(img, "lib/webservices-api.jar");
                    cmd.createArg().setLine("-Xbootclasspath/p:" + wsApis.getAbsolutePath());
                }
            }
        }
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
        if (imageFolder != null) {
            if (transportFile != null) {
                cmd.createArg().setLine("-transport " + transportFile.getAbsolutePath());
            }
            File img = new File(imageFolder, "jaxws-ri");
            if (img.exists() && img.isDirectory()) {
                cmd.createArg().setLine("-cp:jaxws-image " + img.getAbsolutePath());
            } else {
                img = new File(imageFolder, "metro");
                if (img.exists() && img.isDirectory()) {
                    cmd.createArg().setLine("-cp:wsit-image " + img.getAbsolutePath());
                }
            }
        } else {
            if (args != null) {
                for (String arg : args) {
                    cmd.createArg().setLine(arg);
                }
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
