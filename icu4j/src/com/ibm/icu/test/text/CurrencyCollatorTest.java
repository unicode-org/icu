/**
*******************************************************************************
* Copyright (C) 1996-2000, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: 
*  /usr/cvs/icu4j/icu4j/src/com/ibm/icu/test/text/CurrencyCollatorTest.java,v $ 
* $Date: 2001/03/09 23:41:46 $ 
* $Revision: 1.2 $
*
*******************************************************************************
*/

package com.ibm.icu4jni.test.text;

import com.ibm.icu4jni.text.Collator;
import com.ibm.icu4jni.text.CollationKey;
import com.ibm.icu4jni.text.CollationAttribute;

/**
* Testing class for currency collator
* Mostly following the test cases for ICU
* @author Syn Wee Quek
* @since jan 23 2001
*/
public final class CurrencyCollatorTest 
{ 
  
  // constructor ===================================================
  
  /**
  * Constructor
  */
  public CurrencyCollatorTest(CollatorTest testprogram) throws Exception
  {
    m_test_ = testprogram;
    m_collator_ = Collator.getInstance();
  }
  
  // public methods ================================================

  /**
  * Test with primary collation strength
  * @exception thrown when error occurs while setting strength
  */
  public void TestCurrency() throws Exception
  {
    int expectedresult;
    int size = m_currency_.length;
    
    String source;
    String target;
    
    // Compare each currency symbol against all the currency symbols, 
    // including itself
    for (int i = 0; i < size; i ++)
    {
      source = m_currency_[i];
      for (int j = 0; j < size; j ++)
      {
        target = m_currency_[j];
        if (i < j)
          expectedresult = Collator.RESULT_LESS;
        else 
          if ( i == j)
            expectedresult = Collator.RESULT_EQUAL;
          else
            expectedresult = Collator.RESULT_GREATER;
            
         CollationKey skey = m_collator_.getCollationKey(source),
                      tkey = m_collator_.getCollationKey(target);

         if (skey.compareTo(tkey) != expectedresult)
         {
           m_test_.errln("Fail : Collation keys for " + source + " and " +
                         target + " expected to be " + expectedresult);
           return;
         }
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
  * Test data in ascending collation order
  */
  private final String m_currency_[] = {"\u00a4", // generic currency
                                        "\u0e3f", // baht
                                        "\u00a2", // cent
                                        "\u20a1", // colon
                                        "\u20a2", // cruzeiro
                                        "\u0024", // dollar
                                        "\u20ab", // dong
                                        "\u20ac", // euro
                                        "\u20a3", // franc
                                        "\u20a4", // lira
                                        "\u20a5", // mill
                                        "\u20a6", // naira
                                        "\u20a7", // peseta
                                        "\u00a3", // pound
                                        "\u20a8", // rupee
                                        "\u20aa", // shekel
                                        "\u20a9", // won
                                        "\u00a5"  // yen
                                       };
}

