/*
 *******************************************************************************
 * Copyright (C) 2002-2004, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */

/** 
 * Port From:   ICU4C v2.1 : Collate/CollationTurkishTest
 * Source File: $ICU4CRoot/source/test/intltest/trcoll.cpp
 **/
 
package com.ibm.icu.dev.test.collator;
 
import com.ibm.icu.dev.test.*;
import com.ibm.icu.text.*;
import java.util.Locale;
 
public class CollationTurkishTest extends TestFmwk{
    public static void main(String[] args) throws Exception{
        new CollationTurkishTest().run(args);
    }
    
    private static char[][] testSourceCases = {
        {0x73, 0x0327},
        {0x76, 0x00E4, 0x74},
        {0x6f, 0x6c, 0x64},
        {0x00FC, 0x6f, 0x69, 0x64},
        {0x68, 0x011E, 0x61, 0x6c, 0x74},
        {0x73, 0x74, 0x72, 0x65, 0x73, 0x015E},
        {0x76, 0x6f, 0x0131, 0x64},
        {0x69, 0x64, 0x65, 0x61},
        {0x00FC, 0x6f, 0x69, 0x64},
        {0x76, 0x6f, 0x0131, 0x64},
        {0x69, 0x64, 0x65, 0x61}
    };

    private static char[][] testTargetCases = {
        {0x75, 0x0308},
        {0x76, 0x62, 0x74},
        {0x00D6, 0x61, 0x79},
        {0x76, 0x6f, 0x69, 0x64},
        {0x68, 0x61, 0x6c, 0x74},
        {0x015E, 0x74, 0x72, 0x65, 0x015E, 0x73},
        {0x76, 0x6f, 0x69, 0x64},
        {0x49, 0x64, 0x65, 0x61},
        {0x76, 0x6f, 0x69, 0x64},
        {0x76, 0x6f, 0x69, 0x64},
        {0x49, 0x64, 0x65, 0x61}
    };

    private static int[] results = {
        -1,
        -1,
        -1,
        -1,
        1,
        -1,
        -1,
        1,
    // test priamry > 8
        -1,
        -1,
        1
    };
    
    private Collator myCollation = null;
    
    public CollationTurkishTest() {
        try {
            myCollation = Collator.getInstance(new Locale("tr", ""));
        } catch (Exception e) {
            errln("ERROR: in creation of collator of TURKISH locale");
            return;
        }
    }
    
    public void TestTertiary() {
        int i = 0;
        myCollation.setStrength(Collator.TERTIARY);
        for (i = 0; i < 8 ; i++) {
            doTest(testSourceCases[i], testTargetCases[i], results[i]);
        }
    }
    
    public void TestPrimary() {
        int i;
        myCollation.setStrength(Collator.PRIMARY);
        for (i = 8; i < 11; i++) {
            doTest(testSourceCases[i], testTargetCases[i], results[i]);
        }
    }
    
    
    // main test routine, tests rules specific to turkish locale
    private void doTest(char[] source, char[] target, int result) {
        String s = new String(source);
        String t = new String(target);
        int compareResult = myCollation.compare(s, t);
        CollationKey sortKey1, sortKey2;
        sortKey1 = myCollation.getCollationKey(s);
        sortKey2 = myCollation.getCollationKey(t);
        int keyResult = sortKey1.compareTo(sortKey2);
        reportCResult(s, t, sortKey1, sortKey2, compareResult, keyResult, compareResult, result);
        
    }
    
    private void reportCResult( String source, String target, CollationKey sourceKey, CollationKey targetKey,
                                int compareResult, int keyResult, int incResult, int expectedResult ) {
        if (expectedResult < -1 || expectedResult > 1) {
            errln("***** invalid call to reportCResult ****");
            return;
        }

        boolean ok1 = (compareResult == expectedResult);
        boolean ok2 = (keyResult == expectedResult);
        boolean ok3 = (incResult == expectedResult);

        if (ok1 && ok2 && ok3 && !isVerbose()) {
            return;    
        } else {
            String msg1 = ok1? "Ok: compare(\"" : "FAIL: compare(\"";
            String msg2 = "\", \"";
            String msg3 = "\") returned ";
            String msg4 = "; expected ";
            
            String sExpect = new String("");
            String sResult = new String("");
            sResult = appendCompareResult(compareResult, sResult);
            sExpect = appendCompareResult(expectedResult, sExpect);
            if (ok1) {
                logln(msg1 + source + msg2 + target + msg3 + sResult);
            } else {
                errln(msg1 + source + msg2 + target + msg3 + sResult + msg4 + sExpect);
            }
            
            msg1 = ok2 ? "Ok: key(\"" : "FAIL: key(\"";
            msg2 = "\").compareTo(key(\"";
            msg3 = "\")) returned ";
            sResult = appendCompareResult(keyResult, sResult);
            if (ok2) {
                logln(msg1 + source + msg2 + target + msg3 + sResult);
            } else {
                errln(msg1 + source + msg2 + target + msg3 + sResult + msg4 + sExpect);
                msg1 = "  ";
                msg2 = " vs. ";
                errln(msg1 + prettify(sourceKey) + msg2 + prettify(targetKey));
            }
            
            msg1 = ok3 ? "Ok: incCompare(\"" : "FAIL: incCompare(\"";
            msg2 = "\", \"";
            msg3 = "\") returned ";

            sResult = appendCompareResult(incResult, sResult);

            if (ok3) {
                logln(msg1 + source + msg2 + target + msg3 + sResult);
            } else {
                errln(msg1 + source + msg2 + target + msg3 + sResult + msg4 + sExpect);
            }                
        }
    }
    
    private String appendCompareResult(int result, String target) {
        if (result == -1) {
            target += "LESS";
        } else if (result == 0) {
            target += "EQUAL";
        } else if (result == 1) {
            target += "GREATER";
        } else {
            String huh = "?";
            target += huh + result;
        }
        return target;
    }
    
    String prettify(CollationKey sourceKey) {
        int i;
        byte[] bytes= sourceKey.toByteArray();
        String target = "[";
    
        for (i = 0; i < bytes.length; i++) {
            target += Integer.toHexString(bytes[i]);
            target += " ";
        }
        target += "]";
        return target;
    }
}