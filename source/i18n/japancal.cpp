/*
*******************************************************************************
* Copyright (C) 2003, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* File JAPANCAL.CPP
*
* Modification History:
*  05/16/2003    srl     copied from buddhcal.cpp
*
*/

#include "unicode/utypes.h"

#if !UCONFIG_NO_FORMATTING

#include "japancal.h"
#include "unicode/gregocal.h"


U_NAMESPACE_BEGIN


const char JapaneseCalendar::fgClassID = 0; // Value is irrelevant


JapaneseCalendar::JapaneseCalendar(const Locale& aLocale, UErrorCode& success)
  :   GregorianCalendar(aLocale, success)
{
  success = U_UNSUPPORTED_ERROR;  // Just a stub for now.
}

JapaneseCalendar::~JapaneseCalendar()
{
}

JapaneseCalendar::JapaneseCalendar(const JapaneseCalendar& source)
  : GregorianCalendar(source)
{
}

JapaneseCalendar& JapaneseCalendar::operator= ( const JapaneseCalendar& right)
{
  GregorianCalendar::operator=(right);
  return *this;
}

Calendar* JapaneseCalendar::clone(void) const
{
  return new JapaneseCalendar(*this);
}

const char *JapaneseCalendar::getType() const
{
  return "japanese";
}




U_NAMESPACE_END

#endif
