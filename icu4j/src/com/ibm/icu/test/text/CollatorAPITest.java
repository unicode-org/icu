/**
*******************************************************************************
* Copyright (C) 1996-2000, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: 
*  /usr/cvs/icu4j/icu4j/src/com/ibm/icu/test/text/CollatorAPITest.java,v $ 
* $Date: 2001/03/09 23:41:46 $ 
* $Revision: 1.2 $
*
*******************************************************************************
*/

package com.ibm.icu4jni.test.text;

import java.util.Locale;
import com.ibm.icu4jni.text.Collator;
import com.ibm.icu4jni.text.RuleBasedCollator;
import com.ibm.icu4jni.text.CollationKey;
import com.ibm.icu4jni.text.CollationAttribute;
import com.ibm.icu4jni.text.CollationElementIterator;
import com.ibm.icu4jni.text.NormalizationMode;

/**
* Collator API testing class
* Mostly following the test cases for ICU
* @author Syn Wee Quek
* @since jan 29 2001
*/
public final class CollatorAPITest 
{ 
  
  // constructor ===================================================
  
  /**
  * Constructor
  */
  public CollatorAPITest(CollatorTest testprogram) throws Exception
  {
    m_test_ = testprogram;
    // m_collator_ = Collator.getInstance();
  }
  
  // public methods ================================================

  /**
  * Testing collator class properties.
  * Constructor, compare, strength retrieval/set, decomposition 
  * retrievale/set
  * @exception thrown when error occurs while setting strength
  */
  public void TestProperties() throws Exception
  {
    m_test_.logln("TestProperties --");
    byte minversion[] = {0x01, 0x00, 0x00, 0x00};
    byte maxversion[] = {0x01, 0x09, 0x09, 0x09};
    byte version[];
    Collator defaultcollator = Collator.getInstance();
    
    if (defaultcollator.compare("ab", "abc") != Collator.RESULT_LESS)
      m_test_.errln("Failed : ab < abc comparison");
    if (defaultcollator.compare("ab", "AB") != Collator.RESULT_LESS)
      m_test_.errln("Failed : ab < AB");
    if (defaultcollator.compare("black-bird", "blackbird") != 
                                                Collator.RESULT_GREATER)
      m_test_.errln("Failed : black-bird > blackbird comparison");
    if (defaultcollator.compare("black bird", "black-bird") != 
                                                   Collator.RESULT_LESS)
      m_test_.errln("Failed : black bird > black-bird comparison");
    if (defaultcollator.compare("Hello", "hello") != 
                                                Collator.RESULT_GREATER)
      m_test_.errln("Failed : Hello > hello comparison");

    if (defaultcollator.getStrength() != CollationAttribute.VALUE_TERTIARY)
      m_test_.errln("Failed : Default collation have tertiary strength");
        
    defaultcollator.setStrength(CollationAttribute.VALUE_SECONDARY);
    if (defaultcollator.getStrength() != CollationAttribute.VALUE_SECONDARY)
      m_test_.errln("Failed : Collation strength set to secondary");
   
    defaultcollator.setDecomposition(NormalizationMode.NO_NORMALIZATION);
    if (defaultcollator.getDecomposition() != 
                                       NormalizationMode.NO_NORMALIZATION)
      m_test_.errln("Failed : Collation strength set to no normalization");

    if (((RuleBasedCollator)defaultcollator).getRules().length() == 0)
      m_test_.errln("Failed : RuleBasedCollator getRules() result");                
    
    Collator collator = Collator.getInstance(Locale.FRENCH);
    
    collator.setStrength(CollationAttribute.VALUE_PRIMARY);
    if (collator.getStrength() != CollationAttribute.VALUE_PRIMARY)
      m_test_.errln("Failed : Collation strength set to primary");
      
    collator.setStrength(CollationAttribute.VALUE_TERTIARY);
    if (defaultcollator.getStrength() != CollationAttribute.VALUE_TERTIARY)
      m_test_.errln("Failed : Collation strength set to tertiary");

    // testing rubbish collator
    // should return the default
    Locale abcd = new Locale("ab", "CD");
    collator = Collator.getInstance(abcd);
    defaultcollator = Collator.getInstance();

    if (!collator.equals(defaultcollator))
      m_test_.errln("Failed : Undefined locale should return the default " +
                    "collator");
                    
    Collator frenchcollator = Collator.getInstance(Locale.FRANCE);
    if (!frenchcollator.equals(collator))
      m_test_.errln("Failed : Undefined locale should return the default " +
                    "collator");
                    
    Collator clonefrench = (Collator)frenchcollator.clone();
    if (!frenchcollator.equals(clonefrench))
      m_test_.errln("Failed : Cloning of a French collator");
  }

  /**
  * Testing hash code method
  * @exception thrown when error occurs while setting strength
  */
  public void TestHashCode() throws Exception
  {
    m_test_.logln("TestHashCode --");

    Locale dk = new Locale("da", "DK");
    Collator collator = Collator.getInstance(dk);
    
    Collator defaultcollator = Collator.getInstance();
    
    if (defaultcollator.hashCode() == collator.hashCode())
      m_test_.errln("Failed : Default collator's hash code not equal to " +
                    "Danish collator's hash code");                 
    if (defaultcollator.hashCode() != defaultcollator.hashCode())
      m_test_.errln("Failed : Hash code of two default collators are equal");               
  }
  
  /**
  * Test collation key
  * @exception thrown when error occurs while setting strength
  */
  public void TestCollationKey() throws Exception
  {
    m_test_.logln("TestCollationKey --");

    String test1 = "Abcda", 
           test2 = "abcda";
    
    Collator defaultcollator = Collator.getInstance();
    CollationKey sortk1 = defaultcollator.getCollationKey(test1), 
                 sortk2 = defaultcollator.getCollationKey(test2);
    if (sortk1.compareTo(sortk2) != Collator.RESULT_GREATER)
      m_test_.errln("Failed : Abcda >>> abcda");

    if (sortk1.equals(sortk2))
      m_test_.errln("Failed : The sort keys of different strings should be " +
                    "different");
    if (sortk1.hashCode() == sortk2.hashCode())
      m_test_.errln("Failed : sort key hashCode() for different strings " +
                    "should be different");
  }
  
  /**
  * Testing the functionality of the collation element iterator
  * @exception thrown when error occurs while setting strength
  */
  public void TestElementIterator() throws Exception
  {       
    m_test_.logln("TestElementIterator --");

    String test1 = "XFILE What subset of all possible test cases has the " +
                   "highest probability of detecting the most errors?";
    String test2 = "Xf ile What subset of all possible test cases has the " +
                   "lowest probability of detecting the least errors?";
    Collator defaultcollator = Collator.getInstance();
    
    CollationElementIterator iterator1 = 
      ((RuleBasedCollator)defaultcollator).getCollationElementIterator(
                                                                       test1);
    
    // copy ctor
    CollationElementIterator iterator2 = 
      ((RuleBasedCollator)defaultcollator).getCollationElementIterator(
                                                                       test2);
    if (iterator1.equals(iterator2))
      m_test_.errln("Failed : Two iterators with different strings should " +
                    "be different");
    
    int order1 = iterator1.next();
    int order2 = iterator2.next();
    
    if (CollationElementIterator.primaryOrder(order1) != 
        CollationElementIterator.primaryOrder(order2))
      m_test_.errln("Failed : The primary orders should be the same");
    if (CollationElementIterator.secondaryOrder(order1) != 
        CollationElementIterator.secondaryOrder(order2))
      m_test_.errln("Failed : The secondary orders should be the same");
    if (CollationElementIterator.tertiaryOrder(order1) == 
        CollationElementIterator.tertiaryOrder(order2))
      m_test_.errln("Failed : The tertiary orders should be the same");

    order1 = iterator1.next(); 
    order2 = iterator2.next();
    
    if (CollationElementIterator.primaryOrder(order1) != 
        CollationElementIterator.primaryOrder(order2))
      m_test_.errln("Failed : The primary orders should be identical");
    if (CollationElementIterator.tertiaryOrder(order1) == 
        CollationElementIterator.tertiaryOrder(order2))
      m_test_.errln("Failed : The tertiary orders should be different");

    order1 = iterator1.next(); 
    order2 = iterator2.next();
    if (CollationElementIterator.secondaryOrder(order1) == 
        CollationElementIterator.secondaryOrder(order2))
      m_test_.errln("Failed : The secondary orders should be different");
    if (order1 == CollationElementIterator.NULLORDER)
      m_test_.errln("Failed : Unexpected end of iterator reached");

    iterator1.reset(); 
    iterator2.reset();
    order1 = iterator1.next();
    order2 = iterator2.next();
    
    if (CollationElementIterator.primaryOrder(order1) != 
        CollationElementIterator.primaryOrder(order2))
      m_test_.errln("Failed : The primary orders should be the same");
    if (CollationElementIterator.secondaryOrder(order1) != 
        CollationElementIterator.secondaryOrder(order2))
      m_test_.errln("Failed : The secondary orders should be the same");
    if (CollationElementIterator.tertiaryOrder(order1) == 
        CollationElementIterator.tertiaryOrder(order2))
      m_test_.errln("Failed : The tertiary orders should be the same");

    order1 = iterator1.next(); 
    order2 = iterator2.next();
    
    if (CollationElementIterator.primaryOrder(order1) != 
        CollationElementIterator.primaryOrder(order2))
      m_test_.errln("Failed : The primary orders should be identical");
    if (CollationElementIterator.tertiaryOrder(order1) == 
        CollationElementIterator.tertiaryOrder(order2))
      m_test_.errln("Failed : The tertiary orders should be different");

    order1 = iterator1.next(); 
    order2 = iterator2.next();
    if (CollationElementIterator.secondaryOrder(order1) == 
        CollationElementIterator.secondaryOrder(order2))
      m_test_.errln("Failed : The secondary orders should be different");
    if (order1 == CollationElementIterator.NULLORDER)
      m_test_.errln("Failed : Unexpected end of iterator reached");

    //test error values
    iterator1.setText("hello there");
    if (iterator1.previous() != CollationElementIterator.NULLORDER)
      m_test_.errln("Failed : Retrieval of previous value in a new iterator "
                    + "has to return a NULLORDER");
    
    String rule = "< a, A < b, B < c, C < d, D, e, E";
    try
    {
      Collator collator = new RuleBasedCollator(rule);
      m_test_.errln("Failed : RuleBasedCollator can't have " + rule +
                    " as its rule");
    }
    catch (Exception e)
    {
    }
    try
    {
      Collator collator = new RuleBasedCollator(rule, 
                                               CollationAttribute.VALUE_PRIMARY);
      m_test_.errln("Failed : RuleBasedCollator can't have " + rule +
                    " as its rule");
    }
    catch (Exception e)
    {
    }
    try
    {
      Collator collator = new RuleBasedCollator(rule, 
                                  NormalizationMode.NO_NORMALIZATION);
      m_test_.errln("Failed : RuleBasedCollator can't have " + rule +
                    " as its rule");
    }
    catch (Exception e)
    {
    }
    try
    {
      Collator collator = new RuleBasedCollator(rule, 
                                         CollationAttribute.VALUE_SECONDARY);
      m_test_.errln("Failed : RuleBasedCollator can't have " + rule +
                    " as its rule");
    }
    catch (Exception e)
    {
    }
  }

  /** 
  * Test RuleBasedCollator constructor, clone, copy, and getRules
  * @exception thrown when error occurs while setting strength
  */
  public void TestOperators() throws Exception
  {
    m_test_.logln("TestOperators --");

    String ruleset1 = "< a, A < b, B < c, C; ch, cH, Ch, CH < d, D, e, E";
    String ruleset2 = "< a, A < b, B < c, C < d, D, e, E";
    RuleBasedCollator col1 = new RuleBasedCollator(ruleset1);
    
    RuleBasedCollator col2 = new RuleBasedCollator(ruleset2);
    
    if (col1.equals(col2))
      m_test_.errln("Failed : Two different rule collations should return " +
                    "different comparisons");
    
    Collator col3 = Collator.getInstance();
    
    Collator col4 = (Collator)col1.clone();
    Collator col5 = (Collator)col3.clone();
    
    if (!col1.equals(col4))
      m_test_.errln("Failed : Cloned collation objects should be equal");
    if (col3.equals(col4))
      m_test_.errln("Failed : Two different rule collations should compare " +
                    "different");
    if (col3.equals(col5)) 
      m_test_.errln("Failed : Cloned collation objects should be equal");
    if (col4.equals(col5))
      m_test_.errln("Failed : Clones of 2 different collations should " +
                    "compare different");

    String defrules = ((RuleBasedCollator)col3).getRules();
    RuleBasedCollator col6 = new RuleBasedCollator(defrules);
    if (!((RuleBasedCollator)col3).getRules().equals(col6.getRules())) 
      m_test_.errln("Failed : Rules from one collator should create a same " +
                    "collator");

    RuleBasedCollator col7 = new RuleBasedCollator(ruleset2, 
                                           CollationAttribute.VALUE_TERTIARY);
    RuleBasedCollator col8 = new RuleBasedCollator(ruleset2, 
                                  NormalizationMode.NO_NORMALIZATION);
    RuleBasedCollator col9 = new RuleBasedCollator(ruleset2, 
      CollationAttribute.VALUE_PRIMARY, NormalizationMode.DECOMP_COMPAT);
    
    if (col7.equals(col9))
      m_test_.errln("Failed : Two different rule collations should compare " +
                    "different");
    if (col8.equals(col9))
      m_test_.errln("Failed : Two different rule collations should compare " +
                    "equal");
  }

  /**
  * Test clone and copy
  * @exception thrown when error occurs while setting strength
  */
  public void TestDuplicate() throws Exception
  {
    m_test_.logln("TestDuplicate --");

    Collator defaultcollator = Collator.getInstance();
    Collator col2 = (Collator)defaultcollator.clone();
    if (!defaultcollator.equals(col2))
      m_test_.errln("Failed : Cloned object should be equal to the orginal");
    String ruleset = "< a, A < b, B < c, C < d, D, e, E";
    RuleBasedCollator col3 = new RuleBasedCollator(ruleset);
    if (defaultcollator.equals(col3))
      m_test_.errln("Failed : Cloned object not equal to collator created " + 
                    "by rules");
  }   

  /**
  * Testing compare methods
  * @exception thrown when error occurs while setting strength
  */
  public void TestCompare() throws Exception
  {
    m_test_.logln("TestCompare --");
    
    String test1 = "Abcda", 
           test2 = "abcda";
           
    Collator defaultcollator = Collator.getInstance();
    
    if (defaultcollator.compare(test1, test2) != Collator.RESULT_GREATER)
      m_test_.errln("Failed : Result should be Abcda >>> abcda");
    
    defaultcollator.setStrength(CollationAttribute.VALUE_SECONDARY);
    
    if (defaultcollator.compare(test1, test2) != Collator.RESULT_EQUAL)
      m_test_.errln("Failed : Result should be Abcda == abcda");
    
    defaultcollator.setStrength(CollationAttribute.VALUE_PRIMARY);
    
    if (!defaultcollator.equals(test1, test2))
      m_test_.errln("Failed : Result should be Abcda == abcda");
  }

  // private variables =============================================
  
  /**
  * Main Collation test program
  */
  private CollatorTest m_test_;
  
  /**
  * Test collator
  */
  // private Collator m_collator_;
  
  /**
  * Source string for testing
  */
  private static final String SOURCE_TEST_CASE_ = 
                                           "-abcdefghijklmnopqrstuvwxyz#&^$@";
}

