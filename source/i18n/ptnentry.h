/******************************************************************************
* Copyright © {1996-1999}, International Business Machines Corporation and others. All Rights Reserved.
 ******************************************************************************
 */

#ifndef PTNENTRY_H
#define PTNENTRY_H

#include "unicode/utypes.h"
#include "unicode/unistr.h"
#include "unicode/coll.h"
#include "unicode/normlzr.h"

/**
 *
 * Utility class for normalizing and merging patterns for collation.
 * This is to be used with MergeCollation for adding patterns to an
 * existing rule table.
 */
 /*
 * Created by:     Mark Davis, Helena Shih
 *
 * Modification History:
 * Date        Name        Description
 *
 *  8/18/97     helena      Added internal API documentation.
 *  8/14/98     erm         Synched with 1.2 version of PatternEntry.java
 * 04/23/99     stephen     Removed EDecompositionMode, merged with
 *                          Normalizer::EMode
 */
class PatternEntry 
{

  friend class MergeCollation;
  friend class PointerToPatternEntry;
  friend class VectorOfPointersToPatternEntry;

 public:

  /**
     * Gets the extension, quoted if necessary, of this pattern entry.
     * @param toAddTo the result string buffer.
     */
  void appendQuotedExtension(UnicodeString& toAddTo) const;

  /**
     * Gets the current chars, quoted if necessary, of this pattern entry.
     */
  void appendQuotedChars(UnicodeString& toAddTo) const;

  /**
     * Compares two pattern entry objects.
     * @param other the other pattern entry object.
     * @return TRUE if the pattern entry objects are the same, FALSE otherwise.
     */
  bool_t equals(const PatternEntry& other) const;

  /**
     * Gets the strength of this entry.
     * @return the strength of this pattern entry.
     */

  int32_t getStrength(void)   const;

  /**
     * Gets the extension characters.
     * @param the extension string reference.
     * @return the extension chars of this pattern entry.
     */
  const   UnicodeString&  getExtension(UnicodeString& ext) const;

  /**
     * Gets the core characters.
     * @param the char string reference.
     * @return the char string of this pattern entry.
     */
  const   UnicodeString&  getChars(UnicodeString& chars) const;

  /**
     * Used to parse a pattern into a list
     * of PatternEntry's.
     */

  class Parser
    {
    public:
      Parser(const UnicodeString &pattern, Normalizer::EMode decompMode);
      
      Parser(const Parser &that);
      
      Parser &operator=(const Parser &that);
      
      ~Parser();
      
      PatternEntry *next(UErrorCode &status);

    private:
      UnicodeString pattern;
      int32_t index;
      Normalizer::EMode fDecompMode;

      UnicodeString newChars;
      UnicodeString newExtensions;
    };

  friend class Parser;


    /**
     * For debugging only.
     */
  UnicodeString& toString(UnicodeString&) const;

 private:

    /** Constructor and destructor
     */
  PatternEntry();

  /**
     * Creates a new pattern entry object.
     */
  PatternEntry(int32_t strength,
           const UnicodeString& chars,
           const UnicodeString& extension,
           Normalizer::EMode decompMode);
  /**
     * Copy constructor.
     */
  PatternEntry(const  PatternEntry& other);
  /**
     * Destructor.
     */
  ~PatternEntry();

  /** assignment 
     */
  const   PatternEntry&       operator=(const PatternEntry&   other);

  /**
     * Transforms the pattern entry into displayable text and adds
     * the text to the buffer, toAddTo.
     * @param toAddTo the result buffer.
     * @param showExtension whether to add the extension chars or not.
     * @param showWhiteSpace whether to add the white spaces or not.
     * @param lastEntry the last pattern entry that was referenced.
     */
  void addToBuffer(UnicodeString& toAddTo,
           bool_t showExtension,
           bool_t showWhiteSpace,
           const PatternEntry* lastEntry) const;

  /**
     * Gets the extension, quoted if necessary, of this pattern entry.
     * @param chars the chars string
     * @param toAddTo the result string buffer.
     */
  static void appendQuoted(const UnicodeString& chars, UnicodeString& toAddTo);

  /**
     * Checks if the Unicode character is a special character, for example, '@'
     * is considered a special character.  The values of a special character is
     * of the following range,
     * <pre>punctuation symbols :
     *                          0x0020 - 0x002F
     *                          0x003A - 0x003F 
     *                          0x005B - 0x0060
     *                          0x007B - 0x007E
     * </pre>
     * @param the Unicode character
     * @return TRUE if the character is a special character, FALSE otherwise.
     */
  static bool_t isSpecialChar(UChar ch);

  int32_t strength;
  UnicodeString chars;
  UnicodeString extension;

  static const int32_t RESET;
  static const int32_t UNSET;
};


#endif // _PTNENTRY
