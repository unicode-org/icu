package com.ibm.text.UCD;

import java.util.*;
import java.io.*;

import com.ibm.text.utility.*;

public class GenerateCaseFolding implements UCD_Types {
    public static boolean DEBUG = false;
    public static UCD ucd = UCD.make("310");
    
    public static void main(String[] args) throws java.io.IOException {
        makeCaseFold();
        //getAge();
    }
    
    public static void makeCaseFold() throws java.io.IOException {
        System.out.println("Making Full Data");
        Map fullData = getCaseFolding(true);
        System.out.println("Making Simple Data");
        Map simpleData = getCaseFolding(false);
        // write the data
        
        System.out.println("Writing");
        PrintWriter out = new PrintWriter(
            new BufferedWriter(
            new OutputStreamWriter(
                new FileOutputStream("CaseFoldingSample.txt"),
                "UTF8"),
            4*1024));

        for (int ch = 0; ch < 0x10FFFF; ++ch) {
            String rFull = (String)fullData.get(UTF32.valueOf32(ch));
            String rSimple = (String)simpleData.get(UTF32.valueOf32(ch));
            if (rFull == null && rSimple == null) continue;
            if (rFull != null && rFull.equals(rSimple)) {
                String type = "C";
                if (ch == 0x130 || ch == 0x131) type = "I";
                drawLine(out, ch, type, rFull);
            } else {
                if (rFull != null) {
                    drawLine(out, ch, "F", rFull);
                }
                if (rSimple != null) {
                    drawLine(out, ch, "S", rSimple);
                }
            }
        }
        out.close();
    }
    
    static void drawLine(PrintWriter out, int ch, String type, String result) {
        out.println(Utility.hex(ch) 
            + "; " + type + 
            "; " + Utility.hex(result, " ") +  
            "; # " + ucd.getName(ch));
    }
    
    
    static Map getCaseFolding(boolean full) throws java.io.IOException {
        Map data = new TreeMap();
        Map repChar = new TreeMap();
        //String option = "";
        
        // get the equivalence classes
        
        for (int ch = 0; ch < 0x10FFFF; ++ch) {
            if ((ch & 0x3FF) == 0) System.out.println(Utility.hex(ch));
            if (!ucd.isRepresented(ch)) continue;
            getClosure(ch, data, full);
        }
        
        // get the representative characters
        
        Iterator it = data.keySet().iterator();
        while (it.hasNext()) {
            String s = (String) it.next();
            Set set = (Set) data.get(s);
            String rep = null;
            int repGood = 0;
            String dup = null;
            Iterator it2 = set.iterator();
            while (it2.hasNext()) {
                String s2 = (String)it2.next();
                int s2Good = goodness(s2, full);
                if (s2Good > repGood) {
                    rep = s2;
                    repGood = s2Good;
                    dup = null;
                } else if (s2Good == repGood) {
                    dup = s2;
                }
            }
            if (rep == null) System.err.println("No representative for: " + toString(set));
            else if (repGood < 128) {
                System.err.println("Non-optimal!!: " 
                    + ucd.getName(rep) + ", " + toString(set,true));
            }
            it2 = set.iterator();
            while (it2.hasNext()) {
                String s2 = (String)it2.next();
                if (s2.length() == 1 && !s2.equals(rep)) repChar.put(UTF32.getCodePointSubstring(s2,0), rep);
            }
        }
        return repChar;
    }
    
    static int goodness(String s, boolean full) {
        if (s == null) return 0;
        int result = s.length();
        if (s.equals(lower(upper(s, full), full))) result |= 128;
        if (s.equals(NFC.normalize(s))) result |= 64;
        return result;
    }

    
    static Normalizer NFC = new Normalizer(Normalizer.NFC);
    /*
    static HashSet temp = new HashSet();
    static void normalize(HashSet set) {
        temp.clear();
        temp.addAll(set);
        set.clear();
        Iterator it = temp.iterator();
        while (it.hasNext()) {
            String s = (String) it.next();
            String s2 = KC.normalize(s);
            set.add(s);
            data2.put(s,set);
            if (!s.equals(s2)) {
                set.add(s2);
                data2.put(s2,set);
                System.err.println("Adding " + Utility.hex(s) + " by " + Utility.hex(s2));
            }
        }
    }
    */
        
            /*
            String 
            String lower1 = ucd.getLowercase(ch);
            String lower2 = ucd.toLowercase(ch,option);
            
            char ch2 = ucd.getLowercase(ucd.getUppercase(ch).charAt(0)).charAt(0);
            //String lower1 = String.valueOf(ucd.getLowercase(ch));
            //String lower = ucd.toLowercase(ch2,option);
            String upper = ucd.toUppercase(ch2,option);
            String lowerUpper = ucd.toLowercase(upper,option);
            //String title = ucd.toTitlecase(ch2,option);
            //String lowerTitle = ucd.toLowercase(upper,option);
            
            if (ch != ch2 || lowerUpper.length() != 1 || ch != lowerUpper.charAt(0)) { // 
                output.println(Utility.hex(ch) 
                    + "; " + (lowerUpper.equals(lower1) ? "L" : lowerUpper.equals(lower2) ? "S" : "E")
                    + "; " + Utility.hex(lowerUpper," ")
                    + ";\t#" + ucd.getName(ch)
                    );
                //if (!lowerUpper.equals(lower)) {
                //    output.println("Warning1: " + Utility.hex(lower) + " " + ucd.getName(lower));
                //}
                //if (!lowerUpper.equals(lowerTitle)) {
                //    output.println("Warning2: " + Utility.hex(lowerTitle) + " " + ucd.getName(lowerTitle));
                //}
            }
            */
        
    static void getClosure(int ch, Map data, boolean full) {
        String charStr = UTF32.valueOf32(ch);
        String lowerStr = lower(charStr, full);
        String titleStr = title(charStr, full);
        String upperStr = upper(charStr, full);
        if (charStr.equals(lowerStr) && charStr.equals(upperStr) && charStr.equals(titleStr)) return;
        if (DEBUG) System.err.println("Closure for " + Utility.hex(ch));
        
        // make new set
        Set set = new TreeSet();
        set.add(charStr);
        data.put(charStr, set);
        
        // add cases to get started
        add(set, lowerStr, data);
        add(set, upperStr, data);
        add(set, titleStr, data);
        
        // close it
        main:
        while (true) {
            Iterator it = set.iterator();
            while (it.hasNext()) {
                String s = (String) it.next();
                // do funny stuff since we can't modify set while iterating
                //if (add(set, NFC.normalize(s), data)) continue main;
                if (add(set, lower(s, full), data)) continue main;
                if (add(set, title(s, full), data)) continue main;
                if (add(set, upper(s, full), data)) continue main;
            }
            break;
        }
    }
    
    static String lower(String s, boolean full) {
        String result = lower2(s,full);
        return result.replace('\u03C2', '\u03C3'); // HACK for lower
    }
    
    // These functions are no longer necessary, since UCD is parameterized,
    // but it's not worth changing
    
    static String lower2(String s, boolean full) {
        if (!full) {
            if (s.length() != 1) return s;
            return ucd.getCase(UTF32.char32At(s,0), SIMPLE, LOWER);
        }
        return ucd.getCase(s, FULL, LOWER);
    }
    
    static String upper(String s, boolean full) {
        if (!full) {
            if (s.length() != 1) return s;
            return ucd.getCase(UTF32.char32At(s,0), FULL, UPPER);
        }
        return ucd.getCase(s, SIMPLE, UPPER);
    }
    
    static String title(String s, boolean full) {
        if (!full) {
            if (s.length() != 1) return s;
            return ucd.getCase(UTF32.char32At(s,0), FULL, TITLE);
        }
        return ucd.getCase(s, SIMPLE, TITLE);
    }
    
    static boolean add(Set set, String s, Map data) {
        if (set.contains(s)) return false;
        set.add(s);
        if (DEBUG) System.err.println("adding: " + toString(set));
        Set other = (Set) data.get(s);
        if (other != null && other != set) { // merge
            // make all the items in set point to merged set
            Iterator it = other.iterator();
            while (it.hasNext()) {
                data.put(it.next(), set);
            }
            set.addAll(other);
        }
        if (DEBUG) System.err.println("done adding: " + toString(set));
        return true;
    }
    
    static String toString(Set set) {
        String result = "{";
        Iterator it2 = set.iterator();
        boolean first = true;
        while (it2.hasNext()) {
            String s2 = (String) it2.next();
            if (!first) result += ", ";
            first = false;
            result += Utility.hex(s2, " ");
        }
        return result + "}";
    }
    
    static String toString(Set set, boolean t) {
        String result = "{";
        Iterator it2 = set.iterator();
        boolean first = true;
        while (it2.hasNext()) {
            String s2 = (String) it2.next();
            if (!first) result += ", ";
            first = false;
            result += ucd.getName(s2);
        }
        return result + "}";
    }
    
    static final void getAge() throws IOException {
        PrintStream log = new PrintStream(
            new BufferedOutputStream (
            new FileOutputStream("UnicodeAge.txt"),
            4*1024));
        try {
            log.println("# Derived file showing when various code points were allocated in Unicode");
            log.println("# author: M. Davis");
            log.println("# generated: " + new Date());
            log.println("# Notes:");
            log.println("# - The old Hangul Syllables (removed from 2.0) are not included in the 110 listing.");
            log.println("# - The supplementary private use code points, although allocated earlier,");
            log.println("#   were NOT specifically listed in the UCD until 3.0.1, and are not included until then.");
            new DiffPropertyLister(null, "110", log).print();
            new DiffPropertyLister("110", "200", log).print();
            new DiffPropertyLister("200", "210", log).print();
            new DiffPropertyLister("210", "300", log).print();
            new DiffPropertyLister("300", "310", log).print();
            /*
            printDiff("110", "200");
	        UnicodeSet u11 = fromFile(BASE_DIR + "UnicodeData\\Versions\\UnicodeData-1.1.txt", false);
	        UnicodeSet u20 = fromFile(BASE_DIR + "UnicodeData\\Versions\\UnicodeData-2.0.txt", false);
	        UnicodeSet u21 = fromFile(BASE_DIR + "UnicodeData\\Versions\\UnicodeData-2.1.txt", false);
	        UnicodeSet u30 = fromFile(BASE_DIR + "UnicodeData\\Versions\\UnicodeData-3.0.txt", false);
	        UnicodeSet u31 = fromFile(BASE_DIR + "UnicodeData\\Versions\\UnicodeData-3.1.txt", false);
	        
            log.println();
            log.println("# Code points assigned in Unicode 1.1 (minus Hangul Syllables): " 
                + n.format(u11.count()));
            log.println();
            u11.print(log, false, false, "1.1");
            
            UnicodeSet u20m = new UnicodeSet(u20).remove(u11);
            log.println();
            log.println("# Code points assigned in Unicode 2.0 (minus Unicode 1.1): " 
                + n.format(u20m.count()));
            log.println();
            u20m.print(log, false, false, "2.0");

            UnicodeSet u21m = new UnicodeSet(u21).remove(u20);
            log.println();
            log.println("# Code points assigned in Unicode 2.1 (minus Unicode 2.0): " 
                + n.format(u21m.count()));
            log.println();
            u21m.print(log, false, false, "2.1");

            UnicodeSet u30m = new UnicodeSet(u30).remove(u21);
            log.println();
            log.println("# Code points assigned in Unicode 3.0 (minus Unicode 2.1): " 
                + n.format(u30m.count()));
            log.println();
            u30m.print(log, false, false, "3.0");

            UnicodeSet u31m = new UnicodeSet(u31).remove(u30);
            log.println();
            log.println("# Code points assigned in Unicode 3.1 (minus Unicode 3.0): " 
                + n.format(u31m.count()));
            log.println();
            u31m.print(log, false, false, "3.1");
            */
        } finally {
            if (log != null) log.close();
        }
        
    }
    
}