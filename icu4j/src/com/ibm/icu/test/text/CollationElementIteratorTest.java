/**
*******************************************************************************
* Copyright (C) 1996-2000, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: 
*  /usr/cvs/icu4j/icu4j/src/com/ibm/icu/test/text/CollationElementIteratorTest.java,v $ 
* $Date: 2001/03/09 00:42:46 $ 
* $Revision: 1.1 $
*
*******************************************************************************
*/

package com.ibm.icu4jni.test.text;

import java.util.Locale;
import java.text.ParseException;
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
public final class CollationElementIteratorTest 
{ 
  
  // constructor ===================================================
  
  /**
  * Constructor
  */
  public CollationElementIteratorTest(CollatorTest testprogram)
  {
    m_test_ = testprogram;
    m_collator_ = (RuleBasedCollator)Collator.getInstance(Locale.US);
  }
  
  // public methods ================================================

  /**
  * Testing previous method
  * @exception thrown when error occurs while setting strength
  */
  public void TestPrevious()
  {
    CollationElementIterator iterator = 
                       m_collator_.getCollationElementIterator(TEST_CASE_1_);

    // A basic test to see if it's working at all
    testPreviousNext(iterator);
    
    try
    {
      // Test with a contracting character sequence
      RuleBasedCollator collator = new RuleBasedCollator(
                                "< a,A < b,B < c,C, d,D < z,Z < ch,cH,Ch,CH");

      iterator = collator.getCollationElementIterator("abchdcba");
      testPreviousNext(iterator);
      
      // Test with an expanding character sequence
      collator = new RuleBasedCollator("< a < b < c/abd < d");

      iterator = collator.getCollationElementIterator("abcd");
      testPreviousNext(iterator);
      
      // Now try both
      collator = new RuleBasedCollator("< a < b < c/aba < d < z < ch");

      String source = "abcdbchdc";
      iterator = collator.getCollationElementIterator(source);
      testPreviousNext(iterator);
      
      source= "\u0e41\u0e02\u0e27abc";
      
      collator = (RuleBasedCollator)Collator.getInstance(new Locale("th", 
                                                                      "TH"));
      
      iterator = collator.getCollationElementIterator(source);
      testPreviousNext(iterator);
      
      collator = (RuleBasedCollator)Collator.getInstance();
      
      iterator = collator.getCollationElementIterator(source);
      testPreviousNext(iterator);
    }
    catch (ParseException e)
    {
      m_test_.errln("Failed : Rule parse error " + e.getMessage());
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
      m_test_.errln("Failed : Collation element iterator offset is not " + 
                    "equals to " + TEST_CASE_1_.length());

    // Now set the offset back to the beginning and see if it works
    CollationElementIterator iter2 = 
                       m_collator_.getCollationElementIterator(TEST_CASE_1_);           
      
    iter.setOffset(0);

    for (int i = 0; i < offset; i ++)
      if (iter.next() != iter2.next())
      {
        m_test_.errln("Failed : Offset reset should revert collation element"
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
        m_test_.errln("Failed : Collation element iterator should be equal " 
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
    // Try a simple one first:
    // The only expansion ends with 'e' and has length 2
    String rule = "< a & ae = \u00e4 < b < e";
    
    char testchar1[] = {0x61, 0x62, 0x65};
    int testint1[] = {1, 1, 2};
    
    verifyExpansion(rule, testchar1, testint1);
      
    // Now a more complicated one:
    //   "a1" --> "ae"
    //   "z" --> "aeef"
    //
    rule = "< a & ae = a1 & aeef = z < b < e < f";
    char testchar2[] = {0x61, 0x62, 0x65, 0x66};
    int testint2[] = {1, 1, 2, 4};
    verifyExpansion(rule, testchar2, testint2);
  }

  /**
  * Clear buffer test.
  * Testing @bug 4157299
  * @exception thrown when error occurs
  */
  public void TestClearBuffers()
  {
    try
    {
      RuleBasedCollator collator = 
                              new RuleBasedCollator("< a < b < c & ab = d");

      CollationElementIterator iter = 
                                  collator.getCollationElementIterator("abcd");
        
      int elem = iter.next();   // save the first collation element
      iter.setOffset(3);        // go to the expanding character
      iter.next();              // but only use up half of it
      iter.setOffset(0);        // go back to the beginning

      int elem2 = iter.next();  // and get this one again

      if (elem != elem2)
        m_test_.errln("Failed : Clear buffers expected result " + elem + 
                      " not " + elem2);
    }
    catch (ParseException e)
    {
      m_test_.errln("Failed : Rule parse error " + e.getMessage());
    }
  }
  
  // private variables =============================================
  
  /**
  * RuleBasedCollator for testing
  */
  private RuleBasedCollator m_collator_;
  
  /**
  * Main Collation test program
  */
  private CollatorTest m_test_;
  
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
  private void testPreviousNext(CollationElementIterator iter)
  {
    // Run through the iterator forwards and stick it into an array
    int orders[] = getOrders(iter);
    
    // Now go through it backwards and make sure we get the same values
    int index = orders.length;
    int order = iter.previous();
    while (order != CollationElementIterator.NULLORDER)
    {
      if (order != orders[--index])
      {
        m_test_.errln("Fail : CollationElementIterator previous element " 
                      + "expected to be " + 
                      Integer.toHexString(orders[index]) + " not " +
                      Integer.toHexString(order));
        break;
      }
      order = iter.previous();
    }

    if (index != 0)
      m_test_.errln("Fail : CollationElementIterator has more previous " + 
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
  
  /**
  * Verify that getMaxExpansion works on a given set of collation rules.
  * The first row of the "tests" array contains the collation rules at index 
  * 0, and the string at index 1 is ignored.
  * Subsequent rows of the array contain a character and a number, both
  * represented as strings.  The character's collation order is determined,
  * and getMaxExpansion is called for that character.  If its value is
  * not equal to the specified number, an error results.
  * @param rules for constructing RuleBasedCollator
  * @param chararray character array for testing
  * @param countarray count array for testing corresponding to chararray
  * @exception thrown when errors occurs
  */
  private void verifyExpansion(String rules, char chararray[], 
                               int countarray[])
  {
    try
    {
      RuleBasedCollator collator = new RuleBasedCollator(rules);
      CollationElementIterator iter = 
                                      collator.getCollationElementIterator("");

      int order,
          expansion,
          expect;
      char test;
      for (int i = 1; i < chararray.length; i += 1)
      {
        // First get the collation key that the test string expands to
        test = chararray[i];
        iter.setText("" + test);

        order = iter.next();

        if (order == CollationElementIterator.NULLORDER || 
            iter.next() != CollationElementIterator.NULLORDER)
        {
          m_test_.errln("Failed : Collation element iterator nullorder " +
                        "occuring at the wrong places");
          break;
        }
            
        expansion = iter.getMaxExpansion(order);
        expect = countarray[i];
            
        if (expansion != expect)
          m_test_.errln("Failed : Expansion for " + test + " expected " + 
                        expect);
      }
    }
    catch (ParseException e)
    {
      m_test_.errln("Failed : Rule parse error " + e.getMessage());
    }
  }
}

