/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 2002-2012, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/

/**
 * UCAConformanceTest performs conformance tests defined in the data
 * files. ICU ships with stub data files, as the whole test are too 
 * long. To do the whole test, download the test files.
 */

package com.ibm.icu.dev.test.collator;

import java.io.BufferedReader;
import java.util.Locale;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.dev.test.TestUtil;
import com.ibm.icu.lang.UCharacter;
import com.ibm.icu.text.CollationKey;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.UTF16;
import com.ibm.icu.util.VersionInfo;

public class UCAConformanceTest extends TestFmwk {

    /**
     * @param args
     */
    public static void main(String[] args) {
        new UCAConformanceTest().run(args);
    }

    public UCAConformanceTest() {
    }
    protected void init()throws Exception{
        UCA = (RuleBasedCollator)Collator.getInstance(
                new Locale("root", "", ""));

        comparer = new UTF16.StringComparator(true, false, UTF16.StringComparator.FOLD_CASE_DEFAULT);
    }
    private RuleBasedCollator UCA;
    private RuleBasedCollator rbUCA;
    private UTF16.StringComparator comparer;
    private boolean isAtLeastUCA62 =
        UCharacter.getUnicodeVersion().compareTo(VersionInfo.UNICODE_6_2) >= 0;

    public void TestTableNonIgnorable() {
        setCollNonIgnorable(UCA);
        openTestFile("NON_IGNORABLE");
        conformanceTest(UCA);
    }

    public void TestTableShifted() {
        setCollShifted(UCA);
        openTestFile("SHIFTED");
        conformanceTest(UCA);
    }

    public void TestRulesNonIgnorable() {
        initRbUCA();
        if(rbUCA == null) { return; }

        setCollNonIgnorable(rbUCA);
        openTestFile("NON_IGNORABLE");
        conformanceTest(rbUCA);
    }

    public void TestRulesShifted() {
        logln("This test is currently disabled, as it is impossible to "+
        "wholly represent fractional UCA using tailoring rules.");
        return;
        /*
        initRbUCA();
        if(rbUCA == null) { return; }

        setCollShifted(rbUCA);
        openTestFile("SHIFTED");
        testConformance(rbUCA);
        */
    }
    BufferedReader in;
    private void openTestFile(String type)
    {
        String collationTest = "CollationTest_";
        String ext = ".txt";
        try {
            if(in != null) {
                in.close();
            }
        } catch (Exception e) {
            errln("Could not close the opened file!");
            return;
        }
        try {
            in = TestUtil.getDataReader(collationTest+type+ext);
        } catch (Exception e) {
            try {
                in = TestUtil.getDataReader(collationTest+type+"_SHORT"+ext);
            } catch (Exception e1) {
                try {
                    in = TestUtil.getDataReader(collationTest+type+"_STUB"+ext);
                    logln( "INFO: Working with the stub file.\n"+
                            "If you need the full conformance test, please\n"+
                            "download the appropriate data files from:\n"+
                    "http://source.icu-project.org/repos/icu/tools/trunk/unicodetools/com/ibm/text/data/");
                } catch (Exception e11) {
                    errln("ERROR: Could not find any of the test files");
                }
            }
        }
    }          

    private void setCollNonIgnorable(RuleBasedCollator coll) 
    {
        if(coll != null) {
            coll.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
            coll.setLowerCaseFirst(false);
            coll.setCaseLevel(false);
            coll.setStrength(isAtLeastUCA62 ? Collator.IDENTICAL : Collator.TERTIARY);
            coll.setAlternateHandlingShifted(false);
        }
    }

    private void setCollShifted(RuleBasedCollator coll) 
    {
        if(coll != null) {
            coll.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
            coll.setLowerCaseFirst(false);
            coll.setCaseLevel(false);
            coll.setStrength(isAtLeastUCA62 ? Collator.IDENTICAL : Collator.QUATERNARY);
            coll.setAlternateHandlingShifted(true);
        }
    }



    private void initRbUCA() 
    {
        if(rbUCA == null) {
            String ucarules = UCA.getRules(true);
            try {
                rbUCA = new RuleBasedCollator(ucarules);
            } catch(Exception e) {
                errln("Failure creating UCA rule-based collator: " + e);
            }
        }
    }

    private String parseString(String line) {
        int i = 0, value;
        StringBuilder result = new StringBuilder(), buffer = new StringBuilder();

        for(;;) {
            while(i < line.length() && Character.isWhitespace(line.charAt(i))) {
                i++;
            }
            while(i < line.length() && Character.isLetterOrDigit(line.charAt(i))) {
                buffer.append(line.charAt(i));
                i++;
            }
            if(buffer.length() == 0) {
                // We hit something that was not whitespace/letter/digit.
                // Should be ';' or end of string.
                return result.toString();
            }
            /* read one code point */
            value = Integer.parseInt(buffer.toString(), 16);
            buffer.setLength(0);
            result.appendCodePoint(value);
        }

    }

    private static final int IS_SHIFTED = 1;
    private static final int FROM_RULES = 2;

    private static boolean skipLineBecauseOfBug(String s, int flags) {
        // TODO: Fix ICU ticket #8052
        if(s.length() >= 3 &&
                (s.charAt(0) == 0xfb2 || s.charAt(0) == 0xfb3) &&
                s.charAt(1) == 0x334 &&
                (s.charAt(2) == 0xf73 || s.charAt(2) == 0xf75 || s.charAt(2) == 0xf81)) {
            return true;
        }
        // TODO: Fix ICU ticket #9361
        if((flags & IS_SHIFTED) != 0 && s.length() >= 2 && s.charAt(0) == 0xfffe) {
            return true;
        }
        // TODO: Fix ICU ticket #9494
        int c;
        if(s.length() >= 2 && 0xe0100 <= (c = s.codePointAt(0)) && c <= 0xe01ef) {
            return true;
        }
        // TODO: Fix ICU ticket #8923
        if((flags & FROM_RULES) != 0 && 0xac00 <= (c = s.charAt(0)) && c <= 0xd7a3) {
            return true;
        }
        // TODO: Fix UCARules.txt.
        if((flags & FROM_RULES) != 0 && s.length() >= 2 && 0xec0 <= (c = s.charAt(0)) && c <= 0xec4) {
            return true;
        }
        return false;
    }

    private static int normalizeResult(int result) {
        return result < 0 ? -1 : result == 0 ? 0 : 1;
    }

    private void conformanceTest(RuleBasedCollator coll) {
        if(in == null || coll == null) {
            return;
        }
        int skipFlags = 0;
        if(coll.isAlternateHandlingShifted()) {
            skipFlags |= IS_SHIFTED;
        }
        if(coll == rbUCA) {
            skipFlags |= FROM_RULES;
        }

        int lineNo = 0;

        String line = null, oldLine = null, buffer = null, oldB = null;
        CollationKey oldSk = null, newSk = null;

        try {
            while ((line = in.readLine()) != null) {
                lineNo++;
                if(line.length() == 0 || line.charAt(0) == '#') {
                    continue;
                }
                buffer = parseString(line);

                if(skipLineBecauseOfBug(buffer, skipFlags)) {
                    logln("Skipping line " + lineNo + " because of a known bug");
                    continue;
                }

                newSk = coll.getCollationKey(buffer);
                if(oldSk != null) {
                    int skres = oldSk.compareTo(newSk);
                    int cmpres = coll.compare(oldB, buffer);
                    int cmpres2 = coll.compare(buffer, oldB);

                    if(cmpres != -cmpres2) {
                        errln("Compare result not symmetrical on line "+lineNo);
                    }
                    if(normalizeResult(cmpres) != normalizeResult(skres)) {
                        errln("Difference between coll.compare (" + cmpres + ") and sortkey compare (" + skres + ") on line " + lineNo);
                        errln(oldLine);
                        errln(line);
                    }

                    int res = cmpres;
                    if(res == 0 && !isAtLeastUCA62) {
                        // Up to UCA 6.1, the collation test files use a custom tie-breaker,
                        // comparing the raw input strings.
                        res = comparer.compare(oldB, buffer);
                        // Starting with UCA 6.2, the collation test files use the standard UCA tie-breaker,
                        // comparing the NFD versions of the input strings,
                        // which we do via setting strength=identical.
                    }
                    if(res > 0) {
                        errln("Line " + lineNo + " is not greater or equal than previous line");
                        errln(oldLine);
                        errln(line);
                    }
                }

                oldSk = newSk;
                oldB = buffer;
                oldLine = line;
            }
        } catch (Exception e) {
            errln("Unexpected exception "+e);
        }
    }
}
