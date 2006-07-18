package com.sun.xml.ws.test;

import org.codehaus.classworlds.ClassWorld;
import org.codehaus.classworlds.DuplicateRealmException;
import org.codehaus.classworlds.ClassRealm;
import org.codehaus.classworlds.NoSuchRealmException;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.Project;

import java.io.File;
import java.io.IOException;
import java.io.FilenameFilter;
import java.util.logging.Logger;

/**
 * "Global variables" for the test harness. Use with caution.
 *
 * This class includes pointers to
 * various {@link ClassRealm}s that represent compartments inside the VM.
 *
 * <p>
 * The followings are the key realms:
 *
 * <ol>
 * <li>"harness" realm that loads all the test harness code,
 *     including lots of 3rd party jars.
 * <li>"runtime" realm that loads the classes that the client script will use
 *     to execute tests.
 * <li>"wsimport" realm that loads the tool/wsgen tools, if we invoke it
 *     within the same VM. Otherwise this realm is empty.
 *
 * <p>
 * Realms are created when {@link World} is created, but they are filled in
 * from {@link Main}.
 *
 * @author Kohsuke Kawaguchi
 */
public final class World {
    /**
     * Whenever we need a {@link Project} to use Ant tasks,
     * we can use this shared instance.
     */
    public static final Project project = new Project();

    public static final ClassWorld world = initWorld();

    public static final ClassRealm harness = initRealm(null,"harness");
    public static Path harnessClasspath = new Path(project);

    public static final ClassRealm runtime = initRealm(null,"runtime");
    public static Path runtimeClasspath = new Path(project);

    public static final ClassRealm tool = initRealm(runtime,"tool");
    public static Path toolClasspath = new Path(project);

    /**
     * @see Main#debug
     */
    public static boolean debug = false;

    /**
     * Gets the {@link ClassWorld} that governs this VM.
     */
    private static ClassWorld initWorld() {
        try {
            // if the harness is launched through bootstrap,
            // it will set the ClassWorld to this system property.
            ClassWorld w = (ClassWorld)System.getProperties().get("WORLD");
            if(w==null) {
                // otherwise assume it's launched without bootstrap
                // (such as when we debug the harness itself.)
                //
                // create a new classworld, but there's no point in putting
                // anything in the harness realm, as the harness classes
                // should be all available in the system classloader.
                w = new ClassWorld();
                w.newRealm("harness");
            }
            return w;
        } catch (DuplicateRealmException e) {
            throw new AssertionError(e);    // impossible
        }
    }

    private static ClassRealm initRealm(ClassRealm parent, String id) {
        try {
            if(parent==null)
                return world.getRealm(id);
            else
                return parent.createChildRealm(id);
        } catch (NoSuchRealmException e) {
            try {
                return world.newRealm(id);
            } catch (DuplicateRealmException x) {
                throw new AssertionError(x); // impossible
            }
        }
    }

    /**
     * Add contents to the {@link ClassRealm}.
     *
     * <p>
     * If {@link File} is a directory, this method checks if it's a jar directory.
     */
    public static void addConstituentRecursively( ClassRealm realm, File f ) throws IOException {
        if(!f.exists())
            throw new IllegalArgumentException("No such file: "+f);

        realm.addConstituent(f.toURL());
        logger.fine("Added "+f+" to "+realm.getId());

        if(f.isDirectory()) {
            // list up all jars in this directory
            for( File child : f.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return name.endsWith(".jar");
                }
            }) ) {
                addConstituentRecursively(realm,child);
            }
        }
    }

    private static final Logger logger = Logger.getLogger(World.class.getName());
}
