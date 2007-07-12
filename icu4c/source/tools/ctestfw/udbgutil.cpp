/********************************************************************
 * COPYRIGHT:
 * Copyright (c) 2007, International Business Machines Corporation and
 * others. All Rights Reserved.
 ********************************************************************/

#include "unicode/udbgutil.h"



struct Field {
	const char *str;
	const char *fullStr;
	int32_t num;
};

#define DBG_ARRAY_COUNT(x) (sizeof(x)/sizeof(x[0]))


// The fields

#if !UCONFIG_NO_FORMATTING

#include "unicode/ucal.h"
// Calendar


// 'UCAL_' = 5
#define FIELD_NAME_STR(x)  {#x+5, #x, x }

static const int32_t count_UCalendarDateFields = UCAL_FIELD_COUNT;

static const Field names_UCalendarDateFields[] = 
{
    FIELD_NAME_STR( UCAL_ERA ),
    FIELD_NAME_STR( UCAL_YEAR ),
    FIELD_NAME_STR( UCAL_MONTH ),
    FIELD_NAME_STR( UCAL_WEEK_OF_YEAR ),
    FIELD_NAME_STR( UCAL_WEEK_OF_MONTH ),
    FIELD_NAME_STR( UCAL_DATE ),
    FIELD_NAME_STR( UCAL_DAY_OF_YEAR ),
    FIELD_NAME_STR( UCAL_DAY_OF_WEEK ),
    FIELD_NAME_STR( UCAL_DAY_OF_WEEK_IN_MONTH ),
    FIELD_NAME_STR( UCAL_AM_PM ),
    FIELD_NAME_STR( UCAL_HOUR ),
    FIELD_NAME_STR( UCAL_HOUR_OF_DAY ),
    FIELD_NAME_STR( UCAL_MINUTE ),
    FIELD_NAME_STR( UCAL_SECOND ),
    FIELD_NAME_STR( UCAL_MILLISECOND ),
    FIELD_NAME_STR( UCAL_ZONE_OFFSET ),
    FIELD_NAME_STR( UCAL_DST_OFFSET ),
    FIELD_NAME_STR( UCAL_YEAR_WOY ),
    FIELD_NAME_STR( UCAL_DOW_LOCAL ),
    FIELD_NAME_STR( UCAL_EXTENDED_YEAR ),
    FIELD_NAME_STR( UCAL_JULIAN_DAY ),
    FIELD_NAME_STR( UCAL_MILLISECONDS_IN_DAY ),
};


static const int32_t count_UCalendarMonths = UCAL_UNDECIMBER+1;

static const Field names_UCalendarMonths[] = 
{
  FIELD_NAME_STR( UCAL_JANUARY ),
  FIELD_NAME_STR( UCAL_FEBRUARY ),
  FIELD_NAME_STR( UCAL_MARCH ),
  FIELD_NAME_STR( UCAL_APRIL ),
  FIELD_NAME_STR( UCAL_MAY ),
  FIELD_NAME_STR( UCAL_JUNE ),
  FIELD_NAME_STR( UCAL_JULY ),
  FIELD_NAME_STR( UCAL_AUGUST ),
  FIELD_NAME_STR( UCAL_SEPTEMBER ),
  FIELD_NAME_STR( UCAL_OCTOBER ),
  FIELD_NAME_STR( UCAL_NOVEMBER ),
  FIELD_NAME_STR( UCAL_DECEMBER ),
  FIELD_NAME_STR( UCAL_UNDECIMBER)
};


#undef FIELD_NAME_STR
#endif

#define FIELD_NAME_STR(x)  {#x+5, #x, x }

static const int32_t count_UDebugEnumType = UDBG_ENUM_COUNT;

static const Field names_UDebugEnumType[] = 
{
    FIELD_NAME_STR( UDBG_UDebugEnumType ),
    FIELD_NAME_STR( UDBG_UCalendarDateFields ),
    FIELD_NAME_STR( UDBG_UCalendarMonths ),
};
#undef FIELD_NAME_STR


#define COUNT_CASE(x)  case UDBG_##x: return (actual?count_##x:DBG_ARRAY_COUNT(names_##x));
#define COUNT_FAIL_CASE(x) case UDBG_##x: return -1;

#define FIELD_CASE(x)  case UDBG_##x: return names_##x;
#define FIELD_FAIL_CASE(x) case UDBG_##x: return NULL;

// low level

/**
 * @param type type of item
 * @param actual TRUE: for the actual enum's type (UCAL_FIELD_COUNT, etc), or FALSE for the string count
 */
static int32_t _udbg_enumCount(UDebugEnumType type, UBool actual) {
	switch(type) {
		COUNT_CASE(UDebugEnumType)
		COUNT_CASE(UCalendarDateFields)
		COUNT_CASE(UCalendarMonths)
		// COUNT_FAIL_CASE(UNonExistentEnum)
	default:
		return -1;
	}
}

static const Field* _udbg_enumFields(UDebugEnumType type) {
	switch(type) {
		FIELD_CASE(UDebugEnumType)
		FIELD_CASE(UCalendarDateFields)
		FIELD_CASE(UCalendarMonths)
		// FIELD_FAIL_CASE(UNonExistentEnum)
	default:
		return NULL;
	}
}

// implementation

int32_t  udbg_enumCount(UDebugEnumType type) {
	return _udbg_enumCount(type, FALSE);
}

int32_t  udbg_enumExpectedCount(UDebugEnumType type) {
	return _udbg_enumCount(type, TRUE);
}

const char *  udbg_enumName(UDebugEnumType type, int32_t field) {
	if(field<0 || 
				field>=_udbg_enumCount(type,FALSE)) { // also will catch unsupported items
		return NULL;
	} else {
		const Field *fields = _udbg_enumFields(type);
		if(fields == NULL) {
			return NULL;
		} else {
			return fields[field].str;
		}
	}
}

int32_t  udbg_enumArrayValue(UDebugEnumType type, int32_t field) {
	if(field<0 || 
				field>=_udbg_enumCount(type,FALSE)) { // also will catch unsupported items
		return -1;
	} else {
		const Field *fields = _udbg_enumFields(type);
		if(fields == NULL) {
			return -1;
		} else {
			return fields[field].num;
		}
	}
}
