/*
*******************************************************************************
* Copyright (C) 1997-1999, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* File FORMAT.CPP
*
* Modification History:
*
*   Date        Name        Description
*   02/19/97    aliu        Converted from java.
*   03/17/97    clhuang     Implemented with new APIs.
*   03/27/97    helena      Updated to pass the simple test after code review.
*   07/20/98    stephen        Added explicit init values for Field/ParsePosition
********************************************************************************
*/
// *****************************************************************************
// This file was generated from the java source file Format.java
// *****************************************************************************
 
#include "format.h"
 
// *****************************************************************************
// class Format
// *****************************************************************************
 
// -------------------------------------
// default constructor

Format::Format()
{
}

// -------------------------------------

Format::~Format()
{
}

// -------------------------------------
// copy constructor

Format::Format(const Format& that)
{
}

// -------------------------------------
// assignment operator

Format&
Format::operator=(const Format& that)
{
    return *this;
}

// -------------------------------------
// Formats the obj and append the result in the buffer, toAppendTo.
// This calls the actual implementation in the concrete subclasses.
 
UnicodeString&
Format::format(const Formattable& obj, 
               UnicodeString& toAppendTo, 
               UErrorCode& status) const
{
    if (U_FAILURE(status)) return toAppendTo;

    // {sfb} should really be FieldPosition::DONT_CARE, not 0
    // leave at 0 for now, to keep in sync with Java
    FieldPosition pos(0);

    return format(obj, toAppendTo, pos, status);
}
  
// -------------------------------------
// Parses the source string and create the corresponding 
// result object.  Checks the parse position for errors.
 
void
Format::parseObject(const UnicodeString& source, 
                    Formattable& result, 
                    UErrorCode& status) const
{
    if (U_FAILURE(status)) return;

    ParsePosition parsePosition(0);
    parseObject(source, result, parsePosition);
    if (parsePosition.getIndex() == 0) {
        status = U_INVALID_FORMAT_ERROR;
    }
}
 
// -------------------------------------

bool_t
Format::operator==(const Format& that) const
{
    // Add this implementation to make linker happy.
    return TRUE;
}

//eof
