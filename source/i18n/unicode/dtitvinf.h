/*
 *******************************************************************************
 * Copyright (C) 2008, International Business Machines Corporation and
 * others. All Rights Reserved.
 *******************************************************************************
 *
 * File DTITVINF.H
 *
 *******************************************************************************
 */

#ifndef __DTITVINF_H__
#define __DTITVINF_H__

/**
 * \file
 * \brief C++ API: Date/Time interval patterns for formatting date/time interval
 */

#if !UCONFIG_NO_FORMATTING

#include "hash.h"
#include "gregoimp.h"
#include "uresimp.h"
#include "unicode/utypes.h"
#include "unicode/udat.h"
#include "unicode/locid.h"
#include "unicode/ucal.h"
#include "unicode/dtptngen.h"
//#include "dtitv_impl.h"



U_NAMESPACE_BEGIN



/**
 * DateIntervalInfo is a public class for encapsulating localizable
 * date time interval patterns. It is used by DateIntervalFormat.
 *
 * <P>
 * Logically, the interval patterns are mappings
 * from (skeleton, the_largest_different_calendar_field)
 * to (date_interval_pattern).
 *
 * <P>
 * A skeleton 
 * <ol>
 * <li>
 * only keeps the field pattern letter and ignores all other parts 
 * in a pattern, such as space, punctuations, and string literals.
 * <li>
 * hides the order of fields. 
 * <li>
 * might hide a field's pattern letter length.
 *
 * For those non-digit calendar fields, the pattern letter length is 
 * important, such as MMM, MMMM, and MMMMM; EEE and EEEE, 
 * and the field's pattern letter length is honored.
 *    
 * For the digit calendar fields,  such as M or MM, d or dd, yy or yyyy, 
 * the field pattern length is ignored and the best match, which is defined 
 * in date time patterns, will be returned without honor the field pattern
 * letter length in skeleton.
 * </ol>
 *
 * <P>
 * There is a set of pre-defined static skeleton strings.
 * The skeletons defined consist of the desired calendar field set 
 * (for example,  DAY, MONTH, YEAR) and the format length (long, medium, short)
 * used in date time patterns.
 * 
 * For example, skeleton YEAR_MONTH_MEDIUM_FORMAT consists month and year,
 * and it's corresponding full pattern is medium format date pattern.
 * So, the skeleton is "yMMM", for English, the full pattern is "MMM yyyy", 
 * which is the format by removing DATE from medium date format.
 *
 * For example, skeleton YEAR_MONTH_DOW_DAY_MEDIUM_FORMAT consists day, month,
 * year, and day-of-week, and it's corresponding full pattern is the medium
 * format date pattern. So, the skeleton is "yMMMEEEd", for English,
 * the full pattern is "EEE, MMM d, yyyy", which is the medium date format
 * plus day-of-week.
 *
 * <P>
 * The calendar fields we support for interval formatting are:
 * year, month, date, day-of-week, am-pm, hour, hour-of-day, and minute.
 * Those calendar fields can be defined in the following order:
 * year >  month > date > am-pm > hour >  minute 
 *  
 * The largest different calendar fields between 2 calendars is the
 * first different calendar field in above order.
 *
 * For example: the largest different calendar fields between "Jan 10, 2007" 
 * and "Feb 20, 2008" is year.
 *   
 * <P>
 * There are pre-defined interval patterns for those pre-defined skeletons
 * in locales' resource files.
 * For example, for a skeleton YEAR_MONTH_DAY_MEDIUM_FORMAT, which is  "yMMMd",
 * in  en_US, if the largest different calendar field between date1 and date2 
 * is "year", the date interval pattern  is "MMM d, yyyy - MMM d, yyyy", 
 * such as "Jan 10, 2007 - Jan 10, 2008".
 * If the largest different calendar field between date1 and date2 is "month",
 * the date interval pattern is "MMM d - MMM d, yyyy",
 * such as "Jan 10 - Feb 10, 2007".
 * If the largest different calendar field between date1 and date2 is "day",
 * the date interval pattern is ""MMM d-d, yyyy", such as "Jan 10-20, 2007".
 *
 * For date skeleton, the interval patterns when year, or month, or date is 
 * different are defined in resource files.
 * For time skeleton, the interval patterns when am/pm, or hour, or minute is
 * different are defined in resource files.
 *
 *
 * <P>
 * There are 2 dates in interval pattern. For most locales, the first date
 * in an interval pattern is the earlier date. There might be a locale in which
 * the first date in an interval pattern is the later date.
 * We use fallback format for the default order for the locale.
 * For example, if the fallback format is "{0} - {1}", it means
 * the first date in the interval pattern for this locale is earlier date.
 * If the fallback format is "{1} - {0}", it means the first date is the 
 * later date.
 * For a paticular interval pattern, the default order can be overriden
 * by prefixing "latestFirst:" or "earliestFirst:" to the interval pattern.
 * For example, if the fallback format is "{0}-{1}",
 * but for skeleton "yMMMd", the interval pattern when day is different is 
 * "latestFirst:d-d MMM yy", it means by default, the first date in interval
 * pattern is the earlier date. But for skeleton "yMMMd", when day is different,
 * the first date in "d-d MMM yy" is the later date.
 * 
 * <P>
 * The recommended way to create a DateIntervalFormat object is to pass in 
 * the locale. 
 * By using a Locale parameter, the DateIntervalFormat object is 
 * initialized with the pre-defined interval patterns for a given or 
 * default locale.
 * <P>
 * Users can also create DateIntervalFormat object 
 * by supplying their own interval patterns.
 * It provides flexibility for powerful usage.
 *
 * <P>
 * After a DateIntervalInfo object is created, clients may modify
 * the interval patterns using setIntervalPattern function as so desired.
 * Currently, users can only set interval patterns when the following 
 * calendar fields are different: ERA, YEAR, MONTH, DATE,  DAY_OF_MONTH, 
 * DAY_OF_WEEK, AM_PM,  HOUR, HOUR_OF_DAY, and MINUTE.
 * Interval patterns when other calendar fields are different is not supported.
 * <P>
 * DateIntervalInfo objects are clonable. 
 * When clients obtain a DateIntervalInfo object, 
 * they can feel free to modify it as necessary.
 * <P>
 * DateIntervalInfo are not expected to be subclassed. 
 * Data for a calendar is loaded out of resource bundles. 
 * To ICU 4.0, date interval patterns are only supported in Gregorian calendar. 
 * @draft ICU 4.0
**/

class U_I18N_API DateIntervalInfo : public UObject {
public:
    /**
     * Default constructor.
     * It does not initialize any interval patterns.
     * It should be followed by setFallbackIntervalPattern() and 
     * setIntervalPattern(), 
     * and is recommended to be used only for powerful users who
     * wants to create their own interval patterns and use them to create
     * date interval formatter.
     * @param status   output param set to success/failure code on exit
     * @internal ICU 4.0
     */
    DateIntervalInfo(UErrorCode& status);


    /** 
     * Construct DateIntervalInfo for the given locale,
     * @param locale  the interval patterns are loaded from the Gregorian 
     *                calendar data in this locale.
     * @param status  output param set to success/failure code on exit
     * @draft ICU 4.0
     */
    DateIntervalInfo(const Locale& locale, UErrorCode& status);


    /**
     * Copy constructor.
     * @draft ICU 4.0
     */
    DateIntervalInfo(const DateIntervalInfo&);

    /**
     * Assignment operator
     * @draft ICU 4.0
     */
    DateIntervalInfo& operator=(const DateIntervalInfo&);

    /**
     * Clone this object polymorphically.
     * The caller owns the result and should delete it when done.
     * @return   a copy of the object
     * @draft    ICU4.0
     */
    DateIntervalInfo* clone(void) const;

    /**
     * Destructor.
     * It is virtual to be safe, but it is not designed to be subclassed.
     * @draft ICU 4.0
     */
    virtual ~DateIntervalInfo();


    /**
     * Return true if another object is semantically equal to this one.
     *
     * @param other    the DateIntervalInfo object to be compared with.
     * @return         true if other is semantically equal to this.
     * @stable ICU 4.0
     */
    UBool operator==(const DateIntervalInfo& other) const;

    /**
     * Return true if another object is semantically unequal to this one.
     *
     * @param other    the DateIntervalInfo object to be compared with.
     * @return         true if other is semantically unequal to this.
     * @stable ICU 4.0
     */
    UBool operator!=(const DateIntervalInfo& other) const;



    /** 
     * Provides a way for client to build interval patterns.
     * User could construct DateIntervalInfo by providing 
     * a list of patterns.
     * <P>
     * For example:
     * <pre>
     * UErrorCode status = U_ZERO_ERROR;
     * DateIntervalInfo dIntervalInfo = new DateIntervalInfo();
     * dIntervalInfo->setIntervalPattern("yMd", UCAL_YEAR, "'from' yyyy-M-d 'to' yyyy-M-d", status); 
     * dIntervalInfo->setIntervalPattern("yMMMd", UCAL_MONTH, "'from' yyyy MMM d 'to' MMM d", status);
     * dIntervalInfo->setIntervalPattern("yMMMd", UCAL_DAY, "yyyy MMM d-d", status, status);
     * dIntervalInfo->setFallbackIntervalPattern("{0} ~ {1}");
     * </pre>
     *
     * Restriction: 
     * Currently, users can only set interval patterns when the following 
     * calendar fields are different: ERA, YEAR, MONTH, DATE,  DAY_OF_MONTH, 
     * DAY_OF_WEEK, AM_PM,  HOUR, HOUR_OF_DAY, and MINUTE.
     * Interval patterns when other calendar fields are different are 
     * not supported.
     *
     * @param skeleton         the skeleton on which interval pattern based
     * @param lrgDiffCalUnit   the largest different calendar unit.
     * @param intervalPattern  the interval pattern on the largest different
     *                         calendar unit.
     *                         For example, if lrgDiffCalUnit is 
     *                         "year", the interval pattern for en_US when year
     *                         is different could be "'from' yyyy 'to' yyyy".
     * @param status           output param set to success/failure code on exit
     * @draft ICU 4.0
     */
    void setIntervalPattern(const UnicodeString& skeleton, 
                            UCalendarDateFields lrgDiffCalUnit, 
                            const UnicodeString& intervalPattern,
                            UErrorCode& status);

    /**
     * Get the interval pattern given the largest different calendar field.
     * @param skeleton   the skeleton
     * @param field      the largest different calendar field
     * @param status     output param set to success/failure code on exit
     * @return interval pattern
     * @draft ICU 4.0 
     */
    const UnicodeString* getIntervalPattern(const UnicodeString& skeleton,
                                            UCalendarDateFields field,
                                            UErrorCode& status) const; 

    /**
     * Get the fallback interval pattern.
     * @return fallback interval pattern
     * @draft ICU 4.0 
     */
    const UnicodeString& getFallbackIntervalPattern() const;


    /**
     * Set the fallback interval pattern.
     * Fall-back interval pattern is get from locale resource.
     * If a user want to set their own fall-back interval pattern,
     * they can do so by calling the following method.
     * For users who construct DateIntervalInfo() by default constructor,
     * all interval patterns ( including fall-back ) are not set,
     * those users need to call setIntervalPattern() to set their own
     * interval patterns, and call setFallbackIntervalPattern() to set
     * their own fall-back interval patterns. If a certain interval pattern
     * ( for example, the interval pattern when 'year' is different ) is not
     * found, fall-back pattern will be used. 
     * For those users who set all their patterns ( instead of calling 
     * non-defaul constructor to let constructor get those patterns from 
     * locale ), if they do not set the fall-back interval pattern, 
     * it will be fall-back to '{date0} - {date1}'.
     *
     * @param fallbackPattern   fall-back interval pattern.
     * @draft ICU 4.0 
     */
    void setFallbackIntervalPattern(const UnicodeString& fallbackPattern);


    /* Get default order
     * return default date ordering in interval pattern
     * @draft ICU 4.0 
     */
    UBool getDefaultOrder() const;


    /**
     * ICU "poor man's RTTI", returns a UClassID for the actual class.
     *
     * @stable ICU 4.0
     */
    virtual UClassID getDynamicClassID() const;

    /**
     * ICU "poor man's RTTI", returns a UClassID for this class.
     *
     * @stable ICU 4.0
     */
    static UClassID U_EXPORT2 getStaticClassID();


private:
    /**
     * DateIntervalFormat will need access to
     * getBestSkeleton(), parseSkeleton(), enum IntervalPatternIndex,
     * and calendarFieldToPatternIndex().
     *
     * Instead of making above public,
     * make DateIntervalFormat a friend of DateIntervalInfo.
     */
    friend class DateIntervalFormat;

    /**
     * Following is for saving the interval patterns.
     * We only support interval patterns on
     * ERA, YEAR, MONTH, DAY, AM_PM, HOUR, and MINUTE
     */
    enum IntervalPatternIndex
    {
        kIPI_ERA,
        kIPI_YEAR,
        kIPI_MONTH,
        kIPI_DATE,
        kIPI_AM_PM,
        kIPI_HOUR,
        kIPI_MINUTE,
        kIPI_MAX_INDEX
    };

    /** 
     * Initialize the DateIntervalInfo from locale
     * @param locale   the given locale.
     * @param status   output param set to success/failure code on exit
     * @draft ICU 4.0 
     */
    void initializeData(const Locale& locale, UErrorCode& status);


    /* Set Interval pattern.
     *
     * It sets interval pattern into the hash map.
     *
     * @param skeleton         skeleton on which the interval pattern based
     * @param lrgDiffCalUnit   the largest different calendar unit.
     * @param intervalPattern  the interval pattern on the largest different
     *                         calendar unit.
     * @param status           output param set to success/failure code on exit
     * @draft ICU 4.0
     */
    void setIntervalPatternInternally(const UnicodeString& skeleton,
                                      UCalendarDateFields lrgDiffCalUnit,
                                      const UnicodeString& intervalPattern,
                                      UErrorCode& status); 


    /**given an input skeleton, get the best match skeleton 
     * which has pre-defined interval pattern in resource file.
     * Also return the difference between the input skeleton
     * and the best match skeleton.
     *
     * TODO (xji): set field weight or
     *             isolate the funtionality in DateTimePatternGenerator
     * @param  skeleton               input skeleton
     * @param  bestMatchDistanceInfo  the difference between input skeleton
     *                                and best match skeleton.
     *         0, if there is exact match for input skeleton
     *         1, if there is only field width difference between 
     *            the best match and the input skeleton
     *         2, the only field difference is 'v' and 'z'
     *        -1, if there is calendar field difference between
     *            the best match and the input skeleton
     * @return                        best match skeleton
     * @draft ICU 4.0
     */
    const UnicodeString* getBestSkeleton(const UnicodeString& skeleton,
                                         int8_t& bestMatchDistanceInfo) const;


    /**
     * Parse skeleton, save each field's width.
     * It is used for looking for best match skeleton,
     * and adjust pattern field width.
     * @param skeleton            skeleton to be parsed
     * @param skeletonFieldWidth  parsed skeleton field width
     * @draft ICU 4.0
     */
    static void U_EXPORT2 parseSkeleton(const UnicodeString& skeleton, 
                                        int32_t* skeletonFieldWidth);


    /**
     * Check whether one field width is numeric while the other is string.
     *
     * TODO (xji): make it general
     *
     * @param fieldWidth          one field width
     * @param anotherFieldWidth   another field width
     * @param patternLetter       pattern letter char
     * @return true if one field width is numeric and the other is string,
     *         false otherwise.
     * @draft ICU 4.0
     */
    static UBool U_EXPORT2 stringNumeric(int32_t fieldWidth,
                                         int32_t anotherFieldWidth,
                                         char patternLetter);


    /** 
     * Convert calendar field to the interval pattern index in 
     * hash table.
     *
     * Since we only support the following calendar fields: 
     * ERA, YEAR, MONTH, DATE,  DAY_OF_MONTH, DAY_OF_WEEK, 
     * AM_PM,  HOUR, HOUR_OF_DAY, and MINUTE,
     * We reserve only 4 interval patterns for a skeleton.
     *
     * @param field    calendar field
     * @param status   output param set to success/failure code on exit
     * @return  interval pattern index in hash table
     * @draft ICU 4.0
     */
    static IntervalPatternIndex U_EXPORT2 calendarFieldToIntervalIndex(
                                                      UCalendarDateFields field,
                                                      UErrorCode& status);


    /**
     * delete hash table (of type fIntervalPatterns).
     *
     * @param hTable  hash table to be deleted
     * @draft ICU 4.0
     */
    void deleteHash(Hashtable* hTable);


    /**
     * initialize hash table (of type fIntervalPatterns).
     *
     * @param status   output param set to success/failure code on exit
     * @return         hash table initialized
     * @draft ICU 4.0
     */
    Hashtable* initHash(UErrorCode& status);



    /**
     * copy hash table (of type fIntervalPatterns).
     *
     * @param source   the source to copy from
     * @param target   the target to copy to
     * @param status   output param set to success/failure code on exit
     * @draft ICU 4.0
     */
    void copyHash(const Hashtable* source, Hashtable* target, UErrorCode& status);



    /**
     * set hash table value comparator
     *
     * @param val1  one value in comparison
     * @param val2  the other value in comparison
     * @return      TRUE if 2 values are the same, FALSE otherwise
     */
    static UBool U_EXPORT2 hashTableValueComparator(UHashTok val1, UHashTok val2);


    // data members
    // fallback interval pattern 
    UnicodeString fFallbackIntervalPattern;
    // default order
    UBool fFirstDateInPtnIsLaterDate;

    // HashMap<UnicodeString, UnicodeString[kIPI_MAX_INDEX]>
    // HashMap( skeleton, pattern[largest_different_field] )
    Hashtable* fIntervalPatterns;

};// end class DateIntervalInfo


inline UBool
DateIntervalInfo::operator!=(const DateIntervalInfo& other) const {
    return !operator==(other);
}


inline UBool
DateIntervalInfo::getDefaultOrder() const {
    return fFirstDateInPtnIsLaterDate;
}


inline const UnicodeString&
DateIntervalInfo::getFallbackIntervalPattern() const {
    return fFallbackIntervalPattern;
}


U_NAMESPACE_END

#endif

#endif

