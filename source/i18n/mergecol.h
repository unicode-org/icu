/******************************************************************************
 * COPYRIGHT:                                                               
 *  (C) Copyright Taligent, Inc., 1996
 *  (C) Copyright IBM Corp. 1996-1999
 *  Licensed Material - Program-Property of IBM - All Rights Reserved.
 *  US Government Users Restricted Rights - Use, duplication, or disclosure
 *  restricted by GSA ADP Schedule Contact with IBM Corp.
 *
 ******************************************************************************
 */
//=============================================================================
//
// File mergecol.h
//
// 
//
// Created by: Helena Shih
//
// Modification History:
//
//  Date        Name        Description
//  3/5/97      mark        Cleaned up fixEntry().  Added constants BYTEPOWER
//                          and BYTEMASK to replace BYTESIZE.
//  6/17/97     helena      In getPattern, added the queue-up list for entries 
//                          with the same extension chars.
//  8/18/97     helena      Added internal API documentation.
//  8/13/98     erm         Synched up with 1.2 version of MergeCollation.java
// 04/23/99     stephen     Removed EDecompositionMode, merged with
//                          Normalizer::EMode
//=============================================================================

#ifndef MERGECOL_H
#define MERGECOL_H

#include "unistr.h"
#include "ptnentry.h"
#include "tables.h"
#include "coll.h"
#include "normlzr.h"


/**
 * Utility class for normalizing and merging patterns for collation.
 * Patterns are strings of the form <entry>*, where <entry> has the
 * form:
 * <pre>
 * <pattern> := <entry>*
 * <entry> := <separator><chars>{"/"<extension>}
 * <separator> := "=", ",", ";", "<", "&"
 * <chars>, and <extension> are both arbitrary strings.
 * </pre>
 * <P>Unquoted whitespaces are ignored.
 * 'xxx' can be used to quote characters.
 * <P>
 * One difference from Collation is that & is used to reset to a current
 * point. Or, in other words, it introduces a new sequence which is to
 * be added to the old.
 * <P>
 * That is: "a < b < c < d" is the same as "a < b & b < c & c < d" OR
 * "a < b < d & b < c"
 * XXX: make '' be a single quote.
 * @see        PatternEntry
 * @version    1.4 1/7/97
 * @author     Mark Davis, Helena Shih
 */

class MergeCollation 
{
public:

    /**
     * Creates a merged collation table from a pattern string.
     * @param pattern the pattern string.
     * @param status the error code status.  If the input pattern is incorrect,
     * this will be set to U_INVALID_FORMAT_ERROR.
     */
  MergeCollation( const   UnicodeString&  pattern,
          Normalizer::EMode decompMode,
          UErrorCode&      success);
  /**
     * Copy constructor.
     */
  MergeCollation( const   MergeCollation& other);

  /**
     * Destructor.
     */
  ~MergeCollation();

  /** Assignment operator
     */
  const   MergeCollation&     operator=(const MergeCollation& other);
  /**
     * Recovers current pattern from this merged collation object.
     * @param pattern the result buffer.
     * @return the recovered result.
     */
  UnicodeString& getPattern(UnicodeString& pattern) const;

  /**
     * Recovers current pattern with white spaces.
     * @param pattern the result buffer.
     * @param withWhiteSpace puts spacing around the entries, and \n
     * before & and <
     * @return the recovered result.
     */
  UnicodeString& getPattern(UnicodeString& pattern, bool_t withWhiteSpace) const;

  /**
     * Emits the pattern for collation builder.
     * @param pattern the result buffer.
     * @return Emits the string in the format understable to the collation
     * builder.
     */
  UnicodeString& emitPattern(UnicodeString& pattern) const;

  /**
     * Emits the pattern for collation builder.
     * @param pattern the result buffer.
     * @param withWhiteSpace puts spacing around the entries, and \n
     * before & and <
     * @return Emits the string in the format understable to the collation
     * builder.
     */
  UnicodeString& emitPattern(UnicodeString& pattern, bool_t withWhiteSpace) const;

  /**
     * Sets the pattern.
     * @param pattern string.
     * @param status the error code status, it will be set to U_INVALID_FORMAT_ERROR
     * if the pattern is incorrect.
     */
  void setPattern(const   UnicodeString&  pattern,
          Normalizer::EMode decompMode,
          UErrorCode&      status);

  /**
     * Adds a pattern to the current merge collation object.
     * @param pattern the new pattern to be added.
     * @param status the error code status, it will be set to U_INVALID_FORMAT_ERROR
     * if the pattern is incorrect.
     */
  void addPattern(const   UnicodeString&  pattern,
          Normalizer::EMode decompMode,
          UErrorCode&      status);

  /**
     * Gets count of separate entries in the merge collation object.
     * @return the number of pattern entries
     */
  int32_t getCount(void) const;

  /**
     * Gets the specified pattern entry out of the merge collation object.
     * @param index the offset of the desired pattern entry
     * @return the requested pattern entry
     */
  const PatternEntry* getItemAt(UTextOffset index) const;

private:

    //============================================================
    // privates
    //============================================================

  VectorOfPointersToPatternEntry* patterns; // a vector of PatternEntries
  static  const   int32_t         BITARRAYSIZE;
  static  const   uint8_t         BITARRAYMASK;
  static  const   int32_t         BYTEPOWER;
  static  const   int32_t         BYTEMASK;

  PatternEntry*   lastEntry;
  PatternEntry*   saveEntry;
  uint8_t*        statusArray;


    /**
     * Finds the last pattern entry before the specified offset that does not have 
     * extension chars.
     * @param i the offset.
     * @return the pattern entry.
     */
  const PatternEntry* findLastWithNoExtension(int32_t i) const;

  /** 
     * Fixes the new pattern entry in the merge collation table.
     * If the strength is RESET, then just change the lastEntry to
     * be the current. (If the current is not in patterns, signal an error).
     * If not, then remove the current entry, and add it after lastEntry
     * (which is usually at the end).  Strength indicates the text order
     * weight for an entry.
     * @param newEntry the new pattern entry
     * @param status the error code status, it will be set to U_INVALID_FORMAT_ERROR
     * if the strength is RESET and a previous entry can't be found.
     */
  void fixEntry(  PatternEntry*   newEntry,
          UErrorCode&      status);

  /**
     * Finds the offset of the specified entry that was previously installed in the
     * merge collation object.
     * @param lastEntry the entry that was previously installed.
     * @param excess the extra characters 
     * @param status the error code status, it will be set to U_INVALID_FORMAT_ERROR
     * if the strength is RESET and a previous entry can't be found.
     * @return the offset of the found entry
     */
  int32_t findLastEntry(  const PatternEntry* lastEntry, 
              UnicodeString&  excess,
              UErrorCode&      success) const;
};

inline UnicodeString& MergeCollation::getPattern(UnicodeString& result) const
{
  return getPattern(result, TRUE);
}

inline UnicodeString& MergeCollation::emitPattern(UnicodeString& result) const
{
  return emitPattern(result, TRUE);
}


#endif // _MERGECOL
