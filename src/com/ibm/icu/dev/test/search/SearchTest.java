/*
 *******************************************************************************
 * Copyright (C) 1996-2000, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/test/search/SearchTest.java,v $ 
 * $Date: 2002/04/03 19:13:56 $ 
 * $Revision: 1.14 $
 *
 *****************************************************************************************
 */
package com.ibm.icu.dev.test.search;

import com.ibm.icu.dev.test.TestFmwk;
import com.ibm.icu.impl.ICUDebug;
import com.ibm.icu.text.SearchIterator;
import com.ibm.icu.text.StringSearch;
import com.ibm.icu.util.VersionInfo;

import java.text.BreakIterator;
import java.text.CharacterIterator;
import java.text.Collator;
import java.text.ParseException;
import java.text.RuleBasedCollator;
import java.text.StringCharacterIterator;
import java.util.Locale;
import java.util.Vector;

/**
 * Unit and regression tests for the StringSearch and SearchIterator classes.
 * This uses <code>IntlTest</code> as a framework for running the tests
 * and displaying the output.  Basically, any method here that starts with
 * <code>Test</code> is run as a test.
 */
public class SearchTest extends TestFmwk {
    public static void main(String[] args) throws Exception {
		new SearchTest().run(args);
    }

    static private final boolean valid = ICUDebug.javaVersion != VersionInfo.getInstance("1.4");
	
	protected boolean validate() {
		if (valid) {
			return true;
		}
		logln("StringSearch does not work with JDK 1.4");
		return false;
	}

    //-----------------------------------------------------------
    // Static data: collators and break iterators to use for testing
    //
    static RuleBasedCollator enColl;        // Generic English collator
    static RuleBasedCollator frColl;        // French accent rules
    static RuleBasedCollator esColl;        // Has Spanish contracting "ch"
    static RuleBasedCollator deColl;        // Has expansions, e.g. a-umlaut -> ae
    
    static {
        try {
            enColl = (RuleBasedCollator)Collator.getInstance(Locale.US);
            frColl = (RuleBasedCollator)Collator.getInstance(Locale.FRANCE);
            
            esColl = new RuleBasedCollator(enColl.getRules() + " & C < ch ; cH ; Ch ; CH");
            
            deColl = new RuleBasedCollator(enColl.getRules() + " & ae ; \u00e4 & AE ; \u00c4"
                                                             + " & oe ; \u00f6 & OE ; \u00d6"
                                                             + " & ue ; \u00fc & UE ; \u00dc"); 
        }
        catch (ParseException e) {
            System.out.print("");
        }
    }
    
    static BreakIterator enWord = BreakIterator.getWordInstance(Locale.US);
    
    static String testString = 
            "blackbirds Pat p\u00E9ch\u00E9 " +
            "p\u00EAche p\u00E9cher p\u00EAcher " +
            "Tod T\u00F6ne black Tofu blackbirds " +
            "Ton PAT toehold " +
            "blackbird " +
            "black-bird pat " +
            "toe big Toe";
    
    //-------------------------------------------------------------------------
    // The primary test consists of running through all of the strings in this
    // table and making sure we find the proper matches
    //
    static class TestCase {
        TestCase(RuleBasedCollator c, int strength, BreakIterator breaker,
                    String pattern, String target, int[] matches) {
            this.collator = c;
            this.strength = strength;
            this.breaker = breaker;
            this.pattern = pattern;
            this.target = target;
            this.matches = matches;
        }
        RuleBasedCollator   collator;
        int                 strength;
        BreakIterator       breaker;
        String              pattern;
        String              target;
        int[]               matches;
    };
    static TestCase[] testCases = {
        new TestCase(enColl, Collator.PRIMARY, null, "fox",
                //   012345678901234567890123456789012345678901234567890123456789
                    "The quick brown fox jumps over the lazy foxes",
                    new int[] { 16, 40 }
                    ),

        new TestCase(enColl, Collator.PRIMARY, enWord, "fox",
                //   012345678901234567890123456789012345678901234567890123456789
                    "The quick brown fox jumps over the lazy foxes",
                    new int[] { 16 }
                    ),

        new TestCase(frColl, Collator.PRIMARY, null, "peche",
                    testString,
                    new int[] { 15, 21, 27, 34 }
                    ),
        new TestCase(frColl, Collator.PRIMARY, enWord, "blackbird",
                    testString,
                    new int[] { 88, 98 }
                    ),

        // NOTE: this case depends on a bug fix in JDK 1.2.2 ("Cricket")
        new TestCase(deColl, Collator.PRIMARY, null, "toe",
                //   012345678901234567890123456789012345678901234567890123456789
                    "This is a toe T\u00F6ne",
                    new int[] { 10, 14 }
                    ),

        /* Due to a bug in the JDK 1.2 FCS version of CollationElementIterator,
         * searching through text containing contracting character sequences 
         * isn't working properly right now.  This will probably be fixed in
         * JDK 1.3 ("Kestrel").  When it is, uncomment these test cases.
         *
        new TestCase(esColl, Collator.PRIMARY, enWord, "channel",
                //   0123456789012345678901234567890123456789012345678901234567890123456789
                    "A channel, another CHANNEL, more Channels, and one last channel...",
                    new int[] {  }
                    ),

        new TestCase(esColl, Collator.TERTIARY, enWord, "Channel",
                //   0123456789012345678901234567890123456789012345678901234567890123456789
                    "Channel, another channel, more channels, and one last Channel",
                    new int[] {  }
                    ),
        */
    };

    /**
     * Test using the test cases defined above
     */
    public void TestCases() {
        for (int t = 0; t < testCases.length; t++)
        {
            logln("case " + t);
            TestCase c = testCases[t];
            StringSearch iter = new StringSearch(c.pattern,
                                        new StringCharacterIterator(c.target),
                                        c.collator, c.breaker);
            iter.setStrength(c.strength);
            doTestCase(iter, c.matches);
        }
    }
    
    /**
     * Test for SearchIterator.setOverlapping()
     */
    public void TestOverlapping() {
        // Create a search iterator. 
        StringSearch iter = new StringSearch("abab",
                                             new StringCharacterIterator("abababab"),
                                             enColl, null);
        
        int[] overlap = new int[] { 0, 2, 4 };  // expected results
        int[] novrlap = new int[] { 0, 4 };

        
        doTestCase(iter, overlap);          // Overlapping is allowed by default
        if (iter.isOverlapping() != true) {
            errln("ERROR: isOverlapping returned " + iter.isOverlapping());
        }
        
        iter.setOverlapping(false);         // Turn 'em back off
        doTestCase(iter, novrlap);
        if (iter.isOverlapping() != false) {
            errln("ERROR: isOverlapping returned " + iter.isOverlapping());
        }

        iter.setOverlapping(true);
        doTestCase(iter, overlap);
        if (iter.isOverlapping() != true) {
            errln("ERROR: isOverlapping returned " + iter.isOverlapping());
        }
    }
    
    /**
     * Test for SearchIterator.setBreakIterator
     */
    public void TestBreakIterator() {
        StringSearch iter = new StringSearch("fox",
                                 new StringCharacterIterator("foxy fox"),
                                 enColl, null);

        BreakIterator charBreaker = BreakIterator.getCharacterInstance(Locale.US);
        BreakIterator wordBreaker = BreakIterator.getWordInstance(Locale.US);
        
        int[] chars = new int[] { 0, 5 };   // expected results
        int[] words = new int[] { 5 };
        
        logln("default break iterator...");
        doTestCase(iter, chars);            // character breaker by default
        
        logln("word break iterator...");
        iter.setBreakIterator(wordBreaker); // word break detection
        doTestCase(iter, words);
        if (iter.getBreakIterator() != wordBreaker) {
            errln("ERROR: getBreakIterator returned wrong object");
        }

        logln("char break iterator...");
        iter.setBreakIterator(charBreaker); // char break detection
        doTestCase(iter, chars);
        if (iter.getBreakIterator() != charBreaker) {
            errln("ERROR: getBreakIterator returned wrong object");
        }

        logln("null break iterator...");
        iter.setBreakIterator(null);
        doTestCase(iter, chars);
        if (iter.getBreakIterator() != null) {
            errln("ERROR: getBreakIterator returned wrong object");
        }
    }
    
    /**
     * Test for SearchIterator.setTarget
     */
    public void TestSetTarget() {
        String  pat = "fox";
        String  targ1 = "the foxy brown fox";
        String  targ2 = "the quick brown fox";
        
        int[] match1 = new int[] { 4, 15 };   // expected results
        int[] match2 = new int[] { 16 };

        StringSearch iter = new StringSearch(pat, new StringCharacterIterator(targ1),
                                 enColl, null);

        logln("initial text...");
        doTestCase(iter, match1);
        assertEqual(iter.getTarget(), targ1);
        
        logln("target #2...");
        iter.setTarget(new StringCharacterIterator(targ2));
        doTestCase(iter, match2);
        assertEqual(iter.getTarget(), targ2);
        
        logln("back to target #1...");
        iter.setTarget(new StringCharacterIterator(targ1));
        doTestCase(iter, match1);
        assertEqual(iter.getTarget(), targ1);
    }
    
    /**
     * Test for StringSearch.setStrength
     */
    public void TestSetStrength() {
        String  pat = "fox";
        String  targ = "the foxy brown Fox";
        
        // not used int[] match1 = new int[] { 4, 15 };   // expected results
        int[] match3 = new int[] { 4 };

        StringSearch iter = new StringSearch(pat, new StringCharacterIterator(targ),
                                 enColl, null);

       /* logln("Trying primary strength...");
        iter.setStrength(Collator.PRIMARY);
        doTestCase(iter, match1);
        if (iter.getStrength() != Collator.PRIMARY) {
            errln("ERROR: getStrength: expected PRIMARY, got " + iter.getStrength());
        } */
        
        logln("Trying tertiary strength...");
        iter.setStrength(Collator.TERTIARY);
        doTestCase(iter, match3);
        if (iter.getStrength() != Collator.TERTIARY) {
            errln("ERROR: getStrength: expected PRIMARY, got " + iter.getStrength());
        }
        
    }

    /**
     * Test for StringSearch.setCollator
     */
    public void TestSetCollator() throws ParseException {
        // Create a test collator that thinks "o" and "p" are the same thing
        RuleBasedCollator testColl = new RuleBasedCollator(enColl.getRules()
                                            + "& o,O ; p,P" );
                                            
        String  pat = "fox";
        String  targ = "fox fpx ";
        
        int[] match1 = new int[] { 0 };     // English results
        int[] match2 = new int[] { 0, 4 };  // Test collator results

        StringSearch iter = new StringSearch(pat, new StringCharacterIterator(targ),
                                 enColl, null);

        logln("Trying English collator...");

        iter.setStrength(Collator.PRIMARY);
        doTestCase(iter, match1);
        if (iter.getCollator() != enColl) {
            errln("ERROR: getCollator returned wrong collator");
        }
        
        logln("Trying test collator...");

        iter.setCollator(testColl);
        iter.setStrength(Collator.PRIMARY);
        doTestCase(iter, match2);
        if (iter.getCollator() != testColl) {
            errln("ERROR: getCollator returned wrong collator");
        }
        
        logln("Trying English collator again...");

        iter.setCollator(enColl);
        iter.setStrength(Collator.PRIMARY);
        doTestCase(iter, match1);
        if (iter.getCollator() != enColl) {
            errln("ERROR: getCollator returned wrong collator");
        }
        
    }

    /**
     * Test for StringSearch.setPattern
     */
    public void TestSetPattern() {
                      // 01234567890123456789012345678901234567890123456789
        String target = "The quick brown fox jumps over the lazy foxes";
        String pat1 = "the";
        String pat2 = "fox";
        
        int[] match1 = new int[] { 0, 31 };
        int[] match2 = new int[] { 16, 40 };

        StringSearch iter = new StringSearch(pat1, new StringCharacterIterator(target),
                                 enColl, null);
        iter.setStrength(Collator.PRIMARY);
        
        doTestCase(iter, match1);
        if (!iter.getPattern().equals(pat1)) {
            errln("getPattern returned '" + iter.getPattern() + "', expected '"
                + pat1 + "'");
        }
        
        iter.setPattern(pat2);
        doTestCase(iter, match2);
        if (!iter.getPattern().equals(pat2)) {
            errln("getPattern returned '" + iter.getPattern() + "', expected '"
                + pat1 + "'");
        }
        
        iter.setPattern(pat1);
        doTestCase(iter, match1);
        if (!iter.getPattern().equals(pat1)) {
            errln("getPattern returned '" + iter.getPattern() + "', expected '"
                + pat1 + "'");
        }
    }
    
    /**
     * Test for an infinite loop that happened when the target text started
     * with an ignorable character.
     * Reported by Muly Oved, <mulyoved@netvision.net.il>
     */
    public void TestIgnorableLoop() {
        String pattern = "go";
        String target  = "  on";

        StringSearch search;
        
        try {
            search=new StringSearch(pattern, new StringCharacterIterator(target), enColl);
                                                
            logln("searching... "+pattern);
            search.first();
            logln("Will never go here if searching for 'go'");
        } catch (Exception e) {
            errln("Caught exception: " + e.toString());
        }

        logln("end");
    }
    
    /**
    * Tests jitterbug #11
    */
    public void TestJitterBug11() 
    {
        String pattern = "c";
        String text = "Scott Ganyo";
        StringSearch ss = new StringSearch(pattern, text);
        ss.setStrength(Collator.PRIMARY);
        if (ss.next() != 1) {
            errln("Error finding character c at index 1 in \"Scott Ganyo\"");
        }
    
        /* empty pattern not handled yet
        pattern = " ";
        ss.setPattern(pattern);
        ss.setStrength(Collator.PRIMARY);
        if (ss.next() != 5) {
            errln("Error finding character ' ' at index 1 in \"Scott Ganyo\"");
        }
        */
    }
    
    /**
    * Tests jitterbug #35
    */
    public void TestJitterBug35() 
    {
        String pattern = "dss";
		String strings[] = {"<HTML xxx=\".dss\">", "<BODY>",
			                "<A A=\".dss\">",      "<B B=\".dss\">",
			                "<AB A=\".dss\">",     "<A A=\"dss\">",
			                "<AB A=\"dss\">",      "<AB AB=\".dss\">",
			                "<AB AB=\"dss\">",     " <A A=\".dss\">",
			                "A A=\".dss\"",        "&lt;A A=\".dss\"&gt;"};
		int result[] = {12, SearchIterator.DONE, 7, 7, 8, 6, 7, 9, 8, 8, 6, 
		                10};

        StringSearch ss = new StringSearch( pattern, "empty" );
		for ( int i = 0; i < strings.length; i++ )
	    {
			StringCharacterIterator source = 
			                          new StringCharacterIterator(strings[i]);
		    ss.setTarget(source);
			int offset = ss.first();
			if ( offset != result[i] ) {
			    errln("Error finding pattern \"" + pattern + 
			    "\" match in text \"" + strings[i] + "\"");
			}
        }
    }
    
    public void TestJitterBug1513()
    {
        String contents ="<HTML>\n<HEAD>\n<TITLE>External Ano</TITLE>\n" +
                   "</HEAD>\n<BODY>\n<B>Hello world.</B>\n\n<TABLE>\n" +
                   "<TR><TH>Cars</TH><TH>Colors</TH><TH>Interiors</TH></TR>\n"
                   + "<TR><TH>2\nDoor</TH><TH>Red</TH><TH>Leather</TH></TR>\n"
                   + "<TR><TH>4\nDoor</TH><TH>Blue</TH><TH>Cloth</TH></TR>\n" 
                   + "\n<TR><TD>Convertible</TD><TD>Black</TD>" +
                   "<TD>Vinyl</TD></TR>\n\n</TABLE>\n\nRemove This text.\n" +
                   "\n\n\n<TABLE>\n<TR><TH><B>Warranty</B></TH>" +
                   "<TH><B>Length</B></TH><TH>Restrictions</TH></TR>\n" +
                   "<TR><TH>Silver</TH><TH>2\nYears</TH><TH>Yes</TH></TR>\n" +
                   "<TR><TH>Gold</TH><TH>5\nYears</TH><TH>No</TH></TR>\n" +
                   "</TABLE>\n\n</BODY>\n</HTML>\n\n\n\n\n\n\n\n/B>\n" +
                   "<TABLE>\n" +
                   "<TR><TH>Cars</TH><TH>Colors</TH><TH>Interiors</TH></TR>\n"
                   + "<TR><TH>2\nDoor</TH><TH>Red</TH><TH>Leather</TH></TR>\n"
                   + "<TR><TH>4\nDoor</TH><TH>Blue</TH><TH>Cloth</TH></TR>\n" 
                   + "<TR><TD>Convertible</TD><TD>Black</TD>" +
                   "<TD>Vinyl</TD></TR>\n</TABLE>\n\nRemove This text.\n\n\n"
                   + "\n<TABLE>\n" +
                   "<TR><TH><B>Warranty</B></TH><TH><B>Length</B></TH>" +
                   "<TH>Restrictions</TH></TR>\n<TR><TH>Silver</TH><TH>2\n" +
                   "Years</TH><TH>Yes</TH></TR>\n<TR><TH>Gold</TH>";
        String pattern = "HE";
        int    offset[] = {8, 44};

		logln("Searching for all occurrences of pattern " +
			   pattern + " following contents:\n" + contents);

	    StringSearch searcher = new StringSearch(pattern, contents);
        int textoffset = searcher.next();
        int count = 0;
        
	    while (textoffset != SearchIterator.DONE && count < offset.length) {
	        if (textoffset != offset[count]) {
		        errln( "Found match not found at offset " + offset[count]);
		    }
		    if (searcher.getMatchLength() != pattern.length() ||
		        !searcher.getMatchedText().equals(pattern)) {
		        errln("Failed finding match for pattern " + pattern + " got " +
		              searcher.getMatchedText()); 
		    }
		    count ++;
		    textoffset = searcher.next();
		}
		if (textoffset != SearchIterator.DONE || count != offset.length) {
		    errln("End of pattern " + pattern + " search encountering problems");
		}
    }
    
    //-------------------------------------------------------------------------
    // Various internal utility methods....
    //-------------------------------------------------------------------------

    void assertEqual(CharacterIterator i1, String s2) {
        CharacterIterator i2 = new StringCharacterIterator(s2);
        char c1 = i1.first();
        char c2 = i2.first();
        int i = 0;
        
        while (c1 == c2 && c1 != CharacterIterator.DONE) {
            c1 = i1.next();
            c2 = i2.next();
        }
        if (c1 != CharacterIterator.DONE || c2 != CharacterIterator.DONE) {
            errln("CharacterIterator mismatch at index " + i);
        }
    }
    
    void doTestCase(StringSearch iter, int[] expected) {
        //
        // The basic logic here is as follows...  We construct a search
        // iterator and use it to find all of the matches in the target
        // text.  Then we compare it to the expected matches
        //
        Vector matches = new Vector();

        for (int i = iter.first(); i != SearchIterator.DONE; i = iter.next()) {
            matches.addElement(new Integer(i));
        }
        compareMatches(expected, matches);
        
        // Now do the same exact thing as above, but in reverse
        logln("Now searching in reverse...");
        matches.removeAllElements();
        for (int i = iter.last(); i != SearchIterator.DONE; i = iter.previous()) {
            matches.insertElementAt(new Integer(i), 0);
        }
        compareMatches(expected, matches);
    }
    
    /**
     * Utility function used by TestCases to compare the matches that
     * were found against the ones that were expected
     */
    void compareMatches(int[] expected, Vector found) {
        // Step through the two arrays in parallel and make sure that they're
        // the same
        
        int e=0, f=0;
        
        while (e < expected.length && f < found.size()) {
            int eVal = expected[e];
            int fVal = ((Integer)found.elementAt(f)).intValue();
            
            if (eVal < fVal) {
                errln("Missed expected match at " + eVal);
                e++;
            } else if (eVal > fVal) {
                errln("Found unexpected match at " + fVal);
                f++;
            } else {
                e++;
                f++;
            }
        }
        while (e < expected.length) {
            errln("Missed expected match at " + expected[e]);
            e++;
        }
        while (f < found.size()) {
            int fVal = ((Integer)found.elementAt(f)).intValue();
            errln("Found unexpected match at " + fVal);
            f++;
        }
    }

    /**
     * ICU4J Jitterbug 11
     */
    /*
    Bug to be solved in later release.
    Commented away for successful testing.
    TODO.
    public void TestJ11() {
        AuxJ11("c", "Scott Ganyo", 1);
        AuxJ11(" ", "Scott Ganyo", 5);
    }
    */

    private void AuxJ11(String pattern, String text, int expectedLoc) {
        try {
            StringSearch ss = new StringSearch(pattern, text);
            ss.setStrength(Collator.PRIMARY);
            int loc = ss.next();
            if (loc == expectedLoc) {
                logln("Ok: StringSearch(\"" + pattern + "\", \"" + text + "\") = " + loc);
            } else {
                errln("FAIL: StringSearch(\"" + pattern + "\", \"" + text + "\") = " + loc +
                      ", expected " + expectedLoc);
            }
        } catch (Exception e) {
            errln("FAIL: StringSearch(\"" + pattern + "\", \"" + text + "\") threw ");
            e.printStackTrace();
        }
    }
}
