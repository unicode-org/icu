/**
*******************************************************************************
* Copyright (C) 1996-2000, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: 
*  /usr/cvs/icu4j/icu4j/src/com/ibm/icu/test/text/G7CollatorTest.java,v $ 
* $Date: 2001/03/09 00:42:45 $ 
* $Revision: 1.1 $
*
*******************************************************************************
*/

package com.ibm.icu4jni.test.text;

import java.util.Locale;
import com.ibm.icu4jni.text.Collator;
import com.ibm.icu4jni.text.RuleBasedCollator;

/**
* Testing class for collation with 7 different locales
* Mostly following the test cases for ICU
* @author Syn Wee Quek
* @since jan 23 2001
*/
public final class G7CollatorTest 
{ 
  
  // constructor ===================================================
  
  /**
  * Constructor
  */
  public G7CollatorTest(CollatorTest testprogram) throws Exception
  {
    m_test_ = testprogram;
    //m_collator_ = Collator.getInstance(new Locale("tr", ""));
  }
  
  // public methods ================================================

  /**
  * Test with all 7 locales
  * @exception thrown when error occurs while setting strength
  */
  public void TestLocales() throws Exception
  {
    RuleBasedCollator collator,
                         testcollator;
    String rules;
    
    for (int i = 0; i < LOCALES_.length; i ++)
    {
      collator = (RuleBasedCollator)Collator.getInstance(LOCALES_[i]);
      testcollator = new RuleBasedCollator(collator.getRules());
        
      for (int j = 0; j < FIXED_TEST_COUNT_; j ++)
        for (int k = j + 1; j < FIXED_TEST_COUNT_; k ++)
          m_test_.doTest(testcollator, 
                 SOURCE_TEST_CASE_[EXPECTED_TEST_RESULT_[i][j]], 
                 SOURCE_TEST_CASE_[EXPECTED_TEST_RESULT_[i][k]], 
                 Collator.RESULT_LESS);
    }
  }

  /**
  * Test default rules + addition rules.
  * @exception thrown when error occurs while setting strength
  */
  public void TestRules1() throws Exception
  {
    Collator collator = Collator.getInstance();
    String rules = ((RuleBasedCollator)collator).getRules();
    String newrules = rules + " & Z < p, P";
    RuleBasedCollator newcollator = new RuleBasedCollator(newrules);

    for (int j = 0; j < FIXED_TEST_COUNT_; j ++)
      for (int k = j + 1; k < FIXED_TEST_COUNT_; k ++)
        m_test_.doTest(newcollator, 
                       SOURCE_TEST_CASE_[EXPECTED_TEST_RESULT_[8][j]], 
                       SOURCE_TEST_CASE_[EXPECTED_TEST_RESULT_[8][k]], 
                       Collator.RESULT_LESS);
  }
  
  /**
  * Test default rules + addition rules.
  * @exception thrown when error occurs while setting strength
  */
  public void TestRules2() throws Exception
  {
    Collator collator = Collator.getInstance();
    String rules = ((RuleBasedCollator)collator).getRules();
    String newrules = rules + "& C < ch , cH, Ch, CH";
    
    RuleBasedCollator newcollator = new RuleBasedCollator(newrules);

    for (int i = 0; i < TOTAL_TEST_COUNT_; i ++)
      for (int j = i + 1; j < TOTAL_TEST_COUNT_; j++)
        m_test_.doTest(newcollator, 
                       SOURCE_TEST_CASE_[EXPECTED_TEST_RESULT_[9][i]], 
                       SOURCE_TEST_CASE_[EXPECTED_TEST_RESULT_[9][j]], 
                       Collator.RESULT_LESS);
  }
  
  /**
  * Test default rules + addition rules.
  * @exception thrown when error occurs while setting strength
  */
  public void TestRules3() throws Exception
  {
    Collator collator = Collator.getInstance();
    String rules = ((RuleBasedCollator)collator).getRules();
    String newrules = rules + 
             "& Question'-'mark ; '?' & Hash'-'mark ; '#' & Ampersand ; '&'";
    
    RuleBasedCollator newcollator = new RuleBasedCollator(newrules);

    for (int i = 0; i < TOTAL_TEST_COUNT_; i ++)
      for (int j = i + 1; j < TOTAL_TEST_COUNT_; j++)
        m_test_.doTest(newcollator, 
                       SOURCE_TEST_CASE_[EXPECTED_TEST_RESULT_[10][i]], 
                       SOURCE_TEST_CASE_[EXPECTED_TEST_RESULT_[10][j]], 
                       Collator.RESULT_LESS);
  }
  
  /**
  * Test default rules + addition rules.
  * @exception thrown when error occurs while setting strength
  */
  public void TestRules4() throws Exception
  {
    Collator collator = Collator.getInstance();
    String rules = ((RuleBasedCollator)collator).getRules();
    String newrules = rules + 
             " & aa ; a'-' & ee ; e'-' & ii ; i'-' & oo ; o'-' & uu ; u'-' ";
    
    RuleBasedCollator newcollator = new RuleBasedCollator(newrules);

    for (int i = 0; i < TOTAL_TEST_COUNT_; i ++)
      for (int j = i + 1; j < TOTAL_TEST_COUNT_; j++)
        m_test_.doTest(newcollator, 
                       SOURCE_TEST_CASE_[EXPECTED_TEST_RESULT_[11][i]], 
                       SOURCE_TEST_CASE_[EXPECTED_TEST_RESULT_[11][j]], 
                       Collator.RESULT_LESS);
  }
  
  // private variables =============================================
  
  /**
  * Main Collation test program
  */
  private CollatorTest m_test_;
  
  /**
  * Constant test number
  */
  private int FIXED_TEST_COUNT_ = 15;
  
  /**
  * Constant test number
  */
  private int TOTAL_TEST_COUNT_ = 30;
  
  /**
  * List of 7 locales to be tested
  */
  private final Locale LOCALES_[] = {Locale.US, Locale.UK, Locale.CANADA,
                                   Locale.FRANCE, Locale.CANADA_FRENCH,
                                   Locale.GERMAN, Locale.ITALY, 
                                   Locale.JAPAN};
  
  /**
  * Source strings for testing
  */
  private static final String SOURCE_TEST_CASE_[] = 
  {
    "\u0062\u006c\u0061\u0063\u006b\u002d\u0062\u0069\u0072\u0064\u0073",   
    "\u0050\u0061\u0074",                                           
    "\u0070\u00E9\u0063\u0068\u0000E9",                           
    "\u0070\u00EA\u0063\u0068\u0065",                                 
    "\u0070\u00E9\u0063\u0068\u0065\u0072",                         
    "\u0070\u00EA\u0063\u0068\u0065\u0072",                         
    "\u0054\u006f\u0064",                                           
    "\u0054\u00F6\u006e\u0065",                                   
    "\u0054\u006f\u0066\u0075",                                      
    "\u0062\u006c\u0061\u0063\u006b\u0062\u0069\u0072\u0064\u0073",        
    "\u0054\u006f\u006e",                                           
    "\u0050\u0041\u0054",                                           
    "\u0062\u006c\u0061\u0063\u006b\u0062\u0069\u0072\u0064",             
    "\u0062\u006c\u0061\u0063\u006b\u002d\u0062\u0069\u0072\u0064",        
    "\u0070\u0061\u0074",                                           
    "\u0063\u007a\u0061\u0072",                                     
    "\u0063\u0068\u0075\u0072\u006f",                                
    "\u0063\u0061\u0074",                                          
    "\u0064\u0061\u0072\u006e",                                     
    "\u003f",                                                    
    "\u0071\u0075\u0069\u0063\u006b",                                
    "\u0023",                                                    
    "\u0026",                                                    
    "\u0061\u0061\u0072\u0064\u0076\u0061\u0072\u006b",                  
    "\u0061\u002d\u0072\u0064\u0076\u0061\u0072\u006b",                  
    "\u0061\u0062\u0062\u006f\u0074",                                 
    "\u0063\u006f\u006f\u0070",                                     
    "\u0063\u006f\u002d\u0070",                                      
    "\u0063\u006f\u0070",                                           
    "\u007a\u0065\u0062\u0072\u0061"
  };

  /**
  * Comparison result corresponding to above source and target cases
  */
  private final int EXPECTED_TEST_RESULT_[][] = 
  {
    {12, 13, 9, 0, 14, 1, 11, 2, 3, 4, 5, 6, 8, 10, 7, 31, 31, 31, 31, 31, 
     31, 31, 31, 31, 31, 31, 31, 31, 31, 31}, // en_US
    {12, 13, 9, 0, 14, 1, 11, 2, 3, 4, 5, 6, 8, 10, 7, 31, 31, 31, 31, 31, 
     31, 31, 31, 31, 31, 31, 31, 31, 31, 31}, // en_GB
    {12, 13, 9, 0, 14, 1, 11, 2, 3, 4, 5, 6, 8, 10, 7, 31, 31, 31, 31, 31, 
     31, 31, 31, 31, 31, 31, 31, 31, 31, 31}, // en_CA
    {12, 13, 9, 0, 14, 1, 11, 3, 2, 4, 5, 6, 8, 10, 7, 31, 31, 31, 31, 31, 
     31, 31, 31, 31, 31, 31, 31, 31, 31, 31}, // fr_FR
    {12, 13, 9, 0, 14, 1, 11, 3, 2, 4, 5, 6, 8, 10, 7, 31, 31, 31, 31, 31, 
     31, 31, 31, 31, 31, 31, 31, 31, 31, 31}, // fr_CA
    {12, 13, 9, 0, 14, 1, 11, 2, 3, 4, 5, 6, 8, 10, 7, 31, 31, 31, 31, 31, 
     31, 31, 31, 31, 31, 31, 31, 31, 31, 31}, // de_DE
    {12, 13, 9, 0, 14, 1, 11, 2, 3, 4, 5, 6, 8, 10, 7, 31, 31, 31, 31, 31, 
     31, 31, 31, 31, 31, 31, 31, 31, 31, 31}, // it_IT
    {12, 13, 9, 0, 14, 1, 11, 2, 3, 4, 5, 6, 8, 10, 7, 31, 31, 31, 31, 31, 
     31, 31, 31, 31, 31, 31, 31, 31, 31, 31}, // ja_JP
    {12, 13, 9, 0, 6, 8, 10, 7, 14, 1, 11, 2, 3, 4, 5, 31, 31, 31, 31, 31, 
     31, 31, 31, 31, 31, 31, 31, 31, 31, 31},
    {19, 22, 21, 23, 25, 24, 12, 13, 9, 0, 17, 26, 28, 27, 15, 16, 18, 14, 
     1, 11, 2, 3, 4, 5, 20, 6, 8, 10, 7, 29},
    {23, 25, 22, 24, 12, 13, 9, 0, 17, 16, 26, 28, 27, 15, 18, 21, 14, 1, 
     11, 2, 3, 4, 5, 19, 20, 6, 8, 10, 7, 29},
    {19, 22, 21, 23, 24, 25, 12, 13, 9, 0, 17, 16, 26, 27, 28, 15, 18, 14, 
     1, 11, 2, 3, 4, 5, 20, 6, 8, 10, 7, 29}                                          
  };
}

