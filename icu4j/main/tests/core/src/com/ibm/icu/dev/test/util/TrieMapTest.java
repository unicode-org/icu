/*
 *******************************************************************************
 * Copyright (C) 2011, Google, International Business Machines Corporation and
 * others. All Rights Reserved.
 *******************************************************************************
 */
package com.ibm.icu.dev.test.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.test.util.TrieMap.Style;
import com.ibm.icu.impl.Row;
import com.ibm.icu.impl.StringTrieBuilder.Option;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.impl.Row.R3;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.lang.UScript;
import com.ibm.icu.text.DecimalFormat;
import com.ibm.icu.text.NumberFormat;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ULocale;


public class TrieMapTest extends TestFmwk {
    static final boolean SHORT = false;
    static final boolean HACK_TO_MAKE_TESTS_PASS = false;
    static final int MASK = 0x3;

    Map<String, Integer> unicodeTestMap = new HashMap<String, Integer>();
    boolean useSmallList = true;
    int REPEAT = 10;

    @Override
    protected void init() throws Exception {
        super.init();
        if (unicodeTestMap.size() == 0) {
            if (getInclusion() < 5) {
                logln("\tSmall version:\t to get more accurate figures and test for reasonable times, use -e5 or more");
            } else {
                REPEAT *= (getInclusion() - 4);
                useSmallList = false;
            }

            int i = 0;
            UnicodeSet testSet = new UnicodeSet("[[:^C:]-[:sc=han:]]");
            for (String s : testSet) {
                int codePoint = s.codePointAt(0);
                String extendedName = UCharacter.getExtendedName(codePoint);
                if (!unicodeTestMap.containsKey(extendedName)) {
                    unicodeTestMap.put(extendedName, i++);
                }
                if (i > 500 && useSmallList) break;
            }
            ULocale[] locales = useSmallList ? new ULocale[] {new ULocale("zh"), new ULocale("el")} : ULocale.getAvailableLocales();
            for (ULocale locale : locales) {
                if (locale.getDisplayCountry().length() != 0) {
                    continue;
                }
                String localeName;
                for (String languageCode : ULocale.getISOLanguages()) {
                    localeName = ULocale.getDisplayName(languageCode, locale);
                    if (!localeName.equals(languageCode)) {
                        if (!unicodeTestMap.containsKey(localeName)) {
                            unicodeTestMap.put(localeName, MASK & i++);
                        }
                        if (SHORT) break;
                    }
                }
                for (String countryCode : ULocale.getISOCountries()) {
                    localeName = ULocale.getDisplayCountry("und-" + countryCode, locale);
                    if (!localeName.equals(countryCode)) {
                        if (!unicodeTestMap.containsKey(localeName)) {
                            unicodeTestMap.put(localeName, MASK & i++);
                        }
                        if (SHORT) break;
                    }
                }
            }
            int charCount = 0; 
            for (String key : unicodeTestMap.keySet()) {
                charCount += key.length();
            }
            logln("\tTest Data Elements:\t\t\t" + nf.format(unicodeTestMap.size()));
            logln("\tTotal chars:\t\t\t" + nf.format(charCount));
        }
    }

    public static void main(String[] args) {
        new TrieMapTest().run(args);
    }

    public void TestByteConversion() {
        byte bytes[] = new byte[200];
        for (Entry<String, Integer> entry : unicodeTestMap.entrySet()) {
            String source = entry.getKey();
            int limit = TrieMap.ByteConverter.getBytes(source, bytes, 0);
            //logln(source + "\t=> " + Utility.hex(source, " ") + "\t=> " + Utility.hex(bytes, 0, limit, " "));
            String recovered = TrieMap.ByteConverter.getChars(bytes, 0, limit);
            if (!source.equals(recovered)) {
                assertEquals("Char/Byte Conversion", source, recovered);
            }
        }
    }

    public void TestGet() {
        checkGet(unicodeTestMap, TrieMap.Style.BYTES);
        checkGet(unicodeTestMap, TrieMap.Style.CHARS);
    }

    private void checkGet(Map<String, Integer> testmap, TrieMap.Style style) {
        if (testmap.size() == 0) {
            return;
        }
        TrieMap<Integer> trieMap = TrieMap.Builder.with(style, testmap).build(Option.FAST);
        //logln(trieMap.toString());
        for (Entry<String, Integer> entry : testmap.entrySet()) {
            Integer value = entry.getValue();
            String key = entry.getKey();
            Integer foundValue = trieMap.get(key);
            if (!value.equals(foundValue)) {
                // TODO fix this
                if (!HACK_TO_MAKE_TESTS_PASS || 39497 != value) {
                    assertEquals(style + "\tGet of '" + key + "' = {" + Utility.hex(key) + "}", value, foundValue);
                }
            }
        }        
    }

    public void TestTimeIteration() {
        long comparisonTime = timeIteration(unicodeTestMap, 0, null, 0);
        timeIteration(unicodeTestMap, comparisonTime, Style.BYTES, 5);
        timeIteration(unicodeTestMap, comparisonTime, Style.CHARS, 3);
    }

    public long timeIteration(Map<String, Integer> testMap, long comparisonTime, Style style, double ratioToMap) {
        TrieMap<Integer> trieMap = TrieMap.BytesBuilder.with(style, testMap).build(Option.SMALL);
        TreeMap<String,Integer> expected = new TreeMap<String, Integer>(testMap);

        Timer t = new Timer();

        System.gc();
        t.start();
        if (style == null) {
            for (int tt = 0; tt < REPEAT; ++tt) {
                for (Entry<String, Integer> entry : expected.entrySet()) {
                    String key = entry.getKey();
                    Integer value = entry.getValue();
                }
            }
            long mapTime = t.getDuration();
            logln("\titeration time\tTREEMAP\tn/a\t" + t.toString(REPEAT*testMap.size()));
            return mapTime;
        } else {
            for (int tt = 0; tt < REPEAT; ++tt) {
                for (Entry<CharSequence, Integer> entry : trieMap) {
                    CharSequence key = entry.getKey();
                    Integer value = entry.getValue();
                }
            }
            long trieTime = t.getDuration();
            logln("\titeration time\t" + style + "\tn/a\t" + t.toString(REPEAT*testMap.size(), comparisonTime));
            if (!useSmallList && trieTime > ratioToMap * comparisonTime) {
                errln(style + "\tTime iteration takes too long. Expected:\t< " + ratioToMap * comparisonTime + ", Actual:\t" + trieTime);
            }
            return trieTime;
        }
    }

    public void TestContents() {
        checkContents(unicodeTestMap, Style.BYTES);
        checkContents(unicodeTestMap, Style.CHARS);
    }

    public void checkContents(Map<String, Integer> testMap, Style style) {
        if (testMap.size() == 0) {
            return;
        }
        TrieMap<Integer> trieMap = TrieMap.BytesBuilder.with(style, testMap).build(Option.FAST);
        TreeMap<String,Integer> expected = new TreeMap<String, Integer>(testMap);
        Iterator<Entry<CharSequence, Integer>> trieIterator = trieMap.iterator();
        Iterator<Entry<String, Integer>> mapIterator = expected.entrySet().iterator();
        while (true) {
            boolean trieOk = trieIterator.hasNext();
            boolean mapOk = mapIterator.hasNext();
            if (mapOk!=trieOk) {
                assertEquals("Iterators end at same point", mapOk, trieOk);
            }

            if (!mapOk) break;
            Entry<CharSequence, Integer> trieEntry = trieIterator.next();
            Entry<String, Integer> mapEntry = mapIterator.next();
            String mapKey = mapEntry.getKey();
            CharSequence trieKey = trieEntry.getKey();
            if (!mapKey.contentEquals(trieKey)) {
                assertEquals(style + "\tKeys match", mapKey, trieKey.toString());
            }
            Integer mapValue = mapEntry.getValue();
            Integer trieValue = trieEntry.getValue();
            if (!mapValue.equals(trieValue)) {
                assertEquals(style + "\tValues match", mapValue, trieValue);
            }
        }
    }

    public void TestSearch() {
        checkSearch(Style.BYTES);
        checkSearch(Style.CHARS);
    }

    public void checkSearch(Style style) {

        TrieMap<String> trieMap = TrieMap.BytesBuilder.with(style, "abc", "first")
        .add("cdab", "fifth")
        .add("abcde", "second")
        .add("abdfg", "third").build(Option.FAST);

        String string = "xabcdab abcde abdfg";
        @SuppressWarnings("unchecked")
        Row.R3<Integer, Integer, String>[] expected = new Row.R3[] {
            Row.of(1,4,"first"),
            Row.of(3,7,"fifth"),
            Row.of(8,11,"first"),
            Row.of(8,13,"second"),
            Row.of(14,19,"third"),
        };
        List<R3<Integer, Integer, String>> expectedList = Arrays.asList(expected);
        List<R3<Integer, Integer, String>> actualList = new ArrayList<R3<Integer, Integer, String>>();

        TrieMap.Matcher<String> matcher = trieMap.getMatcher();
        matcher.set(string, 0);
        do {
            boolean hasMore;
            do {
                hasMore = matcher.next();
                String value = matcher.getValue();
                if (value != null) {
                    int start = matcher.getStart();
                    int end = matcher.getEnd();
                    actualList.add(Row.of(start,end,value));
                }
            } while (hasMore);
        } while (matcher.nextStart());
        assertEquals(style + "\tTrieMap matcher", expectedList, actualList);
        //        logln(bytes + "\tValue <" + value + "> at " 
        //                + start + ".." + end + ", "
        //                + string.substring(0, start) + "|"
        //                + string.substring(start, end) + "|"
        //                + string.substring(end)
        //        );
    }

    public void TestTimeBuilding() {
        long comparisonTime = timeBuilding(unicodeTestMap, 0, null, Option.SMALL, 0);
        timeBuilding(unicodeTestMap, comparisonTime, Style.BYTES, Option.SMALL, 20);
        timeBuilding(unicodeTestMap, comparisonTime, Style.BYTES, Option.FAST, 20);
        timeBuilding(unicodeTestMap, comparisonTime, Style.CHARS, Option.SMALL, 20);
        timeBuilding(unicodeTestMap, comparisonTime, Style.CHARS, Option.FAST, 20);
    }

    public long timeBuilding(Map<String, Integer> testmap, long comparisonTime, Style style, Option option, double ratioToMap) {
        TrieMap<Integer> trieMap = null;
        TreeMap<String, Integer> map = null;
        Timer t = new Timer();

        System.gc();
        t.start();
        if (style == null) {
            for (int tt = 0; tt < REPEAT; ++tt) {
                map = new TreeMap<String, Integer>(testmap);
            }
            long mapTime = t.getDuration();
            logln("\tbuild time\tTREEMAP\tn/a\t" + t.toString(REPEAT*testmap.size()));
            return mapTime;
        } else {
            for (int tt = 0; tt < REPEAT; ++tt) {
                trieMap = TrieMap.BytesBuilder.with(style, testmap).build(option);
            }
            long trieTime = t.getDuration();
            logln("\tbuild time\t" + style + "\t" + option + "\t" + t.toString(REPEAT*testmap.size(), comparisonTime));
            if (!useSmallList && trieTime > ratioToMap * comparisonTime) {
                errln(style + "\t" + option + "\tTrie build takes too long. Expected:\t< " + nf.format(ratioToMap * comparisonTime) + ", Actual:\t" + nf.format(trieTime));
            }
            return trieTime;
        }
    }

    public void TestSize() {
        int size = checkSize(0, null, Option.SMALL, 0);
        checkSize(size, Style.BYTES, Option.SMALL, 0.20);
        checkSize(size, Style.BYTES, Option.FAST, 0.20);
        checkSize(size, Style.CHARS, Option.SMALL, 0.30);
        checkSize(size, Style.CHARS, Option.FAST, 0.30);
    }

    /**
     * @param option TODO
     * @param ratioToMap TODO
     * @param bytes
     */
    private int checkSize(int comparisonSize, Style style, Option option, double ratioToMap) {
        if (style == null) {
            int mapKeyByteSize = 0;
            TreeMap<String, Integer> map = new TreeMap<String, Integer>(unicodeTestMap);
            for (Entry<String, Integer> entry : map.entrySet()) {
                mapKeyByteSize += 8 * (int) ((((entry.getKey().length()) * 2) + 45) / 8);
            }
            logln("\tkey byte size\tTREEMAP\tn/a\t" + nf.format(mapKeyByteSize));
            return mapKeyByteSize;
        } else {
            TrieMap<Integer> trieMap = TrieMap.BytesBuilder.with(style, unicodeTestMap).build(option);

            int trieKeyByteSize = trieMap.keyByteSize();
            logln("\tkey byte size\t" + style + "\t" + option + "\t" + nf.format(trieKeyByteSize) + "\t\t" + pf.format(trieKeyByteSize/(double)comparisonSize - 1D) + "");


            if (!useSmallList && trieKeyByteSize > ratioToMap * comparisonSize) {
                errln(style + "\t" + option + "\ttrieKeyByteSize too large. Expected:\t< " + nf.format(ratioToMap * comparisonSize) + ", Actual:\t" + nf.format(trieKeyByteSize));
            }
            return trieKeyByteSize;
        }
    }

    private static DecimalFormat nf = (DecimalFormat) NumberFormat.getNumberInstance(ULocale.ENGLISH);
    private static DecimalFormat pf = (DecimalFormat) NumberFormat.getPercentInstance(ULocale.ENGLISH);

    public void TestTimeGet() {
        HashSet<String> keySet = new HashSet<String>(unicodeTestMap.keySet());
        ULocale[] locales = ULocale.getAvailableLocales();
        int i = 0;
        for (ULocale locale : locales) {
            if (locale.getDisplayCountry().length() != 0) {
                continue;
            }
            String localeName;
            for (int scriptCodeInt = 0; scriptCodeInt < UScript.CODE_LIMIT; ++scriptCodeInt) {
                String scriptCode = UScript.getShortName(scriptCodeInt);
                localeName = ULocale.getDisplayScript("und-" + scriptCode, locale);
                if (!localeName.equals(scriptCode)) {
                    if (!keySet.contains(localeName)) {
                        keySet.add(localeName);
                        ++i;
                    }
                    if (SHORT) break;
                }
            }
        }
        logln("\tExtra Key Elements\t" + i);

        ArrayList<String> keys = new ArrayList<String>(keySet);

        long comparisonTime = timeGet(keys, unicodeTestMap, 0, null, 0);
        timeGet(keys, unicodeTestMap, comparisonTime, null, 0);
        timeGet(keys, unicodeTestMap, comparisonTime, Style.BYTES, 3);
        timeGet(keys, unicodeTestMap, comparisonTime, Style.CHARS, 3);
    }

    public long timeGet(ArrayList<String> keys, Map<String, Integer> testmap, long comparisonTime, Style style, int ratioToMap) {
        Timer t = new Timer();

        TrieMap<Integer> trieMap = TrieMap.Builder.with(style, testmap).build(Option.SMALL);
        int repeat = REPEAT;
        
        if (style == null) {
            Map<String, Integer> map = comparisonTime == 0 ? new TreeMap<String, Integer>(testmap) : new HashMap<String, Integer>(testmap);

            System.gc();
            t.start();
            for (int tt = 0; tt < repeat; ++tt) {
                for (String key : keys) {
                    Integer foundValue = map.get(key);
                }
            }
            long mapTime = t.getDuration();
            if (comparisonTime == 0) {
                logln("\tget() time\tTREEMAP\tn/a\t" + t.toString(REPEAT*testmap.size()));
            } else {
                logln("\tget() time\tHASHMAP\tn/a\t" + t.toString(REPEAT*testmap.size(), comparisonTime));
            }
            return mapTime;
        } else {
            System.gc();
            t.start();
            for (int tt = 0; tt < repeat; ++tt) {
                for (String key : keys) {
                    Integer foundValue = trieMap.get(key);
                }
            }
            long trieTime = t.getDuration();
            logln("\tget() time\t" + style + "\tn/a\t" + t.toString(REPEAT*testmap.size(), comparisonTime));
            if (!useSmallList && trieTime > ratioToMap * comparisonTime) {
                errln(style + "\tTime iteration takes too long. Expected:\t< " + ratioToMap * comparisonTime + ", Actual:\t" + trieTime);
            }
            return trieTime;
        }
    }
}
