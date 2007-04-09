/*
*******************************************************************************
* Copyright (C) 2007, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*/

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

//#define DEBUG_RELDTFMT

#include <stdio.h>
#include <stdlib.h>

#include "reldtfmt.h"
#include "unicode/msgfmt.h"

#include "gregoimp.h" // for CalendarData
#include "cmemory.h"

U_NAMESPACE_BEGIN


/**
 * An array of URelativeString structs is used to store the resource data loaded out of the bundle.
 */
struct URelativeString {
    int32_t offset;         /** offset of this item, such as, the relative date **/
    const UChar* string;    /** string, or NULL if not set **/
    int32_t len;            /** length of the string **/
};


UOBJECT_DEFINE_RTTI_IMPLEMENTATION(RelativeDateFormat)

RelativeDateFormat::RelativeDateFormat(const RelativeDateFormat& other) :
DateFormat(other),fCalData(NULL), fStrings(NULL), dates(NULL), datesLen(0), fCombinedFormat(NULL) {
    
    dateStyle = other.dateStyle;
    timeStyle = other.timeStyle;
    if(other.fDateFormat != NULL) {
        fDateFormat = (DateFormat*)other.fDateFormat->clone();
    } else {
        fDateFormat = NULL;
    }
    
    if(other.fTimeFormat != NULL) {
        fTimeFormat = (DateFormat*)other.fTimeFormat->clone();
    } else {
        fTimeFormat = NULL;
    }
}

RelativeDateFormat::RelativeDateFormat( UDateFormatStyle timeStyle, UDateFormatStyle dateStyle, const Locale& locale, UErrorCode& status)
 : dateStyle(dateStyle), timeStyle(timeStyle), fDateFormat(NULL), fCombinedFormat(NULL), fTimeFormat(NULL),
  locale(locale),fCalData(NULL), fStrings(NULL), dates(NULL), datesLen(0) {
    if(U_FAILURE(status) ) {
        return;
    }
    
    if(dateStyle != UDAT_NONE) {
        EStyle newStyle = (EStyle)(dateStyle & ~UDAT_RELATIVE);
        // Create a DateFormat in the non-relative style requested.
        fDateFormat = createDateInstance(newStyle, locale);
    }
    if(timeStyle != UDAT_NONE) {
        // don't support time style, for now
        status = U_ILLEGAL_ARGUMENT_ERROR;
        return;
    }
    
    // Initialize the parent fCalendar, so that parse() works correctly.
    initializeCalendar(NULL, locale, status);
}

RelativeDateFormat::~RelativeDateFormat() {
    delete fDateFormat;
    delete fTimeFormat;
    delete fCombinedFormat;
    uprv_free(dates);
// do NOT: delete fStrings - as they are loaded from mapped memory, all owned by fCalData.
    delete fCalData;
}


Format* RelativeDateFormat::clone(void) const {
    RelativeDateFormat *other = new RelativeDateFormat(*this);
    return other;
}

UBool RelativeDateFormat::operator==(const Format& other) const {
    if(DateFormat::operator==(other)) {
        // DateFormat::operator== guarantees following cast is safe
        RelativeDateFormat* that = (RelativeDateFormat*)&other;
        return (dateStyle==that->dateStyle   &&
                timeStyle==that->timeStyle   &&
                locale==that->locale);
    }
    return FALSE;
}

UnicodeString& RelativeDateFormat::format(  Calendar& cal,
                                UnicodeString& appendTo,
                                FieldPosition& pos) const {
                                
    UErrorCode status = U_ZERO_ERROR;
    
    // calculate the difference, in days, between 'cal' and now.
    int dayDiff = dayDifference(cal, status);

    // look up string
    int32_t len;
    const UChar *theString = getStringForDay(dayDiff, len, status);
    
    if(U_FAILURE(status) || (theString==NULL)) {
        // didn't find it. Fall through to the fDateFormat 
        if(fDateFormat != NULL) {
            return fDateFormat->format(cal,appendTo,pos);
        } else {
            return appendTo; // no op
        }
    } else {
        // found a relative string
        return appendTo.append(theString, len);
    }
}

void RelativeDateFormat::parse( const UnicodeString& text,
                    Calendar& cal,
                    ParsePosition& pos) const {

    // Can the fDateFormat parse it?
    if(fDateFormat != NULL) {
        ParsePosition aPos(pos);
        fDateFormat->parse(text,cal,aPos);
        if((aPos.getIndex() != pos.getIndex()) && 
            (aPos.getErrorIndex()==-1)) {
                pos=aPos; // copy the sub parse
                return; // parsed subfmt OK
        }
    }
    
    // Linear search the relative strings
    for(int n=0;n<datesLen;n++) {
        if(dates[n].string != NULL &&
            (0==text.compare(pos.getIndex(),
                         dates[n].len,
                         dates[n].string))) {
            UErrorCode status = U_ZERO_ERROR;
            
            // Set the calendar to now+offset
            cal.setTime(Calendar::getNow(),status);
            cal.add(UCAL_DATE,dates[n].offset, status);
            
            if(U_FAILURE(status)) { 
                // failure in setting calendar fields
                pos.setErrorIndex(pos.getIndex()+dates[n].len);
            } else {
                pos.setIndex(pos.getIndex()+dates[n].len);
            }
            return;
        }
    }
    
    // parse failed
}

UResourceBundle *RelativeDateFormat::getStrings(UErrorCode& status) const {
    if(fCalData == NULL) {
        // fCalData owns the subsequent strings
        ((RelativeDateFormat*)this)->fCalData = new CalendarData(locale, "gregorian", status);
    }
    
    if(fStrings == NULL) {
        // load the string object
        UResourceBundle *theStrings = fCalData->getByKey3("fields", "day", "relative", status);
        
        if (U_FAILURE(status)) {
            return NULL;
        }
        
        ((RelativeDateFormat*)this)->fStrings = theStrings; // cast away const
    }
    return fStrings;
}

const UChar *RelativeDateFormat::getStringForDay(int32_t day, int32_t &len, UErrorCode &status) const {
    if(U_FAILURE(status)) {
        return NULL;
    }
    
    if(dates == NULL) {
        loadDates(status);
        if(U_FAILURE(status)) {
            return NULL;
        }
    }
    
    // no strings.
    if(datesLen == 0) {
        return NULL;
    }
    
    // Is it outside the resource bundle's range?
    if(day < dayMin || day > dayMax) {
        return NULL; // don't have it.
    }
    
    // Linear search the held strings
    for(int n=0;n<datesLen;n++) {
        if(dates[n].offset == day) {
            len = dates[n].len;
            return dates[n].string;
        }
    }
    
    return NULL;  // not found.
}

void RelativeDateFormat::loadDates(UErrorCode &status) const {
    UResourceBundle *strings = getStrings(status);

    RelativeDateFormat *nonConstThis = ((RelativeDateFormat*)this); // cast away const.
    
    // set up min/max 
    nonConstThis->dayMin=-1;
    nonConstThis->dayMax=1;

    if(U_FAILURE(status)) {
        nonConstThis->datesLen=0;
        return;
    }

    nonConstThis->datesLen = ures_getSize(strings);
    nonConstThis->dates = (URelativeString*) uprv_malloc(sizeof(dates[0])*datesLen);

    // Load in each item into the array...
    int n = 0;

    UResourceBundle *subString = NULL;
    
    while(ures_hasNext(strings) && U_SUCCESS(status)) {  // iterate over items
        subString = ures_getNextResource(strings, subString, &status);
        
        if(U_FAILURE(status) || (subString==NULL)) break;
        
        // key = offset #
        const char *key = ures_getKey(subString);
        
        // load the string and length
        int32_t aLen;
        const UChar* aString = ures_getString(subString, &aLen, &status);
        
        if(U_FAILURE(status) || aString == NULL) break;

        // calculate the offset
        int32_t offset = atoi(key);
        
        // set min/max
        if(offset < dayMin) {
            nonConstThis->dayMin = offset;
        }
        if(offset > dayMax) {
            nonConstThis->dayMax = offset;
        }
        
        // copy the string pointer
        nonConstThis->dates[n].offset = offset;
        nonConstThis->dates[n].string = aString;
        nonConstThis->dates[n].len = aLen; 

        n++;
    }
    
    // the dates[] array could be sorted here, for direct access.
}


// this should to be in DateFormat, instead it was copied from SimpleDateFormat.

Calendar*
RelativeDateFormat::initializeCalendar(TimeZone* adoptZone, const Locale& locale, UErrorCode& status)
{
    if(!U_FAILURE(status)) {
        fCalendar = Calendar::createInstance(adoptZone?adoptZone:TimeZone::createDefault(), locale, status);
    }
    if (U_SUCCESS(status) && fCalendar == NULL) {
        status = U_MEMORY_ALLOCATION_ERROR;
    }
    return fCalendar;
}

int32_t RelativeDateFormat::dayDifference(Calendar &cal, UErrorCode &status) {
    if(U_FAILURE(status)) {
        return 0;
    }
    // TODO: Cache the nowCal to avoid heap allocs?
    Calendar *nowCal = cal.clone();
    nowCal->setTime(Calendar::getNow(), status);
    int32_t dayDiff = nowCal->fieldDifference(cal.getTime(status), Calendar::DATE, status);
    delete nowCal;
    return dayDiff;
}

U_NAMESPACE_END

#endif

