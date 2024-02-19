// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/* Copyright (C) 2008-2012 IBM Corporation and Others. All Rights Reserved. */

package com.ibm.icu.dev.scan;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Calendar;

import javax.xml.parsers.ParserConfigurationException;

import org.unicode.cldr.util.LDMLUtilities;

import com.ibm.icu.dev.meta.IcuInfo;
import com.ibm.icu.dev.meta.XMLUtil;
import com.ibm.icu.util.VersionInfo;

public abstract class SimpleScan {

    /** Capabilities string **/
    public static final String FORMATTING = "formatting";
    public static final String COLLATION = "collation";

    /**
     * @param args
     */
    public static void main(String[] args) throws IOException, ParserConfigurationException {
     
        System.err.println("# "+SimpleScan.class.getSimpleName()+": This is just the base class. Use a subclass.");
        System.exit(1);
    }

    protected String prog;

    protected CapElement addCapability(CapDocument out, CapElement capabilities, String name) {
        return addCapability(out, capabilities, name, (String)null);
    }
    protected CapElement addCapability(CapDocument out, CapElement capabilities, String name,
            String version) {
    
        CapElement e = out.createCapElement(IcuInfo.FEATURE);
        e.setAttribute(IcuInfo.TYPE,name.trim());
        if(version!=null) {
            e.setAttribute(IcuInfo.VERSION,version.trim());
        }
        capabilities.appendChild(e);
        return e;
    }

    protected CapElement addCapability(CapDocument out, CapElement capabilities, String name,
            VersionInfo version) {
        return addCapability(out, capabilities, name, IcuInfo.versionInfoToShortString(version));
    }

    protected SimpleScan(String prog) {
        this.prog = prog;
    }
    
    CapDocument out;
    
    protected CapElement createProduct(String productName) {
        CapElement base = out.createCapElement(IcuInfo.ICU_INFO);
        out.appendChild(base);
        
        CapElement products = out.createCapElement(IcuInfo.ICU_PRODUCTS);
        base.appendChild(products);
        
        CapElement product = out.createCapElement(IcuInfo.ICU_PRODUCT);
        product.setAttribute(IcuInfo.TYPE, productName);
        products.appendChild(product);
        return product;
    }
    
    protected CapElement createRelease(CapElement product, String version) {
        CapElement releases = out.createCapElement(IcuInfo.RELEASES);
        product.appendChild(releases);
        
        CapElement release = out.createCapElement(IcuInfo.RELEASE);
        release.setAttribute(IcuInfo.VERSION, version);
        releases.appendChild(release);
        return release;
    }
    
    protected void runMain(String args[]) throws IOException {
        
        
     System.err.println("# "+prog+": startup.");
    out = CapDocument.newCapDocument();
    
    CapElement product = createProduct(getProduct());
    
    CapElement release = createRelease(product, getRelease());
    
    
    CapElement capabilities = out.createCapElement(IcuInfo.CAPABILITIES);
    release.appendChild(capabilities);
    
    addCapabilitiesTo(capabilities);


    // write out
    OutputStream outstr = null;
    outstr = System.out;
//    java.io.FileOutputStream fos = null;
//    if(outfile!=null) {
//        fos = new FileOutputStream(outfile);
//        out = fos;
//        if(verbose) System.err.println("# Write <"+outfile+">");
//    } else {
//        out = System.out;
//        if(verbose) System.err.println("# Write <stdout>");
//    }
//    try {
     java.io.OutputStreamWriter writer = new java.io.OutputStreamWriter(
             outstr);
     String copy = "";
     if(true) copy = ("<!-- Copyright (c) "+Calendar.getInstance().get(Calendar.YEAR)+" IBM Corporation and Others, All Rights Reserved. -->\n");
     CapDocument.printDOMTree(out, new PrintWriter(writer),copy+"\n<!DOCTYPE icuInfo SYSTEM \"http://icu-project.org/dtd/icumeta.dtd\">\n",null); //
     writer.flush();
    }
    protected abstract String getProduct();
    
    protected abstract String getRelease();
    
    protected abstract void addCapabilitiesTo(CapElement capabilities);
}
