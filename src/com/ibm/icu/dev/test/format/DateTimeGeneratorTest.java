//##header J2SE15
//#if defined(FOUNDATION10) || defined(J2SE13)
//#else
/*
 *******************************************************************************
 * Copyright (C) 2006-2007, Google, International Business Machines Corporation *
 * and others. All Rights Reserved.                                            *
 *******************************************************************************
 */

package com.ibm.icu.dev.test.format;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.impl.PatternTokenizer;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.DateTimePatternGenerator;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;

import java.util.Date;
import java.util.Random;

public class DateTimeGeneratorTest extends TestFmwk {
    public static boolean GENERATE_TEST_DATA = System.getProperty("GENERATE_TEST_DATA") != null;
    public static int RANDOM_COUNT = 1000;
    public static boolean DEBUG = false;
    
    public static void main(String[] args) throws Exception {
        new DateTimeGeneratorTest().run(args);
    }
    
    public void TestSimple() {
      
      // some simple use cases
      Date sampleDate = new Date(99, 9, 13, 23, 58, 59);
      ULocale locale = ULocale.GERMANY;
      TimeZone zone = TimeZone.getTimeZone("Europe/Paris");
      // make from locale
      DateTimePatternGenerator gen = DateTimePatternGenerator.getInstance(locale);
      SimpleDateFormat format = new SimpleDateFormat(gen.getBestPattern("MMMddHmm"), locale);
      format.setTimeZone(zone);
      assertEquals("simple format: MMMddHmm", "8:58 14. Okt", format.format(sampleDate));
      // (a generator can be built from scratch, but that is not a typical use case)
      
      // modify the generator by adding patterns
      DateTimePatternGenerator.PatternInfo returnInfo = new DateTimePatternGenerator.PatternInfo();
      gen.addPattern("d'. von' MMMM", true, returnInfo); 
      // the returnInfo is mostly useful for debugging problem cases
      format.applyPattern(gen.getBestPattern("MMMMddHmm"));
      assertEquals("modified format: MMMddHmm", "8:58 14. von Oktober", format.format(sampleDate));
      
      // get a pattern and modify it
      format = (SimpleDateFormat)DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL, locale);
      format.setTimeZone(zone);
      String pattern = format.toPattern();
      assertEquals("full-date", "Donnerstag, 14. Oktober 1999 08:58:59 Frankreich", format.format(sampleDate));
      
      // modify it to change the zone.
      String newPattern = gen.replaceFieldTypes(pattern, "vvvv");
      format.applyPattern(newPattern);
      assertEquals("full-date: modified zone", "Donnerstag, 14. Oktober 1999 08:58:59 Frankreich", format.format(sampleDate));
    }
    
    public void TestPatternParser() {
        StringBuffer buffer = new StringBuffer();
        PatternTokenizer pp = new PatternTokenizer()
        .setIgnorableCharacters(new UnicodeSet("[-]"))
        .setSyntaxCharacters(new UnicodeSet("[a-zA-Z]"))
        .setEscapeCharacters(new UnicodeSet("[b#]"))
        .setUsingQuote(true);
        logln("Using Quote");
        for (int i = 0; i < patternTestData.length; ++i) {
            String patternTest = (String) patternTestData[i];
            CheckPattern(buffer, pp, patternTest);
        }
        String[] randomSet = {"abcdef", "$12!@#-", "'\\"};
        for (int i = 0; i < RANDOM_COUNT; ++i) {
            String patternTest = getRandomString(randomSet, 0, 10);
            CheckPattern(buffer, pp, patternTest);
        }
        logln("Using Backslash");
        pp.setUsingQuote(false).setUsingSlash(true);
        for (int i = 0; i < patternTestData.length; ++i) {
            String patternTest = (String) patternTestData[i];
            CheckPattern(buffer, pp, patternTest);
        }
        for (int i = 0; i < RANDOM_COUNT; ++i) {
            String patternTest = getRandomString(randomSet, 0, 10);
            CheckPattern(buffer, pp, patternTest);
        }
    }
    
    Random random = new java.util.Random(-1);
    
    private String getRandomString(String[] randomList, int minLen, int maxLen) {
        StringBuffer result = new StringBuffer();
        int len = random.nextInt(maxLen + 1 - minLen) + minLen;
        for (int i = minLen; i < len; ++ i) {
            String source = randomList[random.nextInt(randomList.length)]; // don't bother with surrogates
            char ch = source.charAt(random.nextInt(source.length()));
            UTF16.append(result, ch);
        }
        return result.toString();
    }
    
    private void CheckPattern(StringBuffer buffer, PatternTokenizer pp, String patternTest) {
        pp.setPattern(patternTest);
        if (DEBUG && isVerbose()) {
            showItems(buffer, pp, patternTest);
        }
        String normalized = pp.setStart(0).normalize();
        logln("input:\t<" + patternTest + ">" + "\tnormalized:\t<" + normalized + ">");
        String doubleNormalized = pp.setPattern(normalized).normalize();
        if (!normalized.equals(doubleNormalized)) {
            errln("Normalization not idempotent:\t" + patternTest + "\tnormalized: " + normalized +  "\tnormalized2: " + doubleNormalized);
            // allow for debugging at the point of failure
            if (DEBUG) {
                pp.setPattern(patternTest);
                normalized = pp.setStart(0).normalize();
                pp.setPattern(normalized);
                showItems(buffer, pp, normalized);
                doubleNormalized = pp.normalize();
            }
        }
    }

    private void showItems(StringBuffer buffer, PatternTokenizer pp, String patternTest) {
        logln("input:\t<" + patternTest + ">");
        while (true) {
            buffer.setLength(0);
            int status = pp.next(buffer);
            if (status == PatternTokenizer.DONE) break;
            String lit = "";
            if (status != PatternTokenizer.SYNTAX ) {
                lit = "\t<" + pp.quoteLiteral(buffer) + ">";
            }
            logln("\t" + statusName[status] + "\t<" + buffer + ">" + lit);
        }
    }
    
    static final String[] statusName = {"DONE", "SYNTAX", "LITERAL", "BROKEN_QUOTE", "BROKEN_ESCAPE", "UNKNOWN"};
    
    public void TestBasic() {
        ULocale uLocale = null;
        DateTimePatternGenerator dtfg = null;
        Date date = null;
        for (int i = 0; i < dateTestData.length; ++i) {
            if (dateTestData[i] instanceof ULocale) {
                uLocale = (ULocale) dateTestData[i];
                dtfg = DateTimePatternGenerator.getInstance(uLocale);
                if (GENERATE_TEST_DATA) logln("new ULocale(\"" + uLocale.toString() + "\"),");
            } else if (dateTestData[i] instanceof Date) {
                date = (Date) dateTestData[i];
                if (GENERATE_TEST_DATA) logln("new Date(" + date.getYear() + ", " + date.getMonth() + ", " + date.getDate() + ", " + date.getHours() + ", " + date.getMinutes() + ", " + date.getSeconds()+ "),");
            } else if (dateTestData[i] instanceof String) {
                String testSkeleton = (String) dateTestData[i];
                String pattern = dtfg.getBestPattern(testSkeleton);
                SimpleDateFormat sdf = new SimpleDateFormat(pattern, uLocale);
                String formatted = sdf.format(date);
                if (GENERATE_TEST_DATA) logln("new String[] {\"" + testSkeleton + "\", \"" + Utility.escape(formatted) + "\"},");
                //logln(uLocale + "\t" + testSkeleton + "\t" + pattern + "\t" + sdf.format(date));
            } else {
                String[] testPair = (String[]) dateTestData[i];
                String testSkeleton = testPair[0];
                String testFormatted = testPair[1];
                String pattern = dtfg.getBestPattern(testSkeleton);
                SimpleDateFormat sdf = new SimpleDateFormat(pattern, uLocale);
                String formatted = sdf.format(date);
                if (GENERATE_TEST_DATA) {
                    logln("new String[] {\"" + testSkeleton + "\", \"" + Utility.escape(formatted) + "\"},");
                } else if (!formatted.equals(testFormatted)) {
                    if(skipIfBeforeICU(3,8,0)&& uLocale.equals("zh_Hans_CN") && testSkeleton.equals("HHmm")){
                        logln(uLocale + "\tformatted string doesn't match test case: " + testSkeleton + "\t generated: " +  pattern + "\t expected: " + testFormatted + "\t got: " + formatted);
                        continue;
                    }
                        
                    errln(uLocale + "\tformatted string doesn't match test case: " + testSkeleton + "\t generated: " +  pattern + "\t expected: " + testFormatted + "\t got: " + formatted);
                    if (true) { // debug
                        pattern = dtfg.getBestPattern(testSkeleton);
                        sdf = new SimpleDateFormat(pattern, uLocale);
                        formatted = sdf.format(date);
                    }
                }
                //logln(uLocale + "\t" + testSkeleton + "\t" + pattern + "\t" + sdf.format(date));
            }
        }
    }
    
    static final Object[] patternTestData = {
        "'$f''#c",
        "'' 'a",
        "'.''.'",
        "\\u0061\\\\",
        "mm.dd 'dd ' x",
        "'' ''",
    };
    
    // can be generated by using GENERATE_TEST_DATA. Must be reviewed before adding
    static final Object[] dateTestData = {
        new Date(99, 0, 13, 23, 58, 59),
        new ULocale("en_US"),
        new String[] {"yM", "1/1999"},
        new String[] {"yMMM", "Jan 1999"},
        new String[] {"yMd", "1/13/1999"},
        new String[] {"yMMMd", "Jan/13/1999"},
        new String[] {"Md", "1/13"},
        new String[] {"MMMd", "Jan 13"},
        new String[] {"yQQQ", "Q1 1999"},
        new String[] {"hhmm", "11:58 PM"},
        new String[] {"HHmm", "23:58"},
        new String[] {"mmss", "58:59"},
        new ULocale("zh_Hans_CN"),
        new String[] {"yM", "1999-1"},
        new String[] {"yMMM", "1999-01"},
        new String[] {"yMd", "1999\u5E741\u670813\u65E5"},
        new String[] {"yMMMd", "1999\u5E7401\u670813\u65E5"},
        new String[] {"Md", "1-13"},
        new String[] {"MMMd", "01-13"},
        new String[] {"yQQQ", "1999 Q1"},
        new String[] {"hhmm", "\u4E0B\u534811:58"},
        new String[] {"HHmm", "\u4E0B\u534811:58"},
        new String[] {"mmss", "58:59"},
        new ULocale("de_DE"),
        new String[] {"yM", "1.1999"},
        new String[] {"yMMM", "Jan 1999"},
        new String[] {"yMd", "13.1.1999"},
        new String[] {"yMMMd", "13. Jan 1999"},
        new String[] {"Md", "13.1."},   // 13.1
        new String[] {"MMMd", "13. Jan"},
        new String[] {"yQQQ", "Q1 1999"},
        new String[] {"hhmm", "23:58"},  // 11:58 nachm.
        new String[] {"HHmm", "23:58"},
        new String[] {"mmss", "58:59"},
        new ULocale("fi"),
        new String[] {"yM", "1/1999"},   // 1.1999
        new String[] {"yMMM", "tammi 1999"},  // tammita 1999
        new String[] {"yMd", "13.1.1999"},
        new String[] {"yMMMd", "13. tammita 1999"},
        new String[] {"Md", "13.1."},
        new String[] {"MMMd", "13. tammita"},
        new String[] {"yQQQ", "1. nelj./1999"},  // 1. nelj. 1999
        new String[] {"hhmm", "23.58"},
        new String[] {"HHmm", "23.58"},
        new String[] {"mmss", "58.59"},
    };
}
//#endif
//eof
