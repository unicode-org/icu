/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/test/topleveltest/Attic/TestAll.java,v $ 
 * $Date: 2000/03/14 19:54:51 $ 
 * $Revision: 1.9 $
 *
 *****************************************************************************************
 */
package com.ibm.test.topleveltest;
import com.ibm.test.TestFmwk;
import java.text.*;
import java.util.*;

/**
 * Top level test used to run all other tests as a batch.
 */
 
public class TestAll extends TestFmwk {

    public static void main(String[] args) throws Exception {
        new TestAll().run(args);
    }

    public void TestBigNumberFormat() throws Exception{
        run(new com.ibm.test.bnf.BigNumberFormatTest());
    }
    
    public void TestCompression1() throws Exception{
        run(new com.ibm.test.compression.DecompressionTest());
    }
    
    public void TestCompression2() throws Exception{
        run(new com.ibm.test.compression.ExhaustiveTest());
    }

	public void TestNormalizer1() throws Exception{
		run(new com.ibm.test.normalizer.BasicTest());
	}

	public void TestNormalizer2() throws Exception {
		run(new com.ibm.test.normalizer.ExhaustiveTest());
	}

	public void TestRuleBasedNumberFormat1() throws Exception {
		run(new com.ibm.test.rbnf.RbnfTest());
	}

	public void TestRulebasedNumberFormat2() throws Exception {
		run(new com.ibm.test.rbnf.RbnfRoundTripTest());
	}

	public void TestRuleBasedBreakIteartor1() throws Exception {
		run(new com.ibm.test.rbbi.SimpleBITest());
	}

	public void TestRuleBasedBreakIteartor2() throws Exception {
		run(new com.ibm.test.rbbi.BreakIteratorTest());
	}

	public void TestTranslit1() throws Exception {
		run(new com.ibm.test.translit.UnicodeSetTest());
	}

	public void TestTranslit2() throws Exception {
		run(new com.ibm.test.translit.TransliteratorTest());
	}
	
	public void TestSearch() throws Exception {
		run(new com.ibm.test.search.SearchTest());
	}
}
