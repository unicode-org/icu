/*
*******************************************************************************
*
*   Copyright (C) 2004-2005, International Business Machines
*   Corporation and others.  All Rights Reserved.
*
*******************************************************************************
*   file name:  utext.h
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2004oct06
*   created by: Markus W. Scherer
*/

#ifndef __UTEXT_H__
#define __UTEXT_H__

/**
 * \file
 * \brief C API: Abstract Unicode Text API
 *
 * The Text Access API provides a means to allow text that is stored in alternative
 * formats to work with ICU services.  ICU normally operates on text that is
 * stored UTF-16 format, in (UChar *) arrays for the C APIs or as type
 * UnicodeString for C++ APIs.
 *
 * ICU Text Access allows other formats, such as UTF-8 or non-contiguous
 * UTF-16 strings, to be placed in a UText wrapper and then passed to ICU services.
 *
 * There are three general classes of usage for UText:
 *
 *     Application Level Use.  This is the simplest usage - applications would
 *     use one of the utext_open() functions on their input text, and pass
 *     the resulting UText to the desired ICU service.
 *
 *     Second is usage in ICU Services, such as break iteration, that will need to
 *     operate on input presented to them as a UText.  These implementations
 *     will need to use the iteration and related UText functions to gain
 *     access to the actual text.
 *
 *     The third class of UText users are "text providers."  These are the
 *     UText implementations for the various text storage formats.  An application
 *     or system with a unique text storage format can implement a set of
 *     UText provider functions for that format, which will then allow
 *     ICU services to operate on that format.
 *
 *
 * <em>Iterating over text</em>
 *
 * Here is sample code for a forward iteration over the contents of a UText
 *
 * \code
 *    UChar32  c;
 *    UText    *ut = whatever();
 *
 *    for (c=utext_next32From(ut, 0); c>=0; c=utext_next32(ut)) {
 *       // do whatever with the codepoint c here.
 *    }
 * \endcode
 *
 * And here is similar code to iterate in the reverse direction, from the end
 * of the text towards the beginning.
 *
 * \code
 *    UChar32  c;
 *    UText    *ut = whatever();
 *    int      textLength = utext_nativeLength(ut);
 *    for (c=utext_previous32From(ut, textLength); c>=0; c=utext_previous32(ut)) {
 *       // do whatever with the codepoint c here.
 *    }
 * \endcode
 *
 * <em>Characters and Indexing</em>
 *
 * Indexing into text by UText functions is nearly always in terms of the native
 * indexing of the underlying text storage.  The storage format could be UTF-8
 * or UTF-32, for example.  When coding to the UText access API, no assumptions
 * can be made regarding the size of characters, or how far an index
 * may move when iterating between characters.
 *
 * All indices supplied to UText functions are pinned to the length of the
 * text.  An out-of-bounds index is not considered to be an error, but is
 * adjusted to be in the range  0 <= index <= length of input text.
 *
 *
 * When an index position is returned from a UText function, it will be
 * a native index to the underlying text.  In the case of multi-unit characters,
 * it will  always refer to the first position of the character,
 * never to the interior.  This is essentially the same thing as saying that
 * a returned index will always point to a boundary between characters.
 *
 * When a native index is supplied to a UText function, all indices that
 * refer to any part of a multi-unit character representation are considered
 * to be equivalent.  In the case of multi-unit characters, an incoming index
 * will be logically normalized to refer to the start of the character.
 * 
 * It is possible to test whether a native index is on a code point boundary
 * by doing a utext_setNativeIndex() followed by a utext_getNativeIndex().
 * If the index is returned unchanged, it was on a code point boundary.  If
 * an adjusted index is returned, the original index referred to the
 * interior of a character.
 *
 */



#include "unicode/utypes.h"
#ifdef XP_CPLUSPLUS
#include "unicode/rep.h"
#include "unicode/unistr.h"
#endif

#ifndef U_HIDE_DRAFT_API

U_CDECL_BEGIN

struct UText;
typedef struct UText UText; /**< C typedef for struct UText. @draft ICU 3.4 */

struct UTextChunk;
typedef struct UTextChunk UTextChunk; /**< C typedef for struct UTextChunk. @draft ICU 3.4 */



/***************************************************************************************
 *
 *   C Functions for creating UText wrappers around various kinds of text strings.
 *
 ****************************************************************************************/


/**
  * utext_close    Close function for UText instances.
  *                Cleans up, releases any resources being held by an
  *                open UText.
  * <p/>
  *   If the UText was originally allocated by one of the utext_open functions,
  *   the storage associated with the utext will also be freed.
  *   If the UText storage originated with the application, as it would with
  *   a local or static instance, the storage will not be deleted.
  *
  *   An open UText can be reset to refer to new string by using one of the utext_open()
  *   functions without first closing the UText.  
  *
  * @param ut  The UText to be closed.
  * @return    NULL if the UText struct was deleted by the close.  If the UText struct
  *            was originally provided by the caller to the open function, it is
  *            returned by this function, and may be safely used again in
  *            a subsequent utext_open.
  *
  * @draft ICU 3.4
  */
U_DRAFT UText * U_EXPORT2
utext_close(UText *ut);


/**
 * Open a read-only UText implementation for UTF-8 strings.
 * 
 * \htmlonly
 * Any invalid UTF-8 in the input will be handled in this way:
 * a sequence of bytes that has the form of a truncated, but otherwise valid,
 * UTF-8 sequence will be replaced by a single unicode replacement character, \uFFFD. 
 * Any other illegal bytes will each be replaced by a \uFFFD.
 * \endhtmlonly
 * 
 * @param ut     Pointer to a UText struct.  If NULL, a new UText will be created.
 *               If non-NULL, must refer to an initialized UText struct, which will then
 *               be reset to reference the specified UTF-8 string.
 * @param s      A UTF-8 string
 * @param length The length of the UTF-8 string in bytes, or -1 if the string is
 *               zero terminated.
 * @param status Errors are returned here.
 * @return       A pointer to the UText.  If a pre-allocated UText was provided, it
 *               will always be used and returned.
 * @draft ICU 3.4
 */
U_DRAFT UText * U_EXPORT2
utext_openUTF8(UText *ut, const char *s, int32_t length, UErrorCode *status);


/**
 * Open a read-only UText for UChar * string.
 * 
 * @param ut     Pointer to a UText struct.  If NULL, a new UText will be created.
 *               If non-NULL, must refer to an initialized UText struct, which will then
 *               be reset to reference the specified UChar string.
 * @param s      A UChar (UTF-16) string
 * @param length The number of UChars in the input string, or -1 if the string is
 *               zero terminated.
 * @param status Errors are returned here.
 * @return       A pointer to the UText.  If a pre-allocated UText was provided, it
 *               will always be used and returned.
 * @draft ICU 3.4
 */
U_DRAFT UText * U_EXPORT2
utext_openUChars(UText *ut, const UChar *s, int32_t length, UErrorCode *status);


#ifdef XP_CPLUSPLUS
/**
 * Open a writable UText for a non-const UnicodeString. 
 * 
 * @param ut      Pointer to a UText struct.  If NULL, a new UText will be created.
 *                 If non-NULL, must refer to an initialized UText struct, which will then
 *                 be reset to reference the specified input string.
 * @param s       A UnicodeString.
 * @param status Errors are returned here.
 * @return        Pointer to the UText.  If a UText was supplied as input, this
 *                 will always be used and returned.
 * @draft ICU 3.4
 */
U_DRAFT UText * U_EXPORT2
utext_openUnicodeString(UText *ut, UnicodeString *s, UErrorCode *status);


/**
 * Open a UText for a const UnicodeString.   The resulting UText will not be writable.
 * 
 * @param ut    Pointer to a UText struct.  If NULL, a new UText will be created.
 *               If non-NULL, must refer to an initialized UText struct, which will then
 *               be reset to reference the specified input string.
 * @param s      A const UnicodeString to be wrapped.
 * @param status Errors are returned here.
 * @return       Pointer to the UText.  If a UText was supplied as input, this
 *               will always be used and returned.
 * @draft ICU 3.4
 */
U_DRAFT UText * U_EXPORT2
utext_openConstUnicodeString(UText *ut, const UnicodeString *s, UErrorCode *status);


/**
 * Open a writable UText implementation for an ICU Replaceable object.
 * @param ut    Pointer to a UText struct.  If NULL, a new UText will be created.
 *               If non-NULL, must refer to an already existing UText, which will then
 *               be reset to reference the specified replaceable text.
 * @param rep    A Replaceable text object.
 * @param status Errors are returned here.
 * @return       Pointer to the UText.  If a UText was supplied as input, this
 *               will always be used and returned.
 * @see Replaceable
 * @draft ICU 3.4
 */
U_DRAFT UText * U_EXPORT2
utext_openReplaceable(UText *ut, Replaceable *rep, UErrorCode *status);

#endif


/**
  *  clone a UText.  Much like opening a UText where the source text is itself
  *  another UText.
  *
  *  A deep clone will copy both the UText data structures and the underlying text.
  *  The original and cloned UText will operate completely independently; modifications
  *  made to the text in one will not effect the other.  Text providers are not
  *  required to support deep clones.  The user of clone() must check the status return
  *  and be prepared to handle failures.
  *
  *  A shallow clone replicates only the UText data structures; it does not make
  *  a copy of the underlying text.  Shallow clones can be used as an efficient way to 
  *  have multiple iterators active in a single text string that is not being
  *  modified.
  *
  *  A shallow clone operation will not fail, barring truly exceptional conditions such
  *  as memory allocation failures.
  *
  *  A UText and its clone may be safely concurrently accessed by separate threads.
  *  This is true for both shallow and deep clones.
  *  It is the responsibility of the Text Provider to ensure that this thread safety
  *  constraint is met.
  *
  *  @param dest   A UText struct to be filled in with the result of the clone operation,
  *                or NULL if the clone function should heap-allocate a new UText struct.
  *  @param src    The UText to be cloned.
  *  @param deep   TRUE to request a deep clone, FALSE for a shallow clone.
  *  @param status Errors are returned here.  For deep clones, U_UNSUPPORTED_ERROR
  *                will be returned if the text provider is unable to clone the
  *                original text.
  *  @return       The newly created clone, or NULL if the clone operation failed.
  *  @draft ICU 3.4
  */
U_DRAFT UText * U_EXPORT2
utext_clone(UText *dest, const UText *src, UBool deep, UErrorCode *status);


/*****************************************************************************
 *
 *   C Functions to work with the text represeted by a UText wrapper
 *
 *****************************************************************************/

/**
  * Get the length of the text.  Depending on the characteristics
  * of the underlying text representation, this may be expensive.  
  * @see  utext_isLengthExpensive()
  *
  *
  * @param ut  the text to be accessed.
  * @return the length of the text, expressed in native units.
  *
  * @draft ICU 3.4
  */
U_DRAFT int32_t U_EXPORT2
utext_nativeLength(UText *ut);

/**
 *  Return TRUE if calculating the length of the text could be expensive.
 *  Finding the length of NUL terminated strings is considered to be expensive.
 *
 *  Note that the value of this function may change
 *  as the result of other operations on a UText.
 *  Once the length of a string has been discovered, it will no longer
 *  be expensive to report it.
 *
 * @param ut the text to be accessed.
 * @return TRUE if determining the length of the text could be time consuming.
 * @draft ICU 3.4
 */
U_DRAFT UBool U_EXPORT2
utext_isLengthExpensive(const UText *ut);

/**
 * Returns the code point at the requested index,
 * or U_SENTINEL (-1) if it is out of bounds.
 *
 * If the specified index points to the interior of a multi-unit
 * character - one of the trail bytes of a UTF-8 sequence, for example -
 * the complete code point will be returned.
 *
 * The iteration position will be set to the start of the returned code point.
 *
 * This function is roughly equivalent to the the sequence
 *    utext_setNativeIndex(index);
 *    utext_current32();
 * (There is a difference if the index is out of bounds by being less than zero)
 * 
 * @param ut the text to be accessed
 * @param nativeIndex the native index of the character to be accessed.  If the index points
 *        to other than the first unit of a multi-unit character, it will be adjusted
 *        to the start of the character.
 * @return the code point at the specified index.
 * @draft ICU 3.4
 */
U_DRAFT UChar32 U_EXPORT2
utext_char32At(UText *ut, int32_t nativeIndex);


/**
 *
 * Get the code point at the current iteration position,
 * or U_SENTINEL (-1) if the iteration has reached the end of
 * the input text.
 *
 * @param ut the text to be accessed.
 * @return the Unicode code point at the current iterator position.
 * @draft ICU 3.4
 */
U_DRAFT UChar32 U_EXPORT2
utext_current32(UText *ut);


/**
 * Get the code point at the current iteration position of the UText, and
 * advance the position to the first index following the character.
 * Returns U_SENTINEL (-1) if the position is at the end of the
 * text.
 * This is a post-increment operation
 *
 * An inline macro version of this function, UTEXT_NEXT32(), 
 * is available for performance critical use.
 *
 * @param ut the text to be accessed.
 * @return the Unicode code point at the iteration position.
 * @see UTEXT_NEXT32
 * @draft ICU 3.4
 */
U_DRAFT UChar32 U_EXPORT2
utext_next32(UText *ut);


/**
 *  Move the iterator position to the character (code point) whose
 *  index precedes the current position, and return that character.
 *  This is a pre-decrement operation.
 *  Returns U_SENTINEL (-1) if the position is at the start of the  text.
 *  This is a pre-decrement operation.
 *
 * An inline macro version of this function, UTEXT_PREVIOUS32(), 
 * is available for performance critical use.
 *
 *  @param ut the text to be accessed.
 *  @return the previous UChar32 code point, or U_SENTINEL (-1) 
 *          if the iteration has reached the start of the text.
 *  @see UTEXT_PREVIOUS32
 *  @draft ICU 3.4
 */
U_DRAFT UChar32 U_EXPORT2
utext_previous32(UText *ut);


/**
  * Set the iteration index, access the text for forward iteration,
  * and return the code point starting at or before that index.
  * Leave the iteration index at the start of the following code point.
  *
  * This function is the most efficient and convenient way to
  * begin a forward iteration.
  *
  *  @param ut the text to be accessed.
  *  @param nativeIndex Iteration index, in the native units of the text provider.
  *  @return Code point which starts at or before index,
  *         or U_SENTINEL (-1) if it is out of bounds.
  * @draft ICU 3.4
  */
U_DRAFT UChar32 U_EXPORT2
utext_next32From(UText *ut, int32_t nativeIndex);



/**
  * Set the iteration index, and return the code point preceding the
  * one specified by the initial index.  Leave the iteration position
  * at the start of the returned code point.
  *
  * This function is the most efficient and convenient way to
  * begin a backwards iteration.
  *
  * @param ut the text to be accessed.
  * @param nativeIndex Iteration index in the native units of the text provider.
  * @return Code point preceding the one at the initial index,
  *         or U_SENTINEL (-1) if it is out of bounds.
  *
  * @draft ICU 3.4
  */
U_DRAFT UChar32 U_EXPORT2
utext_previous32From(UText *ut, int32_t nativeIndex);

/**
  * Get the current iterator position, which can range from 0 to 
  * the length of the text.
  * The position is a native index into the input text, in whatever format it
  * may have, and may not always correspond to a UChar (UTF-16) index
  * into the text.  The returned position will always be aligned to a
  * code point boundary 
  *
  * @param ut the text to be accessed.
  * @return the current index position, in the native units of the text provider.
  * @draft ICU 3.4
  */
U_DRAFT int32_t U_EXPORT2
utext_getNativeIndex(UText *ut);

/**
  * Set the current iteration position to the nearest code point
  * boundary at or preceding the specified index.
  * The index is in the native units of the original input text.
  * If the index is out of range, it will be trimmed to be within
  * the range of the input text.
  * <p/>
  * It will usually be more efficient to begin an iteration
  * using the functions utext_next32From() or utext_previous32From()
  * rather than setIndex().
  * <p/>
  * Moving the index position to an adjacent character is best done
  * with utext_next32(), utext_previous32() or utext_moveIndex32().
  * Attempting to do direct arithmetic on the index position is
  * complicated by the fact that the size (in native units) of a
  * character depends on the underlying representation of the character
  * (UTF-8, UTF-16, UTF-32, arbitrary codepage), and is not
  * easily knowable.
  *
  * @param ut the text to be accessed.
  * @param nativeIndex the native unit index of the new iteration position.
  * @draft ICU 3.4
  */
U_DRAFT void U_EXPORT2
utext_setNativeIndex(UText *ut, int32_t nativeIndex);

/**
  * Move the iterator postion by delta code points.  The number of code points
  * is a signed number; a negative delta will move the iterator backwards,
  * towards the start of the text.
  * <p/>
  * The index is moved by <code>delta</code> code points
  * forward or backward, but no further backward than to 0 and
  * no further forward than to utext_nativeLength().
  * The resulting index value will be in between 0 and length, inclusive.
  * <p/>
  * Because the index is kept in the native units of the text provider, the
  * actual numeric amount by which the index moves depends on the
  * underlying text storage representation of the text provider.
  *
  * @param ut the text to be accessed.
  * @param delta the signed number of code points to move the iteration position.
  * @return TRUE if the position could be moved the requested number of positions while
  *              staying within the range [0 - text length].
  * @draft ICU 3.4
  */
U_DRAFT UBool U_EXPORT2
utext_moveIndex32(UText *ut, int32_t delta);


/**
 *
 * Extract text from a UText into a UChar buffer.  The range of text to be extracted
 * is specified in the native indices of the UText provider.  These may not necessarily
 * be UTF-16 indices.
 * <p/>
 * The size (number of 16 bit UChars) in the data to be extracted is returned.  The
 * full number of UChars is returned, even when the extracted text is truncated
 * because the specified buffer size is too small.
 *
 * The extracted string will (if you are a user) / must (if you are a text provider)
 * be NUL-terminated if there is sufficient space in the destination buffer.  This
 * terminating NUL is not included in the returned length.
 *
 * @param  ut    the UText from which to extract data.
 * @param  nativeStart the native index of the first character to extract.
 * @param  nativeLimit the native string index of the position following the last
 *               character to extract.  If the specified limit is greater than the length
 *               of the text, the limit will be trimmed back to the text length.
 * @param  dest  the UChar (UTF-16) buffer into which the extracted text is placed
 * @param  destCapacity  The size, in UChars, of the destination buffer.  May be zero
 *               for precomputing the required size.
 * @param  status receives any error status.
 *         U_BUFFER_OVERFLOW_ERROR: the extracted text was truncated because the 
 *         buffer was too small.  Returns number of UChars for preflighting.
 * @return Number of UChars in the data to be extracted.  Does not include a trailing NUL.
 *
 * @draft ICU 3.4
 */
U_DRAFT int32_t U_EXPORT2
utext_extract(UText *ut,
             int32_t nativeStart, int32_t nativeLimit,
             UChar *dest, int32_t destCapacity,
             UErrorCode *status);



/************************************************************************************
 *
 *  #define inline versions of selected performance-critical text access functions
 *          Caution:  do not use auto increment++ or decrement-- expressions
 *                    as parameters to these macros.
 *
 *          For most use, where there is no extreme performance constraint, the
 *          normal, non-inline functions are a better choice.  The resulting code
 *          will be smaller, and, if the need ever arises, easier to debug.
 *
 *          These are implemented as #defines rather than real functions
 *          because there is no fully portable way to do inline functions in plain C.
 *
 ************************************************************************************/

/**
 * inline version of utext_next32(), for performance-critical situations.
 *
 * Get the code point at the current iteration position of the UText, and
 * advance the position to the first index following the character.
 * This is a post-increment operation.
 * Returns U_SENTINEL (-1) if the position is at the end of the
 * text.
 *
 * @draft ICU 3.4
 */
#define UTEXT_NEXT32(ut)  \
    ((ut)->chunk.offset < (ut)->chunk.length && ((ut)->chunk.contents)[(ut)->chunk.offset]<0xd800 ? \
    ((ut)->chunk.contents)[((ut)->chunk.offset)++] : utext_next32(ut))

/**
 * inline version of utext_previous32(), for performance-critical situations.
 *
 *  Move the iterator position to the character (code point) whose
 *  index precedes the current position, and return that character.
 *  This is a pre-decrement operation.
 *  Returns U_SENTINEL (-1) if the position is at the start of the  text.
 *
 * @draft ICU 3.4
 */
#define UTEXT_PREVIOUS32(ut)  \
    ((ut)->chunk.offset > 0 && \
     (ut)->chunk.contents[(ut)->chunk.offset-1] < 0xd800 ? \
          (ut)->chunk.contents[--((ut)->chunk.offset)]  :  utext_previous32(ut))




/************************************************************************************
 *
 *   Functions related to writing or modifying the text.
 *   These will work only with modifiable UTexts.  Attempting to
 *   modify a read-only UText will return an error status.
 *
 ************************************************************************************/


/**
 *  Return TRUE if the text can be written with utext_replace() or
 *  utext_copy().  For the text to be writable, the text provider must
 *  be of a type that supports writing.
 *
 * @param  ut   the UText to be tested.
 * @return TRUE if the text is modifiable.
 * @draft ICU 3.4
 *
 */
U_DRAFT UBool U_EXPORT2
utext_isWritable(const UText *ut);


/**
  * Test whether there is meta data associated with the text.
  * @see Replaceable::hasMetaData()
  *
  * @param ut The UText to be tested
  * @return TRUE if the underlying text includes meta data.
  * @draft ICU 3.4
  */
U_DRAFT UBool U_EXPORT2
utext_hasMetaData(const UText *ut);


/**
 * Replace a range of the original text with a replacement text.
 *
 * Leaves the current iteration position at the position following the
 *  newly inserted replacement text.
 *
 * This function is only available on UText types that support writing,
 * that is, ones where utext_isWritable() returns TRUE.
 *
 * When using this function, there should be only a single UText opened onto the
 * underlying native text string.  Behavior after a replace operation
 * on a UText is undefined for any other additional UTexts that refer to the
 * modified string.
 *
 * @param ut               the UText representing the text to be operated on.
 * @param nativeStart      the native index of the start of the region to be replaced
 * @param nativeLimit      the native index of the character following the region to be replaced.
 * @param replacementText  pointer to the replacement text
 * @param replacementLength length of the replacement text, or -1 if the text is NUL terminated.
 * @param status           receives any error status.  Possible errors include
 *                         U_NO_WRITE_PERMISSION
 *
 * @return The signed number of (native) storage units by which
 *         the length of the text expanded or contracted.
 *
 * @draft ICU 3.4
 */
U_DRAFT int32_t U_EXPORT2
utext_replace(UText *ut,
             int32_t nativeStart, int32_t nativeLimit,
             const UChar *replacementText, int32_t replacementLength,
             UErrorCode *status);



/**
 *
 * Copy or move a substring from one position to another within the text,
 * while retaining any metadata associated with the text.
 * This function is used to duplicate or reorder substrings.
 * The destination index must not overlap the source range.
 *
 * The text to be copied or moved is inserted at destIndex;
 * it does not replace or overwrite any existing text.
 *
 * This function is only available on UText types that support writing,
 * that is, ones where utext_isWritable() returns TRUE.
 *
 * When using this function, there should be only a single UText opened onto the
 * underlying native text string.  Behavior after a copy operation
 * on a UText is undefined in any other additional UTexts that refer to the
 * modified string.
 *
 * @param ut           The UText representing the text to be operated on.
 * @param nativeStart  The native index of the start of the region to be copied or moved
 * @param nativeLimit  The native index of the character position following the region to be copied.
 * @param destIndex    The native destination index to which the source substring is copied or moved.
 * @param move         If TRUE, then the substring is moved, not copied/duplicated.
 * @param status       receives any error status.  Possible errors include U_NO_WRITE_PERMISSION
 *                       
 * @draft ICU 3.4
 */
U_DRAFT void U_EXPORT2
utext_copy(UText *ut,
          int32_t nativeStart, int32_t nativeLimit,
          int32_t destIndex,
          UBool move,
          UErrorCode *status);





/****************************************************************************************
 *
 *   The following items are required by text providers implementations -
 *    by packages that are writing UText wrappers for additional types of text strings.
 *    These declarations are not needed by applications that use already existing
 *    UText functions for wrapping strings or accessing text data that has been
 *    wrapped in a UText.
 *
 *****************************************************************************************/


/**
  *  Descriptor of a chunk, or segment of text in UChar format.
  *
  *  UText provider implementations surface their text in the form of UTextChunks.
  *
  *  If the native form of the text if UTF-16, a chunk will typically refer back to the
  *   original native text storage.  If the native format is something else, chunks
  *   will typically refer to a buffer maintained by the provider that contains
  *   some amount input that has been converted to UTF-16 (UChar) form.
  *
  * @draft ICU 3.4
  */  
struct UTextChunk {
    /** Pointer to contents of text chunk.  UChar format.   */
    const UChar *contents;

    /**  Index within the contents of the current iteration position. */
    int32_t     offset;  

    /** Number of UChars in the chunk. */
    int32_t     length;

    /** (Native) text index corresponding to the start of the chunk. */
    int32_t     nativeStart;

    /** (Native) text index corresponding to the end of the chunk (contents+length). */
    int32_t     nativeLimit;

    /** If TRUE, then non-UTF-16 indexes are used in this chunk. */
    UBool       nonUTF16Indexes;

    /** Unused. */
    UBool       padding1, padding2, padding3;

    /** Unused. */
    int32_t     padInt1, padInt2;

    /** Contains sizeof(UTextChunk) and allows the future addition of fields. */
    int32_t     sizeOfStruct;
};


/**
 * UText provider properties (bit field indexes).
 *
 * @see UText
 * @draft ICU 3.4
 */
enum {
    /**
     * The provider works with non-UTF-16 ("native") text indexes.
     * For example, byte indexes into UTF-8 text or UTF-32 indexes into UTF-32 text.
     * @draft ICU 3.4
     */
    UTEXT_PROVIDER_NON_UTF16_INDEXES = 0,
    /**
     * It is potentially time consuming for the provider to determine the length of the text.
     * @draft ICU 3.4
     */
    UTEXT_PROVIDER_LENGTH_IS_EXPENSIVE = 1,
    /**
     * Text chunks remain valid and usable until the text object is modified or
     * deleted, not just until the next time the access() function is called
     * (which is the default).
     * @draft ICU 3.4
     */
    UTEXT_PROVIDER_STABLE_CHUNKS = 2,
    /**
     * The provider supports modifying the text via the replace() and copy()
     * functions.
     * @see Replaceable
     * @draft ICU 3.4
     */
    UTEXT_PROVIDER_WRITABLE = 3,
    /**
     * There is meta data associated with the text.
     * @see Replaceable::hasMetaData()
     * @draft ICU 3.4
     */
    UTEXT_PROVIDER_HAS_META_DATA = 4
};

/**
  * Function type declaration for UText.clone().
  *
  *  clone a UText.  Much like opening a UText where the source text is itself
  *  another UText.
  *
  *  A deep clone will copy both the UText data structures and the underlying text.
  *  The original and cloned UText will operate completely independently; modifications
  *  made to the text in one will not effect the other.  Text providers are not
  *  required to support deep clones.  The user of clone() must check the status return
  *  and be prepared to handle failures.
  *
  *  A shallow clone replicates only the UText data structures; it does not make
  *  a copy of the underlying text.  Shallow clones can be used as an efficient way to 
  *  have multiple iterators active in a single text string that is not being
  *  modified.
  *
  *  A shallow clone operation must not fail except for truly exceptional conditions such
  *  as memory allocation failures.
  *
  *  A UText and its clone may be safely concurrently accessed by separate threads.
  *  This is true for both shallow and deep clones.
  *  It is the responsibility of the Text Provider to ensure that this thread safety
  *  constraint is met.

  *
  *  @param dest   A UText struct to be filled in with the result of the clone operation,
  *                or NULL if the clone function should heap-allocate a new UText struct.
  *  @param src    The UText to be cloned.
  *  @param deep   TRUE to request a deep clone, FALSE for a shallow clone.
  *  @param status Errors are returned here.  For deep clones, U_UNSUPPORTED_ERROR
  *                should be returned if the text provider is unable to clone the
  *                original text.
  *  @return       The newly created clone, or NULL if the clone operation failed.
  *
  * @draft ICU 3.4
  */
typedef UText * U_CALLCONV
UTextClone(UText *dest, const UText *src, UBool deep, UErrorCode *status);


/**
 * Function type declaration for UText.nativeLength().
 *
 * @param ut the UText to get the length of.
 * @return the length, in the native units of the original text string.
 * @see UText
 * @draft ICU 3.4
 */
typedef int32_t U_CALLCONV
UTextNativeLength(UText *ut);

/**
 * Function type declaration for UText.access().  Get the description of the text chunk
 *  containing the text at a requested native index.  The UText's iteration
 *  position will be left at the requested index.  If the index is out
 *  of bounds, the iteration position will be left at the start or end
 *  of the string, as appropriate.
 *
 *  Chunks must begin and end on code point boundaries.  A single code point
 *  comprised of multiple storage units must never span a chunk boundary.
 *
 *
 * @param ut          the UText being accessed.
 * @param nativeIndex Requested index of the text to be accessed.
 * @param forward     If TRUE, then the returned chunk must contain text
 *                    starting from the index, so that start<=index<limit.
 *                    If FALSE, then the returned chunk must contain text
 *                    before the index, so that start<index<=limit.
 * @return            True if the requested index could be accessed.  The chunk
 *                    will contain the requested text.
 *                    False value if a chunk cannot be accessed
 *                    (the requested index is out of bounds).
 *
 * @see UText
 * @draft ICU 3.4
 */
typedef UBool U_CALLCONV
UTextAccess(UText *ut, int32_t nativeIndex, UBool forward, UTextChunk *chunk);

/**
 * Function type declaration for UText.extract().
 *
 * Extract text from a UText into a UChar buffer.  The range of text to be extracted
 * is specified in the native indices of the UText provider.  These may not necessarily
 * be UTF-16 indices.
 * <p/>
 * The size (number of 16 bit UChars) in the data to be extracted is returned.  The
 * full amount is returned, even when the specified buffer size is smaller.
 *
 * The extracted string will (if you are a user) / must (if you are a text provider)
 * be NUL-terminated if there is sufficient space in the destination buffer.
 *
 * @param  ut            the UText from which to extract data.
 * @param  nativeStart   the native index of the first characer to extract.
 * @param  nativeLimit   the native string index of the position following the last
 *                       character to extract.
 * @param  dest          the UChar (UTF-16) buffer into which the extracted text is placed
 * @param  destCapacity  The size, in UChars, of the destination buffer.  May be zero
 *                       for precomputing the required size.
 * @param  status        receives any error status.
 *                       If U_BUFFER_OVERFLOW_ERROR: Returns number of UChars for
 *                       preflighting.
 * @return Number of UChars in the data.  Does not include a trailing NUL.
 *
 * @draft ICU 3.4
 */
typedef int32_t U_CALLCONV
UTextExtract(UText *ut,
             int32_t nativeStart, int32_t nativeLimit,
             UChar *dest, int32_t destCapacity,
             UErrorCode *status);

/**
 * Function type declaration for UText.replace().
 *
 * Replace a range of the original text with a replacement text.
 *
 * Leaves the current iteration position at the position following the
 *  newly inserted replacement text.
 *
 * This function need only be implemented on UText types that support writing.
 *
 * When using this function, there should be only a single UText opened onto the
 * underlying native text string.  The function is responsible for updating the
 * text chunk within the UText to reflect the updated iteration position,
 * taking into account any changes to the underlying string's structure caused
 * by the replace operation.
 *
 * @param ut               the UText representing the text to be operated on.
 * @param nativeStart      the index of the start of the region to be replaced
 * @param nativeLimit      the index of the character following the region to be replaced.
 * @param replacementText  pointer to the replacement text
 * @param replacmentLength length of the replacement text in UChars, or -1 if the text is NUL terminated.
 * @param status           receives any error status.  Possible errors include
 *                         U_NO_WRITE_PERMISSION
 *
 * @return The signed number of (native) storage units by which
 *         the length of the text expanded or contracted.
 *
 * @draft ICU 3.4
 */
typedef int32_t U_CALLCONV
UTextReplace(UText *ut,
             int32_t nativeStart, int32_t nativeLimit,
             const UChar *replacementText, int32_t replacmentLength,
             UErrorCode *status);

/**
 * Function type declaration for UText.copy().
 *
 * Copy or move a substring from one position to another within the text,
 * while retaining any metadata associated with the text.
 * This function is used to duplicate or reorder substrings.
 * The destination index must not overlap the source range.
 *
 * The text to be copied or moved is inserted at destIndex;
 * it does not replace or overwrite any existing text.
 *
 * This function need only be implemented for UText types that support writing.
 *
 * When using this function, there should be only a single UText opened onto the
 * underlying native text string.  The function is responsible for updating the
 * text chunk within the UText to reflect the updated iteration position,
 * taking into account any changes to the underlying string's structure caused
 * by the replace operation.
 *
 * @param ut           The UText representing the text to be operated on.
 * @param nativeStart  The index of the start of the region to be copied or moved
 * @param nativeLimit  The index of the character following the region to be replaced.
 * @param nativeDest   The destination index to which the source substring is copied or moved.
 * @param move         If TRUE, then the substring is moved, not copied/duplicated.
 * @param status       receives any error status.  Possible errors include U_NO_WRITE_PERMISSION
 *
 * @draft ICU 3.4
 */
typedef void U_CALLCONV
UTextCopy(UText *ut,
          int32_t nativeStart, int32_t nativeLimit,
          int32_t nativeDest,
          UBool move,
          UErrorCode *status);

/**
 * Function type declaration for UText.mapOffsetToNative().
 * Map from a UChar offset within the current text chunk within the UText to
 *  the corresponding native index in the original source text.
 *
 * This is required only for text providers that do not use native UTF-16 indexes.
 *
 * TODO:  specify behavior with out-of-bounds offset?  Shouldn't ever occur.
 *
 * @param ut     the UText.
 * @param offset UTF-16 offset within text chunk 
 *               0<=offset<=chunk->length.
 * @return Absolute (native) index corresponding to the specified chunk offset.
 *         The returned native index should always be to a code point boundary.
 *
 * @draft ICU 3.4
 */
typedef int32_t U_CALLCONV
UTextMapOffsetToNative(UText *ut, int32_t offset);

/**
 * Function type declaration for UText.mapIndexToUTF16().
 * Map from a native index to a UChar offset within a text chunk
 *
 * This function is required only for text providers that do not use native UTF-16 indexes.
 *
 * @param ut          The UText containing the text chunk.
 * @param nativeIndex Absolute (native) text index, chunk->start<=index<=chunk->limit.
 * @return            Chunk-relative UTF-16 offset corresponding to the specified native
 *                    index.
 *
 * TODO:  specify behavior with out-of-bounds index?  Shouldn't ever occur.
 * @draft ICU 3.4
 */
typedef int32_t U_CALLCONV
UTextMapNativeIndexToUTF16(UText *ut, int32_t nativeIndex);


/**
 * Function type declaration for UText.utextClose().
 *
 * A Text Provider close function is only required for provider types that make
 *  allocations in their open function (or other functions) that must be 
 *  cleaned when the UText is closed.
 *
 * The allocation of the UText struct itself and any "extra" storage
 * associated with the UText is handled by the common UText implementation
 * and does not require provider specific cleanup in a close function.
 *
 * Most UText provider implementations do not need to implement this function.
 *
 * @param ut A UText object to be closed.
 *
 * @draft ICU 3.4
 */
typedef void U_CALLCONV
UTextClose(UText *ut);


/**
  *   UText struct.  Provides the interface between the generic UText access code
  *                  and the UText provider code that works on specific kinds of
  *                  text  (UTF-8, noncontiguous UTF-16, whatever.)
  *
  *                  Applications that are using predefined types of text providers
  *                  to pass text data to ICU services will have no need to view the
  *                  internals of the UText structs that they open.
  *
  * @draft ICU 3.4
  */
struct UText {
    /**
     * (protected) Pointer to string or wrapped object or similar.
     * Not used by caller.
     * @draft ICU 3.4
     */
    const void *context;

    /**
     * (protected) Pointer fields available for use by the text provider.
     * Not used by UText common code.
     * @draft ICU 3.4
     */
    const void *p, *q, *r;

    /**
     *  (protected)  Pointer to additional space requested by the
     *               text provider during the utext_open operation.
     * @draft ICU 3.4
     */
    void          *pExtra;

    /**
     *   (protected)  Size in bytes of the extra space (pExtra).
     *  @draft ICU 3.4
     */
    int32_t        extraSize;

    /**
     *     (private)  Flags for managing the allocation and freeing of
     *                memory associated with this UText.
     * @internal
     */
    int32_t        flags;

    /**
     *     (private)  Magic.  Try to detect when we are handed junk.
     *                        utext_openXYZ() functions take an initialized,
     *                        but not necessarily open, UText struct as an,
     *                        optional fill-in parameter.  This magic field
     *                        is used to check for that initialization.
     *                        Text provider close functions must NOT clear
     *                        the magic field because that would prevent
     *                        reuse of the UText struct.
     * @internal
     */
    uint32_t       magic;


    /**
     * (public) sizeOfStruct=sizeof(UText)
     * Allows possible backward compatible extension.
     *
     * @draft ICU 3.4
     */
    int32_t         sizeOfStruct;

    /**
      * (protected) Integer fields for use by text provider.
      * Not used by caller.
      * @draft ICU 3.4
      */
    int32_t         a, b, c;


    /**
      *  Text provider properties.  This set of flags is maintainted by the
      *                             text provider implementation.
      *  @draft ICU 3.4
      */
    int32_t providerProperties;     



    /**  descriptor for the text chunk that includes or is adjacent to
      *  the current iteration position.
      *   @draft ICU 3.4
      */
    UTextChunk      chunk;   


    /**
     * (public) Function pointer for UTextClone
     *
     * @see UTextClone
     * @draft ICU 3.4
     */
    UTextClone *clone;

    /**
     * (public) function pointer for UTextLength
     * May be expensive to compute!
     *
     * @see UTextLength
     * @draft ICU 3.4
     */
    UTextNativeLength *nativeLength;

    /**
     * (public) Function pointer for UTextAccess.
     *
     * @see UTextAccess
     * @draft ICU 3.4
     */
    UTextAccess *access;

    /**
     * (public) Function pointer for UTextExtract.
     *
     * @see UTextExtract
     * @draft ICU 3.4
     */
    UTextExtract *extract;

    /**
     * (public) Function pointer for UTextReplace.
     *
     * @see UTextReplace
     * @draft ICU 3.4
     */
    UTextReplace *replace;

    /**
     * (public) Function pointer for UTextCopy.
     *
     * @see UTextCopy
     * @draft ICU 3.4
     */
    UTextCopy *copy;

    /**
     * (public) Function pointer for UTextMapOffsetToNative.
     *
     * @see UTextMapOffsetToNative
     * @draft ICU 3.4
     */
    UTextMapOffsetToNative *mapOffsetToNative;

    /**
     * (public) Function pointer for UTextMapNativeIndexToUTF16.
     *
     * @see UTextMapNativeIndexToUTF16
     * @draft ICU 3.4
     */
    UTextMapNativeIndexToUTF16 *mapNativeIndexToUTF16;

    /**
     * (public) Function pointer for UTextClose.
      *
      * @see UTextClose
      * @draft ICU 3.4
      */
    UTextClose  *close;
};


/**
 *  Common function for use by Text Provider implementations to allocate and/or initialize
 *  a new UText struct.  To be called in the implementation of utext_open() functions.
 *  If the supplied UText parameter is null, a new UText struct will be allocated on the heap.
 *  If the supplied UText is already open, the provider's close function will be called
 *  so that the struct can be reused by the open that is in progress.
 *
 * @param ut   pointer to a UText struct to be re-used, or null if a new UText
 *             should be allocated.
 * @param extraSpace The amount of additional space to be allocated as part
 *             of this UText, for use by types of providers that require
 *             additional storage.
 * @param status Errors are returned here.
 * @return pointer to the UText, allocated if necessary, with extra space set up if requested.
 * @draft ICU 3.4
 */
U_DRAFT UText * U_EXPORT2
utext_setup(UText *ut, int32_t extraSpace, UErrorCode *status);

/**
  * @internal
  */
enum {
    UTEXT_MAGIC = 0x345ad82c
};


/**
 *  Initializer for a UTextChunk
 *  @internal
 */
#define UTEXT_CHUNK_INIT   {                               \
                  NULL,                /* contents      */ \
                  0,                   /* offset        */ \
                  0,                   /* length        */ \
                  0,                   /* start         */ \
                  0,                   /* limit         */ \
                  FALSE,               /* nonUTF16idx   */ \
                  FALSE, FALSE, FALSE, /* padding1,2,3  */ \
                  0, 0,                /* padInt1, 2    */ \
                  sizeof(UTextChunk)                       \
}               



/**
 * Initializer for the first part of a UText struct, the part that is
 *  in common for all types of text providers.
 *
 * @internal
 */
#define UTEXT_INITIALIZER_HEAD  \
                  NULL,                 /* context       */ \
                  NULL, NULL, NULL,     /* p, q, r       */ \
                  NULL,                 /* pExtra        */ \
                  0,                    /* extraSize     */ \
                  0,                    /* flags         */ \
                  UTEXT_MAGIC,          /* magic         */ \
                  sizeof(UText),        /* sizeOfStruct  */ \
                  0, 0, 0,              /* a, b, c       */ \
                  0,                    /* providerProps */ \
                  UTEXT_CHUNK_INIT      /* UTextChunk    */



/**
 * initializer to be used with local (stack) instances of a UText
 *  struct.  UText structs must be initialized before passing
 *  them to one of the utext_open functions.
 *
 * @draft ICU 3.4
 */
#define UTEXT_INITIALIZER {                                \
                  UTEXT_INITIALIZER_HEAD,                  \
                  NULL,                 /* clone ()     */ \
                  NULL,                 /* length ()    */ \
                  NULL,                 /* access ()    */ \
                  NULL,                 /* extract ()   */ \
                  NULL,                 /* replace ()   */ \
                  NULL,                 /* copy ()      */ \
                  NULL, NULL,           /* map * 2 ()   */ \
                  NULL                  /* close ()     */ \
}


U_CDECL_END



#endif /* U_HIDE_DRAFT_API */

#endif
