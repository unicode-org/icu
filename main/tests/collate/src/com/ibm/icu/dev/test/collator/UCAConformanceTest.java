/********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 2002-2009, International Business Machines Corporation and
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
import com.ibm.icu.text.CollationKey;
import com.ibm.icu.text.Collator;
import com.ibm.icu.text.RuleBasedCollator;
import com.ibm.icu.text.UTF16;

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
    RuleBasedCollator UCA;
    RuleBasedCollator rbUCA;
    UTF16.StringComparator comparer;
    
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
        
        setCollNonIgnorable(rbUCA);
        openTestFile("NON_IGNORABLE");
        conformanceTest(rbUCA);
    }
    
    public void TestRulesShifted() {
        logln("This test is currently disabled, as it is impossible to "+
        "wholly represent fractional UCA using tailoring rules.");
        return;
        /*        initRbUCA();
         
         if(U_SUCCESS(status)) {
         setCollShifted(rbUCA);
         openTestFile("SHIFTED");
         testConformance(rbUCA);
         }
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
            coll.setDecomposition(RuleBasedCollator.CANONICAL_DECOMPOSITION);
            coll.setLowerCaseFirst(false);
            coll.setCaseLevel(false);
            coll.setStrength(RuleBasedCollator.TERTIARY);
            coll.setAlternateHandlingShifted(false);
        }
    }
    
    private void setCollShifted(RuleBasedCollator coll) 
    {
        if(coll != null) {
            coll.setDecomposition(RuleBasedCollator.CANONICAL_DECOMPOSITION);
            coll.setLowerCaseFirst(false);
            coll.setCaseLevel(false);
            coll.setStrength(RuleBasedCollator.QUATERNARY);
            coll.setAlternateHandlingShifted(true);
        }
    }
    
    
    
    private void initRbUCA() 
    {
        /*      if(!rbUCA) {
         UParseError parseError;
         UChar      *ucarules = buffer;
         int32_t size = ucol_getRulesEx(UCA, UCOL_FULL_RULES, ucarules, 
         BUFFER_SIZE_);
         if (size > BUFFER_SIZE_) {
         ucarules = (UChar *)malloc(size * sizeof(UChar));
         size = ucol_getRulesEx(UCA, UCOL_FULL_RULES, ucarules, size);
         }
         rbUCA = ucol_openRules(ucarules, size, UCOL_DEFAULT, UCOL_TERTIARY, 
         &parseError, &status);
         if (U_FAILURE(status)) {
         errln("Failure creating UCA rule-based collator: %s", u_errorName(status));
         return;
         }
         }
         */
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
    private void conformanceTest(RuleBasedCollator coll) {
        if(in == null || coll == null) {
            return;
        }
        
        int lineNo = 0;
        
        String line = null, oldLine = null, buffer = null, oldB = null;
        CollationKey oldSk = null, newSk = null;
        
        int res = 0, cmpres = 0, cmpres2 = 0;
        
        try {
            while ((line = in.readLine()) != null) {
                lineNo++;
                if(line.length() < 3 || line.charAt(0) == '#') {
                    continue;
                }
                buffer = parseString(line);
                
                newSk = coll.getCollationKey(buffer);
                if(oldSk != null) {
                    res = oldSk.compareTo(newSk);
                    cmpres = coll.compare(oldB, buffer);
                    cmpres2 = coll.compare(buffer, oldB);
                    
                    if(cmpres != -cmpres2) {
                        errln("Compare result not symmetrical on line "+lineNo);
                    }
                    if(((res&0x80000000) != (cmpres&0x80000000)) || (res == 0 && cmpres != 0) || (res != 0 && cmpres == 0)) {
                        errln("Difference between ucol_strcoll and sortkey compare on line " + lineNo);
                        logln(oldLine);
                        logln(line);
                    }
                    
                    if(res > 0) {
                        errln("Line " + lineNo + " is not greater or equal than previous line");
                        logln(oldLine);
                        logln(line);
                        cmpres = coll.compare(oldB, buffer);
                    } else if(res == 0) {  // equal 
                        res = comparer.compare(oldB, buffer);
                        if (res == 0) {
                            errln("Probable error in test file on line " + lineNo +" (comparing identical strings)");
                            logln(oldLine);
                            logln(line);
                        } else if (res > 0) {
                            errln("Sortkeys are identical, but code point comapare gives >0 on line " + lineNo);
                            logln(oldLine);
                            logln(line);
                        }
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
