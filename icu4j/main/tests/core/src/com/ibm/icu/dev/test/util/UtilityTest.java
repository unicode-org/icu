/*
**********************************************************************
* Copyright (c) 2003-2009, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
* Author: Alan Liu
* Created: March 8 2003
* Since: ICU 2.6
**********************************************************************
*/
package com.ibm.icu.dev.test.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.impl.Assert;
import com.ibm.icu.impl.InvalidFormatException;
import com.ibm.icu.impl.Utility;
import com.ibm.icu.text.UnicodeSet;
import com.ibm.icu.util.ByteArrayWrapper;
import com.ibm.icu.util.CaseInsensitiveString;

/**
 * @test
 * @summary Test of internal Utility class
 */
public class UtilityTest extends TestFmwk {

    public static void main(String[] args) throws Exception {
        new UtilityTest().run(args);
    }

    public void TestUnescape() {
        final String input =
            "Sch\\u00f6nes Auto: \\u20ac 11240.\\fPrivates Zeichen: \\U00102345\\e\\cC\\n \\x1b\\x{263a}";

        final String expect = 
            "Sch\u00F6nes Auto: \u20AC 11240.\u000CPrivates Zeichen: \uDBC8\uDF45\u001B\u0003\012 \u001B\u263A";

        String result = Utility.unescape(input);
        if (!result.equals(expect)) {
            errln("FAIL: Utility.unescape() returned " + result + ", exp. " + expect);
        }
    }
    
    public void TestFormat()
    {
        String data[] = {
            "the quick brown fox jumps over the lazy dog",
            // result of this conversion will exceed the original length and
            // cause a newline to be inserted
            "testing space , quotations \"",
            "testing weird supplementary characters \ud800\udc00",
            "testing control characters \u0001 and line breaking!! \n are we done yet?"
        };
        String result[] = {
            "        \"the quick brown fox jumps over the lazy dog\"",
            "        \"testing space , quotations \\042\"",
            "        \"testing weird supplementary characters \\uD800\\uDC00\"",
            "        \"testing control characters \\001 and line breaking!! \\n are we done ye\"+"
                     + Utility.LINE_SEPARATOR + "        \"t?\""
        };
        String result1[] = {
            "\"the quick brown fox jumps over the lazy dog\"",
            "\"testing space , quotations \\042\"",
            "\"testing weird supplementary characters \\uD800\\uDC00\"",
            "\"testing control characters \\001 and line breaking!! \\n are we done yet?\""
        };
        
        for (int i = 0; i < data.length; i ++) {
            assertEquals("formatForSource(\"" + data[i] + "\")",
                         result[i], Utility.formatForSource(data[i]));
        }
        for (int i = 0; i < data.length; i ++) {
            assertEquals("format1ForSource(\"" + data[i] + "\")",
                         result1[i], Utility.format1ForSource(data[i]));
        }
    }
    
    public void TestHighBit()
    {
        int data[] = {-1, -1276, 0, 0xFFFF, 0x1234};
        byte result[] = {-1, -1, -1, 15, 12};
        for (int i = 0; i < data.length; i ++) {
            if (Utility.highBit(data[i]) != result[i]) {
                errln("Fail: Highest bit of \\u" 
                      + Integer.toHexString(data[i]) + " should be "
                      + result[i]);
            }
        }
    }
    
    public void TestCompareUnsigned()
    {
        int data[] = {0, 1, 0x8fffffff, -1, Integer.MAX_VALUE, 
                      Integer.MIN_VALUE, 2342423, -2342423};
        for (int i = 0; i < data.length; i ++) {
            for (int j = 0; j < data.length; j ++) {
                if (Utility.compareUnsigned(data[i], data[j]) 
                    != compareLongUnsigned(data[i], data[j])) {
                    errln("Fail: Unsigned comparison failed with " + data[i] 
                          + " " + data[i + 1]);
                }
            }
        }
    }

    // This test indends to test the utility class ByteArrayWrapper
    // Seems that the class is somewhat incomplete, for example
    //      - getHashCode(Object) is weird
    //      - PatternMatch feature(search part of array within the whole one) lacks
    public void TestByteArrayWrapper()
    {
        byte[] ba = {0x00, 0x01, 0x02};
        byte[] bb = {0x00, 0x01, 0x02, -1};

        java.nio.ByteBuffer buffer = java.nio.ByteBuffer.wrap(ba);
        ByteArrayWrapper x = new ByteArrayWrapper(buffer);
        
        ByteArrayWrapper y = new ByteArrayWrapper(ba, 3);
        ByteArrayWrapper z = new ByteArrayWrapper(bb, 3);

        
        if (!y.toString().equals("00 01 02")){
            errln("FAIL: test toString : Failed!");
        }
        
        // test equality
        if (!x.equals(y) || !x.equals(z))
            errln("FAIL: test (operator ==): Failed!");
        if (x.hashCode()!=y.hashCode())
            errln("FAIL: identical objects have different hash codes.");

        // test non-equality
        y = new ByteArrayWrapper(bb, 4);
        if (x.equals(y))
            errln("FAIL: test (operator !=): Failed!");

        // test sign of unequal comparison
        if ((x.compareTo(y) > 0) != (y.compareTo(x) < 0)) {
            errln("FAIL: comparisons not opposite sign");
        }
    }

    private int compareLongUnsigned(int x, int y)
    {
        long x1 = x & 0xFFFFFFFFl;
        long y1 = y & 0xFFFFFFFFl;
        if (x1 < y1) {
            return -1;
        }
        else if (x1 > y1) {
            return 1;
        }
        return 0;
    }
    public void TestUnicodeSet(){
        String[] array = new String[]{"a", "b", "c", "{de}"};
        List list = Arrays.asList(array);
        Set aset = new HashSet(list);
        logln(" *** The source set's size is: " + aset.size());
    //The size reads 4
        UnicodeSet set = new UnicodeSet();
        set.clear();
        set.addAll(aset);
        logln(" *** After addAll, the UnicodeSet size is: " + set.size());
    //The size should also read 4, but 0 is seen instead

    }

    public void TestAssert(){
        try {
            Assert.assrt(false);
            errln("FAIL: Assert.assrt(false)");
        }
        catch (IllegalStateException e) {
            if (e.getMessage().equals("assert failed")) {
                logln("Assert.assrt(false) works");
            }
            else {
                errln("FAIL: Assert.assrt(false) returned " + e.getMessage());
            }
        }
        try {
            Assert.assrt("Assert message", false);
            errln("FAIL: Assert.assrt(false)");
        }
        catch (IllegalStateException e) {
            if (e.getMessage().equals("assert 'Assert message' failed")) {
                logln("Assert.assrt(false) works");
            }
            else {
                errln("FAIL: Assert.assrt(false) returned " + e.getMessage());
            }
        }
        try {
            Assert.fail("Assert message");
            errln("FAIL: Assert.fail");
        }
        catch (IllegalStateException e) {
            if (e.getMessage().equals("failure 'Assert message'")) {
                logln("Assert.fail works");
            }
            else {
                errln("FAIL: Assert.fail returned " + e.getMessage());
            }
        }
        try {
            Assert.fail(new InvalidFormatException());
            errln("FAIL: Assert.fail with an exception");
        }
        catch (IllegalStateException e) {
            logln("Assert.fail works");
        }
    }
    
    public void TestCaseInsensitiveString() {
        CaseInsensitiveString str1 = new CaseInsensitiveString("ThIs is A tEst");
        CaseInsensitiveString str2 = new CaseInsensitiveString("This IS a test");
        if (!str1.equals(str2)
            || !str1.toString().equals(str1.getString())
            || str1.toString().equals(str2.toString()))
        {
            errln("FAIL: str1("+str1+") != str2("+str2+")");
        }
    }
}
