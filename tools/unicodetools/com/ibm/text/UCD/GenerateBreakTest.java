/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/unicodetools/com/ibm/text/UCD/GenerateBreakTest.java,v $
* $Date: 2003/04/01 02:52:00 $
* $Revision: 1.4 $
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
        
    static UnicodeSet extraAlpha = new UnicodeSet("[\\u02B9-\\u02BA\\u02C2-\\u02CF\\u02D2-\\u02DF\\u02E5-\\u02ED\\u05F3]");
    static UnicodeSet alphabeticSet = UnifiedBinaryProperty.make(DERIVED | PropAlphabetic).getSet()
        .addAll(extraAlpha);
        
    static UnicodeSet ideographicSet = UnifiedBinaryProperty.make(BINARY_PROPERTIES | Ideographic).getSet();
    
    static {
        if (false) System.out.println("alphabetic: " + alphabeticSet.toPattern(true));
    }
    

    // ====================== Main ===========================
    
    static final boolean SHOW_TYPE = false;
    
    UnicodeMap sampleMap = null;


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
            //showSet("sepSet", GenerateSentenceBreakTest.sepSet);
            //showSet("atermSet", GenerateSentenceBreakTest.atermSet);
            //showSet("termSet", GenerateSentenceBreakTest.termSet);
        }
        
        if (true) {
            GenerateSentenceBreakTest foo = new GenerateSentenceBreakTest();
            //foo.isBreak("(\"Go.\") (He did)", 5, true);
            foo.isBreak("3.4", 2, true);
        }

        new GenerateGraphemeBreakTest().run();
        new GenerateWordBreakTest().run();
        new GenerateLineBreakTest().run();
        new GenerateSentenceBreakTest().run();
        
        //if (true) return; // cut short for now
        
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
        //System.out.println("otherExtendSet: " + ((GenerateGraphemeBreakTest)tests[0]).otherExtendSet.toPattern(true));
        //Utility.showSetNames("", ((GenerateGraphemeBreakTest)tests[0]).otherExtendSet, false, Default.ucd);
        
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

        PrintWriter out = Utility.openPrintWriter("TR29\\" + fileName + "BreakTest.html", Utility.UTF8_WINDOWS);
        out.println("<html><head><meta http-equiv='Content-Type' content='text/html; charset=utf-8'>");
        out.println("<title>" + fileName + " Break Chart</title>");
        out.println("<style>");
        out.println("td, th { vertical-align: top }");
        out.println("</style></head>");


        out.println("<body bgcolor='#FFFFFF'><h2>Sample Break Table</h2>");
        out.println("<p>Version: " + Default.ucd.getVersion() + "</p>");


        if (recommendedDiffers()) {
            generateTable(out, false);
            out.println("<h3>Recommended:</h3>");
            generateTable(out, true);
            out.println("</body></html>");
        } else {
            generateTable(out, true);
        }
        

        if (sampleMap != null) {
            out.println("<h3>Character Type Breakdown</h3>");
            out.println("<table border='1' cellspacing='0' width='100%'>");
            for (int i = 0; i < sampleMap.size(); ++i) {
                out.println("<tr><th>" + sampleMap.getLabelFromIndex(i) 
                    + "</th><td>" + sampleMap.getSetFromIndex(i)
                    + "</td></tr>");
            }
            out.println("</table>");
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

        PrintWriter out = Utility.openPrintWriter("TR29\\" + fileName + "BreakTest" 
            + (recommended & recommendedDiffers() ? "_NEW" : "")
            + (shortVersion ? "_SHORT" : "")
            + ".txt", Utility.UTF8_WINDOWS);
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
        String width = "width='" + (100 / (tableLimit + 1)) + "%'";
        out.print("<table border='1' cellspacing='0' width='100%'>");
        String types = "";
        String codes = "";
        for (int type = 0; type < tableLimit; ++type) {
            String after = samples[type];
            if (after == null) continue;

            String h = getTypeID(after, recommended);
            types += "<th " + width + " title='" + getInfo(after) + "'>" + h + "</th>";
            //codes += "<th " + width + " title='" + getInfo(after) + "'>" + Utility.hex(after) + "</th>";
        }

        out.println("<tr><th " + width + "></th>" + types + "</tr>");
        // out.println("<tr><th " + width + "></th><th " + width + "></th>" + codes + "</tr>");

        String[] rule = new String[1];
        String[] rule2 = new String[1];
        for (int type = 0; type < sampleLimit; ++type) {
            String before = samples[type];
            if (before == null) continue;

            String line = "<tr><th title='" + Default.ucd.getCodeAndName(before) + "'>" 
                + getTypeID(before, recommended) + "</th>";

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
            out.println("<h3>Sample Strings</h3>");
        
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

    public static UnicodeSet getSet(int prop, byte propValue) {
        return UnifiedBinaryProperty.make(prop | propValue).getSet();
    }

    static public class Context {
        public int cpBefore2, cpBefore, cpAfter, cpAfter2;
        public byte tBefore2, tBefore, tAfter, tAfter2;
        public String toString() {
            return "[" 
            + Utility.hex(cpBefore2) + "(" + tBefore2 + "), "
            + Utility.hex(cpBefore) + "(" + tBefore + "), "
            + Utility.hex(cpAfter) + "(" + tAfter + "), "
            + Utility.hex(cpAfter2) + "(" + tAfter2 + ")]";
        }
    }

    public void getGraphemeBases(String source, int offset, boolean recommended, int ignoreType, Context context) {
        context.cpBefore2 = context.cpBefore = context.cpAfter = context.cpAfter2 = -1;
        context.tBefore2 = context.tBefore = context.tAfter = context.tAfter2 = -1;
        //if (DEBUG_GRAPHEMES) System.out.println(Utility.hex(source) + "; " + offset + "; " + ignoreType);
            
        MyBreakIterator graphemeIterator = new MyBreakIterator();

        graphemeIterator.set(source, offset);
        while (true) {
            int cp = graphemeIterator.previousBase();
            if (cp == -1) break;
            byte t = getResolvedType(cp, recommended);
            if (t == ignoreType) continue;
                
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
            if (t == ignoreType) continue;
                
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


    //==============================================

    static class GenerateGraphemeBreakTest extends GenerateBreakTest {


        static final UnicodeMap map = new UnicodeMap();
        static final int
            CR =    map.add("CR",    new UnicodeSet(0xA, 0xA)),
            LF =    map.add("LF",    new UnicodeSet(0xD, 0xD)),
            Control = map.add("Control", 
                        getSet(CATEGORY, Cc)
                .addAll(getSet(CATEGORY, Cf))
                .addAll(getSet(CATEGORY, Zp))
                .addAll(getSet(CATEGORY, Zl))
                .removeAll(map.getSetFromIndex(CR))
                .removeAll(map.getSetFromIndex(LF))),
            Extend = map.add("Extend", getSet(DERIVED, GraphemeExtend)),
            L =     map.add("L",     getSet(HANGUL_SYLLABLE_TYPE, UCD_Types.L)),
            V =     map.add("V",     getSet(HANGUL_SYLLABLE_TYPE, UCD_Types.V)),
            T =     map.add("T",     getSet(HANGUL_SYLLABLE_TYPE, UCD_Types.T)),
            LV =    map.add("LV",    getSet(HANGUL_SYLLABLE_TYPE, UCD_Types.LV)),
            LVT =   map.add("LVT",   getSet(HANGUL_SYLLABLE_TYPE, UCD_Types.LVT)),
            Other = map.add("Other", new UnicodeSet(0,0x10FFFF), false, false);            
                
        {
            fileName = "GraphemeCluster";
            sampleMap = map;
        }

        // stuff that subclasses need to override
        public String getTypeID(int cp, boolean recommended) {
            return map.getLabel(cp);
        }

        // stuff that subclasses need to override
        public byte getType(int cp, boolean recommended) {
            return (byte) map.getIndex(cp);
        }

        public boolean isBreak(String source, int offset, boolean recommended) {
            recommended = true; // don't care about old stuff
            rule="1: sot ÷";
            if (offset < 0 || offset > source.length()) return false;
            if (offset == 0) return true;

            rule = "2: ÷ eot";
            if (offset == source.length()) return true;

            // UTF-16: never break in the middle of a code point
            if (!onCodepointBoundary(source, offset)) return false;

            // now get the character before and after, and their types


            int cpBefore = UTF16.charAt(source, offset-1);
            int cpAfter = UTF16.charAt(source, offset);

            byte before = getResolvedType(cpBefore, recommended);
            byte after = getResolvedType(cpAfter, recommended);

            rule = "3: CR × LF";
            if (before == CR && after == LF) return false;

            rule = "4: ( Control | CR | LF ) ÷";
            if (before == CR || before == LF || before == Control) return true;

            rule = "5: ÷ ( Control | CR | LF )";
            if (after == Control || after == LF || after == CR) return true;

            rule = "6: L × ( L | V | LV | LVT )";
            if (before == L && (after == L || after == V || after == LV || after == LVT)) return false;

            rule = "7: ( LV | V ) × ( V | T )";
            if ((before == LV || before == V) && (after == V || after == T)) return false;

            rule = "8: ( LVT | T ) × T";
            if ((before == LVT || before == T) && (after == T)) return false;

            rule = "9: × Extend";
            if (after == Extend) return false;

            // Otherwise break after all characters.
            rule = "10: Any ÷ Any";
            return true;

        }

    }

    //==============================================

    static class GenerateWordBreakTest extends GenerateBreakTest {
        
        //static String LENGTH = "[\u30FC\uFF70]";
        //static String HALFWIDTH_KATAKANA = "[\uFF66-\uFF9F]";
        //static String KATAKANA_ITERATION = "[\u30FD\u30FE]";
        //static String HIRAGANA_ITERATION = "[\u309D\u309E]";
        
        static final UnicodeMap map = new UnicodeMap();
        static final int
            Format =    map.add("Format",    getSet(CATEGORY, Cf).remove(0x00AD)),
            Katakana =    map.add("Katakana",    getSet(SCRIPT, KATAKANA_SCRIPT)
                .addAll(new UnicodeSet("[\u30FC\uFF70\uFF9E\uFF9F]"))
                //.addAll(new UnicodeSet(HALFWIDTH_KATAKANA))
                //.addAll(new UnicodeSet(KATAKANA_ITERATION))
                ),
            ALetter = map.add("ALetter", 
                        getSet(DERIVED, PropAlphabetic)
                .add(0x05F3, 0x05F3)
                .removeAll(map.getSetFromIndex(Katakana))
                .removeAll(getSet(BINARY_PROPERTIES, Ideographic))
                .removeAll(getSet(SCRIPT, THAI_SCRIPT))
                .removeAll(getSet(SCRIPT, LAO_SCRIPT))
                .removeAll(getSet(SCRIPT, HIRAGANA_SCRIPT))
                ),
            MidLetter = map.add("MidLetter", 
                new UnicodeSet("[\\u0027\\u00AD\\u00B7\\u05f4\\u05F4\\u2019\\u2027]")),
            MidNumLet =     map.add("MidNumLet",
                new UnicodeSet("[\\u002E\\u003A]")),
            MidNum =     map.add("MidNum",     getSet(LINE_BREAK, LB_IN)
                .removeAll(map.getSetFromIndex(MidNumLet))),
            Numeric =     map.add("Numeric",     getSet(LINE_BREAK, LB_NU)),
            Other = map.add("Other", new UnicodeSet(0,0x10FFFF), false, false);      
                
        

        static GenerateGraphemeBreakTest grapheme = new GenerateGraphemeBreakTest();
        static Context context = new Context();

        {
            fileName = "Word";
            sampleMap = map;
            extraSamples = new String[] {
                "\uFF70", "\uFF65", "\u30FD", "a\u2060", "a:", "a'", "a'\u2060", "a,", "1:", "1'", "1,",  "1.\u2060"
            };

            String [] temp = {"can't", "can\u2019t", "ab\u00ADby", "a$-34,567.14%b", "3a" };
            extraSingleSamples = new String [temp.length * 2];
            System.arraycopy(temp, 0, extraSingleSamples, 0, temp.length);
            for (int i = 0; i < temp.length; ++i) {
                extraSingleSamples[i+temp.length] = insertEverywhere(temp[i], "\u2060", grapheme);
            }
            
            if (false) Utility.showSetDifferences("Katakana", map.getSetFromIndex(Katakana), 
                "Script=Katakana", getSet(SCRIPT, KATAKANA_SCRIPT), false, Default.ucd);

        }

        // stuff that subclasses need to override
        public String getTypeID(int cp, boolean recommended) {
            return map.getLabel(cp);
        }

        // stuff that subclasses need to override
        public byte getType(int cp, boolean recommended) {
            return (byte) map.getIndex(cp);
        }

        public int genTestItems(String before, String after, String[] results) {
            results[0] = before + after;
            results[1] = 'a' + before + "\u0301\u0308" + after + "\u0301\u0308" + 'a';
            results[2] = 'a' + before + "\u0301\u0308" + samples[MidLetter] + after + "\u0301\u0308" + 'a';
            results[3] = 'a' + before + "\u0301\u0308" + samples[MidNum] + after + "\u0301\u0308" + 'a';
            return 3;
        }

        public boolean isBreak(String source, int offset, boolean recommended) {
            recommended = true; // don't care about old stuff

            rule = "1: sot ÷";
            if (offset < 0 || offset > source.length()) return false;
  
            if (offset == 0) return true;

            rule = "2: ÷ eot";
            if (offset == source.length()) return true;

            // Treat a grapheme cluster as if it were a single character:
            // the first base character, if there is one; otherwise the first character.
            // GC => FB

            rule="3: GC -> FB; 4: X Format* -> X";
            if (!grapheme.isBreak( source,  offset,  recommended)) return false;

            // now get the base character before and after, and their types

            getGraphemeBases(source, offset, recommended, Format, context);

            byte before = context.tBefore;
            byte after = context.tAfter;
            byte before2 = context.tBefore2;
            byte after2 = context.tAfter2;

            //Don't break between most letters

            rule = "5: ALetter × ALetter";
            if (before == ALetter && after == ALetter) return false;

            // Don’t break letters across certain punctuation

            rule = "6: ALetter × (MidLetter | MidNumLet) ALetter";
            if (before == ALetter && (after == MidLetter || after == MidNumLet) && after2 == ALetter) return false;

            rule = "7: ALetter (MidLetter | MidNumLet) × ALetter";
            if (before2 == ALetter && (before == MidLetter || before == MidNumLet) && after == ALetter) return false;

            // Don’t break within sequences of digits, or digits adjacent to letters.

            rule = "8: Numeric × Numeric";
            if (before == Numeric && after == Numeric) return false;

            rule = "9: ALetter × Numeric";
            if (before == ALetter && after == Numeric) return false;

            rule = "10: Numeric × ALetter";
            if (before == Numeric && after == ALetter) return false;


            // Don’t break within sequences like: '-3.2'
            rule = "11: Numeric (MidNum | MidNumLet) × Numeric";
            if (before2 == Numeric && (before == MidNum || before == MidNumLet) && after == Numeric) return false;

            rule = "12: Numeric × (MidNum | MidNumLet) Numeric";
            if (before == Numeric && (after == MidNum || after == MidNumLet) && after2 == Numeric) return false;

            // Don't break between Katakana

            rule = "13: Katakana × Katakana";
            if (before == Katakana && after == Katakana) return false;

            // Otherwise break always.
            rule = "14: Any ÷ Any";
            return true;

        }

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

    static class GenerateSentenceBreakTest extends GenerateBreakTest {
        
        static final UnicodeMap map = new UnicodeMap();
        static final int
            Sep =    map.add("Sep",    new UnicodeSet("[\\u000A\\u000D\\u0085\\u2028\\u2029]")),
            Format =    map.add("Format",    getSet(CATEGORY, Cf)),
            Sp = map.add("Sp", getSet(BINARY_PROPERTIES, White_space)
                .removeAll(map.getSetFromIndex(Sep))),
            Lower = map.add("Lower", getSet(DERIVED, PropLowercase)),
            Upper = map.add("Upper", getSet(CATEGORY, Lt)
                .addAll(getSet(DERIVED, PropUppercase))),
            OLetter = map.add("OLetter", 
                        getSet(DERIVED, PropAlphabetic)
                .add(0x05F3, 0x05F3)
                .removeAll(map.getSetFromIndex(Lower))
                .removeAll(map.getSetFromIndex(Upper))
                ),
            Numeric =     map.add("Numeric",     getSet(LINE_BREAK, LB_NU)),
            ATerm =     map.add("ATerm", new UnicodeSet(0x002E,0x002E)),
            Term =    map.add("Term", new UnicodeSet(
                "[\\u0021\\u003F\\u0589\\u061F\\u06D4\\u0700\\u0701\\u0702\\u0964\\u1362\\u1367"
                + "\\u1368\\u104A\\u104B\\u166E\\u1803\\u1809\\u203C\\u203D\\u2047\\u2048\\u2049"
                + "\\u3002\\uFE52\\uFE57\\uFF01\\uFF0E\\uFF1F\\uFF61]")),
            Close =     map.add("Close",     
                getSet(CATEGORY, Po)
                .addAll(getSet(CATEGORY, Pe))
                .addAll(getSet(LINE_BREAK, LB_QU))
                .removeAll(map.getSetFromIndex(ATerm))
                .removeAll(map.getSetFromIndex(Term))
                .remove(0x05F3)
                ),
            Other = map.add("Other", new UnicodeSet(0,0x10FFFF), false, false);            
                
        {
            fileName = "GraphemeCluster";
            sampleMap = map;
        }

        // stuff that subclasses need to override
        public String getTypeID(int cp, boolean recommended) {
            return map.getLabel(cp);
        }

        // stuff that subclasses need to override
        public byte getType(int cp, boolean recommended) {
            return (byte) map.getIndex(cp);
        }

        
        /*
        static final byte Format = 0, Sep = 1, Sp = 2, OLetter = 3, Lower = 4, Upper = 5,
            Numeric = 6, Close = 7, ATerm = 8, Term = 9, Other = 10,
            LIMIT = Other + 1;

        static final String[] Names = {"Format", "Sep", "Sp", "OLetter", "Lower", "Upper", "Numeric",
            "Close", "ATerm", "Term", "Other" };


        static UnicodeSet sepSet = new UnicodeSet("[\\u000a\\u000d\\u0085\\u2029\\u2028]");
        static UnicodeSet atermSet = new UnicodeSet("[\\u002E]");
        static UnicodeSet termSet = new UnicodeSet(
            "[\\u0021\\u003F\\u0589\\u061f\\u06d4\\u0700-\\u0702\\u0934"
            + "\\u1362\\u1367\\u1368\\u104A\\u104B\\u166E"
            + "\\u1803\\u1809\\u203c\\u203d"
            + "\\u2048\\u2049\\u3002\\ufe52\\ufe57\\uff01\\uff0e\\uff1f\\uff61]");
        
        static UnicodeProperty lowercaseProp = UnifiedBinaryProperty.make(DERIVED | PropLowercase);
        static UnicodeProperty uppercaseProp = UnifiedBinaryProperty.make(DERIVED | PropUppercase);
        
        UnicodeSet linebreakNS = UnifiedBinaryProperty.make(LINE_BREAK | LB_NU).getSet();
        */
        
        static GenerateGraphemeBreakTest grapheme = new GenerateGraphemeBreakTest();
        {

            fileName = "Sentence";
            extraSamples = new String[] {
            };
            
            extraSingleSamples = new String[] {
                "(\"Go.\") (He did.)", 
                "(\u201CGo?\u201D) (He did.)", 
                "U.S.A\u0300. is", 
                "U.S.A\u0300? He", 
                "U.S.A\u0300.", 
                "3.4", 
                "c.d",
                "etc.)\u2019 \u2018(the",
                "etc.)\u2019 \u2018(The",
                "the resp. leaders are",
                "\u5B57.\u5B57",
                "etc.\u5B83",
                "etc.\u3002",
                "\u5B57\u3002\u5B83",
            };
            String[] temp = new String [extraSingleSamples.length * 2];
            System.arraycopy(extraSingleSamples, 0, temp, 0, extraSingleSamples.length);
            for (int i = 0; i < extraSingleSamples.length; ++i) {
                temp[i+extraSingleSamples.length] = insertEverywhere(extraSingleSamples[i], "\u2060", grapheme);
            }
            extraSingleSamples = temp;

        }
        /*
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
            if (linebreakNS.contains(cp)) return Numeric;
            if (lowercaseProp.hasValue(cp)) return Lower;
            if (uppercaseProp.hasValue(cp) || cat == Lt) return Upper;
            if (alphabeticSet.contains(cp)) return OLetter;
            if (atermSet.contains(cp)) return ATerm;
            if (termSet.contains(cp)) return Term;
            if (cat == Po || cat == Pe
                || Default.ucd.getLineBreak(cp) == LB_QU) return Close;
            return Other;
        }
        */
        
        public int genTestItems(String before, String after, String[] results) {
            results[0] = before + after;
            /*
            results[1] = 'a' + before + "\u0301\u0308" + after + "\u0301\u0308" + 'a';
            results[2] = 'a' + before + "\u0301\u0308" + samples[MidLetter] + after + "\u0301\u0308" + 'a';
            results[3] = 'a' + before + "\u0301\u0308" + samples[MidNum] + after + "\u0301\u0308" + 'a';
            */
            return 1;
        }

        static Context context = new Context();
        
        public boolean isBreak(String source, int offset, boolean recommended) {
    
            // Break at the start and end of text.
            rule = "1: sot ÷";
            if (offset < 0 || offset > source.length()) return false;
  
            if (offset == 0) return true;

            rule = "2: ÷ eot";
            if (offset == source.length()) return true;

            rule = "3: Sep ÷";
            byte beforeChar = getResolvedType(source.charAt(offset-1), recommended);
            if (beforeChar == Sep) return true;
            
            // Treat a grapheme cluster as if it were a single character:
            // the first base character, if there is one; otherwise the first character.
            // GC => FB
            // Ignore interior Format characters. That is, ignore Format characters in all subsequent rules.
            // X Format*
            // ?
            // X
            // (5)

            rule="4: GC -> FB; 5: X Format* -> X";
            if (!grapheme.isBreak( source,  offset,  recommended)) return false;
            
            getGraphemeBases(source, offset, recommended, Format, context);

            byte before = context.tBefore;
            byte after = context.tAfter;
            byte before2 = context.tBefore2;
            byte after2 = context.tAfter2;
            
            
            // Do not break after ambiguous terminators like period, if immediately followed by a number or lowercase letter, is between uppercase letters, or if the first following letter (optionally after certain punctuation) is lowercase. For example, a period may be an abbreviation or numeric period, and not mark the end of a sentence.

            if (before == ATerm) {
                rule = "6: ATerm × ( Numeric | Lower )";
                if (after == Lower || after == Numeric) return false;
                rule = "7: Upper ATerm × Upper";
                if (DEBUG_GRAPHEMES) System.out.println(context + ", " + Upper);
                if (before2 == Upper && after == Upper) return false;
            }
            
            // The following cases are all handled together.
            
            // First we loop backwards, checking for the different types.
            
            MyBreakIterator graphemeIterator = new MyBreakIterator();
            graphemeIterator.set(source, offset);
            
            int state = 0;
            int lookAfter = -1;
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
                rule = "12: Any × Any";
                return false;
            }
                
            // ATerm Close* Sp*×(¬( OLetter))* Lower(8)
            
            // Break after sentence terminators, but include closing punctuation, trailing spaces, and (optionally) a paragraph separator.
            // ( Term | ATerm ) Close*×( Close | Sp | Sep )(9)
            // ( Term | ATerm ) Close* Sp×( Sp | Sep )(10)
            // ( Term | ATerm ) Close* Sp*÷(11)

                        
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
                        rule = "8: ATerm Close* Sp* × ( ¬(OLetter | Upper | Lower) )* Lower";
                        return false;
                    }
                    if (gotSpace) {
                        if (t == Sp || t == Sep) {
                            rule = "10: ( Term | ATerm ) Close* Sp × ( Sp | Sep )";
                            return false;
                        }
                    } else if (t == Close || t == Sp || t == Sep) {
                        rule = "9: ( Term | ATerm ) Close* × ( Close | Sp | Sep )";
                        return false;
                    }
                    if (lookAfter == Term) break;
                }
                    
                // at this point, we have an ATerm. All other conditions are ok, but we need to verify 6
                if (t != OLetter && t != Upper && t != Lower) continue;
                if (t == Lower) {
                    rule = "8: ATerm Close* Sp* × ( ¬(OLetter | Upper | Lower) )* Lower";
                    return false;
                }
                break;
            }
            rule = "11: ( Term | ATerm ) Close* Sp* ÷";
            return true;
        }
    }
    
    static final boolean DEBUG_GRAPHEMES = false;
    
    static class MyBreakIterator {
        int offset = 0;
        String string = "";
        GenerateBreakTest breaker = new GenerateGraphemeBreakTest();
        boolean recommended = true;
        
        public MyBreakIterator set(String source, int offset) {
            //if (DEBUG_GRAPHEMES) System.out.println(Utility.hex(string) + "; " + offset);
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
            //if (DEBUG_GRAPHEMES) System.out.println(Utility.hex(result));
            return result;
        }
        
        public int previousBase() {
            if (offset <= 0) return -1;
            for (--offset; offset >= 0; --offset) {
                if (breaker.isBreak(string, offset, recommended)) break;
            }
            int result = UTF16.charAt(string, offset);
            //if (DEBUG_GRAPHEMES) System.out.println(Utility.hex(result));
            return result;
        }
    }
}

