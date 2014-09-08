/*
 *******************************************************************************
 * Copyright (C) 2014, International Business Machines Corporation and
 * others. All Rights Reserved.
 *******************************************************************************
 */
package com.ibm.icu.text;

import com.ibm.icu.util.ULocale;

/**
 * The BreakIteratorFilter is used to modify the behavior of a BreakIterator
 *  by constructing a new BreakIterator which suppresses certain segment boundaries.
 *  See  http://www.unicode.org/reports/tr35/tr35-general.html#Segmentation_Exceptions .
 *  For example, a typical English Sentence Break Iterator would break on the space
 *  in the string "Mr. Smith" (resulting in two segments),
 *  but with "Mr." as an exception, a filtered break iterator
 *  would consider the string "Mr. Smith" to be a single segment.
 *
 * @author tomzhang
 * 
 * @internal ICU 54 technology preview
 * @deprecated This API might change or be removed in a future release.
 */
@Deprecated
public abstract class FilteredBreakIteratorBuilder {

    /**
     * Construct a FilteredBreakIteratorBuilder based on rules in a locale.
     * The rules are taken from CLDR exception data for the locale,
     * see http://www.unicode.org/reports/tr35/tr35-general.html#Segmentation_Exceptions
     * This is the equivalent of calling createInstance(UErrorCode&)
     * and then repeatedly calling addNoBreakAfter(...) with the contents
     * of the CLDR exception data.
     * @param where the locale.
     * @return the new builder
     * @internal ICU 54 technology preview
     * @deprecated This API might change or be removed in a future release.
     */
    @Deprecated
    public static FilteredBreakIteratorBuilder createInstance(ULocale where) {
        FilteredBreakIteratorBuilder ret = new SimpleFilteredBreakIteratorBuilder(where);
        return ret;
    }

    /**
     * Construct an empty FilteredBreakIteratorBuilder.
     * In this state, it will not suppress any segment boundaries.
     * @return the new builder
     * @internal ICU 54 technology preview
     * @deprecated This API might change or be removed in a future release.
     */
    @Deprecated
    public static FilteredBreakIteratorBuilder createInstance() {
        FilteredBreakIteratorBuilder ret = new SimpleFilteredBreakIteratorBuilder();
        return ret;
    }

    /**
     * Suppress a certain string from being the end of a segment.
     * For example, suppressing "Mr.", then segments ending in "Mr." will not be returned
     * by the iterator.
     * @param str the string to suppress, such as "Mr."
     * @return returns true if the string was not present and now added,
     * false if the call was a no-op because the string was already being suppressed.
     * @internal ICU 54 technology preview
     * @deprecated This API might change or be removed in a future release.
     */
    @Deprecated
    public abstract boolean suppressBreakAfter(String str);

    /**
     * Stop suppressing a certain string from being the end of the segment.
     * This function does not create any new segment boundaries, but only serves to un-do
     * the effect of earlier calls to suppressBreakAfter, or to un-do the effect of
     * locale data which may be suppressing certain strings.
     * @param str the str the string to unsuppress, such as "Mr."
     * @return returns true if the string was present and now removed,
     * false if the call was a no-op because the string was not being suppressed.
     * @internal ICU 54 technology preview
     * @deprecated This API might change or be removed in a future release.
     */
    @Deprecated
    public abstract boolean unsuppressBreakAfter(String str);

    /**
     * Wrap (adopt) an existing break iterator in a new filtered instance.
     * The resulting BreakIterator is owned by the caller.
     * The BreakIteratorFilter may be destroyed before the BreakIterator is destroyed.
     * Note that the adoptBreakIterator is adopted by the new BreakIterator
     * and should no longer be used by the caller.
     * The FilteredBreakIteratorBuilder may be reused.
     * @param adoptBreakIterator the break iterator to adopt
     * @return the new BreakIterator, owned by the caller.
     * @internal ICU 54 technology preview
     * @deprecated This API might change or be removed in a future release.
     */
    @Deprecated
    public abstract BreakIterator build(BreakIterator adoptBreakIterator);

    /**
     * For subclass use
     * @internal ICU 54 technology preview
     * @deprecated This API might change or be removed in a future release.
     */
    @Deprecated
    protected FilteredBreakIteratorBuilder() {}
}