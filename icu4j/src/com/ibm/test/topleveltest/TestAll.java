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
        com.ibm.test.bnf.BigNumberFormatTest.main(getArgs());
    }
    
    public void TestCompression1() throws Exception{
        com.ibm.test.compression.Test.main(getArgs());
    }
    
    public void TestCompression2() throws Exception{
        com.ibm.test.compression.Main.main(getArgs());
    }

	public void TestNormalizer1() throws Exception{
		com.ibm.test.normalizer.BasicTest.main(getArgs());
	}

	public void TestNormalizer2() throws Exception {
		com.ibm.test.normalizer.ExhaustiveTest.main(getArgs());
	}

	public void TestNormalizer3() throws Exception {
		com.ibm.test.normalizer.FooTest.main(getArgs());
	}

	public void TestRuleBasedNumberFormat1() throws Exception {
		com.ibm.test.rbnf.RbnfTest.main(getArgs());
	}

	public void TestRulebasedNumberFormat2() throws Exception {
		com.ibm.test.rbnf.RbnfRoundTripTest.main(getArgs());
	}

	public void TestRuleBasedBreakIteartor1() throws Exception {
		com.ibm.test.RuleBasedBreakIterator.SimpleBITest.main(getArgs());
	}

	public void TestRuleBasedBreakIteartor2() throws Exception {
		com.ibm.test.RuleBasedBreakIterator.BreakIteratorTest.main(getArgs());
	}

	public void TestTranslit1() throws Exception {
		com.ibm.test.translit.UnicodeSetTest.main(getArgs());
	}

	public void TestTranslit2() throws Exception {
		com.ibm.test.translit.TransliteratorTest.main(getArgs());
	}
    
    private String[] getArgs() {
        int size =
            ((verbose) ? 1 : 0)
            + ((prompt) ? 1 : 0)
            + ((nothrow) ? 1 : 0);
        String [] result = new String[size];
        if (verbose) result[--size] = "-verbose";
        if (prompt) result[--size] = "-prompt";
        if (nothrow) result[--size] = "-nothrow";
        return result;
    }

}
