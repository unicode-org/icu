/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/unicodetools/com/ibm/text/UCD/GenerateBreakTest.java,v $
* $Date: 2002/08/09 23:56:24 $
* $Revision: 1.2 $
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

    // quick & dirty routine
    String insertEverywhere(String source, String insertion, GenerateBreakTest breaker) {
        String result = insertion;
        for (int i = 0; i < source.length(); ++i) {
            result += source.charAt(i);
            if (breaker.isBreak(source, i, true)) {
                result += insertion;
            }
        }
        return result + insertion;
    }


    static UnicodeSet midLetterSet = new UnicodeSet("[\u0027\u002E\u003A\u00AD\u05F3\u05F4\u2019\uFE52\uFE55\uFF07\uFF0E\uFF1A]");

    static UnicodeSet ambigSentPunct = new UnicodeSet("[\u002E\u0589\u06D4]");

    static UnicodeSet sentPunct = new UnicodeSet("[\u0021\u003F\u0387\u061F\u0964\u203C\u203D\u2048\u2049"
        + "\u3002\ufe52\ufe57\uff01\uff0e\uff1f\uff61]");
        
    static { 
        Default.setUCD();
    }
        
    static UnicodeSet extraAlpha = new UnicodeSet("[\\u02B9-\\u02BA\\u02C2-\\u02CF\\u02D2-\\u02DF\\u02E5\\u02ED\\u05F3]");
    static UnicodeSet alphabeticSet = UnifiedBinaryProperty.make(DERIVED | PropAlphabetic).getSet()
        .addAll(extraAlpha);
        
    static UnicodeSet ideographicSet = UnifiedBinaryProperty.make(BINARY_PROPERTIES | Ideographic).getSet();
    
    static {
        System.out.println("alphabetic: " + alphabeticSet.toPattern(true));
    }
    

    // ====================== Main ===========================
    
    static final boolean SHOW_TYPE = false;


    public static void main(String[] args) throws IOException {
        System.out.println("Remember to add length marks (half & full) and other punctuation for sentence, with FF61");
        Default.setUCD();
        
        if (DEBUG) {
            checkDecomps();

            Utility.showSetNames("", new UnicodeSet("[\u034F\u00AD\u1806[:DI:]-[:Cs:]-[:Cn:]]"), true, Default.ucd);

            System.out.println("*** Extend - Cf");

            generateTerminalClosure();

            GenerateWordBreakTest gwb = new GenerateWordBreakTest();
            PrintWriter systemPrintWriter = new PrintWriter(System.out);
            gwb.printLine(systemPrintWriter, "n\u0308't", true, true, false);
            systemPrintWriter.flush();
        }
        
        if (false) {
            GenerateSentenceBreakTest foo = new GenerateSentenceBreakTest();
            foo.isBreak("(\"Go.\") (He did)", 5, true);
        
            showSet("sepSet", GenerateSentenceBreakTest.sepSet);
            showSet("atermSet", GenerateSentenceBreakTest.atermSet);
            showSet("termSet", GenerateSentenceBreakTest.termSet);
        }

        new GenerateSentenceBreakTest().run();
        
        //if (true) return; // cut short for now
        
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
    
    static void showSet(String title, UnicodeSet set) {
        System.out.println(title + ": " + set.toPattern(true));
        Utility.showSetNames("", set, false, Default.ucd);
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
        out.println("<body bgcolor='#FFFFFF'><h3>Current:</h3>");


        if (recommendedDiffers()) {
            generateTable(out, false);
            out.println("<h3>Recommended:</h3>");
            generateTable(out, true);
            out.println("</body></html>");
        } else {
            generateTable(out, true);
        }
        out.close();
        
        if (recommendedDiffers()) {
            generateTest(false, false);
        }
        generateTest(false, true);

    }
    
    public void generateTest(boolean shortVersion, boolean recommended) throws IOException {
        String[] testCase = new String[50];
        // do main test

        PrintWriter out = Utility.openPrintWriter(fileName + "BreakTest" 
            + (recommended & recommendedDiffers() ? "_NEW" : "")
            + (shortVersion ? "_SHORT" : "")
            + ".txt", Utility.LATIN1_WINDOWS);
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
                    printLine(out, testCase[q], !shortVersion && q == 0, recommended, false);
                    ++counter;
                }
            }
        }

        for (int ii = 0; ii < extraSingleSamples.length; ++ii) {
            printLine(out, extraSingleSamples[ii], true, recommended, false);
        }
        out.println("# Lines: " + counter);
        out.close();
    }

    public void sampleDescription(PrintWriter out) {}

    abstract public boolean isBreak(String source, int offset, boolean recommended);

    abstract public byte getType (int cp, boolean recommended);

    abstract public String getTypeID(int s, boolean recommended);

    public boolean recommendedDiffers() {
        return false;
    }

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
        String width = "width='" + (100 / (tableLimit + 2)) + "%'";
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
        for (int type = 0; type < sampleLimit; ++type) {
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
                printLine(out, extraSingleSamples[ii], true, recommended, true);
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

        if (extraSamples.length > 0) {
            System.arraycopy(extraSamples, 0, samples, sampleLimit, extraSamples.length);
            sampleLimit += extraSamples.length;
        }
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
            extraSingleSamples = new String[] {"can't", "can\u2019t", "ab\u00ADby", "-3" };
        }


        public boolean recommendedDiffers() {
            return true;
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
            recommended = true; // don't care about old stuff
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
                // FOR FUTURE! if (otherExtendSet.contains(cp)) return Extend;
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

        public boolean isBreak(String source, int offset, boolean recommended) {
            recommended = true; // don't care about old stuff
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

        static final byte Format = 0, Katakana = 1, ALetter = 2, MidLetter = 3, Hyphen = 4,
            Numeric = 5, Infix_Numeric = 6, Prefix_Numeric = 7, Postfix_Numeric = 8,
            Prefix = 9, Postfix = 10, MidNumLet = 11, Hiragana = 12, Other = 13,
            LIMIT = Other + 1;

        static final String[] Names = {"Format", "Katakana", "ALetter", "MidLetter", "Hyphen",
            "Numeric", "INum", "PrNum", "PoNum", "PreLet", "PostLet", "MidNumLet", "Hiragana", "Other" };

        static GenerateGraphemeBreakTest grapheme = new GenerateGraphemeBreakTest();
        static Context context = new Context();

        static String LENGTH = "[\u30FC\uFF70]";
        static String HALFWIDTH_KATAKANA = "[\uFF65-\uFF9F]";
        static String KATAKANA_ITERATION = "[\u30FD\u30FE]";
        static String HIRAGANA_ITERATION = "[\u309D\u309E]";

        static UnicodeSet extraKatakana = new UnicodeSet("[" + LENGTH + HALFWIDTH_KATAKANA + KATAKANA_ITERATION + "]");

        //static UnicodeProperty LineBreakIdeographic = UnifiedBinaryProperty.make(LINE_BREAK | LB_ID);
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
            //UnicodeSet compatIdeographics = new UnicodeSet("[\uf900-\ufa6a\\U0002F800-\\U0002FA1D]");

            UnicodeSet hiragana = UnifiedBinaryProperty.make(SCRIPT | HIRAGANA_SCRIPT).getSet();
            UnicodeSet smallHiragana = new UnicodeSet(hiragana).retainAll(linebreakNS);


            UnicodeSet missingKatakana = new UnicodeSet(extraKatakana).removeAll(new UnicodeSet("[:katakana:]"));
            
            if (DEBUG) {
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
                "\uFF70", "\uFF65", "\u30FD", "a\u2060", "a:", "a'", "a'\u2060", "a,", "1:", "1'", "1,",  "1.\u2060"
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

            String [] temp = {"can't", "can\u2019t", "ab\u00ADby", "a$-34,567.14%b", "3a" };
            extraSingleSamples = new String [temp.length * 2];
            System.arraycopy(temp, 0, extraSingleSamples, 0, temp.length);
            for (int i = 0; i < temp.length; ++i) {
                extraSingleSamples[i+temp.length] = insertEverywhere(temp[i], "\u2060", grapheme);
            }

        }

        // stuff that subclasses need to override
        public String getTypeID(int cp, boolean recommended) {
            byte type = getType(cp, recommended);
            return Names[type];
        }

        // stuff that subclasses need to override
        public byte getType(int cp, boolean recommended) {
            byte cat = Default.ucd.getCategory(cp);
            
            if (cat == Cf) return Format;
            
            byte script = Default.ucd.getScript(cp);

            if (script == KATAKANA_SCRIPT) return Katakana;
            if (extraKatakana.contains(cp)) return Katakana;
            
            if (script == HIRAGANA_SCRIPT || script == THAI_SCRIPT || script == LAO_SCRIPT) return Other;
            if (ideographicSet.contains(cp)) return Other;

            if (alphabeticSet.contains(cp)) return ALetter;
            
            byte lb = Default.ucd.getLineBreak(cp);

            if (lb == LB_NU) return Numeric;
            
            if (midLetterSet.contains(cp)) {
                if (lb == LB_IS) return MidNumLet;
                return MidLetter;
            }
            if (lb == LB_IS) return Infix_Numeric;
            
            return Other;
        }

        public int genTestItems(String before, String after, String[] results) {
            results[0] = before + after;
            results[1] = 'a' + before + "\u0301\u0308" + after + "\u0301\u0308" + 'a';
            results[2] = 'a' + before + "\u0301\u0308" + samples[MidLetter] + after + "\u0301\u0308" + 'a';
            results[3] = 'a' + before + "\u0301\u0308" + samples[Infix_Numeric] + after + "\u0301\u0308" + 'a';
            return 3;
        }

        static public class Context {
            public int cpBefore2, cpBefore, cpAfter, cpAfter2;
            public byte tBefore2, tBefore, tAfter, tAfter2;
        }

        public void getGraphemeBases(String source, int offset, boolean recommended, Context context) {
            context.cpBefore2 = context.cpBefore = context.cpAfter = context.cpAfter2 = -1;
            context.tBefore2 = context.tBefore = context.tAfter = context.tAfter2 = -1;
            
            MyBreakIterator graphemeIterator = new MyBreakIterator();

            graphemeIterator.set(source, offset);
            while (true) {
                int cp = graphemeIterator.previousBase();
                if (cp == -1) break;
                byte t = getResolvedType(cp, recommended);
                if (t == Format) continue;
                
                if (context.cpBefore == -1) {
                    context.cpBefore = cp;
                    context.tBefore = t;
                } else {
                    context.cpBefore2 = cp;
                    context.tBefore2 = t;
                    break;
                }
            }
            graphemeIterator.set(source, offset);
            while (true) {
                int cp = graphemeIterator.nextBase();
                if (cp == -1) break;
                byte t = getResolvedType(cp, recommended);
                if (t == Format) continue;
                
                if (context.cpAfter == -1) {
                    context.cpAfter = cp;
                    context.tAfter = t;
                } else {
                    context.cpAfter2 = cp;
                    context.tAfter2 = t;
                    break;
                }
            }
        }


        public boolean isBreak(String source, int offset, boolean recommended) {
            recommended = true; // don't care about old stuff

            rule = "1";
            if (offset < 0 || offset > source.length()) return false;
  
            if (offset == 0) return true;

            rule = "2";
            if (offset == source.length()) return true;

            // Treat a grapheme cluster as if it were a single character:
            // the first base character, if there is one; otherwise the first character.
            // GC => FB

            rule="3";
            if (!grapheme.isBreak( source,  offset,  recommended)) return false;

            // now get the base character before and after, and their types

            getGraphemeBases(source, offset, recommended, context);

            byte before = context.tBefore;
            byte after = context.tAfter;
            byte before2 = context.tBefore2;
            byte after2 = context.tAfter2;

            //Don't break between most letters
            // ALetter × ALetter

            rule = "5";
            if (before == ALetter && after == ALetter) return false;

            // Don’t break letters across certain punctuation
            // ALetter×(MidLetter | MidNumLet) ALetter(6)
            // ALetter (MidLetter | MidNumLet)×ALetter(7)

            rule = "6";
            if (before == ALetter && (after == MidLetter || after == MidNumLet) && after2 == ALetter) return false;

            rule = "7";
            if (before2 == ALetter && (before == MidLetter || before == MidNumLet) && after == ALetter) return false;

            // Don’t break within sequences of digits, or digits adjacent to letters.

            // Numeric × Numeric (5)
            rule = "8";
            if (before == Numeric && after == Numeric) return false;

            // ALetter × Numeric (6)
            rule = "9";
            if (before == ALetter && after == Numeric) return false;

            // Numeric × ALetter (7)
            rule = "10";
            if (before == Numeric && after == ALetter) return false;


            // Don’t break within sequences like: '-3.2'
            // Numeric (MidNum | MidNumLet)×Numeric(11)
            rule = "11";
            if (before2 == Numeric && (before == Infix_Numeric || before == MidNumLet) && after == Numeric) return false;

            // Numeric×(MidNum | MidNumLet) Numeric(12)
            rule = "12";
            if (before == Numeric && (after == Infix_Numeric || after == MidNumLet) && after2 == Numeric) return false;

            // Don't break between Hiragana

            // Hiragana × Hiragana (13)
            rule = "13";
            if (before == Hiragana && after == Hiragana) return false;

            // Otherwise break always.
            rule = "14";
            return true;

        }

    }

    //==============================================

    static class GenerateSentenceBreakTest extends GenerateBreakTest {
        
        static final byte Format = 0, Sep = 1, Sp = 2, OLetter = 3, Lower = 4, Upper = 5,
            Close = 6, ATerm = 7, Term = 8, Other = 9,
            LIMIT = Other + 1;

        static final String[] Names = {"Format", "Sep", "Sp", "OLetter", "Lower", "Upper",
            "Close", "ATerm", "Term", "Other" };

        static GenerateGraphemeBreakTest grapheme = new GenerateGraphemeBreakTest();

        static UnicodeSet sepSet = new UnicodeSet("[\\u000a\\u000d\\u0085\\u2029\\u2028]");
        static UnicodeSet atermSet = new UnicodeSet("[\\u002E]");
        static UnicodeSet termSet = new UnicodeSet("[\\u0021\\u003F\\u0589\\u061f\\u06d4\\u0700-\\u0702\\u0934"
            + "\\u1362\\u1367\\u1368\\u1803\\u1809\\u203c\\u203d\\u2048\\u2049\\u3002\\ufe52\\ufe57\\uff01\\uff0e\\uff1f\\uff61]");
        
        static UnicodeProperty lowercaseProp = UnifiedBinaryProperty.make(DERIVED | PropLowercase);
        static UnicodeProperty uppercaseProp = UnifiedBinaryProperty.make(DERIVED | PropUppercase);
        
        {

            fileName = "Sentence";
            extraSamples = new String[] {
                
            };
            String[] temp = new String[] {
                "(\"Go.\") (He did.)", 
                "(\"Go?\") (He did.)", 
                "U.S.A\u0300. is", 
                "U.S.A\u0300? He", 
                "U.S.A\u0300.", 
                "\u4e00.\u4300",
                "\u4e00?\u4300",
            };
            extraSingleSamples = new String [temp.length * 2];
            System.arraycopy(temp, 0, extraSingleSamples, 0, temp.length);
            for (int i = 0; i < temp.length; ++i) {
                extraSingleSamples[i+temp.length] = insertEverywhere(temp[i], "\u2060", grapheme);
            }

        }
        
        // stuff that subclasses need to override
        public String getTypeID(int cp, boolean recommended) {
            byte type = getType(cp, recommended);
            return Names[type];
        }

        // stuff that subclasses need to override
        public byte getType(int cp, boolean recommended) {
            byte cat = Default.ucd.getCategory(cp);
            
            if (cat == Cf) return Format;
            if (sepSet.contains(cp)) return Sep;
            if (Default.ucd.getBinaryProperty(cp, White_space)) return Sp;
            if (alphabeticSet.contains(cp)) return OLetter;
            if (lowercaseProp.hasValue(cp)) return Lower;
            if (uppercaseProp.hasValue(cp) || cat == Lt) return Upper;
            if (atermSet.contains(cp)) return ATerm;
            if (termSet.contains(cp)) return Term;
            if (cat == Po || cat == Pe
                || Default.ucd.getLineBreak(cp) == LB_QU) return Close;
            return Other;
        }

        public int genTestItems(String before, String after, String[] results) {
            results[0] = before + after;
            /*
            results[1] = 'a' + before + "\u0301\u0308" + after + "\u0301\u0308" + 'a';
            results[2] = 'a' + before + "\u0301\u0308" + samples[MidLetter] + after + "\u0301\u0308" + 'a';
            results[3] = 'a' + before + "\u0301\u0308" + samples[Infix_Numeric] + after + "\u0301\u0308" + 'a';
            */
            return 1;
        }

        public boolean isBreak(String source, int offset, boolean recommended) {

            rule = "1";
            if (offset < 0 || offset > source.length()) return false;
  
            if (offset == 0) return true;

            rule = "2";
            if (offset == source.length()) return true;

            // Sep ÷  (3) 
            rule = "3";
            byte before = getResolvedType(source.charAt(offset-1), recommended);
            if (before == Sep) return true;
            
            // Treat a grapheme cluster as if it were a single character:
            // the first base character, if there is one; otherwise the first character.
            // GC => FB
            // Ignore interior Format characters. That is, ignore Format characters in all subsequent rules.
            // X Format*
            // ?
            // X
            // (5)

            rule="3";
            if (!grapheme.isBreak( source,  offset,  recommended)) return false;
            
            // Do not break after ambiguous terminators like period, if the first following letter is lowercase. For example, a period may be an abbreviation or numeric period, and not mark the end of a sentence.
            // ATerm Close* Sp*×(¬( OLetter | Upper ))* Lower(6)
            // ATerm ×Upper (7)
            
            // Break after sentence terminators, but include closing punctuation, trailing spaces, and (optionally) a paragraph separator.
            // ( Term | ATerm ) Close*×( Close | Sp | Sep )(8)
            // ( Term | ATerm ) Close* Sp×( Sp | Sep )(9)
            // ( Term | ATerm ) Close* Sp*÷(10)

            
            // These cases are all handled together.
            // First we loop backwards, checking for the different types.
            
            MyBreakIterator graphemeIterator = new MyBreakIterator();
            graphemeIterator.set(source, offset);
            
            int state = 0;
            byte lookAfter = -1;
            int cp;
            byte t;
            boolean gotSpace = false;
            boolean gotClose = false;
            
            behindLoop:
            while (true) {
                cp = graphemeIterator.previousBase();
                if (cp == -1) break;
                t = getResolvedType(cp, recommended);
                if (SHOW_TYPE) System.out.println(Default.ucd.getCodeAndName(cp) + ", " + getTypeID(cp, recommended));
                
                if (t == Format) continue;  // ignore all formats!
                
                switch (state) {
                    case 0:
                        if (t == Sp) {
                            // loop as long as we have Space
                            gotSpace = true;
                            continue behindLoop;
                        } else if (t == Close) {
                            gotClose = true;
                            state = 1;    // go to close loop
                            continue behindLoop;
                        }
                        break;
                    case 1:
                        if (t == Close) {
                            // loop as long as we have Close
                            continue behindLoop;
                        }
                        break;
                }
                if (t == ATerm) {
                    lookAfter = ATerm;
                } else if (t == Term) {
                    lookAfter = Term;
                }
                break;
            }
            
            // if we didn't find ATerm or Term, bail
            
            if (lookAfter == -1) {
                // Otherwise, do not break
                // Any × Any (11)
                rule = "11";
                return false;
            }
                
            // Do not break after ambiguous terminators like period, if the first following letter is lowercase. For example, a period may be an abbreviation or numeric period, and not mark the end of a sentence.
            // ATerm Close* Sp*×(¬( OLetter | Upper ))* Lower(6)
            // ATerm ×Upper (7)
            
            // Break after sentence terminators, but include closing punctuation, trailing spaces, and (optionally) a paragraph separator.
            // ( Term | ATerm ) Close*×( Close | Sp | Sep )(8)
            // ( Term | ATerm ) Close* Sp×( Sp | Sep )(9)
            // ( Term | ATerm ) Close* Sp*÷(10)
            
            // We DID find one. Loop to see if the right side is ok.

            graphemeIterator.set(source, offset);
            boolean isFirst = true;
            while (true) {
                cp = graphemeIterator.nextBase();
                if (cp == -1) break;
                t = getResolvedType(cp, recommended);
                if (SHOW_TYPE) System.out.println(Default.ucd.getCodeAndName(cp) + ", " + getTypeID(cp, recommended));
                    
                if (t == Format) continue;  // skip format characters!
                    
                if (isFirst) {
                    isFirst = false;
                    if (lookAfter == ATerm && t == Upper) {
                        rule = "7";
                        return false;
                    }
                    if (gotSpace) {
                        if (t == Sp || t == Sep) {
                            rule = "9";
                            return false;
                        }
                    } else if (t == Close || t == Sp || t == Sep) {
                        rule = "8";
                        return false;
                    }
                    if (lookAfter == Term) break;
                }
                    
                // at this point, we have an ATerm. All other conditions are ok, but we need to verify 6
                if (t != OLetter && t != Upper && t != Lower) continue;
                if (t == Lower) {
                    rule = "6";
                    return false;
                }
                break;
            }
            rule = "10";
            return true;
        }
    }
    
    static class MyBreakIterator {
        int offset = 0;
        String string = "";
        GenerateBreakTest breaker = new GenerateGraphemeBreakTest();
        boolean recommended = true;
        
        public MyBreakIterator set(String source, int offset) {
            string = source;
            this.offset = offset;
            return this;
        }
        
        public int nextBase() {
            if (offset >= string.length()) return -1;
            int result = UTF16.charAt(string, offset);
            for (++offset; offset < string.length(); ++offset) {
                if (breaker.isBreak(string, offset, recommended)) break;
            }
            return result;
        }
        
        public int previousBase() {
            if (offset <= 0) return -1;
            for (--offset; offset >= 0; --offset) {
                if (breaker.isBreak(string, offset, recommended)) break;
            }
            return UTF16.charAt(string, offset);
        }
    }
}

