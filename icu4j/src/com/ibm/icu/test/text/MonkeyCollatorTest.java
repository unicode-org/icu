/**
*******************************************************************************
* Copyright (C) 1996-2000, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: 
*  /usr/cvs/icu4j/icu4j/src/com/ibm/icu/test/text/MonkeyCollatorTest.java,v $ 
* $Date: 2001/03/09 00:42:45 $ 
* $Revision: 1.1 $
*
*******************************************************************************
*/

package com.ibm.icu4jni.test.text;

import java.util.Locale;
import com.ibm.icu4jni.text.Collator;
import com.ibm.icu4jni.text.RuleBasedCollator;
import com.ibm.icu4jni.text.CollationKey;
import com.ibm.icu4jni.text.CollationAttribute;

/**
* Testing class for collation keys, comparison methods
* Mostly following the test cases for ICU
* @author Syn Wee Quek
* @since jan 29 2001
*/
public final class MonkeyCollatorTest 
{ 
  
  // constructor ===================================================
  
  /**
  * Constructor
  */
  public MonkeyCollatorTest(CollatorTest testprogram) throws Exception
  {
    m_test_ = testprogram;
    m_collator_ = Collator.getInstance();
  }
  
  // public methods ================================================

  /**
  * Testing collation keys
  * @exception thrown when error occurs while setting strength
  */
  public void TestCollationKey() throws Exception
  {
    int s1 = (int)(Math.random() * SOURCE_TEST_CASE_.length());
    int t1 = (int)(Math.random() * SOURCE_TEST_CASE_.length());
    int s2 = (int)(Math.random() * SOURCE_TEST_CASE_.length());
    int t2 = (int)(Math.random() * SOURCE_TEST_CASE_.length());
    
    String subs = SOURCE_TEST_CASE_.substring(Math.min(s1, s2),
                                              Math.max(s1, s2)),
           subt = SOURCE_TEST_CASE_.substring(Math.min(t1, 2),
                                              Math.max(t1, t2));

    CollationKey ck1, ck2;
    m_collator_.setStrength(CollationAttribute.VALUE_TERTIARY);
    ck1 = m_collator_.getCollationKey(subs);
    ck2 = m_collator_.getCollationKey(subt);
    int result = ck1.compareTo(ck2);      // Tertiary
    int revresult = ck2.compareTo(ck1) ;  // Tertiary
    if (result != -revresult)
    {
      m_test_.errln("Failed : Round trip collation key comparison");
      return;
    }

    m_collator_.setStrength(CollationAttribute.VALUE_SECONDARY);
    ck1 = m_collator_.getCollationKey(subs);
    ck2 = m_collator_.getCollationKey(subt);
    result = ck1.compareTo(ck2);      // Secondary
    revresult = ck2.compareTo(ck1) ;  // Secondary
    if (result != -revresult)
    {
      m_test_.errln("Failed : Round trip collation key comparison");
      return;
    }

    m_collator_.setStrength(CollationAttribute.VALUE_PRIMARY);
    ck1 = m_collator_.getCollationKey(subs);
    ck2 = m_collator_.getCollationKey(subt);
    
    result = ck1.compareTo(ck2);  // Tertiary
    revresult = ck2.compareTo(ck1) ;  // Tertiary
    
    if (result != -revresult)
    {
      m_test_.errln("Failed : Round trip collation key comparison");
      return;
    }

    String news = subs + "\uE000";
    ck1 = m_collator_.getCollationKey(subs);
    ck2 = m_collator_.getCollationKey(news);
    if (ck1.compareTo(ck2) != Collator.RESULT_LESS)
    {
      m_test_.errln("Failed : Collation key comparison of a string and " +
                    "a similar string with an extra character is expected " +
                    "to return a LESS");
      return;
    }

    if (ck2.compareTo(ck1) != Collator.RESULT_GREATER)
    {
      m_test_.errln("Failed : Collation key comparison of a string and " +
                    "a similar string with one less character is expected " +
                    "to return a GREATER");
      return;
    }  
  }

  /**
  * Test comparison methods
  * @exception thrown when error occurs while setting strength
  */
  public void TestCompare() throws Exception
  {
    int s1 = (int)(Math.random() * SOURCE_TEST_CASE_.length());
    int t1 = (int)(Math.random() * SOURCE_TEST_CASE_.length());
    int s2 = (int)(Math.random() * SOURCE_TEST_CASE_.length());
    int t2 = (int)(Math.random() * SOURCE_TEST_CASE_.length());
    
    String subs = SOURCE_TEST_CASE_.substring(Math.min(s1, s2),
                                              Math.max(s1, s2)),
           subt = SOURCE_TEST_CASE_.substring(Math.min(t1, 2),
                                              Math.max(t1, t2));

    m_collator_.setStrength(CollationAttribute.VALUE_TERTIARY);
    int result = m_collator_.compare(subs, subt);      // Tertiary
    int revresult = m_collator_.compare(subt, subs);   // Tertiary
    if (result != -revresult)
    {
      m_test_.errln("Failed : Round trip collation key comparison");
      return;
    }

    m_collator_.setStrength(CollationAttribute.VALUE_SECONDARY);
    result = m_collator_.compare(subs, subt);      // Tertiary
    revresult = m_collator_.compare(subt, subs);   // Tertiary
    if (result != -revresult)
    {
      m_test_.errln("Failed : Round trip collation key comparison");
      return;
    }

    m_collator_.setStrength(CollationAttribute.VALUE_PRIMARY);
    result = m_collator_.compare(subs, subt);      // Tertiary
    revresult = m_collator_.compare(subt, subs);   // Tertiary
    if (result != -revresult)
    {
      m_test_.errln("Failed : Round trip collation key comparison");
      return;
    }

    String news = subs + "\uE000";
    if (m_collator_.compare(subs, news) != Collator.RESULT_LESS)
    {
      m_test_.errln("Failed : Collation key comparison of a string and " +
                    "a similar string with an extra character is expected " +
                    "to return a LESS");
      return;
    }

    if (m_collator_.compare(news, subs) != Collator.RESULT_GREATER)
    {
      m_test_.errln("Failed : Collation key comparison of a string and " +
                    "a similar string with one less character is expected " +
                    "to return a GREATER");
      return;
    }  
  }
  
  /**
  * Test rules
  * @exception thrown when error occurs while setting strength
  */
  public void TestRules() throws Exception
  {
    String source[] = {"\u0061\u0062\u007a", "\u0061\u0062\u007a"};
    String target[] = {"\u0061\u0062\u00e4", "\u0061\u0062\u0061\u0308"};
    
    String rules = ((RuleBasedCollator)m_collator_).getRules();
    String newrules = rules + " & z < " + "\u00e4";
    
    RuleBasedCollator collator = new RuleBasedCollator(newrules);
    
    for (int i = 0; i < 2; i ++)
      m_test_.doTest(collator, source[i], target[i], 
                     Collator.RESULT_LESS);
    
    newrules = rules + " & z < a" + "\u0308";
    
    collator = new RuleBasedCollator(rules);
    
    for (int i = 0; i < 2; i ++)
      m_test_.doTest(collator, source[i], target[i], 
                     Collator.RESULT_LESS);
  }
  
  // private variables =============================================
  
  /**
  * Main Collation test program
  */
  private CollatorTest m_test_;
  
  /**
  * Test collator
  */
  private Collator m_collator_;
  
  /**
  * Source string for testing
  */
  private static final String SOURCE_TEST_CASE_ = 
                                           "-abcdefghijklmnopqrstuvwxyz#&^$@";
}

