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
*  Draft version 28 Aug 2005
*
*  Questions and Issues
*
*  0   is (char *) or (UChar *) for the encoding name parameters?
*       o  char * is consistent with the converter API encoding name params.
*       o  char * causes awkwardness in 100% Unicode apps.
*          You would think that a Unicode library would use Unicode for
*          strings.
*
*
*  0  UText: With UFILE related functionality all being move into the realm of
*          ICU IO, does it still make sense to support UText directly from
*          the CharsetDetection API?  
*
*  0  The match type (BOM, encoding scheme, language type, etc.) from the
*          Java API is omitted for now.  It didn't get implemented in the
*          Java for ICU 3.4, and I don't see it as being very useful.
*
*
*   Changes
*
*   - removed all FILE and UFILE related functions.  Equivalent functionality
*     will be added to the icu io package.
*
*   - Changed UCharSetDetector to UCharsetDetector, to match the Java spelling.
*
*   - Add UCharsetMatch, an abstract type to represent a match.  More closely
*     follows the structure of the Java API.
*
*   - add ucsdet_detect(), ucsdet_detectAll(), remove ucsdet_getDetectedCount().
*
*   - removed the UText interface.  This only makes sense when the byte input is
*     coming in from a stream, a capability that is moving into icu io.
*
*/
#ifndef CSDET_H
#define CSDET_H

#include "unicode/utypes.h"

#ifndef U_HIDE_DRAFT_API


struct UCharsetDetector;
/**
  * Structure representing a charset detector
  * @draft ICU 3.6
  */
typedef struct UCharsetDetector UCharsetDetector;

struct UCharsetMatch;
/**
  *  Opaque structure representing a match that was identified
  *  from a charset detection operation.
  *  draft ICU 3.6
  */
typedef struct UCharsetMatch UCharsetMatch;

/**
  *  Open a charset detector.
  *
  *  @param status Any error conditions occurring during the open
  *                operation are reported back in this variable.
  *  @return the newly opened charset detector.
  *  @draft ICU 3.6
  */
U_DRAFT UCharsetDetector * U_EXPORT2
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
ucsdet_close(UCharsetDetector *csd);

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
ucsdet_setText(UCharsetDetector *csd, const char *textIn, int32_t len, UErrorCode *status);


/** Set the declared encoding for charset detection.
 *  The declared encoding of an input text is an encoding obtained
 *  by the user from an http header or xml declaration or similar source that
 *  can be provided as an additional hint to the charset detector.
 *
 *  How and whether the declared encoding will be used during the
 *  detection process is TBD.
 *
 * @param csd       the charset detector to be used.
 * @param encoding  an encoding for the current data obtained from
 *                  a header or declaration or other source outside
 *                  of the byte data itself.
 * @param status    any error conditions are reported back in this variable.
 *
 * @draft ICU 3.6
 */
U_DRAFT void U_EXPORT2
ucsdet_setDeclaredEncoding(UCharsetDetector *csd, const char *encoding, UErrorCode *status);


/**
  * Return the charset that best matches the supplied input data.
  * 
  * Note though, that because the detection 
  * only looks at the start of the input data,
  * there is a possibility that the returned charset will fail to handle
  * the full set of input data.
  * <p/>
  * The returned UCharsetMatch object is owned by the UCharsetDetector.
  * It will remain valid until the detector input is reset, or until
  * the detector is closed.
  * <p/>
  * The function will fail if
  *  <ul>
  *    <li>no charset appears to match the data.</li>
  *    <li>no input text has been provided</li>
  *  </ul>
  *
  * @param csd       the charset detector to be used.
  * @param status    any error conditions are reported back in this variable.
  * @return          a UCharsetMatch  representing the best matching charset.
  *
  * @draft ICU 3.6
  */
U_DRAFT UCharsetMatch * U_EXPORT2
ucsdet_detect(UCharsetDetector *csd, UErrorCode *status);
    

/**
  *  Find all charset matches that appear to be consistent with the input,
  *  filling in an array with the results.  The results are ordered with the
  *  best quality match first.
  *
  *  Note though, that because the detection 
  *  only looks at the start of the input data,
  *  there is a possibility that the returned charset will fail to handle
  *  the full set of input data.
  *  <p/>
  *  The returned UCharsetMatch objects are owned by the UCharsetDetector.
  *  They will remain valid until the detector input is reset, or until
  *  the detector is closed.
  * <p/>
  * Return an error if 
  *  <ul>
  *    <li>no charsets appear to match the input data.</li>
  *    <li>no input text has been provided</li>
  *  </ul>
  * 
  * @param csd     the charset detector to be used.
  * @param dest    an array to be filled in with UCharsetMatch *
  *                pointers for the detected charsets.
  * @param destCapacity  The capacity of the destination array -  the number
  *                of UCharsetMatch pointers it can hold.  If the
  *                capacity is zero, the array may be NULL.
  * @param status  any error conditions are reported back in this variable.
  * @return        The total number of matching charsets.  This will be the
  *                full number, even if capacity of the array is too small
  *                to contain all of the results.
  *
  * @draft ICU 3.4
  */
U_DRAFT int32_t U_EXPORT2
ucsdet_detectAll(UCharsetDetector *csd, UCharsetMatch *dest,
                      int32_t destCapacity, UErrorCode *status);



/**
 *  Get the name of the charset represented by a UCharsetMatch.
 *
 *  The storage for the returned name string is owned by the
 *  UCharsetMatch, and will remain valid while the UCharsetMatch
 *  is valid.
 *
 *  The name returned is suitable for use with the ICU conversion APIs.
 *
 *  @param csm     The charset match object.
 *  @param status  Any error conditions are reported back in this variable.
 *  @return        The name of the matching charset.
 *
 *  @draft ICU 3.6
 */
U_DRAFT const char * U_EXPORT2
ucsdet_getName(const UCharsetMatch *csm, UErrorCode *status);

/**
 *  Get a confidence number for the quality of the match of the byte
 *  data with the charset.  Confidence numbers range from zero to 100,
 *  with 100 representing complete confidence and zero representing
 *  no no confidence.
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
 *  @param csm     The charset match object.
 *  @param status  Any error conditions are reported back in this variable.
 *  @return        A confidence number for the charset match.
 *
 *  @draft ICU 3.6
 */
U_DRAFT int32_t U_EXPORT2
ucsdet_getConfidence(const UCharsetMatch *csm, UErrorCode *status);

/**
 *  Get the RFC 3066 code for the language of the input data.
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
 *    2.  Closely related languages may sometimes be confused.
 *
 *  If more accurate language detection is required, a linguistic
 *  analysis package should be used.
 *
 *  The storage for the returned name string is owned by the
 *  UCharsetMatch, and will remain valid while the UCharsetMatch
 *  is valid.
 *
 *  @param csm     The charset match object.
 *  @param status  Any error conditions are reported back in this variable.
 *  @return        The RFC 3066 code for the language of the input data.
 *
 *  @draft ICU 3.6
 */
U_DRAFT const char * U_EXPORT2
ucsdet_getLanguage(const UCharsetMatch *csm, UErrorCode *status);


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
  * @param csm     The charset match object.
  * @param buf     A UChar buffer to be filled with the converted text data.
  * @param cap     The capacity of the buffer in UChars.
  * @param status  Any error conditions are reported back in this variable.
  * @return        The number of UChars placed in the buffer, not including
  *                the trailing NUL.
  *
  * @draft ICU 3.6
  */
U_DRAFT  int32_t U_EXPORT2
ucsdet_getUChars(const UCharsetMatch *csm,
                 UChar *buf, int32_t cap, UErrorCode *status);



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
ucsdet_getDetectableCharsetsCount(const UCharsetDetector *csd, UErrorCode *status);

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
ucsdet_getDetectableCharsetName(const UCharsetDetector *csd, int32_t index, UErrorCode *status);

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
U_DRAFT  UBool U_EXPORT2
ucsdet_isInputFilterEnabled(const UCharsetDetector *csd);


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
U_DRAFT  UBool U_EXPORT2
ucsdet_enableInputFilter(UCharsetDetector *csd, UBool filter);


#endif   /* U_HIDE_DRAFT_API */
#endif   /* CSDET_H */


