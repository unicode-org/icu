/**
*******************************************************************************
* Copyright (C) 1996-2000, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: 
*  /usr/cvs/icu4j/icu4j/src/com/ibm/icu/test/text/ThaiCollatorTest.java,v $ 
* $Date: 2001/03/09 00:42:45 $ 
* $Revision: 1.1 $
*
*******************************************************************************
*/

package com.ibm.icu4jni.test.text;

import java.util.Locale;
import com.ibm.icu4jni.text.Collator;
import com.ibm.icu4jni.text.CollationKey;

/**
* Testing class for Thai collator
* Mostly following the test cases for ICU
* @author Syn Wee Quek
* @since jan 29 2001
*/
public final class ThaiCollatorTest 
{ 
  
  // constructor ===================================================
  
  /**
  * Constructor
  */
  public ThaiCollatorTest(CollatorTest testprogram) throws Exception
  {
    m_test_ = testprogram;
    m_collator_ = Collator.getInstance(new Locale("th", "TH"));
  }
  
  // public methods ================================================

  /**
  * Test Thai strings.
  * Selective strings are taken out of th18057.txt from ICU for testing.
  * Every interval of a 1000 strings 5 consecutive ones are tested.
  * Strings are in sorted increasing order.
  * @exception thrown when error occurs 
  */
  public void TestStrings() throws Exception
  {
    String s, 
           t;
    for (int i = 1; i < SOURCE_TEST_CASE_.length; i ++)
    {
      s = SOURCE_TEST_CASE_[i - 1];
      t = SOURCE_TEST_CASE_[i];
      if (m_collator_.compare(s, t) != Collator.RESULT_LESS)
        m_test_.errln("Failed : Thai strings are in sorted increasing order "
                      + s + " < " + t);
    }
  }
  
  /**
  * Test odd corner conditions taken from "How to Sort Thai Without 
  * Rewriting Sort", by Doug Cooper, http://seasrc.th.net/paper/thaisort.zip
  * @exception thrown when error occurs 
  */
  public void TestOddCase() throws Exception
  {
    String tests[] = 
    {
      // Shorter words precede longer
      "\u0e01", "<", "\u0e01\u0e01",
      // Tone marks are considered after letters (i.e. are primary ignorable)
      "\u0e01\u0e32", "<", "\u0e01\u0e49\u0e32",
      // ditto for other over-marks
      "\u0e01\u0e32", "<", "\u0e01\u0e32\u0e4c",
      // commonly used mark-in-context order.
      // In effect, marks are sorted after each syllable.
      "\u0e01\u0e32\u0e01\u0e49\u0e32", "<", 
      "\u0e01\u0e48\u0e32\u0e01\u0e49\u0e32",
      // Hyphens and other punctuation follow whitespace but come before 
      // letters
      "\u0e01\u0e32", "<", "\u0e01\u0e32-",
      "\u0e01\u0e32-", "<", "\u0e01\u0e32\u0e01\u0e32",
      // Doubler follows an indentical word without the doubler
      "\u0e01\u0e32", "<", "\u0e01\u0e32\u0e46",
      "\u0e01\u0e32\u0e46", "<", "\u0e01\u0e32\u0e01\u0e32",
      // \u0e45 after either \u0e24 or \u0e26 is treated as a single
      // combining character, similar to "c < ch" in traditional spanish.
      "\u0e24\u0e29\u0e35", "<", "\u0e24\u0e45\u0e29\u0e35",
      "\u0e26\u0e29\u0e35", "<", "\u0e26\u0e45\u0e29\u0e35",
      // Vowels reorder, should compare \u0e2d and \u0e34
      "\u0e40\u0e01\u0e2d", "<", "\u0e40\u0e01\u0e34",
     // Tones are compared after the rest of the word 
     // (e.g. primary ignorable)
     "\u0e01\u0e32\u0e01\u0e48\u0e32", "<", "\u0e01\u0e49\u0e32\u0e01\u0e32",
     // Periods are ignored entirely
     "\u0e01.\u0e01.", "<", "\u0e01\u0e32"
    };
    
    for (int i = 0; i < tests.length; i += 3) 
    {
      int expect = 0;
      if (tests[i + 1].equals("<"))
        expect = Collator.RESULT_LESS;
      else 
        if (tests[i + 1].equals(">"))
          expect = Collator.RESULT_GREATER;
        else 
          if (tests[i + 1].equals("="))
            expect = Collator.RESULT_EQUAL;
          else 
          {
            // expect = Integer.decode(tests[i+1]).intValue();
            m_test_.errln("Failed : unknown operator " + tests[i + 1]);
            return;
          }

      String s1 = tests[i], 
             s2 = tests[i + 2];
      int result = m_collator_.compare(s1, s2);
      if (result != expect)
        m_test_.errln("Failed : " + s1 + tests[i + 1] + s2);
      else
      {
        // Collator.compare worked OK; now try the collation keys
        CollationKey k1 = m_collator_.getCollationKey(s1), 
                     k2 = m_collator_.getCollationKey(s2);
        result = k1.compareTo(k2);
        if (result != expect) 
          m_test_.errln("Failed : Collation key comparison of " + s1 + 
                        tests[i + 1] + s2);
      }   
    }
  }
  
  // private variables =============================================
  
  /**
  * RuleBasedCollator for testing
  */
  private Collator m_collator_;
  
  /**
  * Main Collation test program
  */
  private CollatorTest m_test_;
  
  /**
  * Source strings for testing
  */
  private static final String SOURCE_TEST_CASE_[] = 
  {
    "\u0e01\u0e24\u0e42\u0e28\u0e17\u0e23",
    "\u0e01\u0e24\u0e29\u0e0e\u0e32",
    "\u0e01\u0e24\u0e29\u0e0e\u0e32\u0e0d",
    "\u0e01\u0e24\u0e29\u0e0e\u0e32\u0e0d\u0e0a\u0e25\u0e34\u0e15",
    "\u0e01\u0e24\u0e29\u0e0e\u0e32\u0e0d\u0e0a\u0e25\u0e35",
    "\u0e01\u0e38\u0e25\u0e18\u0e23\u0e23\u0e21",
    "\u0e01\u0e38\u0e25\u0e18\u0e34\u0e14\u0e32",
    "\u0e01\u0e38\u0e25\u0e19\u0e32\u0e04",
    "\u0e01\u0e38\u0e25\u0e19\u0e32\u0e23\u0e35",
    "\u0e01\u0e38\u0e25\u0e19\u0e32\u0e28",
    "\u0e04\u0e25",
    "\u0e04\u0e25\u0e27\u0e07",
    "\u0e04\u0e25\u0e2d",
    "\u0e04\u0e25\u0e49\u0e2d",
    "\u0e04\u0e25\u0e2d\u0e01",
    "\u0e08\u0e49\u0e32\u0e25\u0e30\u0e2b\u0e27\u0e31\u0e48\u0e19",
    "\u0e08\u0e32\u0e27",
    "\u0e08\u0e48\u0e32\u0e27",
    "\u0e08\u0e49\u0e32\u0e27",
    "\u0e08\u0e48\u0e32\u0e2b\u0e27\u0e31\u0e01",
    "\u0e43\u0e0a\u0e48",
    "\u0e43\u0e0a\u0e49",
    "\u0e44\u0e0a",
    "\u0e44\u0e0a\u0e19\u0e30",
    "\u0e44\u0e0a\u0e22",
    "\u0e15\u0e31\u0e07\u0e42\u0e2d\u0e4b",
    "\u0e15\u0e31\u0e08\u0e09\u0e01",
    "\u0e15\u0e31\u0e08\u0e09\u0e19\u0e35",
    "\u0e15\u0e31\u0e13\u0e11\u0e38\u0e25",
    "\u0e15\u0e31\u0e13\u0e2b\u0e31\u0e01\u0e29\u0e31\u0e22",
    "\u0e40\u0e17\u0e34\u0e48\u0e07",
    "\u0e40\u0e17\u0e34\u0e07\u0e1a\u0e2d\u0e07",
    "\u0e40\u0e17\u0e34\u0e14",
    "\u0e40\u0e17\u0e34\u0e19",
    "\u0e40\u0e17\u0e34\u0e1a",
    "\u0e1a\u0e38\u0e01",
    "\u0e1a\u0e38\u0e04\u0e04\u0e25",
    "\u0e1a\u0e38\u0e04\u0e25\u0e32\u0e01\u0e23",
    "\u0e1a\u0e38\u0e04\u0e25\u0e32\u0e18\u0e34\u0e29\u0e10\u0e32\u0e19",
    "\u0e1a\u0e38\u0e04\u0e25\u0e34\u0e01",
    "\u0e1b\u0e31\u0e15\u0e16\u0e23",
    "\u0e1b\u0e31\u0e15\u0e16\u0e30",
    "\u0e1b\u0e31\u0e15\u0e19\u0e34",
    "\u0e1b\u0e31\u0e15\u0e19\u0e35",
    "\u0e1b\u0e31\u0e15\u0e22\u0e31\u0e22",
    "\u0e1e\u0e27\u0e19",
    "\u0e1e\u0e27\u0e22",
    "\u0e1e\u0e2a\u0e01",
    "\u0e1e\u0e2a\u0e19",
    "\u0e1e\u0e2a\u0e38",
    "\u0e21\u0e25\u0e48\u0e32\u0e27\u0e40\u0e21\u0e25\u0e32",
    "\u0e21\u0e25\u0e32\u0e2b\u0e23\u0e32",
    "\u0e21\u0e25\u0e34\u0e19",
    "\u0e21\u0e25\u0e34\u0e49\u0e19",
    "\u0e21\u0e25\u0e37\u0e48\u0e19",
    "\u0e40\u0e22\u0e0b\u0e39",
    "\u0e40\u0e22\u0e47\u0e14",
    "\u0e40\u0e22\u0e47\u0e19",
    "\u0e40\u0e22\u0e47\u0e19\u0e15\u0e32\u0e42\u0e1f",
    "\u0e40\u0e22\u0e47\u0e19\u0e40\u0e15\u0e32\u0e42\u0e1f",
    "\u0e25\u0e33\u0e40\u0e08\u0e35\u0e22\u0e01",
    "\u0e25\u0e33\u0e14\u0e27\u0e19",
    "\u0e25\u0e33\u0e14\u0e31\u0e1a",
    "\u0e25\u0e33\u0e19\u0e31\u0e01",
    "\u0e25\u0e33\u0e40\u0e19\u0e32",
    "\u0e44\u0e27\u0e11\u0e39\u0e23\u0e22\u0e4c",
    "\u0e44\u0e27\u0e17\u0e22\u0e4c",
    "\u0e44\u0e27\u0e1e\u0e08\u0e19\u0e4c",
    "\u0e44\u0e27\u0e22\u0e32\u0e01\u0e23\u0e13\u0e4c",
    "\u0e44\u0e27\u0e22\u0e32\u0e27\u0e31\u0e08\u0e01\u0e23",
    "\u0e2a\u0e31\u0e19\u0e1e\u0e23\u0e49\u0e32\u0e21\u0e2d\u0e0d",
    "\u0e2a\u0e31\u0e19\u0e1e\u0e23\u0e49\u0e32\u0e2b\u0e2d\u0e21",
    "\u0e2a\u0e31\u0e19\u0e23\u0e27\u0e07",
    "\u0e2a\u0e31\u0e19\u0e25\u0e36\u0e01",
    "\u0e2a\u0e31\u0e19\u0e2a\u0e01\u0e24\u0e15",
    "\u0e2b\u0e49\u0e27\u0e19",
    "\u0e2b\u0e27\u0e19\u0e04\u0e33\u0e19\u0e36\u0e07",
    "\u0e2b\u0e27\u0e22",
    "\u0e2b\u0e48\u0e27\u0e22",
    "\u0e2b\u0e49\u0e27\u0e22",
    "\u0e2d\u0e32\u0e19\u0e19",
    "\u0e2d\u0e32\u0e19\u0e19\u0e17\u0e4c",
    "\u0e2d\u0e32\u0e19\u0e30",
    "\u0e2d\u0e32\u0e19\u0e31\u0e19\u0e17\u0e4c",
    "\u0e2d\u0e32\u0e19\u0e31\u0e19\u0e17\u0e19\u0e30"
  };
}

