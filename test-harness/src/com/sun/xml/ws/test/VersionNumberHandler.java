package com.sun.xml.ws.test;

import com.sun.istack.test.VersionNumber;
import org.kohsuke.args4j.spi.OptionHandler;
import org.kohsuke.args4j.spi.Parameters;
import org.kohsuke.args4j.spi.Setter;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

/**
 * {@link OptionHandler} to process version number.
 * 
 * @author Kohsuke Kawaguchi
 */
public class VersionNumberHandler extends OptionHandler<VersionNumber> {
    public VersionNumberHandler(CmdLineParser parser, Option option, Setter<? super VersionNumber> setter) {
        super(parser, option, setter);
    }

    public int parseArguments(Parameters params) throws CmdLineException {
        setter.addValue(new VersionNumber(params.getParameter(0)));
        return 1;
    }

    public String getDefaultMetaVariable() {
        return "VERSION";
    }
}
