/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/unicodetools/com/ibm/text/UCD/GenerateBreakTest.java,v $
* $Date: 2002/08/08 15:38:15 $
* $Revision: 1.1 $
*
*******************************************************************************
*/

package com.ibm.text.UCD;

import java.util.*;
import java.io.*;

import com.ibm.text.utility.*;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;

abstract public class GenerateBreakTest implements UCD_Types {

    static boolean DEBUG = false;

    // COMMON STUFF for Hangul
    static final byte hNot = -1, hL = 0, hV = 1, hT = 2, hLV = 3, hLVT = 4, hLIMIT = 5;
    static final String[] hNames = {"L", "V", "T", "LV", "LVT"};

    static byte getHangulType(int cp) {
        if (Default.ucd.isLeadingJamo(cp)) return hL;
        if (Default.ucd.isVowelJamo(cp)) return hV;
        if (Default.ucd.isTrailingJamo(cp)) return hT;
        if (Default.ucd.isHangulSyllable(cp)) {
            if (Default.ucd.isDoubleHangul(cp)) return hLV;
            return hLVT;
        }
        return hNot;
    }

    public static boolean onCodepointBoundary(String s, int offset) {
        if (offset < 0 || offset > s.length()) return false;
        if (offset == 0 || offset == s.length()) return true;
        if (UTF16.isLeadSurrogate(s.charAt(offset-1))
        && UTF16.isTrailSurrogate(s.charAt(offset))) return false;
        return true;
    }

    // finds the first base character, or the first character if there is no base
    public static int findFirstBase(String source, int start, int limit) {
        int cp;
        for (int i = start; i < limit; i += UTF16.getCharCount(cp)) {
            cp = UTF16.charAt(source, i);
            byte cat = Default.ucd.getCategory(cp);
            if (((1<<cat) & MARK_MASK) != 0) continue;
            return cp;
        }
        return UTF16.charAt(source, start);
    }


    static UnicodeSet midLetterSet = new UnicodeSet("[\u0027\u002E\u003A\u00AD\u05F3\u05F4\u2019\uFE52\uFE55\uFF07\uFF0E\uFF1A]");
    /*
    U+0027 APOSTROPHE
    U+002E FULL STOP
    U+003A COLON # used in Swedish
    U+00AD SOFT HYPHEN
    U+05F3 HEBREW PUNCTUATION GERESH
    U+05F4 HEBREW PUNCTUATION GERSHAYIM
    U+2019 RIGHT SINGLE QUOTATION MARK
    U+FE52 SMALL FULL STOP
    U+FE55 SMALL COLON
    U+FF07 FULLWIDTH APOSTROPHE
    U+FF0E FULLWIDTH FULL STOP
    U+FF1A FULLWIDTH COLON
    */

    static UnicodeSet ambigSentPunct = new UnicodeSet("[\u002E\u0589\u06D4]");
    /*
    U+002E FULL STOP
    U+0589 ARMENIAN FULL STOP
    U+06D4 ARABIC FULL STOP
    */

    static UnicodeSet sentPunct = new UnicodeSet("[\u0021\u003F\u0387\u061F\u0964\u203C\u203D\u2048\u2049"
        + "\u3002\ufe52\ufe57\uff01\uff0e\uff1f\uff61]");
    /*
    U+0021 EXCLAMATION MARK
    U+003F QUESTION MARK
    U+0387 GREEK ANO TELEIA
    U+061F ARABIC QUESTION MARK
    U+0964 DEVANAGARI DANDA
    U+203C DOUBLE EXCLAMATION MARK
    U+203D INTERROBANG
    U+2048 QUESTION EXCLAMATION MARK
    U+2049 EXCLAMATION QUESTION MARK
    U+3002 IDEOGRAPHIC FULL STOP
    U+FE52 SMALL FULL STOP
    U+FE57 SMALL EXCLAMATION MARK
    U+FF01 FULLWIDTH EXCLAMATION MARK
    U+FF0E FULLWIDTH FULL STOP
    U+FF1F FULLWIDTH QUESTION MARK
    U+FF61 HALFWIDTH IDEOGRAPHIC FULL STOP
    */

    public static void main(String[] args) throws IOException {
        System.out.println("Remember to add length marks (half & full) and other punctuation for sentence, with FF61");
        Default.setUCD();

        checkDecomps();

        if (DEBUG) {
            Utility.showSetNames("", new UnicodeSet("[\u034F\u00AD\u1806[:DI:]-[:Cs:]-[:Cn:]]"), true, Default.ucd);

            System.out.println("*** Extend - Cf");

            generateTerminalClosure();

            GenerateWordBreakTest gwb = new GenerateWordBreakTest();
            PrintWriter systemPrintWriter = new PrintWriter(System.out);
            gwb.printLine(systemPrintWriter, "n\u0308't", true, true, false);
            systemPrintWriter.flush();
        }

        new GenerateLineBreakTest().run();
        new GenerateGraphemeBreakTest().run();
        new GenerateWordBreakTest().run();
    }

    static void checkDecomps() {
        UnicodeProperty[]  INFOPROPS = {UnifiedProperty.make(CATEGORY), UnifiedProperty.make(LINE_BREAK)};
        GenerateBreakTest[] tests = {
            new GenerateGraphemeBreakTest(), 
            new GenerateWordBreakTest(),
            new GenerateLineBreakTest(),
        };
        tests[0].isBreak("\u0300\u0903", 1, true);
        
        System.out.println("Check Decomps");
        System.out.println("otherExtendSet: " + ((GenerateGraphemeBreakTest)tests[0]).otherExtendSet.toPattern(true));
        Utility.showSetNames("", ((GenerateGraphemeBreakTest)tests[0]).otherExtendSet, false, Default.ucd);
        
        for (int k = 0; k < tests.length; ++k) {
            for (int i = 0; i < 0x10FFFF; ++i) {
                if (!Default.ucd.isAllocated(i)) continue;
                if (Default.ucd.isHangulSyllable(i)) continue;
                if (Default.nfd.isNormalized(i)) continue;
                String decomp = Default.nfd.normalize(i);
                boolean shown = false;
                String test = decomp;
                for (int j = 1; j < test.length(); ++j) {
                    if (tests[k].isBreak(test, j, true)) {
                        if (!shown) {
                            System.out.println(showData(UTF16.valueOf(i), INFOPROPS, "\r\n\t"));
                            System.out.println(" => " + showData(decomp, INFOPROPS, "\r\n\t"));
                            shown = true;
                        }
                        System.out.println(j  + ": " + tests[k].fileName);
                    }
                }
            }
        }
    }
    
    static String showData(String source, UnicodeProperty[] props, String separator) {
        StringBuffer result = new StringBuffer();
        int cp;
        for (int i = 0; i < source.length(); i += UTF16.getCharCount(cp)) {
            cp = UTF16.charAt(source, i);
            if (i != 0) result.append(separator);
            result.append(Default.ucd.getCodeAndName(cp));
            for (int j = 0; j < props.length; ++j) {
                result.append(", ");
                result.append(props[j].getProperty(SHORT)).append('=').append(props[j].getValue(cp,SHORT));
            }
        }
        return result.toString();
    }
    
    // determines if string is of form Base NSM*
    static boolean isBaseNSMStar(String source) {
        int cp;
        int status = 0;
        for (int i = 0; i < source.length(); i += UTF16.getCharCount(cp)) {
            cp = UTF16.charAt(source, i);
            byte cat = Default.ucd.getCategory(cp);
            int catMask = 1<<cat;
            switch(status) {
            case 0: if ((catMask & BASE_MASK) == 0) return false;
                    status = 1;
                    break;
            case 1: if ((catMask & NONSPACING_MARK_MASK) == 0) return false;
                    break;
            }
                    
        }
        return true;
    }

    static UnicodeSet getClosure(UnicodeSet source) {
        UnicodeSet result = new UnicodeSet(source);
        for (int i = 0; i < 0x10FFFF; ++i) {
            if (!Default.ucd.isAllocated(i)) continue;
            if (Default.nfkd.isNormalized(i)) continue;
            String decomp = Default.nfkd.normalize(i);
            if (source.containsAll(decomp)) result.add(i);
        }
        return result;
    }

    static void generateTerminalClosure() {
        UnicodeSet terminals = UnifiedBinaryProperty.make(BINARY_PROPERTIES | Terminal_Punctuation).getSet();
        UnicodeSet extras = getClosure(terminals).removeAll(terminals);
        System.out.println("Current Terminal_Punctuation");
        Utility.showSetNames("", terminals, true, Default.ucd);

        System.out.println("Missing Terminal_Punctuation");
        Utility.showSetNames("", extras, true, Default.ucd);

        System.out.println("midLetterSet");
        System.out.println(midLetterSet.toPattern(true));
        Utility.showSetNames("", midLetterSet, true, Default.ucd);

        System.out.println("ambigSentPunct");
        System.out.println(ambigSentPunct.toPattern(true));
        Utility.showSetNames("", ambigSentPunct, true, Default.ucd);

        System.out.println("sentPunct");
        System.out.println(sentPunct.toPattern(true));
        Utility.showSetNames("", sentPunct, true, Default.ucd);
        /*

        UnicodeSet sentencePunctuation = new UnicodeSet("[\u0021\003F          ; Terminal_Punctuation # Po       QUESTION MARK
037E          ; Terminal_Punctuation # Po       GREEK QUESTION MARK
061F          ; Terminal_Punctuation # Po       ARABIC QUESTION MARK
06D4          ; Terminal_Punctuation # Po       ARABIC FULL STOP
203C..203D    ; Terminal_Punctuation # Po   [2] DOUBLE EXCLAMATION MARK..INTERROBANG
3002          ; Terminal_Punctuation # Po       IDEOGRAPHIC FULL STOP
2048..2049    ; Terminal_Punctuation # Po   [2] QUESTION EXCLAMATION MARK..EXCLAMATION QUESTION MARK
        */

    }

    //============================

    protected String rule;
    protected String fileName;
    protected String[] samples = new String[100];
    protected String[] extraSamples = new String[0];
    protected String[] extraSingleSamples = new String[0];
    protected int sampleLimit = 0;
    protected int tableLimit = -1;

    public void run() throws IOException {
        findSamples();

        // test individual cases
        //printLine(out, samples[LB_ZW], "", samples[LB_CL]);
        //printLine(out, samples[LB_ZW], " ", samples[LB_CL]);

        PrintWriter out = Utility.openPrintWriter(fileName + "BreakTest.html", Utility.UTF8_WINDOWS);
        out.println("<html><head><meta http-equiv='Content-Type' content='text/html; charset=utf-8'><title>"
            + fileName + "</title></head>");
        out.println("<body bgcolor='#FFFFFF'><h3>Current (fixed only for consistency):</h3>");



        generateTable(out, false);
        out.println("<h3>Recommended:</h3>");
        generateTable(out, true);
        out.println("</body></html>");
        out.close();

        String[] testCase = new String[50];
        // do main test

        for (int k = 0; k < 2; ++k) {
            out = Utility.openPrintWriter(fileName + (k == 0 ? "Test_SHORT.txt" : "Test.txt"), Utility.LATIN1_WINDOWS);
            int counter = 0;

            out.println("# Default " + fileName + " Break Test");
            out.println("# Generated: " + Default.getDate() + ", MED");
            out.println("#");
            out.println("# Format:");
            out.println("# <string> (# <comment>)? ");
            out.println("#  <string> contains hex Unicode code points, with ");
            out.println("#\t" + BREAK + " wherever there is a break opportunity, and ");
            out.println("#\t" + NOBREAK + " wherever there is not.");
            out.println("#  <comment> the format can change, but currently it shows:");
            out.println("#\t- the sample character name");
            out.println("#\t- (x) the line_break property* for the sample character");
            out.println("#\t- [x] the rule that determines whether there is a break or not");
            out.println("#");
            sampleDescription(out);
            out.println("# These samples may be extended or changed in the future.");
            out.println("#");

            for (int ii = 0; ii < sampleLimit; ++ii) {
                String before = samples[ii];

                for (int jj = 0; jj < sampleLimit; ++jj) {
                    Utility.dot(counter);
                    String after = samples[jj];

                    // do line straight
                    int len = genTestItems(before, after, testCase);
                    for (int q = 0; q < len; ++q) {
                        printLine(out, testCase[q], k != 0 && q == 0, false, false);
                        ++counter;
                    }
                }
            }

            for (int ii = 0; ii < extraSingleSamples.length; ++ii) {
                printLine(out, extraSingleSamples[ii], true, false, false);
            }
            out.println("# Lines: " + counter);
            out.close();
        }
    }

    public void sampleDescription(PrintWriter out) {}

    abstract public boolean isBreak(String source, int offset, boolean recommended);

    abstract public byte getType (int cp, boolean recommended);

    abstract public String getTypeID(int s, boolean recommended);


    final public byte getType (int cp) {
        return getType(cp, false);
    }

    final public String getTypeID(int cp) {
        return getTypeID(cp, false);
    }

    final public String getTypeID(String s) {
        return getTypeID(s, false);
    }

    public String getTypeID(String s, boolean recommended) {
        if (s == null) return "<null>";
        if (s.length() == 1) return getTypeID(s.charAt(0), recommended);
        StringBuffer result = new StringBuffer();
        int cp;
        for (int i = 0; i < s.length(); i += UTF32.count16(cp)) {
            cp = UTF32.char32At(s, i);
            if (i > 0) result.append(" ");
            result.append(getTypeID(cp, recommended));
        }
        return result.toString();
    }

    static final int DONE = -1;

    public int next(String source, int offset, boolean recommended) {
        for (int i = offset + 1; i <= source.length(); ++i) {
            if (isBreak(source, i, recommended)) return i;
        }
        return DONE;
    }

    public int previous(String source, int offset, boolean recommended) {
        for (int i = offset - 1; i >= 0; --i) {
            if (isBreak(source, i, recommended)) return i;
        }
        return DONE;
    }

    public int genTestItems(String before, String after, String[] results) {
        results[0] = before + after;
        return 1;
    }

    public String getTableEntry(String before, String after, boolean recommended, String[] ruleOut) {
        boolean normalBreak = isBreak(before + after, before.length(), recommended);
        String normalRule = rule;
        ruleOut[0] = rule;
        return normalBreak ? BREAK : NOBREAK;
    }

    public byte getResolvedType(int cp, boolean recommended) {
        return getType(cp, recommended);
    }

    boolean skipType(int type) {
        return false;
    }

    static String getInfo(String s) {
        if (s == null || s.length() == 0) return "NULL";
        StringBuffer result = new StringBuffer();
        int cp;
        for (int i = 0; i < s.length(); i += UTF32.count16(cp)) {
            cp = UTF32.char32At(s, i);
            if (i > 0) result.append(", ");
            result.append(Default.ucd.getCodeAndName(cp));
            result.append(", gc=" + Default.ucd.getCategoryID_fromIndex(Default.ucd.getCategory(cp),SHORT));
            result.append(", sc=" + Default.ucd.getScriptID_fromIndex(Default.ucd.getScript(cp),SHORT));
            result.append(", lb=" + Default.ucd.getLineBreakID_fromIndex(Default.ucd.getLineBreak(cp)));
        }
        return result.toString();
    }

    public void generateTable(PrintWriter out, boolean recommended) {
        String width = "width='" + (100 / (tableLimit + 1)) + "%'";
        out.print("<table border='1' cellspacing='0' width='100%'>");
        String types = "";
        String codes = "";
        for (int type = 0; type < tableLimit; ++type) {
            String after = samples[type];
            if (after == null) continue;

            String h = getTypeID(after, recommended);
            types += "<th " + width + ">" + h + "</th>";
            codes += "<th " + width + " title='" + getInfo(after) + "'>" + Utility.hex(after) + "</th>";
        }

        out.println("<tr><th " + width + "></th><th " + width + "></th>" + types + "</tr>");
        out.println("<tr><th " + width + "></th><th " + width + "></th>" + codes + "</tr>");

        String[] rule = new String[1];
        String[] rule2 = new String[1];
        for (int type = 0; type < tableLimit; ++type) {
            String before = samples[type];
            if (before == null) continue;

            String line = "<tr><th>" + getTypeID(before, recommended) + "</th>"
                + "<th title='" + Default.ucd.getCodeAndName(before) + "'>" + Utility.hex(before) + "</th>";

            for (int type2 = 0; type2 < tableLimit; ++type2) {
                String after = samples[type2];
                if (after == null) continue;

                String t = getTableEntry(before, after, recommended, rule);
                String background = "";
                String t2 = getTableEntry(before, after, !recommended, rule2);
                if (!t.equals(t2)) {
                    if (t.equals(NOBREAK)) {
                        background = " bgcolor='#CCFFFF'";
                    } else {
                        background = " bgcolor='#FFFF00'";
                    }
                } else if (t.equals(NOBREAK)) {
                    background = " bgcolor='#CCCCFF'";
                }
                line += "<th title='" + rule[0] + "'" + background + ">" + t + "</th>";
            }
            out.println(line + "</tr>");
        }
        out.println("</table>");
        out.println("<ol>");
            for (int ii = 0; ii < extraSingleSamples.length; ++ii) {
                out.println("<li><font size='5'>");
                printLine(out, extraSingleSamples[ii], true, false, true);
                out.println("</font></li>");
            }
        out.println("</ol>");
    }

    static final String BREAK = "\u00F7";
    static final String NOBREAK = "\u00D7";

    public void printLine(PrintWriter out, String source, boolean comments, boolean recommended, boolean html) {
        int cp;
        StringBuffer string = new StringBuffer();
        StringBuffer comment = new StringBuffer("\t# ");
        boolean hasBreak = isBreak(source, 0, recommended);
        String status;
        if (html) {
            status = hasBreak ? " style='border-right: 1px solid blue'" : "";
            string.append("<span title='" + rule + "'><span" + status + ">&nbsp;</span>&nbsp;<span>");
        } else {
            status = hasBreak ? BREAK : NOBREAK;
            string.append(status);
        }
        comment.append(' ').append(status).append(" [").append(rule).append(']');

        for (int offset = 0; offset < source.length(); offset += UTF16.getCharCount(cp)) {

            cp = UTF16.charAt(source, offset);
            hasBreak = isBreak(source, offset + UTF16.getCharCount(cp), recommended);

            if (html) {
                status = hasBreak ? " style='border-right: 1px solid blue'" : "";
                string.append("<span title='" +
                    Utility.quoteXML(Default.ucd.getCodeAndName(cp) + " (" + getTypeID(cp) + ")", true)
                    + "'>"
                    + Utility.quoteXML(Utility.getDisplay(cp), true)
                    + "</span>");
                string.append("<span title='" + rule + "'><span" + status + ">&nbsp;</span>&nbsp;<span>");
            } else {
                if (string.length() > 0) {
                    string.append(' ');
                    comment.append(' ');
                }

                status = hasBreak ? BREAK : NOBREAK;

                string.append(Utility.hex(cp));
                comment.append(Default.ucd.getName(cp) + " (" + getTypeID(cp) + ")");
                string.append(' ').append(status);
                comment.append(' ').append(status).append(" [").append(rule).append(']');
            }
        }

        if (comments && !html) string.append(comment);
        out.println(string);
    }

    public void findSamples() {

        // what we want is a list of sample characters. In the simple case, this is just one per type.
        // However, if there are characters that have different types (when recommended or not), then
        // we want a type for each cross-section

        BitSet bitset = new BitSet();
        Map list = new TreeMap();

        for (int i = 1; i <= 0x10FFFF; ++i) {
            if (!Default.ucd.isAllocated(i)) continue;
            if (0xD800 <= i && i <= 0xDFFF) continue;
            if (DEBUG && i == 0x1100) {
                System.out.println("debug");
            }
            byte lb = getType(i);
            byte lb2 = getType(i, true);
            if (lb == lb2 && skipType(lb)) continue;

            int combined = (lb << 7) + lb2;
            if (!bitset.get(combined)) {
                bitset.set(combined);
                list.put(new Integer(combined), UTF16.valueOf(i));
            }
            /*
            // if the sample slot is full OR
            if (samples[lb] == null) {
                samples[lb] = UTF16.valueOf(i);
                if (sampleLimit <= lb) sampleLimit = lb + 1;
                // byte lb2 = getType(i, true);
                // if (lb2 != lb) bs.set(lb);
            }
            */
        }

        Iterator it = list.keySet().iterator();
        while (it.hasNext()) {
            String sample = (String)list.get(it.next());
            samples[sampleLimit++] = sample;
            if (DEBUG) System.out.println(getTypeID(sample) + ":\t" + Default.ucd.getCodeAndName(sample));
        }

        tableLimit = sampleLimit;

        // now add values that are different
        /*

        for (int i = 1; i <= 0x10FFFF; ++i) {
            if (!Default.ucd.isAllocated(i)) continue;
            if (0xD800 <= i && i <= 0xDFFF) continue;
            byte lb = getType(i);
            byte lb2 = getType(i, true);
            if (lb == lb2) continue;
            // pick some different ones
            if (!bs.get(lb)) {
                samples[sampleLimit++] = UTF16.valueOf(i);
                bs.set(lb);
            }
            if (!bs2.get(lb2)) {
                samples[sampleLimit++] = UTF16.valueOf(i);
                bs.set(lb2);
            }
        }
        */

        System.arraycopy(extraSamples, 0, samples, sampleLimit, extraSamples.length);
        sampleLimit += extraSamples.length;
    }

    public int findLastNon(String source, int offset, byte notLBType, boolean recommended) {
        int cp;
        for (int i = offset-1; i >= 0; i -= UTF16.getCharCount(cp)) {
            cp = UTF16.charAt(source, i);
            byte f = getResolvedType(cp, recommended);
            if (f != notLBType) return i;
        }
        return -1;
    }


    // ========================================

    static class GenerateLineBreakTest extends GenerateBreakTest {
        // all the other items are supplied in UCD_TYPES
        static byte LB_L = LB_LIMIT + hL, LB_V = LB_LIMIT + hV, LB_T = LB_LIMIT + hT,
            LB_LV = LB_LIMIT + hLV, LB_LVT = LB_LIMIT + hLVT, LB_SUP = LB_LIMIT + hLIMIT,
            LB2_LIMIT = (byte)(LB_SUP + 1);

        /*
        private byte[] AsmusOrderToMyOrder = {
                    LB_OP, LB_CL, LB_QU, LB_GL, LB_NS, LB_EX, LB_SY, LB_IS, LB_PR, LB_PO,
                    LB_NU, LB_AL, LB_ID, LB_IN, LB_HY, LB_BA, LB_BB, LB_B2, LB_ZW, LB_CM,
                    // missing from Pair Table
                    LB_SP, LB_BK, LB_CR, LB_LF,
                    // resolved types below
                    LB_CB, LB_AI, LB_SA, LB_SG, LB_XX,
                    // 3 JAMO CLASSES, plus supplementary
                    LB_L, LB_V, LB_T, LB_LV, LB_LVT, LB_SUP
                };

        private byte[] MyOrderToAsmusOrder = new byte[AsmusOrderToMyOrder.length];
        {
            for (byte i = 0; i < AsmusOrderToMyOrder.length; ++i) {
                MyOrderToAsmusOrder[AsmusOrderToMyOrder[i]] = i;
            }
            */
        {
            fileName = "Line";
            extraSingleSamples = new String[] {"can't", "can\u2019t", "ab\u00ADby" };
        }


        public void sampleDescription(PrintWriter out) {
            out.println("# Samples:");
            out.println("# The test currently takes all pairs of linebreak types*,");
            out.println("# picks a sample for each type, and generates three strings: ");
            out.println("#\t- the pair alone");
            out.println("#\t- the pair alone with an imbeded space");
            out.println("#\t- the pair alone with embedded combining marks");
            out.println("# The sample for each type is simply the first code point (above NULL)");
            out.println("# with that property.");
            out.println("# * Note:");
            out.println("#\t- SG is omitted");
            out.println("#\t- 3 different Jamo characters and a supplementary character are added");
            out.println("#\t  The syllable types for the Jamo (L, V, T) are displayed in comments");
            out.println("#\t  instead of the linebreak property");
            out.println("#");
        }

        // stuff that subclasses need to override
        public int genTestItems(String before, String after, String[] results) {
            results[0] = before + after;
            results[1] = before + " " + after;
            results[2] = before + "\u0301\u0308" + after;
            return 3;
        }

        // stuff that subclasses need to override
        boolean skipType(int type) {
            return type == LB_AI || type == LB_SA || type == LB_SG || type == LB_XX;
        }

        // stuff that subclasses need to override
        public String getTypeID(int cp, boolean recommended) {
            byte result = getType(cp, recommended);
            if (result == LB_SUP) return "SUP";
            if (result >= LB_LIMIT) return hNames[result - LB_LIMIT];
            return Default.ucd.getLineBreakID_fromIndex(result); // AsmusOrderToMyOrder[result]);
        }

        // stuff that subclasses need to override
        public byte getType(int cp, boolean recommended) {
            if (cp > 0xFFFF) return LB_SUP;
            byte result = getHangulType(cp);
            if (result != hNot) return (byte)(result + LB_LIMIT);
            // return MyOrderToAsmusOrder[Default.ucd.getLineBreak(cp)];
            return Default.ucd.getLineBreak(cp);
        }

        public String getTableEntry(String before, String after, boolean recommended, String[] ruleOut) {
            String t = "_";
            boolean spaceBreak = isBreak(before + " " + after, before.length() + 1, recommended);
            String spaceRule = rule;

            boolean spaceBreak2 = isBreak(before + " " + after, before.length(), recommended);
            String spaceRule2 = rule;

            boolean normalBreak = isBreak(before + after, before.length(), recommended);
            String normalRule = rule;

            if (!normalBreak) {
                if (!spaceBreak && !spaceBreak2) {
                    t = "^";
                    rule = spaceRule.equals(normalRule) ? normalRule : spaceRule + "/" + normalRule;
                    if (!spaceRule2.equals(normalRule) && !spaceRule2.equals(spaceRule)) {
                        rule += "/" + spaceRule2;
                    }
                } else {
                    t = "%";
                    rule = normalRule;
                }
            }
            ruleOut[0] = rule;
            return t;
        }


        public byte getResolvedType (int cp, boolean recommended) {
            // LB 1  Assign a line break category to each character of the input.
            // Resolve AI, CB, SA, SG, XX into other line break classes depending on criteria outside this algorithm.
            byte result = getType(cp);
            switch (result) {
                case LB_AI: result = LB_AI; break;
                // case LB_CB: result = LB_ID; break;
                case LB_SA: result = LB_AL; break;
                // case LB_SG: result = LB_XX; break; Surrogates; will never occur
                case LB_XX: result = LB_AL; break;
            }
            if (recommended) {
                if (getHangulType(cp) != hNot) {
                        result = LB_ID;
                }
            }

            return result;
        }

        // find out whether there is a break at offset
        // WARNING: as a side effect, sets "rule"

        public boolean isBreak(String source, int offset, boolean recommended) {

            // LB 1  Assign a line break category to each character of the input.
            // Resolve AI, CB, SA, SG, XX into other line break classes depending on criteria outside this algorithm.
            // this is taken care of in the getResolvedType function

            // LB 2a  Never break at the start of text

            rule="2a";
            if (offset <= 0) return false;

            // LB 2b  Always break at the end of text

            rule="2b";
            if (offset >= source.length()) return true;


            // UTF-16: never break in the middle of a code point
            if (!onCodepointBoundary(source, offset)) return false;


            // now get the character before and after, and their types


            int cpBefore = UTF16.charAt(source, offset-1);
            int cpAfter = UTF16.charAt(source, offset);

            byte before = getResolvedType(cpBefore, recommended);
            byte after = getResolvedType(cpAfter, recommended);


            rule="3a";
            // Always break after hard line breaks (but never between CR and LF).
            // CR ^ LF
            if (before == LB_CR && after == LB_LF) return false;
            if (before == LB_BK || before == LB_LF || before == LB_CR) return true;

            //LB 3b  Don’t break before hard line breaks.
            rule="3b";
            if (after == LB_BK || after == LB_LF | after == LB_CR) return false;

            // LB 4  Don’t break before spaces or zero-width space.
            // × SP
            // × ZW

            rule="4";
            if (after == LB_SP || after == LB_ZW) return false;

            // LB 5 Break after zero-width space.
            // ZW ÷
            rule="5";
            if (before == LB_ZW) return true;

            // LB 6  Don’t break graphemes (before combining marks, around virama or on sequences of conjoining Jamos.
            rule="6";
            if (after == LB_CM) return false;

            if (before == LB_L && (after == LB_L || after == LB_V || after == LB_LV || after == LB_LVT)) return false;

            if ((before == LB_LV || before == LB_V) && (after == LB_V || after == LB_T)) return false;

            if ((before == LB_LVT || before == LB_T) && (after == LB_T)) return false;

            boolean setBase = false;
            if (before == LB_CM) {
                setBase = true;
                int backOffset = findLastNon(source, offset, LB_CM, recommended);
                if (backOffset < 0) {
                    before = LB_ID;
                } else {
                    before = getResolvedType(UTF16.charAt(source, backOffset), recommended);
                }
            }

            // LB 7  In all of the following rules, if a space is the base character for a combining mark,
            // the space is changed to type ID. In other words, break before SP CM* in the same cases as
            // one would break before an ID.
            rule="7";
            if (setBase && before == LB_SP) before = LB_ID;

            // LB 8  Don’t break before ‘]’ or ‘!’ or ‘;’ or ‘/’,  even after spaces.
            // × CL, × EX, × IS, × SY
            rule="8";
            if (after == LB_CL || after == LB_EX || after == LB_SY | after == LB_IS) return false;


            // find the last non-space character; we will need it
            byte lastNonSpace = before;
            if (lastNonSpace == LB_SP) {
                int backOffset = findLastNon(source, offset, LB_CM, recommended);
                if (backOffset >= 0) {
                    lastNonSpace = getResolvedType(UTF16.charAt(source, backOffset), recommended);
                }
            }

            // LB 9  Don’t break after ‘[’, even after spaces.
            // OP SP* ×
            rule="9";
            if (lastNonSpace == LB_OP) return false;

            // LB 10  Don’t break within ‘”[’, , even with intervening spaces.
            // QU SP* × OP
            rule="10";
            if (lastNonSpace == LB_QU && after == LB_OP) return false;

            // LB 11  Don’t break within ‘]h’, even with intervening spaces.
            // CL SP* × NS
            rule="11";
            if (lastNonSpace == LB_CL && after == LB_NS) return false;

            // LB 11a  Don’t break within ‘——’, even with intervening spaces.
            // B2 × B2
            rule="11a";
            if (lastNonSpace == LB_B2 && after == LB_B2) return false;


            if (recommended) {
                // LB 13  Don’t break before or after NBSP or WORD JOINER
                // × GL
                // GL ×

                rule="11b";
                if (after == LB_GL || before == LB_GL) return false;
            }

            // [Note: by this time, all of the "X" in the table are accounted for. We can safely break after spaces.]

            rule="12";
            // LB 12  Break after spaces
            // SP ÷

            if (before == LB_SP) return true;

            if (!recommended) {
                // LB 13  Don’t break before or after NBSP or WORD JOINER
                // × GL
                // GL ×

                rule="13";
                if (after == LB_GL || before == LB_GL) return false;
            }

            rule="14";
            // LB 14  Don’t break before or after ‘”’
            // × QU
            // QU ×
            if (before == LB_QU || after == LB_QU) return false;

            // LB 15  Don’t break before hyphen-minus, other hyphens, fixed-width spaces,
            // small kana and other non- starters,  or after acute accents:
            // × BA
            // × HY
            // × NS
            // BB ×

            if (recommended) {
            // LB 14a  Break before and after CB
            // CB ÷
            // ÷ CB
                if (before == LB_CB || after == LB_CB) return true;

            }

            rule="15";
            if (after == LB_NS) return false;
            if (after == LB_HY) return false;
            if (after == LB_BA) return false;
            if (before == LB_BB) return false;

            if (!recommended) {
                // LB 15b  Break after hyphen-minus, and before acute accents:
                // HY ÷
                // ÷ BB

                rule="15b";
                if (before == LB_HY) return true;
                if (after == LB_BB) return true;
            }

            // LB 16  Don’t break between two ellipses, or between letters or numbers and ellipsis:
            // AL × IN
            // ID × IN
            // IN × IN
            // NU × IN
            // Examples: ’9...’, ‘a...’, ‘H...’
            rule="16";
            if ((before == LB_NU || before == LB_AL || before == LB_ID) && after == LB_IN) return false;
            if (before == LB_IN && after == LB_IN) return false;

            // Don't break alphanumerics.
            // LB 17  Don’t break within ‘a9’, ‘3a’, or ‘H%’
            // ID × PO
            // AL × NU
            // NU × AL
            // Numbers are of the form PR ? ( OP | HY ) ? NU (NU | IS) * CL ?  PO ?
            // Examples:   $(12.35)    2,1234    (12)¢    12.54¢
            // This is approximated with the following rules. (Some cases already handled above,
            // like ‘9,’, ‘[9’.)
            rule="17";
            if (before == LB_ID && after == LB_PO) return false;
            if (before == LB_AL && after == LB_NU) return false;
            if (before == LB_NU && after == LB_AL) return false;

            // LB 18  Don’t break between the following pairs of classes.
            // CL × PO
            // HY × NU
            // IS × NU
            // NU × NU
            // NU × PO
            // PR × AL
            // PR × HY
            // PR × ID
            // PR × NU
            // PR × OP
            // SY × NU
            // Example pairs: ‘$9’, ‘$[’, ‘$-‘, ‘-9’, ‘/9’, ‘99’, ‘,9’,  ‘9%’ ‘]%’

            rule="18";
            if (before == LB_CL && after == LB_PO) return false;
            if (before == LB_HY && after == LB_NU) return false;
            if (before == LB_IS && after == LB_NU) return false;
            if (before == LB_NU && after == LB_NU) return false;
            if (before == LB_NU && after == LB_PO) return false;

            if (before == LB_PR && after == LB_AL) return false;
            if (before == LB_PR && after == LB_HY) return false;
            if (before == LB_PR && after == LB_ID) return false;
            if (before == LB_PR && after == LB_NU) return false;
            if (before == LB_PR && after == LB_OP) return false;

            if (before == LB_SY && after == LB_NU) return false;

            if (recommended) {
                // LB 15b  Break after hyphen-minus, and before acute accents:
                // HY ÷
                // ÷ BB

                rule="18b";
                if (before == LB_HY) return true;
                if (after == LB_BB) return true;
            }

            // LB 19  Don’t break between alphabetics (“at”)
            // AL × AL

            rule="19";
            if (before == LB_AL && after == LB_AL) return false;

            // LB 20  Break everywhere else
            // ALL ÷
            // ÷ ALL

            rule="20";
            return true;
        }
    }

    //==============================================

    static class GenerateGraphemeBreakTest extends GenerateBreakTest {

        static final byte CR = 0, LF = 1, Control = 2, Extend = 3, Link = 4, CGJ = 5, Base = 6, LetterBase = 7, Other = 8,
            oLIMIT = 9, // RESET THIS IF LIST ABOVE CHANGES!
            L = oLIMIT + hL, V = oLIMIT + hV, T = oLIMIT + hT, LV = oLIMIT + hLV, LVT = oLIMIT + hLVT,
            LIMIT = LVT + 1;

        static final String[] Names = {"CR", "LF", "CTL", "Extend", "Link", "CGJ", "Base", "LetterBase", "Other" };

        static UnicodeProperty extendProp = UnifiedBinaryProperty.make(DERIVED | GraphemeExtend);
        static UnicodeProperty baseProp = UnifiedBinaryProperty.make(DERIVED | GraphemeBase);
        static UnicodeProperty linkProp = UnifiedBinaryProperty.make(BINARY_PROPERTIES | GraphemeLink);
        static UnicodeSet otherExtendSet = UnifiedBinaryProperty.make(BINARY_PROPERTIES | Other_GraphemeExtend).getSet()
            .addAll(new UnicodeSet("[\u09BE\u09D7\u0B3E\u0B57\u0BD7\u0BBE"
            + "\u0CC2\u0CD5\u0CD6\u0D3E\u0D57\u0DCF\u0DDF\\U0001D165\\U0001D16E\\U0001D16F]"));

        {
            fileName = "GraphemeCluster";
        }

        // stuff that subclasses need to override
        public String getTypeID(int cp, boolean recommended) {
            byte type = getType(cp, recommended);
            if (type >= oLIMIT) return hNames[type - oLIMIT];
            return Names[type];
        }

        // stuff that subclasses need to override
        public byte getType(int cp, boolean recommended) {
            // single characters
            if (cp == 0xA) return LF;
            if (cp == 0xD) return CR;
            if (recommended) {
                if (cp == 0x034F) return CGJ;
            }
            if (cp == 0x2028 || cp == 0x2029) return Control;

            // Hangul
            byte result = getHangulType(cp);
            if (result != hNot) return (byte)(result + oLIMIT);

            // other properties
            // category based
            byte cat = Default.ucd.getCategory(cp);
            if (cat == Cc) return Control;
            if (recommended) {
                if (cat == Cf) return Control;
                if (cat == Me || cat == Mn) return Extend;
                if (otherExtendSet.contains(cp)) return Extend;
                return Base;
            }
            if (cat == Cf) return Extend;

            if (((1<<cat) & LETTER_MASK) != 0) return LetterBase;

            // other binary properties

            if (linkProp.hasValue(cp)) return Link;
            if (extendProp.hasValue(cp)) return Extend;
            if (baseProp.hasValue(cp)) return Base;

            return Other;
        }

        static public class Context {
            public int cpBefore2, cpBefore, cpAfter, cpAfter2;
        }

        public void getGraphemeBases(String source, int offset, boolean recommended, Context context) {
            context.cpBefore2 = context.cpBefore = context.cpAfter = context.cpAfter2 = -1;
            if (false) {
                context.cpBefore = UTF16.charAt(source, offset-1);
                context.cpAfter = UTF16.charAt(source, offset);

                int b2Offset = offset - UTF16.getCharCount(context.cpBefore) - 1;
                context.cpBefore2 = b2Offset < 0 ? -1 : UTF16.charAt(source, b2Offset);

                int a2Offset = offset + UTF16.getCharCount(context.cpAfter);
                context.cpAfter2 = a2Offset >= source.length() ? -1 : UTF16.charAt(source, a2Offset);
            } else {
                if (DEBUG) {
                    System.out.println("stop here");
                }
                int a1 = next(source, offset, recommended);
                context.cpAfter = findFirstBase(source, offset, a1);

                int b1 = previous(source, offset, recommended);
                context.cpBefore = findFirstBase(source, b1, offset);

                int a2 = next(source, a1, recommended);
                if (a2 != DONE) context.cpAfter2 = findFirstBase(source, a1, a2);

                int b2 = previous(source, b1, recommended);
                if (b2 != DONE) context.cpBefore2 = findFirstBase(source, b2, b1);
            }
        }

        public boolean isBreak(String source, int offset, boolean recommended) {
            rule="1";
            if (offset < 0 || offset > source.length()) return false;
            if (offset == 0) return true;

            rule = "2";
            if (offset == source.length()) return true;

            // UTF-16: never break in the middle of a code point
            if (!onCodepointBoundary(source, offset)) return false;

            // now get the character before and after, and their types


            int cpBefore = UTF16.charAt(source, offset-1);
            int cpAfter = UTF16.charAt(source, offset);

            byte before = getResolvedType(cpBefore, recommended);
            byte after = getResolvedType(cpAfter, recommended);

            rule = "3";
            if (before == CR && after == LF) return false;

            rule = "4";
            if (before == CR || before == LF || before == Control
                || after == Control || after == LF || after == CR) return true;

            rule = "6";
            if (before == L && (after == L || after == V || after == LV || after == LVT)) return false;

            rule = "7";
            if ((before == LV || before == V) && (after == V || after == T)) return false;

            rule = "8";
            if ((before == LVT || before == T) && (after == T)) return false;

            rule = "9";
            if (after == Extend) return false;

            if (recommended) {
                if (after == Link || after == CGJ) return false;
            } else {

                // Do not break around a CGJ.
                rule = "10";
                if (before == CGJ && (after == Base
                    || after == LetterBase || after == L || after == V || after == T || after == LV || after == LVT)) return false;
                rule = "11";
                if (after == CGJ) return false;

                // Do not break between linking characters and letters, or before linking characters. This provides for Indic graphemes, where virama (halant) will link character clusters together.

                rule = "12";
                //Link Extend* × LetterBase  (12)
                if (after == LetterBase || after == L || after == V || after == T || after == LV || after == LVT) {
                    int backOffset = findLastNon(source, offset, Extend, recommended);
                    if (backOffset >= 0) {
                        byte last = getResolvedType(UTF16.charAt(source, backOffset), recommended);
                        if (last == Link) return false;
                    }
                }

                rule = "13";
                if (after == Link) return false;
            }

            // Otherwise break after all characters.
            rule = "14";
            return true;

        }

    }

    //==============================================

    static class GenerateWordBreakTest extends GenerateBreakTest {

        static final byte Hiragana = 0, Katakana = 1, Letter = 2, MidLetter = 3, Hyphen = 4,
            Numeric = 5, Infix_Numeric = 6, Prefix_Numeric = 7, Postfix_Numeric = 8,
            Prefix = 9, Postfix = 10, Other = 11,
            LIMIT = Other + 1;

        static final String[] Names = {"Hiragana", "Katakana", "Letter", "MidLetter", "Hyphen",
            "Numeric", "INum", "PrNum", "PoNum", "PreLet", "PostLet", "Other" };

        GenerateGraphemeBreakTest grapheme = new GenerateGraphemeBreakTest();
        GenerateGraphemeBreakTest.Context context = new GenerateGraphemeBreakTest.Context();

        static String LENGTH = "[\u30FC\uFF70]";
        static String HALFWIDTH_KATAKANA = "[\uFF65-\uFF9F]";
        static String KATAKANA_ITERATION = "[\u30FD\u30FE]";
        static String HIRAGANA_ITERATION = "[\u309D\u309E]";

        static UnicodeSet extraKatakana = new UnicodeSet("[" + LENGTH + HALFWIDTH_KATAKANA + KATAKANA_ITERATION + "]");

        static UnicodeProperty LineBreakIdeographic = UnifiedBinaryProperty.make(LINE_BREAK | LB_ID);
        static UnicodeProperty baseProp = UnifiedBinaryProperty.make(DERIVED | GraphemeBase);
        static UnicodeProperty linkProp = UnifiedBinaryProperty.make(BINARY_PROPERTIES | GraphemeLink);

        static UnicodeSet prefixSet = UnifiedBinaryProperty.make(BINARY_PROPERTIES | Logical_Order_Exception).getSet();
        static UnicodeSet postfixSet;
        static UnicodeSet exceptionLetters;
        static UnicodeSet normalLetters;
        static UnicodeSet thaiLaoNormal;

        static UnicodeSet marks = UnifiedBinaryProperty.make(CATEGORY | Mn).getSet()
                .addAll(UnifiedBinaryProperty.make(CATEGORY | Me).getSet());

        static UnicodeSet oughtToBeLm = new UnicodeSet("[\u02B9-\u02BA\u02C2-\u02CF\u02D2-\u02DF\u02E5-\u02ED]");
/*
U+02B9..U+02BA  # MODIFIER LETTER PRIME..MODIFIER LETTER DOUBLE PRIME
U+02C2..U+02CF  # MODIFIER LETTER LEFT ARROWHEAD..MODIFIER LETTER LOW ACUTE ACCENT
U+02D2..U+02DF  # MODIFIER LETTER CENTRED RIGHT HALF RING..MODIFIER LETTER CROSS ACCE
U+02E5..U+02ED  # MODIFIER LETTER EXTRA-HIGH TONE BAR..MODIFIER LETTER UNASPIRATED
*/

        static UnicodeSet letterSet = UnifiedBinaryProperty.make(CATEGORY | Lo).getSet()
                .addAll(UnifiedBinaryProperty.make(CATEGORY | Lu).getSet())
                .addAll(UnifiedBinaryProperty.make(CATEGORY | Lt).getSet())
                .addAll(UnifiedBinaryProperty.make(CATEGORY | Ll).getSet())
                .addAll(UnifiedBinaryProperty.make(CATEGORY | Lm).getSet())
                .addAll(UnifiedBinaryProperty.make(CATEGORY | Mc).getSet())
                .addAll(oughtToBeLm);

        {

            UnicodeSet linebreakNS = UnifiedBinaryProperty.make(LINE_BREAK | LB_NS).getSet();

            postfixSet = new UnicodeSet(linebreakNS)
                .retainAll(letterSet)
                .addAll(new UnicodeSet("[\u0e30-\u0E3A\u0e45-\u0e4e]"))
                .addAll(new UnicodeSet("[[\u0eb0-\u0EBd\u0ec6-\u0ece]-[:Cn:]]"))
                .removeAll(new UnicodeSet("[:mn:]"))
                .removeAll(new UnicodeSet("[:katakana:]").addAll(extraKatakana));

            thaiLaoNormal = new UnicodeSet("[[:thai:][:lao:]]").removeAll(prefixSet).removeAll(postfixSet);

            // we want ideographics, hiragana, thai (except prefix/suffix)
            UnicodeSet compatIdeographics = new UnicodeSet("[\uf900-\ufa6a\\U0002F800-\\U0002FA1D]");

            UnicodeSet hiragana = UnifiedBinaryProperty.make(SCRIPT | HIRAGANA_SCRIPT).getSet();
            UnicodeSet smallHiragana = new UnicodeSet(hiragana).retainAll(linebreakNS);

            exceptionLetters = UnifiedBinaryProperty.make(BINARY_PROPERTIES | Ideographic).getSet()
                .addAll(new UnicodeSet("[[:thai:][:lao:]]"))
                .addAll(compatIdeographics)
                .addAll(hiragana)
                .addAll(thaiLaoNormal);

            normalLetters = new UnicodeSet(letterSet).removeAll(exceptionLetters);

            UnicodeSet missingKatakana = new UnicodeSet(extraKatakana).removeAll(new UnicodeSet("[:katakana:]"));
            
            if (DEBUG) {
                System.out.println("compatIdeographics: " + compatIdeographics.toPattern(true));
                Utility.showSetNames("", compatIdeographics, false, Default.ucd);

                System.out.println("missingKatakana: " + missingKatakana.toPattern(true));
                Utility.showSetNames("", missingKatakana, false, Default.ucd);


                System.out.println("oughtToBeLm: " + oughtToBeLm.toPattern(true));
                Utility.showSetNames("", oughtToBeLm, false, Default.ucd);


                System.out.println("Prefix: " + prefixSet.toPattern(true));
                Utility.showSetNames("", prefixSet, false, Default.ucd);

                System.out.println("Postfix: " + postfixSet.toPattern(true));
                Utility.showSetNames("", postfixSet, false, Default.ucd);

                System.out.println("exceptionLetters: " + exceptionLetters.toPattern(true));

                System.out.println("hiragana: " + hiragana.toPattern(true));

                System.out.println("smallHiragana: " + hiragana.toPattern(true));
                Utility.showSetNames("", smallHiragana, true, Default.ucd);

                System.out.println("midLetterSet: " + midLetterSet.toPattern(true));
                Utility.showSetNames("", midLetterSet, true, Default.ucd);
            }


            fileName = "Word";
            extraSamples = new String[] {
                "\uFF70", "\uFF65", "\u30FD"
            };
            if (DEBUG) {
                System.out.println("length not covered: "
                    + new UnicodeSet(LENGTH).removeAll(new UnicodeSet("[:katakana:]")).toPattern(true));
                System.out.println("half-width not covered: "
                    + new UnicodeSet(HALFWIDTH_KATAKANA).removeAll(new UnicodeSet("[:katakana:]")).toPattern(true));
                System.out.println("k.iteration not covered: "
                    + new UnicodeSet(KATAKANA_ITERATION).removeAll(new UnicodeSet("[:katakana:]")).toPattern(true));
                System.out.println("h.iteration not covered: "
                    + new UnicodeSet(HIRAGANA_ITERATION).removeAll(new UnicodeSet("[:hiragana:]")).toPattern(true));
                System.out.println("L1: " + getTypeID('\u30FC'));
                System.out.println("L2: " + getTypeID('\uFF70'));
            }

            extraSingleSamples = new String[] {"can't", "can\u2019t", "ab\u00ADby", "a$-3.14%b", "3a" };

        }

        // stuff that subclasses need to override
        public String getTypeID(int cp, boolean recommended) {
            byte type = getType(cp, recommended);
            return Names[type];
        }

        // stuff that subclasses need to override
        public byte getType(int cp, boolean recommended) {
            byte cat = Default.ucd.getCategory(cp);
            byte script = Default.ucd.getScript(cp);

            if (recommended) {
                //if (prefixSet.contains(cp)) return Prefix;
                //if (postfixSet.contains(cp)) return Postfix;
                //if (exceptionLetters.contains(cp)) return XLetter;
            }

            boolean isCatLetter = ((1<<cat) & LETTER_MASK) != 0;
            if (!recommended) {
                if (script == HIRAGANA_SCRIPT) return Hiragana;
            } else {
                if (script == HIRAGANA_SCRIPT) return Other;
            }
            if (extraKatakana.contains(cp)) return Katakana;

            if (script == KATAKANA_SCRIPT) return Katakana;

            byte lb = Default.ucd.getLineBreak(cp);
            if (!recommended) {
                if ((isCatLetter || cat == Sk) && lb != LB_ID) return Letter;
            } else {
                if (normalLetters.contains(cp)) return Letter;
            }

            if (lb == LB_HY) return Hyphen;
            if (lb == LB_NU) return Numeric;
            if (lb == LB_IS) return Infix_Numeric;
            if (lb == LB_PR) return Prefix_Numeric;
            if (lb == LB_PO) return Postfix_Numeric;

            if (midLetterSet.contains(cp)) return MidLetter;

            return Other;
        }

        public int genTestItems(String before, String after, String[] results) {
            results[0] = before + after;
            results[1] = 'a' + before + "\u0301\u0308" + after + "\u0301\u0308" + 'a';
            results[2] = 'a' + before + "\u0301\u0308" + samples[MidLetter] + after + "\u0301\u0308" + 'a';
            results[3] = 'a' + before + "\u0301\u0308" + samples[Infix_Numeric] + after + "\u0301\u0308" + 'a';
            return 3;
        }

        public boolean isBreak(String source, int offset, boolean recommended) {

            if (offset < 0 || offset > source.length()) return false;
            rule="16";
            if (offset == 0) return true;

            rule="15";
            if (offset == source.length()) return true;

            // Treat a grapheme cluster as if it were a single character:
            // the first base character, if there is one; otherwise the first character.
            // GC => FB

            rule="1";
            if (!grapheme.isBreak( source,  offset,  recommended)) return false;

            // now get the base character before and after, and their types

            grapheme.getGraphemeBases(source, offset, recommended, context);

            byte before = getResolvedType(context.cpBefore, recommended);
            byte after = getResolvedType(context.cpAfter, recommended);
            byte before2 = context.cpBefore2 < 0 ? (byte)-1 : getResolvedType(context.cpBefore2, recommended);
            byte after2 = context.cpAfter2 < 0 ? (byte)-1 : getResolvedType(context.cpAfter2, recommended);

            //Don't break between most letters
            // Letter × Letter

            rule = "2";
            if (before == Letter && after == Letter) return false;

            // Don’t break letters across certain punctuation
            // Letter × MidLetter Letter (3)
            // Letter MidLetter × Letter (4)

            /*if (recommended) {
                rule = "2a";
                if (before == Prefix && after == Letter) return false;

                rule = "2b";
                if (before == Letter && after == Postfix) return false;
            }
            */


            rule = "3";
            if (before == Letter && after == MidLetter && after2 == Letter) return false;

            rule = "4";
            if (before2 == Letter && before == MidLetter && after == Letter) return false;

            // Don’t break within sequences of digits, or digits adjacent to letters.

            // Numeric × Numeric (5)
            rule = "5";
            if (before == Numeric && after == Numeric) return false;

            // Letter × Numeric (6)
            rule = "6";
            if (before == Letter && after == Numeric) return false;

            // Numeric × Letter (7)
            rule = "7";
            if (before == Numeric && after == Letter) return false;


            // Don’t break within sequences like: '-3.2'

            // Hyphen × Numeric (8)
            rule = "8";
            if (before == Hyphen && after == Numeric) return false;

            // Numeric Infix_Numeric × Numeric (9)
            rule = "9";
            if (before2 == Numeric && before == Infix_Numeric && after == Numeric) return false;

            // Numeric × Infix_Numeric Numeric (10)
            rule = "10";
            if (before == Numeric && after == Infix_Numeric && after2 == Numeric) return false;

            // Prefix_Numeric × Numeric (11)
            rule = "11";
            if (before == Prefix_Numeric && after == Numeric) return false;

            // Numeric × Postfix_Numeric (12)
            rule = "12";
            if (before == Numeric && after == Postfix_Numeric) return false;

            // Don't break between Hiragana or Katakana

            if (!recommended) {
                // Hiragana × Hiragana (13)
                rule = "13";
                if (before == Hiragana && after == Hiragana) return false;
            }

            // Katakana × Katakana (14)
            rule = "14";
            if (before == Katakana && after == Katakana) return false;

            // Otherwise break always.
            rule = "15";
            return true;

        }

    }
}

