/**
*******************************************************************************
* Copyright (C) 1996-2000, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: 
*  /usr/cvs/icu4j/icu4j/src/com/ibm/icu/test/text/FrenchCollatorTest.java,v $ 
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
* Testing class for french collator
* Mostly following the test cases for ICU
* @author Syn Wee Quek
* @since jan 25 2001
*/
public final class FrenchCollatorTest 
{ 
  
  // constructor ===================================================
  
  /**
  * Constructor
  */
  public FrenchCollatorTest(CollatorTest testprogram) throws Exception
  {
    m_test_ = testprogram;
    m_collator_ = Collator.getInstance(Locale.FRENCH);
  }
  
  // public methods ================================================

  /**
  * Test defined bugs
  * @exception thrown when error occurs while setting strength
  */
  public void TestBugs() throws Exception
  {
    int jsize = BUGS_TEST_CASE_.length;
    int isize = jsize - 1;
    m_collator_.setStrength(CollationAttribute.VALUE_TERTIARY);
    for (int i = 0; i < isize; i ++)
      for (int j = i + 1; j < jsize; j ++)
        m_test_.doTest(m_collator_, BUGS_TEST_CASE_[i], BUGS_TEST_CASE_[j], 
                       Collator.RESULT_LESS);
  }

  /**
  * Test with secondary collation strength
  * @exception thrown when error occurs while setting strength
  */
  public void TestSecondary() throws Exception
  {
    //test acute and grave ordering
    int expected;
    m_collator_.setStrength(CollationAttribute.VALUE_SECONDARY);
    int size = ACUTE_TEST_CASE_.length / (ACUTE_TEST_CASE_[0].length());
    for (int i = 0; i < size; i ++)
      for (int j = 0; j < size; j ++)
      {
        if (i <  j)
          expected = Collator.RESULT_LESS;
        else 
          if (i == j)
           expected = Collator.RESULT_EQUAL;
          else 
            expected = Collator.RESULT_GREATER;
        m_test_.doTest(m_collator_, ACUTE_TEST_CASE_[i], ACUTE_TEST_CASE_[j], 
                       expected);
      }
  }
  
  /**
  * Test with tertiary collation strength
  * @exception thrown when error occurs while setting strength
  */
  public void TestTertiary() throws Exception
  { 
    m_collator_.setStrength(CollationAttribute.VALUE_TERTIARY);
    int size = SOURCE_TEST_CASE_.length;
    for (int i = 0; i < size; i ++)
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
    "\u0061\u0062\u0063",
    "\u0043\u004f\u0054\u0045",
    "\u0063\u006f\u002d\u006f\u0070",
    "\u0070\u00EA\u0063\u0068\u0065",
    "\u0070\u00EA\u0063\u0068\u0065\u0072",
    "\u0070\u00E9\u0063\u0068\u0065\u0072",
    "\u0070\u00E9\u0063\u0068\u0065\u0072",
    "\u0048\u0065\u006c\u006c\u006f",
    "\u01f1",
    "\ufb00",
    "\u01fa",
    "\u0101"
  };

  /**
  * Target strings for testing
  */
  private final String TARGET_TEST_CASE_[] = 
  {
    "\u0041\u0042\u0043",
    "\00u63\u00f4\u0074\u0065",
    "\u0043\u004f\u004f\u0050",
    "\u0070\u00E9\u0063\u0068\u00E9",
    "\u0070\u00E9\u0063\u0068\u00E9",
    "\u0070\u00EA\u0063\u0068\u0065",
    "\u0070\u00EA\u0063\u0068\u0065\u0072",
    "\u0068\u0065\u006c\u006c\u004f",
    "\u01ee",
    "\u25ca",
    "\u00e0",
    "\u01df"
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
    Collator.RESULT_GREATER,
    Collator.RESULT_GREATER,
    Collator.RESULT_LESS,
    Collator.RESULT_GREATER,
    Collator.RESULT_GREATER,
    Collator.RESULT_GREATER,
    Collator.RESULT_LESS,
    Collator.RESULT_LESS  
  };

  /**
  * Bug testing data set
  */
  private final String BUGS_TEST_CASE_[] = 
  {
    "\u0061",
    "\u0041",
    "\u0065",
    "\u0045",
    "\u00e9",
    "\u00e8",
    "\u00ea",
    "\u00eb",
    "\u0065\u0061",
    "\u0078"
  };

  /**
  * \u000300 is grave\u000301 is acute.
  * the order of elements in this array must be different than the order in 
  * CollationFrenchTest.
  * Data set for testing accents.
  */
  private final String ACUTE_TEST_CASE_[] = 
  {
    "\u0065\u0065", 
    "\u0065\u0301\u0065",
    "\u0065\u0300\u0301\u0065",
    "\u0065\u0300\u0065",
    "\u0065\u0301\u0300\u0065",
    "\u0065\u0065\u0301", 
    "\u0065\u0301\u0065\u0301",
    "\u0065\u0300\u0301\u0065\u0301",
    "\u0065\u0300\u0065\u0301",
    "\u0065\u0301\u0300\u0065\u0301",
    "\u0065\u0065\u0300\u0301",
    "\u0065\u0301\u0065\u0300\u0301",
    "\u0065\u0300\u0301\u0065\u0300\u0301",
    "\u0065\u0300\u0065\u0300\u0301",
    "\u0065\u0301\u0300\u0065\u0300\u0301",
    "\u0065\u0065\u0300",
    "\u0065\u0301\u0065\u0300",
    "\u0065\u0300\u0301\u0065\u0300",
    "\u0065\u0300\u0065\u0300",
    "\u0065\u0301\u0300\u0065\u0300",
    "\u0065\u0065\u0301\u0300",
    "\u0065\u0301\u0065\u0301\u0300",
    "\u0065\u0300\u0301\u0065\u0301\u0300",
    "\u0065\u0300\u0065\u0301\u0300",
    "\u0065\u0301\u0300\u0065\u0301\u0300"
  };
}

