package com.sun.xml.ws.test.util;

import com.sun.xml.ws.test.tool.WsTool;
import org.apache.tools.ant.types.Path;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Provide convenient methods for building up command-line arguments.
 *
 * <p>
 * This class can be used in a chained-invocation style like {@link StringBuilder}.
 *
 * @author Kohsuke Kawaguchi
 */
public final class ArgumentListBuilder {
    private final List<String> args = new ArrayList<String>();

    public ArgumentListBuilder add(String token) {
        args.add(token);
        return this;
    }

    /**
     * Adds a file path as an argument.
     */
    public ArgumentListBuilder add(File path) {
        return add(path.getAbsoluteFile().getPath());
    }

    public ArgumentListBuilder add(URL path) {
        return add(path.toExternalForm());
    }

    /**
     * Invokes the tool with arguments built so far.
     */
    public void invoke(WsTool tool) throws Exception {
        tool.invoke(args.toArray(new String[args.size()]));
    }

    public ArgumentListBuilder add(Path cp) {
        return add(cp.toString());
    }
}
