/**
*******************************************************************************
* Copyright (C) 1996-2000, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/text/Collator.java,v $ 
* $Date: 2001/03/09 23:42:30 $ 
* $Revision: 1.2 $
*
*******************************************************************************
*/

package com.ibm.icu4jni.text;

import java.util.Locale;
import com.ibm.icu4jni.text.RuleBasedCollator;

/**
* Abstract class for C Collation.
* Considerations :
* 1) ErrorCode not returned to user throw exceptions instead
* 2) Similar API to java.text.Collator
* @author syn wee quek
* @since Jan 17 01
*/

public abstract class Collator implements Cloneable
{ 
  // public data member -------------------------------------------
  
  // Collation result constants -----------------------------------
  // corresponds to ICU's UCollationResult enum balues
  /** 
  * string a == string b 
  */
  public static final int RESULT_EQUAL = 0;
  /** 
  * string a > string b 
  */
  public static final int RESULT_GREATER = 1;
  /** 
  * string a < string b 
  */
  public static final int RESULT_LESS = -1;
  /** 
  * accepted by most attributes 
  */
  public static final int RESULT_DEFAULT = -1;
  
  // public methods -----------------------------------------------
  
  /**
  * Factory method to create an appropriate Collator which uses the default
  * locale collation rules.
  * Current implementation createInstance() returns a RuleBasedCollator(Locale) 
  * instance. The RuleBasedCollator will be created in the following order,
  * <ul>
  * <li> Data from argument locale resource bundle if found, otherwise
  * <li> Data from parent locale resource bundle of arguemtn locale if found,
  *      otherwise
  * <li> Data from built-in default collation rules if found, other
  * <li> null is returned
  * </ul>
  * @return an instance of Collator
  */
  public static Collator getInstance()
  {
    return getInstance(Locale.getDefault());
  }

  /**
  * Factory method to create an appropriate Collator which uses the argument
  * locale collation rules.<br>
  * Current implementation createInstance() returns a RuleBasedCollator(Locale) 
  * instance. The RuleBasedCollator will be created in the following order,
  * <ul>
  * <li> Data from argument locale resource bundle if found, otherwise
  * <li> Data from parent locale resource bundle of arguemtn locale if found,
  *      otherwise
  * <li> Data from built-in default collation rules if found, other
  * <li> null is returned
  * </ul>
  * @param locale to be used for collation
  * @return an instance of Collator
  */
  public static Collator getInstance(Locale locale)
  {
    RuleBasedCollator result = new RuleBasedCollator(locale);
    return result;
  }

  /**
  * Equality check for the argument strings.
  * @param source string
  * @param target string
  * @return true if source is equivalent to target, false otherwise 
  */
  public boolean equals(String source, String target)
  {
    return (compare(source, target) == RESULT_EQUAL);
  }
  
  /**
  * Checks if argument object is equals to this object.
  * @param target object
  * @return true if source is equivalent to target, false otherwise 
  */
  public abstract boolean equals(Object target);
  
  /**
  * Makes a copy of the current object.
  * @return a copy of this object
  */
  public abstract Object clone() throws CloneNotSupportedException;
  
  /**
  * The comparison function compares the character data stored in two
  * different strings. Returns information about whether a string is less 
  * than, greater than or equal to another string.
  * <p>Example of use:
  * <pre>
  * .  Collator myCollation = Collator.createInstance(Locale::US);
  * .  myCollation.setStrength(Collation.PRIMARY);
  * .  // result would be Collation.EQUAL ("abc" == "ABC")
  * .  // (no primary difference between "abc" and "ABC")
  * .  int result = myCollation.compare("abc", "ABC",3);
  * .  myCollation.setStrength(Collation.TERTIARY);
  * .  // result would be Collation.LESS (abc" &lt;&lt;&lt; "ABC")
  * .  // (with tertiary difference between "abc" and "ABC")
  * .  int result = myCollation.compare("abc", "ABC",3);
  * </pre>
  * @param source source string.
  * @param target target string.
  * @return result of the comparison, Collation.EQUAL, Collation.GREATER
  *         or Collation.LESS
  */
  public abstract int compare(String source, String target);
                                               
  /**
  * Get the decomposition mode of this Collator
  * Return values from com.ibm.icu4jni.text.Normalization.
  * @return the decomposition mode
  */
  public abstract int getDecomposition();

  /**
  * Set the decomposition mode of the Collator object. 
  * Argument values from com.ibm.icu4jni.text.Normalization.
  * @param decompositionmode the new decomposition mode
  */
  public abstract void setDecomposition(int mode);

  /**
  * Determines the minimum strength that will be use in comparison or
  * transformation.
  * <p>
  * E.g. with strength == Collation.SECONDARY, the tertiary difference 
  * is ignored
  * </p>
  * <p>
  * E.g. with strength == PRIMARY, the secondary and tertiary difference are 
  * ignored.
  * </p>
  * @return the current comparison level.
  */
  public abstract int getStrength();
  
  /**
  * Sets the minimum strength to be used in comparison or transformation.
  * <p>Example of use:
  * <pre>
  * . Collator myCollation = Collator.createInstance(Locale::US);
  * . myCollation.setStrength(Collation.PRIMARY);
  * . // result will be "abc" == "ABC"
  * . // tertiary differences will be ignored
  * . int result = myCollation->compare("abc", "ABC");
  * </pre>
  * @param strength the new comparison level.
  */
  public abstract void setStrength(int strength);
  
  /**
  * Get the sort key as an CollationKey object from the argument string.
  * To retrieve sort key in terms of byte arrays, use the method as below<br>
  * <code>
  * Collator collator = Collator.getInstance();
  * CollationKey collationkey = collator.getCollationKey("string");
  * byte[] array = collationkey.toByteArray();
  * </code><br>
  * Byte array result are zero-terminated and can be compared using 
  * java.util.Arrays.equals();
  * @param source string to be processed.
  * @return the sort key
  */
  public abstract CollationKey getCollationKey(String source);
  
  /**
  * Returns a hash of this collation object
  * @return hash of this collation object
  */
  public abstract int hashCode();
}
