/**
*******************************************************************************
* Copyright (C) 1996-2000, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: 
*  /usr/cvs/icu4j/icu4j/src/com/ibm/icu/test/text/CollatorJNIPerformanceTest.java,v $ 
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

/**
* Performance testing class for Collator
* @author Syn Wee Quek
* @since jan 23 2001
*/
public final class CollatorPerformanceTest 
{ 
  
  // constructor ===================================================
  
  /**
  * Constructor
  */
  public CollatorPerformanceTest()
  {
  }
  
  // public methods ================================================

  /**
  * Sortkey retrieval performance testing
  * @param locale for processing
  * @param str test string
  * @return time taken for loop
  */
  public long testSortKey(Locale locale, String str) throws Exception
  {
    System.gc();
    RuleBasedCollator collator = 
                        (RuleBasedCollator)Collator.getInstance(locale);
    
    // initial startup cost
    collator.getCollationKey(str);
    
    long start = System.currentTimeMillis();
    for (int i = 0; i < LOOP_COUNT_; i ++)
      collator.getCollationKey(str);
   
    long end = System.currentTimeMillis();
    
    long loopstart = System.currentTimeMillis();
    for (int i = 0; i < LOOP_COUNT_; i ++)
    {
    }
  
    long loopend = System.currentTimeMillis();
    
    collator = null;
    str = null;
    return (end - start) - (loopend - loopstart);
  }
  
  /**
  * Sortkey retrieval performance testing
  * @param locale for processing
  * @param str test string
  * @return time taken for loop
  */
  public long testJDKSortKey(Locale locale, String str) throws Exception
  {
    System.gc();
    java.text.Collator collator = java.text.Collator.getInstance(locale);
   
    collator.getCollationKey(str);
    
    long start = System.currentTimeMillis();
    for (int i = 0; i < LOOP_COUNT_ - 1; i ++)
      collator.getCollationKey(str);
    long end = System.currentTimeMillis();
    
    long loopstart = System.currentTimeMillis();
    for (int i = 0; i < LOOP_COUNT_; i ++)
    {
    }
  
    long loopend = System.currentTimeMillis();
    
    collator = null;
    str = null;
    return (end - start) - (loopend - loopstart);
  }
  
  /**
  * Main method for running test
  * @param arg argument
  */
  public static void main(String[] arg)
  {
    Locale locale[] = {Locale.US, Locale.CHINA, Locale.FRANCE, Locale.GERMANY,
                       Locale.JAPAN, Locale.ITALY};
    String str = LONG_STRING_;
    
    if (arg.length > 0)
    {
      if (arg[0].equals("short"))
        str = SHORT_STRING_;
      else 
        if (!arg[0].equals("long"))
          str = arg[0];
    }
    
    System.out.println(str);
    
    long time = 0;
    CollatorPerformanceTest test = new CollatorPerformanceTest();
    
    try
    {
      System.out.println("");
      System.out.println("US        CHINA     FRANCE    GERMANY   JAPAN     ITALY");
      // doing a first round to remove any possibility of startup cost
      for (int i = 0; i < CONSISTENCY_COUNT_; i ++)
      {
        for (int j = 0; j < locale.length; j ++)
        {
          time = test.testSortKey(locale[j], str);
          System.out.print(time + "      ");
        }
        System.out.println();
      }
      
      System.out.println("JAVA");
      System.out.println("US        CHINA     FRANCE    GERMANY   JAPAN    ITALY");
      // doing a first round to remove any possibility of startup cost
      for (int i = 0; i < CONSISTENCY_COUNT_; i ++)
      {
        for (int j = 0; j < locale.length; j ++)
        {
          time = test.testJDKSortKey(locale[j], str);
          System.out.print(time + "      ");
        }
        System.out.println();
      }
    }
    catch(Exception e)
    {
      e.printStackTrace();
    }
  }
  
  // private variables =============================================
  
  /**
  * RuleBasedCollator for performance testing
  */
  private Collator m_collator_;
  
  /**
  * Number times for looping
  */
  private static final int LOOP_COUNT_ = 200000;
  
  /**
  * Number of times for running the loops to determine consistency
  */
  private static final int CONSISTENCY_COUNT_ = 10;
  
  /**
  * Long test string
  */
  private static final String LONG_STRING_ = 
  "\u0055\u006e\u0069\u0074\u0065\u0064\u0020\u0053\u0074\u0061\u0074\u0065" +
  "\u0073\u4e2d\u534e\u4eba\u6c11\u5171\u548c\u56fd\u0046\u0072\u0061\u006e" +
  "\u0063\u0065\u0044\u0065\u0075\u0074\u0073\u0063\u0068\u006c\u0061\u006e" +
  "\u0064\u65e5\u672c\u0049\u0074\u0061\u006c\u0069\u0061";
   
  /**
  * Short test string
  */
  private static final String SHORT_STRING_ = 
  "\u0055\u4e2d\u534e\u4eba\u0046\u0044\u65e5\u372c\u0049\u0074";
}

