/*
******************************************************************************
*                                                                            *
* Copyright (C) 2003-2004, International Business Machines                        *
*                Corporation and others. All Rights Reserved.                *
*                                                                            *
******************************************************************************
*   file name:  ulocdata.h
*   encoding:   US-ASCII
*   tab size:   8 (not used)
*   indentation:4
*
*   created on: 2003Oct21
*   created by: Ram Viswanadha
*/

#ifndef __ULOCDATA_H__
#define __ULOCDATA_H__

#include "unicode/ures.h"
#include "unicode/uloc.h"
#include "unicode/uset.h"


/**
 * Returns the set of exemplar characters for a locale.
 *
 * @param fillIn    Pointer to a USet object to receive the 
 *                  exemplar character set for the given locale.  Previous
 *                  contents of fillIn are lost.  <em>If fillIn is NULL,
 *                  then a new USet is created and returned.  The caller
 *                  owns the result and must dispose of it by calling
 *                  uset_close.</em>
 * @param localeID  Locale id for which the exemplar character set
 *                  is to be retrieved.
 * @param options   Bitmask for options to apply to the exemplar pattern.
 *                  Specify zero to retrieve the exemplar set as it is
 *                  defined in the locale data.  Specify
 *                  USET_CASE_INSENSITIVE to retrieve a case-folded
 *                  exemplar set.  See uset_applyPattern for a complete
 *                  list of valid options.  The USET_IGNORE_SPACE bit is
 *                  always set, regardless of the value of 'options'.
 * @param status    Pointer to an input-output error code value;
 *                  must not be NULL.
 * @return USet*    Either fillIn, or if fillIn is NULL, a pointer to
 *                  a newly-allocated USet that the user must close.
 * @draft ICU 3.0
 */
U_DRAFT USet* U_EXPORT2 
ulocdata_getExemplarSet(USet *fillIn, const char *localeID,
                        uint32_t options, UErrorCode *status);

#ifndef U_HIDE_DRAFT_API
/**
 * Enumeration for representing the measurement systems.
 * @draft ICU 2.8
 */
typedef enum UMeasurementSystem {
    UMS_SI,     /** Measurement system specified by SI otherwise known as Metric system. */
    UMS_US,     /** Measurement system followed in the United States of America. */ 
    UMS_LIMIT
} UMeasurementSystem;
#endif /*U_HIDE_DRAFT_API */

/**
 * Returns the measurement system used in the locale specified by the localeID.
 *
 * @param localeID      The id of the locale for which the measurement system to be retrieved.
 * @param status        Must be a valid pointer to an error code value,
 *                      which must not indicate a failure before the function call.
 * @return UMeasurementSystem the measurement system used in the locale.
 * @draft ICU 2.8
 */
U_DRAFT UMeasurementSystem U_EXPORT2
ulocdata_getMeasurementSystem(const char *localeID, UErrorCode *status);

/**
 * Returns the element gives the normal business letter size, and customary units. 
 * The units for the numbers are always in <em>milli-meters</em>.
 * For US since 8.5 and 11 do not yeild an integral value when converted to milli-meters,
 * the values are rounded off.
 * So for A4 size paper the height and width are 297 mm and 210 mm repectively, 
 * and for US letter size the height and width are 279 mm and 216 mm respectively.
 *
 * @param localeID      The id of the locale for which the paper size information to be retrieved.
 * @param height        A pointer to int to recieve the height information.
 * @param width         A pointer to int to recieve the width information.
 * @param status        Must be a valid pointer to an error code value,
 *                      which must not indicate a failure before the function call.
 * @draft ICU 2.8
 */
U_DRAFT void U_EXPORT2
ulocdata_getPaperSize(const char *localeID, int32_t *height, int32_t *width, UErrorCode *status);

#endif
