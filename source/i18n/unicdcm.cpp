/*
*****************************************************************************************
*   Copyright (C) 1997-1999, International Business Machines
*   Corporation and others.  All Rights Reserved.
*****************************************************************************************
*
* File UNICDCM.CPP
*
* Modification History:
*
*   Date        Name        Description
*   02/18/97    aliu        Converted from OpenClass.
*   03/11/97    aliu        Recoded mappedChar() slightly to clean up code and improve
*                           performance.
*   04/15/97    aliu        Worked around bug in AIX xlC compiler which occurs if static
*                           arrays contain const elements.
*   05/06/97    aliu        Made SpecialMapping an array of objects instead of pointers,
*                           to help out non-compliant compilers.
*****************************************************************************************
*/

// *****************************************************************************
// This file was generated from the java source file UnicodeClassMapping.java
// *****************************************************************************

#include "unicdcm.h"
#include "unicode/unicode.h"

// *****************************************************************************
// class UnicodeClassMapping
// This class maps categories to state change inputs for the
// WordBreakTable.  An entire category is mapped to the same
// value unless the character in question appears in the exception list.
// *****************************************************************************

// -------------------------------------

UnicodeClassMapping::UnicodeClassMapping(Type* mappedValue, 
                     int32_t mappedValue_length,
                                         const SpecialMapping* exceptionChars,
                                         int32_t exceptionChars_length,
                                         const bool_t *hasException,
                                         Type* asciiValues)
:   fMappedValue(mappedValue),
    fMappedValue_length(mappedValue_length),
    fExceptionChars(exceptionChars),
    fExceptionChars_length(exceptionChars_length),
    fHasException(hasException),
    fAsciiValues(asciiValues)
{
}

// -------------------------------------

TextBoundaryData::Type
UnicodeClassMapping::mappedChar(UChar ch) const
{
    if (ch <= 255) {
        return fAsciiValues[ ch ];
    }

    // get an appropriate category based on the character's Unicode class
    // if there's no entry in the exception table for that Unicode class,
    // we're done; otherwise we have to look in the exception table for
    // the character's category (\uffff is treated here as a sentinel
    // value meaning "end of the string"-- we always look in the exception
    // table for its category)
    Type chType = Unicode::getType(ch);
    if ((fExceptionChars_length == 0) ||
        (!fHasException[chType] && (ch != (UChar)0xFFFF)))
    {
        return fMappedValue[chType];
    }

    // The invariant during this loop is that the character ch is <= max and
    // >= min.  We iterate until min == max.
    int32_t min = 0;
    int32_t max = fExceptionChars_length - 1;
    while (max > min) {
        int32_t pos = (max + min) >> 1;
        if (ch > fExceptionChars[pos].fEndChar) {
            min = pos + 1; 
        }else{
            max = pos;
        }
    }
    const SpecialMapping* sm = &fExceptionChars[min];
    if (sm->fStartChar <= ch && ch <= sm->fEndChar)
        return sm->fNewValue;
    else
        return fMappedValue[chType];
}

//eof
