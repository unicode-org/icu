/*
 * Copyright (C) 1996-2008, International Business Machines Corporation and Others.
 * All rights reserved.
 */

/**
 * \file 
 * \brief C API: Boyer-Moore StringSearch prototype.
 * \internal
 */

#ifndef _BMS_H
#define _BMS_H

#include "unicode/utypes.h"
#include "unicode/ucol.h"

/**
 * A reference to the data used to compute skip distnaces for the
 * Boyer-Moore search algorithm.
 */
typedef void UCD;

/**
 * Open a <code>UCD</code> object.
 *
 * @param collator - the collator
 *
 * @return the <code>UCD</code> object. You must call
 *         <code>ucd_close</code> when you are done using the object.
 *
 * @internal ICU 4.0.1 technology preview
 */
U_CAPI UCD * U_EXPORT2
ucd_open(UCollator *coll);

/**
 * Release a <code>UCD</code> object.
 *
 * @param ucd - the object
 *
 * @internal ICU 4.0.1 technology preview
 */
U_CAPI void U_EXPORT2
ucd_close(UCD *ucd);

/**
 * Get the <code>UCollator</code> object used to create a <code>UCD</code> object.
 * The <code>UCollator</code> object returned may not be the exact
 * object that was used to create this object, but it will have the
 * same behavior.
 *
 * @param ucd - the <code>UCD</code> object
 *
 * @return the <code>UCollator</code> used to create the given
 *         <code>UCD</code> object.
 *
 * @internal ICU 4.0.1 technology preview
 */
U_CAPI UCollator * U_EXPORT2
ucd_getCollator(UCD *ucd);

/**
 * <code>UCD</code> objects are expensive to compute, and so
 * may be cached. This routine will free the cached objects and delete
 * the cache.
 *
 * WARNING: Don't call this until you are have called <code>close</code>
 * for each <code>UCD</code> object that you have used. also,
 * DO NOT call this if another thread may be calling <code>ucd_flushCache</code>
 * at the same time.
 *
 * @internal ICU 4.0.1 technology preview
 */
U_CAPI void U_EXPORT2
ucd_freeCache();

/**
 * <code>UCD</code> objects are expensive to compute, and so
 * may be cached. This routine will remove any unused <code>UCD</code>
 * objects from the cache.
 *
 * @internal 4.0.1 technology preview
 */
U_CAPI void U_EXPORT2
ucd_flushCache();

/**
 * BMS
 *
 * This object holds the information needed to do a Collation sensitive Boyer-Moore search. It encapulates
 * the "bad character" and "good suffix" tables, the Collator-based data needed to compute them, and a reference
 * to the text being searched.
 *
 * NOTE: This is a technology preview. The final version of this API may not bear any resenblence to this API.
 *
 * @internal ICU 4.0.1 technology preview
 */
struct BMS;
typedef struct BMS BMS;

/**
 * Construct a <code>MBS</code> object.
 *
 * @param ucd - A <code>UCD</code> object holding the Collator-sensitive data
 * @param pattern - the string for which to search
 * @param latternLength - the length of the string for which to search
 * @param target - the string in which to search
 * @param targetLength - the length of the string in which to search
 *
 * @return the <code>BMS</code> object.
 *
 * @internal ICU 4.0.1 technology preview
 */
U_CAPI BMS * U_EXPORT2
bms_open(UCD *ucd,
         const UChar *pattern, int32_t patternLength,
         const UChar *target,  int32_t targetLength);

/**
 * Close a <code>BMS</code> object and release all the
 * storage associated with it.
 *
 * @param bms - the <code>BMS</code> object to close.
 */
U_CAPI void U_EXPORT2
bms_close(BMS *bms);

/**
 * Test the pattern to see if it generates any CEs.
 *
 * @return <code>TRUE</code> if the pattern string did not generate any CEs
 *
 * @internal ICU 4.0.1 technology preview
 */
U_CAPI UBool U_EXPORT2
bms_empty(BMS *bms);

/**
 * Get the <code>UCD</code> object used to create
 * a given <code>BMS</code> object.
 *
 * @param bms - the <code>BMS</code> object
 *
 * @return - the <code>UCD</code> object used to create
 *           the given <code>BMS</code> object.
 *
 * @internal ICU 4.0.1 technology preview
 */
U_CAPI UCD * U_EXPORT2
bms_getData(BMS *bms);

/**
 * Search for the pattern string in the target string.
 *
 * @param offset - the offset in the target string at which to begin the search
 * @param start - will be set to the starting offset of the match, or -1 if there's no match
 * @param end - will be set to the ending offset of the match, or -1 if there's no match
 *
 * @return <code>TRUE</code> if the match succeeds, <code>FALSE</code> otherwise.
 *
 * @internal ICU 4.0.1 technology preview
 */
U_CAPI UBool U_EXPORT2
bms_search(BMS *bms, int32_t offset, int32_t *start, int32_t *end);

/**
 * Set the target string for the match.
 *
 * @param target - the new target string
 * @param targetLength - the length of the new target string
 *
 * @internal ICU 4.0.1 technology preview
 */
U_CAPI void U_EXPORT2
bms_setTargetString(BMS *bms, const UChar *target, int32_t targetLength);

#endif /* _BMS_H */
