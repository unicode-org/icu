/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/tools/translit/Attic/UnicodeSetClosure.java,v $
 * $Date: 2001/11/30 00:27:06 $
 * $Revision: 1.2 $
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

// com.ibm.tools.translit.UnicodeSetClosure
// com.ibm.test.translit.TransliteratorTest

public class UnicodeSetClosure {
    public static void main(String[] args) throws Exception {
        
        File f = new File("UnicodeSetClosure.txt");
        String filename = f.getCanonicalFile().toString();
        out = new PrintWriter(
            new OutputStreamWriter(
                new FileOutputStream(filename), "UTF-8"));
        System.out.println("Writing " + filename);
        out.print('\uFEFF'); // BOM
        
        generateSets("Latin-Katakana", true, Normalizer.DECOMP_COMPAT, true,
"[',.a-z~\u00DF\u00E6\u00F0\u00F8\u00FE\u02BE\u0300-\u034E\u0360-\u0362\u0483-\u0486\u0591-\u05A1\u05A3-\u05B9\u05BB-\u05BD\u05BF\u05C1-\u05C2\u05C4\u064B-\u0655\u0670\u06D6-\u06DC\u06DF-\u06E4\u06E7-\u06E8\u06EA-\u06ED\u0711\u0730-\u074A\u07A6-\u07B0\u0901-\u0902\u093C\u0941-\u0948\u094D\u0951-\u0954\u0962-\u0963\u0981\u09BC\u09C1-\u09C4\u09CD\u09E2-\u09E3\u0A02\u0A3C\u0A41-\u0A42\u0A47-\u0A48\u0A4B-\u0A4D\u0A70-\u0A71\u0A81-\u0A82\u0ABC\u0AC1-\u0AC5\u0AC7-\u0AC8\u0ACD\u0B01\u0B3C\u0B3F\u0B41-\u0B43\u0B4D\u0B56\u0B82\u0BC0\u0BCD\u0C3E-\u0C40\u0C46-\u0C48\u0C4A-\u0C4D\u0C55-\u0C56\u0CBF\u0CC6\u0CCC-\u0CCD\u0D41-\u0D43\u0D4D\u0DCA\u0DD2-\u0DD4\u0DD6\u0E31\u0E34-\u0E3A\u0E47-\u0E4E\u0EB1\u0EB4-\u0EB9\u0EBB-\u0EBC\u0EC8-\u0ECD\u0F18-\u0F19\u0F35\u0F37\u0F39\u0F71-\u0F7E\u0F80-\u0F84\u0F86-\u0F87\u0F90-\u0F97\u0F99-\u0FBC\u0FC6\u102D-\u1030\u1032\u1036-\u1037\u1039\u1058-\u1059\u17B7-\u17BD\u17C6\u17C9-\u17D3\u18A9\u20D0-\u20DC\u20E1\u302A-\u302F\uFB1E\uFE20-\uFE23\\U0001D167-\\U0001D169\\U0001D17B-\\U0001D182\\U0001D185-\\U0001D18B\\U0001D1AA-\\U0001D1AD]"
        );
        generateSets("Latin-Katakana", false, Normalizer.DECOMP_COMPAT, false,
"[~\u3001-\u3002\u30A1-\u30AB\u30AD\u30AF\u30B1\u30B3\u30B5\u30B7\u30B9\u30BB\u30BD\u30BF\u30C1\u30C3-\u30C4\u30C6\u30C8\u30CA-\u30CF\u30D2\u30D5\u30D8\u30DB\u30DE-\u30F3\u30F5-\u30F6\u30FC-\u30FD]"
        );
        
        out.close();
        
        /////////////////////////////////////////////////
        if (true) return; // skip the stuff we've done already
        
        generateSets("Cyrillic-Latin", true, Normalizer.DECOMP, false,
            "[\u0402\u0404-\u0406\u0408-\u040B\u040F-\u0418\u041A-\u0438\u043A-\u044F\u0452\u0454-\u0456\u0458-\u045B\u045F\u0490-\u0495\u0498-\u0499\u04D4-\u04D5\u04D8-\u04D9]"
        );
        generateSets("Latin-Cyrillic", false, Normalizer.DECOMP, false,
            "[A-Za-z\u00C6\u00E6\u0110-\u0111\u018F\u0259\u02B9-\u02BA]"
        );
        
        test();
    }
    
    public static void generateSets(String label, boolean forward, 
            Normalizer.Mode m, boolean lowerFirst, String rules) {
        UnicodeSet s = new UnicodeSet(rules);
        System.out.println("Generating " + label + (forward ? "" : " BACKWARD"));
        close(s, m, lowerFirst);
        out.println("# MINIMAL FILTER GENERATED FOR: " + label + (forward ? "" : " BACKWARD"));
        out.println(":: " 
            + (forward ? "" : "( ") 
            + s.toPattern(true) 
            + (forward ? "" : " )")
            + " ;");
        out.println("Unicode: " + s.toPattern(false));
        out.println();
    }
    
    public static void test() throws Exception {
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
            
            //if (cp == '\u00e7') {
              //  System.out.println("debug");
            //}
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