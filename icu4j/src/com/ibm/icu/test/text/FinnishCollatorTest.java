/**
*******************************************************************************
* Copyright (C) 1996-2000, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: 
*  /usr/cvs/icu4j/icu4j/src/com/ibm/icu/test/text/FinnishCollatorTest.java,v $ 
* $Date: 2001/03/09 00:42:45 $ 
* $Revision: 1.1 $
*
*******************************************************************************
*/

package com.ibm.icu4jni.test.text;

import java.util.Locale;
import com.ibm.icu4jni.text.Collator;
import com.ibm.icu4jni.text.CollationAttribute;

/**
* Testing class for Finnish collator
* Mostly following the test cases for ICU
* @author Syn Wee Quek
* @since jan 23 2001
*/
public final class FinnishCollatorTest 
{ 
  
  // constructor ===================================================
  
  /**
  * Constructor
  */
  public FinnishCollatorTest(CollatorTest testprogram) throws Exception
  {
    m_test_ = testprogram;
    m_collator_ = Collator.getInstance(new Locale("fi", "FI"));
  }
  
  // public methods ================================================

  /**
  * Test with primary collation strength
  * @exception thrown when error occurs while setting strength
  */
  public void TestPrimary() throws Exception
  {
    m_collator_.setStrength(CollationAttribute.VALUE_PRIMARY);
    for (int i = 4; i < 5; i ++)
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
    for (int i = 0; i < 4 ; i ++)
      m_test_.doTest(m_collator_, SOURCE_TEST_CASE_[i], TARGET_TEST_CASE_[i], 
                     EXPECTED_TEST_RESULT_[i]);
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
    "\u0077\u0061\u0074",
    "\u0076\u0061\u0074",
    "\u0061\u00FC\u0062\u0065\u0063\u006b",
    "\u004c\u00E5\u0076\u0069",
    "\u0077\u0061\u0074"
  };

  /**
  * Target strings for testing
  */
  private final String TARGET_TEST_CASE_[] = 
  {
    "\u0076\u0061\u0074",
    "\u0077\u0061\u0079",
    "\u0061\u0078\u0062\u0065\u0063\u006b",
    "\u004c\u00E4\u0077\u0065",
    "\u0076\u0061\u0074"
  };

  /**
  * Comparison result corresponding to above source and target cases
  */
  private final int EXPECTED_TEST_RESULT_[] = 
  {
    Collator.RESULT_GREATER,
    Collator.RESULT_LESS,
    Collator.RESULT_GREATER,
    Collator.RESULT_LESS,
    Collator.RESULT_EQUAL,                                        
  };
}

