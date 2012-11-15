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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
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
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Settings;
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
    private static final String TOPLINK_FACTORY = "com.sun.xml.ws.db.toplink.JAXBContextFactory";

    public enum Databinding {

        DEFAULT, TOPLINK;
    }

    public enum Container {

        IN_VM, LWHS, TOMCAT;
    }
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
     * @parameter default-value="DEFAULT"
     */
    private Databinding databinding;
    /**
     * @parameter
     */
    private List<String> args;
    /**
     * @parameter property="ws.jvmOpts"
     */
    private String extraVmArgs;
    /**
     * @parameter
     */
    private List<String> vmArgs;
    /**
     * @parameter default-value="${project.basedir}/lib/ext"
     */
    private File extDir;
    /**
     * @parameter property="ws.imageUrl"
     */
    private String imageUrl;
    /**
     * @parameter property="ws.localImage"
     * default-value="${project.build.directory}/image"
     */
    private File localImage;
    /**
     * @parameter property="ws.transport" default-value="IN_VM"
     */
    private Container transport;
    /**
     * @parameter property="ws.transportUrl"
     */
    private String transportUrl;
    /**
     * @parameter property="tomcat.home"
     */
    private File tomcatHome;
    /**
     * @parameter default-value="${project.basedir}/misc"
     */
    private File wsitConf;
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
     * @parameter default-value="${settings}"
     * @readonly
     */
    private Settings settings;    /**
     * To look up Archiver/UnArchiver implementations
     *
     * @component
     */
    protected ArchiverManager archiverManager;
    /**
     * @component
     */
    private ArtifactMetadataSource mdataSource;
    private File imageRoot = null;

    public void execute() throws MojoExecutionException, MojoFailureException {
        File imageFolder = new File(project.getBuild().getDirectory(), "tested-image");
        try {
            URL u = new URL(imageUrl);
            try {
                imageFolder.mkdirs();
                imageRoot = prepareImage(u, imageFolder);
                getLog().info("testing downloaded image...");
            } catch (NoSuchArchiverException ex) {
                throw new MojoExecutionException(ex.getMessage(), ex);
            } catch (IOException ex) {
                throw new MojoFailureException(ex.getMessage(), ex);
            }
        } catch (MalformedURLException muex) {
        }
        if (imageRoot == null) {
            if (localImage.exists() && localImage.isDirectory() && findImageRoot(localImage) != null) {
                imageRoot = findImageRoot(localImage);
                getLog().info("testing local image...");
            } else {
                getLog().info("testing local workspace...");
            }
        } else {
            getLog().info("testing local workspace...");
        }

        Commandline cmd = new Commandline();
        cmd.setExecutable(new File(new File(System.getProperty("java.home"), "bin"), getJavaExec()).getAbsolutePath());
        cmd.setWorkingDirectory(project.getBasedir());

        //set API bootclasspath/endorsed
        if (imageRoot != null) {
            cmd.createArg().setLine("-Xbootclasspath/p:" + getAPICP(imageRoot));
        } else if (endorsedDir != null && endorsedDir.isDirectory()) {
            cmd.createArg().setValue("-Djava.endorsed.dirs=" + endorsedDir.getAbsolutePath());
        } else {
            getLog().warn("Endorsed not applied. Set 'endorsedDir' in plugin's configuration.");
        }

        if (extDir != null && extDir.exists() && extDir.isDirectory()) {
            cmd.createArg().setValue("-DHARNESS_EXT=" + extDir.getAbsolutePath());
        } else {
            getLog().info("'ext' directory not found");
        }

        if (wsitConf != null && wsitConf.exists() && wsitConf.isDirectory()) {
            cmd.createArg().setValue("-DWSIT_HOME=" + wsitConf.getAbsolutePath());
        }

        if (isToplink()) {
            cmd.createArg().setLine("-DBindingContextFactory=" + TOPLINK_FACTORY);
        }

        if (extraVmArgs != null && extraVmArgs.trim().length() > 1) {
            cmd.createArg().setLine(extraVmArgs);
        }

        if (settings != null) {
            for (Proxy p : settings.getProxies()) {
                if (p.isActive()) {
                    String protocol = p.getProtocol().trim();
                    if (p.getHost() != null) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("-D");
                        sb.append(protocol);
                        sb.append(".proxyHost=");
                        sb.append(p.getHost());
                        cmd.createArg().setLine(sb.toString());
                    }
                    if (p.getPort() > -1) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("-D");
                        sb.append(protocol);
                        sb.append(".proxyPort=");
                        sb.append(p.getPort());
                        cmd.createArg().setLine(sb.toString());
                    }
                    if (p.getNonProxyHosts() != null) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("-D");
                        sb.append(protocol);
                        sb.append(".nonProxyHosts=\"");
                        sb.append(p.getNonProxyHosts().trim());
                        sb.append("\"");
                        cmd.createArg().setValue(sb.toString());
                    }
                }
            }
        }

        if (vmArgs != null) {
            for (String arg : vmArgs) {
                if (arg.contains("-DBindingContextFactory=") && isToplink()) {
                    String[] opts = arg.split(" ");
                    if (opts.length > 1) {
                        StringBuilder sb = new StringBuilder();
                        for (String opt : opts) {
                            if (opt.trim().startsWith("-DBindingContextFactory=")) {
                                getLog().info("removing '" + opt + "' from default configuration.");
                                continue;
                            }
                            sb.append(opt);
                            sb.append(" ");
                        }
                        cmd.createArg().setLine(sb.toString().trim());
                    } else {
                        getLog().info("removing '" + arg + "' from default configuration.");
                    }
                } else {
                    cmd.createArg().setLine(arg);
                }
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

        switch (transport) {
            case IN_VM:
                if (imageRoot != null) {
                    File transportFile = null;
                    try {
                        transportFile = download(new URL(transportUrl), imageFolder);
                    } catch (IOException ex) {
                        transportFile = new File(project.getBuild().getDirectory(), "test-lib/jaxws-local-transport.jar");
                        if (!transportFile.exists()) {
                            throw new MojoExecutionException("Cannot find local transport jar.  Set 'transportUrl' in plugin's configuration.", ex);
                        }
                    }
                    cmd.createArg().setLine("-transport " + transportFile.getAbsolutePath());
                }
                break;
            case LWHS:
                cmd.createArg().setLine("-lwhs");
                break;
            case TOMCAT:
                if (tomcatHome == null) {
                    throw new MojoExecutionException("'tomcat.home' is not set.");
                }
                cmd.createArg().setLine("-tomcat-embedded " + tomcatHome.getAbsolutePath());
                break;
        }

        List<String> filters = new ArrayList<String>();
        if (imageRoot != null) {
            if (isJaxWsRIRoot(imageRoot)) {
                cmd.createArg().setLine("-cp:jaxws-image " + imageRoot.getAbsolutePath());
            } else if (isMetroRoot(imageRoot)) {
                cmd.createArg().setLine("-cp:wsit-image " + imageRoot.getAbsolutePath());
            } else {
                throw new MojoExecutionException("Unknown/Unsupported image: " + imageRoot);
            }
            if (isToplink()) {
                cmd.createArg().setLine("-cp:override " + getToplinkCP(imageRoot));
                filters.add("-cp:override");
            }
            filters.add("-cp:jaxws");
            filters.add("-cp:wsit");
        }

        if (args != null) {
            for (String arg : args) {
                if (filters.isEmpty()) {
                    cmd.createArg().setLine(arg);
                } else {
                    boolean found = false;
                    for (String filter : filters) {
                        if (arg.contains(filter)) {
                            getLog().info("removing '" + arg + "' from default configuration.");
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        cmd.createArg().setLine(arg);
                    }
                }
            }
        }

        cmd.createArg().setLine(tests.getAbsolutePath());

        StreamConsumer sc = new DefaultConsumer();
        //TODO: move to debug level
        getLog().info(cmd.toString());
        try {
            CommandLineUtils.executeCommandLine(cmd, sc, sc);
        } catch (CommandLineException ex) {
            throw new MojoExecutionException(ex.getMessage(), ex);
        }
    }

    private File download(URL u, File destDir) throws IOException {
        getLog().info("Downloading: " + u);
        File imageZip = new File(destDir, new File(u.getFile()).getName());
        if (imageZip.createNewFile()) {
            getLog().info("to: " + imageZip.getAbsolutePath());
            IOUtil.copy(u.openStream(), new FileOutputStream(imageZip));
        }
        return imageZip;
    }

    private File prepareImage(URL u, File destDir) throws IOException, NoSuchArchiverException {
        File zip = download(u, destDir);
        getLog().info("unpacking " + zip.getName() + "...");
        UnArchiver unArchiver = archiverManager.getUnArchiver(zip);
        unArchiver.setSourceFile(zip);
        unArchiver.setDestDirectory(destDir);
        unArchiver.extract();
        return findImageRoot(destDir);
    }

    private File findImageRoot(File dir) {
        File f = new File(dir, "jaxws-ri");
        if (f.exists() && f.isDirectory()) {
            return f;
        }
        f = new File(dir, "metro");
        if (f.exists() && f.isDirectory()) {
            return f;
        }
        return null;
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

    private boolean isToplink() {
        return Databinding.TOPLINK == databinding;
    }

    private String getCP(File root, String... paths) {
        StringBuilder sb = new StringBuilder();
        for (String p : paths) {
            sb.append(new File(root, p).getAbsolutePath());
            sb.append(File.pathSeparatorChar);
        }
        return sb.substring(0, sb.length() - 1);
    }

    private String getAPICP(File root) throws MojoExecutionException {
        if (isJaxWsRIRoot(root)) {
            return getCP(root,
                    "lib/saaj-api.jar",
                    "lib/jaxb-api.jar",
                    "lib/jaxws-api.jar");
        } else if (isMetroRoot(root)) {
            return getCP(root, "lib/webservices-api.jar");
        }
        throw new MojoExecutionException("Unknown/Unsupported image: " + imageRoot);
    }

    private String getToplinkCP(File root) throws MojoExecutionException {
        if (isJaxWsRIRoot(root)) {
            return getCP(root,
                    "lib/plugins/jaxws-eclipselink-plugin.jar",
                    "lib/plugins/eclipselink.jar",
                    "lib/plugins/mail.jar");
        } else if (isMetroRoot(root)) {
            return getCP(root, "lib/databinding/jaxws-eclipselink-plugin.jar") +
                    File.pathSeparatorChar +
                    getCP(new File(project.getBuild().getDirectory()), "test-lib/eclipselink.jar");
        }
        throw new MojoExecutionException("Unknown/Unsupported image: " + imageRoot);
    }

    private boolean isJaxWsRIRoot(File root) {
        return "jaxws-ri".equals(root.getName());
    }

    private boolean isMetroRoot(File root) {
        return "metro".equals(root.getName());
    }
}
