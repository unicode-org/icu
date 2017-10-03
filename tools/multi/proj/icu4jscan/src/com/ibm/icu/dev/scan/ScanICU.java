// © 2017 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html
/* Copyright (C) 2008-2012 IBM Corporation and Others. All Rights Reserved. */

/**
 * 
 */
package com.ibm.icu.dev.scan;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.Date;

import javax.xml.parsers.ParserConfigurationException;

import org.unicode.cldr.util.LDMLUtilities;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.ibm.icu.dev.meta.IcuInfo;
import com.ibm.icu.dev.meta.XMLUtil;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;
import com.ibm.icu.util.VersionInfo;

/**
 * @author srl
 *
 */
public class ScanICU extends CapScan{

    protected ScanICU() {
        super(ScanICU.class.getName());
    }

    /**
     * @param args
     * @throws ParserConfigurationException 
     * @throws IOException 
     * @throws IOException 
     * @throws ParserConfigurationException 
     * @throws IOException 
     */
    public static void main(String[] args) throws IOException, ParserConfigurationException  {
        new ScanICU().runMain(args);
    }
    
    protected String getProduct() {
        return "icu4j";
    }

    protected String getRelease() {
        /*
         *     try {
    } catch(NoClassDefFoundError ncdf) {
        System.err.println(prog+": Can't get ICU version: " + ncdf.toString());
        System.exit(1);
    } catch(NoSuchFieldError nsme) {
        System.err.println(prog+": Can't get ICU version: " + nsme.toString());
        System.exit(1);
    }
         */
        return IcuInfo.versionInfoToShortString(VersionInfo.ICU_VERSION);
    }

    protected void addCapabilitiesTo(Element capabilities) {
        
        addCapability(out, capabilities, "unicode", UCharacter.getUnicodeVersion());
        try {
            addCapability(out, capabilities, "uca", Collator.getInstance().getUCAVersion());
        } catch(NoSuchMethodError nsme) {
            System.err.println(prog+": Note: "+nsme.toString());
        }
        try {
            addCapability(out, capabilities, "tz", TimeZone.getTZDataVersion());
        } catch(NoSuchMethodError nsme) {
            System.err.println(prog+": Note: "+nsme.toString());
        }
        addFormatting(capabilities);
        addCollation(capabilities);
    }
    
    void addFormatting(Element capabilities) {
            Element locs = addCapability(out, capabilities, CapScan.FORMATTING);
            
            try {
            ULocale available[] = ULocale.getAvailableLocales();
            locs.setAttribute("total", new Integer(available.length).toString());
            
                String locinfo = "";
                
                Spinner sp = new Spinner(locs.getNodeName(),available.length);
                for(int i=0;i<available.length;i++) {
                	ULocale loc = available[i];
                    if(loc.toString().equals("root")) continue;
                    try {                    	
                        DateFormat fmt = null;
                        try {
                            fmt = DateFormat.getTimeInstance(DateFormat.FULL, loc);
                        }catch (Throwable t) {
                        	fmt = DateFormat.getTimeInstance(DateFormat.FULL, loc.toLocale());
                        }
                        fmt.format(new Date(1234));
                        locinfo = locinfo + " " + loc.toString();
                        sp.spin(loc.getBaseName());
                    } catch (Throwable t) {
                        System.err.println(prog+": loc fail: " + loc + " - " + t.toString());
                    }
                }
                
                Node tt = out.createTextNode(locinfo.trim());
                locs.appendChild(tt);
            } catch(NoClassDefFoundError ncdf) {
                System.err.println(prog+": locales: " + ncdf.toString());
            } catch(NoSuchMethodError ncdf) {
                System.err.println(prog+": locales: " + ncdf.toString());
            }
        }    
    void addCollation(Element capabilities) {
        {
            Element locs = addCapability(out, capabilities, CapScan.COLLATION);
            
            try {
                ULocale available[] = Collator.getAvailableULocales();
                Spinner sp = new Spinner(locs.getNodeName(),available.length);
                locs.setAttribute("total", new Integer(available.length).toString());
            
                String locinfo = "";
                
                for(int i=0;i<available.length;i++) {
                	ULocale loc = available[i];
                    if(loc.toString().equals("root")) continue;
                    try {
                      //  Collator.getInstance(loc);
                        locinfo = locinfo + " " + loc.toString();
                        sp.spin(loc.getBaseName());
                    } catch (Throwable t) {
                        System.err.println("loc fail: " + loc + " - " + t.toString());
                    }
                }
                
                Node tt = out.createTextNode(locinfo.trim());
                locs.appendChild(tt);
            } catch(NoClassDefFoundError ncdf) {
                System.err.println("locales: " + ncdf.toString());
            } catch(NoSuchMethodError ncdf) {
                System.err.println("locales: " + ncdf.toString());
            }
        }
    }
}
