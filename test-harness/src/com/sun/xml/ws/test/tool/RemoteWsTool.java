package com.sun.xml.ws.test.tool;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Launches {@code wsimport} as a separate process.
 *
 * @author Kohsuke Kawaguchi
 */
final class RemoteWsTool extends WsTool {

    /**
     * The path to the executable tool.bat or wsimport.sh
     */
    private final File executable;

    public RemoteWsTool(File executable) {
        this.executable = executable;
        if(!executable.exists())
            throw new IllegalArgumentException("Non-existent executable "+executable);
    }

    public void invoke(String... args) throws Exception {
        List<String> params = new ArrayList<String>();
        params.add(executable.getPath());
        params.addAll(Arrays.asList(args));

        ProcessBuilder b = new ProcessBuilder(params);
        b.redirectErrorStream(true);
        Process proc = b.start();

        // copy the stream and wait for the process completion
        proc.getOutputStream().close();
        byte[] buf = new byte[8192];
        InputStream in = proc.getInputStream();
        int len;
        while((len=in.read(buf))>=0) {
            System.out.write(buf,0,len);
        }

        int exit = proc.waitFor();
        assertEquals(
            "wsimport reported exit code "+exit,
            0,exit);
    }
}
