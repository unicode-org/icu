/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/unicodetools/com/ibm/text/UCD/GenerateLineBreakTest.java,v $
* $Date: 2002/07/30 09:57:18 $
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

public class GenerateLineBreakTest implements UCD_Types {

    static String[] samples = new String[LB_LIMIT + 3];
    
    static byte[] TROrder = {
        LB_OP, LB_CL, LB_QU, LB_GL, LB_NS, LB_EX, LB_SY, LB_IS, LB_PR, LB_PO,
        LB_NU, LB_AL, LB_ID, LB_IN, LB_HY, LB_BA, LB_BB, LB_B2, LB_ZW, LB_CM,
        // missing from Pair Table
        LB_SP, LB_BK, LB_CR, LB_LF, 
        // resolved types below
        LB_CB, LB_AI, LB_SA, LB_SG, LB_XX,
        // 3 JAMO CLASSES
        29, 30, 31
    };
    static final int TABLE_LIMIT = 25;
     
    
    public static void main(String[] args) throws IOException {
        Default.setUCD();
        
        findSamples();
        
        // test individual cases
        //printLine(out, samples[LB_ZW], "", samples[LB_CL]);
        //printLine(out, samples[LB_ZW], " ", samples[LB_CL]);
        
        PrintWriter out = Utility.openPrintWriter("LineBreakTest.html", Utility.UTF8_WINDOWS);
        out.println("<html><body><h1>Current (fixed only for consistency):</h1>");
        generateTable(out, false);
        out.println("<h1>Recommended:</h1>");
        generateTable(out, true);
        out.println("</body></html>");
        out.close();
        
        // do main test
        
        for (int k = 0; k < 2; ++k) {
            out = Utility.openPrintWriter(k == 0 ? "LineBreakTest_SHORT.txt" : "LineBreakTest.txt", Utility.UTF8_WINDOWS);
            int counter = 0;
            
            out.println("# Default Linebreak conformance test");
            out.println("# " + Default.getDate() + ", MED");
            out.println("#");
            
            for (int ii = 0; ii < samples.length; ++ii) {
                int i = TROrder[ii];
                String before = samples[i];
                
                for (int jj = 0; jj < samples.length; ++jj) {
                    Utility.dot(counter++);
                    int j = TROrder[jj];
                    String after = samples[j];
                    // do line straight
                    printLine(out, before, "", after, k != 0);
                    printLine(out, before, " ", after, k != 0);
                    printLine(out, before, "\u0301\u0308", after, k != 0);
                }
            }
            out.println("# Lines: " + counter);
            out.close();
        }
    }
    
    public static void generateTable(PrintWriter out, boolean recommended) {
        out.print("<table border='1' cellspacing='0'><tr><th></th>");
        for (int i = 0; i < TABLE_LIMIT; ++i) {
            String h = getLBID(samples[TROrder[i]]);
            out.print("<th>" + h + "</th>");
        }
        out.print("</tr>");
        String[] rule = new String[1];
        String[] rule2 = new String[1];
        for (int i = 0; i < TABLE_LIMIT; ++i) {
            String before = samples[TROrder[i]];
            String line = "<tr><th>" + getLBID(before) + "</th>";
            for (int j = 0; j < TABLE_LIMIT; ++j) {
                String after = samples[TROrder[j]];
                String t = getTableEntry(before, after, recommended, rule);
                String background = "";
                if (recommended) {
                    String t2 = getTableEntry(before, after, false, rule2);
                    if (!t.equals(t2)) background = " bgcolor='#FFFF00'";
                }
                line += "<th title='" + rule[0] + "'" + background + ">" + t + "</th>";
            }
            out.println(line + "</tr>");
        }
        out.println("</table>");
    }
    
    public static String getTableEntry(String before, String after, boolean recommended, String[] ruleOut) {
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
    
    
    public static void printLine(PrintWriter out, String before, String filler, String after, boolean comments) {
        String s = before + filler + after;
        int offset = before.length() + filler.length();
        
        boolean lb = isBreak(s, offset, false);
        
        String tlb = (lb ? "b" : "n");
        String comment = "";
        if (comments) comment = 
            " # " + getLBID(before + filler)
            + " " + tlb
            + " " + getLBID(after)
            + " # " + Default.ucd.getName(before + filler)
            + " " + tlb
            + " " + Default.ucd.getName(after);
            
        out.println(Utility.hex(before + filler)
            + "; " + tlb
            + "; " + Utility.hex(after)
            + comment);
    }

    public static void findSamples() {
        for (int i = 1; i <= 0x10FFFF; ++i) {
            if (!Default.ucd.isAllocated(i)) continue;
            if (Default.ucd.isLeadingJamo(i) 
                || Default.ucd.isVowelJamo(i) 
                || Default.ucd.isTrailingJamo(i)) continue;
            byte lb = Default.ucd.getLineBreak(i);
            if (samples[lb] == null) {
                samples[lb] = UTF16.valueOf(i);
            }
        }
        // fill the last with special cases
        samples[LB_LIMIT] = "\u1100";
        samples[LB_LIMIT+1] = "\u1162";
        samples[LB_LIMIT+2] = "\u11A8";
    }
       

    public static String getLBID(String s) {
        if (s.length() == 1) return Default.ucd.getLineBreakID(s.charAt(0));
        StringBuffer result = new StringBuffer();
        int cp;
        for (int i = 0; i < s.length(); i += UTF32.count16(cp)) {
            cp = UTF32.char32At(s, i);
            if (i > 0) result.append(" ");
            result.append(Default.ucd.getLineBreakID(cp));
        }
        return result.toString();
    }
       
    static String rule;

    public static int findLastNon(String source, int offset, byte notLBType) {
        int cp;
        for (int i = offset-2; i >= 0; i -= UTF16.getCharCount(cp)) {
            cp = UTF16.charAt(source, i);
            byte f = getResolvedLB(cp);
            if (f != notLBType) return cp;
        }
        return 0;
    }

    public static byte getResolvedLB (int cp) {
        // LB 1  Assign a line break category to each character of the input.
        // Resolve AI, CB, SA, SG, XX into other line break classes depending on criteria outside this algorithm.
        byte result = Default.ucd.getLineBreak(cp);
        switch (result) {
            case LB_AI: result = LB_AI; break;
            // case LB_CB: result = LB_ID; break;
            case LB_SA: result = LB_AL; break;
            // case LB_SG: result = LB_XX; break; Surrogates; will never occur
            case LB_XX: result = LB_AL; break;
        }
        return result;
    }

    // find out whether there is a break at offset
    // WARNING: as a side effect, sets "rule"

    public static boolean isBreak(String source, int offset, boolean recommended) {

        // LB 1  Assign a line break category to each character of the input.
        // Resolve AI, CB, SA, SG, XX into other line break classes depending on criteria outside this algorithm.
        // this is taken care of in the getResolvedLB function

        // LB 2a  Never break at the start of text

        rule="2a";
        if (offset <= 0) return false;

        // LB 2b  Always break at the end of text

        rule="2b";
        if (offset >= source.length()) return true;


        // UTF-16: never break in the middle of a code point
        if (UTF16.isLeadSurrogate(source.charAt(offset-1))
            && UTF16.isTrailSurrogate(source.charAt(offset))) return false;


        // now get the character before and after, and their types


        int cpBefore = UTF16.charAt(source, offset-1);
        int cpAfter = UTF16.charAt(source, offset);

        byte before = getResolvedLB(cpBefore);
        byte after = getResolvedLB(cpAfter);


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
        if (Default.ucd.isLeadingJamo(cpBefore)) {
            if (Default.ucd.isLeadingJamo(cpAfter) || Default.ucd.isVowelJamo(cpAfter)) return false;
        } else if (Default.ucd.isVowelJamo(cpBefore)) {
            if (Default.ucd.isVowelJamo(cpAfter) || Default.ucd.isTrailingJamo(cpAfter)) return false;
        } else if (Default.ucd.isTrailingJamo(cpBefore)) {
            if (Default.ucd.isTrailingJamo(cpAfter)) return false;
        }

        boolean setBase = false;
        if (before == LB_CM) {
            setBase = true;
            int cp = findLastNon(source, offset, LB_CM);
            if (cp == 0) {
                before = LB_ID;
            } else {
                before = getResolvedLB(cp);
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
            int cp = findLastNon(source, offset, LB_CM);
            if (cp != 0) {
                lastNonSpace = getResolvedLB(cp);
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