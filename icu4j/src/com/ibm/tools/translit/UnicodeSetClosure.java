/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/tools/translit/Attic/UnicodeSetClosure.java,v $
 * $Date: 2001/11/29 23:21:42 $
 * $Revision: 1.1 $
 *
 *****************************************************************************************
 */
package com.ibm.tools.translit;
import com.ibm.text.*;
import com.ibm.test.*;
import com.ibm.util.Utility;
//import java.text.*;
import java.io.*;
import java.util.Locale;

public class UnicodeSetClosure {
    public static void main(String[] args) throws Exception {
        // test it
        String testStr = "[\u00E0Bc]";
        
        File f = new File("TestUnicodeSetClosure.txt");
        String filename = f.getCanonicalFile().toString();
        out = new PrintWriter(
            new OutputStreamWriter(
                new FileOutputStream(filename), "UTF-8"));
        System.out.println("Writing " + filename);
        out.print('\uFEFF'); // BOM
        
        UnicodeSet test = new UnicodeSet(testStr);
        
        close(test, Normalizer.DECOMP, false);
        print("NFD", test);
        
        test = new UnicodeSet(testStr);
        close(test, Normalizer.NO_OP, true);
        print("Lower", test);

        test = new UnicodeSet(testStr);
        close(test, Normalizer.COMPOSE, false);
        print("NFC", test);

        test = new UnicodeSet(testStr);
        close(test, Normalizer.DECOMP_COMPAT, false);
        print("NFKD", test);

        test = new UnicodeSet(testStr);
        close(test, Normalizer.COMPOSE_COMPAT, false);
        print("NFKC", test);
        
        out.close();
    }
    
    static PrintWriter out;
    
    public static void print(String label, UnicodeSet test) {
        System.out.println(label);
        out.println(label + ": " + test.toPattern(false));
        out.println();
    }
    
    public static void close(UnicodeSet s, Normalizer.Mode m, boolean lowerFirst) {
        close(s, new NFToString(m, lowerFirst));
    }
    
    // dumb, slow implementations
    public static class NFToString implements Char32ToString {
        Normalizer.Mode mode;
        boolean lowerFirst;
        
        NFToString(Normalizer.Mode m, boolean lowerFirst) {
            mode = m;
            this.lowerFirst = lowerFirst;
        }
        
        public String get(int cp) {
            String source = UTF16.valueOf(cp);
            String result = source;
            if (lowerFirst) result = UCharacter.toLowerCase(Locale.US, result);
            result = Normalizer.normalize(result, mode, 0);
            if (lowerFirst) result = UCharacter.toLowerCase(Locale.US, result);
            if (result.equals(source)) return null;
            return result;
        }
    }
        
    
    /** Returns a mapping from char32 to a string. If there is no change,
     * null is returned.
     */
     
    interface Char32ToString {
        public String get(int cp);
    }
    
    public static void close(UnicodeSet s, Char32ToString f) {
        for (int cp = 0; cp <= 0x10FFFF; ++cp) {
            int type = UCharacter.getType(cp);
            if (type == Character.UNASSIGNED) continue;
            
            if (cp == '\u00e7') {
                System.out.println("debug");
            }
            String result = f.get(cp);
            if (result == null) continue;
            if (!containsSome(s, result)) continue;
            s.add(cp);
        }
    }
    
    // These should both be public, and on the respective classes
    
    public static void addAll(UnicodeSet s, String str) {
        int cp;
        for (int i = 0; i < str.length(); i += UTF16.getCharCount(cp)) {
            cp = UTF16.charAt(str,i);
            s.add(cp);
        }
    }
    
    public static boolean containsSome(UnicodeSet s, String str) {
        int cp;
        for (int i = 0; i < str.length(); i += UTF16.getCharCount(cp)) {
            cp = UTF16.charAt(str,i);
            if (s.contains(cp)) return true;
        }
        return false;
    }
}