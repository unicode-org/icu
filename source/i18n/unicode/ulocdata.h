/*
******************************************************************************
*                                                                            *
* Copyright (C) 2003, International Business Machines                        *
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
 * Fills the set with the set of exemplar characters for the locale and 
 * returns the set.
 *
 * @param fillIn    A pointer to USet object to be filled in with the 
 *                  exemplar characters set for the locale. 
 *                  <em> If NULL is passed, then a new USet will be created and returned.
 *                   The caller owns this object and must dispose it by calling uset_close.
 *                   </em>
 * @param localeID  The id of the locale for which the exemplar character set 
 *                  needs to be retrieved.
 * @param status    Must be a valid pointer to an error code value,
 *                  which must not indicate a failure before the function call.
 * @return USet*    The pointer to the fillIn USet object.
 * @draft ICU 2.8
 */
U_CAPI USet* U_EXPORT2 
ulocdata_getExemplarSet(USet *fillIn, const char *localeID, UErrorCode *status);


/**
 * Enumeration for representing the measurement systems.
 * @draft ICU 2.8
 */
typedef enum UMeasurementSystem {
    UMS_SI,     /** Measurement system specified by SI otherwise known as Metric system. */
    UMS_US,     /** Measurement system followed in the United States of America. */ 
    UMS_LIMIT
} UMeasurementSystem;

/**
 * Returns the measurement system used in the locale specified by the localeID.
 *
 * @param localeID      The id of the locale for which the measurement system to be retrieved.
 * @param status        Must be a valid pointer to an error code value,
 *                      which must not indicate a failure before the function call.
 * @return UMeasurementSystem the measurement system used in the locale.
 * @draft ICU 2.8
 */
U_CAPI UMeasurementSystem U_EXPORT2
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
U_CAPI void U_EXPORT2
ulocdata_getPaperSize(const char *localeID, int32_t *height, int32_t *width, UErrorCode *status);

#endif
