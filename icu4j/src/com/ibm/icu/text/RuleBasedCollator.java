/**
*******************************************************************************
* Copyright (C) 1996-2000, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/text/RuleBasedCollator.java,v $ 
* $Date: 2001/03/09 23:42:30 $ 
* $Revision: 1.2 $
*
*******************************************************************************
*/

package com.ibm.icu4jni.text;

import java.util.Locale;
import java.text.ParseException;
import com.ibm.icu4jni.common.ErrorCode;

/**
* Concrete implementation class for Collation.
* @author syn wee quek
* @since Jan 17 01
*/
    
public final class RuleBasedCollator extends Collator 
{
  // public constructors ------------------------------------------
  
  /**
  * RuleBasedCollator constructor. This takes the table rules and builds a 
  * collation table out of them. Please see RuleBasedCollator class
  * description for more details on the collation rule syntax.
  * @param rules the collation rules to build the collation table from.
  * @exception ParseException thrown if rules are empty or a Runtime error
  *            if collator can not be created.
  */
  public RuleBasedCollator(String rules) throws ParseException
  {
    
    if (rules.length() == 0)
      throw new ParseException("Build rules empty.", 0);
    m_collator_ = NativeCollation.openCollatorFromRules(rules,
                              NormalizationMode.DEFAULT_NORMALIZATION,
                              CollationAttribute.VALUE_DEFAULT_STRENGTH);
  }

  /**
  * RuleBasedCollator constructor. This takes the table rules and builds a 
  * collation table out of them. Please see RuleBasedCollator class
  * description for more details on the collation rule syntax.
  * @param rules the collation rules to build the collation table from.
  * @param strength collation strength
  * @exception ParseException thrown if rules are empty or a Runtime error
  *            if collator can not be created.
  */
  public RuleBasedCollator(String rules, int strength) throws ParseException
  {
    if (rules.length() == 0)
      throw new ParseException("Build rules empty.", 0);
    if (!CollationAttribute.checkStrength(strength))
      throw ErrorCode.getException(ErrorCode.U_ILLEGAL_ARGUMENT_ERROR);
      
    m_collator_ = NativeCollation.openCollatorFromRules(rules,
                                NormalizationMode.DEFAULT_NORMALIZATION,
                              strength);
  }

  /**
  * RuleBasedCollator constructor. This takes the table rules and builds a 
  * collation table out of them. Please see RuleBasedCollator class
  * description for more details on the collation rule syntax.
  * @param rules the collation rules to build the collation table from.
  * @param strength collation strength
  * @param normalizationmode normalization mode
  * @exception thrown when constructor error occurs
  */
  public RuleBasedCollator(String rules, int normalizationmode, int strength)
  {
    if (!CollationAttribute.checkStrength(strength) || 
        !NormalizationMode.check(normalizationmode)) {
      throw ErrorCode.getException(ErrorCode.U_ILLEGAL_ARGUMENT_ERROR);
    }
      
    m_collator_ = NativeCollation.openCollatorFromRules(rules,
                                          normalizationmode, strength);
  }
  
  // public methods -----------------------------------------------
  
  /**
  * Makes a complete copy of the current object.
  * @return a copy of this object if data clone is a success, otherwise null
  */
  public Object clone() 
  {
    RuleBasedCollator result = null;
    long collatoraddress = NativeCollation.safeClone(m_collator_);
    result = new RuleBasedCollator(collatoraddress);
    return (Collator)result;
  }
                              
  /**
  * The comparison function compares the character data stored in two
  * different strings. Returns information about whether a string is less 
  * than, greater than or equal to another string.
  * <p>Example of use:
  * <pre>
  * .  Collator myCollation = Collator.createInstance(Locale::US);
  * .  myCollation.setStrength(CollationAttribute.VALUE_PRIMARY);
  * .  // result would be Collator.RESULT_EQUAL ("abc" == "ABC")
  * .  // (no primary difference between "abc" and "ABC")
  * .  int result = myCollation.compare("abc", "ABC",3);
  * .  myCollation.setStrength(CollationAttribute.VALUE_TERTIARY);
  * .  // result would be Collation::LESS (abc" &lt;&lt;&lt; "ABC")
  * .  // (with tertiary difference between "abc" and "ABC")
  * .  int result = myCollation.compare("abc", "ABC",3);
  * </pre>
  * @param source The source string.
  * @param target The target string.
  * @return result of the comparison, Collator.RESULT_EQUAL, 
  *         Collator.RESULT_GREATER or Collator.RESULT_LESS
  */
  public int compare(String source, String target)
  {
    return NativeCollation.compare(m_collator_, source, target);
  }
                                               
  /**
  * Get the normalization mode for this object.
  * The normalization mode influences how strings are compared.
  * @return normalization mode; one of the values from NormalizationMode
  */
  public int getDecomposition()
  {
    return NativeCollation.getNormalization(m_collator_);
  }

  /**
  * Set the normalization mode used int this object
  * The normalization mode influences how strings are compared.
  * @param normalizationmode desired normalization mode; one of the values 
  *        from NormalizationMode
  * @exception thrown when argument does not belong to any normalization mode
  */
  public void setDecomposition(int decompositionmode)
  {
    if (!NormalizationMode.check(decompositionmode)) 
      throw ErrorCode.getException(ErrorCode.U_ILLEGAL_ARGUMENT_ERROR);
    NativeCollation.setNormalization(m_collator_, decompositionmode);
  }

  /**
  * Determines the minimum strength that will be use in comparison or
  * transformation.
  * <p>
  * E.g. with strength == CollationAttribute.VALUE_SECONDARY, the tertiary difference 
  * is ignored
  * </p>
  * <p>
  * E.g. with strength == PRIMARY, the secondary and tertiary difference are 
  * ignored.
  * </p>
  * @return the current comparison level.
  */
  public int getStrength()
  {
    return NativeCollation.getAttribute(m_collator_, 
                                        CollationAttribute.STRENGTH);
  }
  
  /**
  * Sets the minimum strength to be used in comparison or transformation.
  * <p>Example of use:
  * <pre>
  * . Collator myCollation = Collator.createInstance(Locale::US);
  * . myCollation.setStrength(CollationAttribute.VALUE_PRIMARY);
  * . // result will be "abc" == "ABC"
  * . // tertiary differences will be ignored
  * . int result = myCollation->compare("abc", "ABC");
  * </pre>
  * @param strength the new comparison level.
  * @exception thrown when argument does not belong to any collation strength 
  *            mode or error occurs while setting data.
  */
  public void setStrength(int strength)
  {
    if (!CollationAttribute.checkStrength(strength)) 
      throw ErrorCode.getException(ErrorCode.U_ILLEGAL_ARGUMENT_ERROR);
    NativeCollation.setAttribute(m_collator_, CollationAttribute.STRENGTH, 
                                 strength);
  }
  
  /**
  * Get the sort key as an CollationKey object from the argument string.
  * To retrieve sort key in terms of byte arrays, use the method as below<br>
  * <code>
  * Collator collator = Collator.getInstance();
  * byte[] array = collator.getSortKey(source);
  * </code><br>
  * Byte array result are zero-terminated and can be compared using 
  * java.util.Arrays.equals();
  * @param source string to be processed.
  * @return the sort key
  */
  public CollationKey getCollationKey(String source)
  {
    return new CollationKey(NativeCollation.getSortKey(m_collator_, source));
  }
  
  /**
  * Get a sort key for the argument string
  * Sort keys may be compared using java.util.Arrays.equals
  * @param collatoraddress address of the C collator
  * @param source string for key to be generated
  * @return sort key
  */
  public byte[] getSortKey(String source)
  {
    return NativeCollation.getSortKey(m_collator_, source);
  }
  
  /**
  * Get the collation rules of this Collation object
  * The rules will follow the rule syntax.
  * @return collation rules.
  */
  public String getRules()
  {
    return NativeCollation.getRules(m_collator_);
  }
  
  /** 
  * Create a CollationElementIterator object that will iterator over the 
  * elements in a string, using the collation rules defined in this 
  * RuleBasedCollator
  * @param collatoraddress address of C collator
  * @param source string to iterate over
  * @return address of C collationelement
  * @exception thrown when error occurs
  */
  public CollationElementIterator getCollationElementIterator(String source)
  {
    CollationElementIterator result = new CollationElementIterator(
         NativeCollation.getCollationElementIterator(m_collator_, source));
    // result.setOwnCollationElementIterator(true);
    return result;
  }
                             
  /**
  * Returns a hash of this collation object
  * @return hash of this collation object
  */
  public int hashCode()
  {
    // since rules do not change once it is created, we can cache the hash
    if (m_hashcode_ == 0) {
      m_hashcode_ = NativeCollation.hashCode(m_collator_);
      if (m_hashcode_ == 0)
        m_hashcode_ = 1;
    }
    return m_hashcode_;
  }
  
  /**
  * Checks if argument object is equals to this object.
  * @param target object
  * @return true if source is equivalent to target, false otherwise 
  */
  public boolean equals(Object target)
  {
    if (this == target) 
      return true;
    if (target == null) 
      return false;
    if (getClass() != target.getClass()) 
      return false;
    
    long tgtcollatoraddress = ((RuleBasedCollator)target).m_collator_;
    return m_collator_ == tgtcollatoraddress;
  }
  
  // package constructor ----------------------------------------
  
  /**
  * RuleBasedCollator default constructor. This constructor takes the default 
  * locale. The only caller of this class should be Collator.getInstance(). 
  * Current implementation createInstance() returns a RuleBasedCollator(Locale) 
  * instance. The RuleBasedCollator will be created in the following order,
  * <ul>
  * <li> Data from argument locale resource bundle if found, otherwise
  * <li> Data from parent locale resource bundle of arguemtn locale if found,
  *      otherwise
  * <li> Data from built-in default collation rules if found, other
  * <li> null is returned
  * </ul>
  * @param desiredLocale locale used
  * @param status error code status
  */
  RuleBasedCollator()
  {
    Locale locale = Locale.getDefault();
    m_collator_ = NativeCollation.openCollator(locale.toString());
  }

  /**
  * RuleBasedCollator constructor. This constructor takes a locale. The 
  * only caller of this class should be Collator.createInstance(). 
  * Current implementation createInstance() returns a RuleBasedCollator(Locale) 
  * instance. The RuleBasedCollator will be created in the following order,
  * <ul>
  * <li> Data from argument locale resource bundle if found, otherwise
  * <li> Data from parent locale resource bundle of arguemtn locale if found,
  *      otherwise
  * <li> Data from built-in default collation rules if found, other
  * <li> null is returned
  * </ul>
  * @param desiredLocale locale used
  * @param status error code status
  */
  RuleBasedCollator(Locale locale)
  {
    m_collator_ = NativeCollation.openCollator(locale.toString());
  }
  
  // protected methods --------------------------------------------
  
  /**
  * Garbage collection.
  * Close C collator and reclaim memory.
  */
  protected void finalize()
  {
    NativeCollation.closeCollator(m_collator_);
  }
  
  // private data members -----------------------------------------
  
  /**
  * C collator
  */
  private long m_collator_;
  
  /**
  * Hash code for rules
  */
  private int m_hashcode_ = 0;
  
  // private constructor -----------------------------------------
  
  /**
  * Private use constructor.
  * Does not create any instance of the C collator. Accepts argument as the
  * C collator for new instance.
  * @param collatoraddress address of C collator
  */
  private RuleBasedCollator(long collatoraddress)
  {
    m_collator_ = collatoraddress;
  }
}