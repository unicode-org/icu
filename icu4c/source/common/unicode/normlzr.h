/*
 ********************************************************************
 * COPYRIGHT: 
 * Copyright (c) 1996-2001, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************
 */

#ifndef NORMLZR_H
#define NORMLZR_H

#include "unicode/utypes.h"
#include "unicode/uobject.h"
#include "unicode/unistr.h"
#include "unicode/chariter.h"
#include "unicode/unorm.h"

struct UCharIterator;
typedef struct UCharIterator UCharIterator;

U_NAMESPACE_BEGIN
/**
 * \brief C++ API: Unicode Normalization 
 *
 * The Normalizer class consists of two parts:
 * - static functions that normalize strings or test if strings are normalized
 * - a Normalizer object is an iterator that takes any kind of text and
 *   provides iteration over its normalized form
 *
 * The static functions are basically wrappers around the C implementation,
 * using UnicodeString instead of UChar*.
 * For basic information about normalization forms and details about the C API
 * please see the documentation in unorm.h.
 *
 * The iterator API with the Normalizer constructors and the non-static functions
 * uses a CharacterIterator as input. It is possible to pass a string which
 * is then internally wrapped in a CharacterIterator.
 * The input text is not normalized all at once, but incrementally where needed
 * (providing efficient random access).
 * This allows to pass in a large text but spend only a small amount of time
 * normalizing a small part of that text.
 * However, if the entire text is normalized, then the iterator will be
 * slower than normalizing the entire text at once and iterating over the result.
 * A possible use of the Normalizer iterator is also to report an index into the
 * original text that is close to where the normalized characters come from.
 *
 * <em>Important:</em> The iterator API was cleaned up significantly for ICU 2.0.
 * The earlier implementation reported the getIndex() inconsistently,
 * and previous() could not be used after setIndex(), next(), first(), and current().
 *
 * Normalizer allows to start normalizing from anywhere in the input text by
 * calling setIndexOnly(), setIndex(), first(), or last().
 * Without calling any of these, the iterator will start at the beginning of the text.
 *
 * At any time, next() returns the next normalized code point (UChar32),
 * with post-increment semantics (like CharacterIterator::next32PostInc()).
 * previous() returns the previous normalized code point (UChar32),
 * with pre-decrement semantics (like CharacterIterator::previous32()).
 *
 * current() and setIndex() return the current code point
 * (respectively the one at the newly set index) without moving
 * the getIndex(). Note that if the text at the current position
 * needs to be normalized, then these functions will do that.
 * (This is why current() is not const.)
 * If you call setIndex() and then previous() then you normalize a piece of
 * text (and get a code point from setIndex()) that you probably do not need.
 * It is more efficient to call setIndexOnly() instead, which does not
 * normalize.
 *
 * getIndex() always refers to the position in the input text where the normalized
 * code points are returned from. It does not always change with each returned
 * code point.
 * The code point that is returned from any of the functions
 * corresponds to text at or after getIndex(), according to the
 * function's iteration semantics (post-increment or pre-decrement).
 *
 * next() returns a code point from at or after the getIndex()
 * from before the next() call. After the next() call, the getIndex()
 * might have moved to where the next code point will be returned from
 * (from a next() or current() call).
 * This is semantically equivalent to array access with array[index++]
 * (post-increment semantics).
 *
 * previous() returns a code point from at or after the getIndex()
 * from after the previous() call.
 * This is semantically equivalent to array access with array[--index]
 * (pre-decrement semantics).
 *
 * Internally, the Normalizer iterator normalizes a small piece of text
 * starting at the getIndex() and ending at a following "safe" index.
 * The normalized results is stored in an internal string buffer, and
 * the code points are iterated from there.
 * With multiple iteration calls, this is repeated until the next piece
 * of text needs to be normalized, and the getIndex() needs to be moved.
 *
 * The following "safe" index, the internal buffer, and the secondary
 * iteration index into that buffer are not exposed on the API.
 * This also means that it is currently not practical to return to
 * a particular, arbitrary position in the text because one would need to
 * know, and be able to set, in addition to the getIndex(), at least also the
 * current index into the internal buffer.
 * It is currently only possible to observe when getIndex() changes
 * (with careful consideration of the iteration semantics),
 * at which time the internal index will be 0.
 * For example, if getIndex() is different after next() than before it,
 * then the internal index is 0 and one can return to this getIndex()
 * later with setIndexOnly().
 *
 * @author Laura Werner, Mark Davis, Markus Scherer
 * @draft ICU 2.0
 */
class U_COMMON_API Normalizer : public UObject {
public:
  /**
   * If DONE is returned from an iteration function that returns a code point,
   * then there are no more normalization results available.
   * @stable
   */
  enum {
      DONE=0xffff
  };

  // Constructors

  /**
   * Creates a new <code>Normalizer</code> object for iterating over the
   * normalized form of a given string.
   * <p>
   * @param str   The string to be normalized.  The normalization
   *              will start at the beginning of the string.
   *
   * @param mode  The normalization mode.
   * @draft ICU 2.0
   */
  Normalizer(const UnicodeString& str, UNormalizationMode mode);
    
  /**
   * Creates a new <code>Normalizer</code> object for iterating over the
   * normalized form of a given string.
   * <p>
   * @param str   The string to be normalized.  The normalization
   *              will start at the beginning of the string.
   *
   * @param length Length of the string, or -1 if NUL-terminated.
   * @param mode  The normalization mode.
   * @draft ICU 2.0
   */
  Normalizer(const UChar* str, int32_t length, UNormalizationMode mode);

  /**
   * Creates a new <code>Normalizer</code> object for iterating over the
   * normalized form of the given text.
   * <p>
   * @param iter  The input text to be normalized.  The normalization
   *              will start at the beginning of the string.
   *
   * @param mode  The normalization mode.
   * @draft ICU 2.0
   */
  Normalizer(const CharacterIterator& iter, UNormalizationMode mode);

  /**
   * Copy constructor.
   * @stable
   */
  Normalizer(const Normalizer& copy);

  /**
   * Destructor
   * @stable
   */
  ~Normalizer();


  //-------------------------------------------------------------------------
  // Static utility methods
  //-------------------------------------------------------------------------

  /**
   * Normalizes a <code>UnicodeString</code> according to the specified normalization mode.
   * This is a wrapper for unorm_normalize(), using UnicodeString's.
   * <p>
   * The <code>options</code> parameter specifies which optional
   * <code>Normalizer</code> features are to be enabled for this operation.
   * Currently the only available option is deprecated.
   * If you want the default behavior corresponding to one of the standard
   * Unicode Normalization Forms, use 0 for this argument.
   * <p>
   * @param source    the input string to be normalized.
   * @param mode      the normalization mode
   * @param options   the optional features to be enabled (0 for no options)
   * @param result    The normalized string (on output).
   * @param status    The error code.
   * @draft ICU 2.0
   */
  static void normalize(const UnicodeString& source,
                        UNormalizationMode mode, int32_t options,
                        UnicodeString& result,
                        UErrorCode &status);

  /**
   * Compose a <code>UnicodeString</code>.
   * This is equivalent to normalize() with mode UNORM_NFC or UNORM_NFKC.
   * This is a wrapper for unorm_normalize(), using UnicodeString's.
   * <p>
   * The <code>options</code> parameter specifies which optional
   * <code>Normalizer</code> features are to be enabled for this operation.
   * Currently the only available option is deprecated.
   * If you want the default behavior corresponding
   * to Unicode Normalization Form <b>C</b> or <b>KC</b>,
   * use 0 for this argument.
   * <p>
   * @param source    the string to be composed.
   * @param compat    Perform compatibility decomposition before composition.
   *                  If this argument is <code>FALSE</code>, only canonical
   *                  decomposition will be performed.
   * @param options   the optional features to be enabled (0 for no options)
   * @param result    The composed string (on output).
   * @param status    The error code.
   * @stable
   */
  static void compose(const UnicodeString& source,
                      UBool compat, int32_t options,
                      UnicodeString& result,
                      UErrorCode &status);

  /**
   * Static method to decompose a <code>UnicodeString</code>.
   * This is equivalent to normalize() with mode UNORM_NFD or UNORM_NFKD.
   * This is a wrapper for unorm_normalize(), using UnicodeString's.
   * <p>
   * The <code>options</code> parameter specifies which optional
   * <code>Normalizer</code> features are to be enabled for this operation.
   * Currently the only available option is deprecated.
   * The desired options should be OR'ed together to determine the value
   * of this argument.  If you want the default behavior corresponding
   * to Unicode Normalization Form <b>D</b> or <b>KD</b>,
   * use 0 for this argument.
   * <p>
   * @param source    the string to be decomposed.
   * @param compat    Perform compatibility decomposition.
   *                  If this argument is <code>FALSE</code>, only canonical
   *                  decomposition will be performed.
   * @param options   the optional features to be enabled (0 for no options)
   * @param result    The decomposed string (on output).
   * @param status    The error code.
   * @stable
   */
  static void decompose(const UnicodeString& source,
                        UBool compat, int32_t options,
                        UnicodeString& result,
                        UErrorCode &status);

  /**
   * Performing quick check on a string, to quickly determine if the string is 
   * in a particular normalization format.
   * This is a wrapper for unorm_quickCheck(), using a UnicodeString.
   *
   * Three types of result can be returned UNORM_YES, UNORM_NO or
   * UNORM_MAYBE. Result UNORM_YES indicates that the argument
   * string is in the desired normalized format, UNORM_NO determines that
   * argument string is not in the desired normalized format. A 
   * UNORM_MAYBE result indicates that a more thorough check is required, 
   * the user may have to put the string in its normalized form and compare the 
   * results.
   * @param source       string for determining if it is in a normalized format
   * @paran mode         normalization format
   * @param status A reference to a UErrorCode to receive any errors
   * @return UNORM_YES, UNORM_NO or UNORM_MAYBE
   *
   * @see isNormalized
   * @draft ICU 2.0
   */
  static inline UNormalizationCheckResult
  quickCheck(const UnicodeString &source, UNormalizationMode mode, UErrorCode &status);

  /**
   * Test if a string is in a given normalization form.
   * This is semantically equivalent to source.equals(normalize(source, mode)) .
   *
   * Unlike unorm_quickCheck(), this function returns a definitive result,
   * never a "maybe".
   * For NFD, NFKD, and FCD, both functions work exactly the same.
   * For NFC and NFKC where quickCheck may return "maybe", this function will
   * perform further tests to arrive at a TRUE/FALSE result.
   *
   * @param src        String that is to be tested if it is in a normalization format.
   * @paran mode       Which normalization form to test for.
   * @param errorCode  ICU error code in/out parameter.
   *                   Must fulfill U_SUCCESS before the function call.
   * @return Boolean value indicating whether the source string is in the
   *         "mode" normalization form.
   *
   * @see quickCheck
   * @draft ICU 2.2
   */
  static inline UBool
  isNormalized(const UnicodeString &src, UNormalizationMode mode, UErrorCode &errorCode);

  /*
   * Concatenate normalized strings, making sure that the result is normalized as well.
   *
   * If both the left and the right strings are in
   * the normalization form according to "mode",
   * then the result will be
   *
   * \code
   *     dest=normalize(left+right, mode)
   * \endcode
   *
   * For details see unorm_concatenate in unorm.h.
   *
   * @param left Left source string.
   * @param right Right source string.
   * @param dest The output string.
   * @param mode The normalization mode.
   * @param options A bit set of normalization options.
   * @param pErrorCode ICU error code in/out parameter.
   *                   Must fulfill U_SUCCESS before the function call.
   * @return result
   *
   * @see unorm_concatenate
   * @see normalize
   * @see unorm_next
   * @see unorm_previous
   *
   * @draft ICU 2.1
   */
  static UnicodeString &
  concatenate(UnicodeString &left, UnicodeString &right,
              UnicodeString &result,
              UNormalizationMode mode, int32_t options,
              UErrorCode &errorCode);

  /**
   * Compare two strings for canonical equivalence.
   * Further options include case-insensitive comparison and
   * code point order (as opposed to code unit order).
   *
   * Canonical equivalence between two strings is defined as their normalized
   * forms (NFD or NFC) being identical.
   * This function compares strings incrementally instead of normalizing
   * (and optionally case-folding) both strings entirely,
   * improving performance significantly.
   *
   * Bulk normalization is only necessary if the strings do not fulfill the FCD
   * conditions. Only in this case, and only if the strings are relatively long,
   * is memory allocated temporarily.
   * For FCD strings and short non-FCD strings there is no memory allocation.
   *
   * Semantically, this is equivalent to
   *   strcmp[CodePointOrder](NFD(foldCase(s1)), NFD(foldCase(s2)))
   * where code point order and foldCase are all optional.
   *
   * UAX 21 2.5 Caseless Matching specifies that for a canonical caseless match
   * the case folding must be performed first, then the normalization.
   *
   * @param s1 First source string.
   * @param s2 Second source string.
   *
   * @param options A bit set of options:
   *   - U_FOLD_CASE_DEFAULT or 0 is used for default options:
   *     Case-sensitive comparison in code unit order, and the input strings
   *     are quick-checked for FCD.
   *
   *   - UNORM_INPUT_IS_FCD
   *     Set if the caller knows that both s1 and s2 fulfill the FCD conditions.
   *     If not set, the function will quickCheck for FCD
   *     and normalize if necessary.
   *
   *   - U_COMPARE_CODE_POINT_ORDER
   *     Set to choose code point order instead of code unit order
   *     (see u_strCompare for details).
   *
   *   - U_COMPARE_IGNORE_CASE
   *     Set to compare strings case-insensitively using case folding,
   *     instead of case-sensitively.
   *     If set, then the following case folding options are used.
   *
   *   - Options as used with case-insensitive comparisons, currently:
   *
   *   - U_FOLD_CASE_EXCLUDE_SPECIAL_I
   *    (see u_strCaseCompare for details)
   *
   * @param errorCode ICU error code in/out parameter.
   *                  Must fulfill U_SUCCESS before the function call.
   * @return <0 or 0 or >0 as usual for string comparisons
   *
   * @see unorm_compare
   * @see normalize
   * @see UNORM_FCD
   * @see u_strCompare
   * @see u_strCaseCompare
   *
   * @draft ICU 2.2
   */
  static inline int32_t
  compare(const UnicodeString &s1, const UnicodeString &s2,
          uint32_t options,
          UErrorCode &errorCode);

  //-------------------------------------------------------------------------
  // Iteration API
  //-------------------------------------------------------------------------
  
  /**
   * Return the current character in the normalized text.
   * current() may need to normalize some text at getIndex().
   * The getIndex() is not changed.
   *
   * @return the current normalized code point
   * @draft ICU 2.0
   */
  UChar32              current(void);

  /**
   * Return the first character in the normalized text.
   * This is equivalent to setIndexOnly(startIndex()) followed by next().
   * (Post-increment semantics.)
   *
   * @return the first normalized code point
   * @draft ICU 2.0
   */
  UChar32              first(void);

  /**
   * Return the last character in the normalized text.
   * This is equivalent to setIndexOnly(endIndex()) followed by previous().
   * (Pre-decrement semantics.)
   *
   * @return the last normalized code point
   * @draft ICU 2.0
   */
  UChar32              last(void);

  /**
   * Return the next character in the normalized text.
   * (Post-increment semantics.)
   * If the end of the text has already been reached, {@link #DONE} is returned.
   *
   * @return the next normalized code point
   * @draft ICU 2.0
   */
  UChar32              next(void);

  /**
   * Return the previous character in the normalized text. and decrement
   * (Pre-decrement semantics.)
   * If the beginning of the text has already been reached, {@link #DONE} is returned.
   *
   * @return the previous normalized code point
   * @draft ICU 2.0
   */
  UChar32              previous(void);

  /**
   * Set the iteration position in the input text that is being normalized
   * and return the first normalized character at that position.
   * This is equivalent to setIndexOnly() followed by current().
   * After setIndex(), getIndex() will return the same index that is
   * specified here.
   *
   * Note that setIndex() normalizes some text starting at the specified index
   * and returns the first code point from that normalization.
   * If the next call is to previous() then this piece of text probably
   * did not need to be normalized.
   *
   * This function is deprecated.
   * It is recommended to use setIndexOnly() instead of setIndex().
   *
   * @param index the desired index in the input text.
   * @return      the normalized character from the text at index
   * @deprecated To be removed after 2002-aug-31. Use setIndexOnly().
   */
  UChar32              setIndex(int32_t index);

  /**
   * Set the iteration position in the input text that is being normalized,
   * without any immediate normalization.
   * After setIndexOnly(), getIndex() will return the same index that is
   * specified here.
   *
   * @param index the desired index in the input text.
   * @draft ICU 2.0
   */
  void                 setIndexOnly(int32_t index);

  /**
   * Reset the index to the beginning of the text.
   * This is equivalent to setIndexOnly(startIndex)).
   * @stable
   */
  void                reset(void);

  /**
   * Retrieve the current iteration position in the input text that is
   * being normalized.
   *
   * A following call to next() will return a normalized code point from
   * the input text at or after this index.
   *
   * After a call to previous(), getIndex() will point at or before the
   * position in the input text where the normalized code point
   * was returned from with previous().
   *
   * @return the current index in the input text
   * @stable
   */
  int32_t            getIndex(void) const;

  /**
   * Retrieve the index of the start of the input text. This is the begin index
   * of the <code>CharacterIterator</code> or the start (i.e. index 0) of the string
   * over which this <code>Normalizer</code> is iterating.
   *
   * @return the smallest index in the input text where the Normalizer operates
   * @stable
   */
  int32_t            startIndex(void) const;

  /**
   * Retrieve the index of the end of the input text. This is the end index
   * of the <code>CharacterIterator</code> or the length of the string
   * over which this <code>Normalizer</code> is iterating.
   * This end index is exclusive, i.e., the Normalizer operates only on characters
   * before this index.
   *
   * @return the first index in the input text where the Normalizer does not operate
   * @stable
   */
  int32_t            endIndex(void) const;

  /**
   * Returns TRUE when both iterators refer to the same character in the same
   * input text.
   *
   * @param that a Normalizer object to compare this one to
   * @return comparison result
   * @stable
   */
  UBool        operator==(const Normalizer& that) const;

  /**
   * Returns FALSE when both iterators refer to the same character in the same
   * input text.
   *
   * @param that a Normalizer object to compare this one to
   * @return comparison result
   * @stable
   */
  inline UBool        operator!=(const Normalizer& that) const;

  /**
   * Returns a pointer to a new Normalizer that is a clone of this one.
   * The caller is responsible for deleting the new clone.
   *
   * @stable
   */
  Normalizer*        clone(void) const;

  /**
   * Generates a hash code for this iterator.
   *
   * @return the hash code
   * @stable
   */
  int32_t                hashCode(void) const;

  //-------------------------------------------------------------------------
  // Property access methods
  //-------------------------------------------------------------------------

  /**
   * Set the normalization mode for this object.
   * <p>
   * <b>Note:</b>If the normalization mode is changed while iterating
   * over a string, calls to {@link #next} and {@link #previous} may
   * return previously buffers characters in the old normalization mode
   * until the iteration is able to re-sync at the next base character.
   * It is safest to call {@link #setIndexOnly}, {@link #reset},
   * {@link #setText setText()}, {@link #first},
   * {@link #last}, etc. after calling <code>setMode</code>.
   * <p>
   * @param newMode the new mode for this <code>Normalizer</code>.
   * @see #getUMode
   * @stable
   */
  void setMode(UNormalizationMode newMode);

  /**
   * Return the normalization mode for this object.
   *
   * This is an unusual name because there used to be a getMode() that
   * returned a different type.
   *
   * @return the mode for this <code>Normalizer</code>
   * @see #setMode
   * @draft ICU 2.0
   */
  UNormalizationMode getUMode(void) const;

  /**
   * Set options that affect this <code>Normalizer</code>'s operation.
   * Options do not change the basic composition or decomposition operation
   * that is being performed, but they control whether
   * certain optional portions of the operation are done.
   * Currently the only available option is deprecated.
   *
   * It is possible to specify multiple options that are all turned on or off.
   *
   * @param   option  the option(s) whose value is/are to be set.
   * @param   value   the new setting for the option.  Use <code>TRUE</code> to
   *                  turn the option(s) on and <code>FALSE</code> to turn it/them off.
   *
   * @see #getOption
   * @stable
   */
  void setOption(int32_t option, 
         UBool value);

  /**
   * Determine whether an option is turned on or off.
   * If multiple options are specified, then the result is TRUE if any
   * of them are set.
   * <p>
   * @param option the option(s) that are to be checked
   * @return TRUE if any of the option(s) are set
   * @see #setOption
   * @stable
   */
  UBool getOption(int32_t option) const;

  /**
   * Set the input text over which this <code>Normalizer</code> will iterate.
   * The iteration position is set to the beginning.
   *
   * @param newText a string that replaces the current input text
   * @param status a UErrorCode
   * @stable
   */
  void setText(const UnicodeString& newText, 
           UErrorCode &status);

  /**
   * Set the input text over which this <code>Normalizer</code> will iterate.
   * The iteration position is set to the beginning.
   *
   * @param newText a CharacterIterator object that replaces the current input text
   * @param status a UErrorCode
   * @stable
   */
  void setText(const CharacterIterator& newText, 
           UErrorCode &status);

  /**
   * Set the input text over which this <code>Normalizer</code> will iterate.
   * The iteration position is set to the beginning.
   *
   * @param newText a string that replaces the current input text
   * @param length the length of the string, or -1 if NUL-terminated
   * @param status a UErrorCode
   * @stable
   */
  void setText(const UChar* newText,
                    int32_t length,
            UErrorCode &status);
  /**
   * Copies the input text into the UnicodeString argument.
   *
   * @param result Receives a copy of the text under iteration.
   * @stable
   */
  void            getText(UnicodeString&  result);

  /**
   * ICU "poor man's RTTI", returns a UClassID for the actual class.
   *
   * @draft ICU 2.2
   */
  virtual inline UClassID getDynamicClassID() const { return getStaticClassID(); }

  /**
   * ICU "poor man's RTTI", returns a UClassID for this class.
   *
   * @draft ICU 2.2
   */
  static inline UClassID getStaticClassID() { return (UClassID)&fgClassID; }

  //-------------------------------------------------------------------------
  // Deprecated APIs
  //-------------------------------------------------------------------------

  /**
   * This tells us what the bits in the "mode" mean.
   * @deprecated To be removed after 2002-sep-30. Use UNormalizationMode.
   */
  enum {
    COMPAT_BIT         = 1,
    DECOMP_BIT         = 2,
    COMPOSE_BIT        = 4,
    FCD_BIT            = 8
  };

  /**
   * The mode of a Normalizer object
   * @deprecated To be removed after 2002-sep-30. Use UNormalizationMode.
   */
  enum EMode {
    /**
     * Null operation for use with the {@link #Normalizer constructors}
     * and the static {@link #normalize normalize} method.  This value tells
     * the <code>Normalizer</code> to do nothing but return unprocessed characters
     * from the underlying UnicodeString or CharacterIterator.  If you have code which
     * requires raw text at some times and normalized text at others, you can
     * use <code>NO_OP</code> for the cases where you want raw text, rather
     * than having a separate code path that bypasses <code>Normalizer</code>
     * altogether.
     * <p>
     * @see #setMode
     * @deprecated To be removed after 2002-sep-30. Use UNORM_NONE from UNormalizationMode.
     */
    NO_OP         = 0,
    
    /**
     * Canonical decomposition followed by canonical composition.  Used with 
     * the {@link #Normalizer constructors} and the static 
     * {@link #normalize normalize}
     * method to determine the operation to be performed.
     * <p>
     * If all optional features (<i>e.g.</i> {@link #IGNORE_HANGUL}) are turned
     * off, this operation produces output that is in
     * <a href=http://www.unicode.org/unicode/reports/tr15/>Unicode Canonical
     * Form</a>
     * <b>C</b>.
     * <p>
     * @see #setMode
     * @deprecated To be removed after 2002-sep-30. Use UNORM_NFC from UNormalizationMode.
     */
    COMPOSE         = COMPOSE_BIT,

    /**
     * Compatibility decomposition followed by canonical composition.
     * Used with the {@link #Normalizer constructors} and the static
     * {@link #normalize normalize} method to determine the operation to be
     * performed.
     * <p>
     * If all optional features (<i>e.g.</i> {@link #IGNORE_HANGUL}) are turned
     * off, this operation produces output that is in
     * <a href=http://www.unicode.org/unicode/reports/tr15/>Unicode Canonical
     * Form</a>
     * <b>KC</b>.
     * <p>
     * @see #setMode
     * @deprecated To be removed after 2002-sep-30. Use UNORM_NFKC from UNormalizationMode.
     */
    COMPOSE_COMPAT     = COMPOSE_BIT | COMPAT_BIT,

    /**
     * Canonical decomposition.  This value is passed to the
     * {@link #Normalizer constructors} and the static 
     * {@link #normalize normalize}
     * method to determine the operation to be performed.
     * <p>
     * If all optional features (<i>e.g.</i> {@link #IGNORE_HANGUL}) are turned
     * off, this operation produces output that is in
     * <a href=http://www.unicode.org/unicode/reports/tr15/>Unicode Canonical 
     * Form</a>
     * <b>D</b>.
     * <p>
     * @see #setMode
     * @deprecated To be removed after 2002-sep-30. Use UNORM_NFD from UNormalizationMode.
     */
    DECOMP         = DECOMP_BIT,

    /**
     * Compatibility decomposition.  This value is passed to the
     * {@link #Normalizer constructors} and the static 
     * {@link #normalize normalize}
     * method to determine the operation to be performed.
     * <p>
     * If all optional features (<i>e.g.</i> {@link #IGNORE_HANGUL}) are turned
     * off, this operation produces output that is in
     * <a href=http://www.unicode.org/unicode/reports/tr15/>Unicode Canonical 
     * Form</a>
     * <b>KD</b>.
     * <p>
     * @see #setMode
     * @deprecated To be removed after 2002-sep-30. Use UNORM_NFKD from UNormalizationMode.
     */
    DECOMP_COMPAT     = DECOMP_BIT | COMPAT_BIT,

    /**
     * @deprecated To be removed after 2002-sep-30. Use UNORM_FCD from UNormalizationMode.
     */
    FCD = FCD_BIT
  };

  /** The options for a Normalizer object */
  enum {
    /**
     * Option to disable Hangul/Jamo composition and decomposition.
     * This option applies to Korean text, 
     * which can be represented either in the Jamo alphabet or in Hangul
     * characters, which are really just two or three Jamo combined
     * into one visual glyph.  Since Jamo takes up more storage space than
     * Hangul, applications that process only Hangul text may wish to turn
     * this option on when decomposing text.
     * <p>
     * The Unicode standard treates Hangul to Jamo conversion as a 
     * canonical decomposition, so this option must be turned <b>off</b> if you
     * wish to transform strings into one of the standard
     * <a href="http://www.unicode.org/unicode/reports/tr15/" target="unicode">
     * Unicode Normalization Forms</a>.
     * <p>
     * @see #setOption
     * @deprecated To be removed (or moved to private for documentation) after 2002-aug-31. Obsolete option.
     */
    IGNORE_HANGUL     = 0x001
  };

  /**
   * Creates a new <code>Normalizer</code> object for iterating over the
   * normalized form of a given string.
   * <p>
   * @param str   The string to be normalized.  The normalization
   *              will start at the beginning of the string.
   *
   * @param mode  The normalization mode.
   * @deprecated To be removed after 2002-sep-30. Use UNormalizationMode.
   */
  Normalizer(const UnicodeString& str, 
         EMode mode);
    
  /**
   * Creates a new <code>Normalizer</code> object for iterating over the
   * normalized form of a given string.
   * <p>
   * The <code>options</code> parameter specifies which optional
   * <code>Normalizer</code> features are to be enabled for this object.
   * <p>
   * @param str   The string to be normalized.  The normalization
   *              will start at the beginning of the string.
   *
   * @param mode  The normalization mode.
   *
   * @param opt   Any optional features to be enabled.
   *              Currently the only available option is {@link #IGNORE_HANGUL}
   *              If you want the default behavior corresponding to one of the
   *              standard Unicode Normalization Forms, use 0 for this argument
   * @deprecated To be removed after 2002-sep-30. Use UNormalizationMode.
   */
  Normalizer(const UnicodeString& str, 
         EMode mode, 
         int32_t opt);

  /**
   * Creates a new <code>Normalizer</code> object for iterating over the
   * normalized form of a given UChar string.
   * <p>
   * @param str   The string to be normalized.  The normalization
   *              will start at the beginning of the string.
   *
   * @param length Lenght of the string
   * @param mode  The normalization mode.
   * @deprecated To be removed after 2002-sep-30. Use UNormalizationMode.
   */
  Normalizer(const UChar* str,
         int32_t length,
         EMode mode);

  /**
   * Creates a new <code>Normalizer</code> object for iterating over the
   * normalized form of a given UChar string.
   * <p>
   * @param str   The string to be normalized.  The normalization
   *              will start at the beginning of the string.
   *
   * @param length Lenght of the string
   * @param mode  The normalization mode.
   * @param opt   Any optional features to be enabled.
   *              Currently the only available option is {@link #IGNORE_HANGUL}
   *              If you want the default behavior corresponding to one of the
   *              standard Unicode Normalization Forms, use 0 for this argument
   * @unimplemented 
   */
  Normalizer(const UChar* str,
         int32_t length,
         EMode mode,
         int32_t option);

  /**
   * Creates a new <code>Normalizer</code> object for iterating over the
   * normalized form of the given text.
   * <p>
   * @param iter  The input text to be normalized.  The normalization
   *              will start at the beginning of the string.
   *
   * @param mode  The normalization mode.
   * @deprecated To be removed after 2002-sep-30. Use UNormalizationMode.
   */
  Normalizer(const CharacterIterator& iter, 
         EMode mode);

  /**
   * Creates a new <code>Normalizer</code> object for iterating over the
   * normalized form of the given text.
   * <p>
   * @param iter  The input text to be normalized.  The normalization
   *              will start at the beginning of the string.
   *
   * @param mode  The normalization mode.
   *
   * @param opt   Any optional features to be enabled.
   *              Currently the only available option is {@link #IGNORE_HANGUL}
   *              If you want the default behavior corresponding to one of the
   *              standard Unicode Normalization Forms, use 0 for this argument
   * @deprecated To be removed after 2002-sep-30. Use UNormalizationMode.
   */
  Normalizer(const CharacterIterator& iter, 
         EMode mode, 
         int32_t opt);

  /**
   * Normalizes a <code>UnicodeString</code> using the given normalization operation.
   * <p>
   * The <code>options</code> parameter specifies which optional
   * <code>Normalizer</code> features are to be enabled for this operation.
   * Currently the only available option is {@link #IGNORE_HANGUL}.
   * If you want the default behavior corresponding to one of the standard
   * Unicode Normalization Forms, use 0 for this argument.
   * <p>
   * @param source    the input string to be normalized.
   *
   * @param aMode     the normalization mode
   *
   * @param options   the optional features to be enabled.
   *
   * @param result    The normalized string (on output).
   *
   * @param status    The error code.
   * @deprecated To be removed after 2002-sep-30. Use UNormalizationMode.
   */
  inline static void
  normalize(const UnicodeString& source, 
            EMode mode, 
            int32_t options,
            UnicodeString& result, 
            UErrorCode &status);

  /**
   * Performing quick check on a string, to quickly determine if the string is 
   * in a particular normalization format.
   * Three types of result can be returned UNORM_YES, UNORM_NO or
   * UNORM_MAYBE. Result UNORM_YES indicates that the argument
   * string is in the desired normalized format, UNORM_NO determines that
   * argument string is not in the desired normalized format. A 
   * UNORM_MAYBE result indicates that a more thorough check is required, 
   * the user may have to put the string in its normalized form and compare the 
   * results.
   * @param source       string for determining if it is in a normalized format
   * @paran mode         normalization format
   * @param status A pointer to an UErrorCode to receive any errors
   * @return UNORM_YES, UNORM_NO or UNORM_MAYBE
   * @deprecated To be removed after 2002-sep-30. Use UNormalizationMode.
   */
  inline static UNormalizationCheckResult
  quickCheck(const UnicodeString& source,
             EMode                mode, 
             UErrorCode&          status);

  /**
   * Converts C's Normalizer::EMode to UNormalizationMode
   * @param mode member of the enum Normalizer::EMode
   * @param status error codes status
   * @return UNormalizationMode equivalent of Normalizer::EMode
   * @deprecated To be removed after 2002-sep-30. Use UNormalizationMode.
   */
  inline static UNormalizationMode getUNormalizationMode(EMode mode, 
                                                  UErrorCode& status);

  /**
  * Converts C++'s UNormalizationMode to Normalizer::EMode
  * @param mode member of the enum UNormalizationMode
  * @param status error codes status
  * @return Normalizer::EMode equivalent of UNormalizationMode
  * @deprecated To be removed after 2002-sep-30. Use UNormalizationMode.
  */
  inline static EMode getNormalizerEMode(UNormalizationMode mode, 
                                         UErrorCode& status);

  /**
   * Set the normalization mode for this object.
   * <p>
   * <b>Note:</b>If the normalization mode is changed while iterating
   * over a string, calls to {@link #next} and {@link #previous} may
   * return previously buffers characters in the old normalization mode
   * until the iteration is able to re-sync at the next base character.
   * It is safest to call {@link #setText setText()}, {@link #first},
   * {@link #last}, etc. after calling <code>setMode</code>.
   * <p>
   * @param newMode the new mode for this <code>Normalizer</code>.
   * The supported modes are:
   * <ul>
   *  <li>{@link #COMPOSE}        - Unicode canonical decompositiion
   *                                  followed by canonical composition.
   *  <li>{@link #COMPOSE_COMPAT} - Unicode compatibility decompositiion
   *                                  follwed by canonical composition.
   *  <li>{@link #DECOMP}         - Unicode canonical decomposition
   *  <li>{@link #DECOMP_COMPAT}  - Unicode compatibility decomposition.
   *  <li>{@link #NO_OP}          - Do nothing but return characters
   *                                  from the underlying input text.
   * </ul>
   *
   * @see #getMode
   * @deprecated To be removed after 2002-sep-30. Use UNormalizationMode.
   */
  inline void setMode(EMode newMode);

  /**
   * Return the basic operation performed by this <code>Normalizer</code>
   *
   * @see #setMode
   * @deprecated To be removed after 2002-sep-30. Use UNormalizationMode.
   */
  inline EMode getMode(void) const;

private:
  //-------------------------------------------------------------------------
  // Private functions
  //-------------------------------------------------------------------------

  // Private utility methods for iteration
  // For documentation, see the source code
  UBool nextNormalize();
  UBool previousNormalize();

  void    init(CharacterIterator *iter);
  void    clearBuffer(void);

  // Helper, without UErrorCode, for easier transitional code
  // remove after 2002-sep-30 with EMode etc.
  inline static UNormalizationMode getUMode(EMode mode);

  //-------------------------------------------------------------------------
  // Private data
  //-------------------------------------------------------------------------

  UNormalizationMode  fUMode;
  int32_t             fOptions;

  // The input text and our position in it
  UCharIterator       *text;

  // The normalization buffer is the result of normalization
  // of the source in [currentIndex..nextIndex[ .
  int32_t         currentIndex, nextIndex;

  // A buffer for holding intermediate results
  UnicodeString       buffer;
  int32_t         bufferPos;

  /**
   * The address of this static class variable serves as this class's ID
   * for ICU "poor man's RTTI".
   */
  static const char fgClassID;
};

//-------------------------------------------------------------------------
// Inline implementations
//-------------------------------------------------------------------------

inline UBool
Normalizer::operator!= (const Normalizer& other) const
{ return ! operator==(other); }

inline void 
Normalizer::normalize(const UnicodeString& source, 
                      EMode mode, int32_t options,
                      UnicodeString& result, 
                      UErrorCode &status) {
  normalize(source, getUNormalizationMode(mode, status), options, result, status);
}

inline UNormalizationCheckResult
Normalizer::quickCheck(const UnicodeString& source,
                       EMode mode, 
                       UErrorCode &status) {
  return quickCheck(source, getUNormalizationMode(mode, status), status);
}

inline UNormalizationCheckResult
Normalizer::quickCheck(const UnicodeString& source,
                       UNormalizationMode mode, 
                       UErrorCode &status) {
    if(U_FAILURE(status)) {
        return UNORM_MAYBE;
    }

    return unorm_quickCheck(source.getBuffer(), source.length(),
                            mode, &status);
}

inline UBool
Normalizer::isNormalized(const UnicodeString& source,
                         UNormalizationMode mode, 
                         UErrorCode &status) {
    if(U_FAILURE(status)) {
        return FALSE;
    }

    return unorm_isNormalized(source.getBuffer(), source.length(),
                              mode, &status);
}

inline int32_t
Normalizer::compare(const UnicodeString &s1, const UnicodeString &s2,
                    uint32_t options,
                    UErrorCode &errorCode) {
  // all argument checking is done in unorm_compare
  return unorm_compare(s1.getBuffer(), s1.length(),
                       s2.getBuffer(), s2.length(),
                       options,
                       &errorCode);
}

inline void
Normalizer::setMode(EMode newMode) {
  UErrorCode status = U_ZERO_ERROR;
  fUMode = getUNormalizationMode(newMode, status);
}

inline Normalizer::EMode
Normalizer::getMode() const {
  UErrorCode status = U_ZERO_ERROR;
  return getNormalizerEMode(fUMode, status);
}

inline UNormalizationMode Normalizer::getUNormalizationMode(
                                   Normalizer::EMode  mode, UErrorCode &status)
{
  if (U_SUCCESS(status))
  { 
    switch (mode)
    {
    case Normalizer::NO_OP : 
      return UNORM_NONE;
    case Normalizer::COMPOSE :
      return UNORM_NFC;
    case Normalizer::COMPOSE_COMPAT :
      return UNORM_NFKC;
    case Normalizer::DECOMP :
      return UNORM_NFD;
    case Normalizer::DECOMP_COMPAT :
      return UNORM_NFKD;
    case Normalizer::FCD:
      return UNORM_FCD;
    default : 
      status = U_ILLEGAL_ARGUMENT_ERROR; 
    }
  }
  return UNORM_DEFAULT;
}

inline UNormalizationMode
Normalizer::getUMode(Normalizer::EMode mode) {
  switch(mode) {
  case Normalizer::NO_OP : 
    return UNORM_NONE;
  case Normalizer::COMPOSE :
    return UNORM_NFC;
  case Normalizer::COMPOSE_COMPAT :
    return UNORM_NFKC;
  case Normalizer::DECOMP :
    return UNORM_NFD;
  case Normalizer::DECOMP_COMPAT :
    return UNORM_NFKD;
  case Normalizer::FCD:
    return UNORM_FCD;
  default : 
    return UNORM_DEFAULT;
  }
}

inline Normalizer::EMode Normalizer::getNormalizerEMode(
                                  UNormalizationMode mode, UErrorCode &status)
{
  if (U_SUCCESS(status))
  {
    switch (mode)
    {
    case UNORM_NONE :
      return Normalizer::NO_OP;
    case UNORM_NFD :
      return Normalizer::DECOMP;
    case UNORM_NFKD :
      return Normalizer::DECOMP_COMPAT;
    case UNORM_NFC :
      return Normalizer::COMPOSE;
    case UNORM_NFKC :
      return Normalizer::COMPOSE_COMPAT;
    case UNORM_FCD:
      return Normalizer::FCD;
    default : 
      status = U_ILLEGAL_ARGUMENT_ERROR; 
    }
  }
  return Normalizer::DECOMP_COMPAT;
}

U_NAMESPACE_END
#endif // _NORMLZR
