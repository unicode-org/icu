/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/unicodetools/com/ibm/text/UCD/GenerateCaseFolding.java,v $
* $Date: 2002/07/30 09:56:41 $
* $Revision: 1.11 $
*
*******************************************************************************
*/

package com.ibm.text.UCD;

import java.util.*;
import java.io.*;
import com.ibm.icu.text.UTF16;

import com.ibm.text.utility.*;

public class GenerateCaseFolding implements UCD_Types {
    public static boolean DEBUG = false;
    public static boolean COMMENT_DIFFS = false; // ON if we want a comment on mappings != lowercase
    public static boolean PICK_SHORT = false; // picks short value for SIMPLE if in FULL, changes weighting
    public static boolean NF_CLOSURE = false; // picks short value for SIMPLE if in FULL, changes weighting
    static final int CHECK_CHAR = 0x130; // for debugging, change to actual character, otherwise -1
     
    // PICK_SHORT & NF_CLOSURE = false for old style
    
    
    /*public static void main(String[] args) throws java.io.IOException {
        makeCaseFold(arg[0]);
        //getAge();
    }
    */
    
    static PrintWriter log;
    
    public static void makeCaseFold(boolean normalized) throws java.io.IOException {
        PICK_SHORT = NF_CLOSURE = normalized;
        
        Default.setUCD();
        log = Utility.openPrintWriter("CaseFoldingLog" + GenerateData.getFileSuffix(true), Utility.LATIN1_UNIX);
        System.out.println("Writing Log: " + "CaseFoldingLog" + GenerateData.getFileSuffix(true));
        
        System.out.println("Making Full Data");
        Map fullData = getCaseFolding(true, NF_CLOSURE);
        Utility.fixDot();
        System.out.println("Making Simple Data");
        Map simpleData = getCaseFolding(false, NF_CLOSURE);
        // write the data

        Utility.fixDot();
        System.out.println("Writing");
        String filename = "CaseFolding";
        if (normalized) filename += "-Normalized";
        String directory = "DerivedData/";
        String newFile = directory + filename + GenerateData.getFileSuffix(true);
        PrintWriter out = Utility.openPrintWriter(newFile, Utility.LATIN1_UNIX);
        String mostRecent = GenerateData.generateBat(directory, filename, GenerateData.getFileSuffix(true));
        
        out.println("# CaseFolding" + GenerateData.getFileSuffix(false));
        out.println(GenerateData.generateDateLine());
        out.println("#");
        Utility.appendFile("CaseFoldingHeader.txt", false, out);
        
        /*
        PrintWriter out = new PrintWriter(
            new BufferedWriter(
            new OutputStreamWriter(
                new FileOutputStream(directory + fileRoot + GenerateData.getFileSuffix()),
                "UTF8"),
            4*1024));
        */
        
        for (int ch = 0; ch <= 0x10FFFF; ++ch) {
            Utility.dot(ch);

            if (!charsUsed.get(ch)) continue;
            
            String rFull = (String)fullData.get(UTF32.valueOf32(ch));
            String rSimple = (String)simpleData.get(UTF32.valueOf32(ch));
            if (rFull == null && rSimple == null) continue;
            if (rFull != null && rFull.equals(rSimple) 
              || (PICK_SHORT && UTF16.countCodePoint(rFull) == 1)) {
                String type = "C";
                if (ch == 0x49) {
                	drawLine(out, ch, "C", "i");
                	drawLine(out, ch, "T", "\u0131");
                } else if (ch == 0x130) {
                	drawLine(out, ch, "F", "i\u0307");
                	drawLine(out, ch, "T", "i");
                } else if (ch == 0x131) {
                	// do nothing
                	//drawLine(out, ch, "I", "i");
                } else {
                	drawLine(out, ch, type, rFull);
                }
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
        log.close();
        Utility.renameIdentical(mostRecent, Utility.getOutputName(newFile));
    }
    
/* Goal is following (with no entries for 0131 or 0069)
    
0049; C; 0069; # LATIN CAPITAL LETTER I
0049; T; 0131; # LATIN CAPITAL LETTER I

0130; F; 0069 0307; # LATIN CAPITAL LETTER I WITH DOT ABOVE
0130; T; 0069; # LATIN CAPITAL LETTER I WITH DOT ABOVE
*/

    static void drawLine(PrintWriter out, int ch, String type, String result) {
        String comment = "";
        if (COMMENT_DIFFS) {
            String lower = Default.ucd.getCase(UTF16.valueOf(ch), FULL, LOWER);
            if (!lower.equals(result)) {
                String upper = Default.ucd.getCase(UTF16.valueOf(ch), FULL, UPPER);
                String lower2 = Default.ucd.getCase(UTF16.valueOf(ch), FULL, LOWER);
                if (lower.equals(lower2)) {
                    comment = "[Diff " + Utility.hex(lower, " ") + "] ";
                } else {
                    Utility.fixDot();
                    System.out.println("PROBLEM WITH: " + Default.ucd.getCodeAndName(ch));
                    comment = "[DIFF " + Utility.hex(lower, " ") + ", " + Utility.hex(lower2, " ") + "] ";
                }
            }
        }
        
        out.println(Utility.hex(ch)
            + "; " + type
            + "; " + Utility.hex(result, " ")
            + "; # " + comment + Default.ucd.getName(ch));
    }

    static int probeCh = 0x01f0;
    static String shower = UTF16.valueOf(probeCh);

    static Map getCaseFolding(boolean full, boolean nfClose) throws java.io.IOException {
        Map data = new TreeMap();
        Map repChar = new TreeMap();
        //String option = "";

        // get the equivalence classes

        for (int ch = 0; ch <= 0x10FFFF; ++ch) {
            Utility.dot(ch);
            //if ((ch & 0x3FF) == 0) System.out.println(Utility.hex(ch));
            if (!Default.ucd.isRepresented(ch)) continue;
            getClosure(ch, data, full, nfClose);
        }

        // get the representative characters
        
        Iterator it = data.keySet().iterator();
        while (it.hasNext()) {
            String s = (String) it.next();
            Set set = (Set) data.get(s);
            show = set.contains(shower);
            if (show) {
                Utility.fixDot();
                System.out.println(toString(set));
            }
            
        // Pick the best available representative
            
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
            if (rep == null) {
                Utility.fixDot();
                System.err.println("No representative for: " + toString(set));
            } else if ((repGood & (NFC_FORMAT | ISLOWER)) != (NFC_FORMAT | ISLOWER)) {
                String message = "";
                if ((repGood & NFC_FORMAT) == 0) {
                    message += " [NOT NFC FORMAT]";
                }
                if ((repGood & ISLOWER) == 0) {
                    message += " [NOT LOWERCASE]";
                }
                Utility.fixDot();
                log.println("Non-Optimal Representative " + message);
                log.println(" Rep:\t" + Default.ucd.getCodeAndName(rep));
                log.println(" Set:\t" + toString(set,true, true));
            }
            
        // Add it for all the elements of the set
        
            it2 = set.iterator();
            while (it2.hasNext()) {
                String s2 = (String)it2.next();
                if (UTF16.countCodePoint(s2) == 1 && !s2.equals(rep)) {
                    repChar.put(UTF32.getCodePointSubstring(s2,0), rep);
                    charsUsed.set(UTF16.charAt(s2, 0));
                }
            }
        }
        return repChar;
    }
    
    static BitSet charsUsed = new BitSet();
    static boolean show = false;
    static final int NFC_FORMAT = 64;
    static final int ISLOWER = 128;

    static int goodness(String s, boolean full) {
        if (s == null) return 0;
        int result = 32-s.length();
        if (!PICK_SHORT) {
            result = s.length();
        }
        if (!full) result <<= 8;
        String low = lower(upper(s, full), full);
        if (s.equals(low)) result |= ISLOWER;
        else if (PICK_SHORT && Default.nfd.normalize(s).equals(Default.nfd.normalize(low))) result |= ISLOWER;
        
        if (s.equals(Default.nfc.normalize(s))) result |= NFC_FORMAT;
        
        if (show) {
            Utility.fixDot();
            System.out.println(Utility.hex(result) + ", " + Default.ucd.getCodeAndName(s));
        }
        return result;
    }


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
            String lower1 = Default.ucd.getLowercase(ch);
            String lower2 = Default.ucd.toLowercase(ch,option);

            char ch2 = Default.ucd.getLowercase(Default.ucd.getUppercase(ch).charAt(0)).charAt(0);
            //String lower1 = String.valueOf(Default.ucd.getLowercase(ch));
            //String lower = Default.ucd.toLowercase(ch2,option);
            String upper = Default.ucd.toUppercase(ch2,option);
            String lowerUpper = Default.ucd.toLowercase(upper,option);
            //String title = Default.ucd.toTitlecase(ch2,option);
            //String lowerTitle = Default.ucd.toLowercase(upper,option);

            if (ch != ch2 || lowerUpper.length() != 1 || ch != lowerUpper.charAt(0)) { //
                output.println(Utility.hex(ch)
                    + "; " + (lowerUpper.equals(lower1) ? "L" : lowerUpper.equals(lower2) ? "S" : "E")
                    + "; " + Utility.hex(lowerUpper," ")
                    + ";\t#" + Default.ucd.getName(ch)
                    );
                //if (!lowerUpper.equals(lower)) {
                //    output.println("Warning1: " + Utility.hex(lower) + " " + Default.ucd.getName(lower));
                //}
                //if (!lowerUpper.equals(lowerTitle)) {
                //    output.println("Warning2: " + Utility.hex(lowerTitle) + " " + Default.ucd.getName(lowerTitle));
                //}
            }
            */

    static void getClosure(int ch, Map data, boolean full, boolean nfClose) {
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
                // We don't do this because if the source is not normalized, we don't want to normalize
                if (nfClose) {
                    if (add(set, Default.nfd.normalize(s), data)) continue main;
                    if (add(set, Default.nfc.normalize(s), data)) continue main;
                    if (add(set, Default.nfkd.normalize(s), data)) continue main;
                    if (add(set, Default.nfkc.normalize(s), data)) continue main;
                }
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

    // These functions are no longer necessary, since Default.ucd is parameterized,
    // but it's not worth changing

    static String lower2(String s, boolean full) {
        /*if (!full) {
            if (s.length() != 1) return s;
            return Default.ucd.getCase(UTF32.char32At(s,0), SIMPLE, LOWER);
        }
        */
        return Default.ucd.getCase(s, full ? FULL : SIMPLE, LOWER);
    }

    static String upper(String s, boolean full) {
        /* if (!full) {
            if (s.length() != 1) return s;
            return Default.ucd.getCase(UTF32.char32At(s,0), FULL, UPPER);
        }
        */
        return Default.ucd.getCase(s, full ? FULL : SIMPLE, UPPER);
    }

    static String title(String s, boolean full) {
        /*if (!full) {
            if (s.length() != 1) return s;
            return Default.ucd.getCase(UTF32.char32At(s,0), FULL, TITLE);
        }
        */
        return Default.ucd.getCase(s, full ? FULL : SIMPLE, TITLE);
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
        return toString(set, false, false);
    }

    static String toString(Set set, boolean name, boolean crtab) {
        String result = "{";
        Iterator it2 = set.iterator();
        boolean first = true;
        while (it2.hasNext()) {
            String s2 = (String) it2.next();
            if (!first) {
                if (crtab) {
                    result += ";\r\n\t";
                } else {
                    result += "; ";
                }
            }
            first = false;
            if (name) {
                result += Default.ucd.getCodeAndName(s2);
            } else {
                result += Utility.hex(s2, " ");
            }
        }
        return result + "}";
    }
    
    static boolean specialNormalizationDiffers(int ch) {
        if (ch == 0x00DF) return true;                  // es-zed
        return !Default.nfkd.isNormalized(ch);
    }
    
    static String specialNormalization(String s) {
        if (s.equals("\u00DF")) return "ss";
        return Default.nfkd.normalize(s);
    }
    
    static boolean isExcluded(int ch) {
        // if (ch == 0x130) return true;                  // skip LATIN CAPITAL LETTER I WITH DOT ABOVE
        if (ch == 0x0132 || ch == 0x0133) return true; // skip IJ, ij
        if (ch == 0x037A) return true;                 // skip GREEK YPOGEGRAMMENI
        if (0x249C <= ch && ch <= 0x24B5) return true; // skip PARENTHESIZED LATIN SMALL LETTER A..
        if (0x20A8 <= ch && ch <= 0x217B) return true; // skip Rupee..
        
        byte type = Default.ucd.getDecompositionType(ch);  
        if (type == COMPAT_SQUARE) return true;
        //if (type == COMPAT_UNSPECIFIED) return true;
        return false;
    }
    
    static void generateSpecialCasing(boolean normalize) throws IOException {
        Default.setUCD();
        Map sorted = new TreeMap();
        
        String suffix2 = "";
        if (normalize) suffix2 = "-Normalized";
        
        PrintWriter log = Utility.openPrintWriter("SpecialCasingExceptions"
            + suffix2 + GenerateData.getFileSuffix(true), Utility.LATIN1_UNIX);
        
        for (int ch = 0; ch <= 0x10FFFF; ++ch) {
            Utility.dot(ch);
            if (!Default.ucd.isRepresented(ch)) continue;
            if (!specialNormalizationDiffers(ch)) continue;

            String lower = Default.nfc.normalize(Default.ucd.getCase(ch, SIMPLE, LOWER));
            String upper = Default.nfc.normalize(Default.ucd.getCase(ch, SIMPLE, UPPER));
            String title = Default.nfc.normalize(Default.ucd.getCase(ch, SIMPLE, TITLE));
            
            String chstr = UTF16.valueOf(ch);
            
            String decomp = specialNormalization(chstr);
            String flower = Default.nfc.normalize(Default.ucd.getCase(decomp, SIMPLE, LOWER));
            String fupper = Default.nfc.normalize(Default.ucd.getCase(decomp, SIMPLE, UPPER));
            String ftitle = Default.nfc.normalize(Default.ucd.getCase(decomp, SIMPLE, TITLE));
            
            String base = decomp;
            String blower = specialNormalization(lower);
            String bupper = specialNormalization(upper);
            String btitle = specialNormalization(title);

            if (true) {
                flower = Default.nfc.normalize(flower);
                fupper = Default.nfc.normalize(fupper);
                ftitle = Default.nfc.normalize(ftitle);
                base = Default.nfc.normalize(base);
                blower = Default.nfc.normalize(blower);
                bupper = Default.nfc.normalize(bupper);
                btitle = Default.nfc.normalize(btitle);
            }
            
            if (ch == CHECK_CHAR) {
                System.out.println("Code: " + Default.ucd.getCodeAndName(ch));
                System.out.println("Decomp: " + Default.ucd.getCodeAndName(decomp));
                System.out.println("Base: " + Default.ucd.getCodeAndName(base));
                System.out.println("SLower: " + Default.ucd.getCodeAndName(lower));
                System.out.println("FLower: " + Default.ucd.getCodeAndName(flower));
                System.out.println("BLower: " + Default.ucd.getCodeAndName(blower));
                System.out.println("STitle: " + Default.ucd.getCodeAndName(title));
                System.out.println("FTitle: " + Default.ucd.getCodeAndName(ftitle));
                System.out.println("BTitle: " + Default.ucd.getCodeAndName(btitle));
                System.out.println("SUpper: " + Default.ucd.getCodeAndName(upper));
                System.out.println("FUpper: " + Default.ucd.getCodeAndName(fupper));
                System.out.println("BUpper: " + Default.ucd.getCodeAndName(bupper));
            }
            
            // presumably if there is a single code point, it would already be in the simple mappings
            
            if (UTF16.countCodePoint(flower) == 1 && UTF16.countCodePoint(fupper) == 1 
                	&& UTF16.countCodePoint(title) == 1) {
            	if (ch == CHECK_CHAR) System.out.println("Skipping single code point: " + Default.ucd.getCodeAndName(ch));
            	continue;
            }
            
            // if there is no change from the base, skip
            
            if (flower.equals(base) && fupper.equals(base) && ftitle.equals(base)) {
            	if (ch == CHECK_CHAR) System.out.println("Skipping equals base: " + Default.ucd.getCodeAndName(ch));
            	continue;
            }
            
            // fix special cases
            // if (flower.equals(blower) && fupper.equals(bupper) && ftitle.equals(btitle)) continue;
            if (flower.equals(blower)) flower = lower;
            if (fupper.equals(bupper)) fupper = upper;
            if (ftitle.equals(btitle)) ftitle = title;
            
            // if there are no changes from the original, or the expanded original, skip
            
            if (flower.equals(lower) && fupper.equals(upper) && ftitle.equals(title)) {
            	if (ch == CHECK_CHAR) System.out.println("Skipping unchanged: " + Default.ucd.getCodeAndName(ch));
            	continue;
            }
            
            String name = Default.ucd.getName(ch);
            
            int order = name.equals("LATIN SMALL LETTER SHARP S") ? 1
                : ch == 0x130 ? 2
                : name.indexOf("ARMENIAN SMALL LIGATURE") >= 0 ? 4
                : name.indexOf("LIGATURE") >= 0 ? 3
                : name.indexOf("GEGRAMMENI") < 0 ? 5
                : UTF16.countCodePoint(ftitle) == 1 ? 6
                : UTF16.countCodePoint(fupper) == 2 ? 7
                : 8;
            
            if (ch == CHECK_CHAR) System.out.println("Order: " + order + " for " + Default.ucd.getCodeAndName(ch));
            
            // HACK
            boolean denormalize = !normalize && order != 6 && order != 7;
            
            String mapping = Utility.hex(ch)
                + "; " + Utility.hex(flower.equals(base) ? chstr : denormalize ? Default.nfd.normalize(flower) : flower)
                + "; " + Utility.hex(ftitle.equals(base) ? chstr : denormalize ? Default.nfd.normalize(ftitle) : ftitle)
                + "; " + Utility.hex(fupper.equals(base) ? chstr : denormalize ? Default.nfd.normalize(fupper) : fupper)
                + "; # " + Default.ucd.getName(ch);
            
            // special exclusions 
            if (isExcluded(ch)) {
                log.println("# " + mapping);
            } else {
                int x = ch;
                if (ch == 0x01F0) x = 0x03B1; // HACK to reorder the same
                sorted.put(new Integer((order << 24) | x), mapping);
            }
        }
        log.close();
        
        System.out.println("Writing");
        String newFile = "DerivedData/SpecialCasing" + suffix2 + GenerateData.getFileSuffix(true);
        PrintWriter out = Utility.openPrintWriter(newFile, Utility.LATIN1_UNIX);
        String mostRecent = GenerateData.generateBat("DerivedData/", "SpecialCasing", suffix2 + GenerateData.getFileSuffix(true));
        out.println("# SpecialCasing" + GenerateData.getFileSuffix(false));
        out.println(GenerateData.generateDateLine());
        out.println("#");
        Utility.appendFile("SpecialCasingHeader.txt", true, out);

        Iterator it = sorted.keySet().iterator();
        int lastOrder = -1;
        while (it.hasNext()) {
            Integer key = (Integer) it.next();
            String line = (String) sorted.get(key);
            int order = key.intValue() >> 24;
            if (order != lastOrder) {
                lastOrder = order;
                out.println();
                boolean skipLine = false;
                switch(order) {
                case 1: 
                    out.println("# The German es-zed is special--the normal mapping is to SS.");
                    out.println("# Note: the titlecase should never occur in practice. It is equal to titlecase(uppercase(<es-zed>))");
                    break;
                case 2:
                    out.println("# Preserve canonical equivalence for I with dot. Turkic is handled below.");
					break;                	
                case 3: out.println("# Ligatures"); break;
                case 4: skipLine = true; break;
                case 5: out.println("# No corresponding uppercase precomposed character"); break;
                case 6: Utility.appendFile("SpecialCasingIota.txt", true, out); break;
                case 7: out.println("# Some characters with YPOGEGRAMMENI are also have no corresponding titlecases"); break;
                case 8: skipLine = true; break;
                }
                if (!skipLine) out.println();
            }
            out.println(line);
        }
        Utility.appendFile("SpecialCasingFooter.txt", true, out);
        out.close();
        Utility.renameIdentical(mostRecent, Utility.getOutputName(newFile));
    }
}