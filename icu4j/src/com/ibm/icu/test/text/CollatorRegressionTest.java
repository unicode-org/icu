/**
*******************************************************************************
* Copyright (C) 1996-2000, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: 
*  /usr/cvs/icu4j/icu4j/src/com/ibm/icu/test/text/CollatorRegressionTest.java,v $ 
* $Date: 2001/03/09 23:41:46 $ 
* $Revision: 1.2 $
*
*******************************************************************************
*/

package com.ibm.icu4jni.test.text;

import com.ibm.icu4jni.text.Collator;
import com.ibm.icu4jni.text.RuleBasedCollator;
import com.ibm.icu4jni.text.CollationKey;
import com.ibm.icu4jni.text.CollationElementIterator;
import com.ibm.icu4jni.text.NormalizationMode;
import com.ibm.icu4jni.text.CollationAttribute;
import java.util.Locale;

/**
* Collator regression testing class
* Mostly following the test cases for ICU
* @author Syn Wee Quek
* @since jan 29 2001
*/
public final class CollatorRegressionTest 
{ 
  
  // constructor ===================================================
  
  /**
  * Constructor
  */
  public CollatorRegressionTest(CollatorTest testprogram) throws Exception
  {
    m_test_ = testprogram;
    m_collator_ = Collator.getInstance();
  }
  
  // public methods ================================================

  /**
  * Testing bug 4048446
  * @exception thrown when error occurs while setting strength
  */
  public void Test4048446() throws Exception
  {
    CollationElementIterator i1 = 
      ((RuleBasedCollator)m_collator_).getCollationElementIterator(
                                                            TEST_STRING_1_);
    CollationElementIterator i2 = 
      ((RuleBasedCollator)m_collator_).getCollationElementIterator(
                                                            TEST_STRING_1_);
    if (i1 == null || i2 == null)
    {
      m_test_.errln("Failed : Creation of the collation element iterator");
      return;
    }

    while (i1.next() != CollationElementIterator.NULLORDER)
    {
    }

    i1.reset();

    int c1 = 1,
        c2;
    while (c1 != CollationElementIterator.NULLORDER);
    {
      c1 = i1.next();
      c2 = i2.next();

      if (c1 != c2)
      {
        m_test_.errln("Failed : Resetting collation element iterator " +
                      "should revert it back to the orginal state");
        return;
      }
    }
  }

  /**
  * Testing bug 4051866
  * @exception thrown when error occurs while setting strength
  */
  public void Test4051866() throws Exception
  {
    String rules= "< o & oe ,o\u3080& oe ,\u1530 ,O& OE ,O\u3080& OE ," +
                  "\u1520< p ,P";

    // Build a collator containing expanding characters
    RuleBasedCollator c1 = new RuleBasedCollator(rules);

    // Build another using the rules from  the first
    RuleBasedCollator c2 = new RuleBasedCollator(c1.getRules());

    if (!(c1.getRules().equals(c2.getRules())))
      m_test_.errln("Failed : Rules from equivalent collators should be " +
                    "the same");
  }
  
  /**
  * Testing bug 4053636
  * @exception thrown when error occurs while setting strength
  */
  public void Test4053636() throws Exception
  {
    if (m_collator_.equals("black_bird", "black"))
      m_test_.errln("Failed : black-bird != black");
  }
  
  /**
  * Testing bug 4054238
  * @exception thrown when error occurs while setting strength
  */
  public void Test4054238() throws Exception
  {
    RuleBasedCollator c = (RuleBasedCollator)m_collator_.clone();

    c.setDecomposition(NormalizationMode.DECOMP_CAN);
    CollationElementIterator i1 = c.getCollationElementIterator(
                                                              TEST_STRING_3_);
    c.setDecomposition(NormalizationMode.NO_NORMALIZATION);
    CollationElementIterator i2 = c.getCollationElementIterator(
                                                              TEST_STRING_3_);

    // At this point, BOTH iterators should use NO_DECOMPOSITION, since the
    // collator itself is in that mode
    int c1 = 1,
        c2;
    while (c1 != CollationElementIterator.NULLORDER);
    {
      c1 = i1.next();
      c2 = i2.next();

      if (c1 != c2)
      {
        m_test_.errln("Failed : Comparison of equivalent collation element " +
                      "iterator should be equal");
        return;
      }
    }
  }
  
  /**
  * Testing bug 4054734
  * @exception thrown when error occurs while setting strength
  */
  public void Test4054734() throws Exception
  {
    final String decomp[] = {"\u0001", "\u003c", "\u0002", "\u0001", "\u003d", 
                             "\u0001", "\u0041", "\u0001", "\u003e", "\u007e",
                             "\u0002", "\u00c0", "\u003d", "\u0041\u0300"};

    final String nodecomp[] = {"\u00C0", "\u003e", "\u0041\u0300"};

    RuleBasedCollator c = (RuleBasedCollator)m_collator_.clone();

    c.setStrength(CollationAttribute.VALUE_IDENTICAL);

    c.setDecomposition(NormalizationMode.DECOMP_CAN);
    compareStrings(c, decomp, decomp.length);
    c.setDecomposition(NormalizationMode.NO_NORMALIZATION);
    compareStrings(c, nodecomp, nodecomp.length);
  }
  
  /**
  * Testing bug 4054734
  * @exception thrown when error occurs while setting strength
  */
  public void Test4054736() throws Exception
  {
    RuleBasedCollator c = (RuleBasedCollator)m_collator_.clone();

    c.setDecomposition(NormalizationMode.DECOMP_COMPAT);

    final String tests[] = {"\uFB4F", "\u003d", "\u05D0\u05DC"};  
                           // Alef-Lamed vs. Alef, Lamed

    compareStrings(c, tests, tests.length);
  }
  
  /**
  * Testing bug 4058613
  * @exception thrown when error occurs while setting strength
  */
  public void Test4058613() throws Exception
  {
    Locale oldDefault = Locale.getDefault();
    Locale.setDefault(Locale.KOREAN);

    Collator c = Collator.getInstance();

    // Since the fix to this bug was to turn off decomposition for Korean 
    // collators, ensure that's what we got
    if (c.getDecomposition() != NormalizationMode.NO_NORMALIZATION)    
      m_test_.errln("Failed : Decomposition is not set to NO_DECOMPOSITION " +
                    "for Korean collator");

    Locale.setDefault(oldDefault);
  }
  
  /**
  * Testing bug 4059820
  * @exception thrown when error occurs while setting strength
  */
  public void Test4059820() throws Exception
  {
    String rules = "< a < b , c/a < d < z";
    
    RuleBasedCollator c = new RuleBasedCollator(rules);

    if (c.getRules().indexOf("c/a") == -1)
      m_test_.errln("Failed : Rules should contain 'c/a'");
  }
  
  /**
  * Testing bug 4060154
  * @exception thrown when error occurs while setting strength
  */
  public void Test4060154() throws Exception
  {
    String rules = "< g, G < h, H < i, I < j, J & H < \u0131, \u0130, i, I";
    RuleBasedCollator c = new RuleBasedCollator(rules);

    c.setDecomposition(NormalizationMode.DECOMP_CAN);

    String tertiary[] = {"\u0041", "\u003c", "\u0042", "\u0048", "\u003c", 
                         "\u0131", "\u0048", "\u003c", "\u0049", "\u0131", 
                         "\u003c", "\u0130", "\u0130", "\u003c", "\u0069",
                         "\u0130", "\u003e", "\u0048"};

    c.setStrength(CollationAttribute.VALUE_TERTIARY);
    compareStrings(c, tertiary, tertiary.length);

    String secondary[] = {"\u0048", "\u003c", "\u0049", "\u0131", "\u003d", 
                          "\u0130"};

    c.setStrength(CollationAttribute.VALUE_PRIMARY);
    compareStrings(c, secondary, secondary.length);
  }
  
  /**
  * Testing bug 4062418
  * @exception thrown when error occurs while setting strength
  */
  public void Test4062418() throws Exception
  {
    RuleBasedCollator c = (RuleBasedCollator)Collator.getInstance(
                                                                Locale.FRANCE);

    c.setStrength(CollationAttribute.VALUE_SECONDARY);

    String tests[] = {"\u0070\u00EA\u0063\u0068\u0065", "\u003c", 
                      "\u0070\u00E9\u0063\u0068\u00E9"};

    compareStrings(c, tests, tests.length);
  }
  
  /**
  * Testing bug 4065540
  * @exception thrown when error occurs while setting strength
  */
  public void Test4065540() throws Exception
  {
    if (m_collator_.equals("abcd e", "abcd f"))
      m_test_.errln("Failed : 'abcd e' != 'abcd f'");
  }
  
  /**
  * Testing bug 4066189
  * @exception thrown when error occurs while setting strength
  */
  public void Test4066189() throws Exception
  {
    String chars1 = "\u1EB1";
    String chars2 = "\u0061\u0306\u0300";
    String test1 = chars1;
    String test2 = chars2;

    RuleBasedCollator c1 = (RuleBasedCollator)m_collator_.clone();
    c1.setDecomposition(NormalizationMode.DECOMP_COMPAT);
    CollationElementIterator i1 = c1.getCollationElementIterator(test1);

    RuleBasedCollator c2 = (RuleBasedCollator)m_collator_.clone();
    c2.setDecomposition(NormalizationMode.NO_NORMALIZATION);
    CollationElementIterator i2 = c2.getCollationElementIterator(test2);

    int ce1 = 1,
        ce2;
    while (ce1 != CollationElementIterator.NULLORDER);
    {
      ce1 = i1.next();
      ce2 = i2.next();

      if (ce1 != ce2)
      {
        m_test_.errln("Failed : \u1EB1 == \u0061\u0306\u0300");
        return;
      }
    }
  }
  
  /**
  * Testing bug 4066696
  * @exception thrown when error occurs while setting strength
  */
  public void Test4066696() throws Exception
  {
    RuleBasedCollator c = (RuleBasedCollator)Collator.getInstance(
                                                                Locale.FRANCE);

    c.setStrength(CollationAttribute.VALUE_SECONDARY);

    String tests[] = {"\u00E0", "\u003e", "\u01FA"};

    compareStrings(c, tests, tests.length);
  }
  
  /**
  * Testing bug 4076676
  * @exception thrown when error occurs while setting strength
  */
  public void Test4076676() throws Exception
  {
    // These combining characters are all in the same class, so they should not
    // be reordered, and they should compare as unequal.
    String s1 = "\u0041\u0301\u0302\u0300";
    String s2 = "\u0041\u0302\u0300\u0301";

    RuleBasedCollator c = (RuleBasedCollator)m_collator_.clone();
    c.setStrength(CollationAttribute.VALUE_TERTIARY);

    if (c.equals(s1, s2))
      m_test_.errln("Failed : Reordered combining chars of the same class " + 
                    "are not equal");
  }
  
  /**
  * Testing bug 4078588
  * @exception thrown when error occurs while setting strength
  */
  public void Test4078588() throws Exception
  {
    RuleBasedCollator rbc = new RuleBasedCollator("< a < bb");

    int result = rbc.compare("a", "bb");

    if (result != Collator.RESULT_LESS)
       m_test_.errln("Failed : a < bb");
  }
  
  /**
  * Testing bug 4081866
  * @exception thrown when error occurs while setting strength
  */
  public void Test4081866() throws Exception
  {
    // These combining characters are all in different classes,
    // so they should be reordered and the strings should compare as equal.
    String s1 = "\u0041\u0300\u0316\u0327\u0315";
    String s2 = "\u0041\u0327\u0316\u0315\u0300";

    RuleBasedCollator c = (RuleBasedCollator)m_collator_.clone();
    c.setStrength(CollationAttribute.VALUE_TERTIARY);
    
    // Now that the default collators are set to NO_DECOMPOSITION
    // (as a result of fixing bug 4114077), we must set it explicitly
    // when we're testing reordering behavior.
    c.setDecomposition(NormalizationMode.DECOMP_CAN);

    if (!c.equals(s1, s2))
      m_test_.errln("Failed : \u0041\u0300\u0316\u0327\u0315 = " +
            "\u0041\u0327\u0316\u0315\u0300");
  }
  
  /**
  * Testing bug 4087241
  * @exception thrown when error occurs while setting strength
  */
  public void Test4087241() throws Exception
  {
    Locale da_DK = new Locale("da", "DK");
    RuleBasedCollator c = (RuleBasedCollator)Collator.getInstance(da_DK);

    c.setStrength(CollationAttribute.VALUE_SECONDARY);

    String tests[] = {"\u007a", "\u003c", "\u00E6", // z < ae
                      // a-unlaut < a-ring
                      "\u0061\u0308", "\u003c", "\u0061\u030A", 
                      "\u0059", "\u003c", "\u0075\u0308"}; // Y < u-umlaut

    compareStrings(c, tests, tests.length);
  }
  
  /**
  * Testing bug 4087243
  * @exception thrown when error occurs while setting strength
  */
  public void Test4087243() throws Exception
  {
    RuleBasedCollator c = (RuleBasedCollator)m_collator_.clone();
    c.setStrength(CollationAttribute.VALUE_TERTIARY);

    String tests[] = {"\u0031\u0032\u0033", "\u003d", 
                      "\u0031\u0032\u0033\u0001"};  // 1 2 3  =  1 2 3 ctrl-A
                          
    compareStrings(c, tests, tests.length);
  }
  
  /**
  * Testing bug 4092260
  * @exception thrown when error occurs while setting strength
  */
  public void Test4092260() throws Exception
  {
    Locale el = new Locale("el", "");
    Collator c = Collator.getInstance(el);

    String tests[] = {"\u00B5", "\u003d", "\u03BC"};

    compareStrings(c, tests, tests.length);
  }
  
  /**
  * Testing bug 4095316
  * @exception thrown when error occurs while setting strength
  */
  public void Test4095316() throws Exception
  {
    Locale el = new Locale("el", "GR");
    Collator c = Collator.getInstance(el);
    c.setStrength(CollationAttribute.VALUE_TERTIARY);

    String tests[] = {"\u03D4", "\u003d", "\u03AB"};

    compareStrings(c, tests, tests.length);
  }
  
  /**
  * Testing bug 4101940
  * @exception thrown when error occurs while setting strength
  */
  public void Test4101940() throws Exception
  {
    RuleBasedCollator c = new RuleBasedCollator("< a < b");
   
    CollationElementIterator i = c.getCollationElementIterator("");
    i.reset();

    if (i.next() != CollationElementIterator.NULLORDER)
      m_test_.errln("Failed : next did not return NULLORDER");
  }
  
  /**
  * Testing bug 4103436
  * @exception thrown when error occurs while setting strength
  */
  public void Test4103436() throws Exception
  {
    RuleBasedCollator c = (RuleBasedCollator)m_collator_.clone();
    c.setStrength(CollationAttribute.VALUE_TERTIARY);

    String tests[] = { "\u0066\u0069\u006c\u0065", "\u003c", 
          "\u0066\u0069\u006c\u0065\u0020\u0061\u0063\u0063\u0065\u0073\u0073",
          "\u0066\u0069\u006c\u0065", "\u003c", 
          "\u0066\u0069\u006c\u0065\u0061\u0063\u0063\u0065\u0073\u0073"};

    compareStrings(c, tests, tests.length);
  }
  
  /**
  * Testing bug 4114076
  * @exception thrown when error occurs while setting strength
  */
  public void Test4114076() throws Exception
  {
    RuleBasedCollator c = (RuleBasedCollator)m_collator_.clone();
    c.setStrength(CollationAttribute.VALUE_TERTIARY);

    String tests[] = { "\ud4db", "\u003d", "\u1111\u1171\u11b6"};

    c.setDecomposition(NormalizationMode.DECOMP_CAN);
    compareStrings(c, tests, tests.length);
  }
  
  /**
  * Testing bug 4124632
  * @exception thrown when error occurs while setting strength
  */
  public void Test4124632() throws Exception
  {
    Collator c = Collator.getInstance(Locale.JAPAN);
    String test = "\u0041\u0308\u0062\u0063";
    CollationKey key = c.getCollationKey(test);
  }
  
  /**
  * Testing bug 4132736
  * @exception thrown when error occurs while setting strength
  */
  public void Test4132736() throws Exception
  {
    Collator c = Collator.getInstance(Locale.FRANCE);
    String tests[] = {"\u0065\u0300\u0065\u0301", "\u003c", 
                      "\u0065\u0301\u0065\u0300", "\u0065\u0300\u0301",       
                      "\u003c", "\u0065\u0301\u0300"};
    compareStrings(c, tests, tests.length);
  }
  
  /**
  * Testing bug 4133509
  * @exception thrown when error occurs while setting strength
  */
  public void Test4133509() throws Exception
  {
    Collator c = Collator.getInstance(Locale.FRANCE);
    String tests[] = {
        "\u0045\u0078\u0063\u0065\u0070\u0074\u0069\u006f\u006e", "\u003c", 
        "\u0045\u0078\u0063\u0065\u0070\u0074\u0069\u006f\u006e\u0049\u006e\u0049\u006e\u0069\u0074\u0069\u0061\u006c\u0069\u007a\u0065\u0072\u0045\u0072\u0072\u006f\u0072",
        "\u0047\u0072\u0061\u0070\u0068\u0069\u0063\u0073", "\u003c", 
        "\u0047\u0072\u0061\u0070\u0068\u0069\u0063\u0073\u0045\u006e\u0076\u0069\u0072\u006f\u006e\u006d\u0065\u006e\u0074",
        "\u0053\u0074\u0072\u0069\u006e\u0067", "\u003c", 
        "\u0053\u0074\u0072\u0069\u006e\u0067\u0042\u0075\u0066\u0066\u0065\u0072"
    };
    compareStrings(m_collator_, tests, tests.length);
  }
  
  /**
  * Testing bug 4114077
  * @exception thrown when error occurs while setting strength
  */
  public void Test4114077() throws Exception
  {
    // Ensure that we get the same results with decomposition off
    // as we do with it on....
    
    RuleBasedCollator c = (RuleBasedCollator)m_collator_.clone();
    c.setStrength(CollationAttribute.VALUE_TERTIARY);
    
    String test1[] =
    {
      "\u00C0", "\u003d", "\u0041\u0300",   // Should be equivalent
      "\u0070\u00ea\u0063\u0068\u0065", "\u003e", 
      "\u0070\u00e9\u0063\u0068\u00e9",
      "\u0204", "\u003d", "\u0045\u030F",
      // a-ring-acute -> a-ring, acute
      "\u01fa", "\u003d", "\u0041\u030a\u0301", 
      // -> a, ring, acute
      // No reordering --> unequal
      "\u0041\u0300\u0316", "\u003c", "\u0041\u0316\u0300" 
    };

    c.setDecomposition(NormalizationMode.NO_NORMALIZATION);
    compareStrings(c, test1, test1.length);

    String test2[] = {"\u0041\u0300\u0316", "\u003d", "\u0041\u0316\u0300"};
                      // Reordering --> equal                  

    c.setDecomposition(NormalizationMode.DECOMP_CAN);
    compareStrings(c, test2, test2.length);
  }
  
  /**
  * Testing bug 4139572
  * @exception thrown when error occurs while setting strength
  */
  public void Test4139572() throws Exception
  {
    // Rather than just creating a Swedish collator, we might as well
    // try to instantiate one for every locale available on the system
    // in order to prevent this sort of bug from cropping up in the future
    // Code pasted straight from the bug report
    // (and then translated to C++ ;-)
    Locale l = new Locale("es", "es");
    Collator col = Collator.getInstance(l);

    CollationKey key = col.getCollationKey("Nombre De Objeto");
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
  * Source strings for testing
  */
  private final String TEST_STRING_1_ = "XFILE What subset of all possible " +
       "test cases has the highest probability of detecting the most errors?";
  private final String TEST_STRING_2_ = "Xf ile What subset of all " +
    "possible test cases has the lowest probability of detecting the least " +
    "errors?";
  private final char CHAR_ARRAY_[] = {0x0061, 0x00FC, 0x0062, 0x0065, 0x0063, 
                                      0x006b, 0x0020, 0x0047, 0x0072, 0x00F6, 
                                      0x00DF, 0x0065, 0x0020, 0x004c, 0x00FC, 
                                      0x0062, 0x0063, 0x006b
                                      };
  private final String TEST_STRING_3_ = new String(CHAR_ARRAY_);
  
  // private methods ------------------------------------------------------
  
  /**
  * Comparing strings
  */
  private void compareStrings(Collator c, String tests[], int testcount)
  {
    int expectedResult = Collator.RESULT_EQUAL;

    for (int i = 0; i < testcount; i += 3)
    {
      String source = tests[i];
      String comparison = tests[i + 1];
      String target = tests[i + 2];

      if (comparison.equals("<"))
        expectedResult = Collator.RESULT_LESS;
      else 
        if (comparison.equals(">"))
          expectedResult = Collator.RESULT_GREATER;
        else 
          if (comparison.equals("="))
            expectedResult = Collator.RESULT_EQUAL;
          else
            m_test_.errln("Failed : Bogus comparison string \"" + comparison
                          + "\"");         

      CollationKey sourceKey, targetKey;
      
      sourceKey = c.getCollationKey(source);
      targetKey = c.getCollationKey(target);

      if (sourceKey.compareTo(targetKey) != expectedResult)
      {
        m_test_.errln("Failed : String comparison of " + source + " and " +
                      target + " should be " + expectedResult);
        return;
      }
    }
  }
}

