/*
 ******************************************************************************
 * Copyright (C) 2004, International Business Machines Corporation and        *
 * others. All Rights Reserved.                                               *
 ******************************************************************************
 * 
 * in shell:  (such as .cldrrc)
 *   export CWDEBUG="-DCLDR_DTD_CACHE=/tmp/cldrdtd/"
 *   export CWDEFS="-DCLDR_DTD_CACHE_DEBUG=y ${CWDEBUG}"
 *
 * 
 * in code:
 *   docBuilder.setEntityResolver(new CachingEntityResolver());
 * 
 */

package com.ibm.icu.dev.tool.cldr;

/**
 * @author srl
 * 
 * Caching entity resolver
 */
import java.io.*;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;

// Needed JAXP classes
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

// SAX2 imports
import org.xml.sax.ErrorHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
//DOM imports
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import com.ibm.icu.dev.tool.UOption;


public class CachingEntityResolver implements EntityResolver {
    static final String CLDR_DTD_CACHE = "CLDR_DTD_CACHE";
    private static String gCacheDir = null;
    private static boolean gCheckedEnv = false;
    
    // TODO: synch
    public static void setCacheDir(String s) {
        gCacheDir = s;
        if((gCacheDir==null)||(gCacheDir.length()<=0)) {
            gCacheDir = null;
        }
    }
    public static String getCacheDir() {
//        boolean aDebug = false;
//        if((System.getProperty("CLDR_DTD_CACHE_DEBUG")!=null) || "y".equals(System.getProperty("CLDR_DTD_CACHE_ADEBUG"))) {
//            aDebug = true;
//        }

        if((gCacheDir == null) && (!gCheckedEnv)) {
            gCacheDir = System.getProperty(CLDR_DTD_CACHE);

//            if(aDebug) {
//                System.out.println("CRE:  " + CLDR_DTD_CACHE + " = " + gCacheDir);
//            }
            
            if((gCacheDir==null)||(gCacheDir.length()<=0)) {
                gCacheDir = null;
            }
            gCheckedEnv=true;
        }
        return gCacheDir;
    }
    public InputSource resolveEntity (String publicId, String systemId) {
        boolean aDebug = false;
        if((System.getProperty("CLDR_DTD_CACHE_DEBUG")!=null) || "y".equals(System.getProperty("CLDR_DTD_CACHE_ADEBUG"))) {
            aDebug = true;
        }
        
        String theCache = getCacheDir();

        if(aDebug) {
            System.out.println("CRE:  " + publicId + " | " + systemId + ", cache=" + theCache);
        }
        
        if(theCache!=null) {
            int i;

            StringBuffer systemNew = new StringBuffer(systemId);
            if(systemId.startsWith("file:")) {
                return null;
            }
//          char c = systemNew.charAt(0);
//          if((c=='.')||(c=='/')) {
//              return null;
//          }

            for(i=0;i<systemNew.length();i++) {
                char c = systemNew.charAt(i);
                if(!Character.isLetterOrDigit(c) && (c!='.')) {
                    systemNew.setCharAt(i, '_');
                }
            }

            if(aDebug) {
                System.out.println(systemNew.toString());
            }

            File aDir = new File(theCache);
            if(!aDir.exists() || !aDir.isDirectory()) {
                // doesn't exist or isn't a directory:
                System.err.println("CachingEntityResolver: Warning:  Cache not used, Directory doesn't exist, Check the value of  property " + CLDR_DTD_CACHE + " :  " + theCache);
                return null;
            }
            
            String newName = new String(systemNew);
            
            File t = new File(theCache,newName);
            if(t.exists()) {
                if(aDebug) {
                    System.out.println("Using existing: " + t.getPath());
                }
            } else {
                if(aDebug) {
                    System.out.println(t.getPath() + " doesn't exist. fetching.");
                }
                try {
                    BufferedReader r = new BufferedReader(new InputStreamReader(new java.net.URL(systemId).openStream()));
                    BufferedWriter w = new BufferedWriter(new FileWriter(t.getPath()));
                    String s;
                    while((s=r.readLine())!=null) {
                        w.write(s);
                        w.newLine();
                    }
                    r.close();
                    w.close();
                } catch ( Throwable th ) {
                    System.err.println(th.toString() + " trying to fetch " + t.getPath());
                    return null;
                }
                if(aDebug) {
                    System.out.println(t.getPath() + " fetched.");
                }
            }
            
            return new InputSource(t.getPath());
        }
        return null; // unhelpful
    }
}