/*
 ******************************************************************************
 * Copyright (C) 2016 and later: Unicode, Inc. and others.
 * License & terms of use: http://www.unicode.org/copyright.html
 ******************************************************************************
 */

#ifndef __UNICODEREADER_H
#define __UNICODEREADER_H

#include "unicode/utypes.h"

#include "GUISupport.h"

class UnicodeReader
{
public:
    UnicodeReader()
    {
        // nothing...
    }

    ~UnicodeReader()
    {
        // nothing, too
    }

    static const UChar *readFile(const char *fileName, GUISupport *guiSupport, int32_t &charCount);
};

#endif

