/*
* Copyright © {1997-1999}, International Business Machines Corporation and others. All Rights Reserved.
*****************************************************************************************
*
* File SPCLMAP.H
*
* SpecialMapping represents exceptions to the normal unicode category mapping.
*
* @package  Text and International
* @category Text Scanning
*
* Modification History:
*
*   Date        Name        Description
*   02/18/97    aliu        Converted from OpenClass.
*****************************************************************************************
*/

#ifndef SPCLMAP_H
#define SPCLMAP_H

#include "unicode/utypes.h" // UChar
#include "txtbdat.h"

/**
 * This class represents ranges of characters that are exceptions to the normal
 * unicode category mapping.  Characters from the start char to the end char,
 * inclusive, are mapped to the new value.
 */
class SpecialMapping {
public:
    /**
     * Create a special mapping from the single char ch to the value nv.
     */
    SpecialMapping(UChar ch, TextBoundaryData::Type nv) : fStartChar(ch), fEndChar(ch), fNewValue(nv) {}

    /**
     * Create a special mapping from the range of chars sch - ech, inclusive, to the value nv.
     */
    SpecialMapping(UChar sch, UChar ech, TextBoundaryData::Type nv) : fStartChar(sch), fEndChar(ech), fNewValue(nv) {}

    /**
     * The first character of the range.
     */
    UChar fStartChar;

    /**
     * The last character of the range.
     */
    UChar fEndChar;

    /**
     * The character mapping to use.
     */
    TextBoundaryData::Type fNewValue;
private:
    SpecialMapping() {}
};

#endif // _SPCLMAP
//eof
