/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/test/Attic/TestAll.java,v $ 
 * $Date: 2001/03/09 23:41:08 $ 
 * $Revision: 1.1 $
 *
 *****************************************************************************************
 */
package com.ibm.icu4jni.test;

/**
 * Top level test used to run all other tests as a batch.
 */
 
public class TestAll extends TestFmwk {

    public static void main(String[] args) throws Exception {
        new TestAll().run(args);
    }

    public void TestCollation() throws Exception{
        run(new TestFmwk[] {
            //new com.ibm.icu4jni.test.text.CollatorTest(),
            new com.ibm.icu4jni.test.text.CollationElementIteratorTest()
        });
    }
}
