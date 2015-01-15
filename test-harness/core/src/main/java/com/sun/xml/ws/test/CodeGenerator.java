/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2015 Sun Microsystems, Inc. All rights reserved.
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

package com.sun.xml.ws.test;

import com.sun.xml.ws.test.util.FreeMarkerTemplate;
import com.sun.xml.ws.test.util.JavacTask;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CodeGenerator {

    public static int scriptOrder = 0;
    public static String id;
    public static String workDir;
    static List<String> testcaseScripts = new ArrayList<String>();

    private static List<String> testcases = new ArrayList<String>();
    private static boolean generateTestSources;

    public static void setGenerateTestSources(boolean generateTestSources) {
        CodeGenerator.generateTestSources = generateTestSources;
    }

    public static void testDone() {
        if (!generateTestSources) return;
        scriptOrder = 0;

        FreeMarkerTemplate run = new FreeMarkerTemplate(id, scriptOrder, workDir, "run");
        run.put("scripts", testcaseScripts);
        String filename = run.writeFile();
        testcases.add(filename);

        testcaseScripts.clear();
    }

    public static void allTestsDone() {
        if (!generateTestSources) return;
        FreeMarkerTemplate runall = new FreeMarkerTemplate(id, 0, workDir, "runall");
        runall.put("testcases", testcases);
        runall.writeFile();
    }

    public static void generateDeploy(Map<String, Object> params, String classpath) {
        if (!generateTestSources) return;
        if (workDir == null) return;

        //obsoleteDeploy(filename, classpath);

        FreeMarkerTemplate deploy = new FreeMarkerTemplate(id, scriptOrder, workDir, "deploy");
        deploy.put("classpath", classpath);
        String filename = deploy.writeFile();
        testcaseScripts.add(filename);

        // create dir
        File dir = new File(workDir + "/bsh");
        dir.mkdir();

        //obsoleteDeployCLass(contents, className);

        FreeMarkerTemplate deployClass = new FreeMarkerTemplate(id, scriptOrder, workDir, "bsh/Deploy.java");
        for(String key : params.keySet()) {
            deployClass.put(key, params.get(key));
        }
        deployClass.writeFileTo(workDir + "/bsh/Deploy" + scriptOrder + ".java");

        scriptOrder++;
    }

    public static void generateClient(String classpath, String testName) {
        if (!generateTestSources) return;
        if (workDir == null) return;

        FreeMarkerTemplate client = new FreeMarkerTemplate(id, scriptOrder, workDir, "client");
        client.put("classpath", classpath);
        client.put("testName", testName);
        String filename = client.writeFile();
        testcaseScripts.add(filename);

        // create dir
        File dir = new File(workDir + "/bsh");
        dir.mkdir();

        scriptOrder++;
    }

    public static void log(File file, String ... contents) {
        if (!generateTestSources) return;
        Writer fwriter = null;
        try {
            file.setExecutable(true);
            fwriter = new FileWriter(file);

            for (String s : contents) {
                fwriter.write(s);
            }

            fwriter.flush();
            fwriter.close();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        } finally {
            if (fwriter != null) {
                try {
                    fwriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void startDumpService(String id, File workDir) {
        if (!generateTestSources) return;
        scriptOrder = 1;
        CodeGenerator.id = id;
        CodeGenerator.workDir = workDir.getAbsolutePath();
    }

    public static void generateJavac(JavacTask javac) {
        if (!generateTestSources) return;

        List<String> mkdirs = new ArrayList();
        mkdirs.add(javac.getDestdir().toString());

        List<String> params = new ArrayList();
        params.add("-d " + javac.getDestdir());
        params.add("-cp " + javac.getClasspath());

        for(String p : javac.getSrcdir().list()) {
            params.add("`find " + p + " -name '*.java'`");
            mkdirs.add(p);
        }

        FreeMarkerTemplate run = new FreeMarkerTemplate(id, scriptOrder, workDir, "javac");
        // TODO: how comes this is null?!
        run.put("mkdirs", mkdirs);
        run.put("params", params);
        String filename = run.writeFile();
        testcaseScripts.add(filename);
        scriptOrder++;
    }

    public static void generatedWsScript(List dirsToBeCretaed, List<String> params) {
        if (!generateTestSources) return;

        FreeMarkerTemplate template = new FreeMarkerTemplate(id, scriptOrder, workDir, "tool");
        template.put("dirs", dirsToBeCretaed);
        template.put("params", params);
        String filename = template.writeFile();
        testcaseScripts.add(filename);

        scriptOrder++;
    }

    public static void generateClientClass(
            String classpath,
            String testName,
            List<String> pImports,
            String pContents,
            Map<String, String> varMap) {

        if (!generateTestSources) return;

        // create dir
        File dir = new File(workDir + "/bsh");
        dir.mkdir();

        FreeMarkerTemplate clientClass = new FreeMarkerTemplate(id, scriptOrder, workDir, "bsh/Client.java");
        clientClass.put("classpath", classpath);
        clientClass.put("testName", testName);
        clientClass.put("pImports", pImports);
        clientClass.put("contents", pContents);
        for(String key : varMap.keySet()) {
            String value = varMap.get(key);
            clientClass.put(key, value);
        }
        clientClass.writeFileTo(workDir + "/bsh/Client" + scriptOrder + ".java");
        generateClient(classpath, testName);
    }

    public static boolean getGenerateTestSources() {
        return generateTestSources;
    }
}
