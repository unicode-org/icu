/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/unicodetools/com/ibm/text/UCD/ProcessUnihan.java,v $
* $Date: 2005/03/04 02:50:26 $
* $Revision: 1.3 $
*
*******************************************************************************
*/

package com.ibm.text.UCD;
import java.io.*;
import com.ibm.text.utility.*;
import com.ibm.icu.text.UTF16;
import java.util.*;

// stub file, ignore

public final class ProcessUnihan {
	/*
    
    static final boolean TESTING = false;
    static int type;
    
    public static void main() {
        try {
            type = 0;
            System.out.println("Starting");
            process();
        } catch (Exception e) {
            System.out.println("Exception: " + e);
        }
    }
    
    static PrintWriter out;
    static PrintWriter err;
    
    static int count;
    static int oldLine;
    
    static Map map = new HashMap();
    static Map tags = new HashMap();
  
    static void process() throws java.io.IOException {
        int lineCounter = 0;
        String[] parts = new String[3];
        
        //out = Utility.openPrintWriter("Transliterate_Han_English.txt");
        //err = Utility.openPrintWriter("Transliterate_Han_English.log.txt");
        
        BufferedReader in = Utility.openUnicodeFile("Unihan", "3.2.0", true, Utility.UTF8);
        while (true) {
            Utility.dot(++lineCounter);
            
            String line = in.readLine();
            if (line == null) break;
            int commentPos = line.indexOf('#');
            if (commentPos >= 0) line = line.substring(0,commentPos);
            line = line.trim();
            if (line.length() == 0) continue;
            int count = Utility.split(line, '#', parts);
            
            int code = Integer.parseInt(parts[0].substring(2), 16);
            Byte itag = (Byte) tags.get("a");
            if (itag == null) {}
            String tag = parts[1];
            String value = parts[2];
            if (tags.containsKey(tag)) {}
        }
    }
    */
}
            
