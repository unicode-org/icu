/*
 *******************************************************************************
 * Copyright (C) 2006-2009, Google, International Business Machines Corporation *
 * and others. All Rights Reserved.                                            *
 *******************************************************************************
 */

package com.ibm.icu.dev.test.format;

import java.text.ParsePosition;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.impl.PatternTokenizer;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.DateFormat;
import com.ibm.icu.text.DateTimePatternGenerator;
import com.ibm.icu.text.SimpleDateFormat;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.text.DateTimePatternGenerator.FormatParser;
import com.ibm.icu.text.DateTimePatternGenerator.VariableField;
import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.GregorianCalendar;
import com.ibm.icu.util.SimpleTimeZone;
import com.ibm.icu.util.TimeZone;
import com.ibm.icu.util.ULocale;

public class DateTimeGeneratorTest extends TestFmwk {
    public static boolean GENERATE_TEST_DATA;
    static {
        try {
            GENERATE_TEST_DATA = System.getProperty("GENERATE_TEST_DATA") != null;
        } catch (SecurityException e) {
            GENERATE_TEST_DATA = false;
        }
    };
    public static int RANDOM_COUNT = 1000;
    public static boolean DEBUG = false;
    
    public static void main(String[] args) throws Exception {
        new DateTimeGeneratorTest().run(args);
    }
    
    public void TestSimple() {
        // some simple use cases
        ULocale locale = ULocale.GERMANY;
        TimeZone zone = TimeZone.getTimeZone("Europe/Paris");
        
        // make from locale
        DateTimePatternGenerator gen = DateTimePatternGenerator.getInstance(locale);
        SimpleDateFormat format = new SimpleDateFormat(gen.getBestPattern("MMMddHmm"), locale);
        format.setTimeZone(zone);
        assertEquals("simple format: MMMddHmm", "14. Okt 8:58", format.format(sampleDate));
        // (a generator can be built from scratch, but that is not a typical use case)

        // modify the generator by adding patterns
        DateTimePatternGenerator.PatternInfo returnInfo = new DateTimePatternGenerator.PatternInfo();
        gen.addPattern("d'. von' MMMM", true, returnInfo); 
        // the returnInfo is mostly useful for debugging problem cases
        format.applyPattern(gen.getBestPattern("MMMMddHmm"));
        assertEquals("modified format: MMMddHmm", "14. von Oktober 8:58", format.format(sampleDate));

        // get a pattern and modify it
        format = (SimpleDateFormat)DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL, locale);
        format.setTimeZone(zone);
        String pattern = format.toPattern();
        assertEquals("full-date", "Donnerstag, 14. Oktober 1999 08:58:59 Mitteleurop\u00E4ische Sommerzeit", format.format(sampleDate));

        // modify it to change the zone.
        String newPattern = gen.replaceFieldTypes(pattern, "vvvv");
        format.applyPattern(newPattern);
        assertEquals("full-date: modified zone", "Donnerstag, 14. Oktober 1999 08:58:59 Frankreich", format.format(sampleDate));
        
        // add test of basic cases

        //lang  YYYYMMM MMMd    MMMdhmm hmm hhmm    Full Date-Time
        // en  Mar 2007    Mar 4   6:05 PM Mar 4   6:05 PM 06:05 PM    Sunday, March 4, 2007 6:05:05 PM PT
        DateTimePatternGenerator enGen = DateTimePatternGenerator.getInstance(ULocale.ENGLISH);
        TimeZone enZone = TimeZone.getTimeZone("Etc/GMT");
        SimpleDateFormat enFormat = (SimpleDateFormat)DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL, ULocale.ENGLISH);
        enFormat.setTimeZone(enZone);
        String[][] tests = {
              {"yyyyMMMdd", "Oct 14, 1999"},
              {"yyyyqqqq", "4th quarter 1999"},
              {"yMMMdd", "Oct 14, 1999"},
              {"EyyyyMMMdd", "Thu, Oct 14, 1999"},
              {"yyyyMMdd", "10/14/1999"},
              {"yyyyMMM", "Oct 1999"},
              {"yyyyMM", "10/1999"},
              {"yyMM", "10/99"},
              {"yMMMMMd", "O 14, 1999"},  // narrow format
              {"EEEEEMMMMMd", "T, O 14"},  // narrow format
              {"MMMd", "Oct 14"},
              {"MMMdhmm", "Oct 14 6:58 AM"},
              {"EMMMdhmms", "Thu, Oct 14 6:58:59 AM"},
              {"MMdhmm", "10/14 6:58 AM"},
              {"EEEEMMMdhmms", "Thursday, Oct 14 6:58:59 AM"},
              {"yyyyMMMddhhmmss", "Oct 14, 1999 06:58:59 AM"},
              {"EyyyyMMMddhhmmss", "Thu, Oct 14, 1999 06:58:59 AM"},
              {"hmm", "6:58 AM"},
              {"hhmm", "06:58 AM"},
              {"hhmmVVVV", "06:58 AM GMT+00:00"},
        };
        for (int i = 0; i < tests.length; ++i) {
            final String testSkeleton = tests[i][0];
            String pat = enGen.getBestPattern(testSkeleton);
            enFormat.applyPattern(pat);
            String formattedDate = enFormat.format(sampleDate);
            assertEquals("Testing skeleton '" + testSkeleton + "' with  " + sampleDate, tests[i][1], formattedDate);
        }
    }

    public void TestRoot() {
        DateTimePatternGenerator rootGen = DateTimePatternGenerator.getInstance(ULocale.ROOT);
        SimpleDateFormat rootFormat = new SimpleDateFormat(rootGen.getBestPattern("yMdHms"), ULocale.ROOT);
        rootFormat.setTimeZone(gmt);
        assertEquals("root format: yMdHms", "1999-10-14 6:58:59", rootFormat.format(sampleDate));
    }
    
    public void TestEmpty() {
        // now nothing
        DateTimePatternGenerator nullGen = DateTimePatternGenerator.getEmptyInstance();
        SimpleDateFormat format = new SimpleDateFormat(nullGen.getBestPattern("yMdHms"), ULocale.ROOT);
        TimeZone rootZone = TimeZone.getTimeZone("Etc/GMT");
        format.setTimeZone(rootZone);
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
                if (GENERATE_TEST_DATA) logln("new Date(" + date.getTime()+ "L),");
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
        new Date(916300739000L), // 1999-01-13T23:58:59,0-0800
        new ULocale("en_US"),
        new String[] {"yM", "1/1999"},
        new String[] {"yMMM", "Jan 1999"},
        new String[] {"yMd", "1/13/1999"},
        new String[] {"yMMMd", "Jan 13, 1999"},
        new String[] {"Md", "1/13"},
        new String[] {"MMMd", "Jan 13"},
        new String[] {"yQQQ", "Q1 1999"},
        new String[] {"jjmm", "11:58 PM"},
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
        new String[] {"yQQQ", "1999\u5E741\u5B63"},
        new String[] {"hhmm", "\u4E0B\u534811:58"},
        new String[] {"HHmm", "23:58"},
        new String[] {"mmss", "58:59"},
        new ULocale("de_DE"),
        new String[] {"yM", "1999-1"},
        new String[] {"yMMM", "Jan 1999"},
        new String[] {"yMd", "13.1.1999"},
        new String[] {"yMMMd", "13. Jan 1999"},
        new String[] {"Md", "13.1."},   // 13.1
        new String[] {"MMMd", "13. Jan"},
        new String[] {"yQQQ", "Q1 1999"},
        new String[] {"jjmm", "23:58"},
        new String[] {"hhmm", "11:58 nachm."},
        new String[] {"HHmm", "23:58"},
        new String[] {"mmss", "58:59"},
        new ULocale("fi"),
        new String[] {"yM", "1/1999"},   // 1.1999
        new String[] {"yMMM", "tammikuuta 1999"},  // tammi 1999
        new String[] {"yMd", "13.1.1999"},
        new String[] {"yMMMd", "13. tammikuuta 1999"},
        new String[] {"Md", "13.1."},
        new String[] {"MMMd", "13. tammikuuta"},
        new String[] {"yQQQ", "1. nelj./1999"},  // 1. nelj. 1999
        new String[] {"jjmm", "23.58"},
        new String[] {"hhmm", "11.58 ip."},
        new String[] {"HHmm", "23.58"},
        new String[] {"mmss", "58.59"},
    };
    
    public void DayMonthTest() {
        final ULocale locale = ULocale.FRANCE;
        
        // set up the generator
        DateTimePatternGenerator dtpgen
          = DateTimePatternGenerator.getInstance(locale);
        
        // get a pattern for an abbreviated month and day
        final String pattern = dtpgen.getBestPattern("MMMd");
        SimpleDateFormat formatter = new SimpleDateFormat(pattern, locale);
        
        // use it to format (or parse)
        String formatted = formatter.format(new Date());
        logln("formatted=" + formatted);
        // for French, the result is "13 sept."
    }
    
    public void TestOrdering() {
        ULocale[] locales = ULocale.getAvailableLocales();
        for (int i = 0; i < locales.length; ++i) {
            for (int style1 = DateFormat.FULL; style1 <= DateFormat.SHORT; ++style1) {
                for (int style2 = DateFormat.FULL; style2 < style1; ++style2) {
                    checkCompatible(style1, style2, locales[i]);                    
                }               
            }
        }
    }
    
    public void TestReplacingZoneString() {
        Date testDate = new Date();
        TimeZone testTimeZone = TimeZone.getTimeZone("America/New_York");
        TimeZone bogusTimeZone = new SimpleTimeZone(1234, "Etc/Unknown");
        Calendar calendar = Calendar.getInstance();
        ParsePosition parsePosition = new ParsePosition(0);

        ULocale[] locales = ULocale.getAvailableLocales();
        int count = 0;
        for (int i = 0; i < locales.length; ++i) {
            // skip the country locales unless we are doing exhaustive tests
            if (getInclusion() < 6) {
                if (locales[i].getCountry().length() > 0) {
                    continue;
                }
            }
            count++;
            // Skipping some test case in the non-exhaustive mode to reduce the test time
            //ticket#6503
            if(params.inclusion<=5 && count%3!=0){
                continue;
            }
            logln(locales[i].toString());
            DateTimePatternGenerator dtpgen
            = DateTimePatternGenerator.getInstance(locales[i]);
            
            for (int style1 = DateFormat.FULL; style1 <= DateFormat.SHORT; ++style1) {
                final SimpleDateFormat oldFormat = (SimpleDateFormat) DateFormat.getTimeInstance(style1, locales[i]);
                String pattern = oldFormat.toPattern();
                String newPattern = dtpgen.replaceFieldTypes(pattern, "VVVV"); // replaceZoneString(pattern, "VVVV");
                if (newPattern.equals(pattern)) {
                    continue;
                }
                // verify that it roundtrips parsing
                SimpleDateFormat newFormat = new SimpleDateFormat(newPattern, locales[i]);
                newFormat.setTimeZone(testTimeZone);
                String formatted = newFormat.format(testDate);
                calendar.setTimeZone(bogusTimeZone);
                parsePosition.setIndex(0);
                newFormat.parse(formatted, calendar, parsePosition);
                if (parsePosition.getErrorIndex() >= 0) {
                    errln("Failed parse with VVVV:\t" + locales[i] + ",\t\"" + pattern + "\",\t\"" + newPattern + "\",\t\"" + formatted.substring(0,parsePosition.getErrorIndex()) + "{}" + formatted.substring(parsePosition.getErrorIndex()) + "\"");
                } else if (!calendar.getTimeZone().getID().equals(testTimeZone.getID())) {
                    errln("Failed timezone roundtrip with VVVV:\t" + locales[i] + ",\t\"" + pattern + "\",\t\"" + newPattern + "\",\t\"" + formatted + "\",\t" + calendar.getTimeZone().getID() + " != " + testTimeZone.getID());
                } else {
                    logln(locales[i] + ":\t\"" + pattern + "\" => \t\"" + newPattern + "\"\t" + formatted);
                }
            }
        }
    }
    
    public void TestVariableCharacters() {
        UnicodeSet valid = new UnicodeSet("[G   y   Y   u   Q   q   M   L   w   W   d   D   F   g   E   e   c   a   h   H   K   k   m   s   S   A   z   Z   v   V]");
        for (char c = 0; c < 0xFF; ++c) {
            boolean works = false;
            try {
                VariableField vf = new VariableField(String.valueOf(c), true);
                logln("VariableField " + vf.toString());
                works = true;
            } catch (Exception e) {}
            if (works != valid.contains(c)) {
                if (works) {
                    errln("VariableField can be created with illegal character: " + c);
                } else {
                    errln("VariableField can't be created with legal character: " + c);
                }
            }
        }
    }
    
    static String[] DATE_STYLE_NAMES = {
        "FULL", "LONG", "MEDIUM", "SHORT"
    };
    
    /**
     * @param fullOrder
     * @param longOrder
     */
    private void checkCompatible(int style1, int style2, ULocale uLocale) {
        DateOrder order1 = getOrdering(style1, uLocale);
        DateOrder order2 = getOrdering(style2, uLocale);
        if (!order1.hasSameOrderAs(order2)) {
            if (order1.monthLength == order2.monthLength) { // error if have same month length, different ordering
                if (skipIfBeforeICU(4,3,2)) {
                    logln(showOrderComparison(uLocale, style1, style2, order1, order2));
                } else {
                    errln(showOrderComparison(uLocale, style1, style2, order1, order2));
                }
            } else if (isVerbose() && order1.monthLength > 2 && order2.monthLength > 2) { // warn if both are not numeric
                logln(showOrderComparison(uLocale, style1, style2, order1, order2));
            }
        }
    }

    private String showOrderComparison(ULocale uLocale, int style1, int style2, DateOrder order1, DateOrder order2) {
        String pattern1 = ((SimpleDateFormat) DateFormat.getDateInstance(style1, uLocale)).toPattern();
        String pattern2 = ((SimpleDateFormat) DateFormat.getDateInstance(style2, uLocale)).toPattern();
        return "Mismatch in in ordering for " + uLocale + ": " + DATE_STYLE_NAMES[style1] + ": " + order1 + ", <" + pattern1 
                + ">; " 
                + DATE_STYLE_NAMES[style2] + ": " + order2 + ", <" + pattern2 + ">; " ;
    }

    /**
     * Main date fields -- Poor-man's enum -- change to real enum when we get JDK 1.5
     */
    public static class DateFieldType {
        private String name;
        private DateFieldType(String string) {
            name = string;
        }
        
        public static DateFieldType 
        YEAR = new DateFieldType("YEAR"), 
        MONTH = new DateFieldType("MONTH"), 
        DAY = new DateFieldType("DAY");
        
        public String toString() {
            return name;
        }
    }
    
    /**
     * Simple struct for output from getOrdering
     */
    static class DateOrder {
        int monthLength;
        DateFieldType[] fields = new DateFieldType[3];
        
        public boolean isCompatible(DateOrder other) {
            return monthLength == other.monthLength;
        }
        /**
         * @param order2
         * @return
         */
        public boolean hasSameOrderAs(DateOrder other) {
            // TODO Auto-generated method stub
            return fields[0] == other.fields[0] && fields[1] == other.fields[1] && fields[2] == other.fields[2];
        }
        public String toString() {
            return "{" + monthLength + ", " + fields[0]  + ", " + fields[1]  + ", " + fields[2] + "}";
        }
        public boolean equals(Object that) {
            DateOrder other = (DateOrder) that;
            return monthLength == other.monthLength && fields[0] == other.fields[0] && fields[1] == other.fields[1] && fields[2] == other.fields[2];            
        }
    }
    
    DateTimePatternGenerator.FormatParser formatParser = new DateTimePatternGenerator.FormatParser ();
    DateTimePatternGenerator generator = DateTimePatternGenerator.getEmptyInstance();
    
    private Calendar sampleCalendar = new GregorianCalendar(1999, Calendar.OCTOBER, 13, 23, 58, 59);
    private Date sampleDate = sampleCalendar.getTime();
    private TimeZone gmt = TimeZone.getTimeZone("Etc/GMT");
    
    /**
     * Replace the zone string with a different type, eg v's for z's, etc. <p>Called with a pattern, such as one gotten from 
     * <pre>
     * String pattern = ((SimpleDateFormat) DateFormat.getTimeInstance(style, locale)).toPattern();
     * </pre>
     * @param pattern original pattern to change, such as "HH:mm zzzz"
     * @param newZone Must be: z, zzzz, Z, ZZZZ, v, vvvv, V, or VVVV
     * @return
     */
    public String replaceZoneString(String pattern, String newZone) {
        final List itemList = formatParser.set(pattern).getItems();
        boolean changed = false;
        for (int i = 0; i < itemList.size(); ++i) {
            Object item = itemList.get(i);
            if (item instanceof VariableField) {
                VariableField variableField = (VariableField) item;
                if (variableField.getType() == DateTimePatternGenerator.ZONE) {
                    if (!variableField.toString().equals(newZone)) {
                        changed = true;
                        itemList.set(i, new VariableField(newZone, true));
                    }
                }
            }
        }
        return changed ? formatParser.toString() : pattern;
    }
    
    public boolean containsZone(String pattern) {
        for (Iterator it = formatParser.set(pattern).getItems().iterator(); it.hasNext();) {
            Object item = it.next();
            if (item instanceof VariableField) {
                VariableField variableField = (VariableField) item;
                if (variableField.getType() == DateTimePatternGenerator.ZONE) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Get the ordering from a particular date format. Best is to use
     * DateFormat.FULL to get the format with String form month (like "January")
     * and DateFormat.SHORT for the numeric format order. They may be different.
     * (Theoretically all 4 formats could be different but that never happens in
     * practice.)
     *
     * @param style
     *          DateFormat.FULL..DateFormat.SHORT
     * @param locale
     *          desired locale.
     * @return
     * @return list of ordered items DateFieldType (I
     *         didn't know what form you really wanted so this is just a
     *         stand-in.)
     */
  private DateOrder getOrdering(int style, ULocale locale) {
      // and the date pattern
      String pattern = ((SimpleDateFormat) DateFormat.getDateInstance(style, locale)).toPattern();
      int count = 0;
      DateOrder result = new DateOrder();
     
      for (Iterator it = formatParser.set(pattern).getItems().iterator(); it.hasNext();) {
          Object item = it.next();
        if (!(item instanceof String)) {
          // the first character of the variable field determines the type,
          // according to CLDR.
          String variableField = item.toString();
          switch (variableField.charAt(0)) {
            case 'y': case 'Y': case 'u':
              result.fields[count++] = DateFieldType.YEAR;
              break;
            case 'M': case 'L':
                result.monthLength = variableField.length();
                if (result.monthLength < 2) {
                    result.monthLength = 2;
                }
                result.fields[count++] = DateFieldType.MONTH;
              break;
            case 'd': case 'D': case 'F': case 'g':
                result.fields[count++] = DateFieldType.DAY;
              break;
          }
        }
      }
      return result;
    }
      /* Tests the method
       *        public static DateTimePatternGenerator getInstance()
       */
      public void TestGetInstance(){
          try{
              DateTimePatternGenerator.getInstance();
          } catch(Exception e){
              errln("DateTimePatternGenerator.getInstance() was not suppose to " +
                      "return an exception.");
          }
      }
      
      /* Tests the method
       *        public String getSkeleton(String pattern)
       */
      public void TestGetSkeleton(){
          DateTimePatternGenerator dtpg = DateTimePatternGenerator.getInstance();
          String[] cases = {"MMDD","MMMDD","MMM-DD","DD/MMM","ddM"};
          String[] results = {"MMDD","MMMDD","MMMDD","MMMDD","Mdd"};
          for(int i=0; i<cases.length; i++){
              if(!dtpg.getSkeleton(cases[i]).equals(results[i])){
                  errln("DateTimePatternGenerator.getSkeleton(String) did " +
                          "return the expected result when passing " + cases[i] +
                          " and expected " + results[i] + " but got " +
                          dtpg.getSkeleton(cases[i]));
              }
          }
      }
      
      /* Tests the method
       *        public String getBaseSkeleton(String pattern)
       */
      public void TestGetBaseSkeleton(){
          DateTimePatternGenerator dtpg = DateTimePatternGenerator.getInstance();
          String[] cases = {"MMDD","MMMDD","MMM-DD","DD/MMM","ddM"};
          String[] results = {"MD","MMMD","MMMD","MMMD","Md"};
          for(int i=0; i<cases.length; i++){
              if(!dtpg.getBaseSkeleton(cases[i]).equals(results[i])){
                  errln("DateTimePatternGenerator.getSkeleton(String) did " +
                          "return the expected result when passing " + cases[i] +
                          " and expected " + results[i] + " but got " +
                          dtpg.getBaseSkeleton(cases[i]));
              }
          }
      }
      
      /* Tests the method
       *        public Map<String, String> getSkeletons(Map<String, String> result)
       */
      public void TestGetSkeletons(){
          DateTimePatternGenerator dtpg = DateTimePatternGenerator.getInstance();
          // Tests when "if (result == null)" is true
          try{
              dtpg.getSkeletons(null);
          } catch(Exception e){
              errln("DateTimePatternGenerator.getSkeletons(Map) was suppose to " +
                      "return a new LinkedHashMap for a null parameter.");
          }
          
          // Tests when "if (result == null)" is false
          Map<String,String> mm = new LinkedHashMap<String, String>();
          try{
              dtpg.getSkeletons(mm);
          } catch(Exception e){
              errln("DateTimePatternGenerator.getSkeletons(Map) was suppose to " +
                      "return a new LinkedHashMap for a LinkedHashMap parameter.");
          }
      }
      
      /* Tests the method
       *        public Set<String> getBaseSkeletons(Set<String> result)
       */
      public void TestGetBaseSkeletons(){
          DateTimePatternGenerator dtpg = DateTimePatternGenerator.getInstance();
          // Tests when "if (result == null)" is true
          try{
              dtpg.getBaseSkeletons(null);
          } catch(Exception e){
              errln("DateTimePatternGenerator.getBaseSkeletons(Map) was suppose to " +
                      "return a new LinkedHashMap for a null parameter.");
          }
          
          // Tests when "if (result == null)" is false
          Set<String> mm = new HashSet<String>();
          try{
              dtpg.getBaseSkeletons(mm);
          } catch(Exception e){
              errln("DateTimePatternGenerator.getBaseSkeletons(Map) was suppose to " +
                      "return a new LinkedHashMap for a HashSet parameter.");
          }
      }
      
      /* Tests the method
       *        public String getDecimal()
       */
      public void TestGetDecimal(){
          DateTimePatternGenerator dtpg = DateTimePatternGenerator.getInstance();
          if(!dtpg.getDecimal().equals(".")){
              errln("DateTimePatternGenerator.getDecimal() was to return '.' " +
                      "when the object gets a new instance.");
          }
          
          String[] cases = {",","-","","*","&","a","0"};
          for(int i=0; i<cases.length; i++){
              dtpg.setDecimal(cases[i]);
              if(!dtpg.getDecimal().equals(cases[i])){
                  errln("DateTimePatternGenerator.getDecimal() was to return " + cases[i] +
                          "when setting decimal with " + cases[i]);
              }
          }
      }
      
      /* Tests the method
       *        public Collection<String> getRedundants(Collection<String> output)
       */
      public void TestGetRedundants(){
          DateTimePatternGenerator dtpg = DateTimePatternGenerator.getInstance();
          
          // Tests when "if (output == null)" is true
          try{
              dtpg.getRedundants(null);
          } catch(Exception e){
              errln("DateTimeGenerator.getRedundants was not suppose to return " +
                      "an exception when passing a null parameter.");
          }
          
          // Tests when "if (output == null)" is false
          try{
              Collection<String> out = new LinkedHashSet<String>();
              dtpg.getRedundants(out);
          } catch(Exception e){
              errln("DateTimeGenerator.getRedundants was not suppose to return " +
                  "an exception when passing a new LinkedHashSet<String>() parameter.");
          }
          
      }
      
      /* Tests the method
       *        public String getAppendItemFormat(int field)
       */
      public void TestGetAppendItemFormat(){
          DateTimePatternGenerator dtpg = DateTimePatternGenerator.getInstance();
          String[] cases = {"d","u","m","m","y"};
          for(int i=0; i<cases.length; i++){
              dtpg.setAppendItemFormat(i, cases[i]);
              if(!dtpg.getAppendItemFormat(i).equals(cases[i])){
                  errln("DateTimePatternGeneratorgetAppendItemFormat(int field) " +
                          "did not return as expected. Value set at " + i + " was " +
                          cases[i] + " but got back " + dtpg.getAppendItemFormat(i));
              }
          }
      }
      
      /* Tests the method
       *    public String getAppendItemName(int field)
       */
      public void TestGetAppendItemName(){
          DateTimePatternGenerator dtpg = DateTimePatternGenerator.getInstance();
          String[] cases = {"d","u","m","m","y"};
          for(int i=0; i<cases.length; i++){
              dtpg.setAppendItemName(i, cases[i]);
              if(!dtpg.getAppendItemName(i).equals(cases[i])){
                  errln("DateTimePatternGenerator.getAppendItemFormat(int field) " +
                          "did not return as expected. Value set at " + i + " was " +
                          cases[i] + " but got back " + dtpg.getAppendItemName(i));
              }
          }
      }
      
      /* Tests the method
       *    public static boolean isSingleField(String skeleton)
       */
      @SuppressWarnings("static-access")
    public void TestIsSingleField(){
          DateTimePatternGenerator dtpg = DateTimePatternGenerator.getInstance();
          String[] cases = {" ", "m","mm","md","mmd","mmdd"};
          boolean[] results = {true,true,true,false,false,false};
          for(int i=0; i<cases.length; i++){
              if(dtpg.isSingleField(cases[i]) != results[i]){
                  errln("DateTimePatternGenerator.isSingleField(String skeleton) " +
                          "did not return as expected. Value passed was " + cases[i] +
                          " but got back " + dtpg.isSingleField(cases[i]));
              }
          }
      }
      
      /* Tests the method
       *    public Object freeze()
       *    public Object cloneAsThawed()
       */
      public void TestFreezeAndCloneAsThawed(){
          DateTimePatternGenerator dtpg = DateTimePatternGenerator.getInstance();
          
          if(dtpg.isFrozen() != false){
              errln("DateTimePatternGenerator.isFrozen() is suppose to return false " +
                      "for a DateTimePatternGenerator object that was just " +
                      "created.");
          }
          
          dtpg.freeze();
          if(dtpg.isFrozen() != true){
              errln("DateTimePatternGenerator.isFrozen() is suppose to return true " +
                      "for a DateTimePatternGenerator object that was just " +
                      "created and freeze.");
          }
          
          DateTimePatternGenerator dtpg2 = (DateTimePatternGenerator) dtpg.cloneAsThawed();
          if(dtpg.isFrozen() != false){
              errln("DateTimePatternGenerator.isFrozen() is suppose to return false " +
                      "for a DateTimePatternGenerator object that was just " +
                      "clone as thawed.");
          }
          if(dtpg2.isFrozen() != false){
              errln("DateTimePatternGenerator.isFrozen() is suppose to return false " +
                      "for a second DateTimePatternGenerator object that was just " +
                      "clone as thawed.");
          }
      }
      
      /* Tests the method
       *    public Object clone()
       */
      public void TestClone(){
          DateTimePatternGenerator dtpg = DateTimePatternGenerator.getInstance();
          DateTimePatternGenerator dtpg2 = (DateTimePatternGenerator) dtpg.clone();
          dtpg = (DateTimePatternGenerator) dtpg2.clone();
      }
      
      /* Tests the constructor
       *    public VariableField(String string)
       */
      @SuppressWarnings("unused")
      public void TestVariableField_String(){
          String[] cases = {"d","mm","aa"};
          String[] invalid = {null,"","dummy"};
          for(int i=0; i<cases.length; i++){
              try{
                  VariableField vf = new VariableField(cases[i]);
              } catch(Exception e){
                  errln("VariableField constructor was not suppose to return " +
                  "an exception when created when passing " + cases[i]);
              }
          }
          for(int i=0; i<invalid.length; i++){
              try{
                VariableField vf = new VariableField(invalid[i]);
                  errln("VariableField constructor was suppose to return " +
                          "an exception when created when passing " + invalid[i]);
              } catch(Exception e){}
          }
      }
      
      /* Tests the method
       *    public FormatParser set(String string, boolean strict)
       */
      public void TestSet(){
          FormatParser fp = new FormatParser();
          //Tests when "if (string.length() == 0)" is true
          try{
              fp.set("",true);
          }catch(Exception e){
              errln("FormatParser.set(String,boolean) was not suppose to " +
                      "return an exception.");
          }
      }
      
      /* Tests the method
       *    public String toString()
       */
      public void TestToString(){
          FormatParser fp = new FormatParser();
          if(!fp.toString().equals("")){
              errln("FormatParser.toString() was suppose to return an " +
                      "empty string for a new FormatParser object.");
          }
          
          String[] cases = {"m","d","y","mm","mmm","mm dd","mm':'dd","mm-dd-yyyy"};
          String[] results = {"m","d","y","mm","mmm","mm dd","mm:dd","mm-dd-yyyy"};
          for(int i=0; i<cases.length; i++){
              fp.set(cases[i]);
              if(!fp.toString().equals(results[i])){
                  errln("FormatParser.toString() was suppose to return " + results[i] + 
                  " after setting the object. Got: " + fp.toString());
              }
          }
      }
      
      /* Tests the method
       *    public boolean hasDateAndTimeFields()
       */
      public void TestHasDateAndTimeFields(){
          FormatParser fp = new FormatParser();
          if(fp.hasDateAndTimeFields() != false){
              errln("FormatParser.hasDateAndTimeFields() was suppose to return " +
                      "false when a new object is created.");
          }
          
          String[] cases = {"MMDDYY", "HHMMSS", "", "MM/DD/YYYY HH:MM:SS",
                  "MMDDYY HHMMSS", "HHMMSS MMDDYYYY", "HMS MDY"};
          boolean[] results = {false,true,false,true,true,true,true};
          for(int i=0; i<cases.length; i++){
              fp.set(cases[i]);
              if(fp.hasDateAndTimeFields() != results[i]){
                  errln("FormatParser.hasDateAndTimeFields() was suppose to " +
                          "return " + results[i] + " but returned " +
                          fp.hasDateAndTimeFields() + " for parameter " + 
                          cases[i] + " that is set to FormatParser.");
              }
          }
      }
      
      /* Tests the method
       *    private void checkFrozen()
       * from public void setDateTimeFormat(String dateTimeFormat)
       */
      public void TestCheckFrozen(){
          // Tests when "if (isFrozen())" is true
          DateTimePatternGenerator dt = DateTimePatternGenerator.getInstance();
          try{
              dt.freeze();
              dt.setDateTimeFormat("MMDDYYYY");
              errln("DateTimePatternGenerator.checkFrozen() was suppose to " +
                      "return an exception when trying to setDateTimeFormat " +
                      "for a frozen object.");
          } catch(Exception e){}
          dt = (DateTimePatternGenerator) dt.cloneAsThawed();
      }
      
      /* Tests the method
       *    public String getFields(String pattern)
       */
      public void TestGetFields(){
          DateTimePatternGenerator dt = DateTimePatternGenerator.getInstance();
          String[] cases = {"MMDDYY", "HHMMSS", "", "MM/DD/YYYY HH:MM:SS",
                  "MMDDYY HHMMSS", "HHMMSS MMDDYYYY", "HMS MDY"};
          String[] results = {"{Month:N}{Day_Of_Year:N}{Year:N}",
                  "{Hour:N}{Month:N}{Fractional_Second:N}","",
                  "{Month:N}/{Day_Of_Year:N}/{Year:N} {Hour:N}:{Month:N}:{Fractional_Second:N}",
                  "{Month:N}{Day_Of_Year:N}{Year:N} {Hour:N}{Month:N}{Fractional_Second:N}",
                  "{Hour:N}{Month:N}{Fractional_Second:N} {Month:N}{Day_Of_Year:N}{Year:N}",
                  "{Hour:N}{Month:N}{Fractional_Second:N} {Month:N}{Day_Of_Year:N}{Year:N}"};
          for(int i=0; i<cases.length; i++){
              try{
                  if(!dt.getFields(cases[i]).equals(results[i]));
              } catch(Exception e){
                  errln("DateTimePatternGenerator.getFields(String) did not " +
                          "not return an expected result when passing " + cases[i] +
                          ". Got " + dt.getFields(cases[i]) + " but expected " +
                          results[i]);
              }
          }
      }

      /*
       * Test case for DateFormatPatternGenerator threading problem #7169
       */
      public void TestT7169() {
          Thread[] workers = new Thread[10];
          for (int i = 0 ; i < workers.length; i++) {
              workers[i] = new Thread(new Runnable() {
                  public void run() {
                      try {
                          for (int i = 0; i < 50; i++) {
                              DateTimePatternGenerator patternGenerator =
                                  DateTimePatternGenerator.getFrozenInstance(ULocale.US);
                              patternGenerator.getBestPattern("MMMMd");
                          }
                      } catch (Exception e) {
                          errln("FAIL: Caught an exception (frozen)" + e);
                      }
                      try {
                          for (int i = 0; i < 50; i++) {
                              DateTimePatternGenerator patternGenerator =
                                  DateTimePatternGenerator.getInstance(ULocale.US);
                              patternGenerator.getBestPattern("MMMMd");
                          }
                      } catch (Exception e) {
                          errln("FAIL: Caught an exception " + e);
                      }
                  }
              });
          }
          for (Thread wk : workers) {
              wk.start();
          }
          for (Thread wk : workers) {
              try {
                  wk.join();
              } catch (InterruptedException ie) {
                  
              }
          }
      }
}
