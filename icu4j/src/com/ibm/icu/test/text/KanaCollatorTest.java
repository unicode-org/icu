/**
*******************************************************************************
* Copyright (C) 1996-2000, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: 
*  /usr/cvs/icu4j/icu4j/src/com/ibm/icu/test/text/KanaCollatorTest.java,v $ 
* $Date: 2001/03/09 23:41:46 $ 
* $Revision: 1.2 $
*
*******************************************************************************
*/

package com.ibm.icu4jni.test.text;

import java.util.Locale;
import com.ibm.icu4jni.text.Collator;
import com.ibm.icu4jni.text.CollationAttribute;
import com.ibm.icu4jni.text.NormalizationMode;

/**
* Testing class for Kana collator
* Mostly following the test cases for ICU
* @author Syn Wee Quek
* @since jan 23 2001
*/
public final class KanaCollatorTest 
{ 
  
  // constructor ===================================================
  
  /**
  * Constructor
  */
  public KanaCollatorTest(CollatorTest testprogram) throws Exception
  {
    m_test_ = testprogram;
    m_collator_ = Collator.getInstance(Locale.JAPAN);
    m_collator_.setDecomposition(NormalizationMode.DECOMP_CAN);
  }
  
  // public methods ================================================

  /**
  * Test with tertiary collation strength
  * @exception thrown when error occurs while setting strength
  */
  public void TestTertiary() throws Exception
  {
    m_collator_.setStrength(CollationAttribute.VALUE_TERTIARY);
    for (int i = 0; i < SOURCE_TEST_CASE_.length ; i ++)
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
    "\u0041\u0300\u0301",
    "\u0041\u0300\u0316",
    "\u0041\u0300",
    "\u00C0\u0301",
    "\u00C0\u0316",
    "\uff9E",
    "\u3042",
    "\u30A2",
    "\u3042\u3042",
    "\u30A2\u30FC",
    "\u30A2\u30FC\u30C8" 
  };

  /**
  * Target strings for testing
  */
  private final String TARGET_TEST_CASE_[] = 
  {
    "\u0041\u0301\u0300",
    "\u0041\u0316\u0300",
    "\u00C0",
    "\u0041\u0301\u0300",
    "\u0041\u0316\u0300",
    "\uFF9F",
    "\u30A2",
    "\u3042\u3042",
    "\u30A2\u30FC",
    "\u30A2\u30FC\u30C8",
    "\u3042\u3042\u3089" 
  };

  /**
  * Comparison result corresponding to above source and target cases
  */
  private final int EXPECTED_TEST_RESULT_[] = 
  {
    Collator.RESULT_GREATER,
    Collator.RESULT_EQUAL,
    Collator.RESULT_EQUAL,
    Collator.RESULT_GREATER,
    Collator.RESULT_EQUAL,
    Collator.RESULT_LESS,
    Collator.RESULT_GREATER,
    Collator.RESULT_LESS,
    Collator.RESULT_GREATER,
    Collator.RESULT_LESS,
    Collator.RESULT_LESS                                                      
  };
}

