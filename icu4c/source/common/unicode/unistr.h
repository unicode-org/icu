/*
**********************************************************************
*   Copyright (C) 1998-1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*
* File unistr.h
*
* Modification History:
*
*   Date        Name        Description
*   09/25/98    stephen     Creation.
*   11/11/98    stephen     Changed per 11/9 code review.
*   04/20/99    stephen     Overhauled per 4/16 code review.
*   11/18/99    aliu        Made to inherit from Replaceable.  Added method
*                           handleReplaceBetween(); other methods unchanged.
*******************************************************************************
*/

#ifndef UNISTR_H
#define UNISTR_H

#include <limits.h>

#include "unicode/utypes.h"
#include "unicode/unicode.h"
#include "unicode/ucnv.h"
#include "unicode/rep.h"

class Locale;
class UCharReference;
class UnicodeStringStreamer;
class UnicodeConverterCPP;

// for unistrm.h
class ostream;
U_COMMON_API ostream &operator<<(ostream& stream, const UnicodeString& s);

/**
 * Unicode String literals in C++.
 * Dependent on the platform properties, different UnicodeString
 * constructors should be used to create a UnicodeString object from
 * a string literal.
 * The macros are defined for maximum performance.
 * They work only for strings that contain "invariant characters", i.e.,
 * only latin letters, digits, and some punctuation.
 * See utypes.h for details.
 *
 * The string parameter must be a C string literal.
 * The length of the string, not including the terminating
 * <code>NUL</code>, must be specified as a constant.
 * The U_STRING_DECL macro should be invoked exactly once for one
 * such string variable before it is used.
 */
#if U_SIZEOF_WCHAR_T==U_SIZEOF_UCHAR && U_CHARSET_FAMILY==U_ASCII_FAMILY
#   define UNICODE_STRING(cs, length) UnicodeString(TRUE, (UChar *)L ## cs, length)
#elif U_SIZEOF_UCHAR==1 && U_CHARSET_FAMILY==U_ASCII_FAMILY
#   define UNICODE_STRING(cs, length) UnicodeString(TRUE, (UChar *)cs, length)
#else
#   define UNICODE_STRING(cs, length) UnicodeString(cs, length, "")
#endif

/**
 * UnicodeString is a concrete implementation of the abstract class Replaceable.
 * It is a string class that stores Unicode characters directly and provides
 * similar functionality as the Java string class.
 *
 * UnicodeString uses four storage models:
 * <ol>
 * <li>Short strings are normally stored inside the UnicodeString object itself.
 *     The object has fields for the "bookkeeping" and a small UChar array.
 *     When the object is copied, then the internal characters are copied
 *     into the destination object.</li>
 * <li>Longer strings are normally stored in allocated memory.
 *     The allocated UChar array is preceeded by a reference counter.
 *     When the string object is copied, then the allocated buffer is shared by
 *     incrementing the reference counter.</li>
 * <li>A UnicodeString can be constructed or setTo() such that it aliases a read-only
 *     buffer instead of copying the characters. In this case, the string object
 *     uses this aliased buffer for as long as it is not modified, and it will never
 *     attempt to modify or release the buffer. This has copy-on-write semantics:
 *     When the string object is modified, then the buffer contents is first copied
 *     into writeable memory (inside the object for short strings, or allocated
 *     buffer for longer strings). When a UnicodeString with a read-only alias
 *     is assigned to another UnicodeString, then both string objects will
 *     share the same read-only alias.</li>
 * <li>A UnicodeString can be constructed or setTo() such that it aliases a writeable
 *     buffer instead of copying the characters. The difference from the above is that
 *     the string object will write through to this aliased buffer for write
 *     operations. Only when the capacity of the buffer is not sufficient is
 *     a new buffer allocated and the contents copied.
 *     An efficient way to get the string contents into the original buffer is
 *     to use the extract(..., UChar *dst, ...) function: It will only copy the
 *     string contents if the dst buffer is different from the buffer of the string
 *     object itself. If a string grows and shrinks during a sequence of operations,
 *     then it will not use the same buffer any more, but may fit into it again.
 *     When a UnicodeString with a writeable alias is assigned to another UnicodeString,
 *     then the contents is always copied. The destination string will not alias
 *     to the buffer that the source string aliases.</li>
 * </ol>
 */
class U_COMMON_API UnicodeString : public Replaceable
{
public:

  //========================================
  // Read-only operations
  //========================================
  
  /* Comparison - bitwise only - for international comparison use collation */
  
  /**
   * Equality operator. Performs only bitwise comparison.
   * @param text The UnicodeString to compare to this one.
   * @return TRUE if <TT>text</TT> contains the same characters as this one,
   * FALSE otherwise.
   * @stable
   */
  inline bool_t operator== (const UnicodeString& text) const;
  
  /**
   * Inequality operator. Performs only bitwise comparison.
   * @param text The UnicodeString to compare to this one.
   * @return FALSE if <TT>text</TT> contains the same characters as this one,
   * TRUE otherwise.
   * @stable
   */
  inline bool_t operator!= (const UnicodeString& text) const;

  /**
   * Greater than operator. Performs only bitwise comparison.
   * @param text The UnicodeString to compare to this one.
   * @return TRUE if the characters in <TT>text</TT> are bitwise
   * greater than the characters in this, FALSE otherwise
   * @stable
   */
  inline bool_t operator> (const UnicodeString& text) const;

  /**
   * Less than operator. Performs only bitwise comparison.
   * @param text The UnicodeString to compare to this one.
   * @return TRUE if the characters in <TT>text</TT> are bitwise
   * less than the characters in this, FALSE otherwise
   * @stable
   */
  inline bool_t operator< (const UnicodeString& text) const;

  /**
   * Greater than or equal operator. Performs only bitwise comparison.
   * @param text The UnicodeString to compare to this one.
   * @return TRUE if the characters in <TT>text</TT> are bitwise
   * greater than or equal to the characters in this, FALSE otherwise
   * @stable
   */
  inline bool_t operator>= (const UnicodeString& text) const;

  /**
   * Less than or equal operator. Performs only bitwise comparison.
   * @param text The UnicodeString to compare to this one.
   * @return TRUE if the characters in <TT>text</TT> are bitwise
   * less than or equal to the characters in this, FALSE otherwise
   * @stable
   */
  inline bool_t operator<= (const UnicodeString& text) const;

  /**
   * Compare the characters bitwise in this UnicodeString to
   * the characters in <TT>text</TT>.
   * @param text The UnicodeString to compare to this one.
   * @return The result of bitwise character comparison: 0 if <TT>text</TT>
   * contains the same characters as this, -1 if the characters in 
   * <TT>text</TT> are bitwise less than the characters in this, +1 if the
   * characters in <TT>text</TT> are bitwise greater than the characters 
   * in this.
   * @stable
   */
  inline int8_t compare(const UnicodeString& text) const;

  /**
   * Compare the characters bitwise in the range 
   * [<TT>start</TT>, <TT>start + length</TT>) with the characters 
   * in <TT>srcText</TT>
   * @param start the offset at which the compare operation begins
   * @param length the number of characters of text to compare.
   * @param srcText the text to be compared
   * @return The result of bitwise character comparison: 0 if <TT>text</TT>
   * contains the same characters as this, -1 if the characters in 
   * <TT>text</TT> are bitwise less than the characters in this, +1 if the
   * characters in <TT>text</TT> are bitwise greater than the characters 
   * in this.
   * @stable
   */
  inline int8_t compare(UTextOffset start,
         int32_t length,
         const UnicodeString& srcText) const;

  /**
   * Compare the characters bitwise in the range 
   * [<TT>start</TT>, <TT>start + length</TT>) with the characters 
   * in <TT>srcText</TT> in the range 
   * [<TT>srcStart</TT>, <TT>srcStart + srcLength</TT>).  
   * @param start the offset at which the compare operation begins
   * @param length the number of characters in this to compare.
   * @param srcText the text to be compared
   * @param srcStart the offset into <TT>srcText</TT> to start comparison
   * @param srcLength the number of characters in <TT>src</TT> to compare
   * @return The result of bitwise character comparison: 0 if <TT>text</TT>
   * contains the same characters as this, -1 if the characters in 
   * <TT>text</TT> are bitwise less than the characters in this, +1 if the
   * characters in <TT>text</TT> are bitwise greater than the characters 
   * in this.
   * @stable
   */
   inline int8_t compare(UTextOffset start,
         int32_t length,
         const UnicodeString& srcText,
         UTextOffset srcStart,
         int32_t srcLength) const;

  /**
   * Compare the characters bitwise in this UnicodeString with the first 
   * <TT>srcLength</TT> characters in <TT>srcChars</TT>.
   * @param srcChars The characters to compare to this UnicodeString.
   * @param srcLength the number of characters in <TT>srcChars</TT> to compare
   * @return The result of bitwise character comparison: 0 if <TT>text</TT>
   * contains the same characters as this, -1 if the characters in 
   * <TT>text</TT> are bitwise less than the characters in this, +1 if the
   * characters in <TT>text</TT> are bitwise greater than the characters 
   * in this.
   * @stable
   */
  inline int8_t compare(const UChar *srcChars,
         int32_t srcLength) const;

  /**
   * Compare the characters bitwise in the range 
   * [<TT>start</TT>, <TT>start + length</TT>) with the first 
   * <TT>length</TT> characters in <TT>srcChars</TT>
   * @param start the offset at which the compare operation begins
   * @param length the number of characters to compare.
   * @param srcChars the characters to be compared
   * @return The result of bitwise character comparison: 0 if <TT>text</TT>
   * contains the same characters as this, -1 if the characters in 
   * <TT>text</TT> are bitwise less than the characters in this, +1 if the
   * characters in <TT>text</TT> are bitwise greater than the characters 
   * in this.
   * @stable
   */
  inline int8_t compare(UTextOffset start,
         int32_t length,
         const UChar *srcChars) const;

  /**
   * Compare the characters bitwise in the range 
   * [<TT>start</TT>, <TT>start + length</TT>) with the characters 
   * in <TT>srcChars</TT> in the range 
   * [<TT>srcStart</TT>, <TT>srcStart + srcLength</TT>).  
   * @param start the offset at which the compare operation begins
   * @param length the number of characters in this to compare
   * @param srcChars the characters to be compared
   * @param srcStart the offset into <TT>srcChars</TT> to start comparison
   * @param srcLength the number of characters in <TT>srcChars</TT> to compare
   * @return The result of bitwise character comparison: 0 if <TT>text</TT>
   * contains the same characters as this, -1 if the characters in 
   * <TT>text</TT> are bitwise less than the characters in this, +1 if the
   * characters in <TT>text</TT> are bitwise greater than the characters 
   * in this.
   * @stable
   */
  inline int8_t compare(UTextOffset start,
         int32_t length,
         const UChar *srcChars,
         UTextOffset srcStart,
         int32_t srcLength) const;

  /**
   * Compare the characters bitwise in the range 
   * [<TT>start</TT>, <TT>limit</TT>) with the characters 
   * in <TT>srcText</TT> in the range 
   * [<TT>srcStart</TT>, <TT>srcLimit</TT>).  
   * @param start the offset at which the compare operation begins
   * @param limit the offset immediately following the compare operation
   * @param srcText the text to be compared
   * @param srcStart the offset into <TT>srcText</TT> to start comparison
   * @param srcLimit the offset into <TT>srcText</TT> to limit comparison
   * @return The result of bitwise character comparison: 0 if <TT>text</TT>
   * contains the same characters as this, -1 if the characters in 
   * <TT>text</TT> are bitwise less than the characters in this, +1 if the
   * characters in <TT>text</TT> are bitwise greater than the characters 
   * in this.
   * @stable
   */
  inline int8_t compareBetween(UTextOffset start,
            UTextOffset limit,
            const UnicodeString& srcText,
            UTextOffset srcStart,
            UTextOffset srcLimit) const;

  /**
   * Determine if this starts with the characters in <TT>text</TT>
   * @param text The text to match.
   * @return TRUE if this starts with the characters in <TT>text</TT>, 
   * FALSE otherwise
   * @stable
   */
  inline bool_t startsWith(const UnicodeString& text) const;

  /**
   * Determine if this starts with the characters in <TT>srcText</TT> 
   * in the range [<TT>srcStart</TT>, <TT>srcStart + srcLength</TT>).   
   * @param srcText The text to match.
   * @param srcStart the offset into <TT>srcText</TT> to start matching
   * @param srcLength the number of characters in <TT>srcText</TT> to match
   * @return TRUE if this starts with the characters in <TT>text</TT>, 
   * FALSE otherwise
   * @stable
   */
  inline bool_t startsWith(const UnicodeString& srcText,
            UTextOffset srcStart,
            int32_t srcLength) const;
  
  /**
   * Determine if this starts with the characters in <TT>srcChars</TT>
   * @param srcChars The characters to match.
   * @param srcLength the number of characters in <TT>srcChars</TT>
   * @return TRUE if this starts with the characters in <TT>srcChars</TT>, 
   * FALSE otherwise
   * @stable
   */
  inline bool_t startsWith(const UChar *srcChars,
            int32_t srcLength) const;
 
  /**
   * Determine if this ends with the characters in <TT>srcChars</TT> 
   * in the range  [<TT>srcStart</TT>, <TT>srcStart + srcLength</TT>).   
   * @param srcChars The characters to match.
   * @param srcStart the offset into <TT>srcText</TT> to start matching
   * @param srcLength the number of characters in <TT>srcChars</TT> to match
   * @return TRUE if this ends with the characters in <TT>srcChars</TT>, 
   FALSE otherwise
   * @stable
  */
  inline bool_t startsWith(const UChar *srcChars,
            UTextOffset srcStart,
            int32_t srcLength) const;

  /**
   * Determine if this ends with the characters in <TT>text</TT>
   * @param text The text to match.
   * @return TRUE if this ends with the characters in <TT>text</TT>, 
   * FALSE otherwise
   * @stable
   */
  inline bool_t endsWith(const UnicodeString& text) const;

  /**
   * Determine if this ends with the characters in <TT>srcText</TT> 
   * in the range [<TT>srcStart</TT>, <TT>srcStart + srcLength</TT>).   
   * @param srcText The text to match.
   * @param srcStart the offset into <TT>srcText</TT> to start matching
   * @param srcLength the number of characters in <TT>srcText</TT> to match
   * @return TRUE if this ends with the characters in <TT>text</TT>, 
   * FALSE otherwise
   * @stable
   */
  inline bool_t endsWith(const UnicodeString& srcText,
          UTextOffset srcStart,
          int32_t srcLength) const;

  /**
   * Determine if this ends with the characters in <TT>srcChars</TT>
   * @param srcChars The characters to match.
   * @param srcLength the number of characters in <TT>srcChars</TT>
   * @return TRUE if this ends with the characters in <TT>srcChars</TT>, 
   * FALSE otherwise
   * @stable
   */
  inline bool_t endsWith(const UChar *srcChars,
          int32_t srcLength) const;
 
  /**
   * Determine if this ends with the characters in <TT>srcChars</TT> 
   * in the range  [<TT>srcStart</TT>, <TT>srcStart + srcLength</TT>).   
   * @param srcChars The characters to match.
   * @param srcStart the offset into <TT>srcText</TT> to start matching
   * @param srcLength the number of characters in <TT>srcChars</TT> to match
   * @return TRUE if this ends with the characters in <TT>srcChars</TT>, 
   * FALSE otherwise
   * @stable
   */
  inline bool_t endsWith(const UChar *srcChars,
          UTextOffset srcStart,
          int32_t srcLength) const;

 
  /* Searching - bitwise only */

  /**
   * Locate in this the first occurrence of the characters in <TT>text</TT>,
   * using bitwise comparison.
   * @param text The text to search for.
   * @return The offset into this of the start of <TT>text</TT>, 
   * or -1 if not found.
   * @stable
   */
  inline UTextOffset indexOf(const UnicodeString& text) const;

  /**
   * Locate in this the first occurrence of the characters in <TT>text</TT>
   * starting at offset <TT>start</TT>, using bitwise comparison.
   * @param text The text to search for.
   * @param start The offset at which searching will start.
   * @return The offset into this of the start of <TT>text</TT>, 
   * or -1 if not found.
   * @stable
   */
  inline UTextOffset indexOf(const UnicodeString& text,
              UTextOffset start) const;

  /**
   * Locate in this the first occurrence in the range
   * [<TT>start</TT>, <TT>start + length</TT>) of the characters 
   * in <TT>text</TT>, using bitwise comparison.
   * @param text The text to search for.
   * @param start The offset at which searching will start.
   * @param length The number of characters to search
   * @return The offset into this of the start of <TT>text</TT>, 
   * or -1 if not found.
   * @stable
   */
  inline UTextOffset indexOf(const UnicodeString& text,
              UTextOffset start,
              int32_t length) const;

  /**
   * Locate in this the first occurrence in the range
   * [<TT>start</TT>, <TT>start + length</TT>) of the characters
   *  in <TT>srcText</TT> in the range 
   * [<TT>srcStart</TT>, <TT>srcStart + srcLength</TT>), 
   * using bitwise comparison.   
   * @param text The text to search for.
   * @param srcStart the offset into <TT>srcText</TT> at which
   * to start matching
   * @param srcLength the number of characters in <TT>srcText</TT> to match
   * @param start the offset into this at which to start matching
   * @param length the number of characters in this to search
   * @return The offset into this of the start of <TT>text</TT>, 
   * or -1 if not found.
   * @stable
   */
  inline UTextOffset indexOf(const UnicodeString& srcText,
              UTextOffset srcStart,
              int32_t srcLength,
              UTextOffset start,
              int32_t length) const;

  /**
   * Locate in this the first occurrence of the characters in
   * <TT>srcChars</TT> 
   * starting at offset <TT>start</TT>, using bitwise comparison. 
   * @param srcChars The text to search for.
   * @param srcLength the number of characters in <TT>srcChars</TT> to match
   * @param start the offset into this at which to start matching
   * @return The offset into this of the start of <TT>text</TT>, 
   * or -1 if not found.
   * @stable
   */
  inline UTextOffset indexOf(const UChar *srcChars,
              int32_t srcLength,
              UTextOffset start) const;

  /**
   * Locate in this the first occurrence in the range
   * [<TT>start</TT>, <TT>start + length</TT>) of the characters 
   * in <TT>srcChars</TT>, using bitwise comparison.
   * @param text The text to search for.
   * @param srcLength the number of characters in <TT>srcChars</TT>
   * @param start The offset at which searching will start.
   * @param length The number of characters to search
   * @return The offset into this of the start of <TT>srcChars</TT>, 
   * or -1 if not found.
   * @stable
   */
  inline UTextOffset indexOf(const UChar *srcChars,
              int32_t srcLength,
              UTextOffset start,
              int32_t length) const;
 
  /**
   * Locate in this the first occurrence in the range 
   * [<TT>start</TT>, <TT>start + length</TT>) of the characters 
   * in <TT>srcChars</TT> in the range 
   * [<TT>srcStart</TT>, <TT>srcStart + srcLength</TT>), 
   * using bitwise comparison.   
   * @param srcChars The text to search for.
   * @param srcStart the offset into <TT>srcChars</TT> at which 
   * to start matching
   * @param srcLength the number of characters in <TT>srcChars</TT> to match
   * @param start the offset into this at which to start matching
   * @param length the number of characters in this to search
   * @return The offset into this of the start of <TT>text</TT>, 
   * or -1 if not found.
   * @stable
   */
  UTextOffset indexOf(const UChar *srcChars,
              UTextOffset srcStart,
              int32_t srcLength,
              UTextOffset start,
              int32_t length) const;

  /**
   * Locate in this the first occurrence of the code unit <TT>c</TT>, 
   * using bitwise comparison.
   * @param c The code unit to search for.
   * @return The offset into this of <TT>c</TT>, or -1 if not found.
   * @draft
   */
  inline UTextOffset indexOf(UChar c) const;

  /**
   * Locate in this the first occurrence of the code point <TT>c</TT>, 
   * using bitwise comparison.
   * @param c The code point to search for.
   * @return The offset into this of <TT>c</TT>, or -1 if not found.
   * @draft
   */
  inline UTextOffset indexOf(UChar32 c) const;

  /**
   * Locate in this the first occurrence of the code unit <TT>c</TT>
   * starting at offset <TT>start</TT>, using bitwise comparison.
   * @param c The code unit to search for.
   * @param start The offset at which searching will start.
   * @return The offset into this of <TT>c</TT>, or -1 if not found.
   * @draft
   */
  inline UTextOffset indexOf(UChar c,
              UTextOffset start) const;

  /**
   * Locate in this the first occurrence of the code point <TT>c</TT>
   * starting at offset <TT>start</TT>, using bitwise comparison.
   * @param c The code point to search for.
   * @param start The offset at which searching will start.
   * @return The offset into this of <TT>c</TT>, or -1 if not found.
   * @draft
   */
  inline UTextOffset indexOf(UChar32 c,
              UTextOffset start) const;

  /**
   * Locate in this the first occurrence of the code unit <TT>c</TT> 
   * in the range [<TT>start</TT>, <TT>start + length</TT>), 
   * using bitwise comparison.   
   * @param c The code unit to search for.
   * @param start the offset into this at which to start matching
   * @param length the number of characters in this to search
   * @return The offset into this of <TT>c</TT>, or -1 if not found.
   * @draft
   */
  inline UTextOffset indexOf(UChar c,
              UTextOffset start,
              int32_t length) const;

  /**
   * Locate in this the first occurrence of the code point <TT>c</TT> 
   * in the range [<TT>start</TT>, <TT>start + length</TT>), 
   * using bitwise comparison.   
   * @param c The code point to search for.
   * @param start the offset into this at which to start matching
   * @param length the number of characters in this to search
   * @return The offset into this of <TT>c</TT>, or -1 if not found.
   * @draft
   */
  inline UTextOffset indexOf(UChar32 c,
              UTextOffset start,
              int32_t length) const;

  /**
   * Locate in this the last occurrence of the characters in <TT>text</TT>, 
   * using bitwise comparison.
   * @param text The text to search for.
   * @return The offset into this of the start of <TT>text</TT>, 
   * or -1 if not found.
   * @stable
   */
  inline UTextOffset lastIndexOf(const UnicodeString& text) const;

  /**
   * Locate in this the last occurrence of the characters in <TT>text</TT>
   * starting at offset <TT>start</TT>, using bitwise comparison.
   * @param text The text to search for.
   * @param start The offset at which searching will start.
   * @return The offset into this of the start of <TT>text</TT>, 
   * or -1 if not found.
   * @stable
   */
  inline UTextOffset lastIndexOf(const UnicodeString& text,
              UTextOffset start) const;

  /**
   * Locate in this the last occurrence in the range 
   * [<TT>start</TT>, <TT>start + length</TT>) of the characters
   * in <TT>text</TT>, using bitwise comparison.
   * @param text The text to search for.
   * @param start The offset at which searching will start.
   * @param length The number of characters to search
   * @return The offset into this of the start of <TT>text</TT>, 
   * or -1 if not found.
   * @stable
   */
  inline UTextOffset lastIndexOf(const UnicodeString& text,
              UTextOffset start,
              int32_t length) const;

  /**
   * Locate in this the last occurrence in the range 
   * [<TT>start</TT>, <TT>start + length</TT>) of the characters 
   * in <TT>srcText</TT> in the range 
   * [<TT>srcStart</TT>, <TT>srcStart + srcLength</TT>), 
   * using bitwise comparison.   
   * @param text The text to search for.
   * @param srcStart the offset into <TT>srcText</TT> at which 
   * to start matching
   * @param srcLength the number of characters in <TT>srcText</TT> to match
   * @param start the offset into this at which to start matching
   * @param length the number of characters in this to search
   * @return The offset into this of the start of <TT>text</TT>, 
   * or -1 if not found.
   * @stable
   */
  inline UTextOffset lastIndexOf(const UnicodeString& srcText,
              UTextOffset srcStart,
              int32_t srcLength,
              UTextOffset start,
              int32_t length) const;

  /**
   * Locate in this the last occurrence of the characters in <TT>srcChars</TT> 
   * starting at offset <TT>start</TT>, using bitwise comparison. 
   * @param srcChars The text to search for.
   * @param srcLength the number of characters in <TT>srcChars</TT> to match
   * @param start the offset into this at which to start matching
   * @return The offset into this of the start of <TT>text</TT>, 
   * or -1 if not found.
   * @stable
   */
  inline UTextOffset lastIndexOf(const UChar *srcChars,
              int32_t srcLength,
              UTextOffset start) const;

  /**
   * Locate in this the last occurrence in the range 
   * [<TT>start</TT>, <TT>start + length</TT>) of the characters 
   * in <TT>srcChars</TT>, using bitwise comparison.
   * @param text The text to search for.
   * @param srcLength the number of characters in <TT>srcChars</TT>
   * @param start The offset at which searching will start.
   * @param length The number of characters to search
   * @return The offset into this of the start of <TT>srcChars</TT>, 
   * or -1 if not found.
   * @stable
   */
  inline UTextOffset lastIndexOf(const UChar *srcChars,
              int32_t srcLength,
              UTextOffset start,
              int32_t length) const;
 
  /**
   * Locate in this the last occurrence in the range 
   * [<TT>start</TT>, <TT>start + length</TT>) of the characters 
   * in <TT>srcChars</TT> in the range 
   * [<TT>srcStart</TT>, <TT>srcStart + srcLength</TT>), 
   * using bitwise comparison.   
   * @param srcChars The text to search for.
   * @param srcStart the offset into <TT>srcChars</TT> at which
   * to start matching
   * @param srcLength the number of characters in <TT>srcChars</TT> to match
   * @param start the offset into this at which to start matching
   * @param length the number of characters in this to search
   * @return The offset into this of the start of <TT>text</TT>, 
   * or -1 if not found.
   * @stable
   */
  UTextOffset lastIndexOf(const UChar *srcChars,
              UTextOffset srcStart,
              int32_t srcLength,
              UTextOffset start,
              int32_t length) const;

  /**
   * Locate in this the last occurrence of the code unit <TT>c</TT>, 
   * using bitwise comparison.
   * @param c The code unit to search for.
   * @return The offset into this of <TT>c</TT>, or -1 if not found.
   * @draft
   */
  inline UTextOffset lastIndexOf(UChar c) const;

  /**
   * Locate in this the last occurrence of the code point <TT>c</TT>, 
   * using bitwise comparison.
   * @param c The code point to search for.
   * @return The offset into this of <TT>c</TT>, or -1 if not found.
   * @draft
   */
  inline UTextOffset lastIndexOf(UChar32 c) const;

  /**
   * Locate in this the last occurrence of the code unit <TT>c</TT>
   * starting at offset <TT>start</TT>, using bitwise comparison.
   * @param c The code unit to search for.
   * @param start The offset at which searching will start.
   * @return The offset into this of <TT>c</TT>, or -1 if not found.
   * @draft
   */
  inline UTextOffset lastIndexOf(UChar c,
              UTextOffset start) const;

  /**
   * Locate in this the last occurrence of the code point <TT>c</TT>
   * starting at offset <TT>start</TT>, using bitwise comparison.
   * @param c The code point to search for.
   * @param start The offset at which searching will start.
   * @return The offset into this of <TT>c</TT>, or -1 if not found.
   * @draft
   */
  inline UTextOffset lastIndexOf(UChar32 c,
              UTextOffset start) const;

  /**
   * Locate in this the last occurrence of the code unit <TT>c</TT> 
   * in the range [<TT>start</TT>, <TT>start + length</TT>), 
   * using bitwise comparison.   
   * @param c The code unit to search for.
   * @param start the offset into this at which to start matching
   * @param length the number of characters in this to search
   * @return The offset into this of <TT>c</TT>, or -1 if not found.
   * @draft
   */
  inline UTextOffset lastIndexOf(UChar c,
              UTextOffset start,
              int32_t length) const;

  /**
   * Locate in this the last occurrence of the code point <TT>c</TT> 
   * in the range [<TT>start</TT>, <TT>start + length</TT>), 
   * using bitwise comparison.   
   * @param c The code point to search for.
   * @param start the offset into this at which to start matching
   * @param length the number of characters in this to search
   * @return The offset into this of <TT>c</TT>, or -1 if not found.
   * @draft
   */
  inline UTextOffset lastIndexOf(UChar32 c,
              UTextOffset start,
              int32_t length) const;


  /* Character access */

  /**
   * Return the code unit at offset <tt>offset</tt>.
   * @param offset a valid offset into the text
   * @returns the code unit at offset <tt>offset</tt>
   * @draft
   */
  inline UChar charAt(UTextOffset offset) const;

  /**
   * Return the code unit at offset <tt>offset</tt>.
   * @param offset a valid offset into the text
   * @returns the code unit at offset <tt>offset</tt>
   * @draft
   */
  inline UChar operator [] (UTextOffset offset) const;

  /**
   * Return the code point that contains the code unit
   * at offset <tt>offset</tt>.
   * @param offset a valid offset into the text
   * that indicates the text offset of any of the code units
   * that will be assembled into a code point (21-bit value) and returned
   * @returns the code point of text at <tt>offset</tt>
   * @draft
   */
  inline UChar32 char32At(UTextOffset offset) const;

  /**
   * Adjust a random-access offset so that
   * it points to the beginning of a Unicode character.
   * The offset that is passed in points to
   * any code unit of a code point,
   * while the returned offset will point to the first code unit
   * of the same code point.
   * In UTF-16, if the input offset points to a second surrogate
   * of a surrogate pair, then the returned offset will point
   * to the first surrogate.
   * @param offset a valid offset into one code point of the text
   * @return offset of the first code unit of the same code point
   */
  inline UTextOffset getCharStart(UTextOffset offset);

  /**
   * Adjust a random-access offset so that
   * it points behind a Unicode character.
   * The offset that is passed in points behind
   * any code unit of a code point,
   * while the returned offset will point behind the last code unit
   * of the same code point.
   * In UTF-16, if the input offset points behind the first surrogate
   * (i.e., to the second surrogate)
   * of a surrogate pair, then the returned offset will point
   * behind the second surrogate (i.e., to the first surrogate).
   * @param offset a valid offset after any code unit of a code point of the text
   * @return offset of the first code unit after the same code point
   */
  inline UTextOffset getCharLimit(UTextOffset offset);

  /* Substring extraction */

  /**
   * Copy the characters in the range 
   * [<tt>start</tt>, <tt>start + length</tt>) into the array <tt>dst</tt>,
   * beginning at <tt>dstStart</tt>.
   * If the string aliases to <code>dst</code> itself as an external buffer,
   * then extract() will not copy the contents.
   *
   * @param start offset of first character which will be copied into the array
   * @param length the number of characters to extract
   * @param dst array in which to copy characters.  The length of <tt>dst</tt>
   * must be at least (<tt>dstStart + length</tt>).
   * @param dstStart the offset in <TT>dst</TT> where the first character
   * will be extracted
   * @stable
   */
  inline void extract(UTextOffset start, 
           int32_t length, 
           UChar *dst, 
           UTextOffset dstStart = 0) const;
  
  /**
   * Copy the characters in the range 
   * [<tt>start</tt>, <tt>start + length</tt>) into the  UnicodeString
   * <tt>target</tt>.
   * @param start offset of first character which will be copied
   * @param length the number of characters to extract
   * @param target UnicodeString into which to copy characters.
   * @return A reference to <TT>target</TT>
   * @stable
   */
  inline void extract(UTextOffset start,
           int32_t length,
           UnicodeString& target) const;

  /**
   * Copy the characters in the range [<tt>start</tt>, <tt>limit</tt>) 
   * into the array <tt>dst</tt>, beginning at <tt>dstStart</tt>.
   * @param start offset of first character which will be copied into the array
   * @param limit offset immediately following the last character to be copied
   * @param dst array in which to copy characters.  The length of <tt>dst</tt> 
   * must be at least (<tt>dstStart + (limit - start)</tt>).
   * @param dstStart the offset in <TT>dst</TT> where the first character
   * will be extracted
   * @stable
   */
  inline void extractBetween(UTextOffset start, 
              UTextOffset limit, 
              UChar *dst, 
              UTextOffset dstStart = 0) const;

  /**
   * Copy the characters in the range [<tt>start</tt>, <tt>limit</tt>) 
   * into the UnicodeString <tt>target</tt>.
   * @param start offset of first character which will be copied
   * @param limit offset immediately following the last character to be copied
   * @param target UnicodeString into which to copy characters.
   * @return A reference to <TT>target</TT>
   * @stable
   */
  inline void extractBetween(UTextOffset start,
              UTextOffset limit,
              UnicodeString& target) const;

  /**
   * Copy the characters in the range 
   * [<tt>start</TT>, <tt>start + length</TT>) into an array of characters
   * in a specified codepage.
   * @param start offset of first character which will be copied
   * @param length the number of characters to extract
   * @param target the target buffer for extraction
   * @param codepage the desired codepage for the characters.  0 has 
   * the special meaning of the default codepage
   * If <code>codepage</code> is an empty string (<code>""</code>),
   * then a simple conversion is performed on the codepage-invariant
   * subset ("invariant characters") of the platform encoding. See utypes.h.
   * @return the number of characters written to <TT>dst</TT>
   * @stable
   */
  int32_t extract(UTextOffset start,
           int32_t length,
           char *dst,
           const char *codepage = 0) const;
  

  /* Length operations */

  /**
   * Return the length of the UnicodeString object.  
   * The length is the number of characters in the text.
   * @returns the length of the UnicodeString object
   * @stable
   */
  inline int32_t  length(void) const;

  /**
   * Determine if this string is empty.
   * @return TRUE if this string contains 0 characters, FALSE otherwise.
   * @stable
   */
  inline bool_t empty(void) const;

 
  /* Other operations */

  /**
   * Generate a hash code for this object.
   * @return The hash code of this UnicodeString.
   * @stable
   */
  inline int32_t hashCode(void) const;

  /**
   * Determine if this string is still valid.
   * @return TRUE if the string is valid, FALSE otherwise
   * @draft
   */
  inline bool_t isBogus(void) const;

  
  //========================================
  // Write operations
  //========================================

  /* Assignment operations */

  /**
   * Assignment operator.  Replace the characters in this UnicodeString
   * with the characters from <TT>srcText</TT>.
   * @param srcText The text containing the characters to replace
   * @return a reference to this
   * @stable
   */
   UnicodeString& operator= (const UnicodeString& srcText);

  /**
   * Assignment operator.  Replace the characters in this UnicodeString
   * with the character <TT>ch</TT>.
   * @param ch the character to replace
   * @return a reference to this
   * @draft
   */
  inline UnicodeString& operator= (UChar ch);

  /**
   * Set the text in the UnicodeString object to the characters
   * in <TT>srcText</TT> in the range 
   * [<TT>srcStart</TT>, <TT>srcStart + srcLength</TT>).
   * <TT>srcText</TT> is not modified.
   * @param srcText the source for the new characters
   * @param srcStart the offset into <TT>srcText</TT> where new characters
   * will be obtained
   * @param srcLength the number of characters in <TT>srcText</TT> in the
   * replace string.
   * @return a reference to this
   * @stable
   */
  inline UnicodeString& setTo(const UnicodeString& srcText, 
               UTextOffset srcStart, 
               int32_t srcLength);

  /**
   * Set the text in the UnicodeString object to the characters in 
   * <TT>srcText</TT>.  
   * <TT>srcText</TT> is not modified.
   * @param srcText the source for the new characters
   * @return a reference to this
   * @stable
   */
  inline UnicodeString& setTo(const UnicodeString& srcText);

  /**
   * Set the characters in the UnicodeString object to the characters
   * in <TT>srcChars</TT>. <TT>srcChars</TT> is not modified.
   * @param srcChars the source for the new characters
   * @param srcLength the number of Unicode characters in srcChars.
   * @return a reference to this
   * @stable
   */
  inline UnicodeString& setTo(const UChar *srcChars,
               int32_t srcLength);

  /**
   * Set the characters in the UnicodeString object to the character 
   * <TT>srcChar</TT>.
   * @param srcChar the character which becomes the UnicodeString's character 
   * content
   * @return a reference to this
   * @draft
   */
  UnicodeString& setTo(UChar srcChar);

  /**
   * Aliasing setTo() function, analogous to the readonly-aliasing UChar* constructor.
   * The text will be used for the UnicodeString object, but
   * it will not be released when the UnicodeString is destroyed.
   * This has copy-on-write semantics:
   * When the string is modified, then the buffer is first copied into
   * newly allocated memory.
   * The aliased buffer is never modified.
   * In an assignment to another UnicodeString, the text will be aliased again,
   * so that both strings then alias the same readonly-text.
   *
   * @param isTerminated specifies if <code>text</code> is <code>NUL</code>-terminated.
   *                     This must be true if <code>textLength==-1</code>.
   * @param text The characters to alias for the UnicodeString.
   * @param textLength The number of Unicode characters in <code>text</code> to alias.
   *                   If -1, then this constructor will determine the length
   *                   by calling <code>u_strlen()</code>.
   * @draft
   */
  UnicodeString &setTo(bool_t isTerminated,
                       const UChar *text,
                       int32_t textLength);

  /**
   * Aliasing setTo() function, analogous to the writeable-aliasing UChar* constructor.
   * The text will be used for the UnicodeString object, but
   * it will not be released when the UnicodeString is destroyed.
   * This has write-through semantics:
   * For as long as the capacity of the buffer is sufficient, write operations
   * will directly affect the buffer. When more capacity is necessary, then
   * a new buffer will be allocated and the contents copied as with regularly
   * constructed strings.
   * In an assignment to another UnicodeString, the buffer will be copied.
   * The extract(UChar *dst) function detects whether the dst pointer is the same
   * as the string buffer itself and will in this case not copy the contents.
   *
   * @param buffer The characters to alias for the UnicodeString.
   * @param buffLength The number of Unicode characters in <code>buffer</code> to alias.
   * @param buffCapacity The size of <code>buffer</code> in UChars.
   * @draft
   */
  UnicodeString &setTo(UChar *buffer,
                       int32_t buffLength,
                       int32_t buffCapacity);

  /**
   * Set the character at the specified offset to the specified character.
   * @param offset A valid offset into the text of the character to set
   * @param ch The new character
   * @return A reference to this
   * @draft
   */
  UnicodeString& setCharAt(UTextOffset offset, 
               UChar ch);


  /* Append operations */

  /**
   * Append operator. Append the code unit <TT>ch</TT> to the UnicodeString
   * object.
   * @param ch the code unit to be appended
   * @return a reference to this
   * @draft
   */
 inline  UnicodeString& operator+= (UChar ch);

  /**
   * Append operator. Append the code point <TT>ch</TT> to the UnicodeString
   * object.
   * @param ch the code point to be appended
   * @return a reference to this
   * @draft
   */
 inline  UnicodeString& operator+= (UChar32 ch);

  /**
   * Append operator. Append the characters in <TT>srcText</TT> to the
   * UnicodeString object at offset <TT>start</TT>. <TT>srcText</TT> is
   * not modified.
   * @param srcText the source for the new characters
   * @return a reference to this
   * @stable
   */
  inline UnicodeString& operator+= (const UnicodeString& srcText);

  /**
   * Append the characters
   * in <TT>srcText</TT> in the range 
   * [<TT>srcStart</TT>, <TT>srcStart + srcLength</TT>) to the 
   * UnicodeString object at offset <TT>start</TT>. <TT>srcText</TT> 
   * is not modified.
   * @param srcText the source for the new characters
   * @param srcStart the offset into <TT>srcText</TT> where new characters 
   * will be obtained
   * @param srcLength the number of characters in <TT>srcText</TT> in 
   * the append string
   * @return a reference to this
   * @stable
   */
  inline UnicodeString& append(const UnicodeString& srcText, 
            UTextOffset srcStart, 
            int32_t srcLength);

  /**
   * Append the characters in <TT>srcText</TT> to the UnicodeString object at 
   * offset <TT>start</TT>. <TT>srcText</TT> is not modified.
   * @param srcText the source for the new characters
   * @return a reference to this
   * @stable
   */
  inline UnicodeString& append(const UnicodeString& srcText);

  /**
   * Append the characters in <TT>srcChars</TT> in the range 
   * [<TT>srcStart</TT>, <TT>srcStart + srcLength</TT>) to the UnicodeString 
   * object at offset 
   * <TT>start</TT>. <TT>srcChars</TT> is not modified.
   * @param srcChars the source for the new characters
   * @param srcStart the offset into <TT>srcChars</TT> where new characters 
   * will be obtained
   * @param srcLength the number of characters in <TT>srcChars</TT> in 
   * the append string
   * @return a reference to this
   * @stable
   */
  inline UnicodeString& append(const UChar *srcChars, 
            UTextOffset srcStart, 
            int32_t srcLength);

  /**
   * Append the characters in <TT>srcChars</TT> to the UnicodeString object 
   * at offset <TT>start</TT>. <TT>srcChars</TT> is not modified.
   * @param srcChars the source for the new characters
   * @param srcLength the number of Unicode characters in <TT>srcChars</TT>
   * @return a reference to this
   * @stable
   */
  inline UnicodeString& append(const UChar *srcChars,
            int32_t srcLength);

  /**
   * Append the code unit <TT>srcChar</TT> to the UnicodeString object.
   * @param srcChar the code unit to append
   * @return a reference to this
   * @draft
   */
  inline UnicodeString& append(UChar srcChar);

  /**
   * Append the code point <TT>srcChar</TT> to the UnicodeString object.
   * @param srcChar the code point to append
   * @return a reference to this
   * @draft
   */
  inline UnicodeString& append(UChar32 srcChar);


  /* Insert operations */

  /**
   * Insert the characters in <TT>srcText</TT> in the range 
   * [<TT>srcStart</TT>, <TT>srcStart + srcLength</TT>) into the UnicodeString 
   * object at offset <TT>start</TT>. <TT>srcText</TT> is not modified.
   * @param start the offset where the insertion begins
   * @param srcText the source for the new characters 
   * @param srcStart the offset into <TT>srcText</TT> where new characters 
   * will be obtained
   * @param srcLength the number of characters in <TT>srcText</TT> in 
   * the insert string
   * @return a reference to this
   * @stable
   */
  inline UnicodeString& insert(UTextOffset start, 
            const UnicodeString& srcText, 
            UTextOffset srcStart, 
            int32_t srcLength);

  /**
   * Insert the characters in <TT>srcText</TT> into the UnicodeString object
   * at offset <TT>start</TT>. <TT>srcText</TT> is not modified.
   * @param start the offset where the insertion begins
   * @param srcText the source for the new characters 
   * @return a reference to this
   * @stable
   */
  inline UnicodeString& insert(UTextOffset start, 
            const UnicodeString& srcText);

  /**
   * Insert the characters in <TT>srcChars</TT> in the range 
   * [<TT>srcStart</TT>, <TT>srcStart + srcLength</TT>) into the UnicodeString
   *  object at offset <TT>start</TT>. <TT>srcChars</TT> is not modified.
   * @param start the offset at which the insertion begins
   * @param srcChars the source for the new characters
   * @param srcStart the offset into <TT>srcChars</TT> where new characters 
   * will be obtained
   * @param srcLength the number of characters in <TT>srcChars</TT> 
   * in the insert string
   * @return a reference to this
   * @stable
   */
  inline UnicodeString& insert(UTextOffset start, 
            const UChar *srcChars, 
            UTextOffset srcStart, 
            int32_t srcLength);

  /**
   * Insert the characters in <TT>srcChars</TT> into the UnicodeString object 
   * at offset <TT>start</TT>. <TT>srcChars</TT> is not modified.
   * @param start the offset where the insertion begins
   * @param srcChars the source for the new characters
   * @param srcLength the number of Unicode characters in srcChars.
   * @return a reference to this
   * @stable
   */
  inline UnicodeString& insert(UTextOffset start, 
            const UChar *srcChars,
            int32_t srcLength);

  /**
   * Insert the character <TT>srcChar</TT> into the UnicodeString object at 
   * offset <TT>start</TT>.
   * @param start the offset at which the insertion occurs
   * @param srcChar the character to insert
   * @return a reference to this
   * @draft
   */
  inline UnicodeString& insert(UTextOffset start, 
            UChar srcChar);


  /* Replace operations */

  /**
   * Replace the characters in the range 
   * [<TT>start</TT>, <TT>start + length</TT>) with the characters in 
   * <TT>srcText</TT> in the range 
   * [<TT>srcStart</TT>, <TT>srcStart + srcLength</TT>). 
   * <TT>srcText</TT> is not modified.
   * @param start the offset at which the replace operation begins
   * @param length the number of characters to replace. The character at 
   * <TT>start + length</TT> is not modified.
   * @param srcText the source for the new characters
   * @param srcStart the offset into <TT>srcText</TT> where new characters 
   * will be obtained
   * @param srcLength the number of characters in <TT>srcText</TT> in 
   * the replace string
   * @return a reference to this
   * @stable
   */
  UnicodeString& replace(UTextOffset start, 
             int32_t length, 
             const UnicodeString& srcText, 
             UTextOffset srcStart, 
             int32_t srcLength);

  /**
   * Replace the characters in the range 
   * [<TT>start</TT>, <TT>start + length</TT>) 
   * with the characters in <TT>srcText</TT>.  <TT>srcText</TT> is
   *  not modified.
   * @param start the offset at which the replace operation begins
   * @param length the number of characters to replace. The character at
   * <TT>start + length</TT> is not modified.
   * @param srcText the source for the new characters
   * @return a reference to this
   * @stable
   */
  UnicodeString& replace(UTextOffset start, 
             int32_t length, 
             const UnicodeString& srcText);

  /**
   * Replace the characters in the range 
   * [<TT>start</TT>, <TT>start + length</TT>) with the characters in 
   * <TT>srcChars</TT> in the range 
   * [<TT>srcStart</TT>, <TT>srcStart + srcLength</TT>). <TT>srcChars</TT> 
   * is not modified.
   * @param start the offset at which the replace operation begins
   * @param length the number of characters to replace.  The character at
   * <TT>start + length</TT> is not modified.
   * @param srcChars the source for the new characters
   * @param srcStart the offset into <TT>srcChars</TT> where new characters
   * will be obtained
   * @param srcLength the number of characters in <TT>srcChars</TT> 
   * in the replace string
   * @return a reference to this
   * @stable
   */
  UnicodeString& replace(UTextOffset start, 
             int32_t length, 
             const UChar *srcChars, 
             UTextOffset srcStart, 
             int32_t srcLength);

  /**
   * Replace the characters in the range 
   * [<TT>start</TT>, <TT>start + length</TT>) with the characters in
   * <TT>srcChars</TT>.  <TT>srcChars</TT> is not modified.
   * @param start the offset at which the replace operation begins
   * @param length number of characters to replace.  The character at
   * <TT>start + length</TT> is not modified.
   * @param srcChars the source for the new characters
   * @param srcLength the number of Unicode characters in srcChars
   * @return a reference to this
   * @stable
   */
  inline UnicodeString& replace(UTextOffset start, 
             int32_t length, 
             const UChar *srcChars,
             int32_t srcLength);

  /**
   * Replace the characters in the range 
   * [<TT>start</TT>, <TT>start + length</TT>) with the character
   * <TT>srcChar</TT>.
   * @param start the offset at which the replace operation begins
   * @param length the number of characters to replace.  The character at
   * <TT>start + length</TT> is not modified.
   * @param srcChar the new character
   * @return a reference to this
   * @draft
   */
  inline UnicodeString& replace(UTextOffset start, 
             int32_t length, 
             UChar srcChar);

  /**
   * Replace the characters in the range [<TT>start</TT>, <TT>limit</TT>) 
   * with the characters in <TT>srcText</TT>. <TT>srcText</TT> is not modified.
   * @param start the offset at which the replace operation begins
   * @param limit the offset immediately following the replace range
   * @param srcText the source for the new characters
   * @return a reference to this
   * @stable
   */
  inline UnicodeString& replaceBetween(UTextOffset start, 
                UTextOffset limit, 
                const UnicodeString& srcText);

  /**
   * Replace the characters in the range [<TT>start</TT>, <TT>limit</TT>) 
   * with the characters in <TT>srcText</TT> in the range 
   * [<TT>srcStart</TT>, <TT>srcLimit</TT>). <TT>srcText</TT> is not modified.
   * @param start the offset at which the replace operation begins
   * @param limit the offset immediately following the replace range
   * @param srcText the source for the new characters
   * @param srcStart the offset into <TT>srcChars</TT> where new characters 
   * will be obtained
   * @param srcLimit the offset immediately following the range to copy 
   * in <TT>srcText</TT>
   * @return a reference to this
   * @stable
   */
  inline UnicodeString& replaceBetween(UTextOffset start, 
                UTextOffset limit, 
                const UnicodeString& srcText, 
                UTextOffset srcStart, 
                UTextOffset srcLimit);

  /**
   * Replace a substring of this object with the given text.
   * @param start the beginning index, inclusive; <code>0 <= start
   * <= limit</code>.
   * @param limit the ending index, exclusive; <code>start <= limit
   * <= length()</code>.
   * @param text the text to replace characters <code>start</code>
   * to <code>limit - 1</code>
   * @stable
   */
  virtual void handleReplaceBetween(UTextOffset start,
                                    UTextOffset limit,
                                    const UnicodeString& text);


  /* Search and replace operations */

  /**
   * Replace all occurrences of characters in oldText with the characters 
   * in newText
   * @param oldText the text containing the search text
   * @param newText the text containing the replacement text
   * @return a reference to this
   * @stable
   */
  inline UnicodeString& findAndReplace(const UnicodeString& oldText,
                const UnicodeString& newText);

  /**
   * Replace all occurrences of characters in oldText with characters 
   * in newText
   * in the range [<TT>start</TT>, <TT>start + length</TT>).
   * @param start the start of the range in which replace will performed
   * @param length the length of the range in which replace will be performed
   * @param oldText the text containing the search text
   * @param newText the text containing the replacement text
   * @return a reference to this
   * @stable
   */
  inline UnicodeString& findAndReplace(UTextOffset start,
                int32_t length,
                const UnicodeString& oldText,
                const UnicodeString& newText);

  /**
   * Replace all occurrences of characters in oldText in the range 
   * [<TT>oldStart</TT>, <TT>oldStart + oldLength</TT>) with the characters 
   * in newText in the range 
   * [<TT>newStart</TT>, <TT>newStart + newLength</TT>) 
   * in the range [<TT>start</TT>, <TT>start + length</TT>).
   * @param start the start of the range in which replace will performed
   * @param length the length of the range in which replace will be performed
   * @param oldText the text containing the search text
   * @param oldStart the start of the search range in <TT>oldText</TT>
   * @param oldLength the length of the search range in <TT>oldText</TT>
   * @param newText the text containing the replacement text
   * @param newStart the start of the replacement range in <TT>newText</TT>
   * @param newLength the length of the replacement range in <TT>newText</TT>
   * @return a reference to this
   * @stable
   */
  UnicodeString& findAndReplace(UTextOffset start,
                int32_t length,
                const UnicodeString& oldText,
                UTextOffset oldStart,
                int32_t oldLength,
                const UnicodeString& newText,
                UTextOffset newStart,
                int32_t newLength);


  /* Remove operations */

  /**
   * Remove all characters from the UnicodeString object.
   * @return a reference to this
   * @stable
   */
  inline UnicodeString& remove(void);

  /**
   * Remove the characters in the range 
   * [<TT>start</TT>, <TT>start + length</TT>) from the UnicodeString object.
   * @param start the offset of the first character to remove
   * @param length the number of characters to remove
   * @return a reference to this
   * @stable
   */
  inline UnicodeString& remove(UTextOffset start, 
                               int32_t length = INT32_MAX);

  /**
   * Remove the characters in the range 
   * [<TT>start</TT>, <TT>limit</TT>) from the UnicodeString object.
   * @param start the offset of the first character to remove
   * @param limit the offset immediately following the range to remove
   * @return a reference to this
   * @stable
   */
  inline UnicodeString& removeBetween(UTextOffset start,
                                      UTextOffset limit = INT32_MAX);


  /* Length operations */

  /**
   * Pad the start of this UnicodeString with the character <TT>padChar</TT>.  
   * If the length of this UnicodeString is less than targetLength, 
   * length() - targetLength copies of padChar will be added to the
   * beginning of this UnicodeString.
   * @param targetLength the desired length of the string
   * @param padChar the character to use for padding. Defaults to 
   * space (U+0020)
   * @return TRUE if the text was padded, FALSE otherwise.
   * @draft
   */
  bool_t padLeading(int32_t targetLength,
                    UChar padChar = 0x0020);

  /**
   * Pad the end of this UnicodeString with the character <TT>padChar</TT>.  
   * If the length of this UnicodeString is less than targetLength, 
   * length() - targetLength copies of padChar will be added to the
   * end of this UnicodeString.
   * @param targetLength the desired length of the string
   * @param padChar the character to use for padding. Defaults to 
   * space (U+0020)
   * @return TRUE if the text was padded, FALSE otherwise.
   * @draft
   */
  bool_t padTrailing(int32_t targetLength,
                     UChar padChar = 0x0020);

  /**
   * Truncate this UnicodeString to the <TT>targetLength</TT>.
   * @param targetLength the desired length of this UnicodeString.
   * @return TRUE if the text was truncated, FALSE otherwise
   * @stable
   */
  inline bool_t truncate(int32_t targetLength);

  /**
   * Trims leading and trailing whitespace from this UnicodeString.
   * @return a reference to this
   * @stable
   */
  UnicodeString& trim(void);


  /* Miscellaneous operations */

  /**
   * Reverse this UnicodeString in place.
   * @return a reference to this
   * @stable
   */
  inline UnicodeString& reverse(void);

  /**
   * Reverse the range [<TT>start</TT>, <TT>start + length</TT>) in
   * this UnicodeString.
   * @param start the start of the range to reverse
   * @param length the number of characters to to reverse
   * @return a reference to this
   * @stable
   */
  inline UnicodeString& reverse(UTextOffset start,
             int32_t length);

  /**
   * Convert the characters in this to UPPER CASE following the conventions of
   * the default locale.
   * @return A reference to this.
   * @stable
   */
  UnicodeString& toUpper(void);

  /**
   * Convert the characters in this to UPPER CASE following the conventions of
   * a specific locale.
   * @param locale The locale containing the conventions to use.
   * @return A reference to this.
   * @stable
   */
  UnicodeString& toUpper(const Locale& locale);

  /**
   * Convert the characters in this to UPPER CASE following the conventions of
   * the default.
   * @return A reference to this.
   * @stable
   */
  UnicodeString& toLower(void);

  /**
   * Convert the characters in this to UPPER CASE following the conventions of
   * a specific locale.
   * @param locale The locale containing the conventions to use.
   * @return A reference to this.
   * @stable
   */
  UnicodeString& toLower(const Locale& locale);


  //========================================
  // Constructors
  //========================================

  /**
   * Dummy enumeration for the disambiguation of function signatures.
   */
  enum ESignature {
    kCodePoint
  };

  /** Construct an empty UnicodeString.  
   * @stable
   */
  UnicodeString();


  /**
   * Construct a UnicodeString with capacity to hold <TT>capacity</TT> UChars
   * @param capacity the number of UChars this UnicodeString should hold
   * before a resize is necessary
   * @stable
   */
  UnicodeString(int32_t capacity);

  /**
   * Single UChar (code unit) constructor.
   * @param ch the character to place in the UnicodeString
   * @draft
   */
  UnicodeString(UChar ch);

  /**
   * Single UChar32 (code point) constructor.
   * @param ch the character to place in the UnicodeString
   * @param sig is a dummy parameter that guarantees that this
   *        constructor can be distinguished from
   *        <code>UnicodeString(int32_t capacity)</code>
   *        on all platforms
   * @draft
   *
   * Usage example:
   * <pre>
   * UChar32 c=0x104321;
   * UnicodeString us(c, UnicodeString::kCodePoint);
   * </pre>
   */
  UnicodeString(UChar32 ch, ESignature sig);

  /**
   * UChar* constructor.
   * @param text The characters to place in the UnicodeString.  <TT>text</TT>
   * must be NULL (U+0000) terminated.
   * @stable
   */
  UnicodeString(const UChar *text);

  /**
   * UChar* constructor.
   * @param text The characters to place in the UnicodeString.
   * @param textLength The number of Unicode characters in <TT>text</TT>
   * to copy.
   * @stable
   */
  UnicodeString(const UChar *text,
        int32_t textLength);

  /**
   * Readonly-aliasing UChar* constructor.
   * The text will be used for the UnicodeString object, but
   * it will not be released when the UnicodeString is destroyed.
   * This has copy-on-write semantics:
   * When the string is modified, then the buffer is first copied into
   * newly allocated memory.
   * The aliased buffer is never modified.
   * In an assignment to another UnicodeString, the text will be aliased again,
   * so that both strings then alias the same readonly-text.
   *
   * @param isTerminated specifies if <code>text</code> is <code>NUL</code>-terminated.
   *                     This must be true if <code>textLength==-1</code>.
   * @param text The characters to alias for the UnicodeString.
   * @param textLength The number of Unicode characters in <code>text</code> to alias.
   *                   If -1, then this constructor will determine the length
   *                   by calling <code>u_strlen()</code>.
   * @stable
   */
  UnicodeString(bool_t isTerminated,
                const UChar *text,
                int32_t textLength);

  /**
   * Writeable-aliasing UChar* constructor.
   * The text will be used for the UnicodeString object, but
   * it will not be released when the UnicodeString is destroyed.
   * This has write-through semantics:
   * For as long as the capacity of the buffer is sufficient, write operations
   * will directly affect the buffer. When more capacity is necessary, then
   * a new buffer will be allocated and the contents copied as with regularly
   * constructed strings.
   * In an assignment to another UnicodeString, the buffer will be copied.
   * The extract(UChar *dst) function detects whether the dst pointer is the same
   * as the string buffer itself and will in this case not copy the contents.
   *
   * @param buffer The characters to alias for the UnicodeString.
   * @param buffLength The number of Unicode characters in <code>buffer</code> to alias.
   * @param buffCapacity The size of <code>buffer</code> in UChars.
   * @draft
   */
  UnicodeString(UChar *buffer, int32_t buffLength, int32_t buffCapacity);

  /**
   * char* constructor.
   * @param codepageData an array of bytes, null-terminated
   * @param codepage the encoding of <TT>codepageData</TT>.  The special
   * value 0 for <TT>codepage</TT> indicates that the text is in the 
   * platform's default codepage.
   * If <code>codepage</code> is an empty string (<code>""</code>),
   * then a simple conversion is performed on the codepage-invariant
   * subset ("invariant characters") of the platform encoding. See utypes.h.
   * @stable
   */
  UnicodeString(const char *codepageData,
        const char *codepage = 0);

  /**
   * char* constructor.
   * @param codepageData an array of bytes.
   * @param dataLength The number of bytes in <TT>codepageData</TT>.
   * @param codepage the encoding of <TT>codepageData</TT>.  The special
   * value 0 for <TT>codepage</TT> indicates that the text is in the 
   * platform's default codepage.
   * If <code>codepage</code> is an empty string (<code>""</code>),
   * then a simple conversion is performed on the codepage-invariant
   * subset ("invariant characters") of the platform encoding. See utypes.h.
   * @stable
   */
  UnicodeString(const char *codepageData,
        int32_t dataLength,
        const char *codepage = 0);

  /**
   * Copy constructor.
   * @param that The UnicodeString object to copy.
   * @stable
   */
  UnicodeString(const UnicodeString& that);

  /** Destructor. 
   * @stable
  */
  ~UnicodeString();


  /* Miscellaneous operations */

  /**
   * Returns the number of display cells occupied by the range
   * [<TT>start</TT>, <TT>length</TT>).
   * This function is designed for Asian text and properly takes into account
   * halfwidth and fullwidth variants of various CJK characters and the 
   * combining behavior of the Hangul Jamo characters (with some limitations;
   * see documentation for Unicode::getCellWidth()).<BR>
   * In order to avoid dealing with fractions, this function can either be
   * construed to return twice the actual number of display cells or to 
   * treat a "cell" as the width of a halfwidth character rather than the
   * width of a fullwidth character.
   * @param start the start of the range
   * @param length the number of characters to measure
   * @param asian The <TT>asian</TT> parameter controls whether characters
   * considered NEUTRAL by the Unicode class are treated as halfwidth or 
   * fullwidth here.  If you set <TT>asian</TT> to FALSE, neutrals are 
   * treated as halfwidth, and this function returns a close approximation
   * of how many Latin display cells the text will take up in a monospaced
   * font.
   * @return the number of display cells occupied by the specified substring.
   * @stable
   */
  int32_t numDisplayCells(UTextOffset start = 0,
              int32_t length = INT32_MAX,
              bool_t asian = TRUE) const;


  UCharReference operator[] (UTextOffset pos);

  //========================================
  // Implementation methods
  //========================================
  
private:

  inline int8_t
  doCompare(UTextOffset start,
           int32_t length,
           const UnicodeString& srcText,
           UTextOffset srcStart,
           int32_t srcLength) const;
  
  int8_t doCompare(UTextOffset start,
           int32_t length,
           const UChar *srcChars,
           UTextOffset srcStart,
           int32_t srcLength) const;

  UTextOffset doIndexOf(UChar c,
            UTextOffset start,
            int32_t length) const;

  UTextOffset doLastIndexOf(UChar c,
                UTextOffset start,
                int32_t length) const;

  void doExtract(UTextOffset start, 
         int32_t length, 
         UChar *dst, 
         UTextOffset dstStart) const;
  
  inline void doExtract(UTextOffset start,
         int32_t length,
         UnicodeString& target) const;
  
  inline UChar doCharAt(UTextOffset offset)  const;

  UnicodeString& doReplace(UTextOffset start, 
               int32_t length, 
               const UnicodeString& srcText, 
               UTextOffset srcStart, 
               int32_t srcLength);

  UnicodeString& doReplace(UTextOffset start, 
               int32_t length, 
               const UChar *srcChars, 
               UTextOffset srcStart, 
               int32_t srcLength);

  UnicodeString& doReverse(UTextOffset start,
               int32_t length);

  // calculate hash code
  int32_t doHashCode(void);
  
  // get pointer to start of array
  inline UChar* getArrayStart(void);
  inline const UChar* getArrayStart(void) const;

  // get the "real" capacity of the array, adjusted for ref count
  inline int32_t getCapacity(void) const;

  // allocate the array; result may be fStackBuffer
  // sets refCount to 1 if appropriate
  // sets fArray, fCapacity, and fFlags
  // returns boolean for success or failure
  bool_t allocate(int32_t capacity);

  // release the array if owned
  inline void releaseArray();

  // utility method to get around lack of exception handling
  void setToBogus(void);

  // Pin start and limit to acceptable values.
  inline void pinIndices(UTextOffset& start,
                         int32_t& length) const;

  /*
   * Real constructor for converting from codepage data.
   * It assumes that it is called with !fRefCounted.
   *
   * If <code>codepage==0</code>, then the default converter
   * is used for the platform encoding.
   * If <code>codepage</code> is an empty string (<code>""</code>),
   * then a simple conversion is performed on the codepage-invariant
   * subset ("invariant characters") of the platform encoding. See utypes.h.
   */
  void doCodepageCreate(const char *codepageData,
                        int32_t dataLength,
                        const char *codepage);

  /*
   * This function is called when write access to the array
   * is necessary.
   *
   * We need to make a copy of the array if
   * the buffer is read-only, or
   * the buffer is refCounted (shared), and refCount>1, or
   * the buffer is too small.
   *
   * Return FALSE if memory could not be allocated.
   */
  bool_t cloneArrayIfNeeded(int32_t newCapacity = -1,
                            int32_t growCapacity = -1,
                            bool_t doCopyArray = TRUE,
                            int32_t **pBufferToDelete = 0);

  // ref counting
  inline int32_t addRef(void);
  inline int32_t removeRef(void);
  inline int32_t refCount(void) const;
  inline int32_t setRefCount(int32_t count);

  // constants
  enum {
    US_STACKBUF_SIZE=9, // Size of stack buffer for small strings
    kInvalidUChar=0xffff, // invalid UChar index
    kGrowSize=128, // grow size for this buffer
    kInvalidHashCode=0, // invalid hash code
    kEmptyHashCode=1, // hash code for empty string

    // bit flag values for fFlags
    kIsBogus=1, // this string is bogus, i.e., not valid
    kUsingStackBuffer=2, // fArray==fStackBuffer
    kRefCounted=4, // there is a refCount field before the characters in fArray
    kBufferIsReadonly=8, // do not write to this buffer

    // combined values for convenience
    kShortString=kUsingStackBuffer,
    kLongString=kRefCounted,
    kReadonlyAlias=kBufferIsReadonly,
    kWriteableAlias=0
  };

  // statics

  // default converter cache
  static UConverter* getDefaultConverter(UErrorCode& status);
  static void releaseDefaultConverter(UConverter *converter);

  static UConverter *fgDefaultConverter;

  friend class UnicodeStringStreamer;
  friend class UnicodeConverterCPP;
  friend U_COMMON_API ostream &operator<<(ostream& stream, const UnicodeString& s);
  friend class StringCharacterIterator;

  /*
   * The following are all the class fields that are stored
   * in each UnicodeString object.
   * Note that UnicodeString has virtual functions,
   * therefore there is an implicit vtable pointer
   * as the first real field.
   * The fields should be aligned such that no padding is
   * necessary, mostly by having larger types first.
   * On 32-bit machines, the size should be 40 bytes,
   * on 64-bit machines (8-byte pointers), it should be 48 bytes.
   */
  // (implicit) *vtable;
  UChar     *fArray;        // the Unicode data
  int32_t   fLength;        // number characters in fArray
  int32_t   fCapacity;      // sizeof fArray
  int32_t   fHashCode;      // the hash code
  uint16_t  fFlags;         // bit flags: see constants above
  UChar     fStackBuffer [ US_STACKBUF_SIZE ]; // buffer for small strings

public:

  //========================================
  // Deprecated API
  //========================================

  /* size() -> length()
   * @deprecated */
  inline int32_t size(void) const;

  // parameters reordered for consistency
   /* @deprecated */
  inline UnicodeString& findAndReplace(const UnicodeString& oldText,
                const UnicodeString& newText,
                UTextOffset start,
                int32_t length);

   /* @deprecated */
  inline void* operator new(size_t size);
   /* @deprecated */
  inline void* operator new(size_t size, void *location);
   /* @deprecated */
  inline void operator delete(void *location);

  //========================================
  // Non-public API - will be removed!
  //========================================
  /* @deprecated */
  const UChar* getUChars() const;
};

//========================================
// Array copying
//========================================
// Copy an array of UnicodeString OBJECTS (not pointers).
inline void 
uprv_arrayCopy(const UnicodeString *src, UnicodeString *dst, int32_t count)
{ while(count-- > 0) *dst++ = *src++; }

inline void 
uprv_arrayCopy(const UnicodeString *src, int32_t srcStart, 
        UnicodeString *dst, int32_t dstStart, int32_t count)
{ uprv_arrayCopy(src+srcStart, dst+dstStart, count); }


//========================================
// Inline members
//========================================

//========================================
// Read-only alias methods
//========================================
inline bool_t
UnicodeString::operator== (const UnicodeString& text) const
{
  if(isBogus()) {
    return text.isBogus();
  } else {
    return
      !text.isBogus() &&
      fLength == text.fLength &&
      doCompare(0, fLength, text, 0, text.fLength) == 0;
  }
}

inline bool_t
UnicodeString::operator!= (const UnicodeString& text) const
{ return (! operator==(text)); }

inline bool_t
UnicodeString::operator> (const UnicodeString& text) const
{ return doCompare(0, fLength, text, 0, text.fLength) == 1; }

inline bool_t
UnicodeString::operator< (const UnicodeString& text) const
{ return doCompare(0, fLength, text, 0, text.fLength) == -1; }

inline bool_t
UnicodeString::operator>= (const UnicodeString& text) const
{ return doCompare(0, fLength, text, 0, text.fLength) != -1; }

inline bool_t
UnicodeString::operator<= (const UnicodeString& text) const
{ return doCompare(0, fLength, text, 0, text.fLength) != 1; }

inline int8_t 
UnicodeString::compare(const UnicodeString& text) const
{ return doCompare(0, fLength, text, 0, text.fLength); }

inline int8_t 
UnicodeString::compare(UTextOffset start,
               int32_t length,
               const UnicodeString& srcText) const
{ return doCompare(start, length, srcText, 0, srcText.fLength); }

inline int8_t 
UnicodeString::compare(const UChar *srcChars,
               int32_t srcLength) const
{ return doCompare(0, fLength, srcChars, 0, srcLength); }

inline int8_t 
UnicodeString::compare(UTextOffset start,
               int32_t length,
               const UnicodeString& srcText,
               UTextOffset srcStart,
               int32_t srcLength) const
{ return doCompare(start, length, srcText, srcStart, srcLength); }

inline int8_t
UnicodeString::compare(UTextOffset start,
               int32_t length,
               const UChar *srcChars) const
{ return doCompare(start, length, srcChars, 0, length); }

inline int8_t 
UnicodeString::compare(UTextOffset start,
               int32_t length,
               const UChar *srcChars,
               UTextOffset srcStart,
               int32_t srcLength) const
{ return doCompare(start, length, srcChars, srcStart, srcLength); }

inline int8_t
UnicodeString::compareBetween(UTextOffset start,
                  UTextOffset limit,
                  const UnicodeString& srcText,
                  UTextOffset srcStart,
                  UTextOffset srcLimit) const
{ return doCompare(start, limit - start, 
           srcText, srcStart, srcLimit - srcStart); }

inline int8_t
UnicodeString::doCompare(UTextOffset start,
              int32_t length,
              const UnicodeString& srcText,
              UTextOffset srcStart,
              int32_t srcLength) const
{
  const UChar *srcChars;
  if(!srcText.isBogus()) {
    srcText.pinIndices(srcStart, srcLength);
    srcChars=srcText.getArrayStart();
  } else {
    srcChars=0;
  }
  return doCompare(start, length, srcChars, srcStart, srcLength);
}

inline UTextOffset 
UnicodeString::indexOf(const UnicodeString& text) const
{ return indexOf(text, 0, text.fLength, 0, fLength); }

inline UTextOffset 
UnicodeString::indexOf(const UnicodeString& text,
               UTextOffset start) const
{ return indexOf(text, 0, text.fLength, start, fLength - start); }

inline UTextOffset 
UnicodeString::indexOf(const UnicodeString& text,
               UTextOffset start,
               int32_t length) const
{ return indexOf(text, 0, text.fLength, start, length); }

inline UTextOffset 
UnicodeString::indexOf(const UnicodeString& srcText,
               UTextOffset srcStart,
               int32_t srcLength,
               UTextOffset start,
               int32_t length) const
{
  if(!srcText.isBogus()) {
    srcText.pinIndices(srcStart, srcLength);
    if(srcLength > 0) {
      return indexOf(srcText.getArrayStart(), srcStart, srcLength, start, length);
    }
  }
  return -1;
}

inline UTextOffset 
UnicodeString::indexOf(const UChar *srcChars,
               int32_t srcLength,
               UTextOffset start) const
{ return indexOf(srcChars, 0, srcLength, start, fLength - start); }

inline UTextOffset 
UnicodeString::indexOf(const UChar *srcChars,
               int32_t srcLength,
               UTextOffset start,
               int32_t length) const
{ return indexOf(srcChars, 0, srcLength, start, length); }

inline UTextOffset 
UnicodeString::indexOf(UChar c) const
{ return doIndexOf(c, 0, fLength); }

inline UTextOffset 
UnicodeString::indexOf(UChar32 c) const {
  if(!UTF_NEED_MULTIPLE_UCHAR(c)) {
    return doIndexOf((UChar)c, 0, fLength);
  } else {
    UChar buffer[UTF_MAX_CHAR_LENGTH];
    int32_t length = 0;
    UTF_APPEND_CHAR_UNSAFE(buffer, length, c);
    return indexOf(buffer, length, 0);
  }
}

inline UTextOffset 
UnicodeString::indexOf(UChar c,
               UTextOffset start) const
{ return doIndexOf(c, start, fLength - start); }

inline UTextOffset 
UnicodeString::indexOf(UChar32 c,
               UTextOffset start) const {
  if(!UTF_NEED_MULTIPLE_UCHAR(c)) {
    return doIndexOf((UChar)c, start, fLength - start);
  } else {
    UChar buffer[UTF_MAX_CHAR_LENGTH];
    int32_t length = 0;
    UTF_APPEND_CHAR_UNSAFE(buffer, length, c);
    return indexOf(buffer, length, start);
  }
}

inline UTextOffset 
UnicodeString::indexOf(UChar c,
               UTextOffset start,
               int32_t length) const
{ return doIndexOf(c, start, length); }

inline UTextOffset 
UnicodeString::indexOf(UChar32 c,
               UTextOffset start,
               int32_t length) const {
  if(!UTF_NEED_MULTIPLE_UCHAR(c)) {
    return doIndexOf((UChar)c, start, length);
  } else {
    UChar buffer[UTF_MAX_CHAR_LENGTH];
    int32_t cLength = 0;
    UTF_APPEND_CHAR_UNSAFE(buffer, cLength, c);
    return indexOf(buffer, cLength, start, length);
  }
}

inline UTextOffset 
UnicodeString::lastIndexOf(const UnicodeString& text) const
{ return lastIndexOf(text, 0, text.fLength, 0, fLength); }

inline UTextOffset 
UnicodeString::lastIndexOf(const UnicodeString& text,
               UTextOffset start) const
{ return lastIndexOf(text, 0, text.fLength, start, fLength - start); }

inline UTextOffset 
UnicodeString::lastIndexOf(const UnicodeString& text,
               UTextOffset start,
               int32_t length) const
{ return lastIndexOf(text, 0, text.fLength, start, length); }

inline UTextOffset 
UnicodeString::lastIndexOf(const UnicodeString& srcText,
               UTextOffset srcStart,
               int32_t srcLength,
               UTextOffset start,
               int32_t length) const
{
  if(!srcText.isBogus()) {
    srcText.pinIndices(srcStart, srcLength);
    if(srcLength > 0) {
      return lastIndexOf(srcText.getArrayStart(), srcStart, srcLength, start, length);
    }
  }
  return -1;
}

inline UTextOffset 
UnicodeString::lastIndexOf(const UChar *srcChars,
               int32_t srcLength,
               UTextOffset start) const
{ return lastIndexOf(srcChars, 0, srcLength, start, fLength - start); }

inline UTextOffset 
UnicodeString::lastIndexOf(const UChar *srcChars,
               int32_t srcLength,
               UTextOffset start,
               int32_t length) const
{ return lastIndexOf(srcChars, 0, srcLength, start, length); }

inline UTextOffset 
UnicodeString::lastIndexOf(UChar c) const
{ return doLastIndexOf(c, 0, fLength); }

inline UTextOffset 
UnicodeString::lastIndexOf(UChar32 c) const {
  if(!UTF_NEED_MULTIPLE_UCHAR(c)) {
    return doLastIndexOf((UChar)c, 0, fLength);
  } else {
    UChar buffer[UTF_MAX_CHAR_LENGTH];
    int32_t count = 0;
    UTF_APPEND_CHAR_UNSAFE(buffer, count, c);
    return lastIndexOf(buffer, count, 0);
  }
}

inline UTextOffset 
UnicodeString::lastIndexOf(UChar c,
               UTextOffset start) const
{ return doLastIndexOf(c, start, fLength - start); }

inline UTextOffset 
UnicodeString::lastIndexOf(UChar32 c,
               UTextOffset start) const {
  if(!UTF_NEED_MULTIPLE_UCHAR(c)) {
    return doLastIndexOf((UChar)c, start, fLength - start);
  } else {
    UChar buffer[UTF_MAX_CHAR_LENGTH];
    int32_t count = 0;
    UTF_APPEND_CHAR_UNSAFE(buffer, count, c);
    return lastIndexOf(buffer, count, start);
  }
}

inline UTextOffset 
UnicodeString::lastIndexOf(UChar c,
               UTextOffset start,
               int32_t length) const
{ return doLastIndexOf(c, start, length); }

inline UTextOffset 
UnicodeString::lastIndexOf(UChar32 c,
               UTextOffset start,
               int32_t length) const {
  if(!UTF_NEED_MULTIPLE_UCHAR(c)) {
    return doLastIndexOf((UChar)c, start, length);
  } else {
    UChar buffer[UTF_MAX_CHAR_LENGTH];
    int32_t count = 0;
    UTF_APPEND_CHAR_UNSAFE(buffer, count, c);
    return lastIndexOf(buffer, count, start, length);
  }
}

inline bool_t 
UnicodeString::startsWith(const UnicodeString& text) const
{ return compare(0, text.fLength, text, 0, text.fLength) == 0; }

inline bool_t 
UnicodeString::startsWith(const UnicodeString& srcText,
              UTextOffset srcStart,
              int32_t srcLength) const
{ return doCompare(0, srcLength, srcText, srcStart, srcLength) == 0; }

inline bool_t 
UnicodeString::startsWith(const UChar *srcChars,
              int32_t srcLength) const
{ return doCompare(0, srcLength, srcChars, 0, srcLength) == 0; }

inline bool_t 
UnicodeString::startsWith(const UChar *srcChars,
              UTextOffset srcStart,
              int32_t srcLength) const
{ return doCompare(0, srcLength, srcChars, srcStart, srcLength) == 0;}

inline bool_t 
UnicodeString::endsWith(const UnicodeString& text) const
{ return doCompare(fLength - text.fLength, text.fLength, 
           text, 0, text.fLength) == 0; }

inline bool_t 
UnicodeString::endsWith(const UnicodeString& srcText,
            UTextOffset srcStart,
            int32_t srcLength) const
{ return doCompare(fLength - srcLength, srcLength, 
           srcText, srcStart, srcLength) == 0; }

inline bool_t 
UnicodeString::endsWith(const UChar *srcChars,
            int32_t srcLength) const
{ return doCompare(fLength - srcLength, srcLength, 
           srcChars, 0, srcLength) == 0; }

inline bool_t 
UnicodeString::endsWith(const UChar *srcChars,
            UTextOffset srcStart,
            int32_t srcLength) const
{ return doCompare(fLength - srcLength, srcLength, 
           srcChars, srcStart, srcLength) == 0;}
//========================================
// replace
//========================================
inline UnicodeString& 
UnicodeString::replace(UTextOffset start, 
               int32_t length, 
               const UnicodeString& srcText) 
{ return doReplace(start, length, srcText, 0, srcText.fLength); }

inline UnicodeString& 
UnicodeString::replace(UTextOffset start, 
               int32_t length, 
               const UnicodeString& srcText, 
               UTextOffset srcStart, 
               int32_t srcLength)
{ return doReplace(start, length, srcText, srcStart, srcLength); }

inline UnicodeString& 
UnicodeString::replace(UTextOffset start, 
               int32_t length, 
               const UChar *srcChars,
               int32_t srcLength)
{ return doReplace(start, length, srcChars, 0, srcLength); }

inline UnicodeString& 
UnicodeString::replace(UTextOffset start, 
               int32_t length, 
               const UChar *srcChars, 
               UTextOffset srcStart, 
               int32_t srcLength)
{ return doReplace(start, length, srcChars, srcStart, srcLength); }

inline UnicodeString& 
UnicodeString::replace(UTextOffset start, 
               int32_t length, 
               UChar srcChar)
{ return doReplace(start, length, &srcChar, 0, 1); }

inline UnicodeString& 
UnicodeString::replaceBetween(UTextOffset start, 
                  UTextOffset limit, 
                  const UnicodeString& srcText)
{ return doReplace(start, limit - start, srcText, 0, srcText.fLength); }

inline UnicodeString&
UnicodeString::replaceBetween(UTextOffset start, 
                  UTextOffset limit, 
                  const UnicodeString& srcText, 
                  UTextOffset srcStart, 
                  UTextOffset srcLimit)
{ return doReplace(start, limit - start, srcText, srcStart, srcLimit - srcStart); }

inline UnicodeString& 
UnicodeString::findAndReplace(const UnicodeString& oldText,
                  const UnicodeString& newText)
{ return findAndReplace(0, fLength, oldText, 0, oldText.fLength, 
            newText, 0, newText.fLength); }

inline UnicodeString& 
UnicodeString::findAndReplace(UTextOffset start,
                  int32_t length,
                  const UnicodeString& oldText,
                  const UnicodeString& newText)
{ return findAndReplace(start, length, oldText, 0, oldText.fLength, 
            newText, 0, newText.fLength); }

// ============================
// extract
// ============================
inline void
UnicodeString::doExtract(UTextOffset start,
             int32_t length,
             UnicodeString& target) const
{ target.replace(0, target.fLength, *this, start, length); }

inline void  
UnicodeString::extract(UTextOffset start, 
               int32_t length, 
               UChar *dst, 
               UTextOffset dstStart) const
{ doExtract(start, length, dst, dstStart); }

inline void 
UnicodeString::extract(UTextOffset start,
               int32_t length,
               UnicodeString& target) const
{ doExtract(start, length, target); }

inline void  
UnicodeString::extractBetween(UTextOffset start, 
                  UTextOffset limit, 
                  UChar *dst, 
                  UTextOffset dstStart) const
{ doExtract(start, limit - start, dst, dstStart); }

inline void 
UnicodeString::extractBetween(UTextOffset start,
                  UTextOffset limit,
                  UnicodeString& target) const
{ doExtract(start, limit - start, target); }

inline UChar
UnicodeString::doCharAt(UTextOffset offset) const
{
  if((uint32_t)offset < (uint32_t)fLength) {
    return fArray[offset];
  } else {
    return kInvalidUChar;
  }
}

inline UChar
UnicodeString::charAt(UTextOffset offset) const
{ return doCharAt(offset); }

inline UChar
UnicodeString::operator[] (UTextOffset offset) const
{ return doCharAt(offset); }

inline UChar32
UnicodeString::char32At(UTextOffset offset) const
{
  if((uint32_t)offset < (uint32_t)fLength) {
    UChar32 c;
    UTF_GET_CHAR(fArray, 0, offset, fLength, c);
    return c;
  } else {
    return kInvalidUChar;
  }
}

inline UTextOffset
UnicodeString::getCharStart(UTextOffset offset) {
  if((uint32_t)offset < (uint32_t)fLength) {
    UTF_SET_CHAR_START(fArray, 0, offset);
    return offset;
  } else {
    return 0;
  }
}

inline UTextOffset
UnicodeString::getCharLimit(UTextOffset offset) {
  if((uint32_t)offset < (uint32_t)fLength) {
    UTF_SET_CHAR_LIMIT(fArray, 0, offset, fLength);
    return offset;
  } else {
    return fLength;
  }
}

inline bool_t
UnicodeString::empty() const
{ return fLength == 0; }

//========================================
// Read-only implementation methods
//========================================
inline int32_t  
UnicodeString::length() const
{ return fLength; }

inline int32_t 
UnicodeString::hashCode() const
{ return (fHashCode == kInvalidHashCode 
    ? ((UnicodeString*)this)->doHashCode() : fHashCode); }

//========================================
// Write alias methods
//========================================
inline UnicodeString& 
UnicodeString::operator= (UChar ch) 
{ return doReplace(0, fLength, &ch, 0, 1); }

inline UnicodeString& 
UnicodeString::setTo(const UnicodeString& srcText, 
             UTextOffset srcStart, 
             int32_t srcLength)
{ return doReplace(0, fLength, srcText, srcStart, srcLength); }

inline UnicodeString& 
UnicodeString::setTo(const UnicodeString& srcText)
{ return doReplace(0, fLength, srcText, 0, srcText.fLength); }

inline UnicodeString& 
UnicodeString::setTo(const UChar *srcChars,
             int32_t srcLength)
{ return doReplace(0, fLength, srcChars, 0, srcLength); }

inline UnicodeString& 
UnicodeString::setTo(UChar srcChar)
{ return doReplace(0, fLength, &srcChar, 0, 1); }

inline UnicodeString& 
UnicodeString::operator+= (UChar ch)
{ return doReplace(fLength, 0, &ch, 0, 1); }

inline UnicodeString& 
UnicodeString::operator+= (UChar32 ch) {
  UChar buffer[UTF_MAX_CHAR_LENGTH];
  int32_t length = 0;
  UTF_APPEND_CHAR_UNSAFE(buffer, length, ch);
  return doReplace(fLength, 0, buffer, 0, length);
}

inline UnicodeString& 
UnicodeString::operator+= (const UnicodeString& srcText)
{ return doReplace(fLength, 0, srcText, 0, srcText.fLength); }

inline UnicodeString& 
UnicodeString::append(const UnicodeString& srcText, 
              UTextOffset srcStart, 
              int32_t srcLength)
{ return doReplace(fLength, 0, srcText, srcStart, srcLength); }

inline UnicodeString& 
UnicodeString::append(const UnicodeString& srcText)
{ return doReplace(fLength, 0, srcText, 0, srcText.fLength); }

inline UnicodeString& 
UnicodeString::append(const UChar *srcChars, 
              UTextOffset srcStart, 
              int32_t srcLength)
{ return doReplace(fLength, 0, srcChars, srcStart, srcLength); }

inline UnicodeString& 
UnicodeString::append(const UChar *srcChars,
              int32_t srcLength)
{ return doReplace(fLength, 0, srcChars, 0, srcLength); }

inline UnicodeString& 
UnicodeString::append(UChar srcChar)
{ return doReplace(fLength, 0, &srcChar, 0, 1); }

inline UnicodeString& 
UnicodeString::append(UChar32 srcChar) {
  UChar buffer[UTF_MAX_CHAR_LENGTH];
  int32_t length = 0;
  UTF_APPEND_CHAR_UNSAFE(buffer, length, srcChar);
  return doReplace(fLength, 0, buffer, 0, length);
}

inline UnicodeString& 
UnicodeString::insert(UTextOffset start, 
              const UnicodeString& srcText, 
              UTextOffset srcStart, 
              int32_t srcLength)
{ return doReplace(start, 0, srcText, srcStart, srcLength); }

inline UnicodeString& 
UnicodeString::insert(UTextOffset start, 
              const UnicodeString& srcText)
{ return doReplace(start, 0, srcText, 0, srcText.fLength); }

inline UnicodeString& 
UnicodeString::insert(UTextOffset start, 
              const UChar *srcChars, 
              UTextOffset srcStart, 
              int32_t srcLength)
{ return doReplace(start, 0, srcChars, srcStart, srcLength); }

inline UnicodeString& 
UnicodeString::insert(UTextOffset start, 
              const UChar *srcChars,
              int32_t srcLength)
{ return doReplace(start, 0, srcChars, 0, srcLength); }

inline UnicodeString& 
UnicodeString::insert(UTextOffset start, 
              UChar srcChar)
{ return doReplace(start, 0, &srcChar, 0, 1); }


inline UnicodeString& 
UnicodeString::remove(UTextOffset start, 
             int32_t length)
{ return doReplace(start, length, 0, 0, 0); }

inline UnicodeString& 
UnicodeString::remove()
{ return doReplace(0, fLength, 0, 0, 0); }

inline UnicodeString& 
UnicodeString::removeBetween(UTextOffset start,
                UTextOffset limit)
{ return doReplace(start, limit - start, 0, 0, 0); }

inline bool_t 
UnicodeString::truncate(int32_t targetLength)
{
  if((uint32_t)targetLength < (uint32_t)fLength) {
    fLength = targetLength;
    return TRUE;
  } else {
    return FALSE;
  }
}

inline UnicodeString& 
UnicodeString::reverse()
{ return doReverse(0, fLength); }

inline UnicodeString& 
UnicodeString::reverse(UTextOffset start,
               int32_t length)
{ return doReverse(start, length); }


//========================================
// Write implementation methods
//========================================
inline bool_t 
UnicodeString::isBogus() const
{ return fFlags & kIsBogus; }


//========================================
// Privates
//========================================

inline void
UnicodeString::pinIndices(UTextOffset& start,
                          int32_t& length) const
{
  // pin indices
  if(start < 0) {
    start = 0;
  } else if(start > fLength) {
    start = fLength;
  }
  if(length < 0) {
    length = 0;
  } else if(length > (fLength - start)) {
    length = (fLength - start);
  }
}

inline UChar* 
UnicodeString::getArrayStart()
{ return fArray; }

inline const UChar* 
UnicodeString::getArrayStart() const
{ return fArray; }

inline int32_t 
UnicodeString::getCapacity() const
{ return fCapacity; }

inline void
UnicodeString::releaseArray() {
  if((fFlags & kRefCounted) && removeRef() == 0) {
    delete [] ((int32_t *)fArray - 1);
  }
}

inline int32_t
UnicodeString::addRef()
{ return ++*((int32_t *)fArray - 1); }

inline int32_t
UnicodeString::removeRef()
{ return --*((int32_t *)fArray - 1); }

inline int32_t
UnicodeString::refCount() const
{ return *((int32_t *)fArray - 1); }

inline int32_t
UnicodeString::setRefCount(int32_t count)
{ return (*((int32_t *)fArray - 1) = count); }


// deprecated API - remove later
inline int32_t
UnicodeString::size() const
{ return fLength; }

inline UnicodeString& 
UnicodeString::findAndReplace(const UnicodeString& oldText,
                  const UnicodeString& newText,
                  UTextOffset start,
                  int32_t length)
{ return findAndReplace(start, length, oldText, newText); }

inline void*
UnicodeString::operator new(size_t size)
{ return ::operator new(size); }

inline void* 
UnicodeString::operator new(size_t size, 
              void *location)
{ return location; }

inline void
UnicodeString::operator delete(void *location)
{ ::operator delete(location); }


//========================================
// Static members
//========================================

//========================================
// class UCharReference
//========================================
class UCharReference
{
public:
  UCharReference();
  inline UCharReference(UnicodeString *string,
         UTextOffset pos);
  inline UCharReference(const UCharReference& that);
  ~UCharReference();

  inline UCharReference& operator= (const UCharReference& that);
  inline UCharReference& operator= (UChar c);

  inline operator UChar();

private:
  UnicodeString *fString;
  UTextOffset fPos;
};


//========================================
// Inline members
//========================================
inline
UCharReference::UCharReference(UnicodeString *string, 
                   UTextOffset pos)
  : fString(string), fPos(pos)
{}

inline
UCharReference::UCharReference(const UCharReference& that)
{ this->operator=(that); }

inline
UCharReference::~UCharReference()
{}

inline UCharReference&
UCharReference::operator= (const UCharReference& that)
{ fString->setCharAt(fPos, that.fString->charAt(that.fPos)); return *this; }

inline UCharReference& 
UCharReference::operator= (UChar c)
{ fString->setCharAt(fPos, c); return *this; }

inline
UCharReference::operator UChar()
{ return fString->charAt(fPos); }

#endif
