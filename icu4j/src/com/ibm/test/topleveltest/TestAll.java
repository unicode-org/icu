package com.ibm.test.topleveltest;
import com.ibm.test.TestFmwk;
import java.text.*;
import java.util.*;

/**
 * @test
 * @summary General test of UnicodeSet
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

	public void TestNormalizer3() throws Exception {
		run(new com.ibm.test.normalizer.FooTest());
	}

	public void TestRuleBasedNumberFormat1() throws Exception {
		run(new com.ibm.test.rbnf.RbnfTest());
	}

	public void TestRulebasedNumberFormat2() throws Exception {
		run(new com.ibm.test.rbnf.RbnfRoundTripTest());
	}

	public void TestRuleBasedBreakIteartor1() throws Exception {
		run(new com.ibm.test.RuleBasedBreakIterator.SimpleBITest());
	}

	public void TestRuleBasedBreakIteartor2() throws Exception {
		run(new com.ibm.test.RuleBasedBreakIterator.BreakIteratorTest());
	}

	public void TestTranslit1() throws Exception {
		run(new com.ibm.test.translit.UnicodeSetTest());
	}

	public void TestTranslit2() throws Exception {
		run(new com.ibm.test.translit.TransliteratorTest());
	}
	
	private void run(TestFmwk test) throws Exception {
		test.setParent(this);
		test.run(new String[0]);
	}
}
