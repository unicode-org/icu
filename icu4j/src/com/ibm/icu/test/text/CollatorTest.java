/**
*******************************************************************************
* Copyright (C) 1996-2000, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/test/text/Attic/CollatorTest.java,v $ 
* $Date: 2001/03/09 00:42:45 $ 
* $Revision: 1.1 $
*
*******************************************************************************
*/

package com.ibm.icu4jni.test.text;

import com.ibm.test.TestFmwk;
import com.ibm.icu4jni.text.Collator;
import com.ibm.icu4jni.text.CollationKey;

/**
* Testing class for Collator
* Mostly following the test cases for ICU
* @author Syn Wee Quek
* @since jan 23 2001
*/
public final class CollatorTest extends TestFmwk
{ 
  // private variables =============================================
  
  // constructor ===================================================
  
  /**
  * Constructor
  */
  public CollatorTest()
  {
  }
  
  // public methods ================================================
  
  /**
  * Testing rule collation
  * @param collator to test with
  * @param source test case
  * @param target test case
  * @param result expected
  */
  public void doTest(Collator collator, String source, String target, 
                     int result)
  {
    int compareResult = collator.compare(source, target);
    
    CollationKey sortkey1 = collator.getCollationKey(source),
                 sortkey2 = collator.getCollationKey(target);
                 
    if (sortkey1 == null)
    {
      errln("Failed : Sort key generation for " + source);
      return;
    }
    if (sortkey2 == null)
    {
      errln("Failed : Sort key generation for " + target);
      return;
    }

    int compareresult = sortkey1.compareTo(sortkey2);
    
    if (compareresult != result)
      errln("Failed : Expected result for " + source + " and " + target +
            " sort key comparison is " + result);
  }
  
  /**
  * Testing English rule collation
  */
  public void TestCollationEnglish()
  {
    try
    {
      EnglishCollatorTest englishcollator = 
                                             new EnglishCollatorTest(this);
  
      englishcollator.TestPrimary();
      System.out.println("Tested primary");
      englishcollator.TestSecondary();
      System.out.println("Tested secondary");
      englishcollator.TestTertiary();
      System.out.println("Tested tertiary");
    }
    catch (Exception e)
    {
      errln("Failed : " + e.getMessage());
    }
  }
  
  /**
  * Testing French rule collation
  */
  public void TestCollationFrench()
  {
    try
    {
      FrenchCollatorTest frenchcollator = new FrenchCollatorTest(this);
  
      frenchcollator.TestBugs();
      frenchcollator.TestSecondary();
      frenchcollator.TestTertiary();
    }
    catch (Exception e)
    {
      errln("Failed : " + e.getMessage());
    }
  }
  
  /**
  * Testing German rule collation
  */
  public void TestCollationGerman()
  {
    try
    {
      GermanCollatorTest germancollator = new GermanCollatorTest(this);
  
      germancollator.TestPrimary();
      germancollator.TestTertiary();
    }
    catch (Exception e)
    {
      errln("Failed : " + e.getMessage());
    }
  }
  
  /**
  * Testing Danish rule collation
  */
  public void TestCollationDanish()
  {
    try
    {
      DanishCollatorTest danishcollator = new DanishCollatorTest(this);
  
      danishcollator.TestPrimary();
      danishcollator.TestTertiary();
    }
    catch (Exception e)
    {
      errln("Failed : " + e.getMessage());
    }
  }
  
  /**
  * Testing Spanish rule collation
  */
  public void TestCollationSpanish()
  {
    try
    {
      SpanishCollatorTest spanishcollator = 
                                            new SpanishCollatorTest(this);
  
      spanishcollator.TestPrimary();
      spanishcollator.TestTertiary();
    }
    catch (Exception e)
    {
      errln("Failed : " + e.getMessage());
    }
  }
  
  /**
  * Testing Finnish rule collation
  */
  public void TestCollationFinnish()
  {
    try
    {
      SpanishCollatorTest spanishcollator = 
                                            new SpanishCollatorTest(this);
  
      spanishcollator.TestPrimary();
      spanishcollator.TestTertiary();
    }
    catch (Exception e)
    {
      errln("Failed : " + e.getMessage());
    }
  }
  
  /**
  * Testing Kana rule collation
  */
  public void TestCollationKana()
  {
    try
    {
      KanaCollatorTest kanacollator = new KanaCollatorTest(this);
  
      kanacollator.TestTertiary();
    }
    catch (Exception e)
    {
      errln("Failed : " + e.getMessage());
    }
  }
  
  /**
  * Testing Turkish rule collation
  */
  public void TestCollationTurkish()
  {
    try
    {
      TurkishCollatorTest turkishcollator = 
                                          new TurkishCollatorTest(this);
      turkishcollator.TestPrimary();
      turkishcollator.TestTertiary();
    }
    catch (Exception e)
    {
      errln("Failed : " + e.getMessage());
    }
  }
  
  /**
  * Testing dummy collation
  */
  public void TestCollationDummy()
  {
    try
    {
      DummyCollatorTest dummycollator = new DummyCollatorTest(this);
      dummycollator.TestPrimary();
      dummycollator.TestSecondary();
      dummycollator.TestTertiary();
      dummycollator.TestMiscellaneous();
    }
    catch (Exception e)
    {
      errln("Failed : " + e.getMessage());
    }
  }
  
  /**
  * Testing G7 rule collation
  */
  public void TestCollationG7()
  {
    try
    {
      G7CollatorTest g7collator = new G7CollatorTest(this);
      g7collator.TestLocales();
      g7collator.TestRules1();
      g7collator.TestRules2();
      g7collator.TestRules3();
      g7collator.TestRules4();
    }
    catch (Exception e)
    {
      errln("Failed : " + e.getMessage());
    }
  }
  
  /**
  * Testing Monkey rule collation
  */
  public void TestCollationMonkey()
  {
    try
    {
      MonkeyCollatorTest monkeycollator = new MonkeyCollatorTest(this);
      monkeycollator.TestCollationKey();
      monkeycollator.TestCompare();
      monkeycollator.TestRules();
    }
    catch (Exception e)
    {
      errln("Failed : " + e.getMessage());
    }
  }
  
  /**
  * Testing collation API
  */
  public void TestCollationAPI()
  {
    try
    {
      CollatorAPITest collator = new CollatorAPITest(this);
      collator.TestProperties();
      collator.TestCollationKey();
      collator.TestCompare();
      collator.TestDuplicate();
      collator.TestElementIterator();
      collator.TestHashCode();
      collator.TestOperators();
    }
    catch (Exception e)
    {
      errln("Failed : " + e.getMessage());
    }
  }
  
  /**
  * Testing collation regression
  */
  public void TestCollationRegression()
  {
    try
    {
      CollatorRegressionTest collator = 
                                        new CollatorRegressionTest(this);
      collator.Test4048446();
      collator.Test4051866();
      collator.Test4053636();
      collator.Test4054238();
      collator.Test4054734();
      collator.Test4054736();
      collator.Test4058613();
      collator.Test4059820();
      collator.Test4060154();
      collator.Test4062418();
      collator.Test4065540();
      collator.Test4066189();
      collator.Test4066696();
      collator.Test4076676();
      collator.Test4078588();
      collator.Test4081866();
      collator.Test4087241();
      collator.Test4087243();
      collator.Test4092260();
      collator.Test4095316();
      collator.Test4101940();
      collator.Test4103436();
      collator.Test4114076();
      collator.Test4114077();
      collator.Test4124632();
      collator.Test4132736();
      collator.Test4133509();
      collator.Test4139572();
    }
    catch (Exception e)
    {
      errln("Failed : " + e.getMessage());
    }
  }
  
  /**
  * Testing currency collation
  */
  public void TestCollationCurrency()
  {
    try
    {
      CurrencyCollatorTest test = new CurrencyCollatorTest(this);
      test.TestCurrency();
    }
    catch (Exception e)
    {
      errln("Failed : " + e.getMessage());
    }
  }
  
  /**
  * Testing collation iterator
  */
  public void TestCollationIterator()
  {
    try
    {
      CollationElementIteratorTest test = 
                                   new CollationElementIteratorTest(this);
      test.TestPrevious();
      test.TestSetText();
      test.TestClearBuffers();
      test.TestMaxExpansion();
      test.TestOffset();
    }
    catch (Exception e)
    {
      errln("Failed : " + e.getMessage());
    }
  }
  
  /**
  * Testing Thai rule collation
  */
  public void TestCollationThai()
  {
    try
    {
      ThaiCollatorTest test = new ThaiCollatorTest(this);
      test.TestStrings();
      test.TestOddCase();
    }
    catch (Exception e)
    {
      errln("Failed : " + e.getMessage());
    }
  }
  
  public static void main(String[] arg)
  {
    try
    {
      CollatorTest test = new CollatorTest();
      String args[] = new String[1];
      args[0] = arg[1];
      test.run(args);
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }
}

