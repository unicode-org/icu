/*
**********************************************************************
*   Copyright (C) 2005, International Business Machines
*   Corporation and others.  All Rights Reserved.
**********************************************************************
*   file name:  ucsdet.h
*   encoding:   US-ASCII
*   indentation:4
*
*   created on: 2005Aug04
*   created by: Andy Heninger
*
*   ICU Character Set Detection, API for C
*
*  Draft version 9 Aug 2005
*
*  Questions and Issues
*
*  0   is (char *) or (UChar *) for the encoding name parameters?
*       o  char * is consistent with the converter API encoding name params.
*       o  char * causes awkwardness in 100% Unicode apps.
*          You would think that a Unicode library would use Unicode for
*          strings.
*
*  0     Most of the functions for looking at the results of a detection take
*        an index parameter to specify which of the possibly matching charsets
*        to use.  My guess is that most of the time users will only care about
*        the best match, making this index parameter just boilerplate noise.
*        But the alternative of adding additional functions would bloat the
*        API.  A bigger API is worse in my opinion
*
*  0  UText: Accessing the converted data as UText presents some issues.
*          With a non-seekable stream as the source of the data, a
*          fairly customized UText will be needed that switches between
*          the data used for detection and data further down the stream.
*
*          Also, cloning of any FILE based UText is going to take some
*          thought, and cloning of non-seekable streams will probably
*          be impossible.  UText promises that shallow clone is always
*          available.  More thought is needed on the implications of this,
*          which could result in some tweaking to UText itself.
*
* 0   When does the actual detection happen?  Should there be an explicit
*          ucsdet_detect() function that does the work, after which the various
*          getters can query the results?  Or should the detection be triggered
*          by the first call to any getter after the input text has been set/changed?
*
*          A separate detect() function adds boilerplate to the user's code.
*          But it allows all the getters to be const for the detector, which
*          is nice.
*
*          My inclination is to not have an explicit detect() function.
*          This loses safe concurrent access to the getters, but I doubt
*          that anyone will ever care.
*
*  0  The match type (BOM, encoding scheme, language type, etc.) from the
*          Java API is omitted for now.  It didn't get implemented in the
*          Java for ICU 3.4, and I don't see it as being very useful.
*
*  O  When the input is coming from a FILE stream, and the charset
*          detector is creating a UFILE or a UText, need to think about
*          how best to handle ownership of the FILE.  Who closes it?
*
*  0  Dependencies on ICU IO
*/
#ifndef CSDET_H
#define CSDET_H

#include "unicode/utypes.h"
#include "unicode/ustdio.h"

#ifndef U_HIDE_DRAFT_API


struct UCharSetDetector;
/**
  * Structure representing a charset detector
  * @draft ICU 3.6
  */
typedef struct UCharSetDetector UCharSetDetector;

/**
  *  Open a charset detector.
  *
  *  @param status Any error conditions occurring during the open
  *                operation are reported back in this variable.
  *  @return the newly opened charset detector.
  *  @draft ICU 3.6
  */
U_DRAFT UCharSetDetector * U_EXPORT2
ucsdet_open(UErrorCode   *status);

/**
  * Close a charset detector.  All storage and any other resources
  *   owned by this charset detector will be released.  Failure to
  *   close a charset detector when finished with it can result in
  *   memory leaks in the application.
  *
  *  @param csd  The charset detector to be closed.
  *  @draft ICU 3.6
  */
U_DRAFT void U_EXPORT2
ucsdet_close(UCharSetDetector *csd);

/**
  * Set the input byte data whose charset is to detected.
  *
  * Ownership of the input  text byte array remains with the caller.
  * The input string must not be altered or deleted until the charset
  * detector is either closed or reset to refer to different input text.
  *
  * @param csd    the charset detector to be used.
  * @param textIn the input text of unknown encoding.   .
  * @param len    the length of the input text, or -1 if the text
  *               is NUL terminated.
  * @param status any error conditions are reported back in this variable.
  *
  * @draft ICU 3.6
  */
U_DRAFT void U_EXPORT2
ucsdet_setText(UCharSetDetector *csd, const char *textIn, int32_t len, UErrorCode *status);

/**
  * Set the input byte data whose charset is to be detected to be from the
  *  specified FILE stream.  The detection process will read sufficient
  *  data from the stream to do the detection;  the exact amount
  *  will depend on the characteristics of the data itself.
  *
  *  Ownership of the input FILE stream remains with the caller.
  *  The FILE must not be altered or closed until the charset detector
  *  is either closed or reset  to refer to different input text.
  *  In addition, if a UText or UFILE is requested for accessing the
  *  text, the application must leave the underlying FILE open and
  *  otherwise untouched while the data is being accessed.
  *
  * @param csd    the charset detector to be used.
  * @param textIn the input stream of unknown encoding.
  * @param status any error conditions are reported back in this variable.
  * @draft ICU 3.6
  */
U_DRAFT void U_EXPORT2
ucsdet_setFile(UCharSetDetector *csd, FILE *textIn, UErrorCode *status);

/*  Set the declared encoding for charset detection.
 *  The declared encoding of an input text is an encoding obtained
 *  by the user from an http header or xml declaration or similar source that
 *  can be provided as an additional hint to the charset detector.
 *  A match between a declared encoding and a possible detected encoding
 *  will raise the quality of that detected encoding by a small amount.
 * <p/>
 *  A declared encoding that is unknown to the detector service or
 *  is incompatible with the input data being analyzed will not be added
 *  to the list of possible encodings.
 *
 * @param csd       the charset detector to be used.
 * @param encoding  an encoding for the current data obtained from
 *                  a header or declaration or other source outside
 *                  of the byte data itself.
 * @draft ICU 3.6
 */
U_DRAFT void U_EXPORT2
ucsdet_setDeclaredEncoding(UCharSetDetector *csd, const char *encoding);


/*
 *  Return the number of charsets that appear to be compatible with
 *  the byte input data.   The various getter functions for obtaining
 *  information about matching charsets require an index ranging from
 *  zero to the value returned by this function-1.  The matches are
 *  sorted in order of decreasing quality, with index zero indicating
 *  the encoding with the best match.
 *
 *  @param csd     the charset detector to query
 *  @param status  Any error conditions are reported back in this variable.
 *  @return        The number of character sets that appear to be compatible
 *                 with the byte data.
 *
 *  @draft ICU 3.6
 */
U_DRAFT int32_t U_EXPORT2
ucsdet_getDetectedCount(UCharSetDetector *csd, UErrorCode *status);

/*
 *  Get the name of a charset that matches the input byte data.
 *
 *  The storage for the returned name string is owned by the
 *  charset detector, and will remain valid until it is closed.
 *
 *  The name returned is suitable for use with the ICU conversion APIs.
 *
 *  @param csd     The charset detector.
 *  @param index   Which of the possible matching charsets to return.
 *                 Specify zero for the charset with the best match.
 *  @param status  Any error conditions are reported back in this variable.
 *  @return        The name of the matching charset.
 *
 *  @draft ICU 3.6
 */
U_DRAFT const char * U_EXPORT2
ucsdet_getName(UCharSetDetector *csd, int32_t index, UErrorCode *status);

/*
 *  Get a confidence number for the quality of the match of the byte
 *  data with the charset.  Confidence numbers range from zero to 100.
 *
 *  The confidence values are somewhat arbitrary.  They define an
 *  an ordering within the results for any single detection operation
 *  but are not generally comparable between the results for different input.
 *
 *  A confidence value of ten does have a general meaning - it is used
 *  for charsets that can represent the input data, but for which there
 *  is no other indication that suggests that the charset is the correct one.
 *  Pure 7 bit ASCII data, for example, is compatible with a
 *  great many charsets, most of which will be appear as possible matches
 *  with a confidence of 10.
 *
 *  @param csd     The charset detector.
 *  @param index   Which of the possible charset matches to consider.
 *                 Specify zero for information on the best match.
 *  @param status  Any error conditions are reported back in this variable.
 *  @return        A confidence number for the charset match.
 *
 *  @draft ICU 3.6
 */
U_DRAFT int32_t U_EXPORT2
ucsdet_getConfidence(UCharSetDetector *csd, int32_t index, UErrorCode *status);

/*
 *  Get the ISO code for the language of the input data.
 *
 *  The Charset Detection service is intended primarily for detecting
 *  charsets, not language.  For some, but not all, charsets, a language is
 *  identified as a byproduct of the detection process, and that is what
 *  is returned by this function.
 *
 *  CAUTION:
 *    1.  Language information is not available for input data encoded in.
 *        all charsets. In particular, no language is identified
 *        for UTF-8 input data.
 *
 *    2.   Closely related languages may sometimes be confused.
 *
 *  If more accurate language detection is required, a linguistic
 *  analysis package should be used.
 *
 *  The storage for the returned name string is owned by the
 *  charset detector, and will remain valid until it is closed.
 *
 *  @param csd     The charset detector.
 *  @param index   Which of the possibly matching charsets to use.
 *                 Specify zero for the best match.
 *  @param status  Any error conditions are reported back in this variable.
 *  @return        The ISO code for the language of the input data.
 *
 *  @draft ICU 3.6
 */
U_DRAFT const char * U_EXPORT2
ucsdet_getLanguage(UCharSetDetector *csd, int32_t index, UErrorCode *status);

/**
  *  Open a read-only UFILE onto the byte input, allowing the input
  *  to be read as Unicode by the ICU IO package.
  *
  *  The returned UFILE is owned by the caller.  Subsequent reuse or
  *  closing of the charset detector will have no effect on UFILE.
  *
  *  Closing the UFILE is the responsibility of the caller.
  *
  *  If the byte data is coming from an input FILE, that FILE must
  *  support seek operations, and must be left open while the
  *  UFILE created by this function is in use.
  *
  * @param csd     The charset detector to be used.
  * @param index   Which of the possible matching encodings to use.  Specify
  *                zero for the best match.
  * @param status  Any error conditions are reported back in this variable.
  * @return        A UFILE through which the text can be accessed
  *
  * @draft ICU 3.6
  */
U_DRAFT  UFILE * U_EXPORT2
ucsdet_getUFILE(UCharSetDetector *csd, int32_t index, UErrorCode *status);

/**
  *  Get the entire input text as a UChar string, placing it into
  *  a caller-supplied buffer.
  *
  *  If the supplied buffer is smaller than the actual text, as much
  *  of it as will fit will be placed in the buffer.  A terminating
  *  NUL character will be appended to the buffer if space is available.
  *  Only complete code points will be placed in the buffer - the buffer
  *  will never end with the leading half of a surrogate pair.
  *
  *  The number of UChars actually placed in the buffer is returned.
  *  This is different from other ICU functions that fill a buffer in that
  *  the returned value is NOT the size that would be required to hold
  *  all available data when the buffer is to small.  The reason is that
  *  the size of the data from a file may be very large indeed.
  *
  *
  * @param csd     The charset detector to be used.
  * @param index   Which of the possible matching encodings to use.  Specify
  *                zero for the best match.
  * @param buf     A UChar buffer to be filled with the converted text data.
  * @param cap     The capacity of the buffer in UChars.
  * @param status  Any error conditions are reported back in this variable.
  * @return        The number of UChars placed in the buffer, not including
  *                the trailing NUL.
  *
  * @draft ICU 3.6
  */
U_DRAFT  int32_t U_EXPORT2
ucsdet_getUChars(UCharSetDetector *csd, int32_t index,
                 UChar *buf, int32_t cap, UErrorCode *status);



/**
  *  Open a read-only UText onto the byte input, allowing the input
  *  to be read as Unicode through the UText interface.
  *
  *  The returned UText is owned by the caller.  Subsequent reuse or
  *  closing of the charset detector will have no effect on it.
  *
  *  Closing the UText is the responsibility of the caller.
  *
  *  If the source of the byte data is an input stream that does not
  *  allow seek operations, reverse direction iteration and  random access to
  *  the text is not supported.
  *
  *
  * @param csd     The charset detector to be used.
  * @param index   Which of the possible matching encodings to use.  Specify
  *                zero for the best match.
  * @param status  Any error conditions are reported back in this variable.
  * @return        A UText through which the text data can be accessed.
  *
  * @draft ICU 3.6
  */
U_DRAFT  UText * U_EXPORT2
ucsdet_getUText(UCharSetDetector *csd, int32_t index, UErrorCode *status);

/**
  *  Get the total number of charsets that can be recognized by the
  *  charset detector implementation.
  *
  *  The state of the Charset detector that is passed in does not
  *  affect the result of this function, but requiring a valid, open
  *  charset detector as a parameter insures that the charset detection
  *  service has been safely initialized and that the required detection
  *  data is available.
  *
  *  @param csd a Charset detector.
  *  @param status  Any error conditions are reported back in this variable.
  *  @return the number of charsets that are known to the charset detector service.
  *  @draft ICU 3.6
  */
U_DRAFT  int32_t U_EXPORT2
getDetectableCharsetsCount(UCharSetDetector *csd, UErrorCode *status);

/**
  *  Get the name of the Nth charset that is detectable.  This function
  *  can return the names of all charsets that are known to the
  *  charset detector service.
  *
  *  The storage for the name string is owned by the charset detector
  *  and will remain valid until the detector is closed.  The caller
  *  must not modify or free the returned string.
  *
  *  The charset names are IANA standard names, and are suitable
  *  for use with the ICU conversion functions.
  *
  *  @param csd    A charset detector.  The set of detectable charsets
  *                is the same for all detectors.
  *  @param index  The index of the charset to be returned.  The value
  *                must be between zero and the number of detectable
  *                charsets - 1.
  *  @param status Any error conditions are reported back in this variable.
  *                The possible error conditions include U_INDEX_OUTOFBOUNDS_ERROR.
  *  @return       The name of the charset.
  *
  *  @draft ICU 3.6
  **/
U_DRAFT  const char * U_EXPORT2
ucsdet_getDetectableCharsetName(UCharSetDetector *csd, int32_t index, UErrorCode *status);

/**
  *  Test whether input filtering is enabled for this charset detector.
  *  Input filtering removes text that appears to be HTML or xml
  *  markup from the input before applying the code page detection
  *  heuristics.
  *
  *  @param csd  The charset detector to check.
  *  @return TRUE if filtering is enabled.
  *  @draft ICU 3.4
  */
U_DRAFT  UBool * U_EXPORT2
isInputFilterEnabled(UCharSetDetector *csd);


/**
  * Enable filtering of input text. If filtering is enabled,
  * text within angle brackets ("<" and ">") will be removed
  * before detection, which will remove most HTML or xml markup.
  *
  * @param filter <code>true</code> to enable input text filtering.
  * @return The previous setting.
  *
  * @draft ICU 3.6
  */
U_DRAFT  void U_EXPORT2
enableInputFilter(UCharSetDetector *csd, UBool filter);


#endif   /* U_HIDE_DRAFT_API */
#endif   /* CSDET_H */


