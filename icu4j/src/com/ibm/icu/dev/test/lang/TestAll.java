/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/test/lang/TestAll.java,v $
 * $Date: 2003/01/28 18:55:33 $
 * $Revision: 1.1 $
 *
 *****************************************************************************************
 */
package com.ibm.icu.dev.test.lang;
import com.ibm.icu.dev.test.TestFmwk;
import java.util.TimeZone;

/**
 * Top level test used to run all other tests as a batch.
 */

public class TestAll extends TestFmwk {

    public static void main(String[] args) throws Exception {
        TimeZone.setDefault(TimeZone.getTimeZone("PST"));
        new TestAll().run(args);
    }
    public void TestCharacter() throws Exception {
        run(new TestFmwk[] {
            new com.ibm.icu.dev.test.lang.UCharacterTest(),
            new com.ibm.icu.dev.test.lang.UCharacterCaseTest(),
            new com.ibm.icu.dev.test.lang.UCharacterIteratorTest(),
            new com.ibm.icu.dev.test.lang.UCharacterCategoryTest(),
            new com.ibm.icu.dev.test.lang.UCharacterDirectionTest(),
            new com.ibm.icu.dev.test.lang.UPropertyAliasesTest(),
            new com.ibm.icu.dev.test.lang.UTF16Test()
                });
    }
    public void TestUScript() throws Exception {
            run( new TestFmwk[] {
                new com.ibm.icu.dev.test.lang.TestUScript(),
            });
    }
    public void TestUScriptRun() throws Exception {
            run( new TestFmwk[] {
                new com.ibm.icu.dev.test.lang.TestUScriptRun(),
            });
    }
}