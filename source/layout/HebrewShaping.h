/*
 * %W% %E%
 *
 * (C) Copyright IBM Corp. 1998, 1999, 2000 - All Rights Reserved
 *
 */

#ifndef __HEBREWSHAPING_H
#define __HEBREWSHAPING_H

#include "LETypes.h"
#include "OpenTypeTables.h"

class HebrewShaping
{
public:
    static void shape(const LEUnicode *chars, le_int32 offset, le_int32 charCount, le_int32 charMax,
                      le_bool rightToLeft, const LETag **tags);

    static le_uint8 glyphSubstitutionTable[];
    static le_uint8 glyphDefinitionTable[];
};

#endif
