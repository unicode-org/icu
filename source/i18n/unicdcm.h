/*
*****************************************************************************************
* Copyright © {1997-1999}, International Business Machines Corporation and others. All Rights Reserved.
*                                                                                       *
*****************************************************************************************
*
* File UNICDCM.H
*
* UnicodeClassMapping maps characters to state change inputs for WordBreakTable.
*
* @package  Text and International
* @category Text Scanning
*
* Modification History:
*
*   Date        Name        Description
*   02/18/97    aliu        Converted from OpenClass.
*   05/06/97    aliu        Made SpecialMapping an array of objects instead of pointers,
*                           to help out non-compliant compilers.
*****************************************************************************************
*/

#ifndef UNICDCM_H
#define UNICDCM_H

#include "spclmap.h"
#include "unicode/unicode.h"

/**
 * This class maps characters to state change inputs for
 * WordBreakTable.  If the character appears in the exception list,
 * the mapping there is retuned, otherwise the mapping returned by
 * IUnicode::type is returned.
 *
 * Note in this implementation characters from 0x0040 to 0x009f always use the
 * mapping returned by IUnicode::type and never the exception list.
 */
class UnicodeClassMapping {
public:
    // For convenience
    typedef TextBoundaryData::Type Type;

    /**
     * Create a mapping given a mapping from categories and a list
     * of exceptions.  Both the mapping list and exceptionChars list must
     * be sorted in ascending order.
     */
    UnicodeClassMapping(Type* mappedValue, 
            int32_t mappedValue_length,
                        const SpecialMapping* exceptionChars, 
            int32_t exceptionChars_length,
                        const bool_t* hasException,
                        Type* asiiValues );
    
    /**
     * Map a character to a state change input for WordBreakTable.
     * @param ch the character to map.
     * @return the mapped value.
     */
    Type mappedChar(UChar ch) const;

private:
    const bool_t            *fHasException;
    const Type*             fMappedValue;
    const int32_t           fMappedValue_length;
    const SpecialMapping*   fExceptionChars;
    const int32_t           fExceptionChars_length;
    const Type*             fAsciiValues;
};

#endif // _UNICDCM
//eof
