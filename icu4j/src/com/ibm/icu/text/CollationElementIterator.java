/**
*******************************************************************************
* Copyright (C) 1996-2000, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/text/CollationElementIterator.java,v $ 
* $Date: 2001/03/09 00:34:42 $ 
* $Revision: 1.1 $
*
*******************************************************************************
*/

package com.ibm.icu4jni.text;

import com.ibm.icu4jni.common.ErrorCode;

/**
* Collation element iterator JNI wrapper
* @author syn wee quek
* @since Jan 22 01
*/
    
public final class CollationElementIterator
{
  // public data member -------------------------------------------
  
  public static final int NULLORDER = 0xFFFFFFFF;
  
  // public methods -----------------------------------------------
  
  /**
  * Reset the collation elements to their initial state.
  * This will move the 'cursor' to the beginning of the text.
  */
  public void reset()
  {
    NativeCollation.reset(m_collelemiterator_);
  }

  /**
  * Get the ordering priority of the next collation element in the text.
  * A single character may contain more than one collation element.
  * @return next collation elements ordering, or NULLORDER if the end of the 
  *         text is reached.
  */
  public int next()
  {
    return NativeCollation.next(m_collelemiterator_);
  }

  /**
  * Get the ordering priority of the previous collation element in the text.
  * A single character may contain more than one collation element.
  * @return previous collation element ordering, or NULLORDER if the end of 
  *         the text is reached.
  */
  public int previous()
  {
    return NativeCollation.previous(m_collelemiterator_);
  }

  /**
  * Get the maximum length of any expansion sequences that end with the 
  * specified comparison order.
  * @param order collation order returned by previous or next.
  * @return maximum size of the expansion sequences ending with the collation 
  *              element or 1 if collation element does not occur at the end of any 
  *              expansion sequence
  */
  public int getMaxExpansion(int order)
  {
    return NativeCollation.getMaxExpansion(m_collelemiterator_, order);
  }

  /**
  * Set the text containing the collation elements.
  * @param source text containing the collation elements.
  */
  public void setText(String source)
  {
    NativeCollation.setText(m_collelemiterator_, source);
  }

  /**
  * Get the offset of the current source character.
  * This is an offset into the text of the character containing the current
  * collation elements.
  * @return offset of the current source character.
  */
  public int getOffset()
  {
    return NativeCollation.getOffset(m_collelemiterator_);
  }

  /**
  * Set the offset of the current source character.
  * This is an offset into the text of the character to be processed.
  * @param offset The desired character offset.
  */
  public void setOffset(int offset)
  {
    NativeCollation.setOffset(m_collelemiterator_, offset);
  }
  
  /**
  * Gets the primary order of a collation order.
  * @param order the collation order
  * @return the primary order of a collation order.
  */
  public static int primaryOrder(int order) 
  {
    return NativeCollation.primaryOrder(order);
  }

  /**
  * Gets the secondary order of a collation order.
  * @param order the collation order
  * @return the secondary order of a collation order.
  */
  public static int secondaryOrder(int order)
  {
    return NativeCollation.secondaryOrder(order);
  }

  /**
  * Gets the tertiary order of a collation order.
  * @param order the collation order
  * @return the tertiary order of a collation order.
  */
  public static int tertiaryOrder(int order)
  {
    return NativeCollation.tertiaryOrder(order);
  }
  
  // protected constructor ----------------------------------------
  
  /**
  * CollationElementIteratorJNI constructor. 
  * The only caller of this class should be 
  * RuleBasedCollator.getCollationElementIterator(). 
  * @param collelemiteratoraddress address of C collationelementiterator
  */
  CollationElementIterator(long collelemiteratoraddress)
  {
    m_collelemiterator_ = collelemiteratoraddress;
  }

  // protected methods --------------------------------------------
  
  /**
  * Set ownership for C collator element iterator
  * @param ownership true or false
  */
  /*
  void setOwnCollationElementIterator(boolean ownership)
  {
    m_owncollelemiterator_ = ownership;
  }
  */
  
  /**
  * Garbage collection.
  * Close C collator and reclaim memory.
  */
  protected void finalize()
  {
    // if (m_owncollelemiterator_)
    NativeCollation.closeElements(m_collelemiterator_);
  }
  
  // private data members -----------------------------------------
 
  /**
  * C collator
  */
  private long m_collelemiterator_;
  
  /**
  * Flag indicating if the C data associated with m_collelemiterator_ is 
  * created by this object. This flag is used to determine if the C data is 
  * to be destroyed when this object is garbage-collected.
  */
  // private boolean m_owncollelemiterator_ = false;
}