/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/unicodetools/com/ibm/text/UCD/GenerateLineBreakTest.java,v $
* $Date: 2002/08/04 21:38:45 $
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

public class GenerateLineBreakTest implements UCD_Types {
    
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

    //============================
    
    protected String rule;
    protected String fileName = "Line";

    // all the other items are supplied in UCD_TYPES
    static byte LB_L = LB_LIMIT + hL, LB_V = LB_LIMIT + hV, LB_T = LB_LIMIT + hT, 
        LB_LV = LB_LIMIT + hLV, LB_LVT = LB_LIMIT + hLVT, LB_SUP = LB_LIMIT + hLIMIT,
        LB2_LIMIT = (byte)(LB_SUP + 1);
    
    String[] samples = new String[100];
    
    
    byte[] TypeOrder = {
        LB_OP, LB_CL, LB_QU, LB_GL, LB_NS, LB_EX, LB_SY, LB_IS, LB_PR, LB_PO,
        LB_NU, LB_AL, LB_ID, LB_IN, LB_HY, LB_BA, LB_BB, LB_B2, LB_ZW, LB_CM,
        // missing from Pair Table
        LB_SP, LB_BK, LB_CR, LB_LF, 
        // resolved types below
        LB_CB, LB_AI, LB_SA, LB_SG, LB_XX,
        // 3 JAMO CLASSES, plus supplementary
        LB_L, LB_V, LB_T, LB_LV, LB_LVT, LB_SUP
    };
    
    public static void main(String[] args) throws IOException {
        Default.setUCD();
        new GenerateLineBreakTest().run();
        
        new GenerateWordBreakTest().run();
    }
    
    // stuff that subclasses need to override
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
            out.println("# These samples may be extended in the future.");
            out.println("#");
            
            for (int ii = 0; ii < getLimit(); ++ii) {
                int i = TypeOrder[ii];
                if (i == LB_SG) continue;
                String before = samples[i];
                
                for (int jj = 0; jj < getLimit(); ++jj) {
                    Utility.dot(counter);
                    int j = TypeOrder[jj];
                    if (j == LB_SG) continue;
                    String after = samples[j];
                    // do line straight
                    int len = genTestItems(before, after, testCase);
                    for (int q = 0; q < len; ++q) {
                        printLine(out, testCase[q], k != 0 && q == 0, false);
                        ++counter;
                    }
                }
            }
            out.println("# Lines: " + counter);
            out.close();
        }
    }
    
    // stuff that subclasses need to override
    public int genTestItems(String before, String after, String[] results) {
        results[0] = before + after;
        results[1] = before + " " + after;
        results[2] = before + "\u0301\u0308" + after;
        return 3;
    }
    
    // stuff that subclasses need to override
    boolean skipType(byte type) {
        return type == LB_AI || type == LB_SA || type == LB_SG || type == LB_XX;
    }
    
    // stuff that subclasses need to override
    public String getTypeID(int cp) {
        byte result = getType(cp);
        if (result == LB_SUP) return "SUP";
        if (result >= LB_LIMIT) return hNames[result - LB_LIMIT];
        return Default.ucd.getLineBreakID_fromIndex(result);
    }
    
    // stuff that subclasses need to override
    public byte getType(int cp) {
        if (cp > 0xFFFF) return LB_SUP;
        byte result = getHangulType(cp);
        if (result != hNot) return (byte)(result + LB_LIMIT);
        return Default.ucd.getLineBreak(cp);
    }
    
    public int getLimit() {
        return LB2_LIMIT;
    }
    
    public int getTableLimit() {
        return LB_SUP; // skip last;
    }
    
    
    public void generateTable(PrintWriter out, boolean recommended) {
        String width = "width='" + (100 / (getTableLimit() + 1)) + "%'";
        out.print("<table border='1' cellspacing='0'><tr><th " + width + "></th>");
        byte type;
        for (int i = 0; i < getTableLimit(); ++i) {
            type = TypeOrder[i];
            if (skipType(type)) continue;
            
            String h = getTypeID(samples[TypeOrder[i]]);
            out.print("<th " + width + ">" + h + "</th>");
        }
        out.print("</tr>");
        String[] rule = new String[1];
        String[] rule2 = new String[1];
        for (int i = 0; i < getTableLimit(); ++i) {
            type = TypeOrder[i];
            if (skipType(type)) continue;
            
            String before = samples[type];
            String line = "<tr><th>" + getTypeID(before) + "</th>";
            for (int j = 0; j < getTableLimit(); ++j) {
                type = TypeOrder[j];
                if (skipType(type)) continue;
                
                String after = samples[type];
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
    
    static final String BREAK = "\u00F7";
    static final String NOBREAK = "\u00D7";
    
    public void printLine(PrintWriter out, String source, boolean comments, boolean recommended) {
        int cp;
        StringBuffer string = new StringBuffer();
        StringBuffer comment = new StringBuffer("\t# ");
        String status = isBreak(source, 0, recommended) ? BREAK : NOBREAK;
        string.append(status);
        comment.append(' ').append(status).append(" [").append(rule).append(']');
        
        for (int offset = 0; offset < source.length(); offset += UTF16.getCharCount(cp)) {
            
            cp = UTF16.charAt(source, offset);
            if (string.length() > 0) {
                string.append(' ');
                comment.append(' ');
            }
            
            string.append(Utility.hex(cp));
            comment.append(Default.ucd.getName(cp) + " (" + getTypeID(cp) + ")");
            
            status = isBreak(source, offset + UTF16.getCharCount(cp), recommended) ? BREAK : NOBREAK;
            string.append(' ').append(status);
            comment.append(' ').append(status).append(" [").append(rule).append(']');
        }
        
        if (comments) string.append(comment);
        out.println(string);
    }
    
    public void findSamples() {
        for (int i = 1; i <= 0x10FFFF; ++i) {
            if (!Default.ucd.isAllocated(i)) continue;
            if (0xD800 <= i && i <= 0xDFFF) continue;
            if(i == 0x1100) {
                System.out.print("here");
            }
            byte lb = getType(i);
            if (samples[lb] == null) {
                samples[lb] = UTF16.valueOf(i);
            }
        }
        for (int i = 0; i < TypeOrder.length; ++i) {
            String sample = samples[i];
            System.out.println(getTypeID(sample) + ":\t" + Default.ucd.getCodeAndName(sample));
        }
    }
       

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
       
    public int findLastNon(String source, int offset, byte notLBType, boolean recommended) {
        int cp;
        for (int i = offset-1; i >= 0; i -= UTF16.getCharCount(cp)) {
            cp = UTF16.charAt(source, i);
            byte f = getResolvedType(cp, recommended);
            if (f != notLBType) return i;
        }
        return -1;
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
    
    public boolean onCodepointBoundary(String s, int offset) {
        if (offset < 0 || offset > s.length()) return false;
        if (offset == 0 || offset == s.length()) return true;
        if (UTF16.isLeadSurrogate(s.charAt(offset-1))
        && UTF16.isTrailSurrogate(s.charAt(offset))) return false;
        return true;
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
    
    static class GenerateWordBreakTest extends GenerateLineBreakTest {
        
        static final byte CR = 0, LF = 1, Control = 2, Extend = 3, Link = 4, CGJ = 5, Base = 6, LetterBase = 7, Other = 8,
            oLIMIT = 9, // RESET THIS IF LIST ABOVE CHANGES!
            L = oLIMIT + hL, V = oLIMIT + hV, T = oLIMIT + hT, LV = oLIMIT + hLV, LVT = oLIMIT + hLVT,
            LIMIT = LVT + 1;

        static final String[] Names = {"CR", "LF", "CTL", "Extend", "Link", "CGJ", "Base", "LetterBase", "Other" };
        
        static UnicodeProperty extendProp = UnifiedBinaryProperty.make(DERIVED | GraphemeExtend);
        static UnicodeProperty baseProp = UnifiedBinaryProperty.make(DERIVED | GraphemeBase);
        static UnicodeProperty linkProp = UnifiedBinaryProperty.make(BINARY_PROPERTIES | GraphemeLink);

        {
            fileName = "Word";
            TypeOrder = new byte[LIMIT];
            for (byte i = 0; i < TypeOrder.length; ++i) {
                TypeOrder[i] = i;
            }
        }

        boolean skipType(byte type) {
            return false;
        }
      
        public int getLimit() {
            return LIMIT;
        }
   
        public int getTableLimit() {
            return LIMIT;
        }
        
        // stuff that subclasses need to override
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
    
        // stuff that subclasses need to override
        public String getTypeID(int cp) {
            byte type = getType(cp);
            if (type >= oLIMIT) return hNames[type - oLIMIT];
            return Names[type];
        }
        
        // stuff that subclasses need to override
        public byte getType(int cp) {
            // single characters
            if (cp == 0xA) return LF;
            if (cp == 0xD) return CR;
            if (cp == 0x034F) return CGJ;
            if (cp == 0x2028 || cp == 0x2029) return Control;
            
            // Hangul
            byte result = getHangulType(cp);
            if (result != hNot) return (byte)(result + oLIMIT);
            
            // other properties
            // category based
            byte cat = Default.ucd.getCategory(cp);
            if (cat == Cc) return Control;
            if (cat == Cf) return Extend;
            if (((1<<cat) & LETTER_MASK) != 0) return LetterBase;
            
            // other binary properties
            if (linkProp.hasValue(cp)) return Link;
            if (extendProp.hasValue(cp)) return Extend;
            if (baseProp.hasValue(cp)) return Base;
            
            return Other;
        }
        
        public byte getResolvedType(int cp, boolean recommended) {
            return getType(cp);
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
}