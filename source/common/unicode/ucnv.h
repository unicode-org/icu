/*
**********************************************************************
*   Copyright (C) 1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
 *  ucnv.h:
 *  External APIs for the ICU's codeset conversion library
 *  Bertrand A. Damiba
 *
 * Modification History:
 *
 *   Date        Name        Description
 *   04/04/99    helena      Fixed internal header inclusion.
 *   05/11/00    helena      Added setFallback and usesFallback APIs.
 */

/**
 * @name Character Conversion C API
 *
 * Character Conversion C API documentation is still under construction. 
 * Please check for updates soon.
 */

#ifndef UCNV_H
#define UCNV_H

#include "unicode/utypes.h"
#include "unicode/ucnv_bld.h"
#include "unicode/ucnv_err.h"

U_CDECL_BEGIN

typedef void (*UConverterToUCallback) (UConverter *,
				  UChar **,
				  const UChar *,
				  const char **,
				  const char *,
				  int32_t* offsets,
				  UBool,
				  UErrorCode *);

typedef void (*UConverterFromUCallback) (UConverter *,
				    char **,
				    const char *,
				    const UChar **,
				    const UChar *,
				    int32_t* offsets,
				    UBool,
				    UErrorCode *);

U_CDECL_END

/**
 * Character that separates converter names from options and options from each other.
 * @see ucnv_open
 */
#define UCNV_OPTION_SEP_CHAR ','

/** String version of UCNV_OPTION_SEP_CHAR. */
#define UCNV_OPTION_SEP_STRING ","

/**
 * Character that separates a converter option from its value.
 * @see ucnv_open
 */
#define UCNV_VALUE_SEP_CHAR '='

/** String version of UCNV_VALUE_SEP_CHAR. */
#define UCNV_VALUE_SEP_STRING "="

/**
 * Converter option for specifying a locale.
 * @see ucnv_open
 */
#define UCNV_LOCALE_OPTION_STRING ",locale="

/**
 * Creates a UConverter object with the names specified as a C string.
 * The actual name will be resolved with the alias file.
 * If <code>NULL</code> is passed for the converter name, it will create one with the
 * getDefaultName return value.
 *
 * <p>A converter name for ICU 1.5 and above may contain options
 * like a locale specification to control the specific behavior of
 * the newly instantiated converter.
 * The meaning of the options depends on the particular converter.
 * If an option is not defined for or recognized by a given converter, then it is ignored.</p>
 *
 * <p>Options are appended to the converter name string, with a
 * <code>UCNV_OPTION_SEP_CHAR</code> between the name and the first option and
 * also between adjacent options.</p>
 *
 * @param converterName : name of the uconv table, may have options appended
 * @param err outgoing error status <TT>U_MEMORY_ALLOCATION_ERROR, TABLE_NOT_FOUND</TT>
 * @return the created Unicode converter object, or <TT>NULL</TT> if an error occured
 * @see ucnv_openU
 * @see ucnv_openCCSID
 * @see ucnv_close
 * @stable
 */

U_CAPI
UConverter* U_EXPORT2 ucnv_open   (const char *converterName, UErrorCode * err);


/**
 * Creates a Unicode converter with the names specified as unicode string. The name should be limited to
 * the ASCII-7 alphanumerics range. The actual name will be resolved with the alias file.
 * if <TT>NULL</TT> is passed for the converter name, it will create one with the
 * getDefaultName return value.
 * @param converterName : name of the uconv table in a zero terminated Unicode string
 * @param err outgoing error status <TT>U_MEMORY_ALLOCATION_ERROR, TABLE_NOT_FOUND</TT>
 * @return the created Unicode converter object, or <TT>NULL</TT> if an error occured
 * @see ucnv_open
 * @see ucnv_openCCSID
 * @see ucnv_close
 * @stable
 */
U_CAPI UConverter* U_EXPORT2 ucnv_openU (const UChar * name,
                                         UErrorCode * err);



/**
 * Creates a UConverter object using a CCSID number.
 *
 * @param codepage : codepage # of the uconv table
 * @param platform : codepage's platform (now only <TT>IBM</TT> supported)
 * @param err error status <TT>U_MEMORY_ALLOCATION_ERROR, TABLE_NOT_FOUND</TT>
 * @return the created Unicode converter object, or <TT>NULL</TT> if and error occured
 * @see ucnv_open
 * @see ucnv_openU
 * @see ucnv_close
 * @stable
 */

U_CAPI UConverter* U_EXPORT2 ucnv_openCCSID (int32_t codepage,
					   UConverterPlatform platform,
                       UErrorCode * err);


/**
 * Deletes the unicode converter.
 *
 * @param converter the converter object to be deleted
 * @see ucnv_open
 * @see ucnv_openU
 * @see ucnv_openCCSID
 * @stable
 */
U_CAPI void  U_EXPORT2 ucnv_close (UConverter * converter);



/**
 * Fills in the output parameter, subChars, with the substitution characters
 * as multiple bytes.
 *
 * @param converter: the Unicode converter
 * @param subChars: the subsitution characters
 * @param len: on input the capacity of subChars, on output the number of bytes copied to it
 * @param  err: the outgoing error status code.
 * If the substitution character array is too small, an
 * <TT>U_INDEX_OUTOFBOUNDS_ERROR</TT> will be returned.
 * @see ucnv_setSubstChars
 * @stable
 */

U_CAPI void U_EXPORT2
    ucnv_getSubstChars (const UConverter * converter,
			char *subChars,
			int8_t * len,
			UErrorCode * err);

/**
 * Sets the substitution chars when converting from unicode to a codepage. The
 * substitution is specified as a string of 1-4 bytes, and may contain <TT>NULL</TT> byte.
 * The fill-in parameter err will get the error status on return.
 * @param converter the Unicode converter
 * @param subChars the substitution character byte sequence we want set
 * @param len the number of bytes in subChars
 * @param err the error status code.  <TT>U_INDEX_OUTOFBOUNDS_ERROR </TT> if
 * len is bigger than the maximum number of bytes allowed in subchars
 * @see ucnv_getSubstChars
 * @stable
 */

U_CAPI void U_EXPORT2
    ucnv_setSubstChars (UConverter * converter,
			const char *subChars,
			int8_t len,
			UErrorCode * err);



/**
 * Fills in the output parameter, errBytes, with the error characters from the
 * last failing conversion.
 *
 * @param converter: the Unicode converter
 * @param errBytes: the bytes in error
 * @param len: on input the capacity of errBytes, on output the number of bytes copied to it
 * @param  err: the outgoing error status code.
 * If the substitution character array is too small, an
 * <TT>U_INDEX_OUTOFBOUNDS_ERROR</TT> will be returned.
 * @stable
 */

U_CAPI void U_EXPORT2
    ucnv_getInvalidChars (const UConverter * converter,
			  char *errBytes,
			  int8_t * len,
			  UErrorCode * err);


/**
 * Fills in the output parameter, errChars, with the error characters from the
 * last failing conversion.
 *
 * @param converter: the Unicode converter
 * @param errUChars: the bytes in error
 * @param len: on input the capacity of errUChars, on output the number of UChars copied to it
 * @param  err: the outgoing error status code.
 * If the substitution character array is too small, an
 * <TT>U_INDEX_OUTOFBOUNDS_ERROR</TT> will be returned.
 * @stable
 */

U_CAPI void U_EXPORT2
    ucnv_getInvalidUChars (const UConverter * converter,
			   char *errUChars,
			   int8_t * len,
			   UErrorCode * err);

/**
 * Resets the state of stateful conversion to the default state. This is used
 * in the case of error to restart a conversion from a known default state.
 * it will also empty the internal output buffers.
 * @param converter the Unicode converter
 * @stable
 */

U_CAPI void U_EXPORT2
    ucnv_reset (UConverter * converter);

/**
 * Returns the maximum length of bytes used by a character. This varies between 1 and 4
 * @param converter the Unicode converter
 * @return the maximum number of bytes allowed by this particular converter
 * @see ucnv_getMinCharSize
 * @stable
 */
U_CAPI int8_t U_EXPORT2
    ucnv_getMaxCharSize (const UConverter * converter);


/**
 * Returns the minimum byte length for characters in this codepage. This is either
 * 1 or 2 for all supported codepages.
 * @param converter the Unicode converter
 * @return the minimum number of bytes allowed by this particular converter
 * @see ucnv_getMaxCharSize
 * @stable
 */
U_CAPI int8_t U_EXPORT2
    ucnv_getMinCharSize (const UConverter * converter);


/**
 * Returns the display name of the converter passed in based on the Locale passed in,
 * in the case the locale contains no display name, the internal ASCII name will be
 * filled in.
 *
 * @param converter the Unicode converter.
 * @param displayLocale is the specific Locale we want to localised for
 * @param displayName user provided buffer to be filled in
 * @param displayNameCapacty size of displayName Buffer
 * @param err: outgoing error code.
 * @return displayNameLength number of UChar needed in displayName
 * @see ucnv_getName
 * @stable
 */
U_CAPI
  int32_t U_EXPORT2 ucnv_getDisplayName (const UConverter * converter,
			       const char *displayLocale,
			       UChar * displayName,
			       int32_t displayNameCapacity,
			       UErrorCode * err);

/**
 * Gets the name of the converter (zero-terminated).
 * the name will be the internal name of the converter, the lifetime of the returned
 * string will be that of the converter passed to this function.
 * @param converter the Unicode converter
 * @param err UErrorCode status
 * @return the internal name of the converter
 * @see ucnv_getDisplayName
 * @stable
 */
U_CAPI
  const char * U_EXPORT2 ucnv_getName (const UConverter * converter, UErrorCode * err);


/**
 * Gets a codepage number associated with the converter. This is not guaranteed
 * to be the one used to create the converter. Some converters do not represent
 * IBM registered codepages and return zero for the codepage number.
 * The error code fill-in parameter indicates if the codepage number is available.
 * @param converter the Unicode converter
 * @param err the error status code.
 * the converter is <TT>NULL</TT> or if converter's data table is <TT>NULL</TT>.
 * @return If any error occurrs, -1 will be returned otherwise, the codepage number
 * will be returned
 * @stable
 */
U_CAPI int32_t U_EXPORT2
    ucnv_getCCSID (const UConverter * converter,
		   UErrorCode * err);

/**
 * Gets a codepage platform associated with the converter. Currently, only <TT>IBM</TT> is supported
 * The error code fill-in parameter indicates if the codepage number is available.
 * @param converter the Unicode converter
 * @param err the error status code.
 * the converter is <TT>NULL</TT> or if converter's data table is <TT>NULL</TT>.
 * @return The codepage platform
 * @stable
 */
U_CAPI UConverterPlatform U_EXPORT2
    ucnv_getPlatform (const UConverter * converter,
		      UErrorCode * err);

/**
 *Gets the type of conversion associated with the converter
 * e.g. SBCS, MBCS, DBCS, UTF8, UTF16_BE, UTF16_LE, ISO_2022, EBCDIC_STATEFUL, LATIN_1
 * @param converter: a valid, opened converter
 * @return the type of the converter
 * @stable
 */
U_CAPI UConverterType U_EXPORT2
ucnv_getType (const UConverter * converter);

/**
 *Gets the "starter" bytes for the converters of type MBCS
 *will fill in an <TT>U_ILLEGAL_ARGUMENT_ERROR</TT> if converter passed in
 *is not MBCS.
 *fills in an array of boolean, with the value of the byte as offset to the array.
 *At return, if TRUE is found in at offset 0x20, it means that the byte 0x20 is a starter byte
 *in this converter.
 * @param converter: a valid, opened converter of type MBCS
 * @param starters: an array of size 256 to be filled in
 * @param err: an array of size 256 to be filled in
 * @see ucnv_getType
 * @stable
 */
U_CAPI void U_EXPORT2 ucnv_getStarters(const UConverter* converter, 
				     UBool starters[256],
				     UErrorCode* err);


/**
 * Gets the current calback function used by the converter when illegal or invalid sequence found.
 *
 * @param converter the unicode converter
 * @return a pointer to the callback function
 * @see ucnv_setToUCallBack
 * @stable
 */
U_CAPI UConverterToUCallback U_EXPORT2
    ucnv_getToUCallBack (const UConverter * converter);

/**
 * Gets the current callback function used by the converter when illegal or invalid sequence found.
 *
 * @param converter the unicode converter
 * @return a pointer to the callback function
 * @see ucnv_setFromUCallBack
 * @stable
 */
U_CAPI UConverterFromUCallback U_EXPORT2
    ucnv_getFromUCallBack (const UConverter * converter);

/**
 * Gets the current callback function used by the converter when illegal or invalid sequence found.
 *
 * @param converter the unicode converter
 * @param action the callback function we want to set.
 * @param err The error code status
 * @return the previously assigned callback function pointer
 * @see ucnv_getToUCallBack
 * @stable
 */
U_CAPI UConverterToUCallback U_EXPORT2
    ucnv_setToUCallBack (UConverter * converter,
			 UConverterToUCallback action,
			 UErrorCode * err);

/**
 * Gets the current callback function used by the converter when illegal or invalid sequence found.
 *
 * @param converter the unicode converter
 * @param action the callback function we want to set.
 * @param err The error code status
 * @return the previously assigned callback function pointer
 * @see ucnv_getFromUCallBack
 * @stable
 */
U_CAPI UConverterFromUCallback U_EXPORT2
    ucnv_setFromUCallBack (UConverter * converter,
			   UConverterFromUCallback action,
			   UErrorCode * err);


/**
 * Transcodes an array of unicode characters to an array of codepage characters.
 * The source pointer is an I/O parameter, it starts out pointing where the function is
 * to begin transcoding, and ends up pointing after the first sequence of the bytes
 * that it encounters that are semantically invalid.
 * if ucnv_setToUCallBack is called with an action other than <TT>STOP</TT>
 * before a call is made to this API, <TT>consumed</TT> and <TT>source</TT> should point to the same place
 * (unless <TT>target</TT> ends with an imcomplete sequence of bytes and <TT>flush</TT> is <TT>FALSE</TT>).
 * the <TT>target</TT> buffer buffer needs to be a least the size of the maximum # of bytes per characters
 * allowed by the target codepage.
 * @param converter the Unicode converter
 * @param converter the Unicode converter
 * @param target : I/O parameter. Input : Points to the beginning of the buffer to copy
 *  codepage characters to. Output : points to after the last codepage character copied
 *  to <TT>target</TT>.
 * @param targetLimit the pointer to the end of the <TT>target</TT> array
 * @param source the source Unicode character array
 * @param sourceLimit the pointer to the end of the source array
 * @param offsets if NULL is passed, nothing will happen to it, otherwise it needs to have the same number
 * of allocated cells as <TT>target</TT>. Will fill in offsets from target to source pointer
 * e.g: <TT>offsets[3]</TT> is equal to 6, it means that the <TT>target[3]</TT> was a result of transcoding <TT>source[6]</TT>
 * For output data carried across calls -1 will be placed for offsets.
 * @param flush <TT>TRUE</TT> if the buffer is the last buffer of the conversion interation
 * and the conversion will finish with this call, FALSE otherwise.
 * @param err the error status.  <TT>U_ILLEGAL_ARGUMENT_ERROR</TT> will be returned if the
 * converter is <TT>NULL</TT>.
 * @see ucnv_fromUChars
 * @see ucnv_convert
 * @see ucnv_getMinCharSize
 * @see ucnv_setToUCallBack
 * @draft backslash versus Yen sign in shift-JIS
 */

U_CAPI
  void U_EXPORT2 ucnv_fromUnicode (UConverter * converter,
			 char **target,
			 const char *targetLimit,
			 const UChar ** source,
			 const UChar * sourceLimit,
			 int32_t* offsets,
			 UBool flush,
			 UErrorCode * err);


/**
 * Converts an array of codepage characters into an array of unicode characters.
 * The source pointer is an I/O parameter, it starts out pointing at the place
 * to begin translating, and ends up pointing after the first sequence of the bytes
 * that it encounters that are semantically invalid.
 * if ucnv_setFromUCallBack is called with an action other than STOP
 * before a call is made to this API, consumed and source should point to the same place
 * (unless target ends with an imcomplete sequence of bytes and flush is FALSE).
 * @param converter the Unicode converter
 * @param target : I/O parameter. Input : Points to the beginning of the buffer to copy
 *  Unicode characters to. Output : points to after the last UChar copied to target.
 * @param targetLimit the pointer to the end of the target array
 * @param source the source codepage character array
 * @param sourceLimit the pointer to the end of the source array
 * @param offsets if NULL is passed, nothing will happen to it, otherwise it needs to have the same number
 * of allocated cells as <TT>target</TT>. Will fill in offsets from target to source pointer
 * e.g: <TT>offsets[3]</TT> is equal to 6, it means that the <TT>target[3]</TT> was a result of transcoding <TT>source[6]</TT>
 * For output data carried across calls -1 will be placed for offsets.
 * @param flush TRUE if the buffer is the last buffer and the conversion will finish
 * in this call, FALSE otherwise. 
 * @param err the error code status  <TT>U_ILLEGAL_ARGUMENT_ERROR</TT> will be returned if the
 * converter is <TT>NULL</TT>, or if <TT>targetLimit</TT> and <TT>sourceLimit</TT> are misaligned.
 * @see ucnv_toUChars
 * @see ucnv_getNextUChar
 * @see ucnv_convert
 * @see ucnv_setFromUCallBack
 * @stable
 */

U_CAPI
  void U_EXPORT2 ucnv_toUnicode (UConverter * converter,
		       UChar ** target,
		       const UChar * targetLimit,
		       const char **source,
		       const char *sourceLimit,
		       int32_t* offsets,
		       UBool flush,
		       UErrorCode * err);


/**
 * Transcodes the source Unicode string to the target string in a codepage encoding
 * with the specified Unicode converter.  For example, if a Unicode to/from JIS
 * converter is specified, the source string in Unicode will be transcoded to JIS
 * encoding.  The result will be stored in JIS encoding.
 * if any problems during conversion are encountered it will SUBSTITUTE with the default (initial)
 * substitute characters.
 * This function is a more convenient but less efficient version of \Ref{ucnv_fromUnicode}.
 * @param converter the Unicode converter
 * @param source the <TT>source</TT> Unicode string (zero Terminated)
 * @param target the <TT>target</TT> string in codepage encoding (<STRONG>not zero-terminated</STRONG> because some
 * codepage do not use '\0' as a string terminator
 * @param targetCapacity Input the number of bytes available in the <TT>target</TT> buffer
 * @param source the source buffer to convert with
 * @param sourceLength the length of the source buffer. If -1 is passed in as the value, 
 * the source buffer is NULL terminated string and whole source buffer will be converted.
 * @param err the error status code.
 * <TT>U_INDEX_OUTOFBOUNDS_ERROR</TT> will be returned if the
 * the # of bytes provided are not enough for transcoding.
 * <TT>U_ILLEGAL_ARGUMENT_ERROR</TT> is returned if the converter is <TT>NULL</TT> or the source or target string is empty.
 * <TT>U_BUFFER_OVERFLOW_ERROR</TT> when <TT>targetSize</TT> turns out to be bigger than <TT>targetCapacity</TT>
 * @return number of bytes needed in target, regardless of <TT>targetCapacity</TT>
 * @see ucnv_fromUnicode
 * @see ucnv_convert
 * @draft backslash versus Yen sign in shift-JIS
 */
U_CAPI
  int32_t U_EXPORT2 ucnv_fromUChars (const UConverter * converter,
			   char *target,
			   int32_t targetCapacity,
			   const UChar * source,
               int32_t sourceLength,
			   UErrorCode * err);





/**
 * Transcode the source string in codepage encoding to the target string in
 * Unicode encoding.  For example, if a Unicode to/from JIS
 * converter is specified, the source string in JIS encoding will be transcoded
 * to Unicode and placed into a provided target buffer.
 * if any problems during conversion are encountered it will SUBSTITUTE with the Unicode REPLACEMENT char
 * We recomment, the size of the target buffer needs to be at least as long as the maximum # of bytes per char
 * in this character set.
 * A zero-terminator will be placed at the end of the target buffer
 * This function is a more convenient but less efficient version of \Ref{ucnv_toUnicode}.
 * @param converter the Unicode converter
 * @param source the source string in codepage encoding
 * @param target the target string in Unicode encoding
 * @param targetCapacity capacity of the target buffer
 * @param sourceSize : Number of bytes in <TT>source</TT> to be transcoded
 * @param err the error status code
 * <TT>U_MEMORY_ALLOCATION_ERROR</TT> will be returned if the
 * the internal process buffer cannot be allocated for transcoding.
 * <TT>U_ILLEGAL_ARGUMENT_ERROR</TT> is returned if the converter is <TT>NULL</TT> or
 * if the source or target string is empty.
 * <TT>U_BUFFER_OVERFLOW_ERROR</TT> when the input buffer is prematurely exhausted and targetSize non-<TT>NULL</TT>.
 * @return the number of UChar needed in target (including the zero terminator)
 * @see ucnv_getNextUChar
 * @see ucnv_toUnicode
 * @see ucnv_convert
 * @stable
 */
U_CAPI
  int32_t U_EXPORT2 ucnv_toUChars (const UConverter * converter,
			 UChar * target,
			 int32_t targetCapacity,
			 const char *source,
			 int32_t sourceSize,
			 UErrorCode * err);

/********************************
 * Will convert a codepage buffer one character at a time.
 * This function was written to be efficient when transcoding small amounts of data at a time.
 * In that case it will be more efficient than \Ref{ucnv_toUnicode}.
 * When converting large buffers use \Ref{ucnv_toUnicode}.
 *@param converter an open UConverter
 *@param source the address of a pointer to the codepage buffer, will be updated to point after
 *the bytes consumed in the conversion call.
 *@param points to the end of the input buffer
 *@param err fills in error status (see ucnv_toUnicode)
 *@return a UChar resulting from the partial conversion of source
 *@see ucnv_toUnicode
 *@see ucnv_toUChars
 *@see ucnv_convert
 *@stable
 */
U_CAPI
  UChar32 U_EXPORT2 ucnv_getNextUChar (UConverter * converter,
			   const char **source,
			   const char *sourceLimit,
			   UErrorCode * err);


/**************************
* Will convert a sequence of bytes from one codepage to another.
* This is <STRONG>NOT AN EFFICIENT</STRONG> way to transcode.
* use \Ref{ucnv_toUnicode} and \Ref{ucnv_fromUnicode} for efficiency
* @param toConverterName: The name of the converter that will be used to encode the output buffer
* @param fromConverterName: The name of the converter that will be used to decode the input buffer
* @param target: Pointer to the output buffer to write to
* @param targetCapacity: on input contains the capacity of target
* @param source: Pointer to the input buffer
* @param sourceLength: on input contains the capacity of source
* @param err: fills in an error status
* @return  will be filled in with the number of bytes needed in target
* @see ucnv_fromUnicode
* @see ucnv_toUnicode
* @see ucnv_fromUChars
* @see ucnv_toUChars
* @see ucnv_getNextUChar
* @draft backslash versus Yen sign in shift-JIS
*/
U_CAPI
  int32_t U_EXPORT2 ucnv_convert (const char *toConverterName,
			const char *fromConverterName,
			char *target,
			int32_t targetCapacity,
			const char *source,
			int32_t sourceLength,
			UErrorCode * err);

/**
 * SYSTEM API
 * Iterates through every cached converter and frees all the unused ones.
 *
 * @return the number of cached converters successfully deleted
 * @stable
 * @system
 */
U_CAPI int32_t U_EXPORT2 ucnv_flushCache (void);


/**
 * provides a string containing the internal name (based on the alias file) of the converter.
 * given an index.
 * @param n the number of converters available on the system (<TT>[0..ucnv_countAvaiable()]</TT>)
 * @return a pointer a string (library owned), or <TT>NULL</TT> if the index is out of bounds.
 * @see ucnv_countAvailable
 * @stable
 */
U_CAPI
  const char * U_EXPORT2 ucnv_getAvailableName (int32_t n);

/**
 * returns the number of available converters.
 *
 * @return the number of available converters
 * @see ucnv_getAvailableName
 * @stable
 */
U_CAPI int32_t U_EXPORT2 ucnv_countAvailable (void);


/**
 * Gives the number of aliases for given converter or alias name
 * @param alias alias name
 * @param pErrorCode result of operation
 * @return number of names on alias list
 * @stable
 */
U_CAPI uint16_t
ucnv_countAliases(const char *alias, UErrorCode *pErrorCode);

/**
 * Gives the name of the alias at given index of alias list
 * @param alias alias name
 * @param n index in alias list
 * @param pErrorCode result of operation
 * @return returns the name of the alias at given index
 * @stable
 */
U_CAPI const char *
ucnv_getAlias(const char *alias, uint16_t n, UErrorCode *pErrorCode);

/**
 * Fill-up the list of alias names for the given alias
 * @param alias alias name
 * @param aliases fill-in list, aliases is a pointer to an array of
 *        <code>ucnv_countAliases()</code> string-pointers
 *        (<code>const char *</code>) that will be filled in
 * @param pErrorCode result of operation
 * @stable
 */
U_CAPI void
ucnv_getAliases(const char *alias, const char **aliases, UErrorCode *pErrorCode);

/**
 * returns the current default converter name.
 *
 * @return returns the current default converter name;
 *         if a default converter name cannot be determined,
 *         then <code>NULL</code> is returned
 * @see ucnv_setDefaultName
 * @stable
 */
U_CAPI const char * U_EXPORT2 ucnv_getDefaultName (void);

/**
 * sets the current default converter name.
 * The lifetime of the return ptr is that of the library
 * @param name: the converter name you want as default (has to appear in alias file)
 * @see ucnv_getDefaultName
 * @system
 */
U_CAPI void U_EXPORT2 ucnv_setDefaultName (const char *name);

/**
 * Fixes the backslash character mismapping.  For example, in SJIS, the backslash 
 * character in the ASCII portion is also used to represent the yen currency sign.  
 * When mapping from Unicode character 0x005C, it's unclear whether to map the 
 * character back to yen or backslash in SJIS.  This function will take the input
 * buffer and replace all the yen sign characters with backslash.  This is necessary
 * when the user tries to open a file with the input buffer on Windows.
 * @param source the input buffer to be fixed
 * @param sourceLength the length of the input buffer
 * @draft
 */
U_CAPI void U_EXPORT2 ucnv_fixFileSeparator(const UConverter *cnv, UChar* source, int32_t sourceLen);

/**
 * Determines if the converter contains ambiguous mappings of the same
 * character or not.
 * @return TRUE if the converter contains ambiguous mapping of the same 
 * character, FALSE otherwise.
 * @draft
 */
U_CAPI UBool U_EXPORT2 ucnv_isAmbiguous(const UConverter *cnv);

/**
 * Sets the converter to use fallback mapping or not.
 * @param cnv The converter to set the fallback mapping usage for.
 * @param usesFallback TRUE if the user wants the converter to take advantage of the fallback 
 * mapping, FALSE otherwise.
 * @draft
 */
U_CAPI void U_EXPORT2 ucnv_setFallback(UConverter *cnv, UBool usesFallback);

/**
 * Determines if the converter uses fallback mappings or not.
 * @return TRUE if the converter uses fallback, FALSE otherwise.
 * @draft
 */
U_CAPI UBool U_EXPORT2 ucnv_usesFallback(const UConverter *cnv);

#endif
/*_UCNV*/


