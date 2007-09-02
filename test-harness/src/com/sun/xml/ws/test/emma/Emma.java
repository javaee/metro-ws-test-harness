/* Copyright (C) 2003 Vladimir Roubtsov. All rights reserved.
 *
 * This program and the accompanying materials are made available under
 * the terms of the Common Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/cpl-v10.html
 *
 * $Id: Emma.java,v 1.2 2007-09-02 18:04:30 kohsuke Exp $
 */
package com.sun.xml.ws.test.emma;

import com.vladium.emma.EMMAProperties;
import com.vladium.emma.data.CoverageOptionsFactory;
import com.vladium.emma.data.DataFactory;
import com.vladium.emma.data.ICoverageData;
import com.vladium.emma.data.IMetaData;
import com.vladium.emma.data.SessionData;
import com.vladium.emma.filter.IInclExclFilter;
import com.vladium.emma.rt.InstrClassLoadHook;
import com.vladium.emma.rt.RT;
import com.vladium.emma.rt.RTSettings;
import com.vladium.util.IProperties;
import org.apache.tools.ant.loader.AntClassLoader2;

import java.io.File;
import java.io.IOException;

/**
 * Hides details of the emma and provide convenient API for the rest of the harness.
 *
 * @author Kohsuke Kawaguchi
 */
public class Emma {
    private final IMetaData metadata;
    private final IInclExclFilter filter;
    private ICoverageData coverage;

    public Emma() {
        this(new IInclExclFilter() {
            public boolean included(String s) {
                return true;
            }
        });
    }

    public Emma(IInclExclFilter filter) {
        // we'll take care of record dump
        RTSettings.setStandaloneMode(false);
        RT.reset(true,false);


        // KK : not sure exactly what IProperties are abstracting,
        // but looks like they are used to allow emma configurations
        // to be given from different sources
        
        IProperties appProperties = RT.getAppProperties();
        if (appProperties == null) appProperties = EMMAProperties.getAppProperties();
        metadata = DataFactory.newMetaData(CoverageOptionsFactory.create(appProperties));

        coverage = RT.getCoverageData();

        this.filter = filter;
    }

    public AntClassLoader2 createInstrumentingClassLoader() {
        return new InstrumentingClassLoader(new InstrClassLoadHook(filter, metadata));
    }

    /**
     * Writes the coverate report as a data file.
     */
    public void write(File output) throws IOException {
        IMetaData msnap = metadata.shallowCopy();
        if (msnap.isEmpty ()) {
            System.err.println("no metadata collected at runtime [no reports generated]");
            return;
        }

        ICoverageData csnap = coverage.shallowCopy();
        if (csnap.isEmpty ()) {
            System.err.println("no coverage data collected at runtime [all reports will be empty]");
            return;
        }

        System.out.println("Writing emma coverage report");
        DataFactory.persist(new SessionData(msnap, csnap), output, false);
    }
}
