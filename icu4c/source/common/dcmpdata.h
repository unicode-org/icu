/*
************************************************************************
*   Copyright (c) 1997-2000, International Business Machines
*   Corporation and others.  All Rights Reserved.
************************************************************************
* > THIS FILE WAS MACHINE GENERATED <
* >       DO NOT EDIT BY HAND       <
* >      RUN TOOL TO REGENERATE     <
* Tool: com.ibm.text.Normalizer
* Creation date: Fri Aug 11 10:18:35 PDT 2000
*/
#include "unicode/utypes.h"
#include "ucmp8.h"
#include "ucmp16.h"

struct DecompData {
    enum { MAX_CANONICAL = 21754 };
    enum { MAX_COMPAT = 11177 };
    enum { DECOMP_MASK = 32767 };
    enum { DECOMP_RECURSE = 32768 };
    enum { BASE = 0 };

    static const uint16_t offsets_index[];

    static const uint16_t offsets_values[];

    static const CompactShortArray* offsets;

    static const uint16_t contents[];

    static const uint16_t canonClass_index[];

    static const uint8_t canonClass_values[];

    static const CompactByteArray* canonClass;
};
