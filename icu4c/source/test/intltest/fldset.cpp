/*
************************************************************************
* Copyright (c) 2007, International Business Machines
* Corporation and others.  All Rights Reserved.
************************************************************************
*/

#include "fldset.h"
#include <stdio.h>
#include "unicode/regex.h"


FieldsSet::FieldsSet() {
    // NOTREACHED
}

FieldsSet::FieldsSet(int32_t fieldCount) {
    construct((UDebugEnumType)-1, fieldCount);
}

FieldsSet::FieldsSet(UDebugEnumType field) {
    construct(field, udbg_enumCount(field));
}

FieldsSet::~FieldsSet() {
    
}

int32_t FieldsSet::fieldCount() const {
    return fFieldCount;
}

void FieldsSet::construct(UDebugEnumType field, int32_t fieldCount) {
    fEnum = field;
    if(fieldCount > U_FIELDS_SET_MAX) {
        fieldCount = U_FIELDS_SET_MAX;
    }
    fFieldCount = fieldCount;
    clear();
}

UnicodeString FieldsSet::diffFrom(const FieldsSet& other, UErrorCode& status) const {
    UnicodeString str;
    if(!isSameType(other)) {
        status = U_ILLEGAL_ARGUMENT_ERROR;
        return UnicodeString("U_ILLEGAL_ARGUMENT_ERROR: FieldsSet of a different type!");
    }
    for (int i=0; i<fieldCount(); i++) {
        if (isSet((UCalendarDateFields)i)) {
            int32_t myVal = get(i);
            int32_t theirVal = other.get(i);
            
            if(fEnum != -1) {
                const UnicodeString& fieldName = udbg_enumString(
                        fEnum, i);
                
                char aval[200];
                char bval[200];
                sprintf(aval,"%d",myVal);
                sprintf(bval,"%d",theirVal);
    
                str = str + fieldName +"="+aval+" not "+bval+", ";
            } else {
                str = str + UnicodeString("some field") + "=" + myVal+" not " + theirVal+", ";
            }
        }
    }
    return str;
}

int32_t FieldsSet::parseFrom(const UnicodeString& str, const 
        FieldsSet* inheritFrom, UErrorCode& status) {

    int goodFields = 0;
    
    UnicodeString pattern(",", "");
    RegexMatcher matcher(pattern, 0, status);
    UnicodeString pattern2("=", "");
    RegexMatcher matcher2(pattern2, 0, status);
    if (U_FAILURE(status))
        return -1;

    UnicodeString dest[U_FIELDS_SET_MAX+2]; // TODO: dynamicize
    int32_t destCount = matcher.split(str, dest, sizeof(dest)/sizeof(dest[0]), status);
    if(U_FAILURE(status)) return -1;
    for(int i=0;i<destCount;i++) {
        UnicodeString kv[2];
        matcher2.split(dest[i],kv,2,status);
        if(U_FAILURE(status)) {
            fprintf(stderr, "Parse failed: splitting\n");
            return -1;
        }

        int32_t field = handleParseName(inheritFrom, kv[0], kv[1], status);

        if(U_FAILURE(status)) {
            char ch[256];
            const UChar *u = kv[0].getBuffer();
            int32_t len = kv[0].length();
            u_UCharsToChars(u, ch, len);
            ch[len] = 0; /* include terminating \0 */
            fprintf(stderr,"Parse Failed: Field %s, err %s\n", ch, u_errorName(status));
            return -1;
        }

        if(field != -1) {
            handleParseValue(inheritFrom, field, kv[1], status);
            if(U_FAILURE(status)) {
                char ch[256];
                const UChar *u = kv[1].getBuffer();
                int32_t len = kv[1].length();
                u_UCharsToChars(u, ch, len);
                ch[len] = 0; /* include terminating \0 */
                fprintf(stderr,"Parse Failed: Value %s, err %s\n", ch, u_errorName(status));
                return -1;
            }
            goodFields++;
        }
    }

    return goodFields;
}

UBool FieldsSet::isSameType(const FieldsSet& other) const {
    return((&other==this)||
           ((other.fFieldCount==fFieldCount) && (other.fEnum==fEnum)));  
}

void FieldsSet::clear() {
    for (int i=0; i<fieldCount(); i++) {
        fValue[i]=-1;
        fIsSet[i]=FALSE;
    }
}

void FieldsSet::clear(int32_t field) {
    if (field<0|| field>=fieldCount()) {
        return;
    }
    fValue[field] = -1;
    fIsSet[field] = FALSE;
}
void FieldsSet::set(int32_t field, int32_t amount) {
    if (field<0|| field>=fieldCount()) {
        return;
    }
    fValue[field] = amount;
    fIsSet[field] = TRUE;
}

UBool FieldsSet::isSet(int32_t field) const {
    if (field<0|| field>=fieldCount()) {
        return FALSE;
    }
    return fIsSet[field];
}
int32_t FieldsSet::get(int32_t field) const {
    if (field<0|| field>=fieldCount()) {
        return -1;
    }
    return fValue[field];
}


int32_t FieldsSet::handleParseName(const FieldsSet* /* inheritFrom */, const UnicodeString& name, const UnicodeString& /* substr*/ , UErrorCode& status) {
    if(fEnum > -1) {
        return udbg_enumByString(fEnum, name);
    } else {
        status = U_UNSUPPORTED_ERROR;
        return -1;
    }
}

void FieldsSet::parseValueDefault(const FieldsSet* inheritFrom, int32_t field, const UnicodeString& substr, UErrorCode& status) {
    int32_t value = -1;
    if(substr.length()==0) { // inherit requested
        // inherit
        if((inheritFrom == NULL) || !inheritFrom->isSet((UCalendarDateFields)field)) {
            // couldn't inherit from field 
            fprintf(stderr,"Parse Failed: Couldn't inherit field %d [%s]\n", field, udbg_enumName(fEnum, field));
            status = U_ILLEGAL_ARGUMENT_ERROR;
            return;
        }
        value = inheritFrom->get((UCalendarDateFields)field);
    } else {        
        value = udbg_stoi(substr);
    }
    set(field, value);
}

void FieldsSet::parseValueEnum(UDebugEnumType type, const FieldsSet* inheritFrom, int32_t field, const UnicodeString& substr, UErrorCode& status) {
    int32_t value = udbg_enumByString(type, substr);
    if(value>=0) {
        set(field, value);
    } else {
        // fallback
        parseValueDefault(inheritFrom,field,substr,status);
    }
}

void FieldsSet::handleParseValue(const FieldsSet* inheritFrom, int32_t field, const UnicodeString& substr, UErrorCode& status) {
    parseValueDefault(inheritFrom, field, substr, status);
}

/// CAL FIELDS


CalendarFieldsSet::CalendarFieldsSet() :
FieldsSet(UDBG_UCalendarDateFields) {
    // base class will call clear.
}

CalendarFieldsSet::~CalendarFieldsSet() {
}

void CalendarFieldsSet::handleParseValue(const FieldsSet* inheritFrom, int32_t field, const UnicodeString& substr, UErrorCode& status) {
    if(field==UCAL_MONTH) {
        parseValueEnum(UDBG_UCalendarMonths, inheritFrom, field, substr, status);
        // will fallback to default.
    } else {
        parseValueDefault(inheritFrom, field, substr, status);
    }
}

/**
 * set the specified fields on this calendar. Doesn't clear first. Returns any errors the caller 
 */
void CalendarFieldsSet::setOnCalendar(Calendar *cal, UErrorCode& /*status*/) const {
    for (int i=0; i<UDAT_FIELD_COUNT; i++) {
        if (isSet((UCalendarDateFields)i)) {
            int32_t value = get((UCalendarDateFields)i);
            //fprintf(stderr, "Setting: %s#%d=%d\n",udbg_enumName(UDBG_UCalendarDateFields,i),i,value);            
            cal->set((UCalendarDateFields)i, value);
        }
    }
}

/**
 * return true if the calendar matches in these fields
 */
UBool CalendarFieldsSet::matches(Calendar *cal, CalendarFieldsSet &diffSet,
        UErrorCode& status) const {
    UBool match = TRUE;
    if (U_FAILURE(status))
        return FALSE;
    for (int i=0; i<UDAT_FIELD_COUNT; i++) {
        if (isSet((UCalendarDateFields)i)) {
            int32_t calVal = cal->get((UCalendarDateFields)i, status);
            if (U_FAILURE(status))
                return FALSE;
            if (calVal != get((UCalendarDateFields)i)) {
                match = FALSE;
                diffSet.set((UCalendarDateFields)i, calVal);
                //fprintf(stderr, "match failed: %s#%d=%d != %d\n",udbg_enumName(UDBG_UCalendarDateFields,i),i,cal->get((UCalendarDateFields)i,status), get((UCalendarDateFields)i));;
            }
        }
    }
    return match;
}


enum {
    DTS_DATE = 0,
    DTS_TIME,
    DTS_COUNT
};

/**
 * DateTimeSet 
 * */
DateTimeStyleSet::DateTimeStyleSet() :
    FieldsSet(DTS_COUNT) {
    
}

DateTimeStyleSet::~DateTimeStyleSet() {
    
}

UDateFormatStyle DateTimeStyleSet::getDateStyle() const {
    if(!isSet(DTS_DATE)) {
        return UDAT_NONE;
    } else {
        return (UDateFormatStyle)get(DTS_DATE);
    }
}


UDateFormatStyle DateTimeStyleSet::getTimeStyle() const {
    if(!isSet(DTS_TIME)) {
        return UDAT_NONE;
    } else {
        return (UDateFormatStyle)get(DTS_TIME);
    }
}

void DateTimeStyleSet::handleParseValue(const FieldsSet* inheritFrom, int32_t field, const UnicodeString& substr, UErrorCode& status) {
//    int32_t value = udbg_enumByString(UDBG_UDateFormatStyle, substr);
//    fprintf(stderr, " HPV: %d -> %d\n", field, value);
    parseValueEnum(UDBG_UDateFormatStyle, inheritFrom, field, substr, status);
}

int32_t DateTimeStyleSet::handleParseName(const FieldsSet* /* inheritFrom */, const UnicodeString& name, const UnicodeString& /* substr */, UErrorCode& status) {
    UnicodeString kDATE("DATE"); // TODO: static
    UnicodeString kTIME("TIME"); // TODO: static
    if(name == kDATE ) { 
        return DTS_DATE;
    } else if(name == kTIME) {
        return DTS_TIME;
    } else {
        status = U_ILLEGAL_ARGUMENT_ERROR;
        return -1;   
    }
}

