/**
*******************************************************************************
* Copyright (C) 1996-2000, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: 
*  /usr/cvs/icu4j/icu4j/src/com/ibm/icu/test/text/CollationElementIteratorTest.java,v $ 
* $Date: 2001/03/09 23:41:46 $ 
* $Revision: 1.2 $
*
*******************************************************************************
*/

package com.ibm.icu4jni.test.text;

import java.util.Locale;
import java.text.ParseException;
import com.ibm.text.UCharacter;
import com.ibm.icu4jni.test.TestFmwk;
import com.ibm.icu4jni.text.Collator;
import com.ibm.icu4jni.text.RuleBasedCollator;
import com.ibm.icu4jni.text.CollationKey;
import com.ibm.icu4jni.text.CollationElementIterator;

/**
* Testing class for Finnish collator
* Mostly following the test cases for ICU
* @author Syn Wee Quek
* @since jan 23 2001
*/
public final class CollationElementIteratorTest extends TestFmwk
{ 
  
  // constructor ===================================================
  
  /**
  * Constructor
  */
  public CollationElementIteratorTest()
  {
    m_collator_ = (RuleBasedCollator)Collator.getInstance(Locale.US);
  }
  
  // public methods ================================================

  /**
  * Test for CollationElementIterator previous and next for the whole set of
  * unicode characters.
  */
  public void TestUnicodeChar()
  {
    for (char codepoint = 1; codepoint < 0xFFFE;)
    {
      StringBuffer test = new StringBuffer(0xFF);   
      while (codepoint % 0xFF != 0) 
      {
        if (UCharacter.isDefined(codepoint))
          test.append(codepoint);
        codepoint ++;
      }

      if (UCharacter.isDefined(codepoint))
        test.append(codepoint);
      
      if (codepoint != 0xFFFF)
        codepoint ++;

      CollationElementIterator iter = 
                    m_collator_.getCollationElementIterator(test.toString());
      
      // A basic test to see if it's working at all
      previousNext(iter);
    }
  }
  
  /**
  * Testing previous method
  * @exception thrown when error occurs while setting strength
  */
  public void TestPrevious()
  {
    CollationElementIterator iterator = 
                       m_collator_.getCollationElementIterator(TEST_CASE_1_);

    // A basic test to see if it's working at all
    previousNext(iterator);
    
    try
    {
      // Test with a contracting character sequence
      RuleBasedCollator collator = new RuleBasedCollator(
                                "&a,A < b,B < c,C, d,D < z,Z < ch,cH,Ch,CH");

      iterator = collator.getCollationElementIterator("abchdcba");
      previousNext(iterator);
      
      // Test with an expanding character sequence
      collator = new RuleBasedCollator("&a < b < c/abd < d");

      iterator = collator.getCollationElementIterator("abcd");
      previousNext(iterator);
      
      // Now try both
      collator = new RuleBasedCollator("&a < b < c/aba < d < z < ch");
            
      String source = "abcdbchdc";
      iterator = collator.getCollationElementIterator(source);
       previousNext(iterator);
      
      source= "\u0e41\u0e02\u0e27abc";
      
      collator = (RuleBasedCollator)Collator.getInstance(new Locale("th", 
                                                                      "TH"));
      
      iterator = collator.getCollationElementIterator(source);
      previousNext(iterator);
      
      collator = (RuleBasedCollator)Collator.getInstance();
      
      iterator = collator.getCollationElementIterator(source);
      previousNext(iterator);
    }
    catch (ParseException e)
    {
      errln("Failed : Rule parse error " + e.getMessage());
    }
  }
  
  /**
  * Test for getOffset() and setOffset()
  * @exception thrown when error occurs
  */
  public void TestOffset()
  {
    CollationElementIterator iter = 
                       m_collator_.getCollationElementIterator(TEST_CASE_1_);

    // Run all the way through the iterator, then get the offset
    int orders[] = getOrders(iter);
    int offset = iter.getOffset();

    if (offset != TEST_CASE_1_.length())
      errln("Failed : Collation element iterator offset is not " + 
                    "equals to " + TEST_CASE_1_.length());

    // Now set the offset back to the beginning and see if it works
    CollationElementIterator iter2 = 
                       m_collator_.getCollationElementIterator(TEST_CASE_1_);           
      
    iter.setOffset(0);

    for (int i = 0; i < offset; i ++)
      if (iter.next() != iter2.next())
      {
        errln("Failed : Offset reset should revert collation element"
                      + " to the previous state");
        break;
      }
  }
  
  /**
  * Text setting test
  * @exception thrown when error occurs
  */
  public void TestSetText()
  {
    CollationElementIterator iter1 = 
                      m_collator_.getCollationElementIterator(TEST_CASE_1_);
    CollationElementIterator iter2 = 
                      m_collator_.getCollationElementIterator(TEST_CASE_2_);
    
    // Run through the second iterator just to exercise it
    int c = iter2.next();
    for (int i = 0;  
           ++ i < 10 && c != CollationElementIterator.NULLORDER;)
      c = iter2.next();
      
    // Now set it to point to the same string as the first iterator
    iter2.setText(TEST_CASE_1_);

    c = 1;
    for (int i = 0; c != CollationElementIterator.NULLORDER; i ++)
    {
      c = iter1.next();
      if (c != iter2.next())
      {
        errln("Failed : Collation element iterator should be equal " 
                      + "if the text string is the same");
        break;
      }
    } 
    
    iter1.reset();
  }

  /**  
  * Test for getMaxExpansion() @bug 4108762
  * @exception thrown when error occurs
  */
  public void TestMaxExpansion()
  {
    String rule = "&a < ab < c/aba < d < z < ch";
    try
    {
      RuleBasedCollator coll = new RuleBasedCollator(rule);
      CollationElementIterator iter = coll.getCollationElementIterator("a");
      char ch = 1;
      
      while (ch < 0xFFFF) {
        int count = 1;
        int order = 0;
        ch ++;
        iter.setText(String.valueOf(ch));
        order = iter.previous();

        /* thai management */
        if (order == 0)
          order = iter.previous();

        while (iter.previous() != CollationElementIterator.NULLORDER) {
          count ++; 
        }

        if (iter.getMaxExpansion(order) < count) {
          errln("Failed : Maximum expansion count for 0x" + 
                Integer.toHexString(order) + " < counted size " + 
                Integer.toHexString(count));
        }
      }
    } catch (ParseException e)
    {
      errln("Failed : creation of RuleBasedCollator for rules " +
            rule);
    }
  }
  
  // private variables =============================================
  
  /**
  * RuleBasedCollator for testing
  */
  private RuleBasedCollator m_collator_;
  
  /**
  * Source strings for testing
  */
  private final String TEST_CASE_1_ = 
                                   "What subset of all possible test cases?";
  private final String TEST_CASE_2_ = 
                                  "has the highest probability of detecting";
  
  /*
  * Maximum size used in arrays
  */
  private final int MAX_SIZE_ = 100;
                                  
  // private methods --------------------------------------------------
  
  /**
  * Testing if next and prev works.
  * @param iter collation element iterator to be tested
  */
  private void previousNext(CollationElementIterator iter)
  {
    // Run through the iterator forwards and stick it into an array
    int orders[] = getOrders(iter);
    
    // Now go through it backwards and make sure we get the same values
    iter.reset();
    int index = orders.length;
    int order = iter.previous();
    while (order != CollationElementIterator.NULLORDER)
    {
      if (order != orders[--index]) {
        if (order == 0) {
          index ++;
        }
        else {
          while (index > 0 && orders[-- index] == 0)
          {
          }
          if (order != orders[index])
          {
            errln("Fail : CollationElementIterator previous element " 
                      + "expected to be " + 
                      Integer.toHexString(orders[index]) + " not " +
                      Integer.toHexString(order));
            break;
          }
        }
      }
      order = iter.previous();
    }

    while (index != 0 && orders[index - 1] == 0) {
      index --;
    }
    
    if (index != 0)
      errln("Fail : CollationElementIterator has more previous " + 
                    "elements than next");
  }
  
  /**
  * Return an integer array containing all of the collation orders.
  * returned by calls to next on the specified iterator
  * @param iter collation element iterator
  * @return list of all collation order in collation element iterator
  */
  private int[] getOrders(CollationElementIterator iter)
  {
    int size = 0;
    int maxsize = MAX_SIZE_;
    int result[] = new int[MAX_SIZE_];
    
    int order = iter.next();
    while (order != CollationElementIterator.NULLORDER)
    {
      if (size == maxsize)
      {
        maxsize = maxsize << 1;
        int temp[] = new int[maxsize];
        System.arraycopy(result, 0, temp, 0, size);
        result = temp;
      }

      result[size ++] = order;
      order = iter.next();
    }

    if (maxsize > size)
    {
      int temp[] = new int[size];
      System.arraycopy(result, 0, temp, 0, size);
      result = temp;
    }

    return result;
  }
}

