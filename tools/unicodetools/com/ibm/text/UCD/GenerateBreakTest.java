/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/unicodetools/com/ibm/text/UCD/GenerateBreakTest.java,v $
* $Date: 2006/09/24 23:32:44 $
* $Revision: 1.15 $
*
*******************************************************************************
*/

package com.ibm.text.UCD;

import java.util.*;
import java.io.*;

import org.unicode.cldr.util.Segmenter;

import com.ibm.text.utility.*;
import com.ibm.icu.dev.test.util.UnicodeMap;
import com.ibm.icu.dev.test.util.UnicodeProperty;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;

abstract public class GenerateBreakTest implements UCD_Types {

    static boolean DEBUG = true;
    static final boolean SHOW_TYPE = false;
    UCD ucd;
    Normalizer nfd;
    Normalizer nfkd;
    
    OldUnicodeMap sampleMap = null;
    OldUnicodeMap map = new OldUnicodeMap();
    UnicodeProperty prop;
   
    // ====================== Main ===========================
    
    public static void main(String[] args) throws IOException {
        System.out.println("Remember to add length marks (half & full) and other punctuation for sentence, with FF61");
        //Default.setUCD();
        new GenerateGraphemeBreakTest(Default.ucd()).run();
        new GenerateWordBreakTest(Default.ucd()).run();
        new GenerateLineBreakTest(Default.ucd()).run();
        new GenerateSentenceBreakTest(Default.ucd()).run();
    }
    
    GenerateBreakTest(UCD ucd) {
        this.ucd = ucd;
        nfd = new Normalizer(Normalizer.NFD, ucd.getVersion());
        nfkd = new Normalizer(Normalizer.NFKD, ucd.getVersion());
        	/*
        public void fillMap(String propName) {
        	List list = y.getAvailableValues();
        	for (Iterator it = list.iterator(); it.hasNext();) {
        		String label = (String) it.next();
        		map.add(label, y.getSet(label));
        	}
        }
        */
    }
    
	ToolUnicodePropertySource unicodePropertySource = ToolUnicodePropertySource.make("");

	Set labels = new HashSet();
    
    int addToMap(String label) {
    	labels.add(label);
    	UnicodeSet s = prop.getSet(label);
    	if (s == null || s.size() == 0) throw new IllegalArgumentException("Bad value: " + prop.getName() + ", " + label);
    	return map.add(label, s);
    }

    int addToMapLast(String label) {
    	int result = addToMap(label);
    	Set values = new HashSet(prop.getAvailableValues());
    	if (!values.equals(labels)) throw new IllegalArgumentException("Missing Property Values: " + prop.getName()
    			+ ": " + values.removeAll(labels));
		return result;
    }

    // COMMON STUFF for Hangul
    /*
    static final byte hNot = -1, hL = 0, hV = 1, hT = 2, hLV = 3, hLVT = 4, hLIMIT = 5;
    static final String[] hNames = {"L", "V", "T", "LV", "LVT"};

    
    static byte getHangulType(int cp) {
        if (ucd.isLeadingJamo(cp)) return hL;
        if (ucd.isVowelJamo(cp)) return hV;
        if (ucd.isTrailingJamo(cp)) return hT;
        if (ucd.isHangulSyllable(cp)) {
            if (ucd.isDoubleHangul(cp)) return hLV;
            return hLVT;
        }
        return hNot;
    }
    */
    
   /* static { 
        setUCD();
    }
    */
    
    public static boolean onCodepointBoundary(String s, int offset) {
        if (offset < 0 || offset > s.length()) return false;
        if (offset == 0 || offset == s.length()) return true;
        if (UTF16.isLeadSurrogate(s.charAt(offset-1))
        && UTF16.isTrailSurrogate(s.charAt(offset))) return false;
        return true;
    }

    // finds the first base character, or the first character if there is no base
    public int findFirstBase(String source, int start, int limit) {
        int cp;
        for (int i = start; i < limit; i += UTF16.getCharCount(cp)) {
            cp = UTF16.charAt(source, i);
            byte cat = ucd.getCategory(cp);
            if (((1<<cat) & MARK_MASK) != 0) continue;
            return cp;
        }
        return UTF16.charAt(source, start);
    }

    // quick & dirty routine
    static String insertEverywhere(String source, String insertion, GenerateBreakTest breaker) {
        String result = insertion;
        for (int i = 0; i < source.length(); ++i) {
            result += source.charAt(i);
            if (breaker.isBreak(source, i)) {
                result += insertion;
            }
        }
        return result + insertion;
    }


    static void checkDecomps(UCD ucd) {
        UCDProperty[]  INFOPROPS = {UnifiedProperty.make(CATEGORY), UnifiedProperty.make(LINE_BREAK)};
        GenerateBreakTest[] tests = {
            new GenerateGraphemeBreakTest(ucd), 
            new GenerateWordBreakTest(ucd),
            new GenerateLineBreakTest(ucd),
        };
        tests[0].isBreak("\u0300\u0903", 1);
        Normalizer nfd = new Normalizer(Normalizer.NFD, ucd.getVersion());
        
        System.out.println("Check Decomps");
        //System.out.println("otherExtendSet: " + ((GenerateGraphemeBreakTest)tests[0]).otherExtendSet.toPattern(true));
        //Utility.showSetNames("", ((GenerateGraphemeBreakTest)tests[0]).otherExtendSet, false, ucd);
        
        for (int k = 0; k < tests.length; ++k) {
            for (int i = 0; i < 0x10FFFF; ++i) {
                if (!ucd.isAllocated(i)) continue;
                if (ucd.isHangulSyllable(i)) continue;
                if (nfd.isNormalized(i)) continue;
                String decomp = nfd.normalize(i);
                boolean shown = false;
                String test = decomp;
                for (int j = 1; j < test.length(); ++j) {
                    if (tests[k].isBreak(test, j)) {
                        if (!shown) {
                            System.out.println(showData(ucd, UTF16.valueOf(i), INFOPROPS, "\r\n\t"));
                            System.out.println(" => " + showData(ucd, decomp, INFOPROPS, "\r\n\t"));
                            shown = true;
                        }
                        System.out.println(j  + ": " + tests[k].fileName);
                    }
                }
            }
        }
    }
    
    static String showData(UCD ucd, String source, UCDProperty[] props, String separator) {
        StringBuffer result = new StringBuffer();
        int cp;
        for (int i = 0; i < source.length(); i += UTF16.getCharCount(cp)) {
            cp = UTF16.charAt(source, i);
            if (i != 0) result.append(separator);
            result.append(ucd.getCodeAndName(cp));
            for (int j = 0; j < props.length; ++j) {
                result.append(", ");
                result.append(props[j].getPropertyName(SHORT)).append('=').append(props[j].getValue(cp,SHORT));
            }
        }
        return result.toString();
    }
    
    void showSet(String title, UnicodeSet set) {
        System.out.println(title + ": " + set.toPattern(true));
        Utility.showSetNames("", set, false, ucd);
    }
    
    // determines if string is of form Base NSM*
    boolean isBaseNSMStar(String source) {
        int cp;
        int status = 0;
        for (int i = 0; i < source.length(); i += UTF16.getCharCount(cp)) {
            cp = UTF16.charAt(source, i);
            byte cat = ucd.getCategory(cp);
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

    UnicodeSet getClosure(UnicodeSet source) {
        UnicodeSet result = new UnicodeSet(source);
        for (int i = 0; i < 0x10FFFF; ++i) {
            if (!ucd.isAllocated(i)) continue;
            if (nfkd.isNormalized(i)) continue;
            String decomp = nfkd.normalize(i);
            if (source.containsAll(decomp)) result.add(i);
        }
        return result;
    }
    
    
    /*
    static UnicodeSet extraAlpha = new UnicodeSet("[\\u02B9-\\u02BA\\u02C2-\\u02CF\\u02D2-\\u02DF\\u02E5-\\u02ED\\u05F3]");
    static UnicodeSet alphabeticSet = UnifiedBinaryProperty.make(DERIVED | PropAlphabetic).getSet()
        .addAll(extraAlpha);
        
    static UnicodeSet ideographicSet = UnifiedBinaryProperty.make(BINARY_PROPERTIES | Ideographic).getSet();
    
    static {
        if (false) System.out.println("alphabetic: " + alphabeticSet.toPattern(true));
    }
    */


    void generateTerminalClosure() {
        UnicodeSet midLetterSet = new UnicodeSet("[\u0027\u002E\u003A\u00AD\u05F3\u05F4\u2019\uFE52\uFE55\uFF07\uFF0E\uFF1A]");

        UnicodeSet ambigSentPunct = new UnicodeSet("[\u002E\u0589\u06D4]");

        UnicodeSet sentPunct = new UnicodeSet("[\u0021\u003F\u0387\u061F\u0964\u203C\u203D\u2048\u2049"
            + "\u3002\ufe52\ufe57\uff01\uff0e\uff1f\uff61]");
        
        UnicodeSet terminals = UnifiedBinaryProperty.make(BINARY_PROPERTIES | Terminal_Punctuation).getSet();
        UnicodeSet extras = getClosure(terminals).removeAll(terminals);
        System.out.println("Current Terminal_Punctuation");
        Utility.showSetNames("", terminals, true, ucd);

        System.out.println("Missing Terminal_Punctuation");
        Utility.showSetNames("", extras, true, ucd);

        System.out.println("midLetterSet");
        System.out.println(midLetterSet.toPattern(true));
        Utility.showSetNames("", midLetterSet, true, ucd);

        System.out.println("ambigSentPunct");
        System.out.println(ambigSentPunct.toPattern(true));
        Utility.showSetNames("", ambigSentPunct, true, ucd);

        System.out.println("sentPunct");
        System.out.println(sentPunct.toPattern(true));
        Utility.showSetNames("", sentPunct, true, ucd);
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

    protected String currentRule;
    protected String fileName;
    protected String[] samples = new String[100];
    protected String[] extraSamples = new String[0];
    protected String[] extraSingleSamples = new String[0];
    protected int sampleLimit = 0;
    protected int tableLimit = -1;
    
    protected int[] skippedSamples = new int[100];
    protected boolean didSkipSamples = false;
    
    private String[] ruleList = new String[100];
    private int ruleListCount = 0;
    protected boolean collectingRules = false;
	protected boolean needsFullBreakSample = true;
    
    public void setRule(String rule) {
        if (collectingRules) {
            ruleList[ruleListCount++] = rule;
        }
        currentRule = rule;
    }

    public String getRule() {
        return currentRule;
    }

    public void run() throws IOException {
        findSamples();

        // test individual cases
        //printLine(out, samples[LB_ZW], "", samples[LB_CL]);
        //printLine(out, samples[LB_ZW], " ", samples[LB_CL]);

        UnicodeDataFile fc = UnicodeDataFile.openHTMLAndWriteHeader("DerivedData\\auxiliary\\", fileName + "BreakTest");
        PrintWriter out = fc.out;

/*        PrintWriter out = Utility.openPrintWriter("auxiliary\\" 
            + fileName + "BreakTest-"
            + ucd.getVersion()
            + ".html", Utility.UTF8_WINDOWS);
*/        
        out.println("<!doctype HTML PUBLIC '-//W3C//DTD HTML 4.0 Transitional//EN' 'http://www.w3.org/TR/REC-html40/loose.dtd'>");
        out.println("<html><head><meta http-equiv='Content-Type' content='text/html; charset=utf-8'>");
        out.println("<title>" + fileName + " Break Chart</title>");
        out.println("<style type='text/css'>");
        out.println("td, th { vertical-align: top }");
        out.println("</style></head>");


        out.println("<body bgcolor='#FFFFFF'>");
        out.println("<h2>" + fileName + " Break Chart</h2>");
        out.println("<p><b>Unicode Version:</b> " + ucd.getVersion() + "</p>");
        out.println("<p><b>Date:</b> " + Default.getDate() + "</p>");
        out.println("<p>This page illustrates the application of the boundary specifications. " +
        		"The first chart shows where breaks would appear between different sample characters or strings. " +
        		"The sample characters are chosen mechanically to represent the different properties used by the specification. " +
        		"Where properties used in the rules have 'overlaps', the samples are given 'composed' names. " +
        		"For example, SentenceBreak uses GCLF_Sep: Sep is the SentenceBreak property, but it overlaps with the GraphemeClusterBreak property LF." +
        		"</p>");
        generateTable(out);
        

        if (false) {
            out.println("<h3>Character Type Breakdown</h3>");
            out.println("<table border='1' cellspacing='0' width='100%'>");
            for (int i = 0; i < sampleMap.size(); ++i) {
                out.println("<tr><th>" + sampleMap.getLabelFromIndex(i) 
                    + "</th><td>" + sampleMap.getSetFromIndex(i)
                    + "</td></tr>");
            }
            out.println("</table>");
        }

        fc.close();
        
        generateTest(false);
        
    }
    
    public void generateTest(boolean shortVersion) throws IOException {
        String[] testCase = new String[50];
        // do main test

        UnicodeDataFile fc = UnicodeDataFile.openAndWriteHeader("DerivedData\\auxiliary\\", fileName + "BreakTest" 
                + (shortVersion ? "_SHORT" : ""));
        PrintWriter out = fc.out;
/*        PrintWriter out = Utility.openPrintWriter("TR29\\" + fileName + "BreakTest" 
            + (shortVersion ? "_SHORT" : "")
            + "-" + ucd.getVersion()
            + ".txt", Utility.UTF8_WINDOWS);
*/        
        int counter = 0;

        out.println("#");
        out.println("# Default " + fileName + " Break Test");
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
                    printLine(out, testCase[q], !shortVersion && q == 0, false);
                    ++counter;
                }
            }
        }

        for (int ii = 0; ii < extraSingleSamples.length; ++ii) {
            printLine(out, extraSingleSamples[ii], true, false);
        }
        out.println("# Lines: " + counter);
        fc.close();
    }

    public void sampleDescription(PrintWriter out) {}

    abstract public boolean isBreak(String source, int offset);
    
    abstract public String fullBreakSample();

    abstract public byte getType (int cp);
    
    public byte getSampleType (int cp) {
        return getType(cp);
    }
    
    public int mapType(int input) {
        return input;
    }
    
    public boolean highlightTableEntry(int x, int y, String s) {
        return false;
    }

    abstract public String getTypeID(int s);

    public String getTypeID(String s) {
        if (s == null) return "<null>";
        if (s.length() == 1) return getTypeID(s.charAt(0));
        StringBuffer result = new StringBuffer();
        int cp;
        for (int i = 0; i < s.length(); i += UTF32.count16(cp)) {
            cp = UTF32.char32At(s, i);
            if (i > 0) result.append(" ");
            result.append(getTypeID(cp));
        }
        return result.toString();
    }

    static final int DONE = -1;

    public int next(String source, int offset) {
        for (int i = offset + 1; i <= source.length(); ++i) {
            if (isBreak(source, i)) return i;
        }
        return DONE;
    }

    public int previous(String source, int offset) {
        for (int i = offset - 1; i >= 0; --i) {
            if (isBreak(source, i)) return i;
        }
        return DONE;
    }

    public int genTestItems(String before, String after, String[] results) {
        results[0] = before + after;
        return 1;
    }

    public String getTableEntry(String before, String after, String[] ruleOut) {
        boolean normalBreak = isBreak(before + after, before.length());
        String normalRule = getRule();
        ruleOut[0] = normalRule;
        return normalBreak ? BREAK : NOBREAK;
    }

    public byte getResolvedType(int cp) {
        return getType(cp);
    }

    boolean skipType(int type) {
        return false;
    }

    String getInfo(String s) {
        if (s == null || s.length() == 0) return "NULL";
        StringBuffer result = new StringBuffer();
        int cp;
        for (int i = 0; i < s.length(); i += UTF32.count16(cp)) {
            cp = UTF32.char32At(s, i);
            if (i > 0) result.append(", ");
            result.append(ucd.getCodeAndName(cp));
            result.append(", gc=" + ucd.getCategoryID_fromIndex(ucd.getCategory(cp),SHORT));
            result.append(", sc=" + ucd.getScriptID_fromIndex(ucd.getScript(cp),SHORT));
            //result.append(", lb=" + ucd.getLineBreakID_fromIndex(ucd.getLineBreak(cp))
            //    + "=" + ucd.getLineBreakID_fromIndex(ucd.getLineBreak(cp), LONG));
        }
        return result.toString();
    }

    public void generateTable(PrintWriter out) {
        String width = "width='" + (100 / (tableLimit + 1)) + "%'";
        out.print("<table border='1' cellspacing='0' width='100%'>");
        String types = "";
        String codes = "";
        for (int type = 0; type < tableLimit; ++type) {
            String after = samples[type];
            if (after == null) continue;

            String h = getTypeID(after);
            types += "<th " + width + " class='lbclass' title='" + getInfo(after) + "'>" + h + "</th>";
            
            
            //codes += "<th " + width + " title='" + getInfo(after) + "'>" + Utility.hex(after) + "</th>";
        }

        out.println("<tr><th " + width + "></th>" + types + "</tr>");
        // out.println("<tr><th " + width + "></th><th " + width + "></th>" + codes + "</tr>");

        String[] rule = new String[1];
        String[] rule2 = new String[1];
        for (int type = 0; type < sampleLimit; ++type) {
            if (type == tableLimit) {
                out.println("<tr><td bgcolor='#0000FF' colSpan='" + (tableLimit + 1) + "' style='font-size: 1px'>&nbsp;</td></tr>");
            }
            String before = samples[type];
            if (before == null) continue;

            String h = getTypeID(before);
            String line = "<tr><th class='lbclass' title='" + ucd.getCodeAndName(before) + "'>" + h + "</th>";

            for (int type2 = 0; type2 < tableLimit; ++type2) {
                
                String after = samples[type2];
                if (after == null) continue;

                String t = getTableEntry(before, after, rule);
                String background = "";
                String t2 = getTableEntry(before, after, rule2);
                if (highlightTableEntry(type, type2, t)) {
                    background = " bgcolor='#FFFF00'";
                }
                if (!t.equals(t2)) {
                    if (t.equals(NOBREAK)) {
                        background = " bgcolor='#CCFFFF'";
                    } else {
                        background = " bgcolor='#FFFF00'";
                    }
                } else if (t.equals(NOBREAK)) {
                    background = " bgcolor='#CCCCFF'";
                }
                line += "<th title='" + rule[0] + "'" + background + " class='pairItem'>" + t + "</th>";
            }
            out.println(line + "</tr>");
        }
        out.println("</table>");
            
        if (didSkipSamples) {
            out.println("<p><b>Suppressed:</b> ");
            for (int i = 0; i < skippedSamples.length; ++i) {
                if (skippedSamples[i] > 0) {
                    String tmp = UTF16.valueOf(skippedSamples[i]);
                    out.println("<span title='" + getInfo(tmp) + "'>" + getTypeID(tmp) + "</span>");
                }
            }
            out.println("</p>");
        }
        
        // gather the data for the rules
        if (needsFullBreakSample ) {
        collectingRules = true;
        isBreak(fullBreakSample(), 1);
        collectingRules = false;
        }
        
        out.println("<h3>Rules</h3>");
        out.println("<p>Due to the way they have been mechanically processed for generation, " +
        		"the following rules do not match the UAX rules precisely. " +
        		"In particular:</p>"+
        		"<ol>" +
        		"<li>The rules are cast into a more regex-style.</li>"+
        		"<li>The rules \"sot ÷\", \"÷ eot\", and \"÷ Any\" are added mechanically, and have artificial numbers.</li>"+
        		"<li>The rules are given decimal numbers, so rules such as 11a are given a number using tenths, such as 11.1.</li>"+
        		"<li>Where a rule has multiple parts (lines), each one is numbered using hundredths, such as 21.01) × BA, 21.02) × HY,...</li>"+
        		"<li>Any 'treat as' or 'ignore' rules are handled as discussed in Unicode Standard Annex #29, and thus" +
        		"reflected in a transformation of the rules not visible here.</li>" +
        		"</ol>" +
        		"<p>For the original rules, see the UAX.</p>"

        		);
        out.println("<ul style='list-style-type: none'>");
            for (int ii = 0; ii < ruleListCount; ++ii) {
                out.println("<li>" + ruleList[ii].replaceAll("[$]","") + "</li>");
            }
        out.println("</ul>");
        
        if (extraSingleSamples.length > 0) {
            out.println("<h3>Sample Strings</h3>");
            out.println("<p>" +
            		"The following samples illustrate the application of the rules. " +
            		"The blue lines indicate possible break points. " +
            		"If your browser supports titles, then positioning the mouse over each character will show its name, " +
            		"white positioning between characters shows the rule number of the rule responsible for the break-status." +
            		"</p>");
            out.println("<ol>");
                for (int ii = 0; ii < extraSingleSamples.length; ++ii) {
                    out.println("<li><font size='5'>");
                    printLine(out, extraSingleSamples[ii], true, true);
                    out.println("</font></li>");
                }
            out.println("</ol>");
        }
    }

    static final String BREAK = "\u00F7";
    static final String NOBREAK = "\u00D7";

    public void printLine(PrintWriter out, String source, boolean comments, boolean html) {
        int cp;
        StringBuffer string = new StringBuffer();
        StringBuffer comment = new StringBuffer("\t# ");
        boolean hasBreak = isBreak(source, 0);
        String status;
        if (html) {
            status = hasBreak ? " style='border-right: 1px solid blue'" : "";
            string.append("<span title='" + getRule() + "'><span" + status + ">&nbsp;</span>&nbsp;</span>");
        } else {
            status = hasBreak ? BREAK : NOBREAK;
            string.append(status);
        }
        comment.append(' ').append(status).append(" [").append(getRule()).append(']');

        for (int offset = 0; offset < source.length(); offset += UTF16.getCharCount(cp)) {

            cp = UTF16.charAt(source, offset);
            hasBreak = isBreak(source, offset + UTF16.getCharCount(cp));

            if (html) {
                status = hasBreak ? " style='border-right: 1px solid blue'" : "";
                string.append("<span title='" +
                    Utility.quoteXML(ucd.getCodeAndName(cp) + " (" + getTypeID(cp) + ")", true)
                    + "'>"
                    + Utility.quoteXML(Utility.getDisplay(cp), true)
                    + "</span>");
                string.append("<span title='" + getRule() + "'><span" + status + ">&nbsp;</span>&nbsp;</span>");
            } else {
                if (string.length() > 0) {
                    string.append(' ');
                    comment.append(' ');
                }

                status = hasBreak ? BREAK : NOBREAK;

                string.append(Utility.hex(cp));
                comment.append(ucd.getName(cp) + " (" + getTypeID(cp) + ")");
                string.append(' ').append(status);
                comment.append(' ').append(status).append(" [").append(getRule()).append(']');
            }
        }

        if (comments && !html) string.append(comment);
        out.println(string);
        if (DEBUG) System.out.println("*" + string);
    }

    public void findSamples() {

        // what we want is a list of sample characters. In the simple case, this is just one per type.
        // However, if there are characters that have different types (when recommended or not), then
        // we want a type for each cross-section

        BitSet bitset = new BitSet();
        Map list = new TreeMap();

        for (int i = 1; i <= 0xFFFF; ++i) {
            if (!ucd.isAllocated(i)) continue;
            if (0xD800 <= i && i <= 0xDFFF) continue;
            if (DEBUG && i == 0x1100) {
                System.out.println("debug");
            }
            byte lb = getSampleType(i);
            byte lb2 = lb; // HACK
            if (lb == lb2 && skipType(lb)) {
                skippedSamples[lb] = i;
                didSkipSamples = true;
                continue;
            }

            int combined = (mapType(lb) << 7) + mapType(lb2);
            if (combined < 0) {
            	throw new IllegalArgumentException("should never happen");
            }
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
            if (DEBUG) System.out.println(getTypeID(sample) + ":\t" + ucd.getCodeAndName(sample));
        }

        tableLimit = sampleLimit;

        // now add values that are different
        /*

        for (int i = 1; i <= 0x10FFFF; ++i) {
            if (!ucd.isAllocated(i)) continue;
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

    public int findLastNon(String source, int offset, byte notLBType) {
        int cp;
        for (int i = offset-1; i >= 0; i -= UTF16.getCharCount(cp)) {
            cp = UTF16.charAt(source, i);
            byte f = getResolvedType(cp);
            if (f != notLBType) return i;
        }
        return -1;
    }

    public static UnicodeSet getSet(UCD ucd, int prop, byte propValue) {
        return UnifiedBinaryProperty.make(prop | propValue, ucd).getSet();
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

    public void getGraphemeBases(MyBreakIterator graphemeIterator, String source, int offset, int ignoreType, Context context) {
        context.cpBefore2 = context.cpBefore = context.cpAfter = context.cpAfter2 = -1;
        context.tBefore2 = context.tBefore = context.tAfter = context.tAfter2 = -1;
        //if (DEBUG_GRAPHEMES) System.out.println(Utility.hex(source) + "; " + offset + "; " + ignoreType);
            
        //MyBreakIterator graphemeIterator = new MyBreakIterator(new GenerateGraphemeBreakTest(ucd));

        graphemeIterator.set(source, offset);
        while (true) {
            int cp = graphemeIterator.previousBase();
            if (cp == -1) break;
            byte t = getResolvedType(cp);
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
            byte t = getResolvedType(cp);
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
    
    static class XGenerateBreakTest extends GenerateBreakTest {
    	Segmenter seg;
    	String sample;
    	{
    		needsFullBreakSample = false;
    	}
    	
    	public XGenerateBreakTest(UCD ucd, Segmenter.Builder segBuilder, String sample, String filename, String[] extraSamples, String[] extraSingleSamples) {
    		super(ucd);
    		this.seg = segBuilder.make();
    		this.sample = sample;
    		List rules = segBuilder.getRules();
    		collectingRules = true;
    		for (Iterator it = rules.iterator(); it.hasNext();) {
    			String rule = (String)it.next();
    			setRule(rule);
    		}
    		collectingRules = false;
    		map.add("Other", new UnicodeSet(0,0x10FFFF));
    		UnicodeMap segSamples = seg.getSamples();
    		Collection x = segSamples.getAvailableValues();
    		for (Iterator it = x.iterator(); it.hasNext();) {
    			String label = (String)it.next();
    			map.add(label, segSamples.getSet(label), true, false);
    		}
            this.fileName = filename;
            sampleMap = map;
            this.extraSamples = extraSamples;
            this.extraSingleSamples = extraSingleSamples;
    	}

		public boolean isBreak(String source, int offset) {
			boolean result = seg.breaksAt(source, offset);
			setRule(String.valueOf(seg.getBreakRule()));
			return result;
		}

		public String fullBreakSample() {
			return sample;
		}

        // stuff that subclasses need to override
        public String getTypeID(int cp) {
            return map.getLabel(cp);
        }

        // stuff that subclasses need to override
        public byte getType(int cp) {
            return (byte) map.getIndex(cp);
        }    
    }
    
    static class GenerateGraphemeBreakTest extends XGenerateBreakTest {
		public GenerateGraphemeBreakTest(UCD ucd) {
	        super(ucd, Segmenter.make(ToolUnicodePropertySource.make(ucd.getVersion()),"GraphemeClusterBreak"), "aa", "Grapheme",
	        		new String[]{}, new String[]{});
		}	
    }

    static class GenerateLineBreakTest extends XGenerateBreakTest {
		public GenerateLineBreakTest(UCD ucd) {
	        super(ucd, Segmenter.make(ToolUnicodePropertySource.make(ucd.getVersion()),"LineBreak"), "aa", "Line",
	        	new String[]{}, new String[] {
	        	"can't", "can\u2019t", "ab\u00ADby",
	             "-3",
	             "e.g.",
	             "\u4e00.\u4e00.",
	              "a  b",
	              "a  \u200bb",
	              "a \u0308b",
	              "1\u0308b(a)-(b)",
	              });
		}	
    }
    
    static class GenerateSentenceBreakTest extends XGenerateBreakTest {
		public GenerateSentenceBreakTest(UCD ucd) {
	        super(ucd, Segmenter.make(ToolUnicodePropertySource.make(ucd.getVersion()),"SentenceBreak"), "aa", "Sentence",
	        		new String[]{},
	        		getExtraSamples());
		}	
		static String[] getExtraSamples() {
            GenerateBreakTest grapheme = new GenerateGraphemeBreakTest(Default.ucd());
	        String[] extraSingleSamples = new String[] {
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
	            return extraSingleSamples;
		}
    }

    static class GenerateWordBreakTest extends XGenerateBreakTest {
		public GenerateWordBreakTest(UCD ucd) {
	        super(ucd, Segmenter.make(ToolUnicodePropertySource.make(ucd.getVersion()),"WordBreak"), "aa", "Word",
	    	        new String[] {
                /*"\uFF70", "\uFF65", "\u30FD", */ "a\u2060", "a:", "a'", "a'\u2060", "a,", "1:", "1'", "1,",  "1.\u2060"
            	},


	        	getExtraSamples());
		}	
		static String[] getExtraSamples() {
            GenerateBreakTest grapheme = new GenerateGraphemeBreakTest(Default.ucd());
	                String [] temp = {"can't", "can\u2019t", "ab\u00ADby", "a$-34,567.14%b", "3a" };
	                String[] extraSingleSamples = new String [temp.length * 2];
	                System.arraycopy(temp, 0, extraSingleSamples, 0, temp.length);
	                for (int i = 0; i < temp.length; ++i) {
	                    extraSingleSamples[i+temp.length] = insertEverywhere(temp[i], "\u2060", grapheme);
	                }

	            return extraSingleSamples;
		}
    }

    static class OLDGenerateGraphemeBreakTest extends GenerateBreakTest {

    	OLDGenerateGraphemeBreakTest(UCD ucd) {
            super(ucd);
            fileName = "Grapheme";
            sampleMap = map;
        }

        Object foo = prop = unicodePropertySource.getProperty("Grapheme_Cluster_Break");

        final int
            CR =    addToMap("CR"),
            LF =    addToMap("LF"),
            Control = addToMap("Control"),
            Extend = addToMap("Extend"),
            L =     addToMap("L"),
            V =     addToMap("V"),
            T =     addToMap("T"),
            LV =    addToMap("LV"),
            LVT =   addToMap("LVT"),
            Other = addToMapLast("Other");            
                
        // stuff that subclasses need to override
        public String getTypeID(int cp) {
            return map.getLabel(cp);
        }

        // stuff that subclasses need to override
        public byte getType(int cp) {
            return (byte) map.getIndex(cp);
        }
        
        public String fullBreakSample() {
            return "aa";
        }

        public boolean isBreak(String source, int offset) {
            
            setRule("1: sot ÷");
            if (offset < 0 || offset > source.length()) return false;
            if (offset == 0) return true;

            setRule("2: ÷ eot");
            if (offset == source.length()) return true;

            // UTF-16: never break in the middle of a code point
            if (!onCodepointBoundary(source, offset)) return false;

            // now get the character before and after, and their types


            int cpBefore = UTF16.charAt(source, offset-1);
            int cpAfter = UTF16.charAt(source, offset);

            byte before = getResolvedType(cpBefore);
            byte after = getResolvedType(cpAfter);

            setRule("3: CR × LF");
            if (before == CR && after == LF) return false;

            setRule("4: ( Control | CR | LF ) ÷");
            if (before == CR || before == LF || before == Control) return true;

            setRule("5: ÷ ( Control | CR | LF )");
            if (after == Control || after == LF || after == CR) return true;

            setRule("6: L × ( L | V | LV | LVT )");
            if (before == L && (after == L || after == V || after == LV || after == LVT)) return false;

            setRule("7: ( LV | V ) × ( V | T )");
            if ((before == LV || before == V) && (after == V || after == T)) return false;

            setRule("8: ( LVT | T ) × T");
            if ((before == LVT || before == T) && (after == T)) return false;

            setRule("9: × Extend");
            if (after == Extend) return false;

            // Otherwise break after all characters.
            setRule("10: Any ÷ Any");
            return true;

        }

    }

    //==============================================

    static class XGenerateWordBreakTest extends GenerateBreakTest {
        
        GenerateGraphemeBreakTest grapheme;
        MyBreakIterator breaker;
        Context context = new Context();

        XGenerateWordBreakTest(UCD ucd) {
            super(ucd);
            grapheme = new GenerateGraphemeBreakTest(ucd);
            breaker = new MyBreakIterator(grapheme);
            fileName = "Word";
            sampleMap = map;
            extraSamples = new String[] {
                /*"\uFF70", "\uFF65", "\u30FD", */ "a\u2060", "a:", "a'", "a'\u2060", "a,", "1:", "1'", "1,",  "1.\u2060"
            };

            String [] temp = {"can't", "can\u2019t", "ab\u00ADby", "a$-34,567.14%b", "3a" };
            extraSingleSamples = new String [temp.length * 2];
            System.arraycopy(temp, 0, extraSingleSamples, 0, temp.length);
            for (int i = 0; i < temp.length; ++i) {
                extraSingleSamples[i+temp.length] = insertEverywhere(temp[i], "\u2060", grapheme);
            }
        
            if (false) Utility.showSetDifferences("Katakana", map.getSetFromIndex(Katakana), 
                "Script=Katakana", getSet(ucd, SCRIPT, KATAKANA_SCRIPT), false, ucd);

        }
        
        Object foo = prop = unicodePropertySource.getProperty("Word_Break");

        //static String LENGTH = "[\u30FC\uFF70]";
        //static String HALFWIDTH_KATAKANA = "[\uFF66-\uFF9F]";
        //static String KATAKANA_ITERATION = "[\u30FD\u30FE]";
        //static String HIRAGANA_ITERATION = "[\u309D\u309E]";
        
        final int
            Format =    addToMap("Format"),
            Katakana =    addToMap("Katakana"),
            ALetter = addToMap("ALetter"),
            MidLetter = addToMap("MidLetter"),
            //MidNumLet =     addToMap("MidNumLet"),
            MidNum =     addToMap("MidNum"),
            Numeric =     addToMap("Numeric"),
            ExtendNumLet =     addToMap("ExtendNumLet"),
            Other = addToMapLast("Other");      

        // stuff that subclasses need to override
        public String getTypeID(int cp) {
            return map.getLabel(cp);
        }

        // stuff that subclasses need to override
        public byte getType(int cp) {
            return (byte) map.getIndex(cp);
        }
        
        public String fullBreakSample() {
            return " a";
        }

        public int genTestItems(String before, String after, String[] results) {
            results[0] = before + after;
            results[1] = 'a' + before + "\u0301\u0308" + after + "\u0301\u0308" + 'a';
            results[2] = 'a' + before + "\u0301\u0308" + samples[MidLetter] + after + "\u0301\u0308" + 'a';
            results[3] = 'a' + before + "\u0301\u0308" + samples[MidNum] + after + "\u0301\u0308" + 'a';
            return 3;
        }

        public boolean isBreak(String source, int offset) {

            setRule("1: sot ÷");
            if (offset < 0 || offset > source.length()) return false;
  
            if (offset == 0) return true;

            setRule("2: ÷ eot");
            if (offset == source.length()) return true;

            // Treat a grapheme cluster as if it were a single character:
            // the first base character, if there is one; otherwise the first character.

            setRule("3: GC -> FC");
            if (!grapheme.isBreak( source,  offset)) return false;

            setRule("4: X Format* -> X");
            byte afterChar = getResolvedType(source.charAt(offset));
            if (afterChar == Format) return false;

            // now get the base character before and after, and their types

            getGraphemeBases(breaker, source, offset, Format, context);

            byte before = context.tBefore;
            byte after = context.tAfter;
            byte before2 = context.tBefore2;
            byte after2 = context.tAfter2;

            //Don't break between most letters

            setRule("5: ALetter × ALetter");
            if (before == ALetter && after == ALetter) return false;

            // Don’t break letters across certain punctuation

            setRule("6: ALetter × MidLetter ALetter");
            if (before == ALetter && after == MidLetter && after2 == ALetter) return false;

            setRule("7: ALetter (MidLetter | MidNumLet) × ALetter");
            if (before2 == ALetter && before == MidLetter && after == ALetter) return false;

            // Don’t break within sequences of digits, or digits adjacent to letters.

            setRule("8: Numeric × Numeric");
            if (before == Numeric && after == Numeric) return false;

            setRule("9: ALetter × Numeric");
            if (before == ALetter && after == Numeric) return false;

            setRule("10: Numeric × ALetter");
            if (before == Numeric && after == ALetter) return false;


            // Don’t break within sequences like: '-3.2'
            setRule("11: Numeric (MidNum | MidNumLet) × Numeric");
            if (before2 == Numeric && before == MidNum && after == Numeric) return false;

            setRule("12: Numeric × (MidNum | MidNumLet) Numeric");
            if (before == Numeric && after == MidNum && after2 == Numeric) return false;

            // Don't break between Katakana

            setRule("13: Katakana × Katakana");
            if (before == Katakana && after == Katakana) return false;
            
            // Do not break from extenders
            setRule("13a: (ALetter | Numeric | Katakana | ExtendNumLet)  	×  	ExtendNumLet");
            if ((before == ALetter || before == Numeric || before == Katakana || before == ExtendNumLet) && after == ExtendNumLet) return false;

            setRule("13b: ExtendNumLet 	× 	(ALetter | Numeric | Katakana)");
            if (before == ExtendNumLet && (after == ALetter || after == Numeric || after == Katakana)) return false;

            // Otherwise break always.
            setRule("14: Any ÷ Any");
            return true;

        }

    }

    // ========================================

    static class XGenerateLineBreakTest extends GenerateBreakTest {

        GenerateGraphemeBreakTest grapheme;
        MyBreakIterator breaker;
        Context context = new Context();

        XGenerateLineBreakTest(UCD ucd) {
            super(ucd);
            grapheme = new GenerateGraphemeBreakTest(ucd);
            breaker = new MyBreakIterator(grapheme);

            sampleMap = map;
            fileName = "Line";
            extraSingleSamples = new String[] {"can't", "can\u2019t", "ab\u00ADby",
                 "-3",
                 "e.g.",
                 "\u4e00.\u4e00.",
                  "a  b",
                  "a  \u200bb",
                  "a \u0308b",
                  "1\u0308b(a)-(b)",
                  };
        }
        
        // all the other items are supplied in UCD_TYPES
        
        /*static byte LB_L = LB_LIMIT + hL, LB_V = LB_LIMIT + hV, LB_T = LB_LIMIT + hT,
            LB_LV = LB_LIMIT + hLV, LB_LVT = LB_LIMIT + hLVT, LB_SUP = LB_LIMIT + hLIMIT,
            LB2_LIMIT = (byte)(LB_SUP + 1);
        */    

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
            //System.out.println("Adding Linebreak");
            for (int i = 0; i <= 0x10FFFF; ++i) {
                map.put(i, ucd.getLineBreak(i));
            }
            for (int i = 0; i < LB_LIMIT; ++i) {
                map.setLabel(i, ucd.getLineBreakID_fromIndex((byte)i, SHORT));
            }
            //System.out.println(map.getSetFromIndex(LB_CL));
            //System.out.println("Done adding Linebreak");
        }
        
        public int mapType(int input) {
            int old = input;
            switch (input) {
                case LB_BA: input = 16; break;
                case LB_BB: input = 17; break;
                case LB_B2: input = 18; break;
                case LB_ZW: input = 19; break;
                case LB_CM: input = 20; break;
                case LB_WJ: input = 21; break;
                
                case LB_SP: input = 22; break;
                case LB_BK: input = 23; break;
                case LB_NL: input = 24; break;
                case LB_CR: input = 25; break;
                case LB_LF: input = 26; break;
                
                case LB_CB: input = 27; break;
                case LB_SA: input = 28; break;
                case LB_AI: input = 29; break;
                case LB_SG: input = 30; break;
            }
            //if (old != input) System.out.println(old + " => " + input);
            return input;
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
            return type == LB_AI || type == LB_SA || type == LB_SG || type == LB_XX
                || type == LB_CB || type == LB_CR || type == LB_BK || type == LB_LF
                || type == LB_NL || type == LB_SP;
        }

        // stuff that subclasses need to override
        public String getTypeID(int cp) {
            /*
            byte result = getType(cp);
            if (result == LB_SUP) return "SUP";
            if (result >= LB_LIMIT) return hNames[result - LB_LIMIT];
            */
            // return ucd.getLineBreakID_fromIndex(cp); // AsmusOrderToMyOrder[result]);
            return ucd.getLineBreakID(cp); // AsmusOrderToMyOrder[result]);
        }

        public String fullBreakSample() {
            return ")a";
        }

        // stuff that subclasses need to override
        public byte getType(int cp) {
            /*if (cp > 0xFFFF) return LB_SUP;
            byte result = getHangulType(cp);
            if (result != hNot) return (byte)(result + LB_LIMIT);
            */
            // return MyOrderToAsmusOrder[ucd.getLineBreak(cp)];
            return ucd.getLineBreak(cp);
        }

        public String getTableEntry(String before, String after, String[] ruleOut) {
            String t = "_"; // break
            boolean spaceBreak = isBreak(before + " " + after, before.length()+1);
            String spaceRule = getRule();

            boolean spaceBreak2 = isBreak(before + " " + after, before.length());
            String spaceRule2 = getRule();

            boolean normalBreak = isBreak(before + after, before.length());
            String normalRule = getRule();

            ruleOut[0] = normalRule;
            if (!normalBreak) {
                if (!spaceBreak && !spaceBreak2) {
                    t = "^"; // don't break, even with intervening spaces
                } else {
                    t = "%"; // don't break, but break with intervening spaces
                }
                if (!spaceRule2.equals(normalRule)) {
                    ruleOut[0] += " [" + spaceRule2 + "]";
                }
                if (!spaceRule.equals(normalRule) && !spaceRule.equals(spaceRule2)) {
                    ruleOut[0] += " {" + spaceRule + "}";
                }
            }
            return t;
        }
        
        public boolean highlightTableEntry(int x, int y, String s) {
            return false;
            /*
            try {
                return !oldLineBreak[x][y].equals(s);
            } catch (Exception e) {}
            return true;
            */
        }

/*
        String[][] oldLineBreak = {
{"^",	"^",	"^",	"^",	"^",	"^",	"^",	"^",	"^",	"^",	"^",	"^",	"^",	"^",	"^",	"^",	"^",	"^",	"^",	"%"},
{"_",	"^",	"%",	"%",	"^",	"^",	"^",	"^",	" ",	"%",	"_",	"_",	"_",	"_",	"%",	"%",	"_",	"_",	"^",	"%"},
{"^",	"^",	"%",	"%",	"%",	"^",	"^",	"^",	"%",	"%",	"%",	"%",	"%",	"%",	"%",	"%",	"%",	"%",	"^",	"%"},
{"%",	"^",	"%",	"%",	"%",	"^",	"^",	"^",	"%",	"%",	"%",	"%",	"%",	"%",	"%",	"%",	"%",	"%",	"^",	"%"},
{"_",	"^",	"%",	"%",	"%",	"^",	"^",	"^",	"_",	"_",	"_",	"_",	"_",	"_",	"%",	"%",	"_",	"_",	"^",	"%"},
{"_",	"^",	"%",	"%",	"%",	"^",	"^",	"^",	"_",	"_",	"_",	"_",	"_",	"_",	"%",	"%",	"_",	"_",	"^",	"%"},
{"_",	"^",	"%",	"%",	"%",	"^",	"^",	"^",	"_",	"_",	"%",	"_",	"_",	"_",	"%",	"%",	"_",	"_",	"^",	"%"},
{"_",	"^",	"%",	"%",	"%",	"^",	"^",	"^",	"_",	"_",	"%",	"_",	"_",	"_",	"%",	"%",	"_",	"_",	"^",	"%"},
{"%",	"^",	"%",	"%",	"%",	"^",	"^",	"^",	"_",	"_",	"%",	"%",	"%",	"_",	"%",	"%",	"_",	"_",	"^",	"%"},
{"_",	"^",	"%",	"%",	"%",	"^",	"^",	"^",	"_",	"_",	"_",	"_",	"_",	"_",	"%",	"%",	"_",	"_",	"^",	"%"},
{"_",	"^",	"%",	"%",	"%",	"^",	"^",	"^",	"_",	"%",	"%",	"%",	"_",	"%",	"%",	"%",	"_",	"_",	"^",	"%"},
{"_",	"^",	"%",	"%",	"%",	"^",	"^",	"^",	"_",	"_",	"%",	"%",	"_",	"%",	"%",	"%",	"_",	"_",	"^",	"%"},
{"_",	"^",	"%",	"%",	"%",	"^",	"^",	"^",	"_",	"%",	"_",	"_",	"_",	"%",	"%",	"%",	"_",	"_",	"^",	"%"},
{"_",	"^",	"%",	"%",	"%",	"^",	"^",	"^",	"_",	"_",	"_",	"_",	"_",	"%",	"%",	"%",	"_",	"_",	"^",	"%"},
{"_",	"^",	"%",	"%",	"%",	"^",	"^",	"^",	"_",	"_",	"_",	"_",	"_",	"_",	"%",	"%",	"_",	"_",	"^",	"%"},
{"_",	"^",	"%",	"%",	"%",	"^",	"^",	"^",	"_",	"_",	"_",	"_",	"_",	"_",	"%",	"%",	"_",	"_",	"^",	"%"},
{"%",	"^",	"%",	"%",	"%",	"^",	"^",	"^",	"%",	"%",	"%",	"%",	"%",	"%",	"%",	"%",	"%",	"%",	"^",	"%"},
{"_",	"^",	"%",	"%",	"%",	"^",	"^",	"^",	"_",	"_",	"_",	"_",	"_",	"_",	"%",	"%",	"_",	"^",	"^",	"%"},
{"_",	"_",	"_",	"_",	"_",	"_",	"_",	"_",	"_",	"_",	"_",	"_",	"_",	"_",	"_",	"_",	"_",	"_",	"^",	"%"},
{"_",	"^",	"%",	"%",	"%",	"^",	"^",	"^",	"_",	"_",	"%",	"%",	"_",	"%",	"%",	"%",	"_",	"_",	"^",	"%"}
        };
*/

        public byte getResolvedType (int cp) {
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
            /*
            if (recommended) {
                if (getHangulType(cp) != hNot) {
                        result = LB_ID;
                }
            }
            */
            
            return result;
        }
        
        public byte getSampleType (int cp) {
            if (ucd.getHangulSyllableType(cp) != NA) return LB_XX;
            return getType(cp);
        }


        // find out whether there is a break at offset
        // WARNING: as a side effect, sets "rule"

        public boolean isBreak(String source, int offset) {

            // LB 1  Assign a line break category to each character of the input.
            // Resolve AI, CB, SA, SG, XX into other line break classes depending on criteria outside this algorithm.
            // this is taken care of in the getResolvedType function

            // LB 2a  Never break at the start of text

            setRule("2a: × sot");
            if (offset <= 0) return false;

            // LB 2b  Always break at the end of text

            setRule("2b: ! eot");
            if (offset >= source.length()) return true;


            // UTF-16: never break in the middle of a code point
            
            // now get the base character before and after, and their types

            getGraphemeBases(breaker, source, offset, -1, context);

            byte before = context.tBefore;
            byte after = context.tAfter;
            byte before2 = context.tBefore2;
            byte after2 = context.tAfter2;
            
            
            //if (!onCodepointBoundary(source, offset)) return false;


            // now get the character before and after, and their types


            //int cpBefore = UTF16.charAt(source, offset-1);
            //int cpAfter = UTF16.charAt(source, offset);

            //byte before = getResolvedType(cpBefore);
            //byte after = getResolvedType(cpAfter);


            setRule("3a: CR × LF ; ( BK | CR | LF | NL ) !");
            
            // Always break after hard line breaks (but never between CR and LF).
            // CR ^ LF
            if (before == LB_CR && after == LB_LF) return false;
            if (before == LB_BK || before == LB_LF || before == LB_CR) return true;

            //LB 3b  Don’t break before hard line breaks.
            setRule("3b: × ( BK | CR | LF )");
            if (after == LB_BK || after == LB_LF || after == LB_CR) return false;

            // LB 4  Don’t break before spaces or zero-width space.
            setRule("4: × ( SP | ZW )");
            if (after == LB_SP || after == LB_ZW) return false;

            // LB 5 Break after zero-width space.
            setRule("5: ZW ÷");
            if (before == LB_ZW) return true;

            // LB 6  Don’t break graphemes (before combining marks, around virama or on sequences of conjoining Jamos.
            setRule("6: DGC -> FC");
            if (!grapheme.isBreak( source,  offset)) return false;
            
            /*
            if (before == LB_L && (after == LB_L || after == LB_V || after == LB_LV || after == LB_LVT)) return false;
            if ((before == LB_LV || before == LB_V) && (after == LB_V || after == LB_T)) return false;
            if ((before == LB_LVT || before == LB_T) && (after == LB_T)) return false;
            */
            
            byte backBase = -1;
            boolean setBase = false;
            if (before == LB_CM) {
                setBase = true;
                int backOffset = findLastNon(source, offset, LB_CM);
                if (backOffset >= 0) {
                    backBase = getResolvedType(UTF16.charAt(source, backOffset));
                }
            }
            
            
            // LB 7  In all of the following rules, if a space is the base character for a combining mark,
            // the space is changed to type ID. In other words, break before SP CM* in the same cases as
            // one would break before an ID.
            setRule("7: SP CM* -> ID");
            if (setBase && backBase == LB_SP) before = LB_ID;
            if (after == LB_SP && after2 == LB_CM) after = LB_ID;

            setRule("7a: X CM* -> X");
            if (after == LB_CM) return false;
            if (setBase && backBase != -1) before = LB_ID;

            setRule("7b: CM -> AL");
            if (setBase && backBase == -1) before = LB_AL;

            
            // LB 8  Don’t break before ‘]’ or ‘!’ or ‘;’ or ‘/’,  even after spaces.
            // × CL, × EX, × IS, × SY
            setRule("8: × ( CL | EX | IS | SY )");
            if (after == LB_CL || after == LB_EX || after == LB_SY | after == LB_IS) return false;


            // find the last non-space character; we will need it
            byte lastNonSpace = before;
            if (lastNonSpace == LB_SP) {
                int backOffset = findLastNon(source, offset, LB_SP);
                if (backOffset >= 0) {
                    lastNonSpace = getResolvedType(UTF16.charAt(source, backOffset));
                }
            }

            // LB 9  Don’t break after ‘[’, even after spaces.
            // OP SP* ×
            setRule("9: OP SP* ×");
            if (lastNonSpace == LB_OP) return false;

            // LB 10  Don’t break within ‘�?[’, , even with intervening spaces.
            // QU SP* × OP
            setRule("10: QU SP* × OP");
            if (lastNonSpace == LB_QU && after == LB_OP) return false;

            // LB 11  Don’t break within ‘]h’, even with intervening spaces.
            // CL SP* × NS
            setRule("11: CL SP* × NS");
            if (lastNonSpace == LB_CL && after == LB_NS) return false;

            // LB 11a  Don’t break within ‘——’, even with intervening spaces.
            // B2 × B2
            setRule("11a: B2 × B2");
            if (lastNonSpace == LB_B2 && after == LB_B2) return false;


            // LB 13  Don’t break before or after NBSP or WORD JOINER
            // × GL
            // GL ×

            setRule("11b: × WJ ; WJ ×");
            if (after == LB_WJ || before == LB_WJ) return false;

            // [Note: by this time, all of the "X" in the table are accounted for. We can safely break after spaces.]

            // LB 12  Break after spaces
            setRule("12: SP ÷");
            if (before == LB_SP) return true;

            // LB 13  Don’t break before or after NBSP or WORD JOINER
            setRule("13: × GL ; GL ×");
            if (after == LB_GL || before == LB_GL) return false;

            // LB 14  Don’t break before or after ‘�?’
            setRule("14: × QU ; QU ×");
            if (before == LB_QU || after == LB_QU) return false;

            // LB 14a  Break before and after CB
            setRule("14a: ÷ CB ; CB ÷");
            if (before == LB_CB || after == LB_CB) return true;

            // LB 15  Don’t break before hyphen-minus, other hyphens, fixed-width spaces,
            // small kana and other non- starters,  or after acute accents:

            setRule("15: × ( BA | HY | NS ) ; BB ×");
            if (after == LB_NS) return false;
            if (after == LB_HY) return false;
            if (after == LB_BA) return false;
            if (before == LB_BB) return false;


            //setRule("15a: HY × NU"); // NEW
            //if (before == LB_HY && after == LB_NU) return false;

            // LB 16  Don’t break between two ellipses, or between letters or numbers and ellipsis:
            // Examples: ’9...’, ‘a...’, ‘H...’
            setRule("16: ( AL | ID | IN | NU ) × IN");
            if ((before == LB_NU || before == LB_AL || before == LB_ID) && after == LB_IN) return false;
            if (before == LB_IN && after == LB_IN) return false;

            // Don't break alphanumerics.
            // LB 17  Don’t break within ‘a9’, ‘3a’, or ‘H%’
            // Numbers are of the form PR ? ( OP | HY ) ? NU (NU | IS) * CL ?  PO ?
            // Examples:   $(12.35)    2,1234    (12)¢    12.54¢
            // This is approximated with the following rules. (Some cases already handled above,
            // like ‘9,’, ‘[9’.)
            setRule("17: ID × PO ; AL × NU; NU × AL");
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

            setRule("18: CL × PO ; NU × PO ; ( IS | NU | HY | PR | SY ) × NU ; PR × ( AL | HY | ID | OP )");
            if (before == LB_CL && after == LB_PO) return false;
            if (before == LB_IS && after == LB_NU) return false;
            if (before == LB_NU && after == LB_NU) return false;
            if (before == LB_NU && after == LB_PO) return false;
            
            if (before == LB_HY && after == LB_NU) return false;

            if (before == LB_PR && after == LB_AL) return false;
            if (before == LB_PR && after == LB_HY) return false;
            if (before == LB_PR && after == LB_ID) return false;
            if (before == LB_PR && after == LB_NU) return false;
            if (before == LB_PR && after == LB_OP) return false;

            if (before == LB_SY && after == LB_NU) return false;

            // LB 15b  Break after hyphen-minus, and before acute accents:
            setRule("18b: HY ÷ ; ÷ BB");
            if (before == LB_HY) return true;
            if (after == LB_BB) return true;

            // LB 19  Don’t break between alphabetics (“at�?)
            // AL × AL

            setRule("19: AL × AL");
            if (before == LB_AL && after == LB_AL) return false;

            // LB 20  Break everywhere else
            // ALL ÷
            // ÷ ALL

            if (ucd.getCompositeVersion() > 0x040000) {
                setRule("19b: IS × AL");
                if (before == LB_IS && after == LB_AL) return false;
            }

            // LB 20  Break everywhere else
            // ALL ÷
            // ÷ ALL

            setRule("20: ALL ÷ ; ÷ ALL");
            return true;
        }
    }

    //==============================================

    static class XGenerateSentenceBreakTest extends GenerateBreakTest {
        
        GenerateGraphemeBreakTest grapheme;
        MyBreakIterator breaker;
        
        XGenerateSentenceBreakTest(UCD ucd) {
            super(ucd);
            grapheme = new GenerateGraphemeBreakTest(ucd);
            breaker = new MyBreakIterator(grapheme);

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

        Object foo = prop = unicodePropertySource.getProperty("Sentence_Break");

        final int
            Sep =    addToMap("Sep"),
            Format =    addToMap("Format"),
            Sp = addToMap("Sp"),
            Lower = addToMap("Lower"),
            Upper = addToMap("Upper"),
            OLetter = addToMap("OLetter"),
            Numeric =     addToMap("Numeric"),
            ATerm =     addToMap("ATerm"),
            STerm =    addToMap("STerm"),
            Close =     addToMap("Close"),
            Other = addToMapLast("Other");            
                
        // stuff that subclasses need to override
        public String getTypeID(int cp) {
            return map.getLabel(cp);
        }

        public String fullBreakSample() {
            return "!a";
        }

       // stuff that subclasses need to override
        public byte getType(int cp) {
            return (byte) map.getIndex(cp);
        }

        /*LB_XX = 0, LB_OP = 1, LB_CL = 2, LB_QU = 3, LB_GL = 4, LB_NS = 5, LB_EX = 6, LB_SY = 7,
        LB_IS = 8, LB_PR = 9, LB_PO = 10, LB_NU = 11, LB_AL = 12, LB_ID = 13, LB_IN = 14, LB_HY = 15,
        LB_CM = 16, LB_BB = 17, LB_BA = 18, LB_SP = 19, LB_BK = 20, LB_CR = 21, LB_LF = 22, LB_CB = 23,
        LB_SA = 24, LB_AI = 25, LB_B2 = 26, LB_SG = 27, LB_ZW = 28,
        LB_NL = 29,
        LB_WJ = 30,
        */
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
        
        /*
        // stuff that subclasses need to override
        public String getTypeID(int cp) {
            byte type = getType(cp);
            return Names[type];
        }

        // stuff that subclasses need to override
        public byte getType(int cp) {
            byte cat = ucd.getCategory(cp);
            
            if (cat == Cf) return Format;
            if (sepSet.contains(cp)) return Sep;
            if (ucd.getBinaryProperty(cp, White_space)) return Sp;
            if (linebreakNS.contains(cp)) return Numeric;
            if (lowercaseProp.hasValue(cp)) return Lower;
            if (uppercaseProp.hasValue(cp) || cat == Lt) return Upper;
            if (alphabeticSet.contains(cp)) return OLetter;
            if (atermSet.contains(cp)) return ATerm;
            if (termSet.contains(cp)) return Term;
            if (cat == Po || cat == Pe
                || ucd.getLineBreak(cp) == LB_QU) return Close;
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
        
        public boolean isBreak(String source, int offset) {
    
            // Break at the start and end of text.
            setRule("1: sot ÷");
            if (offset < 0 || offset > source.length()) return false;
  
            if (offset == 0) return true;

            setRule("2: ÷ eot");
            if (offset == source.length()) return true;

            setRule("3: Sep ÷");
            byte beforeChar = getResolvedType(source.charAt(offset-1));
            if (beforeChar == Sep) return true;
            
            // Treat a grapheme cluster as if it were a single character:
            // the first base character, if there is one; otherwise the first character.

            setRule("4: GC -> FC");
            if (!grapheme.isBreak( source,  offset)) return false;
            
            // Ignore interior Format characters. That is, ignore Format characters in all subsequent rules.
            setRule("5: X Format* -> X");
            byte afterChar = getResolvedType(source.charAt(offset));
            if (afterChar == Format) return false;

            getGraphemeBases(breaker, source, offset, Format, context);
            byte before = context.tBefore;
            byte after = context.tAfter;
            byte before2 = context.tBefore2;
            byte after2 = context.tAfter2;
            
            // HACK COPY for rule collection!
            if (collectingRules) {
                setRule("6: ATerm × ( Numeric | Lower )");
                setRule("7: Upper ATerm × Upper");
                setRule("8: ATerm Close* Sp* × ( ¬(OLetter | Upper | Lower) )* Lower");
                setRule("9: ( Term | ATerm ) Close* × ( Close | Sp | Sep )");
                setRule("10: ( Term | ATerm ) Close* Sp × ( Sp | Sep )");
                setRule("11: ( Term | ATerm ) Close* Sp* ÷");
                setRule("12: Any × Any");
                collectingRules = false;
            }
            
            // Do not break after ambiguous terminators like period, if immediately followed by a number or lowercase letter, is between uppercase letters, or if the first following letter (optionally after certain punctuation) is lowercase. For example, a period may be an abbreviation or numeric period, and not mark the end of a sentence.
            
            if (before == ATerm) {
                setRule("6: ATerm × ( Numeric | Lower )");
                if (after == Lower || after == Numeric) return false;
                setRule("7: Upper ATerm × Upper");
                if (DEBUG_GRAPHEMES) System.out.println(context + ", " + Upper);
                if (before2 == Upper && after == Upper) return false;
            }
            
            // The following cases are all handled together.
            
            // First we loop backwards, checking for the different types.
            
            MyBreakIterator graphemeIterator = new MyBreakIterator(grapheme);
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
                t = getResolvedType(cp);
                if (SHOW_TYPE) System.out.println(ucd.getCodeAndName(cp) + ", " + getTypeID(cp));
                
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
                } else if (t == STerm) {
                    lookAfter = STerm;
                }
                break;
            }
            
            // if we didn't find ATerm or Term, bail
            
            if (lookAfter == -1) {
                // Otherwise, do not break
                // Any × Any (11)
                setRule("12: Any × Any");
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
                t = getResolvedType(cp);
                if (SHOW_TYPE) System.out.println(ucd.getCodeAndName(cp) + ", " + getTypeID(cp));
                    
                if (t == Format) continue;  // skip format characters!
                    
                if (isFirst) {
                    isFirst = false;
                    if (lookAfter == ATerm && t == Upper) {
                        setRule("8: ATerm Close* Sp* × ( ¬(OLetter | Upper | Lower) )* Lower");
                        return false;
                    }
                    if (gotSpace) {
                        if (t == Sp || t == Sep) {
                            setRule("10: ( Term | ATerm ) Close* Sp × ( Sp | Sep )");
                            return false;
                        }
                    } else if (t == Close || t == Sp || t == Sep) {
                        setRule("9: ( Term | ATerm ) Close* × ( Close | Sp | Sep )");
                        return false;
                    }
                    if (lookAfter == STerm) break;
                }
                    
                // at this point, we have an ATerm. All other conditions are ok, but we need to verify 6
                if (t != OLetter && t != Upper && t != Lower) continue;
                if (t == Lower) {
                    setRule("8: ATerm Close* Sp* × ( ¬(OLetter | Upper | Lower) )* Lower");
                    return false;
                }
                break;
            }
            setRule("11: ( Term | ATerm ) Close* Sp* ÷");
            return true;
        }
    }
    
    static final boolean DEBUG_GRAPHEMES = false;
    
    static class MyBreakIterator {
        int offset = 0;
        String string = "";
        GenerateBreakTest breaker;
        boolean recommended = true;
        
        MyBreakIterator(GenerateBreakTest breaker) {
            this.breaker = breaker; //  = new GenerateGraphemeBreakTest()
        }
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
                if (breaker.isBreak(string, offset)) break;
            }
            //if (DEBUG_GRAPHEMES) System.out.println(Utility.hex(result));
            return result;
        }
        
        public int previousBase() {
            if (offset <= 0) return -1;
            for (--offset; offset >= 0; --offset) {
                if (breaker.isBreak(string, offset)) break;
            }
            int result = UTF16.charAt(string, offset);
            //if (DEBUG_GRAPHEMES) System.out.println(Utility.hex(result));
            return result;
        }
    }
    /*
     * 
     *         if (false) {
            
            PrintWriter log = Utility.openPrintWriter("Diff.txt", Utility.UTF8_WINDOWS);
            UnicodeSet Term = new UnicodeSet(
                "[\\u0021\\u003F\\u0589\\u061F\\u06D4\\u0700\\u0701\\u0702\\u0964\\u1362\\u1367"
                + "\\u1368\\u104A\\u104B\\u166E\\u1803\\u1809\\u203C\\u203D\\u2047\\u2048\\u2049"
                + "\\u3002\\uFE52\\uFE57\\uFF01\\uFF0E\\uFF1F\\uFF61]");
            UnicodeSet terminal_punctuation = getSet(BINARY_PROPERTIES, Terminal_Punctuation);
            UnicodeMap names = new UnicodeMap();
            names.add("Pd", getSet(CATEGORY, Pd));
            names.add("Ps", getSet(CATEGORY, Ps));
            names.add("Pe", getSet(CATEGORY, Pe));
            names.add("Pc", getSet(CATEGORY, Pc));
            names.add("Po", getSet(CATEGORY, Po));
            names.add("Pi", getSet(CATEGORY, Pi));
            names.add("Pf", getSet(CATEGORY, Pf));
            
            Utility.showSetDifferences(log, "Term", Term, "Terminal_Punctuation", terminal_punctuation, true, true, names, ucd);
            Utility.showSetDifferences(log, "Po", getSet(CATEGORY, Po), "Terminal_Punctuation", terminal_punctuation, true, true, names, ucd);
            log.close();
            
            if (true) return;
                
            UnicodeSet whitespace = getSet(BINARY_PROPERTIES, White_space);
            UnicodeSet space = getSet(CATEGORY, Zs).addAll(getSet(CATEGORY, Zp)).addAll(getSet(CATEGORY, Zl));
            Utility.showSetDifferences("White_Space", whitespace, "Z", space, true, ucd);
            
            UnicodeSet isSpace = new UnicodeSet();
            UnicodeSet isSpaceChar = new UnicodeSet();
            UnicodeSet isWhitespace = new UnicodeSet();
            for (int i = 0; i <= 0xFFFF; ++i) {
                if (Character.isSpace((char)i)) isSpace.add(i);
                if (Character.isSpaceChar((char)i)) isSpaceChar.add(i);
                if (Character.isWhitespace((char)i)) isWhitespace.add(i);
            }
            Utility.showSetDifferences("White_Space", whitespace, "isSpace", isSpace, true, ucd);
            Utility.showSetDifferences("White_Space", whitespace, "isSpaceChar", isSpaceChar, true, ucd);
            Utility.showSetDifferences("White_Space", whitespace, "isWhitespace", isWhitespace, true, ucd);
            return;
        }
        
        if (DEBUG) {
            checkDecomps();

            Utility.showSetNames("", new UnicodeSet("[\u034F\u00AD\u1806[:DI:]-[:Cs:]-[:Cn:]]"), true, ucd);

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
            GenerateBreakTest foo = new GenerateLineBreakTest();
            //foo.isBreak("(\"Go.\") (He did)", 5, true);
            foo.isBreak("\u4e00\u4300", 1, true);
            /*
            GenerateSentenceBreakTest foo = new GenerateSentenceBreakTest();
            //foo.isBreak("(\"Go.\") (He did)", 5, true);
            foo.isBreak("3.4", 2, true);
            * /
        }

        new GenerateGraphemeBreakTest().run();
        new GenerateWordBreakTest().run();
        new GenerateLineBreakTest().run();
        new GenerateSentenceBreakTest().run();
        
        //if (true) return; // cut short for now
        
    }

     */
}

