/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/unicodetools/com/ibm/text/UCD/IANANames.java,v $
* $Date: 2002/08/08 15:38:16 $
* $Revision: 1.1 $
*
*******************************************************************************
*/

package com.ibm.text.UCD;

import com.ibm.text.utility.*;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.UnicodeSetIterator;
import com.ibm.icu.lang.UCharacter;


import java.util.*;
import java.text.NumberFormat;
import java.io.*;

public class IANANames implements UCD_Types {
    private Map aliasToBase = new TreeMap();
    private Map aliasToComment = new TreeMap();
    private Map aliasToLine = new TreeMap();
    
    public static void testSensitivity() throws IOException {
        IANANames iNames = new IANANames();
        Map m = new HashMap();
        Iterator it = iNames.getIterator();
        UnicodeSet removed = new UnicodeSet();
        int maxLength = 0;
        while (it.hasNext()) {
            String alias = (String) it.next();
            if (maxLength < alias.length()) maxLength = alias.length();
            if (alias.length() > 40) System.out.println("Name >40: " + alias);
            if (alias.indexOf(')') >= 0 || alias.indexOf('(') >= 0) System.out.println("Illegal tag: " + alias);
            String skeleton = removeNonAlphanumeric(alias, removed);
            String other = (String) m.get(skeleton);
            if (other != null) {
                String base = iNames.getBase(alias);
                String otherBase = iNames.getBase(other);
                if (!base.equals(otherBase)) {
                    System.out.println("Collision between: " + alias + " (" + base + ") and " 
                        + other + " (" + otherBase + ")");
                } else {
                    System.out.println("Alias Variant: " + alias + " and " + other + " (" + base + ")");
                }
            } else {
                m.put(skeleton, alias);
            }
        }
        System.out.println("Max Length: " + maxLength);
        
        System.out.println("Characters removed: ");
        UnicodeSetIterator usi = new UnicodeSetIterator(removed);
        while (usi.next()) {
            char c = (char) usi.codepoint; // safe, can't be supplementary
            System.out.println("0x" + usi.codepoint + "\t'" + c + "'\t" + UCharacter.getName(usi.codepoint));
        }
    }
    
    public IANANames() throws IOException {
        BufferedReader in = Utility.openReadFile(BASE_DIR + "IANA\\character-sets.txt", false);
        try {
            boolean atStart = true;
            String lastName = "";
            int counter = 0;
            while (true) {
                String line = in.readLine();
                if (line == null) break;
                counter++;
                if (atStart) {
                    if (line.startsWith("-------------")) atStart = false;
                    continue;
                }
                if (line.trim().length() == 0) continue;
                
                if (line.startsWith("Name:") || line.startsWith("Alias:")) {
                    lastName = add(line, lastName, counter);
                } else if (line.startsWith("Source:") || line.startsWith("MIBenum:") 
                        || line.startsWith("        ")) {
                    continue;
                } else if (line.equals("REFERENCES")) {
                    break;
                } else {
                    System.out.println("Unknown Line: " + line);
                }
            }
        } finally {
            in.close();
        }
    }
    
    private String add(String line, String baseName, int counter) {
        // extract the alias, doing a little validity check
        int pos = line.indexOf(": ");
        if (pos < 0) throw new IllegalArgumentException("Bad line: " + counter + " '" + line + "'");
        String alias = line.substring(pos+2).trim();
        
        // get comment
        String comment = null;
        pos = alias.indexOf(' ');
        if (pos >= 0) {
            comment = alias.substring(pos).trim();
            alias = alias.substring(0, pos);
        }
        
        // reset the baseName if we are a name
        if (line.startsWith("Name:")) {
            baseName = alias;
        }
        
        // store
        if (!alias.equals("None")) {
            if (false) {
                if (baseName.equals(alias)) System.out.println();
                System.out.println("Adding " + alias + "\t=> " + baseName + (comment != null ? "\t(" + comment + ")" : ""));
            }
            // check if it is stored already
            String oldbaseName = (String) aliasToBase.get(alias);
            if (oldbaseName != null) {
                System.out.println("Duplicate alias (" + alias + ", " + oldbaseName + ", " + baseName + "): "
                    + counter + " '" + line + "'");
            }
            aliasToBase.put(alias, baseName);
            if (comment != null) aliasToComment.put(alias, comment);
            aliasToLine.put(alias, comment);
        }
        return baseName;
    }
    
    public Iterator getIterator() {
        return aliasToBase.keySet().iterator();
    }
    
    /**
     * Returns the name for this alias, or "" if there is none
     */
    public String getBase(String alias) {
        return (String) aliasToBase.get(alias);
    }
    
    public static String removeNonAlphanumeric(String s, UnicodeSet removed) {
        s = s.toUpperCase(Locale.ENGLISH); // can't have Turkish!
        StringBuffer result = new StringBuffer();
        boolean removedZero = false;
        for (int i = 0; i < s.length(); ++i) {
            char c = s.charAt(i);
            if (c == '0') {
                char cLast = result.length() > 0 ? result.charAt(result.length() - 1) : '0';
                if ('0' <= cLast && cLast <= '9') {
                    result.append(c);
                } else {
                    if (!removed.contains(c)) {
                        System.out.println("Removed '" + c + "' from " + s + " => " + result);
                        removed.add(c);
                    }
                    removedZero = true;
                }
            } else if (('A' <= c && c <= 'Z') || ('0' <= c && c <= '9')) {
                result.append(c);
            } else {
                if (!removed.contains(c)) {
                    System.out.println("Removed '" + c + "' from " + s + " => " + result);
                    removed.add(c);
                }
            }
        }
        //if (removedZero) System.out.println("Removed 0 from " + s + " => " + result);
        return result.toString();
    }
}