/**
*******************************************************************************
* Copyright (C) 1996-2000, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/text/Attic/NativeCollation.java,v $ 
* $Date: 2001/03/09 00:34:42 $ 
* $Revision: 1.1 $
*
*******************************************************************************
*/

package com.ibm.icu4jni.text;

import com.ibm.icu4jni.common.ErrorCode;

/**
* Package static class for declaring all native methods for collation use.
* @author syn wee quek
* @since Mar 05 2001
*/
    
final class NativeCollation
{
  // library loading ----------------------------------------------
  
  static {
    if (ErrorCode.LIBRARY_LOADED)
      System.out.println("test");
  }
  
  // collator methods ---------------------------------------------
  
  /**
  * Method to create a new C Collator using the argument locale rules.
  * @param locale locale name
  * @return new c collator
  */
  static native long openCollator(String locale);
  
  /**
  * Method to create a new C Collator using the argument rules.
  * @param rules, set of collation rules
  * @param normalizationmode default normalization mode
  * @param collationstrength default collation strength
  * @return new c collator
  */
  static native long openCollatorFromRules(String rules,
                                           int normalizationmode,
                                           int collationstrength);

  /** 
  * Close a C collator
  * Once closed, a UCollatorOld should not be used.
  * @param coll The UCollatorOld to close
  */
  static native void closeCollator(long collatoraddress);
  
  /**
  * Compare two strings.
  * The strings will be compared using the normalization mode and options
  * specified in openCollator or openCollatorFromRules
  * @param collatoraddress address of the c collator
  * @param source The source string.
  * @param target The target string.
  * @return result of the comparison, Collation.EQUAL, 
  *         Collation.GREATER or Collation.LESS
  */
  static native int compare(long collatoraddress, String source, 
                            String target);
                             
  /**
  * Get the normalization mode for this object.
  * The normalization mode influences how strings are compared.
  * @param collatoraddress 
  * @return normalization mode; one of the values from Normalization
  */
  static native int getNormalization(long collatoraddress);

  /**
  * Set the normalization mode used int this object
  * The normalization mode influences how strings are compared.
  * @param collatoraddress the address of the C collator
  * @param normalizationmode desired normalization mode; one of the values 
  *        from Normalization
  */
  static native void setNormalization(long collatoraddress, 
                                      int normalizationmode);

  /**
  * Get the collation rules from a UCollator.
  * The rules will follow the rule syntax.
  * @param collatoraddress the address of the C collator
  * @return collation rules.
  */
  static native String getRules(long collatoraddress);

  /**
  * Get a sort key for the argument string
  * Sort keys may be compared using java.util.Arrays.equals
  * @param collatoraddress address of the C collator
  * @param source string for key to be generated
  * @return sort key
  */
  static native byte[] getSortKey(long collatoraddress, String source);
                                   
  /**
  * Gets the version information for collation. 
  * @param collatoraddress address of the C collator
  * @return version information
  */
  // private native String getVersion(int collatoraddress);

  /**
  * Universal attribute setter.
  * @param collatoraddress address of the C collator
  * @param type type of attribute to be set
  * @param value attribute value
  * @exception thrown when error occurs while setting attribute value
  */
  static native void setAttribute(long collatoraddress, int type, int value);

  /**
  * Universal attribute getter
  * @param collatoraddress address of the C collator
  * @param type type of attribute to be set
  * @return attribute value
  * @exception thrown when error occurs while getting attribute value
  */
  static native int getAttribute(long collatoraddress, int type);

  /**
  * Thread safe cloning operation
  * @param collatoraddress address of C collator to be cloned
  * @return address of the new clone
  * @exception thrown when error occurs while cloning
  */
  static native long safeClone(long collatoraddress);

  /** 
  * Create a CollationElementIterator object that will iterator over the 
  * elements in a string, using the collation rules defined in this 
  * RuleBasedCollator
  * @param collatoraddress address of C collator
  * @param source string to iterate over
  * @return address of C collationelementiterator
  */
  static native long getCollationElementIterator(long collatoraddress, 
                                                 String source);
                                                 
  /**
  * Returns a hash of this collation object
  * @param collatoraddress address of C collator
  * @return hash of this collation object
  */
  static native int hashCode(long collatoraddress);

    
  // collationelementiterator methods -------------------------------------
  
  /**
  * Close a C collation element iterator.
  * @param address of C collation element iterator to close.
  */
  static native void closeElements(long address);

  /**
  * Reset the collation elements to their initial state.
  * This will move the 'cursor' to the beginning of the text.
  * @param address of C collation element iterator to reset.
  */
  static native void reset(long address);

  /**
  * Get the ordering priority of the next collation element in the text.
  * A single character may contain more than one collation element.
  * @param address if C collation elements containing the text.
  * @return next collation elements ordering, or NULLORDER if the end of the 
  *         text is reached.
  */
  static native int next(long address);

  /**
  * Get the ordering priority of the previous collation element in the text.
  * A single character may contain more than one collation element.
  * @param address of the C collation element iterator containing the text.
  * @return previous collation element ordering, or NULLORDER if the end of 
  *         the text is reached.
  */
  static native int previous(long address);

  /**
  * Get the maximum length of any expansion sequences that end with the 
  * specified comparison order.
  * @param address of the C collation element iterator containing the text.
  * @param order collation order returned by previous or next.
  * @return maximum length of any expansion sequences ending with the 
  *         specified order.
  */
  static native int getMaxExpansion(long address, int order);

  /**
  * Set the text containing the collation elements.
  * @param address of the C collation element iterator to be set
  * @param source text containing the collation elements.
  */
  static native void setText(long address, String source);

  /**
  * Get the offset of the current source character.
  * This is an offset into the text of the character containing the current
  * collation elements.
  * @param addresss of the C collation elements iterator to query.
  * @return offset of the current source character.
  */
  static native int getOffset(long address);

  /**
  * Set the offset of the current source character.
  * This is an offset into the text of the character to be processed.
  * @param address of the C collation element iterator to set.
  * @param offset The desired character offset.
  */
  static native void setOffset(long address, int offset);
  
  /**
  * Gets the primary order of a collation order.
  * @param order the collation order
  * @return the primary order of a collation order.
  */
  static native int primaryOrder(int order);

  /**
  * Gets the secondary order of a collation order.
  * @param order the collation order
  * @return the secondary order of a collation order.
  */
  static native int secondaryOrder(int order);

  /**
  * Gets the tertiary order of a collation order.
  * @param order the collation order
  * @return the tertiary order of a collation order.
  */
  static native int tertiaryOrder(int order);
}