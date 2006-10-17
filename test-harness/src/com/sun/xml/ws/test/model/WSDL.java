package com.sun.xml.ws.test.model;

import com.sun.istack.Nullable;

import java.io.File;
import java.util.List;

/**
 * @author Bhakti Mehta
 */
public class WSDL {

    //Optional WSDL file that describes this service.
    @Nullable
    public final File wsdlFile;

    //Optional imported wsdls that are imported by the primary wsdl (wsdlFile)
    @Nullable
    public final List<File> importedWsdls;

    //Optional schema files that are imported by the wsdl
    @Nullable
    public final List<File> schemas;


    public WSDL(File wsdlFile ,List<File> importedWsdls,List<File> schemafiles) {
        this.wsdlFile = wsdlFile;
        this.importedWsdls = importedWsdls;
        this.schemas = schemafiles;
    }

}
