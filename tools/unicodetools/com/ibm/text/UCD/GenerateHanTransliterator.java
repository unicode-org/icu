/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/unicodetools/com/ibm/text/UCD/GenerateHanTransliterator.java,v $
* $Date: 2002/03/15 01:57:01 $
* $Revision: 1.3 $
*
*******************************************************************************
*/

package com.ibm.text.UCD;
import java.io.*;
import com.ibm.text.utility.*;
import com.ibm.icu.text.UTF16;
import java.util.*;


public final class GenerateHanTransliterator {
    
    static final boolean TESTING = false;
    static int type;
    
    public static void main() {
        try {
            type = 0;
            System.out.println("Starting");
            generate();
        } catch (Exception e) {
            System.out.println("Exception: " + e);
        }
    }
    
    static PrintWriter out;
    static PrintWriter err;
    
    static int count;
    static int oldLine;
  
    static void generate() throws java.io.IOException {
        String name = "$Han$English";
        String key = "kDefinition"; // kMandarin, kKorean, kJapaneseKun, kJapaneseOn
        String filter = "kJis0";
        switch (type) {
            default: break;
            case 1: name = "$Han$OnRomaji";
                key = "kJapaneseOn";
                filter = "kJis0";
                break;
            case 2: name = "$Han$Pinyin";
                key = "kMandarin";
                filter = null;
                break;
        }
        
        out = Utility.openPrintWriter("Transliterate_Han_English.txt");
        err = Utility.openPrintWriter("Transliterate_Han_English.log.txt");
        
        BufferedReader in = Utility.openUnicodeFile("Unihan", "3.2.0", true); 

        int count = 0;
        String oldCode = "";
        String oldLine = "";
        int oldStart = 0;
        boolean foundFilter = (filter == null);
        boolean foundKey = false;
        
        int lineCounter = 0;
        
        while (true) {
            Utility.dot(++lineCounter);
            
            String line = in.readLine();
            if (line == null) break;
            if (line.length() < 6) continue;
            if (line.charAt(0) == '#') continue;
            String code = line.substring(2,6);
            /* if (code.compareTo("9FA0") >= 0) {
                System.out.println("? " + line);
            }*/
            if (!code.equals(oldCode)) {
                if (foundKey && foundFilter) {
                    count++;
                    /*if (true) { //*/
                    if (count == 1 || (count % 100) == 0) {
                        System.out.println(count + ": " + oldLine);
                    }
                    printDef(out, oldCode, oldLine, oldStart);
                }
                if (TESTING) if (count > 1000) break;
                oldCode = code;
                foundKey = false;
                foundFilter = (filter == null);
            }
            
            // detect key, filter. Must be on different lines
            if (!foundFilter && line.indexOf(filter) >= 0) {
                foundFilter = true;
            } else if (!foundKey && (oldStart = line.indexOf(key)) >= 0) {
                foundKey = true;
                oldLine = line;
                oldStart += key.length();
            }
        }
        if (foundKey && foundFilter) printDef(out, oldCode, oldLine, oldStart);
        
        in.close();
        out.close();
        err.close();
    }
    
    static void printDef(PrintWriter out, String code, String line, int start) {
        if (code.length() == 0) return;
        
        // skip spaces & numbers at start
        for (;start < line.length(); ++start) {
            char ch = line.charAt(start);
            if (ch != ' ' && ch != '\t' && (ch < '0' || ch > '9')) break;
        }

        // go up to comma or semicolon, whichever is earlier
        int end = line.indexOf(";", start);
        if (end < 0) end = line.length();
        
        int end2 = line.indexOf(",", start);
        if (end2 < 0) end2 = line.length();
        if (end > end2) end = end2;
  
        if (type != 0) {
            end2 = line.indexOf(" ", start);
            if (end2 < 0) end2 = line.length();
            if (end > end2) end = end2;
        }
        
        String definition = line.substring(start,end);
        if (type == 2) definition = handlePinyin(definition, line);
        definition.trim();
        String cp = UTF16.valueOf(Integer.parseInt(code, 16));
        String key = (String) definitionMap.get(definition);
        if (key == null) {
            definitionMap.put(definition, cp);
        }
        out.println(cp + (key == null ? " <> " : " > ") + "'[" + definition + "]';");
        if (TESTING) System.out.println("# " + code + " > " + definition);
    }
    
    static Map definitionMap = new HashMap();
    
    static StringBuffer handlePinyinTemp = new StringBuffer();
    
    static String handlePinyin(String source, String debugLine) {
        try {
            char ch = source.charAt(source.length()-1);
            int num = (int)(ch-'1');
            if (num < 0 || num > 5) throw new Exception("none");
            handlePinyinTemp.setLength(0);
            boolean gotIt = false;
            boolean messageIfNoGotIt = true;
            for (int i = source.length()-2; i >= 0; --i) {
                ch = source.charAt(i);
                if (!gotIt) switch (ch) {
                    case 'A': ch = "AÁ\u0102À\u0100".charAt(num); gotIt = true; break;
                    case 'E': ch = "EÉ\u0114È\u0112".charAt(num); gotIt = true; break;
                    case 'I': ch = "IÍ\u012CÌ\u012A".charAt(num); gotIt = true; break;
                    case 'O': ch = "OÓ\u014EÒ\u014C".charAt(num); gotIt = true; break;
                    case 'U': ch = "UÚ\u016CÙ\u016A".charAt(num); gotIt = true; break;
                    case 'Ü': ch = "Ü\u01D7\u01D9\u01DB\u01D5".charAt(num); gotIt = true; break;
                }
                handlePinyinTemp.insert(0,ch);
            }
            if (!gotIt && num > 0) {
                handlePinyinTemp.append(" \u0301\u0306\u0300\u0304".charAt(num));
                if (messageIfNoGotIt) {
                    err.println("Missing vowel?: " + debugLine + " -> " + handlePinyinTemp
                    .toString());
                }
            }
            source = handlePinyinTemp.toString().toLowerCase();
        } catch (Exception e) {
            err.println("Bad line: " + debugLine);
        }
        return source;
    }
}