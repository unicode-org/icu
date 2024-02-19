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
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.ibm.icu.dev.meta.IcuInfo;
import com.ibm.icu.dev.meta.XMLUtil;
import com.ibm.icu.util.VersionInfo;

public abstract class CapScan {

    /** Capabilities string **/
    public static final String FORMATTING = "formatting";
    public static final String COLLATION = "collation";

    /**
     * @param args
     */
    public static void main(String[] args) throws IOException, ParserConfigurationException {
     
        System.err.println("# "+CapScan.class.getSimpleName()+": This is just the base class. Use a subclass.");
        System.exit(1);
    }

    protected String prog;

    protected Element addCapability(Document out, Element capabilities, String name) {
        return addCapability(out, capabilities, name, (String)null);
    }
    protected Element addCapability(Document out, Element capabilities, String name,
            String version) {
    
        Element e = out.createElement(IcuInfo.FEATURE);
        e.setAttribute(IcuInfo.TYPE,name.trim());
        if(version!=null) {
            e.setAttribute(IcuInfo.VERSION,version.trim());
        }
        capabilities.appendChild(e);
        return e;
    }

    protected Element addCapability(Document out, Element capabilities, String name,
            VersionInfo version) {
        return addCapability(out, capabilities, name, IcuInfo.versionInfoToShortString(version));
    }

    protected CapScan(String prog) {
        this.prog = prog;
    }
    
    Document out;
    
    protected Element createProduct(String productName) {
        Element base = out.createElement(IcuInfo.ICU_INFO);
        out.appendChild(base);
        
        Element products = out.createElement(IcuInfo.ICU_PRODUCTS);
        base.appendChild(products);
        
        Element product = out.createElement(IcuInfo.ICU_PRODUCT);
        product.setAttribute(IcuInfo.TYPE, productName);
        products.appendChild(product);
        return product;
    }
    
    protected Element createRelease(Element product, String version) {
        Element releases = out.createElement(IcuInfo.RELEASES);
        product.appendChild(releases);
        
        Element release = out.createElement(IcuInfo.RELEASE);
        release.setAttribute(IcuInfo.VERSION, version);
        releases.appendChild(release);
        return release;
    }
    
    protected void runMain(String args[]) throws IOException, ParserConfigurationException {
        
        
     System.err.println("# "+prog+": startup.");
    // TODO Auto-generated method stub
    out = XMLUtil.getBuilder().newDocument();
    
    Element product = createProduct(getProduct());
    
    Element release = createRelease(product, getRelease());
    
    
    Element capabilities = out.createElement(IcuInfo.CAPABILITIES);
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
     LDMLUtilities.printDOMTree(out, new PrintWriter(writer),copy+"\n<!DOCTYPE icuInfo SYSTEM \"http://icu-project.org/dtd/icumeta.dtd\">\n",null); //
     writer.flush();
    }
    protected abstract String getProduct();
    
    protected abstract String getRelease();
    
    protected abstract void addCapabilitiesTo(Element capabilities);
}
