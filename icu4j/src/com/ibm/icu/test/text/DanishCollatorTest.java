/**
*******************************************************************************
* Copyright (C) 1996-2000, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: 
*  /usr/cvs/icu4j/icu4j/src/com/ibm/icu/test/text/DanishCollatorTest.java,v $ 
* $Date: 2001/03/09 23:41:46 $ 
* $Revision: 1.2 $
*
*******************************************************************************
*/

package com.ibm.icu4jni.test.text;

import java.util.Locale;
import com.ibm.icu4jni.text.Collator;
import com.ibm.icu4jni.text.CollationAttribute;

/**
* Testing class for danish collator
* Mostly following the test cases for ICU
* @author Syn Wee Quek
* @since jan 23 2001
*/
public final class DanishCollatorTest 
{ 
  
  // constructor ===================================================
  
  /**
  * Constructor
  */
  public DanishCollatorTest(CollatorTest testprogram) throws Exception
  {
    m_test_ = testprogram;
    m_collator_ = Collator.getInstance(new Locale("da", "DK"));
  }
  
  // public methods ================================================

  /**
  * Test with primary collation strength
  * @exception thrown when error occurs while setting strength
  */
  public void TestPrimary() throws Exception
  {
    m_collator_.setStrength(CollationAttribute.VALUE_PRIMARY);
    for (int i = 5; i < 8; i ++)
      m_test_.doTest(m_collator_, SOURCE_TEST_CASE_[i], TARGET_TEST_CASE_[i], 
                     EXPECTED_TEST_RESULT_[i]);
  }

  /**
  * Test with tertiary collation strength
  * @exception thrown when error occurs while setting strength
  */
  public void TestTertiary() throws Exception
  {
    m_collator_.setStrength(CollationAttribute.VALUE_TERTIARY);
    for (int i = 0; i < 5 ; i ++)
      m_test_.doTest(m_collator_, SOURCE_TEST_CASE_[i], TARGET_TEST_CASE_[i], 
                     EXPECTED_TEST_RESULT_[i]);
    
    for (int i = 0; i < 53; i ++)
      for (int j = i + 1; j < 54; j ++)
        m_test_.doTest(m_collator_, BUGS_TEST_CASE_[i], BUGS_TEST_CASE_[i], 
                       Collator.RESULT_LESS);
        
    for (int i = 0; i < 52; i ++)
      for (int j = i + 1; j < 53; j ++)
        m_test_.doTest(m_collator_, NT_TEST_CASE_[i], 
                       NT_TEST_CASE_[j], Collator.RESULT_LESS);
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
    "\u004c\u0075\u0063",
    "\u006c\u0075\u0063\u006b",
    "\u004c\u00FC\u0062\u0065\u0063\u006b",
    "\u004c\u00E4\u0076\u0069",
    "\u004c\u00F6\u0077\u0077",
    "\u004c\u0076\u0069",
    "\u004c\u00E4\u0076\u0069",
    "\u004c\u0000FC\u0062\u0065\u0063\u006b"
  };

  /**
  * Target strings for testing
  */
  private final String TARGET_TEST_CASE_[] = 
  {
    "\u006c\u0075\u0063\u006b",
    "\u004c\u00FC\u0062\u0065\u0063\u006b",
    "\u006c\u0079\u0062\u0065\u0063\u006b",
    "\u004c\u00F6\u0077\u0065",
    "\u006d\u0061\u0073\u0074",
    "\u004c\u0077\u0069",
    "\u004c\u00F6\u0077\u0069",
    "\u004c\u0079\u0062\u0065\u0063\u006b"
  };

  /**
  * Comparison result corresponding to above source and target cases
  */
  private final int EXPECTED_TEST_RESULT_[] = 
  {
    Collator.RESULT_LESS,
    Collator.RESULT_LESS,
    Collator.RESULT_GREATER,
    Collator.RESULT_LESS,
    Collator.RESULT_LESS,
    Collator.RESULT_EQUAL,
    Collator.RESULT_LESS,
    Collator.RESULT_EQUAL                                                           
  };

  /**
  * Bug testing data set.
  * Internet data list.
  */
  private final String BUGS_TEST_CASE_[] = 
  {
    "\u0041\u002f\u0053",
    "\u0041\u004e\u0044\u0052\u0045",
    "\u0041\u004e\u0044\u0052\u00C9",
    "\u0041\u004e\u0044\u0052\u0045\u0041\u0053",
    "\u0041\u0053",
    "\u0043\u0041",
    "\u00C7\u0041",
    "\u0043\u0042",
    "\u00C7\u0043",
    "\u0044\u002e\u0053\u002e\u0042\u002e",
    "\u0044\u0041",                                                                           
    "\u0044\u0042",
    "\u0044\u0053\u0042",
    "\u0044\u0053\u0043",
    "\u0045\u004b\u0053\u0054\u0052\u0041\u005f\u0041\u0052\u0042\u0045\u004a\u0044\u0045",
    "\u0045\u004b\u0053\u0054\u0052\u0041\u0042\u0055\u0044",
    "\u0048\u00D8\u0053\u0054",  // could the \u00D8 be \u2205?
    "\u0048\u0041\u0041\u0047",                                                                 
    "\u0048\u00C5\u004e\u0044\u0042\u004f\u0047",
    "\u0048\u0041\u0041\u004e\u0044\u0056\u0000C6\u0052\u004b\u0053\u0042\u0041\u004e\u004b\u0045\u004e",
    "\u006b\u0061\u0072\u006c",
    "\u004b\u0061\u0072\u006c",
    "\u004e\u0049\u0045\u004c\u0053\u0045\u004e",
    "\u004e\u0049\u0045\u004c\u0053\u0020\u004a\u00D8\u0052\u0047\u0045\u004e",
    "\u004e\u0049\u0045\u004c\u0053\u002d\u004a\u00D8\u0052\u0047\u0045\u004e",
    "\u0052\u00C9\u0045\u002c\u0020\u0041",
    "\u0052\u0045\u0045\u002c\u0020\u0042",
    "\u0052\u00C9\u0045\u002c\u0020\u004c",                                                    
    "\u0052\u0045\u0045\u002c\u0020\u0056",
    "\u0053\u0043\u0048\u0059\u0054\u0054\u002c\u0020\u0042",
    "\u0053\u0043\u0048\u0059\u0054\u0054\u002c\u0020\u0048",
    "\u0053\u0043\u0048\u00DC\u0054\u0054\u002c\u0020\u0048",
    "\u0053\u0043\u0048\u0059\u0054\u0054\u002c\u0020\u004c",
    "\u0053\u0043\u0048\u00DC\u0054\u0054\u002c\u0020\u004d",
    "\u0053\u0053",
    "\u00DF",
    "\u0053\u0053\u0041",
    "\u0053\u0054\u004f\u0052\u0045\u004b\u00C6\u0052",
    "\u0053\u0054\u004f\u0052\u0045\u0020\u0056\u0049\u004c\u0044\u004d\u004f\u0053\u0045",               
    "\u0053\u0054\u004f\u0052\u004d\u004c\u0059",
    "\u0053\u0054\u004f\u0052\u004d\u0020\u0050\u0045\u0054\u0045\u0052\u0053\u0045\u004e",
    "\u0054\u0048\u004f\u0052\u0056\u0041\u004c\u0044",
    "\u0054\u0048\u004f\u0052\u0056\u0041\u0052\u0044\u0055\u0052",
    "\u00FE\u004f\u0052\u0056\u0041\u0052\u0110\u0055\u0052",
    "\u0054\u0048\u0059\u0047\u0045\u0053\u0045\u004e",
    "\u0056\u0045\u0053\u0054\u0045\u0052\u0047\u00C5\u0052\u0044\u002c\u0020\u0041",
    "\u0056\u0045\u0053\u0054\u0045\u0052\u0047\u0041\u0041\u0052\u0044\u002c\u0020\u0041",
    "\u0056\u0045\u0053\u0054\u0045\u0052\u0047\u00C5\u0052\u0044\u002c\u0020\u0042",                 
    "\u00C6\u0042\u004c\u0045",
    "\u00C4\u0042\u004c\u0045",
    "\u00D8\u0042\u0045\u0052\u0047",
    "\u00D6\u0042\u0045\u0052\u0047",
    "\u0110\u0041",
    "\u0110\u0043"
  };

  /**
  * Data set for testing.
  * NT data list
  */
  private final String NT_TEST_CASE_[] = 
  {
    "\u0061\u006e\u0064\u0065\u0072\u0065",
    "\u0063\u0068\u0061\u0071\u0075\u0065",
    "\u0063\u0068\u0065\u006d\u0069\u006e",
    "\u0063\u006f\u0074\u0065",
    "\u0063\u006f\u0074\u00e9",
    "\u0063\u00f4\u0074\u0065",
    "\u0063\u00f4\u0074\u00e9",
    "\u010d\u0075\u010d\u0113\u0074",
    "\u0043\u007a\u0065\u0063\u0068",
    "\u0068\u0069\u0161\u0061",
    "\u0069\u0072\u0064\u0069\u0073\u0063\u0068",
    "\u006c\u0069\u0065",
    "\u006c\u0069\u0072\u0065",
    "\u006c\u006c\u0061\u006d\u0061",
    "\u006c\u00f5\u0075\u0067",
    "\u006c\u00f2\u007a\u0061",
    "\u006c\u0075\u010d",                                
    "\u006c\u0075\u0063\u006b",
    "\u004c\u00fc\u0062\u0065\u0063\u006b",
    "\u006c\u0079\u0065",                               
    "\u006c\u00e4\u0076\u0069",
    "\u004c\u00f6\u0077\u0065\u006e",
    "\u006d\u00e0\u0161\u0074\u0061",
    "\u006d\u00ee\u0072",
    "\u006d\u0079\u006e\u0064\u0069\u0067",
    "\u004d\u00e4\u006e\u006e\u0065\u0072",
    "\u006d\u00f6\u0063\u0068\u0074\u0065\u006e",
    "\u0070\u0069\u00f1\u0061",
    "\u0070\u0069\u006e\u0074",
    "\u0070\u0079\u006c\u006f\u006e",
    "\u0161\u00e0\u0072\u0061\u006e",
    "\u0073\u0061\u0076\u006f\u0069\u0072",
    "\u0160\u0065\u0072\u0062\u016b\u0072\u0061",
    "\u0053\u0069\u0065\u0074\u006c\u0061",
    "\u015b\u006c\u0075\u0062",
    "\u0073\u0075\u0062\u0074\u006c\u0065",
    "\u0073\u0079\u006d\u0062\u006f\u006c",
    "\u0073\u00e4\u006d\u0074\u006c\u0069\u0063\u0068",
    "\u0077\u0061\u0066\u0066\u006c\u0065",
    "\u0076\u0065\u0072\u006b\u0065\u0068\u0072\u0074",
    "\u0077\u006f\u006f\u0064",
    "\u0076\u006f\u0078",                                 
    "\u0076\u00e4\u0067\u0061",
    "\u0079\u0065\u006e",
    "\u0079\u0075\u0061\u006e",
    "\u0079\u0075\u0063\u0063\u0061",
    "\u017e\u0061\u006c",
    "\u017e\u0065\u006e\u0061",
    "\u017d\u0065\u006e\u0113\u0076\u0061",
    "\u007a\u006f\u006f",
    "\u005a\u0076\u0069\u0065\u0064\u0072\u0069\u006a\u0061",
    "\u005a\u00fc\u0072\u0069\u0063\u0068",
    "\u007a\u0079\u0073\u006b",             
    "\u00e4\u006e\u0064\u0065\u0072\u0065"
  };
}

