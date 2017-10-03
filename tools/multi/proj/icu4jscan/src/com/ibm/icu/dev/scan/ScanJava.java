// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/* Copyright (C) 2008-2012 IBM Corporation and Others. All Rights Reserved. */


package com.ibm.icu.dev.scan;

import java.io.IOException;
import java.text.Collator;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

import javax.xml.parsers.ParserConfigurationException;


import com.ibm.icu.util.ULocale;

/**
 * @author srl
 *
 */
public class ScanJava extends SimpleScan {

    /**
     * @param prog
     */
    public ScanJava() {
        super(ScanJava.class.getName());
    }

    /* (non-Javadoc)
     * @see com.ibm.icu.dev.scan.CapScan#addCapabilitiesTo(org.w3c.dom.CapElement)
     */
    protected void addCapabilitiesTo(CapElement capabilities) {
        addFormatting(capabilities);
        addCollation(capabilities);
    }

    private void addCollation(CapElement capabilities) {
        CapElement locs = addCapability(out, capabilities, CapScan.COLLATION);
        
        try {
        Locale available[] = Collator.getAvailableLocales();
        locs.setAttribute("total", new Integer(available.length).toString());
        System.err.println("Begin coll!");
            String locinfo = "";
            
            Spinner sp = new Spinner(locs.getNodeName(),available.length);
            for(int i=0;i<available.length;i++) {
            	Locale loc = available[i];
                if(loc.toString().equals("root")) continue;
                try {
                    Collator c = Collator.getInstance(loc);
                    DateFormat fmt = DateFormat.getTimeInstance(DateFormat.FULL, loc);
                    fmt.format(new Date(1234));
                    locinfo = locinfo + " " + loc.toString();
                    sp.spin(loc.toString());
                } catch (Throwable t) {
                    System.err.println(prog+": loc fail: " + loc + " - " + t.toString());
                }
            }
            
            CapNode tt = out.createTextNode(locinfo.trim());
            locs.appendChild(tt);
        } catch(NoClassDefFoundError ncdf) {
            System.err.println(prog+": collation: " + ncdf.toString());
        } catch(NoSuchMethodError ncdf) {
            System.err.println(prog+": collation: " + ncdf.toString());
        }
        
        locs.appendChild(out.createComment("Note: Java locales have different conventions than ICU."));
        
    }

    private void addFormatting(CapElement capabilities) {
        CapElement locs = addCapability(out, capabilities, CapScan.FORMATTING);
        
        try {
        Locale available[] = Locale.getAvailableLocales();
        locs.setAttribute("total", new Integer(available.length).toString());
        
            String locinfo = "";
            
            Spinner sp = new Spinner(locs.getNodeName(),available.length);
            for(int i=0;i<available.length;i++) {
            	Locale loc = available[i];
                if(loc.toString().equals("root")) continue;
                try {
                    DateFormat fmt = DateFormat.getTimeInstance(DateFormat.FULL, loc);
                    fmt.format(new Date(1234));
                    locinfo = locinfo + " " + loc.toString();
                    sp.spin(loc.toString());
                } catch (Throwable t) {
                    System.err.println(prog+": loc fail: " + loc + " - " + t.toString());
                }
            }
            
            CapNode tt = out.createTextNode(locinfo.trim());
            locs.appendChild(tt);
        } catch(NoClassDefFoundError ncdf) {
            System.err.println(prog+": locales: " + ncdf.toString());
        } catch(NoSuchMethodError ncdf) {
            System.err.println(prog+": locales: " + ncdf.toString());
        }
        
        locs.appendChild(out.createComment("Note: Java locales have different conventions than ICU."));
        
    }

    /* (non-Javadoc)
     * @see com.ibm.icu.dev.scan.CapScan#getProduct()
     */
    protected String getProduct() {
        return "java";
    }

    /* (non-Javadoc)
     * @see com.ibm.icu.dev.scan.CapScan#getRelease()
     */
    protected String getRelease() {
        String ver =  System.getProperty("java.version");
        int u = ver.indexOf('_');
        if(u>0) {
        	ver = ver.substring(0,u);
        }
        return ver;
    }

    /**
     * @param args
     * @throws ParserConfigurationException 
     * @throws IOException 
     */
    public static void main(String[] args) throws IOException {
        new ScanJava().runMain(args);
    }

}
