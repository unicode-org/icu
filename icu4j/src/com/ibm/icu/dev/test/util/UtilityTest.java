/*
**********************************************************************
* Copyright (c) 2003, International Business Machines
* Corporation and others.  All Rights Reserved.
**********************************************************************
* Author: Alan Liu
* Created: March 8 2003
* Since: ICU 2.6
**********************************************************************
*/
package com.ibm.icu.dev.test.util;
import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.impl.Utility;

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
}
