/*
 *   Copyright (C) 1997-1999, International Business Machines
 *   Corporation and others.  All Rights Reserved.
 */


#include "utypes.h"
#include "ucmp8.h"
#include "ucmp16.h"

struct DecompData {
    enum { MAX_CANONICAL = 21658 };
    enum { MAX_COMPAT = 11153 };
    enum { BASE = 0 };

    static const UChar offsets_index[];

    static const UChar offsets_values[];

    static const CompactShortArray* offsets;

    static const UChar contents[];

    static const UChar canonClass_index[];

    static const int8_t canonClass_values[];

    static CompactByteArray *canonClass;
};



