/**
*******************************************************************************
* Copyright (C) 1996-2000, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: 
*  /usr/cvs/icu4j/icu4j/src/com/ibm/icu/test/text/SpanishCollatorTest.java,v $ 
* $Date: 2001/03/09 23:41:46 $ 
* $Revision: 1.2 $
*
*******************************************************************************
*/

package com.ibm.icu4jni.test.text;

import java.util.Locale;
import com.ibm.icu4jni.text.Collator;
import com.ibm.icu4jni.text.CollationKey;
import com.ibm.icu4jni.text.CollationAttribute;

/**
* Testing class for Spanish collator
* Mostly following the test cases for ICU
* @author Syn Wee Quek
* @since jan 23 2001
*/
public final class SpanishCollatorTest 
{ 
  
  // constructor ===================================================
  
  /**
  * Constructor
  */
  public SpanishCollatorTest(CollatorTest testprogram) throws Exception
  {
    m_test_ = testprogram;
    m_collator_ = Collator.getInstance(new Locale("es", "ES"));
  }
  
  // public methods ================================================

  /**
  * Test with primary collation strength
  * @exception thrown when error occurs while setting strength
  */
  public void TestPrimary() throws Exception
  {
    m_collator_.setStrength(CollationAttribute.VALUE_PRIMARY);
    for (int i = 5; i < 9; i ++)
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
    "\u0061\u006c\u0069\u0061\u0073",
    "\u0045\u006c\u006c\u0069\u006f\u0074",
    "\u0048\u0065\u006c\u006c\u006f",
    "\u0061\u0063\u0048\u0063",
    "\u0061\u0063\u0063",
    "\u0061\u006c\u0069\u0061\u0073",
    "\u0061\u0063\u0048\u0063",
    "\u0061\u0063\u0063",
    "\u0048\u0065\u006c\u006c\u006f"
  };

  /**
  * Target strings for testing
  */
  private final String TARGET_TEST_CASE_[] = 
  {
    "\u0061\u006c\u006c\u0069\u0061\u0073",
    "\u0045\u006d\u0069\u006f\u0074",
    "\u0068\u0065\u006c\u006c\u004f",
    "\u0061\u0043\u0048\u0063",
    "\u0061\u0043\u0048\u0063",
    "\u0061\u006c\u006c\u0069\u0061\u0073",
    "\u0061\u0043\u0048\u0063",
    "\u0061\u0043\u0048\u0063",
    "\u0068\u0065\u006c\u006c\u004f"
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
    Collator.RESULT_LESS,
    Collator.RESULT_EQUAL,
    Collator.RESULT_LESS,
    Collator.RESULT_EQUAL                                               
  };
}

